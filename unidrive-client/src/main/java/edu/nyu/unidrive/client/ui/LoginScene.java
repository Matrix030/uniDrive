package edu.nyu.unidrive.client.ui;

import edu.nyu.unidrive.client.net.AuthApiClient;
import edu.nyu.unidrive.client.session.UserRole;
import edu.nyu.unidrive.common.dto.LoginResponse;
import java.io.IOException;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class LoginScene {

    private final AuthApiClient authApiClient;
    private final LoginCallback onSuccess;

    public LoginScene(AuthApiClient authApiClient, LoginCallback onSuccess) {
        this.authApiClient = authApiClient;
        this.onSuccess = onSuccess;
    }

    public Scene build() {
        TextField userIdField = new TextField();
        userIdField.setPromptText("Enter your user ID");

        ToggleGroup roleGroup = new ToggleGroup();
        RadioButton studentRadio = new RadioButton("Student");
        studentRadio.setToggleGroup(roleGroup);
        studentRadio.setSelected(true);
        studentRadio.setUserData(UserRole.STUDENT);

        RadioButton instructorRadio = new RadioButton("Instructor");
        instructorRadio.setToggleGroup(roleGroup);
        instructorRadio.setUserData(UserRole.INSTRUCTOR);

        Label statusLabel = new Label();

        Button loginButton = new Button("Login");
        loginButton.setOnAction(event -> {
            String userId = userIdField.getText() == null ? "" : userIdField.getText().trim();
            if (userId.isEmpty()) {
                statusLabel.setText("Please enter a user ID.");
                return;
            }
            UserRole role = (UserRole) roleGroup.getSelectedToggle().getUserData();
            statusLabel.setText("Authenticating...");
            try {
                LoginResponse response = authApiClient.login(userId, role.name());
                onSuccess.onLogin(response.userId(), UserRole.valueOf(response.role()));
            } catch (IOException | RuntimeException exception) {
                statusLabel.setText("Login failed: " + exception.getMessage());
            }
        });

        VBox root = new VBox(
            12,
            new Label("University Drive"),
            new Label("User ID:"),
            userIdField,
            new Label("Role:"),
            new HBox(12, studentRadio, instructorRadio),
            loginButton,
            statusLabel
        );
        root.setPadding(new Insets(24));
        return new Scene(root, 420, 320);
    }

    @FunctionalInterface
    public interface LoginCallback {
        void onLogin(String userId, UserRole role);
    }
}
