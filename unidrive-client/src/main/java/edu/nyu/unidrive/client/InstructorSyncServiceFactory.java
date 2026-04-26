package edu.nyu.unidrive.client;

import edu.nyu.unidrive.client.net.RestAssignmentApiClient;
import edu.nyu.unidrive.client.net.RestFeedbackApiClient;
import edu.nyu.unidrive.client.net.RestSubmissionApiClient;
import edu.nyu.unidrive.client.storage.InstructorWorkspace;
import edu.nyu.unidrive.client.storage.SyncStateRepository;
import edu.nyu.unidrive.client.sync.InstructorFeedbackWatcher;
import edu.nyu.unidrive.client.sync.InstructorSubmissionPollingService;
import edu.nyu.unidrive.client.sync.PublishDirectoryWatcher;
import edu.nyu.unidrive.client.sync.PublishSyncService;
import edu.nyu.unidrive.client.sync.PublishUploadService;
import java.io.IOException;
import java.time.Duration;
import org.springframework.web.client.RestTemplate;

public final class InstructorSyncServiceFactory {

    public SyncServiceHandle create(InstructorWorkspace workspace, String assignmentId, String baseUrl) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            PublishDirectoryWatcher publishWatcher = new PublishDirectoryWatcher(workspace.publishDirectory());
            PublishUploadService publishUploadService = new PublishUploadService(
                new RestAssignmentApiClient(baseUrl, restTemplate)
            );
            SyncStateRepository syncStateRepository = new SyncStateRepository(workspace.databasePath());
            PublishSyncService publishSyncService = new PublishSyncService(
                publishWatcher, publishUploadService, syncStateRepository, Duration.ofMillis(250)
            );

            InstructorSubmissionPollingService submissionPolling = new InstructorSubmissionPollingService(
                new RestSubmissionApiClient(baseUrl, restTemplate),
                workspace.submissionsDirectory(),
                assignmentId,
                Duration.ofSeconds(2)
            );

            InstructorFeedbackWatcher feedbackWatcher = new InstructorFeedbackWatcher(
                new RestFeedbackApiClient(baseUrl, restTemplate),
                workspace.feedbackDirectory(),
                submissionPolling.latestSubmissionByStudent(),
                Duration.ofSeconds(2)
            );

            return new CompositeSyncServiceHandle(publishSyncService, submissionPolling, feedbackWatcher);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create instructor sync service.", exception);
        }
    }
}
