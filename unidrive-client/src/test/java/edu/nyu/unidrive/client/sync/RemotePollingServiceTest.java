package edu.nyu.unidrive.client.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RemotePollingServiceTest {

    @Test
    void processOnceRunsAssignmentAndFeedbackSyncTasks(@TempDir Path tempDir) {
        RecordingAssignmentSyncService assignments = new RecordingAssignmentSyncService();
        RecordingFeedbackSyncService feedback = new RecordingFeedbackSyncService();
        RemotePollingService pollingService = new RemotePollingService(
            assignments,
            feedback,
            tempDir.resolve("Assignments"),
            tempDir.resolve("Feedback"),
            "rvg9395",
            Duration.ofMillis(25)
        );

        pollingService.processOnce();

        assertEquals(1, assignments.invocations);
        assertEquals(1, feedback.invocations);
    }

    private static final class RecordingAssignmentSyncService extends AssignmentSyncService {
        private int invocations;

        private RecordingAssignmentSyncService() {
            super(null);
        }

        @Override
        public int syncAssignments(Path assignmentsDirectory) {
            invocations++;
            return 0;
        }
    }

    private static final class RecordingFeedbackSyncService extends FeedbackSyncService {
        private int invocations;

        private RecordingFeedbackSyncService() {
            super(null);
        }

        @Override
        public int syncFeedback(String studentId, Path feedbackDirectory) {
            invocations++;
            return 0;
        }
    }
}
