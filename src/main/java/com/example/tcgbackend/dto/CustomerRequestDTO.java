package com.example.tcgbackend.dto;

import com.example.tcgbackend.model.CustomerRequest;
import com.example.tcgbackend.model.RequestMessage;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTOs for Customer Request operations
 */
public class CustomerRequestDTO {
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequestRequest {
        
        @NotBlank(message = "Shop ID is required")
        @JsonProperty("shop_id")
        private String shopId;
        
        @NotNull(message = "Request type is required")
        private CustomerRequest.RequestType type;
        
        @NotBlank(message = "Title is required")
        @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
        private String title;
        
        @NotBlank(message = "Description is required")
        @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
        private String description;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequestStatusRequest {
        
        @NotNull(message = "Status is required")
        private CustomerRequest.RequestStatus status;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendMessageRequest {
        
        @NotBlank(message = "Message is required")
        @Size(min = 1, max = 2000, message = "Message must be between 1 and 2000 characters")
        private String message;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestListResponse {
        private java.util.List<CustomerRequest> requests;
        private int total;
        private int page;
        private int pageSize;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestStatsResponse {
        
        @JsonProperty("pending_count")
        private long pendingCount;
        
        @JsonProperty("accepted_count")
        private long acceptedCount;
        
        @JsonProperty("completed_count")
        private long completedCount;
        
        @JsonProperty("unread_count")
        private long unreadCount;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageListResponse {
        private java.util.List<RequestMessage> messages;
        private int total;
    }
}
