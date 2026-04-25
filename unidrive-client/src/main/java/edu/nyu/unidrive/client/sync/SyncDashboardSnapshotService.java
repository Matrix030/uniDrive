package edu.nyu.unidrive.client.sync;

import edu.nyu.unidrive.client.storage.SyncStateRecord;
import edu.nyu.unidrive.client.storage.SyncStateRepository;
import edu.nyu.unidrive.common.model.SyncStatus;
import java.util.Comparator;
import java.util.List;

public final class SyncDashboardSnapshotService {

    private final SyncStateRepository syncStateRepository;

    public SyncDashboardSnapshotService(SyncStateRepository syncStateRepository) {
        this.syncStateRepository = syncStateRepository;
    }

    public SyncDashboardSnapshot loadSnapshot() {
        List<SyncStateRecord> rows = syncStateRepository.findAll().stream()
            .sorted(Comparator.comparing((SyncStateRecord record) -> record.status() == SyncStatus.PENDING ? 0 : 1)
                .thenComparing(SyncStateRecord::localPath))
            .toList();

        int pendingCount = 0;
        int uploadingCount = 0;
        int syncedCount = 0;
        int failedCount = 0;

        for (SyncStateRecord row : rows) {
            switch (row.status()) {
                case PENDING -> pendingCount++;
                case UPLOADING -> uploadingCount++;
                case SYNCED -> syncedCount++;
                case FAILED -> failedCount++;
            }
        }

        return new SyncDashboardSnapshot(pendingCount, uploadingCount, syncedCount, failedCount, rows);
    }
}
