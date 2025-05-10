package org.example.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;
import org.example.App;
import org.example.database.DatabaseManager;
import org.example.model.User;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Button registerButton;
    
    @FXML
    private Label forgotPasswordLink;

    @FXML
    private Label statusLabel;

    private DatabaseManager dbManager;

    public void initialize() {
        dbManager = DatabaseManager.getInstance();
        
        // Estilizar el enlace de olvido de contraseña
        forgotPasswordLink.setStyle("-fx-text-fill: blue; -fx-underline: true; -fx-cursor: hand;");
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Por favor, completa todos los campos");
            return;
        }

        try {
            User user = dbManager.authenticateUser(username, password);
            if (user != null) {
                // Guardar el usuario actual en una sesión
                UserSession.getInstance().setCurrentUser(user);
                
                // Navegar a la pantalla principal
                App.setRoot("main");
            } else {
                statusLabel.setText("Usuario o contraseña incorrectos");
            }
        } catch (SQLException e) {
            statusLabel.setText("Error de conexión a la base de datos");
            e.printStackTrace();
        } catch (IOException e) {
            statusLabel.setText("Error al cargar la pantalla principal");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Por favor, completa todos los campos");
            return;
        }

        try {
            if (dbManager.userExists(username)) {
                statusLabel.setText("El nombre de usuario ya está en uso");
                return;
            }

            User newUser = new User(0, username, password);
            boolean success = dbManager.registerUser(newUser);

            if (success) {
                // Obtener el usuario con el ID asignado
                User user = dbManager.authenticateUser(username, password);
                
                // Guardar el usuario actual en una sesión
                UserSession.getInstance().setCurrentUser(user);
                
                // Navegar a la pantalla principal
                App.setRoot("main");
            } else {
                statusLabel.setText("Error al registrar el usuario");
            }
        } catch (SQLException e) {
            statusLabel.setText("Error de conexión a la base de datos");
            e.printStackTrace();
        } catch (IOException e) {
            statusLabel.setText("Error al cargar la pantalla principal");
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleForgotPassword(MouseEvent event) {
        // Crear el diálogo para recuperar la contraseña
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Recuperación de Contraseña");
        dialog.setHeaderText("Ingresa tu nombre de usuario para reiniciar tu contraseña");
        
        // Configurar los botones
        ButtonType resetButtonType = new ButtonType("Reiniciar Contraseña", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(resetButtonType, ButtonType.CANCEL);
        
        // Crear el contenido del diálogo
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField username = new TextField();
        username.setPromptText("Nombre de usuario");
        
        grid.add(new Label("Usuario:"), 0, 0);
        grid.add(username, 1, 0);
        
        dialog.getDialogPane().setContent(grid);
        
        // Enfocar el campo de usuario
        Platform.runLater(username::requestFocus);
        
        // Convertir el resultado del diálogo
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == resetButtonType) {
                return username.getText();
            }
            return null;
        });
        
        // Mostrar el diálogo y procesar el resultado
        Optional<String> result = dialog.showAndWait();
        
        result.ifPresent(this::processPasswordReset);
    }
    
    private void processPasswordReset(String username) {
        if (username == null || username.trim().isEmpty()) {
            showAlert("Error", "Por favor, ingresa un nombre de usuario válido.");
            return;
        }
        
        try {
            // Verificar si el usuario existe
            if (!dbManager.userExists(username.trim())) {
                showAlert("Usuario no encontrado", "No se encontró ningún usuario con ese nombre.");
                return;
            }
            
            // Crear un diálogo para establecer una nueva contraseña
            Dialog<Pair<String, String>> passwordDialog = new Dialog<>();
            passwordDialog.setTitle("Nueva Contraseña");
            passwordDialog.setHeaderText("Establece tu nueva contraseña");
            
            // Configurar los botones
            ButtonType saveButtonType = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
            passwordDialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
            
            // Crear el contenido del diálogo
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));
            
            PasswordField newPassword = new PasswordField();
            newPassword.setPromptText("Nueva contraseña");
            PasswordField confirmPassword = new PasswordField();
            confirmPassword.setPromptText("Confirmar contraseña");
            
            grid.add(new Label("Nueva contraseña:"), 0, 0);
            grid.add(newPassword, 1, 0);
            grid.add(new Label("Confirmar contraseña:"), 0, 1);
            grid.add(confirmPassword, 1, 1);
            
            passwordDialog.getDialogPane().setContent(grid);
            
            // Enfocar el campo de contraseña
            Platform.runLater(newPassword::requestFocus);
            
            // Deshabilitar el botón de guardar hasta que ambas contraseñas coincidan
            Node saveButton = passwordDialog.getDialogPane().lookupButton(saveButtonType);
            saveButton.setDisable(true);
            
            // Validar las contraseñas en tiempo real
            newPassword.textProperty().addListener((observable, oldValue, newValue) -> {
                saveButton.setDisable(newValue.trim().isEmpty() || 
                                     !newValue.equals(confirmPassword.getText().trim()));
            });
            
            confirmPassword.textProperty().addListener((observable, oldValue, newValue) -> {
                saveButton.setDisable(newValue.trim().isEmpty() || 
                                     !newValue.equals(newPassword.getText().trim()));
            });
            
            // Convertir el resultado del diálogo
            passwordDialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    return new Pair<>(username.trim(), newPassword.getText().trim());
                }
                return null;
            });
            
            // Mostrar el diálogo y procesar el resultado
            Optional<Pair<String, String>> passwordResult = passwordDialog.showAndWait();
            
            passwordResult.ifPresent(userPassword -> {
                try {
                    // Actualizar la contraseña en la base de datos
                    boolean success = dbManager.updateUserPassword(userPassword.getKey(), userPassword.getValue());
                    if (success) {
                        showAlert("Éxito", "La contraseña se ha actualizado correctamente.");
                        // Llenar automáticamente el nombre de usuario en el formulario de inicio de sesión
                        usernameField.setText(userPassword.getKey());
                        passwordField.clear();
                        passwordField.requestFocus();
                    } else {
                        showAlert("Error", "No se pudo actualizar la contraseña.");
                    }
                } catch (SQLException e) {
                    showAlert("Error", "Error de conexión a la base de datos: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            
        } catch (SQLException e) {
            showAlert("Error", "Error de conexión a la base de datos: " + e.getMessage());
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
} 