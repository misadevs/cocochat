package org.example.model;

import java.time.LocalDateTime;

public class FriendRequest {
    private int id;
    private int senderId;
    private int receiverId;
    private LocalDateTime timestamp;
    private RequestStatus status;
    private String senderUsername;
    private String receiverUsername;

    public FriendRequest(int id, int senderId, int receiverId, LocalDateTime timestamp, RequestStatus status) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.timestamp = timestamp;
        this.status = status;
    }

    // Constructor para crear nuevas solicitudes
    public FriendRequest(int senderId, int receiverId) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.timestamp = LocalDateTime.now();
        this.status = RequestStatus.PENDING;
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

    public int getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(int receiverId) {
        this.receiverId = receiverId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getReceiverUsername() {
        return receiverUsername;
    }

    public void setReceiverUsername(String receiverUsername) {
        this.receiverUsername = receiverUsername;
    }

    public enum RequestStatus {
        PENDING,
        ACCEPTED,
        REJECTED
    }
} 