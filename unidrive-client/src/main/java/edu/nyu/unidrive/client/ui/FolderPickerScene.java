package edu.nyu.unidrive.client.ui;

import java.io.File;
import java.nio.file.Path;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public final class FolderPickerScene {

    private final Stage stage;
    private final FolderPickedCallback onPicked;

    public FolderPickerScene(Stage stage, FolderPickedCallback onPicked) {
        this.stage = stage;
        this.onPicked = onPicked;
    }

    public Scene build() {
        Label pathLabel = new Label("(no folder selected)");
        Path[] selected = new Path[1];

        Button browseButton = new Button("Browse...");
        browseButton.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Choose University Drive workspace folder");
            File chosen = chooser.showDialog(stage);
            if (chosen != null) {
                selected[0] = chosen.toPath();
                pathLabel.setText(chosen.getAbsolutePath());
            }
        });

        Label statusLabel = new Label();

        Button continueButton = new Button("Continue");
        continueButton.setOnAction(event -> {
            if (selected[0] == null) {
                statusLabel.setText("Please select a workspace folder.");
                return;
            }
            onPicked.onFolderPicked(selected[0]);
        });

        VBox root = new VBox(
            12,
            new Label("Choose a folder for your University Drive workspace."),
            new HBox(8, browseButton, pathLabel),
            continueButton,
            statusLabel
        );
        root.setPadding(new Insets(24));
        return new Scene(root, 520, 220);
    }

    @FunctionalInterface
    public interface FolderPickedCallback {
        void onFolderPicked(Path workspaceDirectory);
    }
}
