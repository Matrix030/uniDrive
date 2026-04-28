package edu.nyu.unidrive.client.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.nyu.unidrive.client.net.SubmissionApiClient;
import edu.nyu.unidrive.client.storage.SyncStateRecord;
import edu.nyu.unidrive.client.storage.SyncStateRepository;
import edu.nyu.unidrive.common.dto.SubmissionUploadResponse;
import edu.nyu.unidrive.common.model.SyncStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SyncServiceTest {

    @Test
    void startProcessesWatcherEventAndMarksSubmissionSynced(@TempDir Path tempDir) throws Exception {
        SyncStateRepository repository = new SyncStateRepository(tempDir.resolve("sync-state.db"));
        Path file = tempDir.resolve("Submissions/Hello.java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "class Hello {}\n");

        OneShotEventSource eventSource = new OneShotEventSource(new SubmissionFileEvent(file, SubmissionFileEventType.CREATED));
        SubmissionSyncStateService syncStateService = new SubmissionSyncStateService(repository);
        SubmissionUploadService uploadService = new SubmissionUploadService(repository, new SuccessfulApiClient());
        SubmissionReconcileService reconcileService = new SubmissionReconcileService(repository);

        try (SyncService syncService = new SyncService(
            eventSource,
            syncStateService,
            uploadService,
            reconcileService,
            repository,
            file.getParent(),
            "assignment-1",
            "rvg9395",
            Duration.ofMillis(25)
        )) {
            syncService.start();

            SyncStateRecord savedRecord = awaitRecord(repository, file, SyncStatus.SYNCED);

            assertEquals("submission-1", savedRecord.remoteId());
            assertEquals(SyncStatus.SYNCED, savedRecord.status());
            assertTrue(savedRecord.lastSynced() > 0L);
        }
    }

    @Test
    void startProcessesWatcherEventAndMarksSubmissionFailedOnUploadError(@TempDir Path tempDir) throws Exception {
        SyncStateRepository repository = new SyncStateRepository(tempDir.resolve("sync-state.db"));
        Path file = tempDir.resolve("Submissions/Hello.java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "class Hello {}\n");

        OneShotEventSource eventSource = new OneShotEventSource(new SubmissionFileEvent(file, SubmissionFileEventType.CREATED));
        SubmissionSyncStateService syncStateService = new SubmissionSyncStateService(repository);
        SubmissionUploadService uploadService = new SubmissionUploadService(repository, new FailingApiClient());
        SubmissionReconcileService reconcileService = new SubmissionReconcileService(repository);

        try (SyncService syncService = new SyncService(
            eventSource,
            syncStateService,
            uploadService,
            reconcileService,
            repository,
            file.getParent(),
            "assignment-1",
            "rvg9395",
            Duration.ofMillis(25)
        )) {
            syncService.start();

            SyncStateRecord savedRecord = awaitRecord(repository, file, SyncStatus.FAILED);

            assertEquals(SyncStatus.FAILED, savedRecord.status());
        }
    }

    private SyncStateRecord awaitRecord(SyncStateRepository repository, Path localPath, SyncStatus expectedStatus)
        throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();

        while (System.nanoTime() < deadline) {
            Optional<SyncStateRecord> record = repository.findByLocalPath(localPath);
            if (record.isPresent() && record.get().status() == expectedStatus) {
                return record.get();
            }
            Thread.sleep(25L);
        }

        throw new AssertionError("Timed out waiting for sync state " + expectedStatus + " for " + localPath);
    }

    private static final class OneShotEventSource implements SubmissionEventSource {
        private final SubmissionFileEvent event;
        private boolean emitted;

        private OneShotEventSource(SubmissionFileEvent event) {
            this.event = event;
        }

        @Override
        public List<SubmissionFileEvent> pollEvents(Duration timeout) {
            if (emitted) {
                return List.of();
            }
            emitted = true;
            return List.of(event);
        }

        @Override
        public void close() {
        }
    }

    private static final class SuccessfulApiClient implements SubmissionApiClient {
        @Override
        public SubmissionUploadResponse uploadSubmission(String assignmentId, String studentId, Path filePath, String sha256) {
            return new SubmissionUploadResponse("submission-1", assignmentId, studentId, filePath.getFileName().toString(), sha256);
        }

        @Override
        public java.util.List<edu.nyu.unidrive.common.dto.SubmissionSummaryResponse> listSubmissions(String assignmentId) {
            return java.util.List.of();
        }

        @Override
        public edu.nyu.unidrive.client.net.DownloadedFile downloadSubmission(String submissionId) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FailingApiClient implements SubmissionApiClient {
        @Override
        public SubmissionUploadResponse uploadSubmission(String assignmentId, String studentId, Path filePath, String sha256)
            throws IOException {
            throw new IOException("server unavailable");
        }

        @Override
        public java.util.List<edu.nyu.unidrive.common.dto.SubmissionSummaryResponse> listSubmissions(String assignmentId) {
            return java.util.List.of();
        }

        @Override
        public edu.nyu.unidrive.client.net.DownloadedFile downloadSubmission(String submissionId) {
            throw new UnsupportedOperationException();
        }
    }
}
