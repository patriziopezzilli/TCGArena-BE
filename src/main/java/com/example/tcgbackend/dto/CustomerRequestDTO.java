package com.example.tcgbackend.dto;

import com.example.tcgbackend.model.CustomerRequest;
import com.example.tcgbackend.model.RequestMessage;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

import java.util.Objects;

/**
 * DTOs for Customer Request operations
 */
public class CustomerRequestDTO {
    
    public static class CreateRequestRequest {
        
        @NotNull(message = "Shop ID is required")
        @JsonProperty("shop_id")
        private Long shopId;
        
        @NotNull(message = "Request type is required")
        private CustomerRequest.RequestType type;
        
        @NotBlank(message = "Title is required")
        @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
        private String title;
        
        @NotBlank(message = "Description is required")
        @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
        private String description;
        
        public CreateRequestRequest() {
        }
        
        public CreateRequestRequest(Long shopId, CustomerRequest.RequestType type, String title, String description) {
            this.shopId = shopId;
            this.type = type;
            this.title = title;
            this.description = description;
        }

        // Getters and setters
        public Long getShopId() {
            return shopId;
        }

        public void setShopId(Long shopId) {
            this.shopId = shopId;
        }

        public CustomerRequest.RequestType getType() {
            return type;
        }

        public void setType(CustomerRequest.RequestType type) {
            this.type = type;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CreateRequestRequest that = (CreateRequestRequest) o;
            return Objects.equals(shopId, that.shopId) &&
                   type == that.type &&
                   Objects.equals(title, that.title) &&
                   Objects.equals(description, that.description);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(shopId, type, title, description);
        }
        
        @Override
        public String toString() {
            return "CreateRequestRequest{" +
                    "shopId=" + shopId +
                    ", type=" + type +
                    ", title='" + title + '\'' +
                    ", description='" + description + '\'' +
                    '}';
        }
    }
    
    public static class UpdateRequestStatusRequest {
        
        @NotNull(message = "Status is required")
        private CustomerRequest.RequestStatus status;
        
        public UpdateRequestStatusRequest() {
        }
        
        public UpdateRequestStatusRequest(CustomerRequest.RequestStatus status) {
            this.status = status;
        }

        // Getters and setters
        public CustomerRequest.RequestStatus getStatus() {
            return status;
        }

        public void setStatus(CustomerRequest.RequestStatus status) {
            this.status = status;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UpdateRequestStatusRequest that = (UpdateRequestStatusRequest) o;
            return status == that.status;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(status);
        }
        
        @Override
        public String toString() {
            return "UpdateRequestStatusRequest{" +
                    "status=" + status +
                    '}';
        }
    }
    
    public static class SendMessageRequest {
        
        @NotBlank(message = "Message is required")
        @Size(min = 1, max = 2000, message = "Message must be between 1 and 2000 characters")
        private String message;
        
        public SendMessageRequest() {
        }
        
        public SendMessageRequest(String message) {
            this.message = message;
        }

        // Getters and setters
        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SendMessageRequest that = (SendMessageRequest) o;
            return Objects.equals(message, that.message);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(message);
        }
        
        @Override
        public String toString() {
            return "SendMessageRequest{" +
                    "message='" + message + '\'' +
                    '}';
        }
    }
    
    public static class RequestListResponse {
        private java.util.List<CustomerRequest> requests;
        private int total;
        private int page;
        private int pageSize;
        
        public RequestListResponse() {
        }

        // Constructor with arguments
        public RequestListResponse(java.util.List<CustomerRequest> requests, int total, int page, int pageSize) {
            this.requests = requests;
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
        }
        
        public java.util.List<CustomerRequest> getRequests() {
            return requests;
        }
        
        public void setRequests(java.util.List<CustomerRequest> requests) {
            this.requests = requests;
        }
        
        public int getTotal() {
            return total;
        }
        
        public void setTotal(int total) {
            this.total = total;
        }
        
        public int getPage() {
            return page;
        }
        
        public void setPage(int page) {
            this.page = page;
        }
        
        public int getPageSize() {
            return pageSize;
        }
        
        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RequestListResponse that = (RequestListResponse) o;
            return total == that.total &&
                   page == that.page &&
                   pageSize == that.pageSize &&
                   Objects.equals(requests, that.requests);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(requests, total, page, pageSize);
        }
        
        @Override
        public String toString() {
            return "RequestListResponse{" +
                    "requests=" + requests +
                    ", total=" + total +
                    ", page=" + page +
                    ", pageSize=" + pageSize +
                    '}';
        }
    }
    
    public static class RequestStatsResponse {
        
        @JsonProperty("pending_count")
        private long pendingCount;
        
        @JsonProperty("accepted_count")
        private long acceptedCount;
        
        @JsonProperty("completed_count")
        private long completedCount;
        
        @JsonProperty("unread_count")
        private long unreadCount;
        
        public RequestStatsResponse() {
        }

        // Constructor with arguments
        public RequestStatsResponse(long pendingCount, long acceptedCount, long completedCount, long unreadCount) {
            this.pendingCount = pendingCount;
            this.acceptedCount = acceptedCount;
            this.completedCount = completedCount;
            this.unreadCount = unreadCount;
        }
        
        public long getPendingCount() {
            return pendingCount;
        }
        
        public void setPendingCount(long pendingCount) {
            this.pendingCount = pendingCount;
        }
        
        public long getAcceptedCount() {
            return acceptedCount;
        }
        
        public void setAcceptedCount(long acceptedCount) {
            this.acceptedCount = acceptedCount;
        }
        
        public long getCompletedCount() {
            return completedCount;
        }
        
        public void setCompletedCount(long completedCount) {
            this.completedCount = completedCount;
        }
        
        public long getUnreadCount() {
            return unreadCount;
        }
        
        public void setUnreadCount(long unreadCount) {
            this.unreadCount = unreadCount;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RequestStatsResponse that = (RequestStatsResponse) o;
            return pendingCount == that.pendingCount &&
                   acceptedCount == that.acceptedCount &&
                   completedCount == that.completedCount &&
                   unreadCount == that.unreadCount;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(pendingCount, acceptedCount, completedCount, unreadCount);
        }
        
        @Override
        public String toString() {
            return "RequestStatsResponse{" +
                    "pendingCount=" + pendingCount +
                    ", acceptedCount=" + acceptedCount +
                    ", completedCount=" + completedCount +
                    ", unreadCount=" + unreadCount +
                    '}';
        }
    }
    
    public static class MessageListResponse {
        private java.util.List<RequestMessage> messages;
        private int total;
        
        public MessageListResponse() {
        }

        // Constructor with arguments
        public MessageListResponse(java.util.List<RequestMessage> messages, int total) {
            this.messages = messages;
            this.total = total;
        }
        
        public java.util.List<RequestMessage> getMessages() {
            return messages;
        }
        
        public void setMessages(java.util.List<RequestMessage> messages) {
            this.messages = messages;
        }
        
        public int getTotal() {
            return total;
        }
        
        public void setTotal(int total) {
            this.total = total;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MessageListResponse that = (MessageListResponse) o;
            return total == that.total &&
                   Objects.equals(messages, that.messages);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(messages, total);
        }
        
        @Override
        public String toString() {
            return "MessageListResponse{" +
                    "messages=" + messages +
                    ", total=" + total +
                    '}';
        }
    }
}
