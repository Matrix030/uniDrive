package edu.nyu.unidrive.client.storage;

import edu.nyu.unidrive.common.model.SyncStatus;
import java.nio.file.Path;

public record ReceivedStateRecord(
    Path localPath,
    String remoteId,
    String sha256,
    SyncStatus status,
    long lastSynced,
    String source
) {
}

