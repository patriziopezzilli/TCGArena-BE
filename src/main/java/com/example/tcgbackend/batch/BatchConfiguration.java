package com.example.tcgbackend.batch;

import com.example.tcgbackend.model.Card;
import com.example.tcgbackend.model.CardCondition;
import com.example.tcgbackend.model.Expansion;
import com.example.tcgbackend.model.Rarity;
import com.example.tcgbackend.model.TCGType;
import com.example.tcgbackend.repository.CardRepository;
import com.example.tcgbackend.service.ApiService;
import com.example.tcgbackend.service.CardService;
import com.example.tcgbackend.service.TCGApiClient;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
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
    private CardService cardService;

    @Autowired
    private TCGApiClient tcgApiClient;

    @Autowired
    private CardRepository cardRepository;

    @Bean
    public Job importCardsJob(JobRepository jobRepository, Step step1) {
        return new JobBuilder("importCardsJob", jobRepository)
                .start(step1)
                .build();
    }

    @Bean
    public Step step1(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("step1", jobRepository)
                .<Card, Card>chunk(50, transactionManager) // Process 50 cards at a time
                .reader(reader())
                .processor(processor())
                .writer(writer())
                .build();
    }

    @Bean
    public ItemReader<Card> reader() {
        // Import cards from all TCG types
        return new ItemReader<Card>() {
            private Iterator<Card> cardIterator;
            private boolean initialized = false;
            private int currentTcgIndex = 0;
            private final TCGType[] tcgTypes = {TCGType.POKEMON, TCGType.MAGIC, TCGType.ONE_PIECE};

            @Override
            public Card read() throws Exception {
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

                List<Card> cards = null;
                try {
                    switch (currentTcg) {
                        case POKEMON:
                            cards = tcgApiClient.fetchPokemonCards().collectList().block();
                            break;
                        case MAGIC:
                            cards = tcgApiClient.fetchMagicCards().collectList().block();
                            break;
                        case ONE_PIECE:
                            cards = tcgApiClient.fetchOnePieceCards().collectList().block();
                            break;
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
    public ItemProcessor<Card, Card> processor() {
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
                Expansion expansion = cardService.getExpansionByName(expansionName);
                if (expansion == null) {
                    expansion = new Expansion();
                    expansion.setTitle(expansionName);
                    expansion.setTcgType(card.getTcgType());
                    expansion = cardService.saveExpansion(expansion);
                }
                card.setExpansion(expansion);
            }

            // Check if card already exists to avoid duplicates (efficient database query)
            List<Card> existingCards = cardRepository.findByNameAndSetCodeAndCardNumber(
                    card.getName(), card.getSetCode(), card.getCardNumber());

            if (!existingCards.isEmpty()) {
                // Update existing card
                Card existingCard = existingCards.get(0);
                existingCard.setImageUrl(card.getImageUrl());
                existingCard.setDescription(card.getDescription());
                return existingCard;
            } else {
                // New card
                return card;
            }
        };
    }

    @Bean
    public ItemWriter<Card> writer() {
        return cards -> {
            for (Card card : cards) {
                cardService.saveCard(card);
            }
        };
    }
}