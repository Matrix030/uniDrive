package edu.nyu.unidrive.client.sync;

import edu.nyu.unidrive.client.storage.SyncStateRecord;
import edu.nyu.unidrive.client.storage.SyncStateRepository;
import edu.nyu.unidrive.common.model.SyncStatus;

public final class SubmissionSyncStateService {

    private final SyncStateRepository syncStateRepository;

    public SubmissionSyncStateService(SyncStateRepository syncStateRepository) {
        this.syncStateRepository = syncStateRepository;
    }

    public void recordPendingEvent(SubmissionFileEvent event) {
        SyncStateRecord existingRecord = syncStateRepository.findByLocalPath(event.path()).orElse(null);

        SyncStateRecord pendingRecord = new SyncStateRecord(
            event.path(),
            existingRecord == null ? null : existingRecord.remoteId(),
            null,
            SyncStatus.PENDING,
            existingRecord == null ? 0L : existingRecord.lastSynced()
        );

        syncStateRepository.save(pendingRecord);
    }
}
