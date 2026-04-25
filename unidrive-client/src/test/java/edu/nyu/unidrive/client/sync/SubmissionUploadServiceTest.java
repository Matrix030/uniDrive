package edu.nyu.unidrive.client.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.nyu.unidrive.client.net.SubmissionApiClient;
import edu.nyu.unidrive.client.storage.SyncStateRecord;
import edu.nyu.unidrive.client.storage.SyncStateRepository;
import edu.nyu.unidrive.common.dto.SubmissionUploadResponse;
import edu.nyu.unidrive.common.model.SyncStatus;
import edu.nyu.unidrive.common.util.FileHasher;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SubmissionUploadServiceTest {

    @Test
    void uploadPendingSubmissionMarksRowSyncedOnSuccess(@TempDir Path tempDir) throws Exception {
        SyncStateRepository repository = new SyncStateRepository(tempDir.resolve("sync-state.db"));
        Path file = tempDir.resolve("Submissions/Hello.java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "class Hello {}\n");
        repository.save(new SyncStateRecord(file, null, null, SyncStatus.PENDING, 0L));

        RecordingSubmissionApiClient apiClient = new RecordingSubmissionApiClient();
        SubmissionUploadService service = new SubmissionUploadService(repository, apiClient);

        SyncStatus result = service.uploadPendingSubmission("assignment-1", "rvg9395", file);

        Optional<SyncStateRecord> savedRecord = repository.findByLocalPath(file);
        assertEquals(SyncStatus.SYNCED, result);
        assertTrue(savedRecord.isPresent());
        assertEquals("submission-1", savedRecord.get().remoteId());
        assertEquals(FileHasher.sha256Hex(file), savedRecord.get().sha256());
        assertEquals(SyncStatus.SYNCED, savedRecord.get().status());
        assertTrue(savedRecord.get().lastSynced() > 0L);
        assertEquals("assignment-1", apiClient.assignmentId);
        assertEquals("rvg9395", apiClient.studentId);
        assertEquals(file, apiClient.filePath);
        assertEquals(FileHasher.sha256Hex(file), apiClient.sha256);
    }

    @Test
    void uploadPendingSubmissionMarksRowFailedOnApiFailure(@TempDir Path tempDir) throws Exception {
        SyncStateRepository repository = new SyncStateRepository(tempDir.resolve("sync-state.db"));
        Path file = tempDir.resolve("Submissions/Hello.java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "class Hello {}\n");
        repository.save(new SyncStateRecord(file, "submission-old", "oldhash", SyncStatus.PENDING, 123L));

        SubmissionUploadService service = new SubmissionUploadService(repository, new FailingSubmissionApiClient());

        SyncStatus result = service.uploadPendingSubmission("assignment-1", "rvg9395", file);

        Optional<SyncStateRecord> savedRecord = repository.findByLocalPath(file);
        assertEquals(SyncStatus.FAILED, result);
        assertTrue(savedRecord.isPresent());
        assertEquals("submission-old", savedRecord.get().remoteId());
        assertEquals(FileHasher.sha256Hex(file), savedRecord.get().sha256());
        assertEquals(SyncStatus.FAILED, savedRecord.get().status());
        assertEquals(123L, savedRecord.get().lastSynced());
    }

    private static final class RecordingSubmissionApiClient implements SubmissionApiClient {
        private String assignmentId;
        private String studentId;
        private Path filePath;
        private String sha256;

        @Override
        public SubmissionUploadResponse uploadSubmission(String assignmentId, String studentId, Path filePath, String sha256) {
            this.assignmentId = assignmentId;
            this.studentId = studentId;
            this.filePath = filePath;
            this.sha256 = sha256;
            return new SubmissionUploadResponse("submission-1", assignmentId, studentId, filePath.getFileName().toString(), sha256);
        }
    }

    private static final class FailingSubmissionApiClient implements SubmissionApiClient {
        @Override
        public SubmissionUploadResponse uploadSubmission(String assignmentId, String studentId, Path filePath, String sha256)
            throws IOException {
            throw new IOException("server unavailable");
        }
    }
}
