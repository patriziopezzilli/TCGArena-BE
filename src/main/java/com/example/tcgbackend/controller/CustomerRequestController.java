package com.example.tcgbackend.controller;

import com.example.tcgbackend.dto.CustomerRequestDTO.*;
import com.example.tcgbackend.model.CustomerRequest;
import com.example.tcgbackend.model.RequestMessage;
import com.example.tcgbackend.service.CustomerRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CustomerRequestController {
    
    private final CustomerRequestService customerRequestService;
    
    /**
     * Create a new request (Player)
     * POST /api/requests
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CustomerRequest> createRequest(
        Authentication authentication,
        @Valid @RequestBody CreateRequestRequest request
    ) {
        String userId = authentication.getName();
        log.info("POST /api/requests - user: {}, shop: {}", userId, request.getShopId());
        
        CustomerRequest created = customerRequestService.createRequest(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    /**
     * Get requests with filters
     * GET /api/requests?shopId={shopId}&userId={userId}&status={status}&type={type}
     */
    @GetMapping
    public ResponseEntity<RequestListResponse> getRequests(
        @RequestParam(required = false) String shopId,
        @RequestParam(required = false) String userId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String type,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        log.info("GET /api/requests - shopId: {}, userId: {}, status: {}, type: {}", 
                 shopId, userId, status, type);
        
        CustomerRequest.RequestStatus requestStatus = status != null ? 
            CustomerRequest.RequestStatus.valueOf(status) : null;
        CustomerRequest.RequestType requestType = type != null ?
            CustomerRequest.RequestType.valueOf(type) : null;
        
        RequestListResponse response = customerRequestService.getRequests(
            shopId,
            userId,
            requestStatus,
            requestType,
            page,
            size
        );
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get single request
     * GET /api/requests/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<CustomerRequest> getRequest(@PathVariable String id) {
        log.info("GET /api/requests/{}", id);
        
        CustomerRequest request = customerRequestService.getRequest(id);
        return ResponseEntity.ok(request);
    }
    
    /**
     * Update request status (Merchant)
     * PUT /api/requests/{id}/status
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<CustomerRequest> updateRequestStatus(
        @PathVariable String id,
        @RequestParam String shopId,
        @Valid @RequestBody UpdateRequestStatusRequest request
    ) {
        log.info("PUT /api/requests/{}/status - shopId: {}, status: {}", 
                 id, shopId, request.getStatus());
        
        CustomerRequest updated = customerRequestService.updateRequestStatus(shopId, id, request);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * Cancel request (Player)
     * POST /api/requests/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CustomerRequest> cancelRequest(
        Authentication authentication,
        @PathVariable String id
    ) {
        String userId = authentication.getName();
        log.info("POST /api/requests/{}/cancel - user: {}", id, userId);
        
        CustomerRequest cancelled = customerRequestService.cancelRequest(userId, id);
        return ResponseEntity.ok(cancelled);
    }
    
    /**
     * Get messages for a request
     * GET /api/requests/{id}/messages
     */
    @GetMapping("/{id}/messages")
    public ResponseEntity<MessageListResponse> getMessages(@PathVariable String id) {
        log.info("GET /api/requests/{}/messages", id);
        
        MessageListResponse response = customerRequestService.getMessages(id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Send a message (Player)
     * POST /api/requests/{id}/messages
     */
    @PostMapping("/{id}/messages")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<RequestMessage> sendMessageAsUser(
        Authentication authentication,
        @PathVariable String id,
        @Valid @RequestBody SendMessageRequest request
    ) {
        String userId = authentication.getName();
        log.info("POST /api/requests/{}/messages - user: {}", id, userId);
        
        RequestMessage message = customerRequestService.sendMessage(
            id,
            userId,
            RequestMessage.SenderType.USER,
            request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }
    
    /**
     * Send a message (Merchant)
     * POST /api/requests/{id}/messages/merchant
     */
    @PostMapping("/{id}/messages/merchant")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<RequestMessage> sendMessageAsMerchant(
        @PathVariable String id,
        @RequestParam String shopId,
        @Valid @RequestBody SendMessageRequest request
    ) {
        log.info("POST /api/requests/{}/messages/merchant - shopId: {}", id, shopId);
        
        RequestMessage message = customerRequestService.sendMessage(
            id,
            shopId,
            RequestMessage.SenderType.MERCHANT,
            request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }
    
    /**
     * Mark request as read
     * POST /api/requests/{id}/read
     */
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
        Authentication authentication,
        @PathVariable String id
    ) {
        String userId = authentication.getName();
        log.info("POST /api/requests/{}/read - user: {}", id, userId);
        
        customerRequestService.markAsRead(id, userId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Get request statistics (Merchant)
     * GET /api/requests/stats?shopId={shopId}
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<RequestStatsResponse> getRequestStats(
        @RequestParam String shopId
    ) {
        log.info("GET /api/requests/stats - shopId: {}", shopId);
        
        RequestStatsResponse stats = customerRequestService.getRequestStats(shopId);
        return ResponseEntity.ok(stats);
    }
}
