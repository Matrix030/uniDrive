package edu.nyu.unidrive.client.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import edu.nyu.unidrive.client.storage.SyncStateRecord;
import edu.nyu.unidrive.client.storage.SyncStateRepository;
import edu.nyu.unidrive.common.model.SyncStatus;
import edu.nyu.unidrive.common.util.FileHasher;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SubmissionReconcileServiceTest {

    @Test
    void reconcileMarksExistingSubmissionFilePendingAfterStartup(@TempDir Path tempDir) throws IOException {
        Path workspaceRoot = tempDir.resolve("workspace");
        Path submissionFile = workspaceRoot.resolve("fall2026/daa/hashing/submission/Solution.java");
        Files.createDirectories(submissionFile.getParent());
        Files.writeString(submissionFile, "class Solution {}");

        SyncStateRepository repository = new SyncStateRepository(tempDir.resolve("sync-state.db"));

        new SubmissionReconcileService(repository).reconcileExistingSubmissions(workspaceRoot);

        SyncStateRecord record = repository.findByLocalPath(submissionFile).orElseThrow();
        assertEquals(submissionFile, record.localPath());
        assertNull(record.remoteId());
        assertNull(record.sha256());
        assertEquals(SyncStatus.PENDING, record.status());
        assertEquals(0L, record.lastSynced());
    }

    @Test
    void reconcileMarksModifiedSyncedSubmissionPendingAgain(@TempDir Path tempDir) throws IOException {
        Path workspaceRoot = tempDir.resolve("workspace");
        Path submissionFile = workspaceRoot.resolve("fall2026/daa/hashing/submission/Solution.java");
        Files.createDirectories(submissionFile.getParent());
        Files.writeString(submissionFile, "class Solution {}");
        String originalHash = FileHasher.sha256Hex(submissionFile);

        SyncStateRepository repository = new SyncStateRepository(tempDir.resolve("sync-state.db"));
        repository.save(new SyncStateRecord(submissionFile, "submission-1", originalHash, SyncStatus.SYNCED, 1234L));
        Files.writeString(submissionFile, "class Solution { int changed; }");

        new SubmissionReconcileService(repository).reconcileExistingSubmissions(workspaceRoot);

        SyncStateRecord record = repository.findByLocalPath(submissionFile).orElseThrow();
        assertEquals("submission-1", record.remoteId());
        assertEquals(originalHash, record.sha256());
        assertEquals(SyncStatus.PENDING, record.status());
        assertEquals(1234L, record.lastSynced());
    }
}
