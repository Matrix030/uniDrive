package edu.nyu.unidrive.client.sync;

import edu.nyu.unidrive.client.storage.SyncStateRecord;
import java.util.List;

public record SyncDashboardSnapshot(
    int pendingCount,
    int uploadingCount,
    int syncedCount,
    int failedCount,
    List<SyncStateRecord> rows
) {
}
