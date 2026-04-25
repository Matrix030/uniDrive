package edu.nyu.unidrive.client.session;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

public final class SessionPersistence {

    private static final String FILE_NAME = "config.properties";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_ROLE = "role";
    private static final String KEY_WORKSPACE = "workspaceDirectory";
    private static final String KEY_ASSIGNMENT = "assignmentId";
    private static final String KEY_BASE_URL = "baseUrl";

    private final Path configDirectory;

    public SessionPersistence() {
        this(defaultConfigDirectory());
    }

    public SessionPersistence(Path configDirectory) {
        this.configDirectory = configDirectory;
    }

    public Optional<SessionConfig> load() {
        Path file = configDirectory.resolve(FILE_NAME);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        } catch (IOException e) {
            return Optional.empty();
        }
        String userId = props.getProperty(KEY_USER_ID);
        String role = props.getProperty(KEY_ROLE);
        String workspace = props.getProperty(KEY_WORKSPACE);
        String assignmentId = props.getProperty(KEY_ASSIGNMENT);
        String baseUrl = props.getProperty(KEY_BASE_URL);
        if (userId == null || role == null || workspace == null || assignmentId == null || baseUrl == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new SessionConfig(
                userId,
                UserRole.fromString(role),
                Path.of(workspace),
                assignmentId,
                baseUrl
            ));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public void save(SessionConfig config) {
        try {
            Files.createDirectories(configDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create session directory: " + configDirectory, e);
        }
        Properties props = new Properties();
        props.setProperty(KEY_USER_ID, config.userId());
        props.setProperty(KEY_ROLE, config.role().name());
        props.setProperty(KEY_WORKSPACE, config.workspaceDirectory().toString());
        props.setProperty(KEY_ASSIGNMENT, config.assignmentId());
        props.setProperty(KEY_BASE_URL, config.baseUrl());
        Path file = configDirectory.resolve(FILE_NAME);
        try (OutputStream out = Files.newOutputStream(file)) {
            props.store(out, "University Drive session");
        } catch (IOException e) {
            throw new RuntimeException("Failed to write session config: " + file, e);
        }
    }

    public void clear() {
        try {
            Files.deleteIfExists(configDirectory.resolve(FILE_NAME));
        } catch (IOException e) {
            throw new RuntimeException("Failed to clear session config", e);
        }
    }

    private static Path defaultConfigDirectory() {
        return Path.of(System.getProperty("user.home"), ".unidrive");
    }
}
