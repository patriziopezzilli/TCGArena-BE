package com.tcg.arena.repository;

import com.tcg.arena.model.PendingReview;
import com.tcg.arena.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PendingReviewRepository extends JpaRepository<PendingReview, Long> {

    // Find all pending reviews for a user (not yet submitted)
    List<PendingReview> findByReviewerAndCompletedAtIsNullOrderByCreatedAtDesc(User reviewer);

    // Find by reviewer ID
    List<PendingReview> findByReviewerIdAndCompletedAtIsNullOrderByCreatedAtDesc(Long reviewerId);

    // Count pending reviews for a user
    long countByReviewerIdAndCompletedAtIsNull(Long reviewerId);

    // Find reviews by conversation
    List<PendingReview> findByConversationId(Long conversationId);

    // Check if a pending review exists for a user on a conversation
    Optional<PendingReview> findByConversationIdAndReviewerId(Long conversationId, Long reviewerId);

    // Find all completed reviews for a user (as reviewee)
    List<PendingReview> findByRevieweeIdAndCompletedAtIsNotNullOrderByCompletedAtDesc(Long revieweeId);
}
