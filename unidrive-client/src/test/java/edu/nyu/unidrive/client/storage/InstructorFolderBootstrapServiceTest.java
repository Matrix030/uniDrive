package edu.nyu.unidrive.client.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InstructorFolderBootstrapServiceTest {

    @Test
    void bootstrapCreatesRootAndDatabase(@TempDir Path tempDir) {
        InstructorWorkspace workspace = new InstructorFolderBootstrapService().bootstrap(tempDir);

        assertEquals(tempDir, workspace.rootDirectory());
        assertTrue(Files.exists(workspace.databasePath()));
    }

    @Test
    void bootstrapIsIdempotent(@TempDir Path tempDir) {
        InstructorFolderBootstrapService service = new InstructorFolderBootstrapService();
        service.bootstrap(tempDir);
        InstructorWorkspace workspace = service.bootstrap(tempDir);

        assertTrue(Files.exists(workspace.databasePath()));
    }

    @Test
    void bootstrapSeedsTermAndCourseDirectories(@TempDir Path tempDir) {
        new InstructorFolderBootstrapService().bootstrap(tempDir);

        Path termRoot = tempDir.resolve("fall2026");
        assertTrue(Files.isDirectory(termRoot));
        assertTrue(Files.isDirectory(termRoot.resolve("daa")));
        assertTrue(Files.isDirectory(termRoot.resolve("java")));
        assertTrue(Files.isDirectory(termRoot.resolve("bda")));
    }
}
