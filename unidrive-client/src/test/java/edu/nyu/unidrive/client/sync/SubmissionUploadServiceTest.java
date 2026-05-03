package edu.nyu.unidrive.client.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.nyu.unidrive.client.net.DownloadedFile;
import edu.nyu.unidrive.client.net.SubmissionApiClient;
import edu.nyu.unidrive.client.storage.SyncStateRecord;
import edu.nyu.unidrive.client.storage.SyncStateRepository;
import edu.nyu.unidrive.common.dto.SubmissionSummaryResponse;
import edu.nyu.unidrive.common.dto.SubmissionUploadResponse;
import edu.nyu.unidrive.common.model.SyncStatus;
import edu.nyu.unidrive.common.util.FileHasher;
import edu.nyu.unidrive.common.workspace.CoursePath;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SubmissionUploadServiceTest {

    @Test
    void failedUploadCanBeRetriedAndMarkedSynced(@TempDir Path tempDir) throws IOException {
        Path workspaceRoot = tempDir.resolve("workspace");
        Path submissionFile = workspaceRoot.resolve("fall2026/daa/hashing/submission/Solution.java");
        Files.createDirectories(submissionFile.getParent());
        Files.writeString(submissionFile, "class Solution {}");

        SyncStateRepository repository = new SyncStateRepository(tempDir.resolve("sync-state.db"));
        FailsOnceSubmissionApiClient apiClient = new FailsOnceSubmissionApiClient();
        SubmissionUploadService service = new SubmissionUploadService(repository, apiClient, workspaceRoot);

        SyncStatus firstResult = service.uploadPendingSubmission("rvg9395", submissionFile);

        SyncStateRecord failedRecord = repository.findByLocalPath(submissionFile).orElseThrow();
        assertEquals(SyncStatus.FAILED, firstResult);
        assertEquals(SyncStatus.FAILED, failedRecord.status());
        assertEquals(FileHasher.sha256Hex(submissionFile), failedRecord.sha256());
        assertTrue(failedRecord.remoteId() == null || failedRecord.remoteId().isBlank());

        SyncStatus retryResult = service.uploadPendingSubmission("rvg9395", submissionFile);

        SyncStateRecord syncedRecord = repository.findByLocalPath(submissionFile).orElseThrow();
        assertEquals(SyncStatus.SYNCED, retryResult);
        assertEquals(SyncStatus.SYNCED, syncedRecord.status());
        assertEquals("submission-1", syncedRecord.remoteId());
        assertEquals(FileHasher.sha256Hex(submissionFile), syncedRecord.sha256());
        assertTrue(syncedRecord.lastSynced() > 0L);
        assertEquals(2, apiClient.uploadAttempts);
    }

    private static final class FailsOnceSubmissionApiClient implements SubmissionApiClient {

        private int uploadAttempts;

        @Override
        public SubmissionUploadResponse uploadSubmission(CoursePath coursePath, String studentId, Path filePath, String sha256)
            throws IOException {
            uploadAttempts++;
            if (uploadAttempts == 1) {
                throw new IOException("temporary outage");
            }
            return new SubmissionUploadResponse(
                "submission-1",
                coursePath.term(),
                coursePath.courseSlug(),
                coursePath.assignmentId(),
                studentId,
                filePath.getFileName().toString(),
                sha256
            );
        }

        @Override
        public List<SubmissionSummaryResponse> listSubmissions(CoursePath coursePath) {
            return List.of();
        }

        @Override
        public DownloadedFile downloadSubmission(String submissionId) {
            return new DownloadedFile("unused", new byte[0]);
        }

        @Override
        public void deleteSubmission(String submissionId) {
        }
    }
}
