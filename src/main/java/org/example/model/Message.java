package org.example.model;

import java.time.LocalDateTime;

public class Message {
    private int id;
    private int senderId;
    private int chatId;
    private String content;
    private LocalDateTime timestamp;
    private String senderUsername;

    public Message(int id, int senderId, int chatId, String content, LocalDateTime timestamp) {
        this.id = id;
        this.senderId = senderId;
        this.chatId = chatId;
        this.content = content;
        this.timestamp = timestamp;
    }

    // Constructor para crear mensajes nuevos
    public Message(int senderId, int chatId, String content) {
        this.senderId = senderId;
        this.chatId = chatId;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public int getChatId() {
        return chatId;
    }

    public void setChatId(int chatId) {
        this.chatId = chatId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    @Override
    public String toString() {
        return content;
    }
} 