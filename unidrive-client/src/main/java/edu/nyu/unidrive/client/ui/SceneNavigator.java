package edu.nyu.unidrive.client.ui;

import edu.nyu.unidrive.client.net.RestAuthApiClient;
import edu.nyu.unidrive.client.session.SessionConfig;
import edu.nyu.unidrive.client.session.SessionPersistence;
import edu.nyu.unidrive.client.session.UserRole;
import javafx.stage.Stage;
import org.springframework.web.client.RestTemplate;

public final class SceneNavigator {

    private final Stage stage;
    private final SessionPersistence persistence;
    private final String baseUrl;
    private DashboardScene activeDashboard;

    public SceneNavigator(Stage stage, SessionPersistence persistence, String baseUrl) {
        this.stage = stage;
        this.persistence = persistence;
        this.baseUrl = baseUrl;
        stage.setOnCloseRequest(event -> shutdownDashboard());
    }

    public void showLoginScene() {
        shutdownDashboard();
        LoginScene scene = new LoginScene(
            new RestAuthApiClient(baseUrl, new RestTemplate()),
            (userId, role) -> showFolderPickerScene(userId, role)
        );
        stage.setTitle("University Drive — Login");
        stage.setScene(scene.build());
        stage.show();
    }

    public void showFolderPickerScene(String userId, UserRole role) {
        FolderPickerScene scene = new FolderPickerScene(stage, workspace -> {
            SessionConfig config = new SessionConfig(userId, role, workspace, baseUrl);
            persistence.save(config);
            showDashboardScene(config);
        });
        stage.setTitle("University Drive — Choose Workspace");
        stage.setScene(scene.build());
        stage.show();
    }

    public void showDashboardScene(SessionConfig config) {
        shutdownDashboard();
        activeDashboard = new DashboardScene(config, () -> {
            persistence.clear();
            showLoginScene();
        });
        stage.setTitle("University Drive — " + config.role().name() + " (" + config.userId() + ")");
        stage.setScene(activeDashboard.build());
        stage.show();
    }

    private void shutdownDashboard() {
        if (activeDashboard != null) {
            activeDashboard.shutdown();
            activeDashboard = null;
        }
    }
}
