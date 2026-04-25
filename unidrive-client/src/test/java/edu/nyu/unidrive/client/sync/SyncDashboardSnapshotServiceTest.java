package edu.nyu.unidrive.client.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.nyu.unidrive.client.storage.SyncStateRecord;
import edu.nyu.unidrive.client.storage.SyncStateRepository;
import edu.nyu.unidrive.common.model.SyncStatus;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SyncDashboardSnapshotServiceTest {

    @Test
    void loadSnapshotSummarizesTrackedRowsByStatus(@TempDir Path tempDir) {
        SyncStateRepository repository = new SyncStateRepository(tempDir.resolve("sync-state.db"));
        repository.save(new SyncStateRecord(tempDir.resolve("Submissions/Pending.java"), null, null, SyncStatus.PENDING, 0L));
        repository.save(new SyncStateRecord(tempDir.resolve("Submissions/Uploading.java"), null, "hash-up", SyncStatus.UPLOADING, 0L));
        repository.save(new SyncStateRecord(tempDir.resolve("Submissions/Synced.java"), "submission-1", "hash-ok", SyncStatus.SYNCED, 100L));
        repository.save(new SyncStateRecord(tempDir.resolve("Submissions/Failed.java"), "submission-2", "hash-fail", SyncStatus.FAILED, 50L));

        SyncDashboardSnapshot snapshot = new SyncDashboardSnapshotService(repository).loadSnapshot();

        assertEquals(1, snapshot.pendingCount());
        assertEquals(1, snapshot.uploadingCount());
        assertEquals(1, snapshot.syncedCount());
        assertEquals(1, snapshot.failedCount());
        assertEquals(4, snapshot.rows().size());
        assertEquals("Pending.java", snapshot.rows().getFirst().localPath().getFileName().toString());
    }
}
