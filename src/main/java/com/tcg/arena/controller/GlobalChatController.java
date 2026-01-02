package com.tcg.arena.controller;

import com.tcg.arena.dto.GlobalChatMessageDto;
import com.tcg.arena.model.User;
import com.tcg.arena.repository.UserRepository;
import com.tcg.arena.service.GlobalChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for Arena Chat WebSocket messages and REST endpoints.
 */
@Controller
public class GlobalChatController {

    @Autowired
    private GlobalChatService chatService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Handle incoming chat messages via WebSocket STOMP.
     * Client sends to: /app/arena-chat
     * Response broadcast to: /topic/arena-chat
     */
    @MessageMapping("/arena-chat")
    @SendTo("/topic/arena-chat")
    public GlobalChatMessageDto handleMessage(
            @Payload ChatMessageRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        // Get user from session or token
        Principal principal = headerAccessor.getUser();
        if (principal == null) {
            // Return error message or null
            return null;
        }

        // Get user by username
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null) {
            return null;
        }

        // Send message (returns null if rate limited)
        GlobalChatMessageDto message = chatService.sendMessage(user.getId(), request.getContent());

        return message; // Will be broadcast to all subscribers if not null
    }

    /**
     * Simple message request DTO for incoming WebSocket messages.
     */
    public static class ChatMessageRequest {
        private String content;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}

/**
 * REST controller for chat-related HTTP endpoints.
 */
@RestController
@RequestMapping("/api/global-chat")
class GlobalChatRestController {

    @Autowired
    private GlobalChatService chatService;

    @Autowired
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get recent messages (for initial load when connecting).
     */
    @GetMapping("/messages")
    public ResponseEntity<List<GlobalChatMessageDto>> getRecentMessages() {
        return ResponseEntity.ok(chatService.getRecentMessages());
    }

    /**
     * Send a new message via REST (broadcasts to WebSocket subscribers).
     */
    @org.springframework.web.bind.annotation.PostMapping("/messages")
    public ResponseEntity<?> sendMessage(
            @org.springframework.web.bind.annotation.RequestBody SendMessageRequest request,
            java.security.Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not found"));
        }

        // Check rate limit
        if (!chatService.canUserSendMessage(user.getId())) {
            int remaining = chatService.getSecondsUntilNextMessage(user.getId());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Rate limited", "secondsRemaining", remaining));
        }

        // Send message
        GlobalChatMessageDto message = chatService.sendMessage(user.getId(), request.getContent());
        if (message == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to send message"));
        }

        // Broadcast to all WebSocket subscribers
        messagingTemplate.convertAndSend("/topic/arena-chat", message);

        return ResponseEntity.ok(message);
    }

    /**
     * Get messages after a specific ID (for reconnection sync).
     */
    @GetMapping("/messages/after")
    public ResponseEntity<List<GlobalChatMessageDto>> getMessagesAfter(@RequestParam Long afterId) {
        return ResponseEntity.ok(chatService.getMessagesAfterId(afterId));
    }

    /**
     * Get rate limit status for current user.
     */
    @GetMapping("/rate-limit")
    public ResponseEntity<Map<String, Object>> getRateLimitStatus(@RequestParam Long userId) {
        Map<String, Object> response = new HashMap<>();
        response.put("canSend", chatService.canUserSendMessage(userId));
        response.put("secondsRemaining", chatService.getSecondsUntilNextMessage(userId));
        return ResponseEntity.ok(response);
    }

    /**
     * Request DTO for sending messages.
     */
    public static class SendMessageRequest {
        private String content;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
