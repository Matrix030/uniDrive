package edu.nyu.unidrive.server.service;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

final class AtomicFileWriter {

    private AtomicFileWriter() {
    }

    static void write(Path destination, byte[] content) throws IOException {
        Files.createDirectories(destination.getParent());
        Path temporaryFile = Files.createTempFile(
            destination.getParent(),
            "." + destination.getFileName(),
            ".tmp"
        );
        try {
            Files.write(temporaryFile, content);
            try {
                Files.move(
                    temporaryFile,
                    destination,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }
}
