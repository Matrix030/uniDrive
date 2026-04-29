package edu.nyu.unidrive.client;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import edu.nyu.unidrive.client.storage.InstructorFolderBootstrapService;
import edu.nyu.unidrive.client.storage.InstructorWorkspace;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InstructorSyncServiceFactoryTest {

    @Test
    void createReturnsCompositeHandleAndCloseDoesNotThrow(@TempDir Path tempDir) {
        InstructorWorkspace workspace = new InstructorFolderBootstrapService().bootstrap(tempDir);
        InstructorSyncServiceFactory factory = new InstructorSyncServiceFactory();

        SyncServiceHandle handle = factory.create(workspace, "assignment-1", "http://localhost:8080");

        assertNotNull(handle);
        handle.close();
    }
}
