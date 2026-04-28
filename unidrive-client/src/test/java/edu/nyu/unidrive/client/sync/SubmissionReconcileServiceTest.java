package edu.nyu.unidrive.client.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.nyu.unidrive.client.storage.SyncStateRecord;
import edu.nyu.unidrive.client.storage.SyncStateRepository;
import edu.nyu.unidrive.common.model.SyncStatus;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SubmissionReconcileServiceTest {

    @Test
    void reconcileExistingSubmissionsCreatesPendingRowsForFilesWithoutState(@TempDir Path tempDir) throws Exception {
        Path submissionsDir = tempDir.resolve("Submissions");
        Files.createDirectories(submissionsDir);
        Path file1 = submissionsDir.resolve("A.java");
        Path file2 = submissionsDir.resolve("B.java");
        Files.writeString(file1, "class A {}\n");
        Files.writeString(file2, "class B {}\n");

        SyncStateRepository repository = new SyncStateRepository(tempDir.resolve("sync-state.db"));
        SubmissionReconcileService service = new SubmissionReconcileService(repository);

        service.reconcileExistingSubmissions(submissionsDir);

        Optional<SyncStateRecord> row1 = repository.findByLocalPath(file1);
        Optional<SyncStateRecord> row2 = repository.findByLocalPath(file2);
        assertTrue(row1.isPresent());
        assertTrue(row2.isPresent());
        assertEquals(SyncStatus.PENDING, row1.get().status());
        assertEquals(SyncStatus.PENDING, row2.get().status());
    }

    @Test
    void reconcileExistingSubmissionsDoesNotOverwriteExistingRow(@TempDir Path tempDir) throws Exception {
        Path submissionsDir = tempDir.resolve("Submissions");
        Files.createDirectories(submissionsDir);
        Path file1 = submissionsDir.resolve("A.java");
        Files.writeString(file1, "class A {}\n");

        SyncStateRepository repository = new SyncStateRepository(tempDir.resolve("sync-state.db"));
        repository.save(new SyncStateRecord(file1, "submission-1", "hash", SyncStatus.SYNCED, 123L));
        SubmissionReconcileService service = new SubmissionReconcileService(repository);

        service.reconcileExistingSubmissions(submissionsDir);

        Optional<SyncStateRecord> row1 = repository.findByLocalPath(file1);
        assertTrue(row1.isPresent());
        assertEquals("submission-1", row1.get().remoteId());
        assertEquals(SyncStatus.SYNCED, row1.get().status());
        assertEquals(123L, row1.get().lastSynced());
    }
}

