package edu.nyu.unidrive.client.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.nyu.unidrive.common.dto.AssignmentSummaryResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public final class RestAssignmentApiClient implements AssignmentApiClient {

    private final String baseUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public RestAssignmentApiClient(String baseUrl, RestTemplate restTemplate) {
        this.baseUrl = baseUrl;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<AssignmentSummaryResponse> listAssignments() throws IOException {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/api/v1/assignments", String.class);
        JsonNode dataNode = objectMapper.readTree(response.getBody()).path("data");
        List<AssignmentSummaryResponse> assignments = new ArrayList<>();
        for (JsonNode assignmentNode : dataNode) {
            assignments.add(new AssignmentSummaryResponse(
                assignmentNode.path("assignmentId").asText(),
                assignmentNode.path("title").asText(),
                assignmentNode.path("fileName").asText(),
                assignmentNode.path("sha256").asText()
            ));
        }
        return assignments;
    }

    @Override
    public DownloadedFile downloadAssignment(String assignmentId) {
        ResponseEntity<byte[]> response = restTemplate.getForEntity(
            baseUrl + "/api/v1/assignments/" + assignmentId + "/download",
            byte[].class
        );
        return new DownloadedFile(extractFileName(response), response.getBody() == null ? new byte[0] : response.getBody());
    }

    private String extractFileName(ResponseEntity<?> response) {
        String contentDisposition = response.getHeaders().getFirst("Content-Disposition");
        if (contentDisposition == null) {
            return "download.bin";
        }
        int start = contentDisposition.indexOf("filename=\"");
        if (start < 0) {
            return "download.bin";
        }
        return contentDisposition.substring(start + 10, contentDisposition.length() - 1);
    }
}
