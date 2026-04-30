package edu.nyu.unidrive.client.storage;

import java.nio.file.Path;

public record ClientWorkspace(Path rootDirectory, Path databasePath) {
}
