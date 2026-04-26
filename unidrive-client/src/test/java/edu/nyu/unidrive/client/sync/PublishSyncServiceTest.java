package edu.nyu.unidrive.client.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.nyu.unidrive.client.net.AssignmentApiClient;
import edu.nyu.unidrive.client.net.DownloadedFile;
import edu.nyu.unidrive.client.storage.SyncStateRecord;
import edu.nyu.unidrive.client.storage.SyncStateRepository;
import edu.nyu.unidrive.common.dto.AssignmentSummaryResponse;
import edu.nyu.unidrive.common.model.SyncStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PublishSyncServiceTest {

    @Test
    void processOnceWritesSyncedRecordAfterSuccessfulUpload(@TempDir Path tempDir) throws Exception {
        Path publishDir = Files.createDirectory(tempDir.resolve("Publish"));
        SyncStateRepository repo = new SyncStateRepository(tempDir.resolve("sync-state.db"));

        try (PublishDirectoryWatcher watcher = new PublishDirectoryWatcher(publishDir)) {
            PublishUploadService uploadService = new PublishUploadService(new StubAssignmentApiClient("asgn-1", "hash-abc"));
            PublishSyncService service = new PublishSyncService(watcher, uploadService, repo, Duration.ofSeconds(2));

            Path file = publishDir.resolve("Homework1.txt");
            Files.writeString(file, "content");

            service.processOnce();

            Optional<SyncStateRecord> record = repo.findByLocalPath(file);
            assertTrue(record.isPresent());
            assertEquals(SyncStatus.SYNCED, record.get().status());
            assertEquals("asgn-1", record.get().remoteId());
            assertEquals("hash-abc", record.get().sha256());
        }
    }

    @Test
    void processOnceWritesFailedRecordWhenUploadThrows(@TempDir Path tempDir) throws Exception {
        Path publishDir = Files.createDirectory(tempDir.resolve("Publish"));
        SyncStateRepository repo = new SyncStateRepository(tempDir.resolve("sync-state.db"));

        try (PublishDirectoryWatcher watcher = new PublishDirectoryWatcher(publishDir)) {
            PublishUploadService uploadService = new PublishUploadService(new FailingAssignmentApiClient());
            PublishSyncService service = new PublishSyncService(watcher, uploadService, repo, Duration.ofSeconds(2));

            Path file = publishDir.resolve("Homework1.txt");
            Files.writeString(file, "content");

            service.processOnce();

            Optional<SyncStateRecord> record = repo.findByLocalPath(file);
            assertTrue(record.isPresent());
            assertEquals(SyncStatus.FAILED, record.get().status());
        }
    }

    private static final class StubAssignmentApiClient implements AssignmentApiClient {
        private final String assignmentId;
        private final String sha256;

        StubAssignmentApiClient(String assignmentId, String sha256) {
            this.assignmentId = assignmentId;
            this.sha256 = sha256;
        }

        @Override
        public List<AssignmentSummaryResponse> listAssignments() {
            return List.of();
        }

        @Override
        public DownloadedFile downloadAssignment(String id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AssignmentSummaryResponse publishAssignment(String title, Path file) {
            return new AssignmentSummaryResponse(assignmentId, title, file.getFileName().toString(), sha256);
        }
    }

    private static final class FailingAssignmentApiClient implements AssignmentApiClient {
        @Override
        public List<AssignmentSummaryResponse> listAssignments() {
            return List.of();
        }

        @Override
        public DownloadedFile downloadAssignment(String id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AssignmentSummaryResponse publishAssignment(String title, Path file) throws IOException {
            throw new IOException("upload failed");
        }
    }
}
