package edu.nyu.unidrive.client.sync;

import edu.nyu.unidrive.client.SyncServiceHandle;
import edu.nyu.unidrive.client.storage.SyncStateRecord;
import edu.nyu.unidrive.client.storage.SyncStateRepository;
import edu.nyu.unidrive.common.model.SyncStatus;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

public final class PublishSyncService implements SyncServiceHandle {

    private final PublishDirectoryWatcher watcher;
    private final PublishUploadService uploadService;
    private final SyncStateRepository syncStateRepository;
    private final Duration pollTimeout;
    private final Set<Path> publishedFiles = new HashSet<>();
    private Thread workerThread;

    public PublishSyncService(PublishDirectoryWatcher watcher, PublishUploadService uploadService,
            SyncStateRepository syncStateRepository, Duration pollTimeout) {
        this.watcher = watcher;
        this.uploadService = uploadService;
        this.syncStateRepository = syncStateRepository;
        this.pollTimeout = pollTimeout;
    }

    @Override
    public synchronized void start() {
        if (workerThread != null) {
            return;
        }
        workerThread = new Thread(this::runLoop, "unidrive-publish-sync");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    public void processOnce() {
        for (SubmissionFileEvent event : watcher.pollEvents(pollTimeout)) {
            if (publishedFiles.contains(event.path())) {
                continue;
            }
            syncStateRepository.save(new SyncStateRecord(event.path(), null, null, SyncStatus.PENDING, 0L));
            try {
                var response = uploadService.publish(event.path());
                syncStateRepository.save(new SyncStateRecord(
                    event.path(),
                    response.getAssignmentId(),
                    response.getSha256(),
                    SyncStatus.SYNCED,
                    System.currentTimeMillis()
                ));
                publishedFiles.add(event.path());
            } catch (IOException ignored) {
                syncStateRepository.save(new SyncStateRecord(event.path(), null, null, SyncStatus.FAILED, 0L));
            }
        }
    }

    @Override
    public synchronized void close() {
        if (workerThread != null) {
            workerThread.interrupt();
        }
        try {
            watcher.close();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to close publish watcher.", exception);
        }
        if (workerThread != null) {
            try {
                workerThread.join(2000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            workerThread = null;
        }
    }

    private void runLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            processOnce();
        }
    }
}
