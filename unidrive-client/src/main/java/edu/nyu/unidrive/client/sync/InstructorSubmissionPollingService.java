package edu.nyu.unidrive.client.sync;

import edu.nyu.unidrive.client.SyncServiceHandle;
import edu.nyu.unidrive.client.net.DownloadedFile;
import edu.nyu.unidrive.client.net.SubmissionApiClient;
import edu.nyu.unidrive.client.storage.ReceivedStateRecord;
import edu.nyu.unidrive.client.storage.ReceivedStateRepository;
import edu.nyu.unidrive.common.dto.SubmissionSummaryResponse;
import edu.nyu.unidrive.common.model.SyncStatus;
import edu.nyu.unidrive.common.util.FileHasher;
import edu.nyu.unidrive.common.workspace.CoursePath;
import edu.nyu.unidrive.common.workspace.MockCourseRegistry;
import edu.nyu.unidrive.common.workspace.MockCourseRegistry.Course;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class InstructorSubmissionPollingService implements SyncServiceHandle {

    private final SubmissionApiClient submissionApiClient;
    private final Path workspaceRoot;
    private final MockCourseRegistry courseRegistry;
    private final ReceivedStateRepository receivedStateRepository;
    private final Duration pollInterval;
    private final Map<String, String> latestSubmissionByStudent = new ConcurrentHashMap<>();
    private Thread workerThread;

    public InstructorSubmissionPollingService(
        SubmissionApiClient submissionApiClient,
        Path workspaceRoot,
        MockCourseRegistry courseRegistry,
        ReceivedStateRepository receivedStateRepository,
        Duration pollInterval
    ) {
        this.submissionApiClient = submissionApiClient;
        this.workspaceRoot = workspaceRoot;
        this.courseRegistry = courseRegistry;
        this.receivedStateRepository = receivedStateRepository;
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
            for (Course course : courseRegistry.courses()) {
                Path courseDir = workspaceRoot.resolve(courseRegistry.currentTerm()).resolve(course.slug());
                if (!Files.isDirectory(courseDir)) {
                    continue;
                }
                try (Stream<Path> assignmentDirs = Files.list(courseDir)) {
                    assignmentDirs
                        .filter(Files::isDirectory)
                        .forEach(assignmentDir -> {
                            CoursePath coursePath = new CoursePath(
                                courseRegistry.currentTerm(),
                                course.slug(),
                                assignmentDir.getFileName().toString()
                            );
                            try {
                                pollAssignment(coursePath, assignmentDir);
                            } catch (IOException ignored) {
                            }
                        });
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to scan instructor workspace.", exception);
        }
    }

    private void pollAssignment(CoursePath coursePath, Path assignmentDir) throws IOException {
        Path submissionsDir = assignmentDir.resolve(CoursePath.INSTRUCTOR_SUBMISSIONS_DIR);
        Files.createDirectories(submissionsDir);

        for (SubmissionSummaryResponse submission : submissionApiClient.listSubmissions(coursePath)) {
            latestSubmissionByStudent.put(submission.getStudentId(), submission.getSubmissionId());

            Path studentDir = submissionsDir.resolve(CoursePath.STUDENT_PREFIX + submission.getStudentId());
            Files.createDirectories(studentDir);

            Path destination = studentDir.resolve(submission.getFileName());
            if (Files.exists(destination) && FileHasher.sha256Hex(destination).equals(submission.getSha256())) {
                receivedStateRepository.save(new ReceivedStateRecord(
                    destination,
                    submission.getSubmissionId(),
                    submission.getSha256(),
                    SyncStatus.SYNCED,
                    System.currentTimeMillis(),
                    ReceivedReconcileService.SOURCE_INSTRUCTOR_SUBMISSIONS
                ));
                continue;
            }

            receivedStateRepository.save(new ReceivedStateRecord(
                destination,
                submission.getSubmissionId(),
                submission.getSha256(),
                SyncStatus.PENDING,
                0L,
                ReceivedReconcileService.SOURCE_INSTRUCTOR_SUBMISSIONS
            ));

            DownloadedFile download = submissionApiClient.downloadSubmission(submission.getSubmissionId());
            Files.write(destination, download.content());

            receivedStateRepository.save(new ReceivedStateRecord(
                destination,
                submission.getSubmissionId(),
                submission.getSha256(),
                SyncStatus.SYNCED,
                System.currentTimeMillis(),
                ReceivedReconcileService.SOURCE_INSTRUCTOR_SUBMISSIONS
            ));
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
