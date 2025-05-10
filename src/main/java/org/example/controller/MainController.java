package org.example.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.App;
import org.example.database.DatabaseManager;
import org.example.model.Chat;
import org.example.model.Message;
import org.example.model.User;
import org.example.network.ChatClient;
import org.example.network.MessageListener;
import org.example.config.ConfigManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

public class MainController implements MessageListener {

    @FXML
    private Label usernameLabel;

    @FXML
    private TextField searchField;

    @FXML
    private ListView<Chat> temporaryChatsListView;

    @FXML
    private ListView<Chat> friendChatsListView;

    @FXML
    private ListView<Chat> groupChatsListView;

    @FXML
    private BorderPane chatArea;

    @FXML
    private StackPane welcomePane;

    @FXML
    private Label currentChatNameLabel;

    @FXML
    private VBox messagesContainer;

    @FXML
    private TextField messageField;

    private DatabaseManager dbManager;
    private ChatClient chatClient;
    private User currentUser;
    private Chat currentChat;

    private ObservableList<Chat> temporaryChats = FXCollections.observableArrayList();
    private ObservableList<Chat> friendChats = FXCollections.observableArrayList();
    private ObservableList<Chat> groupChats = FXCollections.observableArrayList();

    public void initialize() {
        dbManager = DatabaseManager.getInstance();
        currentUser = UserSession.getInstance().getCurrentUser();
        
        // Configurar el nombre de usuario
        usernameLabel.setText("Hola, " + currentUser.getUsername());
        
        // Configurar las listas de chats
        temporaryChatsListView.setItems(temporaryChats);
        friendChatsListView.setItems(friendChats);
        groupChatsListView.setItems(groupChats);
        
        // Configurar los cell factories para mostrar los nombres de los chats
        temporaryChatsListView.setCellFactory(param -> new ChatListCell());
        friendChatsListView.setCellFactory(param -> new ChatListCell());
        groupChatsListView.setCellFactory(param -> new ChatListCell());
        
        // Cargar los chats del usuario
        loadChats();
        
        // Configurar los listeners para selección de chat
        temporaryChatsListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        friendChatsListView.getSelectionModel().clearSelection();
                        groupChatsListView.getSelectionModel().clearSelection();
                        showChat(newValue);
                    }
                });
        
        friendChatsListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        temporaryChatsListView.getSelectionModel().clearSelection();
                        groupChatsListView.getSelectionModel().clearSelection();
                        showChat(newValue);
                    }
                });
        
        groupChatsListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        temporaryChatsListView.getSelectionModel().clearSelection();
                        friendChatsListView.getSelectionModel().clearSelection();
                        showChat(newValue);
                    }
                });
        
        // Inicialmente mostrar el panel de bienvenida
        showWelcomePane();
        
        // Iniciar el cliente de chat
        try {
            // Usar ConfigManager para obtener host y puerto del servidor
            String serverHost = ConfigManager.getInstance().getServerHost();
            int serverPort = ConfigManager.getInstance().getServerPort();
            
            chatClient = new ChatClient(serverHost, serverPort, currentUser.getId(), this);
            chatClient.start();
            
            // Guardar la instancia del cliente en UserSession para uso global
            UserSession.getInstance().setChatClient(chatClient);
        } catch (IOException e) {
            showAlert("Error de conexión", "No se pudo conectar al servidor de chat.");
            e.printStackTrace();
        }
    }

    private void loadChats() {
        try {
            // Obtener los chats actuales del usuario desde la base de datos
            List<Chat> chats = dbManager.getUserChats(currentUser.getId());
            
            // Almacenar los IDs de chats que existen en la base de datos
            Set<Integer> existingChatIds = new HashSet<>();
            for (Chat chat : chats) {
                existingChatIds.add(chat.getId());
            }
            
            // Eliminar chats que ya no existen en la base de datos de nuestras listas
            temporaryChats.removeIf(chat -> !existingChatIds.contains(chat.getId()));
            friendChats.removeIf(chat -> !existingChatIds.contains(chat.getId()));
            groupChats.removeIf(chat -> !existingChatIds.contains(chat.getId()));
            
            // Actualizar las listas de chats con información actualizada
            for (Chat chat : chats) {
                boolean exists = false;
                
                // Verificar si el chat ya está en alguna de nuestras listas y actualizarlo
                switch (chat.getType()) {
                    case TEMPORARY:
                        for (int i = 0; i < temporaryChats.size(); i++) {
                            if (temporaryChats.get(i).getId() == chat.getId()) {
                                temporaryChats.set(i, chat);
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            temporaryChats.add(chat);
                        }
                        break;
                        
                    case FRIEND:
                        for (int i = 0; i < friendChats.size(); i++) {
                            if (friendChats.get(i).getId() == chat.getId()) {
                                friendChats.set(i, chat);
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            friendChats.add(chat);
                        }
                        break;
                        
                    case GROUP:
                        for (int i = 0; i < groupChats.size(); i++) {
                            if (groupChats.get(i).getId() == chat.getId()) {
                                groupChats.set(i, chat);
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            groupChats.add(chat);
                        }
                        break;
                }
            }
            
            // Notificar a las vistas de lista que sus datos han cambiado
            Platform.runLater(() -> {
                temporaryChatsListView.refresh();
                friendChatsListView.refresh();
                groupChatsListView.refresh();
            });
        } catch (SQLException e) {
            showAlert("Error", "No se pudieron cargar los chats.");
            e.printStackTrace();
        }
    }

    private void showChat(Chat chat) {
        currentChat = chat;
        welcomePane.setVisible(false);
        chatArea.setVisible(true);
        
        // Mostrar el nombre del chat
        currentChatNameLabel.setText(chat.getName());
        
        // Habilitar campo de mensaje y botones
        messageField.setDisable(false);
        
        // Cargar mensajes
        loadMessages();
        
        // Desplazar al último mensaje
        Platform.runLater(() -> {
            if (messagesContainer.getChildren().size() > 0) {
                messagesContainer.getChildren().get(messagesContainer.getChildren().size() - 1)
                    .requestFocus();
            }
        });
    }

    private void showWelcomePane() {
        currentChat = null;
        welcomePane.setVisible(true);
        chatArea.setVisible(false);
    }

    private void loadMessages() {
        // Limpiar el contenedor de mensajes antes de cargarlos nuevamente
        messagesContainer.getChildren().clear();
        
        try {
            List<Message> messages = dbManager.getChatMessages(currentChat.getId());
            
            // Usar Platform.runLater para evitar problemas de concurrencia con JavaFX
            Platform.runLater(() -> {
                // Recorrer y añadir los mensajes al contenedor
                for (Message message : messages) {
                    HBox messageBox = new HBox();
                    Label messageLabel = new Label(message.getContent());
                    Label usernameLabel = new Label(message.getSenderUsername() + ": ");
                    
                    if (message.getSenderId() == currentUser.getId()) {
                        // Mensaje enviado por el usuario actual
                        messageBox.setAlignment(Pos.CENTER_RIGHT);
                        messageLabel.getStyleClass().add("message-bubble-sent");
                        messageBox.getChildren().add(messageLabel);
                    } else {
                        // Mensaje recibido de otro usuario
                        messageBox.setAlignment(Pos.CENTER_LEFT);
                        messageLabel.getStyleClass().add("message-bubble-received");
                        VBox messageContent = new VBox();
                        messageContent.getChildren().addAll(usernameLabel, messageLabel);
                        messageBox.getChildren().add(messageContent);
                    }
                    
                    messagesContainer.getChildren().add(messageBox);
                }
                
                // Desplazar al último mensaje después de cargarlos todos
                if (messagesContainer.getChildren().size() > 0) {
                    messagesContainer.getChildren().get(messagesContainer.getChildren().size() - 1)
                        .requestFocus();
                }
            });
        } catch (SQLException e) {
            showAlert("Error", "No se pudieron cargar los mensajes.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSendMessage() {
        if (currentChat == null || messageField.getText().trim().isEmpty()) {
            return;
        }
        
        String content = messageField.getText().trim();
        Message message = new Message(currentUser.getId(), currentChat.getId(), content);
        
        try {
            // Guardar el mensaje en la base de datos
            dbManager.saveMessage(message);
            
            // Enviar el mensaje a través del socket
            chatClient.sendMessage(message);
            
            // Limpiar el campo de mensaje
            messageField.clear();
            
            // No agregamos el mensaje a la UI aquí, ya que llegará de vuelta por el servidor
            // y se mostrará a través de onMessageReceived
        } catch (SQLException e) {
            showAlert("Error", "No se pudo enviar el mensaje.");
            e.printStackTrace();
        } catch (IOException e) {
            showAlert("Error de conexión", "No se pudo enviar el mensaje al servidor.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText().trim();
        
        if (searchTerm.isEmpty()) {
            return;
        }
        
        try {
            User user = dbManager.getUserByUsername(searchTerm);
            
            if (user == null) {
                showAlert("Usuario no encontrado", "No se encontró ningún usuario con ese nombre.");
                return;
            }
            
            if (user.getId() == currentUser.getId()) {
                showAlert("Búsqueda", "No puedes iniciar un chat contigo mismo.");
                return;
            }
            
            // Verificar si ya existe un chat con este usuario
            boolean chatExists = false;
            Chat existingChat = null;
            
            for (Chat chat : friendChats) {
                for (User participant : chat.getParticipants()) {
                    if (participant.getId() == user.getId()) {
                        chatExists = true;
                        existingChat = chat;
                        break;
                    }
                }
                if (chatExists) break;
            }
            
            if (chatExists) {
                showAlert("Chat existente", "Ya tienes un chat con este usuario.");
                // Seleccionar el chat existente
                friendChatsListView.getSelectionModel().select(existingChat);
                return;
            }
            
            // Preguntar si quiere iniciar un chat temporal o enviar solicitud de amistad
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Nuevo Chat");
            alert.setHeaderText("¿Qué tipo de chat quieres iniciar con " + user.getUsername() + "?");
            
            ButtonType temporaryButton = new ButtonType("Chat Temporal");
            ButtonType friendRequestButton = new ButtonType("Solicitud de Amistad");
            ButtonType cancelButton = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
            
            alert.getButtonTypes().setAll(temporaryButton, friendRequestButton, cancelButton);
            
            Optional<ButtonType> result = alert.showAndWait();
            
            if (result.get() == temporaryButton) {
                createTemporaryChat(user);
            } else if (result.get() == friendRequestButton) {
                sendFriendRequest(user);
            }
            
        } catch (SQLException e) {
            showAlert("Error", "No se pudo buscar el usuario.");
            e.printStackTrace();
        }
    }

    private void createTemporaryChat(User otherUser) {
        try {
            // Verificar si ya existe un chat temporal con este usuario
            if (dbManager.existsTemporaryChat(currentUser.getId(), otherUser.getId())) {
                // Si existe, mostrar el chat existente en lugar de crear uno nuevo
                for (Chat chat : temporaryChats) {
                    if (chat.getType() == Chat.ChatType.TEMPORARY && 
                        chat.getParticipants().stream().anyMatch(p -> p.getId() == otherUser.getId())) {
                        // Seleccionar el chat existente
                        temporaryChatsListView.getSelectionModel().select(chat);
                        return;
                    }
                }
                // Si llegamos aquí, el chat existe en la base de datos pero no en nuestra lista
                // Recargar los chats para asegurarnos de que está en la lista
                loadChats();
                showAlert("Información", "Ya tienes un chat temporal con " + otherUser.getUsername());
                return;
            }
            
            // Crear un nuevo chat temporal
            String chatName = "Chat Temporal";
            Chat chat = dbManager.createChat(chatName, Chat.ChatType.TEMPORARY, 0);
            
            // Agregar participantes al chat
            dbManager.addParticipantToChat(chat.getId(), currentUser.getId());
            dbManager.addParticipantToChat(chat.getId(), otherUser.getId());
            
            // Establecer nombres personalizados para ambos usuarios
            dbManager.updateChatNameForUser(chat.getId(), currentUser.getId(), otherUser.getUsername());
            dbManager.updateChatNameForUser(chat.getId(), otherUser.getId(), currentUser.getUsername());
            
            // Actualizar la lista de chats
            chat.addParticipant(currentUser);
            chat.addParticipant(otherUser);
            chat.setName(otherUser.getUsername()); // Para la visualización inmediata
            temporaryChats.add(chat);
            
            // Notificar al otro usuario sobre el nuevo chat (si está conectado)
            Message systemMessage = new Message(0, chat.getId(), 
                "¡" + currentUser.getUsername() + " ha iniciado un chat temporal contigo!");
            systemMessage.setSenderUsername("Sistema");
            
            try {
                chatClient.sendMessage(systemMessage);
            } catch (IOException e) {
                System.err.println("No se pudo notificar al otro usuario sobre el nuevo chat");
            }
            
            // Seleccionar el nuevo chat
            temporaryChatsListView.getSelectionModel().select(chat);
        } catch (SQLException e) {
            showAlert("Error", "No se pudo crear el chat temporal.");
            e.printStackTrace();
        }
    }

    private void sendFriendRequest(User receiver) {
        try {
            // Verificar si ya son amigos
            if (dbManager.areFriends(currentUser.getId(), receiver.getId())) {
                showAlert("Información", "Ya eres amigo de " + receiver.getUsername());
                return;
            }
            
            // Verificar si ya existe una solicitud pendiente
            if (dbManager.existsFriendRequest(currentUser.getId(), receiver.getId())) {
                showAlert("Información", "Ya existe una solicitud de amistad pendiente con " + receiver.getUsername());
                return;
            }
            
            // Crear y enviar la solicitud
            org.example.model.FriendRequest request = new org.example.model.FriendRequest(
                    currentUser.getId(), receiver.getId());
            dbManager.sendFriendRequest(request);
            
            showAlert("Solicitud enviada", "Se ha enviado una solicitud de amistad a " + receiver.getUsername());
        } catch (SQLException e) {
            showAlert("Error", "No se pudo enviar la solicitud de amistad.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleNewChat() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nuevo Chat");
        dialog.setHeaderText("Ingresa el nombre de usuario para iniciar un chat");
        dialog.setContentText("Usuario:");
        
        Optional<String> result = dialog.showAndWait();
        
        result.ifPresent(username -> {
            searchField.setText(username);
            handleSearch();
        });
    }

    @FXML
    private void handleNewGroup() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/new_group.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Nuevo Grupo");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
            // Recargar los chats después de crear un grupo
            loadChats();
        } catch (IOException e) {
            showAlert("Error", "No se pudo abrir la ventana de nuevo grupo.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleViewRequests() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/requests.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Solicitudes");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
            // Recargar los chats después de manejar solicitudes
            loadChats();
        } catch (IOException e) {
            showAlert("Error", "No se pudo abrir la ventana de solicitudes.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleChatInfo() {
        if (currentChat == null) {
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/chat_info.fxml"));
            Parent root = loader.load();
            
            ChatInfoController controller = loader.getController();
            controller.setChat(currentChat);
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Información del Chat");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
            // Recargar los chats después de ver/modificar la información
            loadChats();
            
            // Si el chat actual fue eliminado, mostrar la pantalla de bienvenida
            boolean chatExists = false;
            
            switch (currentChat.getType()) {
                case TEMPORARY:
                    chatExists = temporaryChats.contains(currentChat);
                    break;
                case FRIEND:
                    chatExists = friendChats.contains(currentChat);
                    break;
                case GROUP:
                    chatExists = groupChats.contains(currentChat);
                    break;
            }
            
            if (!chatExists) {
                showWelcomePane();
            }
            
        } catch (IOException e) {
            showAlert("Error", "No se pudo abrir la información del chat.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        try {
            // Cerrar la conexión del cliente
            if (chatClient != null) {
                chatClient.stop();
            }
            
            // Limpiar la sesión (esto también cierra el chatClient)
            UserSession.getInstance().clearSession();
            
            // Volver a la pantalla de login
            App.setRoot("login");
        } catch (IOException e) {
            showAlert("Error", "No se pudo cerrar la sesión.");
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void onMessageReceived(Message message) {
        // Este método se llama cuando se recibe un mensaje del servidor
        if (currentChat != null && message.getChatId() == currentChat.getId()) {
            // Si el mensaje es para el chat actual, mostrarlo
            Platform.runLater(() -> {
                HBox messageBox = new HBox();
                Label messageLabel = new Label(message.getContent());
                Label usernameLabel = new Label(message.getSenderUsername() + ": ");
                
                if (message.getSenderId() == currentUser.getId()) {
                    // Mensaje enviado por el usuario actual
                    messageBox.setAlignment(Pos.CENTER_RIGHT);
                    messageLabel.getStyleClass().add("message-bubble-sent");
                    messageBox.getChildren().add(messageLabel);
                } else {
                    // Mensaje recibido de otro usuario
                    messageBox.setAlignment(Pos.CENTER_LEFT);
                    messageLabel.getStyleClass().add("message-bubble-received");
                    VBox messageContent = new VBox();
                    messageContent.getChildren().addAll(usernameLabel, messageLabel);
                    messageBox.getChildren().add(messageContent);
                }
                
                messagesContainer.getChildren().add(messageBox);
                
                // Desplazar al último mensaje
                if (messagesContainer.getChildren().size() > 0) {
                    messagesContainer.getChildren().get(messagesContainer.getChildren().size() - 1)
                        .requestFocus();
                }
            });
        } else {
            // Si es un mensaje del sistema indicando un nuevo chat o de un chat que no tenemos abierto
            // verificamos si es necesario recargar los chats
            try {
                // Verificar si el chatId existe en nuestras listas actuales
                boolean chatExists = false;
                for (Chat chat : temporaryChats) {
                    if (chat.getId() == message.getChatId()) {
                        chatExists = true;
                        break;
                    }
                }
                if (!chatExists) {
                    for (Chat chat : friendChats) {
                        if (chat.getId() == message.getChatId()) {
                            chatExists = true;
                            break;
                        }
                    }
                }
                if (!chatExists) {
                    for (Chat chat : groupChats) {
                        if (chat.getId() == message.getChatId()) {
                            chatExists = true;
                            break;
                        }
                    }
                }
                
                // Si el chat no existe en nuestras listas, recargamos los chats
                if (!chatExists) {
                    Platform.runLater(this::loadChats);
                }
                
                // También podríamos mostrar una notificación
                if (message.getSenderId() == 0) {
                    if (message.getContent().contains("ha iniciado un chat temporal contigo")) {
                        Platform.runLater(() -> {
                            showAlert("Nuevo chat", message.getContent());
                            // Recargar los chats y asegurarse de que las listas se actualicen
                            loadChats();
                        });
                    } else if (message.getContent().contains("ha aceptado tu solicitud de amistad")) {
                        Platform.runLater(() -> {
                            showAlert("Solicitud aceptada", message.getContent());
                            // Recargar los chats y asegurarse de que las listas se actualicen
                            loadChats();
                        });
                    }
                }
            } catch (Exception e) {
                System.err.println("Error al actualizar los chats: " + e.getMessage());
            }
        }
    }

    // Clase para mostrar los chats en las listas
    private class ChatListCell extends ListCell<Chat> {
        @Override
        protected void updateItem(Chat chat, boolean empty) {
            super.updateItem(chat, empty);
            
            if (empty || chat == null) {
                setText(null);
            } else {
                setText(chat.getName());
            }
        }
    }
} 