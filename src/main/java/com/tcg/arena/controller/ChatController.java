package com.tcg.arena.controller;

import com.tcg.arena.dto.*;
import com.tcg.arena.service.ChatService;
import com.tcg.arena.security.JwtTokenUtil;
import com.tcg.arena.repository.UserRepository;
import com.tcg.arena.model.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserRepository userRepository;

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        String username = jwtTokenUtil.getUsernameFromToken(token);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found from token"));
        return user.getId();
    }

    @GetMapping
    public ResponseEntity<List<ChatConversationDto>> getConversations(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return ResponseEntity.ok(chatService.getUserConversations(userId));
    }

    @PostMapping("/start")
    public ResponseEntity<ChatConversationDto> startConversation(
            @RequestBody CreateChatRequest createRequest,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return ResponseEntity.ok(chatService.startOrGetConversation(userId, createRequest));
    }

    @PostMapping("/{conversationId}/send")
    public ResponseEntity<ChatMessageDto> sendMessage(
            @PathVariable Long conversationId,
            @RequestBody SendMessageRequest sendRequest,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return ResponseEntity.ok(chatService.sendMessage(userId, conversationId, sendRequest.getContent()));
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<ChatMessageDto>> getMessages(
            @PathVariable Long conversationId,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return ResponseEntity.ok(chatService.getMessages(userId, conversationId));
    }

    @PostMapping("/{conversationId}/complete")
    public ResponseEntity<ChatConversationDto> completeTrade(
            @PathVariable Long conversationId,
            @RequestBody CompleteTradeRequest completeRequest,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        int points = completeRequest.getPoints() != null ? completeRequest.getPoints() : 0;
        return ResponseEntity.ok(chatService.completeTrade(userId, conversationId, points));
    }
}
