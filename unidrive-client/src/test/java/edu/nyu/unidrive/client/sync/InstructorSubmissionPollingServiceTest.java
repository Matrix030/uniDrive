package edu.nyu.unidrive.client.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.nyu.unidrive.client.net.DownloadedFile;
import edu.nyu.unidrive.client.storage.ReceivedStateRecord;
import edu.nyu.unidrive.client.net.SubmissionApiClient;
import edu.nyu.unidrive.client.storage.ReceivedStateRepository;
import edu.nyu.unidrive.common.model.SyncStatus;
import edu.nyu.unidrive.common.dto.SubmissionSummaryResponse;
import edu.nyu.unidrive.common.dto.SubmissionUploadResponse;
import edu.nyu.unidrive.common.util.FileHasher;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InstructorSubmissionPollingServiceTest {

    @Test
    void processOnceDownloadsSubmissionsAndPopulatesMap(@TempDir Path tempDir) throws Exception {
        Path submissionsDir = Files.createDirectory(tempDir.resolve("Submissions"));
        Path feedbackDir = Files.createDirectory(tempDir.resolve("Feedbacks"));
        byte[] content = "class Solution {}".getBytes();
        String hash = FileHasher.sha256Hex(content);
        StubSubmissionApiClient apiClient = new StubSubmissionApiClient(
            List.of(new SubmissionSummaryResponse("sub-1", "assignment-1", "rvg9395", "Solution.java", hash, "SYNCED")),
            content
        );
        ReceivedStateRepository repository = new ReceivedStateRepository(tempDir.resolve("received.db"));

        InstructorSubmissionPollingService service = new InstructorSubmissionPollingService(
            apiClient,
            submissionsDir,
            feedbackDir,
            repository,
            "assignment-1",
            Duration.ofMillis(50)
        );
        try (service) {
            service.processOnce();

            Path expected = submissionsDir.resolve("rvg9395").resolve("Solution.java");
            assertTrue(Files.exists(expected));
            assertEquals("class Solution {}", Files.readString(expected));
            assertEquals("sub-1", service.latestSubmissionByStudent().get("rvg9395"));

            assertTrue(Files.isDirectory(feedbackDir.resolve("rvg9395")));
            ReceivedStateRecord row = repository.findByLocalPath(expected).orElseThrow();
            assertEquals("sub-1", row.remoteId());
            assertEquals(hash, row.sha256());
            assertEquals(SyncStatus.SYNCED, row.status());
            assertEquals("INSTRUCTOR_SUBMISSIONS", row.source());
            assertTrue(row.lastSynced() > 0L);
        }
    }

    @Test
    void processOnceSkipsDownloadWhenLocalHashMatches(@TempDir Path tempDir) throws Exception {
        Path submissionsDir = Files.createDirectory(tempDir.resolve("Submissions"));
        Path feedbackDir = Files.createDirectory(tempDir.resolve("Feedbacks"));
        Path studentDir = Files.createDirectory(submissionsDir.resolve("rvg9395"));
        byte[] content = "class Solution {}".getBytes();
        Files.write(studentDir.resolve("Solution.java"), content);
        String hash = FileHasher.sha256Hex(content);

        StubSubmissionApiClient apiClient = new StubSubmissionApiClient(
            List.of(new SubmissionSummaryResponse("sub-1", "assignment-1", "rvg9395", "Solution.java", hash, "SYNCED")),
            "different".getBytes()
        );
        ReceivedStateRepository repository = new ReceivedStateRepository(tempDir.resolve("received.db"));

        InstructorSubmissionPollingService service = new InstructorSubmissionPollingService(
            apiClient,
            submissionsDir,
            feedbackDir,
            repository,
            "assignment-1",
            Duration.ofMillis(50)
        );
        try (service) {
            service.processOnce();

            assertEquals(0, apiClient.downloadCount);
            assertEquals("sub-1", service.latestSubmissionByStudent().get("rvg9395"));

            assertTrue(Files.isDirectory(feedbackDir.resolve("rvg9395")));
            ReceivedStateRecord row = repository.findByLocalPath(studentDir.resolve("Solution.java")).orElseThrow();
            assertEquals(SyncStatus.SYNCED, row.status());
            assertEquals("sub-1", row.remoteId());
            assertEquals(hash, row.sha256());
        }
    }

    private static final class StubSubmissionApiClient implements SubmissionApiClient {
        private final List<SubmissionSummaryResponse> submissions;
        private final byte[] downloadBytes;
        int downloadCount;

        StubSubmissionApiClient(List<SubmissionSummaryResponse> submissions, byte[] downloadBytes) {
            this.submissions = submissions;
            this.downloadBytes = downloadBytes;
        }

        @Override
        public SubmissionUploadResponse uploadSubmission(String assignmentId, String studentId, Path filePath, String sha256) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<SubmissionSummaryResponse> listSubmissions(String assignmentId) {
            return submissions;
        }

        @Override
        public DownloadedFile downloadSubmission(String submissionId) throws IOException {
            downloadCount++;
            SubmissionSummaryResponse match = submissions.stream()
                .filter(s -> s.getSubmissionId().equals(submissionId))
                .findFirst()
                .orElseThrow();
            return new DownloadedFile(match.getFileName(), downloadBytes);
        }
    }
}
