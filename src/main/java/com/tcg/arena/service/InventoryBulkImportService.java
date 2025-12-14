package com.tcg.arena.service;

import com.tcg.arena.dto.InventoryBulkImportDTO.*;
import com.tcg.arena.dto.InventoryCardDTO.CreateInventoryCardRequest;
import com.tcg.arena.model.CardNationality;
import com.tcg.arena.model.CardTemplate;
import com.tcg.arena.model.InventoryCard.CardCondition;
import com.tcg.arena.model.InventoryImportRequest;
import com.tcg.arena.model.InventoryImportRequest.ImportStatus;
import com.tcg.arena.repository.CardTemplateRepository;
import com.tcg.arena.repository.InventoryImportRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class InventoryBulkImportService {

    private static final Logger log = LoggerFactory.getLogger(InventoryBulkImportService.class);

    private static final String CSV_HEADER = "card_name,set_code,tcg_type,condition,quantity,price,nationality,notes";
    private static final String CSV_EXAMPLE = "Charizard,SV01,POKEMON,NEAR_MINT,2,9.99,EN,\"My first Charizard\"";

    private final InventoryImportRequestRepository importRequestRepository;
    private final InventoryCardService inventoryCardService;
    private final CardTemplateRepository cardTemplateRepository;

    public InventoryBulkImportService(
            InventoryImportRequestRepository importRequestRepository,
            InventoryCardService inventoryCardService,
            CardTemplateRepository cardTemplateRepository) {
        this.importRequestRepository = importRequestRepository;
        this.inventoryCardService = inventoryCardService;
        this.cardTemplateRepository = cardTemplateRepository;
    }

    /**
     * Generate CSV template for download
     */
    public byte[] generateCSVTemplate() {
        StringBuilder sb = new StringBuilder();
        sb.append(CSV_HEADER).append("\n");
        sb.append("# Example row (remove this line before importing):\n");
        sb.append("# ").append(CSV_EXAMPLE).append("\n");
        sb.append("\n");
        sb.append("# COLUMNS:\n");
        sb.append("# card_name: Card name (partial match, e.g. 'Charizard' or 'char')\n");
        sb.append("# set_code: Set code (exact match, e.g. 'SV01', 'PAL')\n");
        sb.append("# tcg_type: TCG type (exact match: POKEMON, MAGIC, YUGIOH, ONEPIECE, LORCANA, DRAGONBALL)\n");
        sb.append("# condition: MINT, NEAR_MINT, EXCELLENT, GOOD, LIGHT_PLAYED, PLAYED, POOR\n");
        sb.append("# nationality: EN, ITA, JPN, COR, FRA, GER, SPA, POR, CHI, RUS (default: EN)\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Process bulk import from standard CSV template
     */
    @Transactional
    public BulkImportResponse bulkImportFromCSV(Long shopId, MultipartFile file) throws IOException {
        log.info("Starting bulk import for shop: {}", shopId);

        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int totalRows = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Skip header, empty lines, and comments
                if (lineNumber == 1 || line.trim().isEmpty() || line.trim().startsWith("#")) {
                    continue;
                }

                totalRows++;

                try {
                    BulkImportRow row = parseCSVRow(line, lineNumber);
                    processImportRow(shopId, row);
                    successCount++;
                } catch (Exception e) {
                    String error = String.format("Row %d: %s", lineNumber, e.getMessage());
                    errors.add(error);
                    log.warn("Error processing row {}: {}", lineNumber, e.getMessage());
                }
            }
        }

        String message = successCount == totalRows
                ? "Import completato con successo"
                : String.format("Import completato con %d errori", errors.size());

        log.info("Bulk import completed for shop {}: {} success, {} errors",
                shopId, successCount, errors.size());

        return new BulkImportResponse(totalRows, successCount, errors.size(), errors, message);
    }

    /**
     * Save custom import request for AI processing
     */
    @Transactional
    public ImportRequestResponse saveCustomImportRequest(Long shopId, MultipartFile file, String notes)
            throws IOException {
        log.info("Saving custom import request for shop: {}", shopId);

        InventoryImportRequest request = new InventoryImportRequest(
                shopId,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getBytes(),
                notes);

        InventoryImportRequest saved = importRequestRepository.save(request);

        log.info("Custom import request saved with ID: {}", saved.getId());

        return new ImportRequestResponse(
                saved.getId(),
                "La tua richiesta è stata salvata con successo. Verrà elaborata dal nostro team.",
                "48 ore");
    }

    /**
     * Get import requests for a shop
     */
    @Transactional(readOnly = true)
    public List<InventoryImportRequest> getImportRequests(Long shopId, ImportStatus status) {
        if (status != null) {
            return importRequestRepository.findByShopIdAndStatusOrderByCreatedAtDesc(shopId, status);
        }
        return importRequestRepository.findByShopIdOrderByCreatedAtDesc(shopId, null).getContent();
    }

    /**
     * Parse a CSV row into BulkImportRow
     * Format:
     * card_name,set_code,tcg_type,condition,quantity,price,nationality,notes
     */
    private BulkImportRow parseCSVRow(String line, int lineNumber) {
        String[] parts = parseCSVLine(line);

        if (parts.length < 6) {
            throw new IllegalArgumentException(
                    "Colonne insufficienti (minimo 6: card_name, set_code, tcg_type, condition, quantity, price)");
        }

        BulkImportRow row = new BulkImportRow();

        // card_name (required)
        String cardName = parts[0].trim();
        if (cardName.isEmpty()) {
            throw new IllegalArgumentException("card_name vuoto");
        }
        row.setCardName(cardName);

        // set_code (required)
        String setCode = parts[1].trim().toUpperCase();
        if (setCode.isEmpty()) {
            throw new IllegalArgumentException("set_code vuoto");
        }
        row.setSetCode(setCode);

        // tcg_type (required)
        String tcgType = parts[2].trim().toUpperCase();
        if (tcgType.isEmpty()) {
            throw new IllegalArgumentException("tcg_type vuoto");
        }
        row.setTcgType(tcgType);

        // condition (required)
        String condition = parts[3].trim().toUpperCase();
        try {
            CardCondition.valueOf(condition);
            row.setCondition(condition);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Condizione non valida: " + parts[3]);
        }

        // quantity (required)
        try {
            row.setQuantity(Integer.parseInt(parts[4].trim()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Quantità non valida: " + parts[4]);
        }

        // price (required)
        try {
            row.setPrice(Double.parseDouble(parts[5].trim()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Prezzo non valido: " + parts[5]);
        }

        // nationality (optional, default EN)
        if (parts.length > 6 && !parts[6].trim().isEmpty()) {
            row.setNationality(parts[6].trim().toUpperCase());
        } else {
            row.setNationality("EN");
        }

        // notes (optional)
        if (parts.length > 7) {
            row.setNotes(parts[7].trim().replace("\"", ""));
        }

        return row;
    }

    /**
     * Parse CSV line handling quoted values
     */
    private String[] parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());

        return result.toArray(new String[0]);
    }

    /**
     * Process a single import row - finds card by name (LIKE), setCode, and tcgType
     */
    private void processImportRow(Long shopId, BulkImportRow row) {
        // Search for card template using LIKE match on name + exact match on setCode +
        // tcgType
        List<CardTemplate> matches = cardTemplateRepository.findByNameLikeAndSetCodeAndTcgType(
                row.getCardName(),
                row.getSetCode(),
                row.getTcgType());

        if (matches.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "Carta non trovata: '%s' nel set '%s' (%s)",
                    row.getCardName(), row.getSetCode(), row.getTcgType()));
        }

        if (matches.size() > 1) {
            throw new IllegalArgumentException(String.format(
                    "Più carte corrispondono a '%s' nel set '%s': trovate %d carte. Specifica un nome più preciso.",
                    row.getCardName(), row.getSetCode(), matches.size()));
        }

        CardTemplate cardTemplate = matches.get(0);
        row.setCardTemplateId(cardTemplate.getId());

        CreateInventoryCardRequest request = new CreateInventoryCardRequest();
        request.setCardTemplateId(cardTemplate.getId());
        request.setShopId(shopId);
        request.setCondition(CardCondition.valueOf(row.getCondition()));
        request.setQuantity(row.getQuantity());
        request.setPrice(row.getPrice());
        request.setNotes(row.getNotes());

        if (row.getNationality() != null) {
            try {
                request.setNationality(CardNationality.valueOf(row.getNationality()));
            } catch (IllegalArgumentException e) {
                request.setNationality(CardNationality.EN);
            }
        }

        inventoryCardService.createInventoryCard(request);
        log.debug("Imported card '{}' (ID: {}) for shop {}", cardTemplate.getName(), cardTemplate.getId(), shopId);
    }

    /**
     * Bulk add all cards from a specific set
     */
    @Transactional
    public BulkImportResponse bulkAddBySetCode(Long shopId, String setCode, String condition,
            int quantity, double price, String nationality) {
        log.info("Starting bulk add by set code '{}' for shop: {}", setCode, shopId);

        List<CardTemplate> templates = cardTemplateRepository.findBySetCode(setCode);
        return processBulkAdd(shopId, templates, condition, quantity, price, nationality,
                "set '" + setCode + "'");
    }

    /**
     * Bulk add all cards from a specific expansion
     */
    @Transactional
    public BulkImportResponse bulkAddByExpansionId(Long shopId, Long expansionId, String condition,
            int quantity, double price, String nationality) {
        log.info("Starting bulk add by expansion {} for shop: {}", expansionId, shopId);

        List<CardTemplate> templates = cardTemplateRepository.findByExpansionId(expansionId);
        return processBulkAdd(shopId, templates, condition, quantity, price, nationality,
                "expansion ID " + expansionId);
    }

    /**
     * Bulk add specific card templates by their IDs
     */
    @Transactional
    public BulkImportResponse bulkAddByTemplateIds(Long shopId, List<Long> templateIds, String condition,
            int quantity, double price, String nationality) {
        log.info("Starting bulk add of {} templates for shop: {}", templateIds.size(), shopId);

        List<CardTemplate> templates = cardTemplateRepository.findAllById(templateIds);
        return processBulkAdd(shopId, templates, condition, quantity, price, nationality,
                templateIds.size() + " selected cards");
    }

    /**
     * Common method to process bulk add for a list of card templates
     */
    private BulkImportResponse processBulkAdd(Long shopId, List<CardTemplate> templates,
            String condition, int quantity, double price, String nationality, String sourceDesc) {

        if (templates.isEmpty()) {
            return new BulkImportResponse(0, 0, 1,
                    List.of("Nessuna carta trovata per " + sourceDesc),
                    "Nessuna carta trovata");
        }

        List<String> errors = new ArrayList<>();
        int successCount = 0;

        CardCondition cardCondition;
        try {
            cardCondition = CardCondition.valueOf(condition);
        } catch (IllegalArgumentException e) {
            return new BulkImportResponse(0, 0, 1,
                    List.of("Condizione non valida: " + condition),
                    "Errore: condizione non valida");
        }

        CardNationality cardNationality = CardNationality.EN;
        if (nationality != null && !nationality.isEmpty()) {
            try {
                cardNationality = CardNationality.valueOf(nationality);
            } catch (IllegalArgumentException e) {
                // Fallback to EN
            }
        }

        for (CardTemplate template : templates) {
            try {
                CreateInventoryCardRequest request = new CreateInventoryCardRequest();
                request.setCardTemplateId(template.getId());
                request.setShopId(shopId);
                request.setCondition(cardCondition);
                request.setQuantity(quantity);
                request.setPrice(price);
                request.setNationality(cardNationality);

                inventoryCardService.createInventoryCard(request);
                successCount++;
            } catch (Exception e) {
                String error = String.format("Carta '%s': %s", template.getName(), e.getMessage());
                errors.add(error);
                log.warn("Error adding card '{}' to inventory: {}", template.getName(), e.getMessage());
            }
        }

        String message = successCount == templates.size()
                ? String.format("Import completato: %d carte aggiunte da %s", successCount, sourceDesc)
                : String.format("Import completato con %d errori su %d carte", errors.size(), templates.size());

        log.info("Bulk add for shop {} from {}: {} success, {} errors",
                shopId, sourceDesc, successCount, errors.size());

        return new BulkImportResponse(templates.size(), successCount, errors.size(), errors, message);
    }
}
