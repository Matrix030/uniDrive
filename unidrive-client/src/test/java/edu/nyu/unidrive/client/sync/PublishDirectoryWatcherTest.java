package edu.nyu.unidrive.client.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PublishDirectoryWatcherTest {

    @Test
    void pollEventsReturnsCreatedEventForNewPublishedFile(@TempDir Path tempDir) throws Exception {
        Path publishDir = Files.createDirectory(tempDir.resolve("Publish"));
        try (PublishDirectoryWatcher watcher = new PublishDirectoryWatcher(publishDir)) {
            Path file = publishDir.resolve("Assignment1.txt");
            Files.writeString(file, "instructions");

            List<SubmissionFileEvent> events = watcher.pollEvents(Duration.ofSeconds(2));

            assertEquals(1, events.size());
            assertEquals(SubmissionFileEventType.CREATED, events.getFirst().type());
            assertEquals(file, events.getFirst().path());
        }
    }

    @Test
    void pollEventsIgnoresSubdirectories(@TempDir Path tempDir) throws Exception {
        Path publishDir = Files.createDirectory(tempDir.resolve("Publish"));
        try (PublishDirectoryWatcher watcher = new PublishDirectoryWatcher(publishDir)) {
            Files.createDirectory(publishDir.resolve("nested"));

            List<SubmissionFileEvent> events = watcher.pollEvents(Duration.ofMillis(500));

            assertTrue(events.isEmpty());
        }
    }
}
