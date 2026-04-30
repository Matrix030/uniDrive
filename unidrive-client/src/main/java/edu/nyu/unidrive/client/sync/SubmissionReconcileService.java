package edu.nyu.unidrive.client.sync;

import edu.nyu.unidrive.client.storage.SyncStateRecord;
import edu.nyu.unidrive.client.storage.SyncStateRepository;
import edu.nyu.unidrive.common.model.SyncStatus;
import edu.nyu.unidrive.common.workspace.CoursePath;
import edu.nyu.unidrive.common.workspace.CoursePath.Leaf;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public final class SubmissionReconcileService {

    private final SyncStateRepository syncStateRepository;

    public SubmissionReconcileService(SyncStateRepository syncStateRepository) {
        this.syncStateRepository = syncStateRepository;
    }

    public void reconcileExistingSubmissions(Path workspaceRoot) {
        if (!Files.isDirectory(workspaceRoot)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(workspaceRoot)) {
            paths.filter(Files::isRegularFile)
                .filter(path -> CoursePath.parseFromWorkspace(workspaceRoot, path)
                    .map(parsed -> parsed.leaf() == Leaf.SUBMISSIONS)
                    .orElse(false))
                .forEach(path -> {
                    if (syncStateRepository.findByLocalPath(path).isPresent()) {
                        return;
                    }
                    syncStateRepository.save(new SyncStateRecord(path, null, null, SyncStatus.PENDING, 0L));
                });
        } catch (IOException e) {
            throw new IllegalStateException("Failed to reconcile submissions in workspace: " + workspaceRoot, e);
        }
    }
}
