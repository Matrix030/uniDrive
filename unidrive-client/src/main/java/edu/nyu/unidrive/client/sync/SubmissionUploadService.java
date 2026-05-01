package edu.nyu.unidrive.client.sync;

import edu.nyu.unidrive.client.net.SubmissionApiClient;
import edu.nyu.unidrive.client.storage.SyncStateRecord;
import edu.nyu.unidrive.client.storage.SyncStateRepository;
import edu.nyu.unidrive.common.dto.SubmissionUploadResponse;
import edu.nyu.unidrive.common.model.SyncStatus;
import edu.nyu.unidrive.common.util.FileHasher;
import edu.nyu.unidrive.common.workspace.CoursePath;
import edu.nyu.unidrive.common.workspace.CoursePath.Leaf;
import edu.nyu.unidrive.common.workspace.CoursePath.ParsedLocation;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public final class SubmissionUploadService {

    private final SyncStateRepository syncStateRepository;
    private final SubmissionApiClient submissionApiClient;
    private final Path workspaceRoot;

    public SubmissionUploadService(
        SyncStateRepository syncStateRepository,
        SubmissionApiClient submissionApiClient,
        Path workspaceRoot
    ) {
        this.syncStateRepository = syncStateRepository;
        this.submissionApiClient = submissionApiClient;
        this.workspaceRoot = workspaceRoot;
    }

    public SyncStatus uploadPendingSubmission(String studentId, Path localPath) {
        Optional<ParsedLocation> parsed = CoursePath.parseFromWorkspace(workspaceRoot, localPath);
        if (parsed.isEmpty() || parsed.get().leaf() != Leaf.SUBMISSIONS) {
            return SyncStatus.FAILED;
        }
        CoursePath coursePath = parsed.get().coursePath();

        SyncStateRecord existingRecord = syncStateRepository.findByLocalPath(localPath).orElse(null);

        try {
            String sha256 = FileHasher.sha256Hex(localPath);

            if (existingRecord != null
                && existingRecord.remoteId() != null
                && existingRecord.sha256() != null
                && existingRecord.sha256().equals(sha256)
                && existingRecord.lastSynced() > 0L) {
                syncStateRepository.save(new SyncStateRecord(
                    localPath,
                    existingRecord.remoteId(),
                    sha256,
                    SyncStatus.SYNCED,
                    existingRecord.lastSynced()
                ));
                return SyncStatus.SYNCED;
            }

            syncStateRepository.save(new SyncStateRecord(
                localPath,
                existingRecord == null ? null : existingRecord.remoteId(),
                sha256,
                SyncStatus.UPLOADING,
                existingRecord == null ? 0L : existingRecord.lastSynced()
            ));

            SubmissionUploadResponse response = submissionApiClient.uploadSubmission(coursePath, studentId, localPath, sha256);
            syncStateRepository.save(new SyncStateRecord(
                localPath,
                response.getSubmissionId(),
                sha256,
                SyncStatus.SYNCED,
                System.currentTimeMillis()
            ));
            return SyncStatus.SYNCED;
        } catch (IOException | RuntimeException exception) {
            String fallbackSha256;
            try {
                fallbackSha256 = FileHasher.sha256Hex(localPath);
            } catch (IOException hashException) {
                throw new IllegalStateException("Failed to hash local submission.", hashException);
            }

            syncStateRepository.save(new SyncStateRecord(
                localPath,
                existingRecord == null ? null : existingRecord.remoteId(),
                fallbackSha256,
                SyncStatus.FAILED,
                existingRecord == null ? 0L : existingRecord.lastSynced()
            ));
            return SyncStatus.FAILED;
        }
    }

    public void deleteSubmission(Path localPath) {
        SyncStateRecord existingRecord = syncStateRepository.findByLocalPath(localPath).orElse(null);
        if (existingRecord == null) {
            return;
        }
        try {
            if (existingRecord.remoteId() != null && !existingRecord.remoteId().isBlank()) {
                submissionApiClient.deleteSubmission(existingRecord.remoteId());
            }
            syncStateRepository.deleteByLocalPath(localPath);
        } catch (IOException | RuntimeException exception) {
            syncStateRepository.save(new SyncStateRecord(
                localPath,
                existingRecord.remoteId(),
                existingRecord.sha256(),
                SyncStatus.FAILED,
                existingRecord.lastSynced()
            ));
        }
    }
}
