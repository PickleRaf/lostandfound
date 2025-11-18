package com.example.myapplication.models;

public class Message {
    private String senderId;
    private String text;
    private long timestamp;

    public Message() {
        // Needed for Firestore
    }

    public Message(String senderId, String text, long timestamp) {
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getSenderId() { return senderId; }
    public String getText() { return text; }
    public long getTimestamp() { return timestamp; }

    public void setSenderId(String senderId) { this.senderId = senderId; }
    public void setText(String text) { this.text = text; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
