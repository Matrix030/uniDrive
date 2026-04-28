package edu.nyu.unidrive.client.sync;

import edu.nyu.unidrive.client.SyncServiceHandle;
import edu.nyu.unidrive.client.storage.SyncStateRecord;
import edu.nyu.unidrive.client.storage.SyncStateRepository;
import edu.nyu.unidrive.common.model.SyncStatus;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class SyncService implements SyncServiceHandle {

    private final SubmissionEventSource eventSource;
    private final SubmissionSyncStateService syncStateService;
    private final SubmissionUploadService uploadService;
    private final SubmissionReconcileService reconcileService;
    private final SyncStateRepository syncStateRepository;
    private final String assignmentId;
    private final String studentId;
    private final Duration pollTimeout;
    private final ExecutorService uploadExecutor;
    private final Path submissionsDirectory;
    private final Map<Path, RetryState> retryStateByPath = new ConcurrentHashMap<>();
    private final Map<Path, Boolean> inFlight = new ConcurrentHashMap<>();

    private Thread workerThread;

    public SyncService(
        SubmissionEventSource eventSource,
        SubmissionSyncStateService syncStateService,
        SubmissionUploadService uploadService,
        SubmissionReconcileService reconcileService,
        SyncStateRepository syncStateRepository,
        Path submissionsDirectory,
        String assignmentId,
        String studentId,
        Duration pollTimeout
    ) {
        this.eventSource = eventSource;
        this.syncStateService = syncStateService;
        this.uploadService = uploadService;
        this.reconcileService = reconcileService;
        this.syncStateRepository = syncStateRepository;
        this.submissionsDirectory = submissionsDirectory;
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
            submitUploadIfAllowed(event.path(), false);
        }

        for (SyncStateRecord row : syncStateRepository.findAll()) {
            if (row.status() != SyncStatus.PENDING && row.status() != SyncStatus.FAILED) {
                continue;
            }
            if (!Files.isRegularFile(row.localPath())) {
                continue;
            }
            submitUploadIfAllowed(row.localPath(), row.status() == SyncStatus.FAILED);
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
        reconcileService.reconcileExistingSubmissions(submissionsDirectory);

        while (!Thread.currentThread().isInterrupted()) {
            processOnce();
        }
    }

    private void submitUploadIfAllowed(Path path, boolean isRetry) {
        if (inFlight.putIfAbsent(path, Boolean.TRUE) != null) {
            return;
        }

        long now = System.currentTimeMillis();
        RetryState state = retryStateByPath.computeIfAbsent(path, p -> new RetryState());
        if (isRetry && now < state.nextAllowedAttemptAtMs) {
            inFlight.remove(path);
            return;
        }

        uploadExecutor.submit(() -> {
            try {
                SyncStatus result = uploadService.uploadPendingSubmission(assignmentId, studentId, path);
                if (result == SyncStatus.SYNCED) {
                    state.reset();
                } else if (result == SyncStatus.FAILED) {
                    state.onFailure();
                }
            } finally {
                inFlight.remove(path);
            }
        });
    }

    private static final class RetryState {
        private int attempts = 0;
        private long nextAllowedAttemptAtMs = 0L;

        void reset() {
            attempts = 0;
            nextAllowedAttemptAtMs = 0L;
        }

        void onFailure() {
            attempts = Math.min(attempts + 1, 10);
            long backoffMs = Math.min(30_000L, 1_000L * (1L << (attempts - 1)));
            nextAllowedAttemptAtMs = System.currentTimeMillis() + backoffMs;
        }
    }
}
