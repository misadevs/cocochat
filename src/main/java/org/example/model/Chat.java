package org.example.model;

import java.util.ArrayList;
import java.util.List;

public class Chat {
    private int id;
    private String name;
    private ChatType type;
    private List<User> participants;
    private int adminId; // Solo para chats grupales
    private List<Message> messages;

    public Chat(int id, String name, ChatType type) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.participants = new ArrayList<>();
        this.messages = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ChatType getType() {
        return type;
    }

    public void setType(ChatType type) {
        this.type = type;
    }

    public List<User> getParticipants() {
        return participants;
    }

    public void setParticipants(List<User> participants) {
        this.participants = participants;
    }

    public void addParticipant(User user) {
        if (!participants.contains(user)) {
            participants.add(user);
        }
    }

    public void removeParticipant(User user) {
        participants.remove(user);
    }

    public int getAdminId() {
        return adminId;
    }

    public void setAdminId(int adminId) {
        this.adminId = adminId;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public void addMessage(Message message) {
        messages.add(message);
    }

    public boolean isGroupChat() {
        return type == ChatType.GROUP;
    }

    public boolean isFriendChat() {
        return type == ChatType.FRIEND;
    }

    public boolean isTemporaryChat() {
        return type == ChatType.TEMPORARY;
    }

    @Override
    public String toString() {
        return name;
    }

    public enum ChatType {
        TEMPORARY, // Chat temporal
        FRIEND,    // Chat con amigo
        GROUP      // Chat grupal
    }
} 