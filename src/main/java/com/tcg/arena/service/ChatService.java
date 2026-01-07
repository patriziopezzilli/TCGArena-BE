package com.tcg.arena.service;

import com.tcg.arena.dto.*;
import com.tcg.arena.model.*;
import com.tcg.arena.repository.ChatConversationRepository;
import com.tcg.arena.repository.ChatMessageRepository;
import com.tcg.arena.repository.PendingReviewRepository;
import com.tcg.arena.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChatService {

    @Autowired
    private ChatConversationRepository conversationRepository;

    @Autowired
    private ChatMessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    // Quick helper to reuse Radar DTO mappings if needed, or implement simple
    // mapping
    @Autowired
    private RadarService radarService;

    @Autowired
    private PendingReviewRepository pendingReviewRepository;

    @Transactional(readOnly = true)
    public List<ChatConversationDto> getUserConversations(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        return conversationRepository.findByUser(user).stream()
                .map(c -> convertToDto(c, userId))
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatConversationDto startOrGetConversation(Long currentUserId, CreateChatRequest request) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        User targetUser = userRepository.findById(request.getTargetUserId())
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        ChatConversation.ChatType type = ChatConversation.ChatType.valueOf(request.getType());

        // Check if existing FREE conversation exists
        if (type == ChatConversation.ChatType.FREE) {
            Optional<ChatConversation> existing = conversationRepository.findFreeConversation(currentUser, targetUser);
            if (existing.isPresent()) {
                return convertToDto(existing.get(), currentUserId);
            }
        }

        // Create new
        ChatConversation conversation = new ChatConversation();
        conversation.setType(type);
        conversation.setLastMessageAt(LocalDateTime.now());
        conversation.setContextJson(request.getContextJson());
        conversation.getParticipants().add(currentUser);
        conversation.getParticipants().add(targetUser);
        conversation.setInitiatorId(currentUserId); // Set initiator

        conversation = conversationRepository.save(conversation);
        return convertToDto(conversation, currentUserId);
    }

    @Transactional
    public ChatMessageDto sendMessage(Long userId, Long conversationId, String content) {
        User sender = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        ChatConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        // Validate participation
        if (!conversation.getParticipants().contains(sender)) {
            throw new RuntimeException("User is not a participant");
        }

        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());
        message.setRead(false);

        message = messageRepository.save(message);

        // Update conversation timestamp
        conversation.setLastMessageAt(message.getTimestamp());
        conversationRepository.save(conversation);

        // Send push notification to recipient
        User recipient = conversation.getParticipants().stream()
                .filter(u -> !u.getId().equals(userId))
                .findFirst()
                .orElse(null);

        if (recipient != null) {
            String title = "Nuovo messaggio da " + sender.getDisplayName().toLowerCase();
            String preview = content.length() > 50 ? content.substring(0, 47) + "..." : content;
            try {
                notificationService.sendChatNotification(recipient.getId(), title, preview, conversationId);
            } catch (Exception e) {
                // Log but don't fail the message send
                System.err.println("Failed to send push notification: " + e.getMessage());
            }
        }

        return convertToMessageDto(message);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getMessages(Long userId, Long conversationId) {
        ChatConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        if (!conversation.getParticipants().contains(user)) {
            throw new RuntimeException("User is not a participant");
        }

        return messageRepository.findByConversationIdOrderByTimestampAsc(conversationId).stream()
                .map(this::convertToMessageDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markConversationAsRead(Long userId, Long conversationId) {
        ChatConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        if (!conversation.getParticipants().contains(user)) {
            throw new RuntimeException("User is not a participant");
        }

        List<ChatMessage> unreadMessages = messageRepository.findUnreadByConversationAndRecipient(conversationId,
                userId);
        for (ChatMessage msg : unreadMessages) {
            msg.setRead(true);
        }
        messageRepository.saveAll(unreadMessages);
    }

    private ChatConversationDto convertToDto(ChatConversation conversation, Long currentUserId) {
        ChatConversationDto dto = new ChatConversationDto();
        dto.setId(conversation.getId());
        dto.setLastMessageAt(conversation.getLastMessageAt());
        dto.setType(conversation.getType().name());
        dto.setContextJson(conversation.getContextJson());
        dto.setStatus(conversation.getStatus() != null ? conversation.getStatus().name() : "ACTIVE");
        dto.setIsReadOnly(conversation.getIsReadOnly() != null ? conversation.getIsReadOnly() : false);
        dto.setAgreementReached(conversation.getAgreementReached()); // Can be null for active trades
        dto.setInitiatorId(conversation.getInitiatorId());

        // Set unread count for current user
        Long unreadCount = messageRepository.countUnreadByConversationAndRecipient(conversation.getId(), currentUserId);
        dto.setUnreadCount(unreadCount != null ? unreadCount.intValue() : 0);

        // Set last message preview
        messageRepository.findTopByConversationIdOrderByTimestampDesc(conversation.getId())
                .ifPresent(lastMsg -> dto.setLastMessagePreview(lastMsg.getContent()));

        // Map participants to simple RadarUserDto style (reusing DTO logic or
        // simplifying)
        // Here we do custom mapping to avoid circular deps or complex service calls if
        // simple
        dto.setParticipants(conversation.getParticipants().stream()
                .map(u -> new RadarUserDto(u.getId(), u.getUsername(), u.getDisplayName(),
                        u.getLocation() != null ? u.getLocation().getLatitude() : null,
                        u.getLocation() != null ? u.getLocation().getLongitude() : null,
                        !u.getFavoriteTCGTypes().isEmpty() ? u.getFavoriteTCGTypes().get(0) : null,
                        u.getProfileImageUrl(),
                        true)) // Online status mock or fetch
                .collect(Collectors.toList()));

        // Quick preview
        return dto;
    }

    private ChatMessageDto convertToMessageDto(ChatMessage message) {
        ChatMessageDto dto = new ChatMessageDto();
        dto.setId(message.getId());
        dto.setConversationId(message.getConversation().getId());
        dto.setSenderId(message.getSender().getId());
        dto.setContent(message.getContent());
        dto.setTimestamp(message.getTimestamp());
        dto.setRead(message.isRead());
        return dto;
    }

    @Transactional
    public ChatConversationDto completeTrade(Long userId, Long conversationId, int pointsToAssign) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        ChatConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        // Validate participation
        if (!conversation.getParticipants().contains(user)) {
            throw new RuntimeException("User is not a participant");
        }

        // Validate it's a trade conversation
        if (conversation.getType() != ChatConversation.ChatType.TRADE) {
            throw new RuntimeException("Can only complete trade conversations");
        }

        // Mark as completed and readonly
        conversation.setStatus(ChatConversation.ChatStatus.COMPLETED);
        conversation.setIsReadOnly(true);
        conversation.setAgreementReached(true); // Agreement was reached successfully
        conversationRepository.save(conversation);

        // Award points and rating to the other participant
        User otherUser = conversation.getParticipants().stream()
                .filter(u -> !u.getId().equals(userId))
                .findFirst()
                .orElse(null);

        if (otherUser != null && pointsToAssign > 0) {
            // Award points
            otherUser.setPoints(otherUser.getPoints() + pointsToAssign);

            // Add trade rating (points are used as rating 1-5)
            otherUser.addTradeRating(pointsToAssign);

            userRepository.save(otherUser);
            System.out.println(
                    "🎯 ChatService: Awarded " + pointsToAssign + " points and rating to user " + otherUser.getId());
            System.out.println("📊 ChatService: User " + otherUser.getId() + " now has trade rating: "
                    + otherUser.getTradeRating());

            // Create pending review for the OTHER user to review the COMPLETER
            // The other user needs to leave a review for the user who completed the trade
            PendingReview pendingReview = new PendingReview(
                    conversationId,
                    otherUser, // reviewer (other user)
                    user, // reviewee (user who completed)
                    conversation.getContextJson() // trade context
            );
            pendingReviewRepository.save(pendingReview);
            System.out.println("📝 ChatService: Created pending review for user " + otherUser.getId()
                    + " to review user " + userId);

            // Send notification to the other user
            notificationService.sendPushNotification(
                    otherUser.getId(),
                    "Trattativa completata!",
                    "Lascia una recensione per " + user.getDisplayName().toLowerCase());
        }

        return convertToDto(conversation, userId);
    }

    @Transactional
    public ChatConversationDto closeWithoutAgreement(Long userId, Long conversationId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        ChatConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        // Validate participation
        if (!conversation.getParticipants().contains(user)) {
            throw new RuntimeException("User is not a participant");
        }

        // Validate it's a trade conversation
        if (conversation.getType() != ChatConversation.ChatType.TRADE) {
            throw new RuntimeException("Can only close trade conversations");
        }

        // Mark as completed and readonly WITHOUT awarding points
        conversation.setStatus(ChatConversation.ChatStatus.COMPLETED);
        conversation.setIsReadOnly(true);
        conversation.setAgreementReached(false); // No agreement was reached
        conversationRepository.save(conversation);

        System.out.println("❌ ChatService: Trade closed without agreement for conversation " + conversationId);

        return convertToDto(conversation, userId);
    }
}
