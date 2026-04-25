package edu.nyu.unidrive.client;

import edu.nyu.unidrive.client.net.RestSubmissionApiClient;
import edu.nyu.unidrive.client.storage.ClientWorkspace;
import edu.nyu.unidrive.client.storage.SyncStateRepository;
import edu.nyu.unidrive.client.sync.SubmissionDirectoryWatcher;
import edu.nyu.unidrive.client.sync.SubmissionSyncStateService;
import edu.nyu.unidrive.client.sync.SubmissionUploadService;
import edu.nyu.unidrive.client.sync.SyncService;
import java.io.IOException;
import java.time.Duration;
import org.springframework.web.client.RestTemplate;

public final class DefaultSyncServiceFactory implements SyncServiceFactory {

    @Override
    public SyncServiceHandle create(ClientWorkspace workspace, String assignmentId, String studentId, String baseUrl) {
        try {
            SyncStateRepository syncStateRepository = new SyncStateRepository(workspace.databasePath());
            SubmissionDirectoryWatcher watcher = new SubmissionDirectoryWatcher(workspace.submissionsDirectory());
            SubmissionSyncStateService syncStateService = new SubmissionSyncStateService(syncStateRepository);
            SubmissionUploadService uploadService = new SubmissionUploadService(
                syncStateRepository,
                new RestSubmissionApiClient(baseUrl, new RestTemplate())
            );

            return new SyncService(
                watcher,
                syncStateService,
                uploadService,
                assignmentId,
                studentId,
                Duration.ofMillis(250)
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create client sync service.", exception);
        }
    }
}
