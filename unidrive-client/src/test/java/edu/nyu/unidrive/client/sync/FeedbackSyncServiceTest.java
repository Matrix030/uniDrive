package edu.nyu.unidrive.client.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.nyu.unidrive.client.net.DownloadedFile;
import edu.nyu.unidrive.client.net.FeedbackApiClient;
import edu.nyu.unidrive.common.dto.FeedbackSummaryResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FeedbackSyncServiceTest {

    @Test
    void syncFeedbackDownloadsMissingFeedback(@TempDir Path tempDir) throws Exception {
        Path feedbackDirectory = tempDir.resolve("Feedback");
        Files.createDirectories(feedbackDirectory);
        RecordingFeedbackApiClient apiClient = new RecordingFeedbackApiClient();
        FeedbackSyncService service = new FeedbackSyncService(apiClient);

        int downloaded = service.syncFeedback("rvg9395", feedbackDirectory);

        assertEquals(1, downloaded);
        assertTrue(Files.exists(feedbackDirectory.resolve("Feedback.txt")));
        assertEquals("feedback contents", Files.readString(feedbackDirectory.resolve("Feedback.txt")));
        assertEquals(List.of("feedback-1"), apiClient.downloadedIds);
    }

    private static final class RecordingFeedbackApiClient implements FeedbackApiClient {
        private final List<String> downloadedIds = new java.util.ArrayList<>();

        @Override
        public List<FeedbackSummaryResponse> listFeedback(String studentId) {
            return List.of(new FeedbackSummaryResponse("feedback-1", "submission-1", studentId, "Feedback.txt", "hash-1"));
        }

        @Override
        public DownloadedFile downloadFeedback(String feedbackId) {
            downloadedIds.add(feedbackId);
            return new DownloadedFile("Feedback.txt", "feedback contents".getBytes());
        }
    }
}
