package edu.nyu.unidrive.client.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.nyu.unidrive.common.dto.FeedbackSummaryResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public final class RestFeedbackApiClient implements FeedbackApiClient {

    private final String baseUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public RestFeedbackApiClient(String baseUrl, RestTemplate restTemplate) {
        this.baseUrl = baseUrl;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<FeedbackSummaryResponse> listFeedback(String studentId) throws IOException {
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "/api/v1/feedback?studentId=" + studentId,
            String.class
        );
        JsonNode dataNode = objectMapper.readTree(response.getBody()).path("data");
        List<FeedbackSummaryResponse> feedbackRows = new ArrayList<>();
        for (JsonNode feedbackNode : dataNode) {
            feedbackRows.add(new FeedbackSummaryResponse(
                feedbackNode.path("feedbackId").asText(),
                feedbackNode.path("submissionId").asText(),
                feedbackNode.path("studentId").asText(),
                feedbackNode.path("fileName").asText(),
                feedbackNode.path("sha256").asText()
            ));
        }
        return feedbackRows;
    }

    @Override
    public DownloadedFile downloadFeedback(String feedbackId) {
        ResponseEntity<byte[]> response = restTemplate.getForEntity(
            baseUrl + "/api/v1/feedback/" + feedbackId + "/download",
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
