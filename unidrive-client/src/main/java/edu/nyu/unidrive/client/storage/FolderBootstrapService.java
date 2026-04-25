package edu.nyu.unidrive.client.storage;

import java.nio.file.Files;
import java.nio.file.Path;

public final class FolderBootstrapService {

    public ClientWorkspace bootstrap(Path rootDirectory) {
        Path assignmentsDirectory = rootDirectory.resolve("Assignments");
        Path submissionsDirectory = rootDirectory.resolve("Submissions");
        Path feedbackDirectory = rootDirectory.resolve("Feedback");
        Path databasePath = rootDirectory.resolve("sync-state.db");

        try {
            Files.createDirectories(assignmentsDirectory);
            Files.createDirectories(submissionsDirectory);
            Files.createDirectories(feedbackDirectory);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to create client workspace folders.", exception);
        }

        new SyncStateRepository(databasePath);

        return new ClientWorkspace(
            rootDirectory,
            assignmentsDirectory,
            submissionsDirectory,
            feedbackDirectory,
            databasePath
        );
    }
}
