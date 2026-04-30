package edu.nyu.unidrive.client.sync;

import edu.nyu.unidrive.client.net.AssignmentApiClient;
import edu.nyu.unidrive.client.net.DownloadedFile;
import edu.nyu.unidrive.client.storage.AssignmentSlot;
import edu.nyu.unidrive.client.storage.ReceivedStateRecord;
import edu.nyu.unidrive.client.storage.ReceivedStateRepository;
import edu.nyu.unidrive.client.storage.WorkspaceLayout;
import edu.nyu.unidrive.common.dto.AssignmentSummaryResponse;
import edu.nyu.unidrive.common.model.SyncStatus;
import edu.nyu.unidrive.common.util.FileHasher;
import edu.nyu.unidrive.common.workspace.CoursePath;
import edu.nyu.unidrive.common.workspace.WorkspaceRole;
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

    public int syncAssignmentsForCourse(String term, String courseSlug, Path workspaceRoot) {
        try {
            int downloadedCount = 0;
            for (AssignmentSummaryResponse assignment : assignmentApiClient.listAssignments(term, courseSlug)) {
                CoursePath coursePath = new CoursePath(term, courseSlug, assignment.getAssignmentId());
                AssignmentSlot slot = WorkspaceLayout.ensureAssignmentSlot(workspaceRoot, coursePath, WorkspaceRole.STUDENT);
                Path destination = slot.publishDir().resolve(assignment.getFileName());

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

                DownloadedFile download = assignmentApiClient.downloadAssignment(assignment.getAssignmentId(), assignment.getFileName());
                Files.write(slot.publishDir().resolve(download.fileName()), download.content());
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
            throw new IllegalStateException("Failed to synchronize assignments for " + term + "/" + courseSlug, exception);
        }
    }

}
