package edu.nyu.unidrive.client.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.nyu.unidrive.client.storage.SyncStateRecord;
import edu.nyu.unidrive.client.storage.SyncStateRepository;
import edu.nyu.unidrive.common.model.SyncStatus;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SubmissionSyncStateServiceTest {

    @Test
    void recordPendingEventCreatesPendingRowForNewSubmission(@TempDir Path tempDir) {
        SyncStateRepository repository = new SyncStateRepository(tempDir.resolve("sync-state.db"));
        SubmissionSyncStateService service = new SubmissionSyncStateService(repository);
        Path localPath = tempDir.resolve("Submissions/Hello.java");

        service.recordPendingEvent(new SubmissionFileEvent(localPath, SubmissionFileEventType.CREATED));

        Optional<SyncStateRecord> record = repository.findByLocalPath(localPath);
        assertTrue(record.isPresent());
        assertEquals(localPath, record.get().localPath());
        assertNull(record.get().remoteId());
        assertNull(record.get().sha256());
        assertEquals(SyncStatus.PENDING, record.get().status());
        assertEquals(0L, record.get().lastSynced());
    }

    @Test
    void recordPendingEventPreservesRemoteIdAndLastSyncedForExistingRow(@TempDir Path tempDir) {
        SyncStateRepository repository = new SyncStateRepository(tempDir.resolve("sync-state.db"));
        SubmissionSyncStateService service = new SubmissionSyncStateService(repository);
        Path localPath = tempDir.resolve("Submissions/Hello.java");

        repository.save(new SyncStateRecord(localPath, "submission-1", "oldhash", SyncStatus.SYNCED, 1234L));

        service.recordPendingEvent(new SubmissionFileEvent(localPath, SubmissionFileEventType.MODIFIED));

        Optional<SyncStateRecord> record = repository.findByLocalPath(localPath);
        assertTrue(record.isPresent());
        assertEquals("submission-1", record.get().remoteId());
        assertEquals("oldhash", record.get().sha256());
        assertEquals(SyncStatus.PENDING, record.get().status());
        assertEquals(1234L, record.get().lastSynced());
    }
}
