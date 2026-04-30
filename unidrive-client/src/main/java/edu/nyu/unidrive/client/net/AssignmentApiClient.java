package edu.nyu.unidrive.client.net;

import edu.nyu.unidrive.common.dto.AssignmentSummaryResponse;
import edu.nyu.unidrive.common.workspace.CoursePath;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface AssignmentApiClient {

    List<AssignmentSummaryResponse> listAssignments(String term, String courseSlug) throws IOException;

    DownloadedFile downloadAssignment(String assignmentId) throws IOException;

    AssignmentSummaryResponse publishAssignment(CoursePath coursePath, String title, Path file) throws IOException;
}
