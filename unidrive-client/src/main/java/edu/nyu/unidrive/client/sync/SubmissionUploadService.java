package edu.nyu.unidrive.client.sync;

import edu.nyu.unidrive.client.net.SubmissionApiClient;
import edu.nyu.unidrive.client.storage.SyncStateRecord;
import edu.nyu.unidrive.client.storage.SyncStateRepository;
import edu.nyu.unidrive.common.dto.SubmissionUploadResponse;
import edu.nyu.unidrive.common.model.SyncStatus;
import edu.nyu.unidrive.common.util.FileHasher;
import java.io.IOException;
import java.nio.file.Path;

public final class SubmissionUploadService {

    private final SyncStateRepository syncStateRepository;
    private final SubmissionApiClient submissionApiClient;

    public SubmissionUploadService(SyncStateRepository syncStateRepository, SubmissionApiClient submissionApiClient) {
        this.syncStateRepository = syncStateRepository;
        this.submissionApiClient = submissionApiClient;
    }

    public SyncStatus uploadPendingSubmission(String assignmentId, String studentId, Path localPath) {
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

            SubmissionUploadResponse response = submissionApiClient.uploadSubmission(assignmentId, studentId, localPath, sha256);
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
}
