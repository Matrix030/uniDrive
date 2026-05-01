package edu.nyu.unidrive.client.sync;

import edu.nyu.unidrive.client.SyncServiceHandle;
import edu.nyu.unidrive.common.workspace.MockCourseRegistry;
import edu.nyu.unidrive.common.workspace.MockCourseRegistry.Course;
import java.nio.file.Path;
import java.time.Duration;

public final class RemotePollingService implements SyncServiceHandle {

    private final AssignmentSyncService assignmentSyncService;
    private final ReceivedReconcileService receivedReconcileService;
    private final Path workspaceRoot;
    private final MockCourseRegistry courseRegistry;
    private final Duration pollInterval;
    private Thread workerThread;

    public RemotePollingService(
        AssignmentSyncService assignmentSyncService,
        ReceivedReconcileService receivedReconcileService,
        Path workspaceRoot,
        MockCourseRegistry courseRegistry,
        Duration pollInterval
    ) {
        this.assignmentSyncService = assignmentSyncService;
        this.receivedReconcileService = receivedReconcileService;
        this.workspaceRoot = workspaceRoot;
        this.courseRegistry = courseRegistry;
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
        for (Course course : courseRegistry.courses()) {
            try {
                assignmentSyncService.syncAssignmentsForCourse(courseRegistry.currentTerm(), course.slug(), workspaceRoot);
            } catch (RuntimeException exception) {
                System.err.println("Assignment sync failed for " + course.slug() + ": " + exception);
            }
        }
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
        receivedReconcileService.reconcileWorkspaceRoot(workspaceRoot);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                processOnce();
            } catch (RuntimeException exception) {
                System.err.println("Remote polling loop error: " + exception);
            }
            try {
                Thread.sleep(pollInterval.toMillis());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
