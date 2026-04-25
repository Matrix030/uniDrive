package edu.nyu.unidrive.client;

import edu.nyu.unidrive.client.session.SessionPersistence;
import edu.nyu.unidrive.client.ui.SceneNavigator;
import javafx.application.Application;
import javafx.stage.Stage;

public class UniDriveClientApplication extends Application {

    @Override
    public void start(Stage stage) {
        SessionPersistence persistence = new SessionPersistence();
        SceneNavigator navigator = new SceneNavigator(
            stage,
            persistence,
            System.getProperty("unidrive.serverBaseUrl", "http://localhost:8080")
        );
        persistence.load().ifPresentOrElse(
            navigator::showDashboardScene,
            navigator::showLoginScene
        );
    }

    public static void main(String[] args) {
        launch(args);
    }
}
