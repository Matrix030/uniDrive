package edu.nyu.unidrive.client.net;

import edu.nyu.unidrive.common.dto.AssignmentSummaryResponse;
import java.io.IOException;
import java.util.List;

public interface AssignmentApiClient {

    List<AssignmentSummaryResponse> listAssignments() throws IOException;

    DownloadedFile downloadAssignment(String assignmentId) throws IOException;
}
