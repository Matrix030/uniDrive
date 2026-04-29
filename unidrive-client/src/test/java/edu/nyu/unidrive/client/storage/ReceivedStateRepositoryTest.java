package edu.nyu.unidrive.client.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.nyu.unidrive.common.model.SyncStatus;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReceivedStateRepositoryTest {

    @Test
    void saveAndFindByLocalPathRoundTripsAllFields(@TempDir Path tempDir) {
        ReceivedStateRepository repository = new ReceivedStateRepository(tempDir.resolve("received.db"));
        ReceivedStateRecord record = new ReceivedStateRecord(
            tempDir.resolve("Assignments").resolve("Assignment1.txt"),
            "assignment-1",
            "abc123",
            SyncStatus.SYNCED,
            123456789L,
            "ASSIGNMENTS"
        );

        repository.save(record);

        ReceivedStateRecord loaded = repository.findByLocalPath(record.localPath()).orElseThrow();
        assertEquals(record.localPath(), loaded.localPath());
        assertEquals(record.remoteId(), loaded.remoteId());
        assertEquals(record.sha256(), loaded.sha256());
        assertEquals(record.status(), loaded.status());
        assertEquals(record.lastSynced(), loaded.lastSynced());
        assertEquals(record.source(), loaded.source());
    }

    @Test
    void saveUpdatesExistingRowByLocalPath(@TempDir Path tempDir) {
        ReceivedStateRepository repository = new ReceivedStateRepository(tempDir.resolve("received.db"));
        Path localPath = tempDir.resolve("Feedback").resolve("Feedback.txt");
        repository.save(new ReceivedStateRecord(localPath, "feedback-1", "hash-1", SyncStatus.PENDING, 0L, "FEEDBACK"));
        repository.save(new ReceivedStateRecord(localPath, "feedback-2", "hash-2", SyncStatus.SYNCED, 55L, "FEEDBACK"));

        ReceivedStateRecord loaded = repository.findByLocalPath(localPath).orElseThrow();
        assertEquals("feedback-2", loaded.remoteId());
        assertEquals("hash-2", loaded.sha256());
        assertEquals(SyncStatus.SYNCED, loaded.status());
        assertEquals(55L, loaded.lastSynced());
        assertEquals("FEEDBACK", loaded.source());
        assertEquals(1, repository.findAll().size());
        assertTrue(repository.findAll().stream().allMatch(row -> row.localPath().equals(localPath)));
    }
}

