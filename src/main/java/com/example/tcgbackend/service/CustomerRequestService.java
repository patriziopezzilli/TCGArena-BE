package com.example.tcgbackend.service;

import com.example.tcgbackend.dto.CustomerRequestDTO.*;
import com.example.tcgbackend.model.CustomerRequest;
import com.example.tcgbackend.model.RequestMessage;
import com.example.tcgbackend.repository.CustomerRequestRepository;
import com.example.tcgbackend.repository.RequestMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CustomerRequestService {
    
    private static final Logger log = LoggerFactory.getLogger(CustomerRequestService.class);
    
    private final CustomerRequestRepository customerRequestRepository;
    private final RequestMessageRepository requestMessageRepository;
    
    public CustomerRequestService(
        CustomerRequestRepository customerRequestRepository,
        RequestMessageRepository requestMessageRepository
    ) {
        this.customerRequestRepository = customerRequestRepository;
        this.requestMessageRepository = requestMessageRepository;
    }
    
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
        customerRequest.setUserId(Long.valueOf(userId));
        customerRequest.setType(request.getType());
        customerRequest.setTitle(request.getTitle());
        customerRequest.setDescription(request.getDescription());
        customerRequest.setStatus(CustomerRequest.RequestStatus.PENDING);
        customerRequest.setHasUnreadMessages(false);
        
        CustomerRequest saved = customerRequestRepository.save(customerRequest);
        
        // Create initial message with the description
        RequestMessage initialMessage = new RequestMessage();
        initialMessage.setRequestId(saved.getId());
        initialMessage.setSenderId(Long.valueOf(userId));
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
            shopId != null ? Long.valueOf(shopId) : null,
            userId != null ? Long.valueOf(userId) : null,
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
        if (!customerRequest.getShopId().equals(Long.valueOf(shopId))) {
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
        if (!customerRequest.getUserId().equals(Long.valueOf(userId))) {
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
        
        Long senderIdLong = Long.valueOf(senderId);
        
        // Verify authorization
        if (senderType == RequestMessage.SenderType.USER && 
            !customerRequest.getUserId().equals(senderIdLong)) {
            throw new RuntimeException("Unauthorized");
        }
        if (senderType == RequestMessage.SenderType.MERCHANT && 
            !customerRequest.getShopId().equals(senderIdLong)) {
            throw new RuntimeException("Unauthorized");
        }
        
        // Create message
        RequestMessage message = new RequestMessage();
        message.setRequestId(requestId);
        message.setSenderId(senderIdLong);
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
        
        Long userIdLong = Long.valueOf(userId);
        
        // Only the recipient can mark as read
        if (!customerRequest.getUserId().equals(userIdLong) && 
            !customerRequest.getShopId().equals(userIdLong)) {
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
        
        Long shopIdLong = Long.valueOf(shopId);
        
        long pendingCount = customerRequestRepository.countByShopIdAndStatus(
            shopIdLong, 
            CustomerRequest.RequestStatus.PENDING
        );
        
        long acceptedCount = customerRequestRepository.countByShopIdAndStatus(
            shopIdLong,
            CustomerRequest.RequestStatus.ACCEPTED
        );
        
        long completedCount = customerRequestRepository.countByShopIdAndStatus(
            shopIdLong,
            CustomerRequest.RequestStatus.COMPLETED
        );
        
        long unreadCount = customerRequestRepository.countByShopIdAndHasUnreadMessages(
            shopIdLong,
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
