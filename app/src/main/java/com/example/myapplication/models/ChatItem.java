package com.example.myapplication.models;

public class ChatItem {
    private String chatId;
    private String title;
    private String itemId;
    private String userId;
    private String reporterId;
    private long timestamp;

    // New fields for display
    private String otherUserName;
    private String otherUserRole; // "Finder" or "Claimer"

    public ChatItem() {
    }

    public ChatItem(String chatId, String title, String itemId, String userId,
                    String reporterId, long timestamp) {
        this.chatId = chatId;
        this.title = title;
        this.itemId = itemId;
        this.userId = userId;
        this.reporterId = reporterId;
        this.timestamp = timestamp;
    }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getReporterId() { return reporterId; }
    public void setReporterId(String reporterId) { this.reporterId = reporterId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getOtherUserName() { return otherUserName; }
    public void setOtherUserName(String otherUserName) { this.otherUserName = otherUserName; }

    public String getOtherUserRole() { return otherUserRole; }
    public void setOtherUserRole(String otherUserRole) { this.otherUserRole = otherUserRole; }

    // Helper method to get display text
    public String getDisplayTitle() {
        if (otherUserName != null && otherUserRole != null) {
            return otherUserName + " - " + otherUserRole + " - " + title;
        }
        return title;
    }
}