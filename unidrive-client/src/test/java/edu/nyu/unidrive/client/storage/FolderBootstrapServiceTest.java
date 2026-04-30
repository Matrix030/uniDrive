package edu.nyu.unidrive.client.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FolderBootstrapServiceTest {

    @Test
    void bootstrapCreatesRootAndDatabase(@TempDir Path tempDir) {
        FolderBootstrapService service = new FolderBootstrapService();

        ClientWorkspace workspace = service.bootstrap(tempDir);

        assertEquals(tempDir, workspace.rootDirectory());
        assertEquals(tempDir.resolve("sync-state.db"), workspace.databasePath());
        assertTrue(Files.exists(workspace.databasePath()));
    }

    @Test
    void bootstrapIsIdempotentForExistingWorkspace(@TempDir Path tempDir) {
        FolderBootstrapService service = new FolderBootstrapService();

        ClientWorkspace first = service.bootstrap(tempDir);
        ClientWorkspace second = service.bootstrap(tempDir);

        assertEquals(first, second);
        assertTrue(Files.exists(tempDir.resolve("sync-state.db")));
    }

    @Test
    void bootstrapSeedsTermAndCourseDirectories(@TempDir Path tempDir) {
        new FolderBootstrapService().bootstrap(tempDir);

        Path termRoot = tempDir.resolve("fall2026");
        assertTrue(Files.isDirectory(termRoot));
        assertTrue(Files.isDirectory(termRoot.resolve("daa")));
        assertTrue(Files.isDirectory(termRoot.resolve("java")));
        assertTrue(Files.isDirectory(termRoot.resolve("bda")));
    }
}
