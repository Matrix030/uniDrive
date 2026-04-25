package edu.nyu.unidrive.client.sync;

import edu.nyu.unidrive.client.net.AssignmentApiClient;
import edu.nyu.unidrive.client.net.DownloadedFile;
import edu.nyu.unidrive.common.dto.AssignmentSummaryResponse;
import edu.nyu.unidrive.common.util.FileHasher;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AssignmentSyncService {

    private final AssignmentApiClient assignmentApiClient;

    public AssignmentSyncService(AssignmentApiClient assignmentApiClient) {
        this.assignmentApiClient = assignmentApiClient;
    }

    public int syncAssignments(Path assignmentsDirectory) {
        try {
            Files.createDirectories(assignmentsDirectory);
            int downloadedCount = 0;
            for (AssignmentSummaryResponse assignment : assignmentApiClient.listAssignments()) {
                Path destination = assignmentsDirectory.resolve(assignment.getFileName());
                if (Files.exists(destination) && FileHasher.sha256Hex(destination).equals(assignment.getSha256())) {
                    continue;
                }

                DownloadedFile download = assignmentApiClient.downloadAssignment(assignment.getAssignmentId());
                Files.write(assignmentsDirectory.resolve(download.fileName()), download.content());
                downloadedCount++;
            }
            return downloadedCount;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to synchronize assignments.", exception);
        }
    }
}
