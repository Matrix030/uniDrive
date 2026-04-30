package edu.nyu.unidrive.client.storage;

import java.nio.file.Path;

public record InstructorWorkspace(Path rootDirectory, Path databasePath) {
}
