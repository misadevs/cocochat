package org.example.network;

import org.example.model.Message;

/**
 * Interfaz para recibir notificaciones de mensajes entrantes
 */
public interface MessageListener {
    /**
     * Método llamado cuando se recibe un mensaje
     * @param message El mensaje recibido
     */
    void onMessageReceived(Message message);
} 