package com.tcg.arena.repository;

import com.tcg.arena.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversationIdOrderByTimestampAsc(Long conversationId);

    // Count unread messages for a user in a conversation (messages not sent by them
    // and not read)
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.conversation.id = :conversationId AND m.sender.id != :userId AND m.isRead = false")
    Long countUnreadByConversationAndRecipient(@Param("conversationId") Long conversationId,
            @Param("userId") Long userId);

    // Find unread messages in a conversation for marking as read
    @Query("SELECT m FROM ChatMessage m WHERE m.conversation.id = :conversationId AND m.sender.id != :userId AND m.isRead = false")
    List<ChatMessage> findUnreadByConversationAndRecipient(@Param("conversationId") Long conversationId,
            @Param("userId") Long userId);

    // Get the latest message for preview
    Optional<ChatMessage> findTopByConversationIdOrderByTimestampDesc(Long conversationId);
}
