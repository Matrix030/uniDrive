package edu.nyu.unidrive.client.storage;

import edu.nyu.unidrive.common.workspace.MockCourseRegistry;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FolderBootstrapService implements WorkspaceBootstrapService {

    private final MockCourseRegistry courseRegistry;

    public FolderBootstrapService() {
        this(new MockCourseRegistry());
    }

    public FolderBootstrapService(MockCourseRegistry courseRegistry) {
        this.courseRegistry = courseRegistry;
    }

    @Override
    public ClientWorkspace bootstrap(Path rootDirectory) {
        Path databasePath = rootDirectory.resolve("sync-state.db");
        try {
            Files.createDirectories(rootDirectory);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to create workspace root.", exception);
        }
        WorkspaceLayout.createTermAndCourses(rootDirectory, courseRegistry);
        new SyncStateRepository(databasePath);
        return new ClientWorkspace(rootDirectory, databasePath);
    }
}
