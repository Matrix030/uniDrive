package edu.nyu.unidrive.client.sync;

import edu.nyu.unidrive.client.SyncServiceHandle;
import java.nio.file.Path;
import java.time.Duration;

public final class RemotePollingService implements SyncServiceHandle {

    private final AssignmentSyncService assignmentSyncService;
    private final FeedbackSyncService feedbackSyncService;
    private final Path assignmentsDirectory;
    private final Path feedbackDirectory;
    private final String studentId;
    private final Duration pollInterval;
    private Thread workerThread;

    public RemotePollingService(
        AssignmentSyncService assignmentSyncService,
        FeedbackSyncService feedbackSyncService,
        Path assignmentsDirectory,
        Path feedbackDirectory,
        String studentId,
        Duration pollInterval
    ) {
        this.assignmentSyncService = assignmentSyncService;
        this.feedbackSyncService = feedbackSyncService;
        this.assignmentsDirectory = assignmentsDirectory;
        this.feedbackDirectory = feedbackDirectory;
        this.studentId = studentId;
        this.pollInterval = pollInterval;
    }

    @Override
    public synchronized void start() {
        if (workerThread != null) {
            return;
        }

        workerThread = new Thread(this::runLoop, "unidrive-remote-polling");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    public void processOnce() {
        assignmentSyncService.syncAssignments(assignmentsDirectory);
        feedbackSyncService.syncFeedback(studentId, feedbackDirectory);
    }

    @Override
    public synchronized void close() {
        if (workerThread != null) {
            workerThread.interrupt();
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
            try {
                Thread.sleep(pollInterval.toMillis());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
