package edu.nyu.unidrive.client.sync;

import edu.nyu.unidrive.client.SyncServiceHandle;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class SyncService implements SyncServiceHandle {

    private final SubmissionEventSource eventSource;
    private final SubmissionSyncStateService syncStateService;
    private final SubmissionUploadService uploadService;
    private final String assignmentId;
    private final String studentId;
    private final Duration pollTimeout;
    private final ExecutorService uploadExecutor;

    private Thread workerThread;

    public SyncService(
        SubmissionEventSource eventSource,
        SubmissionSyncStateService syncStateService,
        SubmissionUploadService uploadService,
        String assignmentId,
        String studentId,
        Duration pollTimeout
    ) {
        this.eventSource = eventSource;
        this.syncStateService = syncStateService;
        this.uploadService = uploadService;
        this.assignmentId = assignmentId;
        this.studentId = studentId;
        this.pollTimeout = pollTimeout;
        this.uploadExecutor = Executors.newFixedThreadPool(2);
    }

    public synchronized void start() {
        if (workerThread != null) {
            return;
        }

        workerThread = new Thread(this::runLoop, "unidrive-sync-service");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    public void processOnce() {
        for (SubmissionFileEvent event : eventSource.pollEvents(pollTimeout)) {
            syncStateService.recordPendingEvent(event);
            uploadExecutor.submit(() -> uploadService.uploadPendingSubmission(assignmentId, studentId, event.path()));
        }
    }

    @Override
    public synchronized void close() {
        if (workerThread != null) {
            workerThread.interrupt();
        }

        uploadExecutor.shutdownNow();
        try {
            uploadExecutor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }

        try {
            eventSource.close();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to close submission event source.", exception);
        }

        if (workerThread != null) {
            try {
                workerThread.join(2000L);
            } catch (InterruptedException exception) {
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
