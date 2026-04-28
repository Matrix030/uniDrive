package edu.nyu.unidrive.client.sync;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.FileSystems;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class SubmissionDirectoryWatcher implements SubmissionEventSource {

    private final Path submissionsDirectory;
    private final WatchService watchService;

    public SubmissionDirectoryWatcher(Path submissionsDirectory) throws IOException {
        this.submissionsDirectory = submissionsDirectory;
        this.watchService = FileSystems.getDefault().newWatchService();
        submissionsDirectory.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY
        );
    }

    public List<SubmissionFileEvent> pollEvents(Duration timeout) {
        try {
            WatchKey firstKey = watchService.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (firstKey == null) {
                return List.of();
            }

            Map<Path, SubmissionFileEventType> eventTypesByPath = new LinkedHashMap<>();
            collectEvents(firstKey, eventTypesByPath);

            WatchKey nextKey;
            while ((nextKey = watchService.poll()) != null) {
                collectEvents(nextKey, eventTypesByPath);
            }

            List<SubmissionFileEvent> events = new ArrayList<>();
            for (Map.Entry<Path, SubmissionFileEventType> entry : eventTypesByPath.entrySet()) {
                events.add(new SubmissionFileEvent(entry.getKey(), entry.getValue()));
            }
            return events;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for submission file events.", exception);
        }
    }

    @Override
    public void close() throws IOException {
        watchService.close();
    }

    private void collectEvents(WatchKey watchKey, Map<Path, SubmissionFileEventType> eventTypesByPath) {
        for (WatchEvent<?> event : watchKey.pollEvents()) {
            if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                continue;
            }

            Path relativePath = (Path) event.context();
            Path absolutePath = submissionsDirectory.resolve(relativePath);
            if (!Files.isRegularFile(absolutePath)) {
                continue;
            }
            if (relativePath.getFileName() != null && "desktop.ini".equalsIgnoreCase(relativePath.getFileName().toString())) {
                continue;
            }

            SubmissionFileEventType newType = mapEventType(event.kind());
            SubmissionFileEventType existingType = eventTypesByPath.get(absolutePath);
            if (existingType == SubmissionFileEventType.CREATED || newType == SubmissionFileEventType.MODIFIED && existingType != null) {
                continue;
            }

            eventTypesByPath.put(absolutePath, newType);
        }

        watchKey.reset();
    }

    private SubmissionFileEventType mapEventType(WatchEvent.Kind<?> kind) {
        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
            return SubmissionFileEventType.CREATED;
        }
        return SubmissionFileEventType.MODIFIED;
    }
}
