package edu.nyu.unidrive.client.sync;

import edu.nyu.unidrive.client.SyncServiceHandle;
import edu.nyu.unidrive.client.net.FeedbackApiClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class InstructorFeedbackWatcher implements SyncServiceHandle {

    private final FeedbackApiClient feedbackApiClient;
    private final Path feedbackDirectory;
    private final Map<String, String> latestSubmissionByStudent;
    private final Duration pollInterval;
    private final Set<Path> uploadedFiles = new HashSet<>();
    private Thread workerThread;

    public InstructorFeedbackWatcher(
        FeedbackApiClient feedbackApiClient,
        Path feedbackDirectory,
        Map<String, String> latestSubmissionByStudent,
        Duration pollInterval
    ) {
        this.feedbackApiClient = feedbackApiClient;
        this.feedbackDirectory = feedbackDirectory;
        this.latestSubmissionByStudent = latestSubmissionByStudent;
        this.pollInterval = pollInterval;
    }

    @Override
    public synchronized void start() {
        if (workerThread != null) {
            return;
        }
        workerThread = new Thread(this::runLoop, "unidrive-instructor-feedback-watcher");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    public void processOnce() {
        try (Stream<Path> studentDirs = Files.list(feedbackDirectory)) {
            studentDirs
                .filter(Files::isDirectory)
                .forEach(this::processStudentDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to scan feedback directory.", exception);
        }
    }

    @Override
    public synchronized void close() {
        if (workerThread != null) {
            workerThread.interrupt();
            try {
                workerThread.join(2000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            workerThread = null;
        }
    }

    private void processStudentDirectory(Path studentDir) {
        String studentId = studentDir.getFileName().toString();
        String submissionId = latestSubmissionByStudent.get(studentId);
        if (submissionId == null) {
            return;
        }
        try (Stream<Path> files = Files.list(studentDir)) {
            files.filter(Files::isRegularFile).forEach(file -> uploadIfNew(submissionId, file));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to scan feedback for " + studentId, exception);
        }
    }

    private void uploadIfNew(String submissionId, Path file) {
        if (uploadedFiles.contains(file)) {
            return;
        }
        try {
            feedbackApiClient.uploadFeedback(submissionId, file);
            uploadedFiles.add(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to upload feedback file " + file, exception);
        }
    }

    private void runLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                processOnce();
            } catch (RuntimeException ignored) {
                // continue polling on transient failures
            }
            try {
                Thread.sleep(pollInterval.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
