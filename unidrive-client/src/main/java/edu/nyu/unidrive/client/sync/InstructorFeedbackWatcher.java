package edu.nyu.unidrive.client.sync;

import edu.nyu.unidrive.client.SyncServiceHandle;
import edu.nyu.unidrive.client.net.FeedbackApiClient;
import edu.nyu.unidrive.client.storage.ReceivedStateRecord;
import edu.nyu.unidrive.client.storage.ReceivedStateRepository;
import edu.nyu.unidrive.common.model.SyncStatus;
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
    private final ReceivedStateRepository receivedStateRepository;
    private final Map<String, String> latestSubmissionByStudent;
    private final Duration pollInterval;
    private final Set<Path> uploadedFiles = new HashSet<>();
    private Thread workerThread;

    public InstructorFeedbackWatcher(
        FeedbackApiClient feedbackApiClient,
        Path feedbackDirectory,
        ReceivedStateRepository receivedStateRepository,
        Map<String, String> latestSubmissionByStudent,
        Duration pollInterval
    ) {
        this.feedbackApiClient = feedbackApiClient;
        this.feedbackDirectory = feedbackDirectory;
        this.receivedStateRepository = receivedStateRepository;
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
            receivedStateRepository.save(new ReceivedStateRecord(
                file,
                null,
                null,
                SyncStatus.PENDING,
                0L,
                "INSTRUCTOR_FEEDBACKS"
            ));

            var response = feedbackApiClient.uploadFeedback(submissionId, file);
            uploadedFiles.add(file);
            receivedStateRepository.save(new ReceivedStateRecord(
                file,
                response.getFeedbackId(),
                response.getSha256(),
                SyncStatus.SYNCED,
                System.currentTimeMillis(),
                "INSTRUCTOR_FEEDBACKS"
            ));
        } catch (IOException exception) {
            receivedStateRepository.save(new ReceivedStateRecord(
                file,
                null,
                null,
                SyncStatus.FAILED,
                0L,
                "INSTRUCTOR_FEEDBACKS"
            ));
            throw new IllegalStateException("Failed to upload feedback file " + file, exception);
        }
    }

    private void runLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                processOnce();
            } catch (RuntimeException ignored) {
            }
            try {
                Thread.sleep(pollInterval.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
