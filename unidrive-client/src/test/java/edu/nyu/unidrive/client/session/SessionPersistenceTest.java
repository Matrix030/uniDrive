package edu.nyu.unidrive.client.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SessionPersistenceTest {

    @Test
    void loadReturnsEmptyWhenFileAbsent(@TempDir Path tempDir) {
        SessionPersistence persistence = new SessionPersistence(tempDir);

        assertTrue(persistence.load().isEmpty());
    }

    @Test
    void saveAndLoadRoundTripsAllFields(@TempDir Path tempDir) {
        SessionPersistence persistence = new SessionPersistence(tempDir);
        Path workspace = tempDir.resolve("workspace");
        SessionConfig config = new SessionConfig(
            "rvg9395",
            UserRole.STUDENT,
            workspace,
            "http://localhost:8080"
        );

        persistence.save(config);
        Optional<SessionConfig> loaded = persistence.load();

        assertTrue(loaded.isPresent());
        assertEquals("rvg9395", loaded.get().userId());
        assertEquals(UserRole.STUDENT, loaded.get().role());
        assertEquals(workspace, loaded.get().workspaceDirectory());
        assertEquals("http://localhost:8080", loaded.get().baseUrl());
    }

    @Test
    void loadReturnsEmptyWhenRequiredKeysMissing(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("config.properties");
        Files.writeString(configFile, "userId=rvg9395\n");

        SessionPersistence persistence = new SessionPersistence(tempDir);

        assertTrue(persistence.load().isEmpty());
    }
}
