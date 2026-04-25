package edu.nyu.unidrive.client;

import java.nio.file.Path;

public record ClientStartupConfig(
    Path rootDirectory,
    String assignmentId,
    String studentId,
    String baseUrl
) {

    public static ClientStartupConfig fromSystemProperties() {
        String studentId = System.getProperty("unidrive.studentId", "rvg9395");
        Path rootDirectory = Path.of(System.getProperty("unidrive.root", "demo-workspace/student-" + studentId));
        return new ClientStartupConfig(
            rootDirectory,
            System.getProperty("unidrive.assignmentId", "assignment-1"),
            studentId,
            System.getProperty("unidrive.serverBaseUrl", "http://localhost:8080")
        );
    }
}
