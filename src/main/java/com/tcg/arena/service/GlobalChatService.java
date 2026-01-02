package com.tcg.arena.service;

import com.tcg.arena.dto.GlobalChatMessageDto;
import com.tcg.arena.model.GlobalChatMessage;
import com.tcg.arena.model.User;
import com.tcg.arena.repository.GlobalChatRepository;
import com.tcg.arena.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for global Arena Chat functionality.
 * Handles message creation, rate limiting, and retrieval.
 */
@Service
public class GlobalChatService {

    private static final int RATE_LIMIT_SECONDS = 20;
    private static final int MAX_MESSAGE_LENGTH = 500;

    @Autowired
    private GlobalChatRepository chatRepository;

    @Autowired
    private UserRepository userRepository;

    // In-memory rate limit tracking (userId -> lastMessageTime)
    private final Map<Long, LocalDateTime> userLastMessageTime = new ConcurrentHashMap<>();

    /**
     * Send a new message to the global chat.
     * Returns the created message DTO if successful, null if rate limited.
     */
    public GlobalChatMessageDto sendMessage(Long userId, String content) {
        // Validate content
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }

        String trimmedContent = content.trim();
        if (trimmedContent.length() > MAX_MESSAGE_LENGTH) {
            trimmedContent = trimmedContent.substring(0, MAX_MESSAGE_LENGTH);
        }

        // Check rate limit
        if (!canUserSendMessage(userId)) {
            return null; // Rate limited
        }

        // Get user info
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User user = userOpt.get();

        // Create and save message
        GlobalChatMessage message = new GlobalChatMessage(
                userId,
                user.getUsername(),
                user.getDisplayName(),
                trimmedContent);
        message = chatRepository.save(message);

        // Update rate limit tracker
        userLastMessageTime.put(userId, LocalDateTime.now());

        return new GlobalChatMessageDto(message);
    }

    /**
     * Check if user can send a message (rate limit check).
     */
    public boolean canUserSendMessage(Long userId) {
        LocalDateTime lastMessage = userLastMessageTime.get(userId);
        if (lastMessage == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(lastMessage.plusSeconds(RATE_LIMIT_SECONDS));
    }

    /**
     * Get seconds remaining until user can send next message.
     */
    public int getSecondsUntilNextMessage(Long userId) {
        LocalDateTime lastMessage = userLastMessageTime.get(userId);
        if (lastMessage == null) {
            return 0;
        }
        LocalDateTime nextAllowed = lastMessage.plusSeconds(RATE_LIMIT_SECONDS);
        if (LocalDateTime.now().isAfter(nextAllowed)) {
            return 0;
        }
        return (int) java.time.Duration.between(LocalDateTime.now(), nextAllowed).getSeconds();
    }

    /**
     * Get recent messages for initial load.
     */
    public List<GlobalChatMessageDto> getRecentMessages() {
        List<GlobalChatMessage> messages = chatRepository.findRecentMessages();
        // Reverse to get chronological order (oldest first)
        Collections.reverse(messages);
        return messages.stream()
                .map(GlobalChatMessageDto::new)
                .collect(Collectors.toList());
    }

    /**
     * Get messages after a specific ID (for sync after reconnect).
     */
    public List<GlobalChatMessageDto> getMessagesAfterId(Long afterId) {
        return chatRepository.findMessagesAfterId(afterId).stream()
                .map(GlobalChatMessageDto::new)
                .collect(Collectors.toList());
    }
}
