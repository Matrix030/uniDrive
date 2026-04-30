package edu.nyu.unidrive.client.storage;

import edu.nyu.unidrive.common.workspace.MockCourseRegistry;
import java.nio.file.Files;
import java.nio.file.Path;

public final class InstructorFolderBootstrapService {

    private final MockCourseRegistry courseRegistry;

    public InstructorFolderBootstrapService() {
        this(new MockCourseRegistry());
    }

    public InstructorFolderBootstrapService(MockCourseRegistry courseRegistry) {
        this.courseRegistry = courseRegistry;
    }

    public InstructorWorkspace bootstrap(Path rootDirectory) {
        Path publishDirectory = rootDirectory.resolve("Publish");
        Path submissionsDirectory = rootDirectory.resolve("Submissions");
        Path feedbackDirectory = rootDirectory.resolve("Feedback");
        Path databasePath = rootDirectory.resolve("sync-state.db");

        try {
            Files.createDirectories(publishDirectory);
            Files.createDirectories(submissionsDirectory);
            Files.createDirectories(feedbackDirectory);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to create instructor workspace folders.", exception);
        }

        WorkspaceLayout.createTermAndCourses(rootDirectory, courseRegistry);

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
