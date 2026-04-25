package edu.nyu.unidrive.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class UniDriveClientApplication extends Application {

    @Override
    public void start(Stage stage) {
        Label label = new Label("University Drive client bootstrap");
        Scene scene = new Scene(new StackPane(label), 640, 360);

        stage.setTitle("University Drive");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
