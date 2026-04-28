package edu.nyu.unidrive.client.sync;

import edu.nyu.unidrive.client.SyncServiceHandle;
import edu.nyu.unidrive.client.net.DownloadedFile;
import edu.nyu.unidrive.client.net.SubmissionApiClient;
import edu.nyu.unidrive.client.storage.ReceivedStateRecord;
import edu.nyu.unidrive.client.storage.ReceivedStateRepository;
import edu.nyu.unidrive.common.dto.SubmissionSummaryResponse;
import edu.nyu.unidrive.common.model.SyncStatus;
import edu.nyu.unidrive.common.util.FileHasher;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InstructorSubmissionPollingService implements SyncServiceHandle {

    private final SubmissionApiClient submissionApiClient;
    private final Path submissionsDirectory;
    private final Path feedbackDirectory;
    private final ReceivedStateRepository receivedStateRepository;
    private final String assignmentId;
    private final Duration pollInterval;
    private final Map<String, String> latestSubmissionByStudent = new ConcurrentHashMap<>();
    private Thread workerThread;

    public InstructorSubmissionPollingService(
        SubmissionApiClient submissionApiClient,
        Path submissionsDirectory,
        Path feedbackDirectory,
        ReceivedStateRepository receivedStateRepository,
        String assignmentId,
        Duration pollInterval
    ) {
        this.submissionApiClient = submissionApiClient;
        this.submissionsDirectory = submissionsDirectory;
        this.feedbackDirectory = feedbackDirectory;
        this.receivedStateRepository = receivedStateRepository;
        this.assignmentId = assignmentId;
        this.pollInterval = pollInterval;
    }

    @Override
    public synchronized void start() {
        if (workerThread != null) {
            return;
        }
        workerThread = new Thread(this::runLoop, "unidrive-instructor-submission-polling");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    public void processOnce() {
        try {
            for (SubmissionSummaryResponse submission : submissionApiClient.listSubmissions(assignmentId)) {
                latestSubmissionByStudent.put(submission.getStudentId(), submission.getSubmissionId());

                Path studentDirectory = submissionsDirectory.resolve(submission.getStudentId());
                Files.createDirectories(studentDirectory);

                Files.createDirectories(feedbackDirectory.resolve(submission.getStudentId()));

                Path destination = studentDirectory.resolve(submission.getFileName());
                if (Files.exists(destination) && FileHasher.sha256Hex(destination).equals(submission.getSha256())) {
                    receivedStateRepository.save(new ReceivedStateRecord(
                        destination,
                        submission.getSubmissionId(),
                        submission.getSha256(),
                        SyncStatus.SYNCED,
                        System.currentTimeMillis(),
                        "INSTRUCTOR_SUBMISSIONS"
                    ));
                    continue;
                }

                receivedStateRepository.save(new ReceivedStateRecord(
                    destination,
                    submission.getSubmissionId(),
                    submission.getSha256(),
                    SyncStatus.PENDING,
                    0L,
                    "INSTRUCTOR_SUBMISSIONS"
                ));

                DownloadedFile download = submissionApiClient.downloadSubmission(submission.getSubmissionId());
                Files.write(destination, download.content());

                receivedStateRepository.save(new ReceivedStateRecord(
                    destination,
                    submission.getSubmissionId(),
                    submission.getSha256(),
                    SyncStatus.SYNCED,
                    System.currentTimeMillis(),
                    "INSTRUCTOR_SUBMISSIONS"
                ));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to synchronize instructor submissions.", exception);
        }
    }

    public Map<String, String> latestSubmissionByStudent() {
        return latestSubmissionByStudent;
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
