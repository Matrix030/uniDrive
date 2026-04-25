package edu.nyu.unidrive.client.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.nyu.unidrive.client.storage.ClientWorkspace;
import edu.nyu.unidrive.client.storage.FolderBootstrapService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SubmissionDirectoryWatcherTest {

    @Test
    void pollEventsReturnsCreatedFileEventForNewSubmission(@TempDir Path tempDir) throws Exception {
        ClientWorkspace workspace = new FolderBootstrapService().bootstrap(tempDir);

        try (SubmissionDirectoryWatcher watcher = new SubmissionDirectoryWatcher(workspace.submissionsDirectory())) {
            Path submission = workspace.submissionsDirectory().resolve("Hello.java");
            Files.writeString(submission, "class Hello {}\n");

            List<SubmissionFileEvent> events = watcher.pollEvents(Duration.ofSeconds(2));

            assertEquals(1, events.size());
            assertEquals(SubmissionFileEventType.CREATED, events.getFirst().type());
            assertEquals(submission, events.getFirst().path());
        }
    }

    @Test
    void pollEventsReturnsModifiedEventWhenSubmissionChanges(@TempDir Path tempDir) throws Exception {
        ClientWorkspace workspace = new FolderBootstrapService().bootstrap(tempDir);
        Path submission = workspace.submissionsDirectory().resolve("Hello.java");
        Files.writeString(submission, "class Hello {}\n");

        try (SubmissionDirectoryWatcher watcher = new SubmissionDirectoryWatcher(workspace.submissionsDirectory())) {
            watcher.pollEvents(Duration.ofMillis(250));

            Files.writeString(submission, "class Hello { int value = 1; }\n");
            List<SubmissionFileEvent> events = watcher.pollEvents(Duration.ofSeconds(2));

            assertFalse(events.isEmpty());
            assertTrue(events.stream().anyMatch(event ->
                event.type() == SubmissionFileEventType.MODIFIED && event.path().equals(submission)
            ));
        }
    }

    @Test
    void pollEventsIgnoresDirectoryEvents(@TempDir Path tempDir) throws Exception {
        ClientWorkspace workspace = new FolderBootstrapService().bootstrap(tempDir);

        try (SubmissionDirectoryWatcher watcher = new SubmissionDirectoryWatcher(workspace.submissionsDirectory())) {
            Files.createDirectory(workspace.submissionsDirectory().resolve("nested-folder"));

            List<SubmissionFileEvent> events = watcher.pollEvents(Duration.ofMillis(500));

            assertTrue(events.isEmpty());
        }
    }
}
