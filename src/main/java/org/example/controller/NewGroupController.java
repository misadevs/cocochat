package org.example.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.database.DatabaseManager;
import org.example.model.Chat;
import org.example.model.GroupRequest;
import org.example.model.User;

import java.sql.SQLException;
import java.util.List;

public class NewGroupController {

    @FXML
    private TextField groupNameField;

    @FXML
    private TextField searchField;

    @FXML
    private ListView<User> usersListView;

    @FXML
    private ListView<User> selectedUsersListView;

    @FXML
    private Button addParticipantButton;

    @FXML
    private Button removeParticipantButton;

    @FXML
    private Button createButton;

    @FXML
    private Button cancelButton;

    private DatabaseManager dbManager;
    private User currentUser;
    private ObservableList<User> searchedUsers = FXCollections.observableArrayList();
    private ObservableList<User> selectedUsers = FXCollections.observableArrayList();

    public void initialize() {
        dbManager = DatabaseManager.getInstance();
        currentUser = UserSession.getInstance().getCurrentUser();
        
        // Configurar las listas
        usersListView.setItems(searchedUsers);
        selectedUsersListView.setItems(selectedUsers);
        
        // Configurar los cell factories
        usersListView.setCellFactory(param -> new UserListCell());
        selectedUsersListView.setCellFactory(param -> new UserListCell());
        
        // Configurar los listeners para habilitar/deshabilitar botones
        usersListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    addParticipantButton.setDisable(newValue == null);
                });
        
        selectedUsersListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    removeParticipantButton.setDisable(newValue == null);
                });
        
        // Configurar listener para habilitar/deshabilitar el botón de crear grupo
        selectedUsers.addListener((javafx.collections.ListChangeListener.Change<? extends User> c) -> {
            // Se requieren al menos 2 participantes para crear un grupo (más el usuario actual)
            createButton.setDisable(selectedUsers.size() < 2 || groupNameField.getText().trim().isEmpty());
        });
        
        groupNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            createButton.setDisable(selectedUsers.size() < 2 || newValue.trim().isEmpty());
        });
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
            
            if (user != null && user.getId() != currentUser.getId() && !selectedUsers.contains(user)) {
                searchedUsers.add(user);
            }
            
            if (searchedUsers.isEmpty()) {
                showAlert("Búsqueda", "No se encontraron usuarios con ese nombre o ya están seleccionados.");
            }
        } catch (SQLException e) {
            showAlert("Error", "Error al buscar usuarios.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddParticipant() {
        User selectedUser = usersListView.getSelectionModel().getSelectedItem();
        
        if (selectedUser == null) {
            return;
        }
        
        // Agregar a la lista de seleccionados
        if (!selectedUsers.contains(selectedUser)) {
            selectedUsers.add(selectedUser);
        }
        
        // Quitar de la lista de búsqueda
        searchedUsers.remove(selectedUser);
    }

    @FXML
    private void handleRemoveParticipant() {
        User selectedUser = selectedUsersListView.getSelectionModel().getSelectedItem();
        
        if (selectedUser == null) {
            return;
        }
        
        // Quitar de la lista de seleccionados
        selectedUsers.remove(selectedUser);
    }

    @FXML
    private void handleCreate() {
        String groupName = groupNameField.getText().trim();
        
        if (groupName.isEmpty() || selectedUsers.size() < 2) {
            showAlert("Error", "Ingresa un nombre para el grupo y selecciona al menos 2 participantes.");
            return;
        }
        
        try {
            // Crear el grupo
            Chat groupChat = dbManager.createChat(groupName, Chat.ChatType.GROUP, currentUser.getId());
            
            // Agregar al creador como participante
            dbManager.addParticipantToChat(groupChat.getId(), currentUser.getId());
            
            // Enviar invitaciones a los participantes seleccionados
            for (User user : selectedUsers) {
                GroupRequest request = new GroupRequest(groupChat.getId(), currentUser.getId(), user.getId());
                dbManager.sendGroupRequest(request);
            }
            
            showAlert("Grupo creado", "El grupo se ha creado y se han enviado las invitaciones a los participantes.");
            
            // Cerrar la ventana
            closeWindow();
        } catch (SQLException e) {
            showAlert("Error", "No se pudo crear el grupo.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Clase para mostrar los usuarios en las listas
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