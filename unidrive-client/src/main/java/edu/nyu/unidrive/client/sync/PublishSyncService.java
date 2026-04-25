package edu.nyu.unidrive.client.sync;

import edu.nyu.unidrive.client.SyncServiceHandle;
import java.io.IOException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

public final class PublishSyncService implements SyncServiceHandle {

    private final PublishDirectoryWatcher watcher;
    private final PublishUploadService uploadService;
    private final Duration pollTimeout;
    private final Set<java.nio.file.Path> publishedFiles = new HashSet<>();
    private Thread workerThread;

    public PublishSyncService(PublishDirectoryWatcher watcher, PublishUploadService uploadService, Duration pollTimeout) {
        this.watcher = watcher;
        this.uploadService = uploadService;
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
            try {
                uploadService.publish(event.path());
                publishedFiles.add(event.path());
            } catch (IOException ignored) {
                // retry on next iteration
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
