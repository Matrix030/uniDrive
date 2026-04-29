package edu.nyu.unidrive.client.sync;

import edu.nyu.unidrive.client.storage.ReceivedStateRecord;
import edu.nyu.unidrive.client.storage.ReceivedStateRepository;
import edu.nyu.unidrive.common.model.SyncStatus;
import edu.nyu.unidrive.common.util.FileHasher;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public final class ReceivedReconcileService {

    public static final String SOURCE_ASSIGNMENTS = "ASSIGNMENTS";
    public static final String SOURCE_FEEDBACK = "FEEDBACK";
    public static final String SOURCE_INSTRUCTOR_SUBMISSIONS = "INSTRUCTOR_SUBMISSIONS";
    public static final String SOURCE_INSTRUCTOR_FEEDBACKS = "INSTRUCTOR_FEEDBACKS";

    private final ReceivedStateRepository receivedStateRepository;

    public ReceivedReconcileService(ReceivedStateRepository receivedStateRepository) {
        this.receivedStateRepository = receivedStateRepository;
    }

    public void reconcileExistingReceivedFiles(Path assignmentsDirectory, Path feedbackDirectory) {
        reconcileDirectory(assignmentsDirectory, SOURCE_ASSIGNMENTS);
        reconcileDirectory(feedbackDirectory, SOURCE_FEEDBACK);
    }

    private void reconcileDirectory(Path directory, String source) {
        if (directory == null || !Files.isDirectory(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.list(directory)) {
            paths.filter(Files::isRegularFile)
                .filter(path -> !isIgnoredFile(path))
                .forEach(path -> receivedStateRepository.findByLocalPath(path).ifPresentOrElse(
                    existing -> {
                    },
                    () -> receivedStateRepository.save(new ReceivedStateRecord(
                        path,
                        sourcePrefix(source) + path.getFileName(),
                        safeHash(path),
                        SyncStatus.SYNCED,
                        safeLastModified(path),
                        source
                    ))
                ));
        } catch (IOException ignored) {
        }
    }

    private boolean isIgnoredFile(Path path) {
        Path name = path.getFileName();
        return name != null && "desktop.ini".equalsIgnoreCase(name.toString());
    }

    private String sourcePrefix(String source) {
        return SOURCE_ASSIGNMENTS.equals(source) ? "assignment:" : "feedback:";
    }

    private String safeHash(Path path) {
        try {
            return FileHasher.sha256Hex(path);
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    private long safeLastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ignored) {
            return 0L;
        }
    }
}

