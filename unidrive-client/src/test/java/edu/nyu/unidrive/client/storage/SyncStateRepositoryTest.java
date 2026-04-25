package edu.nyu.unidrive.client.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.nyu.unidrive.common.model.SyncStatus;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SyncStateRepositoryTest {

    @Test
    void initializesSchemaAndReturnsEmptyWhenPathHasNoRecord(@TempDir Path tempDir) {
        SyncStateRepository repository = new SyncStateRepository(tempDir.resolve("sync-state.db"));

        Optional<SyncStateRecord> result = repository.findByLocalPath(tempDir.resolve("Submissions/Hello.java"));

        assertTrue(repository.databaseExists());
        assertFalse(result.isPresent());
    }

    @Test
    void savePersistsAndLoadsSyncStateByLocalPath(@TempDir Path tempDir) {
        SyncStateRepository repository = new SyncStateRepository(tempDir.resolve("sync-state.db"));
        Path localPath = tempDir.resolve("Submissions/Hello.java");
        SyncStateRecord expected = new SyncStateRecord(
            localPath,
            "submission-1",
            "abc123",
            SyncStatus.PENDING,
            1714017600000L
        );

        repository.save(expected);

        Optional<SyncStateRecord> actual = repository.findByLocalPath(localPath);

        assertTrue(actual.isPresent());
        assertEquals(expected, actual.get());
    }

    @Test
    void saveUpdatesExistingRowForSameLocalPath(@TempDir Path tempDir) {
        SyncStateRepository repository = new SyncStateRepository(tempDir.resolve("sync-state.db"));
        Path localPath = tempDir.resolve("Submissions/Hello.java");

        repository.save(new SyncStateRecord(localPath, "submission-1", "abc123", SyncStatus.PENDING, 100L));
        repository.save(new SyncStateRecord(localPath, "submission-2", "def456", SyncStatus.SYNCED, 200L));

        Optional<SyncStateRecord> actual = repository.findByLocalPath(localPath);

        assertTrue(actual.isPresent());
        assertEquals("submission-2", actual.get().remoteId());
        assertEquals("def456", actual.get().sha256());
        assertEquals(SyncStatus.SYNCED, actual.get().status());
        assertEquals(200L, actual.get().lastSynced());
    }
}
