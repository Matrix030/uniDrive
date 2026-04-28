package edu.nyu.unidrive.client.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InstructorFolderBootstrapServiceTest {

    @Test
    void bootstrapCreatesPublishSubmissionsAndFeedbackDirectories(@TempDir Path tempDir) {
        InstructorWorkspace workspace = new InstructorFolderBootstrapService().bootstrap(tempDir);

        assertEquals(tempDir, workspace.rootDirectory());
        assertTrue(Files.isDirectory(workspace.publishDirectory()));
        assertTrue(Files.isDirectory(workspace.submissionsDirectory()));
        assertTrue(Files.isDirectory(workspace.feedbackDirectory()));
        assertTrue(Files.exists(workspace.databasePath()));
        assertEquals(tempDir.resolve("Publish"), workspace.publishDirectory());
        assertEquals(tempDir.resolve("Submissions"), workspace.submissionsDirectory());
        assertEquals(tempDir.resolve("Feedbacks"), workspace.feedbackDirectory());
    }

    @Test
    void bootstrapIsIdempotent(@TempDir Path tempDir) {
        InstructorFolderBootstrapService service = new InstructorFolderBootstrapService();
        service.bootstrap(tempDir);
        InstructorWorkspace workspace = service.bootstrap(tempDir);

        assertTrue(Files.isDirectory(workspace.publishDirectory()));
        assertTrue(Files.exists(workspace.databasePath()));
    }
}
