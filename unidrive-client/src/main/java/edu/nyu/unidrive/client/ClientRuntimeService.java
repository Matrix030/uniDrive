package edu.nyu.unidrive.client;

import edu.nyu.unidrive.client.storage.ClientWorkspace;
import edu.nyu.unidrive.client.storage.WorkspaceBootstrapService;
import java.nio.file.Path;

public final class ClientRuntimeService {

    private final WorkspaceBootstrapService folderBootstrapService;
    private final SyncServiceFactory syncServiceFactory;

    public ClientRuntimeService(WorkspaceBootstrapService folderBootstrapService, SyncServiceFactory syncServiceFactory) {
        this.folderBootstrapService = folderBootstrapService;
        this.syncServiceFactory = syncServiceFactory;
    }

    public ClientRuntime start(Path rootDirectory, String assignmentId, String studentId, String baseUrl) {
        ClientWorkspace workspace = folderBootstrapService.bootstrap(rootDirectory);
        SyncServiceHandle syncService = syncServiceFactory.create(workspace, assignmentId, studentId, baseUrl);
        syncService.start();
        return new ClientRuntime(workspace, syncService);
    }
}
