package edu.nyu.unidrive.client.storage;

import java.nio.file.Files;
import java.nio.file.Path;

public final class InstructorFolderBootstrapService {

    public InstructorWorkspace bootstrap(Path rootDirectory) {
        Path publishDirectory = rootDirectory.resolve("Publish");
        Path submissionsDirectory = rootDirectory.resolve("Submissions");
        Path feedbackDirectory = rootDirectory.resolve("Feedbacks");
        Path databasePath = rootDirectory.resolve("sync-state.db");

        try {
            Files.createDirectories(publishDirectory);
            Files.createDirectories(submissionsDirectory);
            Files.createDirectories(feedbackDirectory);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to create instructor workspace folders.", exception);
        }

        new SyncStateRepository(databasePath);

        return new InstructorWorkspace(
            rootDirectory,
            publishDirectory,
            submissionsDirectory,
            feedbackDirectory,
            databasePath
        );
    }
}
