package com.example.tcgbackend.batch;

import com.example.tcgbackend.model.Card;
import com.example.tcgbackend.model.CardTemplate;
import com.example.tcgbackend.model.Expansion;
import com.example.tcgbackend.model.Rarity;
import com.example.tcgbackend.model.TCGType;
import com.example.tcgbackend.repository.CardTemplateRepository;
import com.example.tcgbackend.service.ApiService;
import com.example.tcgbackend.service.CardTemplateService;
import com.example.tcgbackend.service.TCGApiClient;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Configuration
public class BatchConfiguration {

    @Autowired
    private ApiService apiService;

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
    public Step step1(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("step1", jobRepository)
                .<CardTemplate, CardTemplate>chunk(50, transactionManager) // Process 50 cards at a time
                .reader(reader())
                .processor(processor())
                .writer(writer())
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<CardTemplate> reader() {
        // Import cards from specific TCG type if provided, otherwise all types
        return new ItemReader<CardTemplate>() {
            private Iterator<CardTemplate> cardIterator;
            private boolean initialized = false;
            private int currentTcgIndex = 0;
            private TCGType[] tcgTypes;
            private TCGType specificTcgType = null;

            @BeforeStep
            public void beforeStep(StepExecution stepExecution) {
                String param = stepExecution.getJobParameters().getString("tcgType");
                if (param != null && !param.isEmpty()) {
                    try {
                        specificTcgType = TCGType.valueOf(param);
                        tcgTypes = new TCGType[]{specificTcgType};
                    } catch (IllegalArgumentException e) {
                        System.err.println("Invalid TCG type: " + param + ", importing all types");
                        tcgTypes = new TCGType[]{TCGType.POKEMON, TCGType.MAGIC, TCGType.ONE_PIECE};
                    }
                } else {
                    tcgTypes = new TCGType[]{TCGType.POKEMON, TCGType.MAGIC, TCGType.ONE_PIECE};
                }
            }

            @Override
            public CardTemplate read() throws Exception {
                if (!initialized) {
                    initializeCardIterator();
                    initialized = true;
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
                if (currentTcgIndex >= tcgTypes.length) {
                    cardIterator = null;
                    return;
                }

                TCGType currentTcg = tcgTypes[currentTcgIndex];
                System.out.println("Starting import for " + currentTcg + " cards...");

                List<CardTemplate> cards = null;
                try {
                    // Fetch cards and convert to CardTemplate
                    List<Card> rawCards = null;
                    switch (currentTcg) {
                        case POKEMON:
                            rawCards = tcgApiClient.fetchPokemonCards().collectList().block();
                            break;
                        case MAGIC:
                            rawCards = tcgApiClient.fetchMagicCards().collectList().block();
                            break;
                        case ONE_PIECE:
                            rawCards = tcgApiClient.fetchOnePieceCards().collectList().block();
                            break;
                    }

                    // Convert Card to CardTemplate
                    if (rawCards != null) {
                        cards = rawCards.stream().map(card -> {
                            CardTemplate template = new CardTemplate();
                            template.setName(card.getName());
                            template.setTcgType(card.getTcgType());
                            template.setSetCode(card.getSetCode());
                            template.setExpansion(card.getExpansion());
                            template.setRarity(card.getRarity());
                            template.setCardNumber(card.getCardNumber());
                            template.setDescription(card.getDescription());
                            template.setImageUrl(card.getImageUrl());
                            template.setMarketPrice(card.getMarketPrice());
                            template.setManaCost(card.getManaCost());
                            return template;
                        }).toList();
                    }
                } catch (Exception e) {
                    System.err.println("Error fetching " + currentTcg + " cards: " + e.getMessage());
                    cards = Collections.emptyList();
                }

                cardIterator = cards != null ? cards.iterator() : Collections.emptyIterator();
                System.out.println("Loaded " + (cards != null ? cards.size() : 0) + " " + currentTcg + " cards");
            }
        };
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

            // Check if card template already exists to avoid duplicates (efficient database query)
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
            for (CardTemplate card : cards) {
                cardTemplateService.saveCardTemplate(card);
            }
        };
    }
}