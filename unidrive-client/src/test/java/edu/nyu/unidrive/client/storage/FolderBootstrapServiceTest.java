package edu.nyu.unidrive.client.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FolderBootstrapServiceTest {

    @Test
    void bootstrapCreatesExpectedFoldersAndSyncStateDatabase(@TempDir Path tempDir) {
        FolderBootstrapService service = new FolderBootstrapService();

        ClientWorkspace workspace = service.bootstrap(tempDir);

        assertEquals(tempDir.resolve("Assignments"), workspace.assignmentsDirectory());
        assertEquals(tempDir.resolve("Submissions"), workspace.submissionsDirectory());
        assertEquals(tempDir.resolve("Feedback"), workspace.feedbackDirectory());
        assertEquals(tempDir.resolve("sync-state.db"), workspace.databasePath());
        assertTrue(Files.isDirectory(workspace.assignmentsDirectory()));
        assertTrue(Files.isDirectory(workspace.submissionsDirectory()));
        assertTrue(Files.isDirectory(workspace.feedbackDirectory()));
        assertTrue(Files.exists(workspace.databasePath()));
    }

    @Test
    void bootstrapIsIdempotentForExistingWorkspace(@TempDir Path tempDir) {
        FolderBootstrapService service = new FolderBootstrapService();

        ClientWorkspace first = service.bootstrap(tempDir);
        ClientWorkspace second = service.bootstrap(tempDir);

        assertEquals(first, second);
        assertTrue(Files.isDirectory(tempDir.resolve("Assignments")));
        assertTrue(Files.isDirectory(tempDir.resolve("Submissions")));
        assertTrue(Files.isDirectory(tempDir.resolve("Feedback")));
        assertTrue(Files.exists(tempDir.resolve("sync-state.db")));
    }
}
