package edu.nyu.unidrive.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.nyu.unidrive.client.storage.ClientWorkspace;
import edu.nyu.unidrive.client.storage.FolderBootstrapService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClientRuntimeServiceTest {

    @Test
    void startBootstrapsWorkspaceAndStartsSyncService(@TempDir Path tempDir) {
        RecordingSyncServiceFactory factory = new RecordingSyncServiceFactory();
        ClientRuntimeService runtimeService = new ClientRuntimeService(new FolderBootstrapService(), factory);

        ClientRuntime runtime = runtimeService.start(tempDir, "assignment-1", "rvg9395", "http://localhost:8080");

        assertEquals(tempDir, runtime.workspace().rootDirectory());
        assertTrue(Files.isDirectory(tempDir.resolve("Assignments")));
        assertTrue(Files.isDirectory(tempDir.resolve("Submissions")));
        assertTrue(Files.isDirectory(tempDir.resolve("Feedbacks")));
        assertTrue(Files.exists(tempDir.resolve("sync-state.db")));
        assertEquals("assignment-1", factory.assignmentId);
        assertEquals("rvg9395", factory.studentId);
        assertEquals("http://localhost:8080", factory.baseUrl);
        assertEquals(runtime.workspace(), factory.workspace);
        assertTrue(factory.syncHandle.started);
    }

    @Test
    void closeStopsSyncService(@TempDir Path tempDir) {
        RecordingSyncServiceFactory factory = new RecordingSyncServiceFactory();
        ClientRuntimeService runtimeService = new ClientRuntimeService(new FolderBootstrapService(), factory);

        ClientRuntime runtime = runtimeService.start(tempDir, "assignment-1", "rvg9395", "http://localhost:8080");
        runtime.close();

        assertTrue(factory.syncHandle.closed);
    }

    private static final class RecordingSyncServiceFactory implements SyncServiceFactory {
        private ClientWorkspace workspace;
        private String assignmentId;
        private String studentId;
        private String baseUrl;
        private final RecordingSyncServiceHandle syncHandle = new RecordingSyncServiceHandle();

        @Override
        public SyncServiceHandle create(ClientWorkspace workspace, String assignmentId, String studentId, String baseUrl) {
            this.workspace = workspace;
            this.assignmentId = assignmentId;
            this.studentId = studentId;
            this.baseUrl = baseUrl;
            return syncHandle;
        }
    }

    private static final class RecordingSyncServiceHandle implements SyncServiceHandle {
        private boolean started;
        private boolean closed;

        @Override
        public void start() {
            started = true;
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
