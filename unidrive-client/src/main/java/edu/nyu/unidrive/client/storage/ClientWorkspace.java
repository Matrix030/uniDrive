package edu.nyu.unidrive.client.storage;

import java.nio.file.Path;

public record ClientWorkspace(
    Path rootDirectory,
    Path assignmentsDirectory,
    Path submissionsDirectory,
    Path feedbackDirectory,
    Path databasePath
) {
}
