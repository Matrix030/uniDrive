package edu.nyu.unidrive.client.sync;

import edu.nyu.unidrive.client.net.AssignmentApiClient;
import edu.nyu.unidrive.client.net.DownloadedFile;
import edu.nyu.unidrive.client.storage.ReceivedStateRecord;
import edu.nyu.unidrive.client.storage.ReceivedStateRepository;
import edu.nyu.unidrive.common.dto.AssignmentSummaryResponse;
import edu.nyu.unidrive.common.model.SyncStatus;
import edu.nyu.unidrive.common.util.FileHasher;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AssignmentSyncService {

    private final AssignmentApiClient assignmentApiClient;
    private final ReceivedStateRepository receivedStateRepository;

    public AssignmentSyncService(AssignmentApiClient assignmentApiClient, ReceivedStateRepository receivedStateRepository) {
        this.assignmentApiClient = assignmentApiClient;
        this.receivedStateRepository = receivedStateRepository;
    }

    public int syncAssignments(Path assignmentsDirectory) {
        try {
            Files.createDirectories(assignmentsDirectory);
            int downloadedCount = 0;
            for (AssignmentSummaryResponse assignment : assignmentApiClient.listAssignments()) {
                Path destination = assignmentsDirectory.resolve(assignment.getFileName());
                if (Files.exists(destination) && FileHasher.sha256Hex(destination).equals(assignment.getSha256())) {
                    receivedStateRepository.save(new ReceivedStateRecord(
                        destination,
                        assignment.getAssignmentId(),
                        assignment.getSha256(),
                        SyncStatus.SYNCED,
                        System.currentTimeMillis(),
                        ReceivedReconcileService.SOURCE_ASSIGNMENTS
                    ));
                    continue;
                }

                receivedStateRepository.save(new ReceivedStateRecord(
                    destination,
                    assignment.getAssignmentId(),
                    assignment.getSha256(),
                    SyncStatus.PENDING,
                    0L,
                    ReceivedReconcileService.SOURCE_ASSIGNMENTS
                ));

                DownloadedFile download = assignmentApiClient.downloadAssignment(assignment.getAssignmentId());
                Files.write(assignmentsDirectory.resolve(download.fileName()), download.content());
                receivedStateRepository.save(new ReceivedStateRecord(
                    destination,
                    assignment.getAssignmentId(),
                    assignment.getSha256(),
                    SyncStatus.SYNCED,
                    System.currentTimeMillis(),
                    ReceivedReconcileService.SOURCE_ASSIGNMENTS
                ));
                downloadedCount++;
            }
            return downloadedCount;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to synchronize assignments.", exception);
        }
    }
}
