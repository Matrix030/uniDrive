package edu.nyu.unidrive.client.sync;

import java.nio.file.Path;

public record SubmissionFileEvent(Path path, SubmissionFileEventType type) {
}
