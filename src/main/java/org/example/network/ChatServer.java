package org.example.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.fatboyindustrial.gsonjavatime.Converters;
import org.example.database.DatabaseManager;
import org.example.model.Message;
import org.example.model.User;
import org.example.config.ConfigManager;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servidor de chat que maneja la comunicación entre clientes
 */
public class ChatServer {
    private int port;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private boolean running;
    private Map<Integer, ClientHandler> clients; // userId -> ClientHandler
    private DatabaseManager dbManager;
    private Gson gson;

    /**
     * Constructor para el servidor de chat
     * @param port Puerto en el que se ejecutará el servidor
     */
    public ChatServer(int port) {
        this.port = port;
        this.threadPool = Executors.newCachedThreadPool();
        this.clients = new ConcurrentHashMap<>();
        this.dbManager = DatabaseManager.getInstance();
        this.gson = Converters.registerAll(new GsonBuilder()).create();
    }

    /**
     * Inicia el servidor
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            
            System.out.println("Servidor de chat iniciado en el puerto " + port);
            
            // Aceptar conexiones entrantes
            while (running) {
                Socket clientSocket = serverSocket.accept();
                
                // Manejar la conexión en un hilo separado
                threadPool.execute(() -> handleClientConnection(clientSocket));
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Error en el servidor: " + e.getMessage());
                e.printStackTrace();
            }
        } finally {
            stop();
        }
    }

    /**
     * Detiene el servidor
     */
    public void stop() {
        running = false;
        
        // Cerrar el socket del servidor
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error al cerrar el servidor: " + e.getMessage());
        }
        
        // Cerrar todas las conexiones de clientes
        for (ClientHandler handler : clients.values()) {
            handler.close();
        }
        
        // Apagar el pool de hilos
        threadPool.shutdown();
        
        // Cerrar la conexión a la base de datos
        dbManager.closeConnection();
        
        System.out.println("Servidor de chat detenido");
    }

    /**
     * Maneja una conexión entrante de un cliente
     * @param clientSocket Socket de la conexión con el cliente
     */
    private void handleClientConnection(Socket clientSocket) {
        ClientHandler clientHandler = null;
        
        try {
            // Configurar streams de entrada/salida
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            
            // Leer el ID de usuario para autenticación
            String userIdStr = in.readLine();
            int userId = Integer.parseInt(userIdStr);
            
            // Obtener información del usuario
            User user = dbManager.getUserById(userId);
            
            if (user == null) {
                System.err.println("Usuario no encontrado: " + userId);
                clientSocket.close();
                return;
            }
            
            // Crear y registrar el manejador de cliente
            clientHandler = new ClientHandler(userId, clientSocket, in, out);
            clients.put(userId, clientHandler);
            
            System.out.println("Cliente conectado: " + user.getUsername() + " (ID: " + userId + ")");
            
            // Procesar mensajes entrantes
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                // Convertir el JSON a un objeto Message
                Message message = gson.fromJson(inputLine, Message.class);
                
                // Ya no guardamos el mensaje aquí, se guarda desde el cliente
                // para evitar duplicación en la base de datos
                
                // Añadir el nombre de usuario al mensaje
                message.setSenderUsername(user.getUsername());
                
                // Reenviar el mensaje a todos los clientes conectados que están en el mismo chat
                broadcastMessage(message);
            }
        } catch (IOException | SQLException e) {
            System.err.println("Error al manejar la conexión del cliente: " + e.getMessage());
        } finally {
            // Eliminar el cliente de la lista y cerrar la conexión
            if (clientHandler != null) {
                int userId = clientHandler.getUserId();
                clients.remove(userId);
                clientHandler.close();
                
                try {
                    User user = dbManager.getUserById(userId);
                    System.out.println("Cliente desconectado: " + user.getUsername() + " (ID: " + userId + ")");
                } catch (SQLException e) {
                    System.err.println("Error al obtener información del usuario: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Envía un mensaje a todos los clientes conectados que están en el mismo chat
     * @param message El mensaje a enviar
     */
    private void broadcastMessage(Message message) {
        try {
            // Obtener los participantes del chat
            int chatId = message.getChatId();
            org.example.model.Chat chat = null;
            
            // Buscar el chat y sus participantes
            for (ClientHandler client : clients.values()) {
                int userId = client.getUserId();
                
                // Verificar si el usuario está en el chat
                boolean isParticipant = false;
                try {
                    List<org.example.model.Chat> userChats = dbManager.getUserChats(userId);
                    for (org.example.model.Chat userChat : userChats) {
                        if (userChat.getId() == chatId) {
                            isParticipant = true;
                            chat = userChat;
                            break;
                        }
                    }
                } catch (SQLException e) {
                    System.err.println("Error al obtener los chats del usuario: " + e.getMessage());
                }
                
                // Enviar el mensaje solo a los participantes
                if (isParticipant) {
                    client.sendMessage(message);
                }
            }
        } catch (Exception e) {
            System.err.println("Error al difundir el mensaje: " + e.getMessage());
        }
    }

    /**
     * Manejador para un cliente conectado
     */
    private class ClientHandler {
        private int userId;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        public ClientHandler(int userId, Socket socket, BufferedReader in, PrintWriter out) {
            this.userId = userId;
            this.socket = socket;
            this.in = in;
            this.out = out;
        }

        public int getUserId() {
            return userId;
        }

        /**
         * Envía un mensaje al cliente
         * @param message El mensaje a enviar
         */
        public void sendMessage(Message message) {
            // Convertir el mensaje a JSON
            String json = gson.toJson(message);
            
            // Enviar el mensaje
            out.println(json);
        }

        /**
         * Cierra la conexión con el cliente
         */
        public void close() {
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
                System.err.println("Error al cerrar la conexión del cliente: " + e.getMessage());
            }
        }
    }

    /**
     * Método principal para ejecutar el servidor
     * @param args Argumentos de línea de comandos
     */
    public static void main(String[] args) {
        // Usar ConfigManager para obtener el puerto configurado
        int port = ConfigManager.getInstance().getServerPort();
        
        // Si se proporciona un puerto como argumento, usarlo en lugar del configurado
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
                System.out.println("Usando puerto especificado por línea de comandos: " + port);
            } catch (NumberFormatException e) {
                System.err.println("Puerto inválido, usando el puerto configurado: " + port);
            }
        } else {
            System.out.println("Usando puerto configurado: " + port);
        }
        
        // Iniciar el servidor
        ChatServer server = new ChatServer(port);
        server.start();
    }
} 