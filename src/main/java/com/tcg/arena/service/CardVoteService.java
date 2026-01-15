package com.tcg.arena.service;

import com.tcg.arena.dto.CardVoteRequest;
import com.tcg.arena.dto.CardVoteStatsDTO;
import com.tcg.arena.model.CardTemplate;
import com.tcg.arena.model.CardVote;
import com.tcg.arena.model.TCGType;
import com.tcg.arena.model.User;
import com.tcg.arena.repository.CardTemplateRepository;
import com.tcg.arena.repository.CardVoteRepository;
import com.tcg.arena.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CardVoteService {

    @Autowired
    private CardVoteRepository voteRepository;

    @Autowired
    private CardTemplateRepository cardTemplateRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public CardVoteStatsDTO submitVote(Long cardTemplateId, CardVoteRequest request, Long userId) {
        CardTemplate cardTemplate = cardTemplateRepository.findById(cardTemplateId)
                .orElseThrow(() -> new RuntimeException("Card template not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Use findByCardTemplateAndUser with pessimistic locking to prevent race conditions
        Optional<CardVote> existingVote = voteRepository.findByCardTemplateAndUser(cardTemplate, user);

        if (existingVote.isPresent()) {
            CardVote vote = existingVote.get();
            CardVote.VoteType oldVoteType = vote.getVoteType();
            CardVote.VoteType newVoteType = request.getVoteType();

            if (!oldVoteType.equals(newVoteType)) {
                // Update vote type and counters
                if (oldVoteType == CardVote.VoteType.LIKE) {
                    cardTemplate.decrementLikesCount();
                } else {
                    cardTemplate.decrementDislikesCount();
                }

                if (newVoteType == CardVote.VoteType.LIKE) {
                    cardTemplate.incrementLikesCount();
                } else {
                    cardTemplate.incrementDislikesCount();
                }

                vote.setVoteType(newVoteType);
                vote.setVotedAt(LocalDateTime.now());
                voteRepository.save(vote);
            }
        } else {
            // Create new vote
            try {
                CardVote vote = new CardVote();
                vote.setCardTemplate(cardTemplate);
                vote.setUser(user);
                vote.setVoteType(request.getVoteType());
                vote.setVotedAt(LocalDateTime.now());
                voteRepository.save(vote);

                // Update counters
                if (request.getVoteType() == CardVote.VoteType.LIKE) {
                    cardTemplate.incrementLikesCount();
                } else {
                    cardTemplate.incrementDislikesCount();
                }
            } catch (Exception e) {
                // Handle duplicate key constraint violation (race condition)
                // If vote was already created by concurrent request, fetch and return stats
                if (e.getMessage() != null && e.getMessage().contains("unique constraint")) {
                    // Vote already exists, just return current stats
                    return new CardVoteStatsDTO(
                            cardTemplate.getId(),
                            cardTemplate.getLikesCount(),
                            cardTemplate.getDislikesCount()
                    );
                }
                throw e;
            }
        }

        cardTemplateRepository.save(cardTemplate);

        return new CardVoteStatsDTO(
                cardTemplate.getId(),
                cardTemplate.getLikesCount(),
                cardTemplate.getDislikesCount()
        );
    }

    public Page<CardTemplate> getDiscoverFeed(TCGType tcgType, Long userId, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Pageable pageable = PageRequest.of(page, size);
        return voteRepository.findRandomUnvotedCardTemplates(tcgType, user, pageable);
    }

    public CardVoteStatsDTO getVoteStats(Long cardTemplateId) {
        CardTemplate cardTemplate = cardTemplateRepository.findById(cardTemplateId)
                .orElseThrow(() -> new RuntimeException("Card template not found"));

        return new CardVoteStatsDTO(
                cardTemplate.getId(),
                cardTemplate.getLikesCount(),
                cardTemplate.getDislikesCount()
        );
    }

    public Page<CardTemplate> getTopLovedCards(TCGType tcgType, Pageable pageable) {
        if (tcgType != null) {
            return cardTemplateRepository.findByTcgTypeOrderByLikesCountDesc(tcgType, pageable);
        } else {
            return cardTemplateRepository.findAllByOrderByLikesCountDesc(pageable);
        }
    }

    public Page<CardTemplate> getTopHatedCards(TCGType tcgType, Pageable pageable) {
        if (tcgType != null) {
            return cardTemplateRepository.findByTcgTypeOrderByDislikesCountDesc(tcgType, pageable);
        } else {
            return cardTemplateRepository.findAllByOrderByDislikesCountDesc(pageable);
        }
    }
}
