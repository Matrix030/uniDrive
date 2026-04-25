package edu.nyu.unidrive.client.sync;

import java.io.Closeable;
import java.time.Duration;
import java.util.List;

public interface SubmissionEventSource extends Closeable {

    List<SubmissionFileEvent> pollEvents(Duration timeout);
}
