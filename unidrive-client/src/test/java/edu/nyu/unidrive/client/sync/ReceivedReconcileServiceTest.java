package edu.nyu.unidrive.client.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.nyu.unidrive.client.storage.ReceivedStateRecord;
import edu.nyu.unidrive.client.storage.ReceivedStateRepository;
import edu.nyu.unidrive.common.model.SyncStatus;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReceivedReconcileServiceTest {

    @Test
    void reconcileExistingReceivedFilesCreatesSyncedRowsForAssignmentsAndFeedbacks(@TempDir Path tempDir) throws Exception {
        Path assignmentsDir = Files.createDirectory(tempDir.resolve("Assignments"));
        Path feedbacksDir = Files.createDirectory(tempDir.resolve("Feedback"));
        Path assignmentFile = assignmentsDir.resolve("Assignment1.txt");
        Path feedbackFile = feedbacksDir.resolve("feedback.txt");
        Files.writeString(assignmentFile, "assignment body");
        Files.writeString(feedbackFile, "feedback body");

        ReceivedStateRepository repository = new ReceivedStateRepository(tempDir.resolve("received.db"));
        ReceivedReconcileService service = new ReceivedReconcileService(repository);

        service.reconcileExistingReceivedFiles(assignmentsDir, feedbacksDir);

        Optional<ReceivedStateRecord> assignmentRow = repository.findByLocalPath(assignmentFile);
        Optional<ReceivedStateRecord> feedbackRow = repository.findByLocalPath(feedbackFile);
        assertTrue(assignmentRow.isPresent());
        assertTrue(feedbackRow.isPresent());
        assertEquals(SyncStatus.SYNCED, assignmentRow.get().status());
        assertEquals(SyncStatus.SYNCED, feedbackRow.get().status());
        assertEquals(ReceivedReconcileService.SOURCE_ASSIGNMENTS, assignmentRow.get().source());
        assertEquals(ReceivedReconcileService.SOURCE_FEEDBACK, feedbackRow.get().source());
        assertTrue(assignmentRow.get().remoteId().startsWith("assignment:"));
        assertTrue(feedbackRow.get().remoteId().startsWith("feedback:"));
        assertTrue(assignmentRow.get().lastSynced() > 0L);
        assertTrue(feedbackRow.get().lastSynced() > 0L);
    }

    @Test
    void reconcileExistingReceivedFilesIgnoresDesktopIni(@TempDir Path tempDir) throws Exception {
        Path assignmentsDir = Files.createDirectory(tempDir.resolve("Assignments"));
        Path feedbacksDir = Files.createDirectory(tempDir.resolve("Feedback"));
        Path desktopIni = assignmentsDir.resolve("desktop.ini");
        Files.writeString(desktopIni, "[.ShellClassInfo]");

        ReceivedStateRepository repository = new ReceivedStateRepository(tempDir.resolve("received.db"));
        ReceivedReconcileService service = new ReceivedReconcileService(repository);

        service.reconcileExistingReceivedFiles(assignmentsDir, feedbacksDir);

        assertFalse(repository.findByLocalPath(desktopIni).isPresent());
        assertEquals(0, repository.findAll().size());
    }
}

