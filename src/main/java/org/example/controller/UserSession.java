package org.example.controller;

import org.example.model.User;
import org.example.network.ChatClient;

/**
 * Clase Singleton para mantener los datos de la sesión del usuario actual
 */
public class UserSession {
    private static UserSession instance;
    private User currentUser;
    private ChatClient chatClient;

    private UserSession() {
        // Constructor privado (singleton)
    }

    public static synchronized UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }
    
    public ChatClient getChatClient() {
        return chatClient;
    }
    
    public void setChatClient(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public void clearSession() {
        currentUser = null;
        
        // Cerrar la conexión del cliente de chat
        if (chatClient != null) {
            try {
                chatClient.stop();
            } catch (Exception e) {
                System.err.println("Error al cerrar la conexión del cliente: " + e.getMessage());
            }
            chatClient = null;
        }
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }
} 