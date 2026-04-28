package edu.nyu.unidrive.client.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.nyu.unidrive.client.storage.ReceivedStateRepository;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RemotePollingServiceTest {

    @Test
    void processOnceRunsAssignmentAndFeedbackSyncTasks(@TempDir Path tempDir) {
        RecordingAssignmentSyncService assignments = new RecordingAssignmentSyncService();
        RecordingFeedbackSyncService feedback = new RecordingFeedbackSyncService();
        ReceivedReconcileService reconcileService = new ReceivedReconcileService(new ReceivedStateRepository(tempDir.resolve("received.db")));
        RemotePollingService pollingService = new RemotePollingService(
            assignments,
            feedback,
            reconcileService,
            tempDir.resolve("Assignments"),
            tempDir.resolve("Feedbacks"),
            "rvg9395",
            Duration.ofMillis(25)
        );

        pollingService.processOnce();

        assertEquals(1, assignments.invocations);
        assertEquals(1, feedback.invocations);
        pollingService.close();
    }

    private static final class RecordingAssignmentSyncService extends AssignmentSyncService {
        private int invocations;

        private RecordingAssignmentSyncService() {
            super(null, new ReceivedStateRepository(Path.of("target/test-received.db")));
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
            super(null, new ReceivedStateRepository(Path.of("target/test-received.db")));
        }

        @Override
        public int syncFeedback(String studentId, Path feedbackDirectory) {
            invocations++;
            return 0;
        }
    }
}
