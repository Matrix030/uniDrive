package edu.nyu.unidrive.client.net;

import edu.nyu.unidrive.common.dto.SubmissionSummaryResponse;
import edu.nyu.unidrive.common.dto.SubmissionUploadResponse;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface SubmissionApiClient {

    SubmissionUploadResponse uploadSubmission(String assignmentId, String studentId, Path filePath, String sha256)
        throws IOException;

    List<SubmissionSummaryResponse> listSubmissions(String assignmentId) throws IOException;

    DownloadedFile downloadSubmission(String submissionId) throws IOException;
}
