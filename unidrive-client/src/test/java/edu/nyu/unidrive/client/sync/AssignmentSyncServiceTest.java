package edu.nyu.unidrive.client.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.nyu.unidrive.client.net.AssignmentApiClient;
import edu.nyu.unidrive.client.net.DownloadedFile;
import edu.nyu.unidrive.client.storage.ReceivedStateRecord;
import edu.nyu.unidrive.client.storage.ReceivedStateRepository;
import edu.nyu.unidrive.common.model.SyncStatus;
import edu.nyu.unidrive.common.dto.AssignmentSummaryResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AssignmentSyncServiceTest {

    @Test
    void syncAssignmentsDownloadsMissingAssignments(@TempDir Path tempDir) throws Exception {
        Path assignmentsDirectory = tempDir.resolve("Assignments");
        Files.createDirectories(assignmentsDirectory);
        RecordingAssignmentApiClient apiClient = new RecordingAssignmentApiClient();
        ReceivedStateRepository repository = new ReceivedStateRepository(tempDir.resolve("received.db"));
        AssignmentSyncService service = new AssignmentSyncService(apiClient, repository);

        int downloaded = service.syncAssignments(assignmentsDirectory);

        assertEquals(1, downloaded);
        Path file = assignmentsDirectory.resolve("Assignment1.txt");
        assertTrue(Files.exists(file));
        assertEquals("assignment contents", Files.readString(file));
        assertEquals(List.of("assignment-1"), apiClient.downloadedIds);
        ReceivedStateRecord row = repository.findByLocalPath(file).orElseThrow();
        assertEquals("assignment-1", row.remoteId());
        assertEquals("hash-1", row.sha256());
        assertEquals(SyncStatus.SYNCED, row.status());
        assertEquals(ReceivedReconcileService.SOURCE_ASSIGNMENTS, row.source());
        assertTrue(row.lastSynced() > 0L);
    }

    private static final class RecordingAssignmentApiClient implements AssignmentApiClient {
        private final List<String> downloadedIds = new java.util.ArrayList<>();

        @Override
        public List<AssignmentSummaryResponse> listAssignments() {
            return List.of(new AssignmentSummaryResponse("assignment-1", "Assignment 1", "Assignment1.txt", "hash-1"));
        }

        @Override
        public DownloadedFile downloadAssignment(String assignmentId) {
            downloadedIds.add(assignmentId);
            return new DownloadedFile("Assignment1.txt", "assignment contents".getBytes());
        }

        @Override
        public AssignmentSummaryResponse publishAssignment(String title, java.nio.file.Path file) {
            throw new UnsupportedOperationException();
        }
    }
}
