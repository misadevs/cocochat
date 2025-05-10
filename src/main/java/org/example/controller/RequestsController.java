package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import org.example.database.DatabaseManager;
import org.example.model.Chat;
import org.example.model.FriendRequest;
import org.example.model.GroupRequest;
import org.example.model.User;
import org.example.model.Message;
import org.example.network.ChatClient;

import java.sql.SQLException;
import java.util.List;
import java.io.IOException;

public class RequestsController {

    @FXML
    private ListView<FriendRequest> friendRequestsListView;

    @FXML
    private Button acceptFriendRequestButton;

    @FXML
    private Button rejectFriendRequestButton;

    @FXML
    private ListView<GroupRequest> groupRequestsListView;

    @FXML
    private Button acceptGroupRequestButton;

    @FXML
    private Button rejectGroupRequestButton;

    @FXML
    private Button closeButton;

    private DatabaseManager dbManager;
    private User currentUser;
    private ObservableList<FriendRequest> friendRequests = FXCollections.observableArrayList();
    private ObservableList<GroupRequest> groupRequests = FXCollections.observableArrayList();

    public void initialize() {
        dbManager = DatabaseManager.getInstance();
        currentUser = UserSession.getInstance().getCurrentUser();
        
        // Configurar las listas
        friendRequestsListView.setItems(friendRequests);
        groupRequestsListView.setItems(groupRequests);
        
        // Configurar los cell factories
        friendRequestsListView.setCellFactory(param -> new FriendRequestListCell());
        groupRequestsListView.setCellFactory(param -> new GroupRequestListCell());
        
        // Configurar los listeners para habilitar/deshabilitar botones
        friendRequestsListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    boolean hasSelection = newValue != null;
                    acceptFriendRequestButton.setDisable(!hasSelection);
                    rejectFriendRequestButton.setDisable(!hasSelection);
                });
        
        groupRequestsListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    boolean hasSelection = newValue != null;
                    acceptGroupRequestButton.setDisable(!hasSelection);
                    rejectGroupRequestButton.setDisable(!hasSelection);
                });
        
        // Cargar las solicitudes
        loadRequests();
    }

    private void loadRequests() {
        try {
            // Cargar solicitudes de amistad
            List<FriendRequest> pendingFriendRequests = dbManager.getUserPendingFriendRequests(currentUser.getId());
            friendRequests.clear();
            friendRequests.addAll(pendingFriendRequests);
            
            // Cargar invitaciones a grupos
            List<GroupRequest> pendingGroupRequests = dbManager.getUserPendingGroupRequests(currentUser.getId());
            groupRequests.clear();
            groupRequests.addAll(pendingGroupRequests);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAcceptFriendRequest() {
        FriendRequest selectedRequest = friendRequestsListView.getSelectionModel().getSelectedItem();
        if (selectedRequest == null) {
            return;
        }
        
        try {
            // Actualizar el estado de la solicitud
            dbManager.updateFriendRequestStatus(selectedRequest.getId(), FriendRequest.RequestStatus.ACCEPTED);
            
            // Agregar a ambos usuarios como amigos
            dbManager.addFriend(selectedRequest.getSenderId(), selectedRequest.getReceiverId());
            
            // Obtener usuarios para nombres correctos de chat
            User sender = dbManager.getUserById(selectedRequest.getSenderId());
            User receiver = dbManager.getUserById(selectedRequest.getReceiverId());
            
            // Crear un chat de amigos con nombre personalizado para cada usuario
            // El nombre para el remitente será el nombre del destinatario, y viceversa
            // Primero, creamos el chat
            Chat chat = dbManager.createChat("Chat de amistad", Chat.ChatType.FRIEND, 0);
            
            // Agregar participantes al chat
            dbManager.addParticipantToChat(chat.getId(), selectedRequest.getSenderId());
            dbManager.addParticipantToChat(chat.getId(), selectedRequest.getReceiverId());
            
            // Actualizar los nombres de visualización del chat para cada usuario
            dbManager.updateChatNameForUser(chat.getId(), selectedRequest.getSenderId(), receiver.getUsername());
            dbManager.updateChatNameForUser(chat.getId(), selectedRequest.getReceiverId(), sender.getUsername());
            
            // Enviar mensaje de sistema para notificar al remitente que se ha aceptado su solicitud
            try {
                // Obtener el cliente de chat actual
                ChatClient chatClient = UserSession.getInstance().getChatClient();
                if (chatClient != null) {
                    // Crear y enviar un mensaje de sistema
                    Message systemMessage = new Message(0, chat.getId(), 
                        "¡" + receiver.getUsername() + " ha aceptado tu solicitud de amistad! Ya pueden chatear.");
                    systemMessage.setSenderUsername("Sistema");
                    chatClient.sendMessage(systemMessage);
                }
            } catch (Exception e) {
                System.err.println("Error al notificar al remitente: " + e.getMessage());
            }
            
            // Eliminar la solicitud de la lista
            friendRequests.remove(selectedRequest);
            
            // Mostrar confirmación al usuario
            showAlert("Solicitud aceptada", "Ahora eres amigo de " + selectedRequest.getSenderUsername());
        } catch (SQLException e) {
            showAlert("Error", "No se pudo aceptar la solicitud de amistad: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRejectFriendRequest() {
        FriendRequest selectedRequest = friendRequestsListView.getSelectionModel().getSelectedItem();
        if (selectedRequest == null) {
            return;
        }
        
        try {
            // Actualizar el estado de la solicitud
            dbManager.updateFriendRequestStatus(selectedRequest.getId(), FriendRequest.RequestStatus.REJECTED);
            
            // Eliminar la solicitud de la lista
            friendRequests.remove(selectedRequest);
            
            // Mostrar confirmación al usuario
            showAlert("Solicitud rechazada", "Has rechazado la solicitud de amistad de " + 
                    selectedRequest.getSenderUsername());
        } catch (SQLException e) {
            showAlert("Error", "No se pudo rechazar la solicitud de amistad: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAcceptGroupRequest() {
        GroupRequest selectedRequest = groupRequestsListView.getSelectionModel().getSelectedItem();
        if (selectedRequest == null) {
            return;
        }
        
        try {
            // Actualizar el estado de la solicitud
            dbManager.updateGroupRequestStatus(selectedRequest.getId(), GroupRequest.RequestStatus.ACCEPTED);
            
            // Agregar al usuario como participante del chat
            dbManager.addParticipantToChat(selectedRequest.getChatId(), selectedRequest.getReceiverId());
            
            // Eliminar la solicitud de la lista
            groupRequests.remove(selectedRequest);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRejectGroupRequest() {
        GroupRequest selectedRequest = groupRequestsListView.getSelectionModel().getSelectedItem();
        if (selectedRequest == null) {
            return;
        }
        
        try {
            // Actualizar el estado de la solicitud
            dbManager.updateGroupRequestStatus(selectedRequest.getId(), GroupRequest.RequestStatus.REJECTED);
            
            // Verificar si hay que eliminar el grupo
            List<GroupRequest> pendingRequests = dbManager.getPendingRequestsForChat(selectedRequest.getChatId());
            int participantCount = dbManager.getGroupChatParticipantCount(selectedRequest.getChatId());
            
            // Si hay menos de 3 participantes y no hay más solicitudes pendientes, eliminar el grupo
            if (participantCount < 3 && pendingRequests.size() <= 1) {
                dbManager.deleteChat(selectedRequest.getChatId());
            }
            
            // Eliminar la solicitud de la lista
            groupRequests.remove(selectedRequest);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    // Clases para mostrar las solicitudes en las listas
    private class FriendRequestListCell extends ListCell<FriendRequest> {
        @Override
        protected void updateItem(FriendRequest request, boolean empty) {
            super.updateItem(request, empty);
            
            if (empty || request == null) {
                setText(null);
            } else {
                setText(request.getSenderUsername() + " quiere ser tu amigo");
            }
        }
    }

    private class GroupRequestListCell extends ListCell<GroupRequest> {
        @Override
        protected void updateItem(GroupRequest request, boolean empty) {
            super.updateItem(request, empty);
            
            if (empty || request == null) {
                setText(null);
            } else {
                setText(request.getSenderUsername() + " te invita al grupo " + request.getChatName());
            }
        }
    }

    // Método auxiliar para mostrar alertas
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
} 