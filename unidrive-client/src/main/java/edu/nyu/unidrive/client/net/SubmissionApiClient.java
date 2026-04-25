package edu.nyu.unidrive.client.net;

import edu.nyu.unidrive.common.dto.SubmissionUploadResponse;
import java.io.IOException;
import java.nio.file.Path;

public interface SubmissionApiClient {

    SubmissionUploadResponse uploadSubmission(String assignmentId, String studentId, Path filePath, String sha256)
        throws IOException;
}
