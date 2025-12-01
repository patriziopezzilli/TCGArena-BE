package com.example.tcgbackend.service;

import com.example.tcgbackend.dto.CustomerRequestDTO.*;
import com.example.tcgbackend.model.CustomerRequest;
import com.example.tcgbackend.model.RequestMessage;
import com.example.tcgbackend.repository.CustomerRequestRepository;
import com.example.tcgbackend.repository.RequestMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerRequestService {
    
    private final CustomerRequestRepository customerRequestRepository;
    private final RequestMessageRepository requestMessageRepository;
    
    /**
     * Create a new customer request
     */
    @Transactional
    public CustomerRequest createRequest(
        String userId,
        CreateRequestRequest request
    ) {
        log.info("Creating request for user: {} to shop: {}", userId, request.getShopId());
        
        CustomerRequest customerRequest = new CustomerRequest();
        customerRequest.setShopId(request.getShopId());
        customerRequest.setUserId(userId);
        customerRequest.setType(request.getType());
        customerRequest.setTitle(request.getTitle());
        customerRequest.setDescription(request.getDescription());
        customerRequest.setStatus(CustomerRequest.RequestStatus.PENDING);
        customerRequest.setHasUnreadMessages(false);
        
        CustomerRequest saved = customerRequestRepository.save(customerRequest);
        
        // Create initial message with the description
        RequestMessage initialMessage = new RequestMessage();
        initialMessage.setRequestId(saved.getId());
        initialMessage.setSenderId(userId);
        initialMessage.setSenderType(RequestMessage.SenderType.USER);
        initialMessage.setMessage(request.getDescription());
        requestMessageRepository.save(initialMessage);
        
        return saved;
    }
    
    /**
     * Get requests with filters
     */
    @Transactional(readOnly = true)
    public RequestListResponse getRequests(
        String shopId,
        String userId,
        CustomerRequest.RequestStatus status,
        CustomerRequest.RequestType type,
        int page,
        int size
    ) {
        log.info("Getting requests - shop: {}, user: {}, status: {}, type: {}", 
                 shopId, userId, status, type);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<CustomerRequest> requestPage = customerRequestRepository.searchRequests(
            shopId,
            userId,
            status,
            type,
            pageable
        );
        
        return new RequestListResponse(
            requestPage.getContent(),
            (int) requestPage.getTotalElements(),
            page,
            size
        );
    }
    
    /**
     * Get single request
     */
    @Transactional(readOnly = true)
    public CustomerRequest getRequest(String requestId) {
        log.info("Getting request: {}", requestId);
        return customerRequestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Request not found: " + requestId));
    }
    
    /**
     * Update request status (merchant only)
     */
    @Transactional
    public CustomerRequest updateRequestStatus(
        String shopId,
        String requestId,
        UpdateRequestStatusRequest request
    ) {
        log.info("Updating request: {} to status: {}", requestId, request.getStatus());
        
        CustomerRequest customerRequest = getRequest(requestId);
        
        // Verify shop ownership
        if (!customerRequest.getShopId().equals(shopId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        customerRequest.setStatus(request.getStatus());
        
        if (request.getStatus() == CustomerRequest.RequestStatus.COMPLETED ||
            request.getStatus() == CustomerRequest.RequestStatus.REJECTED) {
            customerRequest.setResolvedAt(LocalDateTime.now());
        }
        
        return customerRequestRepository.save(customerRequest);
    }
    
    /**
     * Cancel request (user only)
     */
    @Transactional
    public CustomerRequest cancelRequest(
        String userId,
        String requestId
    ) {
        log.info("Cancelling request: {} by user: {}", requestId, userId);
        
        CustomerRequest customerRequest = getRequest(requestId);
        
        // Verify user ownership
        if (!customerRequest.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        // Can only cancel pending or accepted requests
        if (customerRequest.getStatus() != CustomerRequest.RequestStatus.PENDING &&
            customerRequest.getStatus() != CustomerRequest.RequestStatus.ACCEPTED) {
            throw new RuntimeException("Cannot cancel request in status: " + customerRequest.getStatus());
        }
        
        customerRequest.setStatus(CustomerRequest.RequestStatus.CANCELLED);
        customerRequest.setResolvedAt(LocalDateTime.now());
        
        return customerRequestRepository.save(customerRequest);
    }
    
    /**
     * Get messages for a request
     */
    @Transactional(readOnly = true)
    public MessageListResponse getMessages(String requestId) {
        log.info("Getting messages for request: {}", requestId);
        
        // Verify request exists
        getRequest(requestId);
        
        List<RequestMessage> messages = requestMessageRepository.findByRequestIdOrderByCreatedAtAsc(requestId);
        
        return new MessageListResponse(
            messages,
            messages.size()
        );
    }
    
    /**
     * Send a message
     */
    @Transactional
    public RequestMessage sendMessage(
        String requestId,
        String senderId,
        RequestMessage.SenderType senderType,
        SendMessageRequest request
    ) {
        log.info("Sending message to request: {} from: {} ({})", 
                 requestId, senderId, senderType);
        
        CustomerRequest customerRequest = getRequest(requestId);
        
        // Verify authorization
        if (senderType == RequestMessage.SenderType.USER && 
            !customerRequest.getUserId().equals(senderId)) {
            throw new RuntimeException("Unauthorized");
        }
        if (senderType == RequestMessage.SenderType.MERCHANT && 
            !customerRequest.getShopId().equals(senderId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        // Create message
        RequestMessage message = new RequestMessage();
        message.setRequestId(requestId);
        message.setSenderId(senderId);
        message.setSenderType(senderType);
        message.setMessage(request.getMessage());
        
        RequestMessage saved = requestMessageRepository.save(message);
        
        // Mark request as having unread messages for the recipient
        customerRequest.setHasUnreadMessages(true);
        customerRequestRepository.save(customerRequest);
        
        return saved;
    }
    
    /**
     * Mark request as read
     */
    @Transactional
    public void markAsRead(String requestId, String userId) {
        log.info("Marking request: {} as read by: {}", requestId, userId);
        
        CustomerRequest customerRequest = getRequest(requestId);
        
        // Only the recipient can mark as read
        if (!customerRequest.getUserId().equals(userId) && 
            !customerRequest.getShopId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        customerRequest.setHasUnreadMessages(false);
        customerRequestRepository.save(customerRequest);
    }
    
    /**
     * Get request statistics for a shop
     */
    @Transactional(readOnly = true)
    public RequestStatsResponse getRequestStats(String shopId) {
        log.info("Getting request stats for shop: {}", shopId);
        
        long pendingCount = customerRequestRepository.countByShopIdAndStatus(
            shopId, 
            CustomerRequest.RequestStatus.PENDING
        );
        
        long acceptedCount = customerRequestRepository.countByShopIdAndStatus(
            shopId,
            CustomerRequest.RequestStatus.ACCEPTED
        );
        
        long completedCount = customerRequestRepository.countByShopIdAndStatus(
            shopId,
            CustomerRequest.RequestStatus.COMPLETED
        );
        
        long unreadCount = customerRequestRepository.countByShopIdAndHasUnreadMessages(
            shopId,
            true
        );
        
        return new RequestStatsResponse(
            pendingCount,
            acceptedCount,
            completedCount,
            unreadCount
        );
    }
}
