package org.example.database;

import org.example.config.ConfigManager;
import org.example.model.*;
import org.example.model.FriendRequest.RequestStatus;
import org.example.model.Chat.ChatType;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static DatabaseManager instance;
    private Connection connection;
    
    // ConfigManager para obtener los parámetros de configuración
    private ConfigManager configManager;

    private DatabaseManager() {
        try {
            // Obtener configuración
            configManager = ConfigManager.getInstance();
            
            // Establecer conexión con los parámetros de configuración
            connection = DriverManager.getConnection(
                configManager.getDbUrl(), 
                configManager.getDbUser(), 
                configManager.getDbPassword()
            );
            
            // Crear tablas si no existen
            createTables();
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error al conectar a la base de datos: " + e.getMessage());
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void createTables() throws SQLException {
        Statement stmt = connection.createStatement();

        // Tabla de usuarios
        stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                "id SERIAL PRIMARY KEY, " +
                "username VARCHAR(50) NOT NULL UNIQUE, " +
                "password VARCHAR(100) NOT NULL)");
                
        // Crear usuario del sistema si no existe
        String checkSystemUser = "SELECT COUNT(*) FROM users WHERE id = 0";
        ResultSet rs = stmt.executeQuery(checkSystemUser);
        rs.next();
        int count = rs.getInt(1);
        
        if (count == 0) {
            // Insertar el usuario del sistema usando raw SQL para establecer ID específico
            stmt.execute("INSERT INTO users (id, username, password) " +
                    "VALUES (0, 'Sistema', 'system-password-not-for-login') " +
                    "ON CONFLICT (id) DO NOTHING");
            
            // Resetear la secuencia del id para que siga desde 1
            stmt.execute("SELECT setval('users_id_seq', (SELECT MAX(id) FROM users), true)");
        }

        // Tabla de amigos
        stmt.execute("CREATE TABLE IF NOT EXISTS friends (" +
                "user_id INT NOT NULL, " +
                "friend_id INT NOT NULL, " +
                "PRIMARY KEY (user_id, friend_id), " +
                "FOREIGN KEY (user_id) REFERENCES users(id), " +
                "FOREIGN KEY (friend_id) REFERENCES users(id))");

        // Crear tipo ENUM para tipos de chat (PostgreSQL usa tipos personalizados)
        try {
            stmt.execute("CREATE TYPE chat_type AS ENUM ('TEMPORARY', 'FRIEND', 'GROUP')");
        } catch (SQLException e) {
            // El tipo ya existe, ignorar error
            if (!e.getMessage().contains("already exists")) {
                throw e;
            }
        }

        // Tabla de chats
        stmt.execute("CREATE TABLE IF NOT EXISTS chats (" +
                "id SERIAL PRIMARY KEY, " +
                "name VARCHAR(100) NOT NULL, " +
                "type chat_type NOT NULL, " +
                "admin_id INT, " +
                "FOREIGN KEY (admin_id) REFERENCES users(id))");

        // Tabla de participantes de chat con nombres personalizados
        stmt.execute("CREATE TABLE IF NOT EXISTS chat_participants (" +
                "chat_id INT NOT NULL, " +
                "user_id INT NOT NULL, " +
                "display_name VARCHAR(100), " +  // Nombre personalizado del chat para el usuario
                "PRIMARY KEY (chat_id, user_id), " +
                "FOREIGN KEY (chat_id) REFERENCES chats(id), " +
                "FOREIGN KEY (user_id) REFERENCES users(id))");

        // Tabla de mensajes
        stmt.execute("CREATE TABLE IF NOT EXISTS messages (" +
                "id SERIAL PRIMARY KEY, " +
                "chat_id INT NOT NULL, " +
                "sender_id INT NOT NULL, " +
                "content TEXT NOT NULL, " +
                "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (chat_id) REFERENCES chats(id), " +
                "FOREIGN KEY (sender_id) REFERENCES users(id))");

        // Crear tipo ENUM para estados de solicitud
        try {
            stmt.execute("CREATE TYPE request_status AS ENUM ('PENDING', 'ACCEPTED', 'REJECTED')");
        } catch (SQLException e) {
            // El tipo ya existe, ignorar error
            if (!e.getMessage().contains("already exists")) {
                throw e;
            }
        }

        // Tabla de solicitudes de amistad
        stmt.execute("CREATE TABLE IF NOT EXISTS friend_requests (" +
                "id SERIAL PRIMARY KEY, " +
                "sender_id INT NOT NULL, " +
                "receiver_id INT NOT NULL, " +
                "status request_status NOT NULL DEFAULT 'PENDING', " +
                "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (sender_id) REFERENCES users(id), " +
                "FOREIGN KEY (receiver_id) REFERENCES users(id))");

        // Tabla de solicitudes de grupo
        stmt.execute("CREATE TABLE IF NOT EXISTS group_requests (" +
                "id SERIAL PRIMARY KEY, " +
                "chat_id INT NOT NULL, " +
                "sender_id INT NOT NULL, " +
                "receiver_id INT NOT NULL, " +
                "status request_status NOT NULL DEFAULT 'PENDING', " +
                "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (chat_id) REFERENCES chats(id), " +
                "FOREIGN KEY (sender_id) REFERENCES users(id), " +
                "FOREIGN KEY (receiver_id) REFERENCES users(id))");

        stmt.close();
    }

    // Métodos para usuarios
    public User authenticateUser(String username, String password) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM users WHERE username = ? AND password = ?");
        stmt.setString(1, username);
        stmt.setString(2, password);
        ResultSet rs = stmt.executeQuery();

        User user = null;
        if (rs.next()) {
            user = new User(rs.getInt("id"), rs.getString("username"), rs.getString("password"));
            loadUserFriends(user);
        }

        rs.close();
        stmt.close();
        return user;
    }

    public boolean userExists(String username) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) FROM users WHERE username = ?");
        stmt.setString(1, username);
        ResultSet rs = stmt.executeQuery();
        rs.next();
        int count = rs.getInt(1);
        rs.close();
        stmt.close();
        return count > 0;
    }

    public User getUserById(int userId) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM users WHERE id = ?");
        stmt.setInt(1, userId);
        ResultSet rs = stmt.executeQuery();

        User user = null;
        if (rs.next()) {
            user = new User(rs.getInt("id"), rs.getString("username"));
        }

        rs.close();
        stmt.close();
        return user;
    }

    public User getUserByUsername(String username) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM users WHERE username = ?");
        stmt.setString(1, username);
        ResultSet rs = stmt.executeQuery();

        User user = null;
        if (rs.next()) {
            user = new User(rs.getInt("id"), rs.getString("username"));
        }

        rs.close();
        stmt.close();
        return user;
    }

    public List<User> getAllUsers() throws SQLException {
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM users");

        List<User> users = new ArrayList<>();
        while (rs.next()) {
            users.add(new User(rs.getInt("id"), rs.getString("username")));
        }

        rs.close();
        stmt.close();
        return users;
    }

    public boolean registerUser(User user) throws SQLException {
        // Verificar y corregir la secuencia si es necesario
        Statement checkStmt = connection.createStatement();
        ResultSet maxIdRs = checkStmt.executeQuery("SELECT MAX(id) FROM users");
        if (maxIdRs.next()) {
            int maxId = maxIdRs.getInt(1);
            checkStmt.execute("SELECT setval('users_id_seq', " + maxId + ", true)");
        }
        maxIdRs.close();
        checkStmt.close();
        
        // Continuar con el registro normalmente
        PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO users (username, password) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, user.getUsername());
        stmt.setString(2, user.getPassword());
        
        int affectedRows = stmt.executeUpdate();
        
        if (affectedRows > 0) {
            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                user.setId(generatedKeys.getInt(1));
            }
            generatedKeys.close();
        }
        
        stmt.close();
        return affectedRows > 0;
    }

    private void loadUserFriends(User user) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "SELECT u.* FROM users u " +
                "JOIN friends f ON u.id = f.friend_id " +
                "WHERE f.user_id = ?");
        stmt.setInt(1, user.getId());
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            User friend = new User(rs.getInt("id"), rs.getString("username"));
            user.addFriend(friend);
        }

        rs.close();
        stmt.close();
    }

    // Métodos para chats
    public Chat createChat(String name, ChatType type, int adminId) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO chats (name, type, admin_id) VALUES (?, ?::chat_type, ?)",
                Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, name);
        stmt.setString(2, type.name());
        
        if (type == ChatType.GROUP) {
            stmt.setInt(3, adminId);
        } else {
            stmt.setNull(3, Types.INTEGER);
        }
        
        stmt.executeUpdate();
        
        ResultSet generatedKeys = stmt.getGeneratedKeys();
        int chatId = -1;
        if (generatedKeys.next()) {
            chatId = generatedKeys.getInt(1);
        }
        generatedKeys.close();
        stmt.close();
        
        return new Chat(chatId, name, type);
    }

    public void addParticipantToChat(int chatId, int userId) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO chat_participants (chat_id, user_id) VALUES (?, ?)");
        stmt.setInt(1, chatId);
        stmt.setInt(2, userId);
        stmt.executeUpdate();
        stmt.close();
    }

    public void removeParticipantFromChat(int chatId, int userId) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM chat_participants WHERE chat_id = ? AND user_id = ?");
        stmt.setInt(1, chatId);
        stmt.setInt(2, userId);
        stmt.executeUpdate();
        stmt.close();
    }

    public List<Chat> getUserChats(int userId) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "SELECT c.*, cp.display_name FROM chats c " +
                "JOIN chat_participants cp ON c.id = cp.chat_id " +
                "WHERE cp.user_id = ?");
        stmt.setInt(1, userId);
        ResultSet rs = stmt.executeQuery();

        List<Chat> chats = new ArrayList<>();
        while (rs.next()) {
            int chatId = rs.getInt("id");
            String name = rs.getString("name");
            String displayName = rs.getString("display_name");
            ChatType type = ChatType.valueOf(rs.getString("type"));
            
            Chat chat = new Chat(chatId, displayName != null ? displayName : name, type);
            
            if (type == ChatType.GROUP) {
                chat.setAdminId(rs.getInt("admin_id"));
            }
            
            // Cargar participantes
            loadChatParticipants(chat);
            
            chats.add(chat);
        }

        rs.close();
        stmt.close();
        return chats;
    }

    private void loadChatParticipants(Chat chat) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "SELECT u.* FROM users u " +
                "JOIN chat_participants cp ON u.id = cp.user_id " +
                "WHERE cp.chat_id = ?");
        stmt.setInt(1, chat.getId());
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            User participant = new User(rs.getInt("id"), rs.getString("username"));
            chat.addParticipant(participant);
        }

        rs.close();
        stmt.close();
    }

    public void deleteChat(int chatId) throws SQLException {
        // Primero eliminar mensajes
        PreparedStatement deleteMessages = connection.prepareStatement(
                "DELETE FROM messages WHERE chat_id = ?");
        deleteMessages.setInt(1, chatId);
        deleteMessages.executeUpdate();
        deleteMessages.close();
        
        // Luego eliminar participantes
        PreparedStatement deleteParticipants = connection.prepareStatement(
                "DELETE FROM chat_participants WHERE chat_id = ?");
        deleteParticipants.setInt(1, chatId);
        deleteParticipants.executeUpdate();
        deleteParticipants.close();
        
        // Eliminar solicitudes de grupo pendientes
        PreparedStatement deleteRequests = connection.prepareStatement(
                "DELETE FROM group_requests WHERE chat_id = ?");
        deleteRequests.setInt(1, chatId);
        deleteRequests.executeUpdate();
        deleteRequests.close();
        
        // Finalmente eliminar el chat
        PreparedStatement deleteChat = connection.prepareStatement(
                "DELETE FROM chats WHERE id = ?");
        deleteChat.setInt(1, chatId);
        deleteChat.executeUpdate();
        deleteChat.close();
    }

    // Métodos para mensajes
    public void saveMessage(Message message) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO messages (chat_id, sender_id, content, timestamp) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS);
        stmt.setInt(1, message.getChatId());
        stmt.setInt(2, message.getSenderId());
        stmt.setString(3, message.getContent());
        stmt.setTimestamp(4, Timestamp.valueOf(message.getTimestamp()));
        
        stmt.executeUpdate();
        
        ResultSet generatedKeys = stmt.getGeneratedKeys();
        if (generatedKeys.next()) {
            message.setId(generatedKeys.getInt(1));
        }
        generatedKeys.close();
        stmt.close();
    }

    public List<Message> getChatMessages(int chatId) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "SELECT m.*, u.username FROM messages m " +
                "JOIN users u ON m.sender_id = u.id " +
                "WHERE m.chat_id = ? " +
                "ORDER BY m.timestamp");
        stmt.setInt(1, chatId);
        ResultSet rs = stmt.executeQuery();

        List<Message> messages = new ArrayList<>();
        while (rs.next()) {
            int id = rs.getInt("id");
            int senderId = rs.getInt("sender_id");
            String content = rs.getString("content");
            LocalDateTime timestamp = rs.getTimestamp("timestamp").toLocalDateTime();
            String senderUsername = rs.getString("username");
            
            Message message = new Message(id, senderId, chatId, content, timestamp);
            message.setSenderUsername(senderUsername);
            messages.add(message);
        }

        rs.close();
        stmt.close();
        return messages;
    }

    // Métodos para solicitudes de amistad
    public void sendFriendRequest(FriendRequest request) throws SQLException {
        // Verificar si ya existe una solicitud pendiente entre estos usuarios
        if (existsFriendRequest(request.getSenderId(), request.getReceiverId())) {
            return; // No hacer nada si ya existe una solicitud
        }
        
        // Verificar si ya son amigos
        if (areFriends(request.getSenderId(), request.getReceiverId())) {
            return; // No hacer nada si ya son amigos
        }
        
        PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO friend_requests (sender_id, receiver_id, status, timestamp) VALUES (?, ?, ?::request_status, ?)",
                Statement.RETURN_GENERATED_KEYS);
        stmt.setInt(1, request.getSenderId());
        stmt.setInt(2, request.getReceiverId());
        stmt.setString(3, request.getStatus().name());
        stmt.setTimestamp(4, Timestamp.valueOf(request.getTimestamp()));
        
        stmt.executeUpdate();
        
        ResultSet generatedKeys = stmt.getGeneratedKeys();
        if (generatedKeys.next()) {
            request.setId(generatedKeys.getInt(1));
        }
        generatedKeys.close();
        stmt.close();
    }

    // Verifica si ya existe una solicitud pendiente entre dos usuarios
    public boolean existsFriendRequest(int senderId, int receiverId) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) FROM friend_requests " +
                "WHERE (sender_id = ? AND receiver_id = ? OR sender_id = ? AND receiver_id = ?) " +
                "AND status = 'PENDING'");
        stmt.setInt(1, senderId);
        stmt.setInt(2, receiverId);
        stmt.setInt(3, receiverId);
        stmt.setInt(4, senderId);
        
        ResultSet rs = stmt.executeQuery();
        rs.next();
        int count = rs.getInt(1);
        
        rs.close();
        stmt.close();
        
        return count > 0;
    }
    
    // Verifica si dos usuarios ya son amigos
    public boolean areFriends(int userId1, int userId2) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) FROM friends " +
                "WHERE user_id = ? AND friend_id = ?");
        stmt.setInt(1, userId1);
        stmt.setInt(2, userId2);
        
        ResultSet rs = stmt.executeQuery();
        rs.next();
        int count = rs.getInt(1);
        
        rs.close();
        stmt.close();
        
        return count > 0;
    }

    public List<FriendRequest> getUserPendingFriendRequests(int userId) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "SELECT fr.*, u1.username as sender_username, u2.username as receiver_username " +
                "FROM friend_requests fr " +
                "JOIN users u1 ON fr.sender_id = u1.id " +
                "JOIN users u2 ON fr.receiver_id = u2.id " +
                "WHERE fr.receiver_id = ? AND fr.status = 'PENDING'");
        stmt.setInt(1, userId);
        ResultSet rs = stmt.executeQuery();

        List<FriendRequest> requests = new ArrayList<>();
        while (rs.next()) {
            int id = rs.getInt("id");
            int senderId = rs.getInt("sender_id");
            int receiverId = rs.getInt("receiver_id");
            LocalDateTime timestamp = rs.getTimestamp("timestamp").toLocalDateTime();
            RequestStatus status = RequestStatus.valueOf(rs.getString("status"));
            String senderUsername = rs.getString("sender_username");
            String receiverUsername = rs.getString("receiver_username");
            
            FriendRequest request = new FriendRequest(id, senderId, receiverId, timestamp, status);
            request.setSenderUsername(senderUsername);
            request.setReceiverUsername(receiverUsername);
            requests.add(request);
        }

        rs.close();
        stmt.close();
        return requests;
    }

    public void updateFriendRequestStatus(int requestId, RequestStatus status) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "UPDATE friend_requests SET status = ?::request_status WHERE id = ?");
        stmt.setString(1, status.name());
        stmt.setInt(2, requestId);
        stmt.executeUpdate();
        stmt.close();
    }

    public void addFriend(int userId, int friendId) throws SQLException {
        // Agregar en ambas direcciones para facilitar las consultas
        PreparedStatement stmt1 = connection.prepareStatement(
                "INSERT INTO friends (user_id, friend_id) VALUES (?, ?)");
        stmt1.setInt(1, userId);
        stmt1.setInt(2, friendId);
        stmt1.executeUpdate();
        stmt1.close();
        
        PreparedStatement stmt2 = connection.prepareStatement(
                "INSERT INTO friends (user_id, friend_id) VALUES (?, ?)");
        stmt2.setInt(1, friendId);
        stmt2.setInt(2, userId);
        stmt2.executeUpdate();
        stmt2.close();
    }

    // Métodos para solicitudes de grupo
    public void sendGroupRequest(GroupRequest request) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO group_requests (chat_id, sender_id, receiver_id, status, timestamp) VALUES (?, ?, ?, ?::request_status, ?)",
                Statement.RETURN_GENERATED_KEYS);
        stmt.setInt(1, request.getChatId());
        stmt.setInt(2, request.getSenderId());
        stmt.setInt(3, request.getReceiverId());
        stmt.setString(4, request.getStatus().name());
        stmt.setTimestamp(5, Timestamp.valueOf(request.getTimestamp()));
        
        stmt.executeUpdate();
        
        ResultSet generatedKeys = stmt.getGeneratedKeys();
        if (generatedKeys.next()) {
            request.setId(generatedKeys.getInt(1));
        }
        generatedKeys.close();
        stmt.close();
    }

    public List<GroupRequest> getUserPendingGroupRequests(int userId) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "SELECT gr.*, c.name as chat_name, u1.username as sender_username, u2.username as receiver_username " +
                "FROM group_requests gr " +
                "JOIN chats c ON gr.chat_id = c.id " +
                "JOIN users u1 ON gr.sender_id = u1.id " +
                "JOIN users u2 ON gr.receiver_id = u2.id " +
                "WHERE gr.receiver_id = ? AND gr.status = 'PENDING'");
        stmt.setInt(1, userId);
        ResultSet rs = stmt.executeQuery();

        List<GroupRequest> requests = new ArrayList<>();
        while (rs.next()) {
            int id = rs.getInt("id");
            int chatId = rs.getInt("chat_id");
            int senderId = rs.getInt("sender_id");
            int receiverId = rs.getInt("receiver_id");
            LocalDateTime timestamp = rs.getTimestamp("timestamp").toLocalDateTime();
            GroupRequest.RequestStatus status = GroupRequest.RequestStatus.valueOf(rs.getString("status"));
            String chatName = rs.getString("chat_name");
            String senderUsername = rs.getString("sender_username");
            String receiverUsername = rs.getString("receiver_username");
            
            GroupRequest request = new GroupRequest(id, chatId, senderId, receiverId, timestamp, status);
            request.setChatName(chatName);
            request.setSenderUsername(senderUsername);
            request.setReceiverUsername(receiverUsername);
            requests.add(request);
        }

        rs.close();
        stmt.close();
        return requests;
    }

    public void updateGroupRequestStatus(int requestId, GroupRequest.RequestStatus status) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "UPDATE group_requests SET status = ?::request_status WHERE id = ?");
        stmt.setString(1, status.name());
        stmt.setInt(2, requestId);
        stmt.executeUpdate();
        stmt.close();
    }

    public int getGroupChatParticipantCount(int chatId) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) FROM chat_participants WHERE chat_id = ?");
        stmt.setInt(1, chatId);
        ResultSet rs = stmt.executeQuery();
        rs.next();
        int count = rs.getInt(1);
        rs.close();
        stmt.close();
        return count;
    }

    public List<GroupRequest> getPendingRequestsForChat(int chatId) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM group_requests WHERE chat_id = ? AND status = 'PENDING'");
        stmt.setInt(1, chatId);
        ResultSet rs = stmt.executeQuery();

        List<GroupRequest> requests = new ArrayList<>();
        while (rs.next()) {
            int id = rs.getInt("id");
            int senderId = rs.getInt("sender_id");
            int receiverId = rs.getInt("receiver_id");
            LocalDateTime timestamp = rs.getTimestamp("timestamp").toLocalDateTime();
            GroupRequest.RequestStatus status = GroupRequest.RequestStatus.valueOf(rs.getString("status"));
            
            GroupRequest request = new GroupRequest(id, chatId, senderId, receiverId, timestamp, status);
            requests.add(request);
        }

        rs.close();
        stmt.close();
        return requests;
    }

    /**
     * Verifica si ya existe un chat temporal entre dos usuarios específicos
     * @param userId1 ID del primer usuario
     * @param userId2 ID del segundo usuario
     * @return true si ya existe un chat temporal entre estos usuarios, false en caso contrario
     * @throws SQLException Si hay un error de base de datos
     */
    public boolean existsTemporaryChat(int userId1, int userId2) throws SQLException {
        String query = "SELECT c.id FROM chats c " +
                "JOIN chat_participants p1 ON c.id = p1.chat_id " +
                "JOIN chat_participants p2 ON c.id = p2.chat_id " +
                "WHERE c.type = 'TEMPORARY' " +
                "AND p1.user_id = ? " +
                "AND p2.user_id = ? " +
                "AND (SELECT COUNT(*) FROM chat_participants WHERE chat_id = c.id) = 2";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId1);
            stmt.setInt(2, userId2);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next(); // Si hay al menos un resultado, ya existe un chat temporal
            }
        }
    }

    public void updateChatNameForUser(int chatId, int userId, String displayName) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "UPDATE chat_participants SET display_name = ? WHERE chat_id = ? AND user_id = ?");
        stmt.setString(1, displayName);
        stmt.setInt(2, chatId);
        stmt.setInt(3, userId);
        stmt.executeUpdate();
        stmt.close();
    }

    public boolean updateUserPassword(String username, String newPassword) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "UPDATE users SET password = ? WHERE username = ?");
        stmt.setString(1, newPassword);
        stmt.setString(2, username);
        
        int affectedRows = stmt.executeUpdate();
        stmt.close();
        
        return affectedRows > 0;
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
} 