package com.tcg.arena.batch;

import com.tcg.arena.model.Card;
import com.tcg.arena.model.CardTemplate;
import com.tcg.arena.model.Expansion;
import com.tcg.arena.model.TCGType;
import com.tcg.arena.repository.CardTemplateRepository;
import com.tcg.arena.service.CardTemplateService;
import com.tcg.arena.service.TCGApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Collections;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

@StepScope
class TCGCardReader implements ItemReader<CardTemplate> {

    private static final Logger logger = LoggerFactory.getLogger(TCGCardReader.class);

    private Iterator<CardTemplate> cardIterator;
    private boolean initialized = false;
    private int currentTcgIndex = 0;
    private TCGType[] tcgTypes;
    private TCGType specificTcgType = null;

    @Value("${app.demo-env:false}")
    private boolean demoEnv;

    private final TCGApiClient tcgApiClient;
    private final int startIndex;
    private final int endIndex;

    public TCGCardReader(TCGApiClient tcgApiClient, String tcgTypeParam, int startIndex, int endIndex) {
        if (tcgApiClient == null) {
            throw new IllegalArgumentException("TCGApiClient cannot be null");
        }
        this.tcgApiClient = tcgApiClient;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        logger.info("TCGCardReader initialized with tcgTypeParam: {}, startIndex: {}, endIndex: {}", tcgTypeParam,
                startIndex, endIndex);
        initializeTcgTypes(tcgTypeParam);
    }

    private void initializeTcgTypes(String param) {
        logger.info("Initializing TCG types with parameter: '{}'", param);

        if (param != null && !param.trim().isEmpty()) {
            try {
                specificTcgType = TCGType.valueOf(param.trim());
                tcgTypes = new TCGType[] { specificTcgType };
                logger.info("Initialized with specific TCG type: {}", specificTcgType);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid TCG type: '{}', falling back to all types. Valid types: {}", param,
                        java.util.Arrays.toString(TCGType.values()));
                tcgTypes = new TCGType[] { TCGType.POKEMON, TCGType.MAGIC, TCGType.ONE_PIECE };
            }
        } else {
            logger.info("No specific TCG type provided, importing all types");
            tcgTypes = new TCGType[] { TCGType.POKEMON, TCGType.MAGIC, TCGType.ONE_PIECE };
        }

        // Final safety check
        if (tcgTypes == null || tcgTypes.length == 0) {
            logger.error("CRITICAL: TCG types array is null or empty, forcing default values");
            tcgTypes = new TCGType[] { TCGType.POKEMON, TCGType.MAGIC, TCGType.ONE_PIECE };
        }

        logger.info("Final TCG types configuration: {}", java.util.Arrays.toString(tcgTypes));
    }

    @Override
    public CardTemplate read() throws Exception {
        // Safety check: ensure tcgTypes is initialized
        if (tcgTypes == null) {
            logger.warn("tcgTypes was null, initializing with all types");
            tcgTypes = new TCGType[] { TCGType.POKEMON, TCGType.MAGIC, TCGType.ONE_PIECE };
        }

        if (!initialized) {
            logger.info("Initializing card iterator for batch processing...");
            initializeCardIterator();
            initialized = true;
            logger.info("Card iterator initialized successfully");
        }

        // If current iterator is exhausted, try next TCG type
        while (cardIterator == null || !cardIterator.hasNext()) {
            if (currentTcgIndex >= tcgTypes.length) {
                return null; // All TCG types processed
            }

            currentTcgIndex++;
            if (currentTcgIndex < tcgTypes.length) {
                initializeCardIterator();
            } else {
                return null;
            }
        }

        return cardIterator.hasNext() ? cardIterator.next() : null;
    }

    private void initializeCardIterator() {
        // Safety check: ensure we have valid TCG types
        if (tcgTypes == null || tcgTypes.length == 0) {
            logger.error("No TCG types available, cannot initialize card iterator");
            cardIterator = Collections.emptyIterator();
            return;
        }

        if (currentTcgIndex >= tcgTypes.length) {
            cardIterator = null;
            return;
        }

        TCGType currentTcg = tcgTypes[currentTcgIndex];
        logger.info("Starting import for {} cards...", currentTcg);

        // NOTE: We no longer clear existing CardTemplates to preserve data
        // The processor() method will handle updates for existing cards and only add
        // new ones
        logger.info("Importing {} cards (updating existing and adding new ones)", currentTcg);

        List<CardTemplate> cards = null;
        try {
            // Fetch cards and convert to CardTemplate
            List<Card> rawCards = null;
            switch (currentTcg) {
                case POKEMON:
                    logger.info("Starting Pokemon card fetch...");
                    tcgApiClient.importCardsForTCG(TCGType.POKEMON).block();
                    logger.info("Pokemon cards imported successfully");
                    rawCards = new ArrayList<>(); // Empty list since cards are already saved
                    break;
                case MAGIC:
                    logger.info("Starting Magic card fetch...");
                    tcgApiClient.importCardsForTCG(TCGType.MAGIC).block();
                    logger.info("Magic cards imported successfully");
                    rawCards = new ArrayList<>(); // Empty list since cards are already saved
                    break;
                case ONE_PIECE:
                    logger.info("Starting One Piece card fetch...");
                    tcgApiClient.importCardsForTCG(TCGType.ONE_PIECE).block();
                    logger.info("One Piece cards imported successfully");
                    rawCards = new ArrayList<>(); // Empty list since cards are already saved
                    break;
                case DIGIMON:
                    logger.warn("Digimon card import not implemented yet");
                    rawCards = new ArrayList<>();
                    break;
                case YUGIOH:
                    logger.warn("Yu-Gi-Oh card import not implemented yet");
                    rawCards = new ArrayList<>();
                    break;
                default:
                    // Handle other TCG types (Dragon Ball, Flesh and Blood, Lorcana, etc.)
                    logger.warn("{} card import not implemented yet", currentTcg.getDisplayName());
                    rawCards = new ArrayList<>();
                    break;
            }

            // Convert Card to CardTemplate
            if (rawCards != null) {
                logger.info("Converting {} cards to CardTemplate...", rawCards.size());
                cards = rawCards.stream().map(card -> {
                    CardTemplate template = new CardTemplate();
                    template.setName(card.getName());
                    template.setTcgType(card.getTcgType());
                    template.setSetCode(card.getSetCode());
                    template.setExpansion(card.getExpansion());
                    template.setRarity(card.getRarity());
                    template.setCardNumber(card.getCardNumber()); // Add cardNumber mapping
                    template.setImageUrl(card.getImageUrl());
                    template.setDescription(card.getDescription());
                    template.setManaCost(card.getManaCost());
                    template.setDateCreated(
                            card.getDateAdded() != null ? card.getDateAdded() : java.time.LocalDateTime.now());
                    return template;
                }).collect(java.util.stream.Collectors.toList());
                logger.info("Conversion completed: {} CardTemplates created", cards.size());
            }
        } catch (Exception e) {
            logger.error("Error fetching cards for {}: {}", currentTcg, e.getMessage());
            cards = Collections.emptyList();
        }

        cardIterator = cards != null ? cards.iterator() : Collections.emptyIterator();
        logger.info("Loaded {} {} cards", (cards != null ? cards.size() : 0), currentTcg);
    }
}

@Configuration
public class BatchConfiguration {

    @Autowired
    private CardTemplateService cardTemplateService;

    @Autowired
    private TCGApiClient tcgApiClient;

    @Autowired
    private CardTemplateRepository cardTemplateRepository;

    @Bean
    public Job importCardsJob(JobRepository jobRepository, Step step1) {
        return new JobBuilder("importCardsJob", jobRepository)
                .start(step1)
                .build();
    }

    @Bean
    public Step step1(JobRepository jobRepository, PlatformTransactionManager transactionManager,
            @Qualifier("tcgCardReader") ItemReader<CardTemplate> reader) {
        return new StepBuilder("step1", jobRepository)
                .<CardTemplate, CardTemplate>chunk(10, transactionManager) // Process 10 cards at a time
                .reader(reader)
                .processor(processor())
                .writer(writer())
                .build();
    }

    @Bean
    @StepScope
    @Qualifier("tcgCardReader")
    public ItemReader<CardTemplate> tcgCardReader(
            @Value("#{jobParameters['tcgType']}") String tcgTypeParam,
            @Value("#{jobParameters['startIndex']}") Long startIndexParam,
            @Value("#{jobParameters['endIndex']}") Long endIndexParam) {
        int startIndex = startIndexParam != null ? startIndexParam.intValue() : -99;
        int endIndex = endIndexParam != null ? endIndexParam.intValue() : -99;
        return new TCGCardReader(tcgApiClient, tcgTypeParam, startIndex, endIndex);
    }

    @Bean
    public ItemProcessor<CardTemplate, CardTemplate> processor() {
        return card -> {
            // Extract expansion name from description if present
            String expansionName = null;
            String description = card.getDescription();
            if (description != null && description.contains("[Expansion: ")) {
                int start = description.indexOf("[Expansion: ") + 12;
                int end = description.indexOf("]", start);
                if (end > start) {
                    expansionName = description.substring(start, end);
                    card.setDescription(description.substring(0, description.indexOf(" [Expansion: ")));
                }
            }

            // Handle expansion
            if (expansionName != null) {
                Expansion expansion = cardTemplateService.getExpansionByName(expansionName);
                if (expansion == null) {
                    expansion = new Expansion();
                    expansion.setTitle(expansionName);
                    expansion.setTcgType(card.getTcgType());
                    expansion = cardTemplateService.saveExpansion(expansion);
                }
                card.setExpansion(expansion);
            }

            // Check if card template already exists to avoid duplicates (efficient database
            // query)
            List<CardTemplate> existingCards = cardTemplateRepository.findByNameAndSetCodeAndCardNumber(
                    card.getName(), card.getSetCode(), card.getCardNumber());

            if (!existingCards.isEmpty()) {
                // Update existing card template
                CardTemplate existingCard = existingCards.get(0);
                existingCard.setImageUrl(card.getImageUrl());
                existingCard.setDescription(card.getDescription());
                return existingCard;
            } else {
                // New card template
                return card;
            }
        };
    }

    @Bean
    public ItemWriter<CardTemplate> writer() {
        return cards -> {
            cardTemplateService.saveAllCardTemplates(cards.getItems());
        };
    }
}