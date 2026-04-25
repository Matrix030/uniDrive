package edu.nyu.unidrive.client.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.nyu.unidrive.client.net.AssignmentApiClient;
import edu.nyu.unidrive.client.net.DownloadedFile;
import edu.nyu.unidrive.common.dto.AssignmentSummaryResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PublishUploadServiceTest {

    @Test
    void publishCallsApiWithTitleStrippedOfExtension(@TempDir Path tempDir) throws Exception {
        RecordingAssignmentApiClient apiClient = new RecordingAssignmentApiClient();
        PublishUploadService service = new PublishUploadService(apiClient);

        Path file = tempDir.resolve("Assignment1.txt");
        Files.writeString(file, "instructions");

        AssignmentSummaryResponse response = service.publish(file);

        assertEquals("Assignment1", apiClient.lastTitle);
        assertEquals(file, apiClient.lastFile);
        assertEquals("assignment-1", response.getAssignmentId());
    }

    @Test
    void publishUsesFullNameWhenNoExtension(@TempDir Path tempDir) throws Exception {
        RecordingAssignmentApiClient apiClient = new RecordingAssignmentApiClient();
        PublishUploadService service = new PublishUploadService(apiClient);

        Path file = tempDir.resolve("README");
        Files.writeString(file, "x");

        service.publish(file);

        assertEquals("README", apiClient.lastTitle);
    }

    private static final class RecordingAssignmentApiClient implements AssignmentApiClient {
        String lastTitle;
        Path lastFile;

        @Override
        public List<AssignmentSummaryResponse> listAssignments() {
            return List.of();
        }

        @Override
        public DownloadedFile downloadAssignment(String assignmentId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AssignmentSummaryResponse publishAssignment(String title, Path file) throws IOException {
            this.lastTitle = title;
            this.lastFile = file;
            return new AssignmentSummaryResponse("assignment-1", title, file.getFileName().toString(), "hash");
        }
    }
}
