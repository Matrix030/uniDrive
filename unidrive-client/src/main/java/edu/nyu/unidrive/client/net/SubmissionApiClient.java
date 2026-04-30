package edu.nyu.unidrive.client.net;

import edu.nyu.unidrive.common.dto.SubmissionSummaryResponse;
import edu.nyu.unidrive.common.dto.SubmissionUploadResponse;
import edu.nyu.unidrive.common.workspace.CoursePath;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface SubmissionApiClient {

    SubmissionUploadResponse uploadSubmission(CoursePath coursePath, String studentId, Path filePath, String sha256)
        throws IOException;

    List<SubmissionSummaryResponse> listSubmissions(CoursePath coursePath) throws IOException;

    DownloadedFile downloadSubmission(String submissionId) throws IOException;
}
