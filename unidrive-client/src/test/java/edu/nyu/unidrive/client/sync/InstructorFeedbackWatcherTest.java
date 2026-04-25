package edu.nyu.unidrive.client.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.nyu.unidrive.client.net.DownloadedFile;
import edu.nyu.unidrive.client.net.FeedbackApiClient;
import edu.nyu.unidrive.common.dto.FeedbackSummaryResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InstructorFeedbackWatcherTest {

    @Test
    void processOnceUploadsNewFeedbackForKnownSubmission(@TempDir Path tempDir) throws Exception {
        Path feedbackDir = Files.createDirectory(tempDir.resolve("Feedback"));
        Path studentDir = Files.createDirectory(feedbackDir.resolve("rvg9395"));
        Path file = studentDir.resolve("Feedback.txt");
        Files.writeString(file, "good work");

        Map<String, String> map = new ConcurrentHashMap<>();
        map.put("rvg9395", "submission-42");

        RecordingFeedbackApiClient apiClient = new RecordingFeedbackApiClient();
        InstructorFeedbackWatcher watcher = new InstructorFeedbackWatcher(
            apiClient, feedbackDir, map, Duration.ofMillis(50)
        );

        watcher.processOnce();

        assertEquals(1, apiClient.uploads.size());
        assertEquals("submission-42", apiClient.uploads.get(0).submissionId);
        assertEquals(file, apiClient.uploads.get(0).file);
    }

    @Test
    void processOnceSkipsStudentsWithNoKnownSubmission(@TempDir Path tempDir) throws Exception {
        Path feedbackDir = Files.createDirectory(tempDir.resolve("Feedback"));
        Path studentDir = Files.createDirectory(feedbackDir.resolve("rvg9395"));
        Files.writeString(studentDir.resolve("Feedback.txt"), "x");

        RecordingFeedbackApiClient apiClient = new RecordingFeedbackApiClient();
        InstructorFeedbackWatcher watcher = new InstructorFeedbackWatcher(
            apiClient, feedbackDir, new ConcurrentHashMap<>(), Duration.ofMillis(50)
        );

        watcher.processOnce();

        assertEquals(0, apiClient.uploads.size());
    }

    @Test
    void processOnceDoesNotReuploadAlreadyUploadedFiles(@TempDir Path tempDir) throws Exception {
        Path feedbackDir = Files.createDirectory(tempDir.resolve("Feedback"));
        Path studentDir = Files.createDirectory(feedbackDir.resolve("rvg9395"));
        Files.writeString(studentDir.resolve("Feedback.txt"), "x");

        Map<String, String> map = new ConcurrentHashMap<>();
        map.put("rvg9395", "submission-1");

        RecordingFeedbackApiClient apiClient = new RecordingFeedbackApiClient();
        InstructorFeedbackWatcher watcher = new InstructorFeedbackWatcher(
            apiClient, feedbackDir, map, Duration.ofMillis(50)
        );

        watcher.processOnce();
        watcher.processOnce();

        assertEquals(1, apiClient.uploads.size());
    }

    private static final class RecordingFeedbackApiClient implements FeedbackApiClient {
        record Upload(String submissionId, Path file) {}
        final List<Upload> uploads = new ArrayList<>();

        @Override
        public List<FeedbackSummaryResponse> listFeedback(String studentId) {
            return List.of();
        }

        @Override
        public DownloadedFile downloadFeedback(String feedbackId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public FeedbackSummaryResponse uploadFeedback(String submissionId, Path file) throws IOException {
            uploads.add(new Upload(submissionId, file));
            return new FeedbackSummaryResponse("fb-" + uploads.size(), submissionId, "rvg9395",
                file.getFileName().toString(), "hash");
        }
    }
}
