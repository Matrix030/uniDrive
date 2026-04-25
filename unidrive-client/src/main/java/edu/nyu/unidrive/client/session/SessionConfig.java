package edu.nyu.unidrive.client.session;

import java.nio.file.Path;

public record SessionConfig(
    String userId,
    UserRole role,
    Path workspaceDirectory,
    String assignmentId,
    String baseUrl
) {
}
