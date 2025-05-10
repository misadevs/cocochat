package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.example.database.DatabaseManager;
import org.example.model.Chat;
import org.example.model.GroupRequest;
import org.example.model.User;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AddParticipantController {

    @FXML
    private Text groupNameText;

    @FXML
    private TextField searchField;

    @FXML
    private ListView<User> usersListView;

    @FXML
    private Button inviteButton;

    @FXML
    private Button closeButton;

    private DatabaseManager dbManager;
    private User currentUser;
    private Chat chat;
    private ObservableList<User> searchedUsers = FXCollections.observableArrayList();

    public void initialize() {
        dbManager = DatabaseManager.getInstance();
        currentUser = UserSession.getInstance().getCurrentUser();
        
        // Configurar la lista de usuarios
        usersListView.setItems(searchedUsers);
        usersListView.setCellFactory(param -> new UserListCell());
        
        // Configurar el listener para habilitar/deshabilitar el botón de invitar
        usersListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    inviteButton.setDisable(newValue == null);
                });
    }

    public void setChat(Chat chat) {
        this.chat = chat;
        groupNameText.setText(chat.getName());
    }

    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText().trim();
        
        if (searchTerm.isEmpty()) {
            return;
        }
        
        try {
            // Buscar por nombre de usuario
            User user = dbManager.getUserByUsername(searchTerm);
            
            searchedUsers.clear();
            
            if (user != null && isValidParticipant(user)) {
                searchedUsers.add(user);
            }
            
            if (searchedUsers.isEmpty()) {
                showAlert("Búsqueda", "No se encontraron usuarios con ese nombre o ya son participantes del grupo.");
            }
        } catch (SQLException e) {
            showAlert("Error", "Error al buscar usuarios.");
            e.printStackTrace();
        }
    }

    private boolean isValidParticipant(User user) {
        // No puede ser el usuario actual
        if (user.getId() == currentUser.getId()) {
            return false;
        }
        
        // No puede ser ya un participante del grupo
        for (User participant : chat.getParticipants()) {
            if (participant.getId() == user.getId()) {
                return false;
            }
        }
        
        return true;
    }

    @FXML
    private void handleInvite() {
        User selectedUser = usersListView.getSelectionModel().getSelectedItem();
        
        if (selectedUser == null) {
            return;
        }
        
        try {
            // Crear la solicitud de grupo
            GroupRequest request = new GroupRequest(chat.getId(), currentUser.getId(), selectedUser.getId());
            dbManager.sendGroupRequest(request);
            
            showAlert("Invitación enviada", "Se ha enviado una invitación a " + selectedUser.getUsername() + " para unirse al grupo.");
            
            // Eliminar el usuario de la lista de búsqueda
            searchedUsers.remove(selectedUser);
        } catch (SQLException e) {
            showAlert("Error", "No se pudo enviar la invitación.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClose() {
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
                setText(user.getUsername());
            }
        }
    }
} 