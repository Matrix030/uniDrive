package edu.nyu.unidrive.client.sync;

import edu.nyu.unidrive.client.storage.SyncStateRecord;
import edu.nyu.unidrive.client.storage.SyncStateRepository;
import edu.nyu.unidrive.common.model.SyncStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public final class SubmissionReconcileService {

    private final SyncStateRepository syncStateRepository;

    public SubmissionReconcileService(SyncStateRepository syncStateRepository) {
        this.syncStateRepository = syncStateRepository;
    }

    public void reconcileExistingSubmissions(Path submissionsDirectory) {
        try (Stream<Path> paths = Files.list(submissionsDirectory)) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                if (syncStateRepository.findByLocalPath(path).isPresent()) {
                    return;
                }
                syncStateRepository.save(new SyncStateRecord(path, null, null, SyncStatus.PENDING, 0L));
            });
        } catch (IOException e) {
            throw new IllegalStateException("Failed to reconcile submissions directory: " + submissionsDirectory, e);
        }
    }
}

