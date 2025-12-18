package com.tcg.arena.dto;

import java.time.LocalDateTime;
import java.util.List;

public class MerchantNotificationsDTO {

    private List<NotificationItem> items;
    private int totalCount;

    public MerchantNotificationsDTO(List<NotificationItem> items) {
        this.items = items;
        this.totalCount = items.size();
    }

    public List<NotificationItem> getItems() {
        return items;
    }

    public void setItems(List<NotificationItem> items) {
        this.items = items;
        this.totalCount = items.size();
    }

    public int getTotalCount() {
        return totalCount;
    }

    public static class NotificationItem {
        private String id;
        private NotificationType type;
        private String title;
        private String message;
        private String link;
        private LocalDateTime timestamp;
        private boolean urgent;

        public NotificationItem(String id, NotificationType type, String title, String message, String link,
                LocalDateTime timestamp, boolean urgent) {
            this.id = id;
            this.type = type;
            this.title = title;
            this.message = message;
            this.link = link;
            this.timestamp = timestamp;
            this.urgent = urgent;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public NotificationType getType() {
            return type;
        }

        public void setType(NotificationType type) {
            this.type = type;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }

        public boolean isUrgent() {
            return urgent;
        }

        public void setUrgent(boolean urgent) {
            this.urgent = urgent;
        }
    }

    public enum NotificationType {
        TOURNAMENT_TODAY,
        TOURNAMENT_UPCOMING,
        PENDING_REQUEST,
        ACTIVE_RESERVATION,
        UNREAD_MESSAGE
    }
}
