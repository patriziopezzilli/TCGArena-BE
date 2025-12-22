package com.tcg.arena.service;

import com.tcg.arena.model.CardTemplate;
import com.tcg.arena.repository.CardTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class CardImageService {

    @Autowired
    private CardTemplateRepository cardTemplateRepository;

    @Value("${app.image-storage.path}")
    private String storagePath;

    private final AtomicInteger processedCount = new AtomicInteger(0);
    private final AtomicInteger totalCount = new AtomicInteger(0);
    private boolean isSyncing = false;

    @Async
    @org.springframework.transaction.annotation.Transactional
    public void syncImages(String tcgTypeFilter, Integer yearFilter) {
        if (isSyncing) {
            System.out.println("Image sync already in progress.");
            return;
        }

        isSyncing = true;
        processedCount.set(0);

        try {
            System.out.println("Starting card image sync to: " + storagePath);
            if (tcgTypeFilter != null)
                System.out.println("Filter TCG: " + tcgTypeFilter);
            if (yearFilter != null)
                System.out.println("Filter Year: " + yearFilter);

            // Create directory if not exists
            Path rootLocation = Paths.get(storagePath);
            if (!Files.exists(rootLocation)) {
                Files.createDirectories(rootLocation);
            }

            List<CardTemplate> allCards = cardTemplateRepository.findAll();

            // Apply Filters
            List<CardTemplate> cardsToProcess = allCards.stream()
                    .filter(card -> {
                        if (tcgTypeFilter != null && !tcgTypeFilter.isEmpty()) {
                            return card.getTcgType() != null &&
                                    card.getTcgType().name().equalsIgnoreCase(tcgTypeFilter);
                        }
                        return true;
                    })
                    .filter(card -> {
                        if (yearFilter != null) {
                            if (card.getExpansion() == null)
                                return false;
                            // Use the transient method getReleaseDate()
                            return card.getExpansion().getReleaseDate().getYear() == yearFilter;
                        }
                        return true;
                    })
                    .toList();

            totalCount.set(cardsToProcess.size());

            System.out.println("Found " + cardsToProcess.size() + " cards to process (after filtering).");

            for (CardTemplate card : cardsToProcess) {
                try {
                    downloadImageForCard(card, rootLocation);
                } catch (Exception e) {
                    System.err.println("Failed to process card " + card.getId() + ": " + e.getMessage());
                }
                processedCount.incrementAndGet();

                if (processedCount.get() % 100 == 0) {
                    System.out.println("Processed " + processedCount.get() + "/" + totalCount.get() + " images...");
                }
            }

            System.out.println("Card image sync completed.");

        } catch (Exception e) {
            System.err.println("Error during image sync: " + e.getMessage());
            e.printStackTrace();
        } finally {
            isSyncing = false;
        }
    }

    private void downloadImageForCard(CardTemplate card, Path rootLocation) {
        // Use ID as filename to ensure uniqueness and simplicity for the backend
        // We can also use tcgplayerId if preferred, but ID is internal and safe.
        // User suggested: "puoi usare card template id per il nome del file"
        String filename = card.getId() + ".jpg";
        Path destinationFile = rootLocation.resolve(filename);

        if (Files.exists(destinationFile)) {
            // Skip if already exists
            return;
        }

        String imageUrl = card.getImageUrl();
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        try (InputStream in = new URL(imageUrl).openStream()) {
            Files.copy(in, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            System.err.println(
                    "Failed to download image for card " + card.getId() + " (" + card.getName() + ") from " + imageUrl);
        }
    }

    public String getStatus() {
        if (!isSyncing) {
            return "Idle. Last run processed " + processedCount.get() + " frames.";
        }
        return "Syncing: " + processedCount.get() + "/" + totalCount.get();
    }
}
