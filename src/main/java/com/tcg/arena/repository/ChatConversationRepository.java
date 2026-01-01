package com.tcg.arena.repository;

import com.tcg.arena.model.ChatConversation;
import com.tcg.arena.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    // Find all conversations for a user, ordered by last message
    @Query("SELECT c FROM ChatConversation c JOIN c.participants p WHERE p = :user ORDER BY c.lastMessageAt DESC")
    List<ChatConversation> findByUser(@Param("user") User user);

    // Find a direct conversation (FREE) between two users
    @Query("SELECT c FROM ChatConversation c WHERE c.type = 'FREE' AND :user1 MEMBER OF c.participants AND :user2 MEMBER OF c.participants AND SIZE(c.participants) = 2")
    Optional<ChatConversation> findFreeConversation(@Param("user1") User user1, @Param("user2") User user2);

    // Count total unread messages for a user across all their conversations
    // Query messages where user is a participant but not the sender and message is
    // unread
    @Query(value = "SELECT COALESCE(COUNT(*), 0) FROM chat_messages m " +
            "JOIN chat_conversation_participants cp ON cp.conversation_id = m.conversation_id " +
            "WHERE cp.user_id = :userId AND m.sender_id != :userId AND m.is_read = false", nativeQuery = true)
    int countUnreadByUserId(@Param("userId") Long userId);
}
