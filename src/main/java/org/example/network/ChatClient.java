package org.example.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.fatboyindustrial.gsonjavatime.Converters;
import org.example.model.Message;

import java.io.*;
import java.net.Socket;

/**
 * Cliente de chat que maneja la comunicación con el servidor
 */
public class ChatClient {
    private String host;
    private int port;
    private int userId;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private MessageListener listener;
    private Thread receiveThread;
    private boolean running;
    private Gson gson;

    /**
     * Constructor para el cliente de chat
     * @param host Dirección del servidor
     * @param port Puerto del servidor
     * @param userId ID del usuario actual
     * @param listener Listener para recibir mensajes
     */
    public ChatClient(String host, int port, int userId, MessageListener listener) {
        this.host = host;
        this.port = port;
        this.userId = userId;
        this.listener = listener;
        // Usar GsonBuilder con adaptadores para LocalDateTime
        this.gson = Converters.registerAll(new GsonBuilder()).create();
    }

    /**
     * Inicia la conexión con el servidor
     * @throws IOException Si hay un error de conexión
     */
    public void start() throws IOException {
        // Conectar al servidor
        socket = new Socket(host, port);
        
        // Configurar los streams de entrada/salida
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
        // Enviar el ID de usuario para autenticación
        out.println(userId);
        
        // Iniciar el hilo para recibir mensajes
        running = true;
        receiveThread = new Thread(this::receiveMessages);
        receiveThread.setDaemon(true);
        receiveThread.start();
    }

    /**
     * Detiene la conexión con el servidor
     */
    public void stop() {
        running = false;
        
        try {
            if (out != null) {
                out.close();
            }
            
            if (in != null) {
                in.close();
            }
            
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        if (receiveThread != null) {
            receiveThread.interrupt();
        }
    }

    /**
     * Envía un mensaje al servidor
     * @param message El mensaje a enviar
     * @throws IOException Si hay un error al enviar el mensaje
     */
    public void sendMessage(Message message) throws IOException {
        if (out != null) {
            // Convertir el mensaje a JSON
            String json = gson.toJson(message);
            
            // Enviar el mensaje
            out.println(json);
        }
    }

    /**
     * Hilo para recibir mensajes del servidor
     */
    private void receiveMessages() {
        try {
            String inputLine;
            
            // Leer mensajes continuamente mientras el cliente esté ejecutándose
            while (running && (inputLine = in.readLine()) != null) {
                // Convertir el JSON a un objeto Message
                Message message = gson.fromJson(inputLine, Message.class);
                
                // Notificar al listener
                if (listener != null) {
                    listener.onMessageReceived(message);
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Error al recibir mensajes: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
} 