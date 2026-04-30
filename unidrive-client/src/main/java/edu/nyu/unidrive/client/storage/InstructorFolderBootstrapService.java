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
        Path databasePath = rootDirectory.resolve("sync-state.db");
        try {
            Files.createDirectories(rootDirectory);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to create workspace root.", exception);
        }
        WorkspaceLayout.createTermAndCourses(rootDirectory, courseRegistry);
        new SyncStateRepository(databasePath);
        return new InstructorWorkspace(rootDirectory, databasePath);
    }
}
