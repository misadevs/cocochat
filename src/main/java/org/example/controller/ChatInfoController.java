package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.App;
import org.example.database.DatabaseManager;
import org.example.model.Chat;
import org.example.model.FriendRequest;
import org.example.model.User;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

public class ChatInfoController {

    @FXML
    private Text chatNameText;

    @FXML
    private Text chatTypeText;

    @FXML
    private ListView<User> participantsListView;

    @FXML
    private HBox groupActionsContainer;

    @FXML
    private Button addParticipantButton;

    @FXML
    private Button leaveGroupButton;

    @FXML
    private HBox temporaryActionsContainer;

    @FXML
    private Button sendFriendRequestButton;

    @FXML
    private Button endChatButton;

    @FXML
    private Button closeButton;

    private DatabaseManager dbManager;
    private User currentUser;
    private Chat chat;
    private ObservableList<User> participants = FXCollections.observableArrayList();

    public void initialize() {
        dbManager = DatabaseManager.getInstance();
        currentUser = UserSession.getInstance().getCurrentUser();
        
        // Configurar la lista de participantes
        participantsListView.setItems(participants);
        participantsListView.setCellFactory(param -> new UserListCell());
    }

    public void setChat(Chat chat) {
        this.chat = chat;
        
        // Mostrar la información del chat
        chatNameText.setText(chat.getName());
        
        String chatType;
        switch (chat.getType()) {
            case TEMPORARY:
                chatType = "Chat Temporal";
                break;
            case FRIEND:
                chatType = "Chat con Amigo";
                break;
            case GROUP:
                chatType = "Chat Grupal";
                break;
            default:
                chatType = "Chat";
        }
        chatTypeText.setText(chatType);
        
        // Mostrar participantes
        participants.clear();
        participants.addAll(chat.getParticipants());
        
        // Configurar visibilidad de acciones según el tipo de chat
        configureActionButtons();
    }

    private void configureActionButtons() {
        boolean isGroupChat = chat.isGroupChat();
        boolean isTemporaryChat = chat.isTemporaryChat();
        boolean isAdmin = chat.getAdminId() == currentUser.getId();
        
        // Mostrar/ocultar acciones de grupo
        groupActionsContainer.setVisible(isGroupChat);
        groupActionsContainer.setManaged(isGroupChat);
        
        // Si es admin, mostrar el botón de agregar participante
        addParticipantButton.setVisible(isGroupChat && isAdmin);
        addParticipantButton.setManaged(isGroupChat && isAdmin);
        
        // Mostrar/ocultar acciones de chat temporal
        temporaryActionsContainer.setVisible(isTemporaryChat);
        temporaryActionsContainer.setManaged(isTemporaryChat);
        
        // Para chats temporales, verificar si hay otro participante para enviar solicitud
        if (isTemporaryChat && chat.getParticipants().size() > 1) {
            User otherUser = null;
            for (User user : chat.getParticipants()) {
                if (user.getId() != currentUser.getId()) {
                    otherUser = user;
                    break;
                }
            }
            
            // Habilitar el botón de solicitud de amistad solo si no es amigo ya
            if (otherUser != null) {
                boolean isAlreadyFriend = currentUser.getFriends().contains(otherUser);
                sendFriendRequestButton.setDisable(isAlreadyFriend);
            }
        }
    }

    @FXML
    private void handleAddParticipant() {
        if (!chat.isGroupChat() || chat.getAdminId() != currentUser.getId()) {
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/add_participant.fxml"));
            Parent root = loader.load();
            
            AddParticipantController controller = loader.getController();
            controller.setChat(chat);
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Agregar Participante");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
            // Recargar la lista de participantes
            setChat(chat);
        } catch (IOException e) {
            showAlert("Error", "No se pudo abrir la ventana para agregar participantes.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLeaveGroup() {
        if (!chat.isGroupChat()) {
            return;
        }
        
        // Confirmar la acción
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Salir del grupo");
        alert.setHeaderText("¿Estás seguro de que quieres salir del grupo?");
        
        if (chat.getAdminId() == currentUser.getId()) {
            alert.setContentText("Eres el administrador del grupo. Si sales, el grupo se eliminará para todos los participantes.");
        } else {
            alert.setContentText("Ya no podrás ver los mensajes del grupo.");
        }
        
        Optional<ButtonType> result = alert.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Si es el administrador, eliminar el grupo
                if (chat.getAdminId() == currentUser.getId()) {
                    dbManager.deleteChat(chat.getId());
                } else {
                    // Si no es el administrador, solo quitar al usuario
                    dbManager.removeParticipantFromChat(chat.getId(), currentUser.getId());
                    
                    // Verificar si quedan menos de 3 participantes, en cuyo caso eliminar el grupo
                    int participantCount = dbManager.getGroupChatParticipantCount(chat.getId());
                    if (participantCount < 3) {
                        dbManager.deleteChat(chat.getId());
                    }
                }
                
                // Cerrar la ventana
                closeWindow();
            } catch (SQLException e) {
                showAlert("Error", "No se pudo procesar la operación.");
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleSendFriendRequest() {
        if (!chat.isTemporaryChat() || chat.getParticipants().size() <= 1) {
            return;
        }
        
        // Buscar al otro participante
        User otherUser = null;
        for (User user : chat.getParticipants()) {
            if (user.getId() != currentUser.getId()) {
                otherUser = user;
                break;
            }
        }
        
        if (otherUser == null) {
            return;
        }
        
        try {
            // Verificar si ya son amigos
            boolean isAlreadyFriend = currentUser.getFriends().contains(otherUser);
            
            if (isAlreadyFriend) {
                showAlert("Información", "Ya eres amigo de " + otherUser.getUsername());
                return;
            }
            
            // Enviar solicitud de amistad
            FriendRequest request = new FriendRequest(currentUser.getId(), otherUser.getId());
            dbManager.sendFriendRequest(request);
            
            showAlert("Solicitud enviada", "Se ha enviado una solicitud de amistad a " + otherUser.getUsername());
            
            // Deshabilitar el botón
            sendFriendRequestButton.setDisable(true);
        } catch (SQLException e) {
            showAlert("Error", "No se pudo enviar la solicitud de amistad.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleEndChat() {
        if (!chat.isTemporaryChat()) {
            return;
        }
        
        // Confirmar la acción
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Finalizar chat");
        alert.setHeaderText("¿Estás seguro de que quieres finalizar este chat?");
        alert.setContentText("Se eliminará la conversación para todos los participantes.");
        
        Optional<ButtonType> result = alert.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                dbManager.deleteChat(chat.getId());
                closeWindow();
            } catch (SQLException e) {
                showAlert("Error", "No se pudo eliminar el chat.");
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleClose() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Clase para mostrar los usuarios en la lista
    private class UserListCell extends ListCell<User> {
        @Override
        protected void updateItem(User user, boolean empty) {
            super.updateItem(user, empty);
            
            if (empty || user == null) {
                setText(null);
            } else {
                String text = user.getUsername();
                
                // Marcar al usuario actual
                if (user.getId() == currentUser.getId()) {
                    text += " (Tú)";
                }
                
                // Marcar al administrador si es un chat grupal
                if (chat.isGroupChat() && user.getId() == chat.getAdminId()) {
                    text += " (Admin)";
                }
                
                setText(text);
            }
        }
    }
} 