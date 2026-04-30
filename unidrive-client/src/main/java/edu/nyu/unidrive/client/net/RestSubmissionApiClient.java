package edu.nyu.unidrive.client.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.nyu.unidrive.common.dto.SubmissionSummaryResponse;
import edu.nyu.unidrive.common.dto.SubmissionUploadResponse;
import edu.nyu.unidrive.common.workspace.CoursePath;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

public final class RestSubmissionApiClient implements SubmissionApiClient {

    private final String baseUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public RestSubmissionApiClient(String baseUrl, RestTemplate restTemplate) {
        this.baseUrl = baseUrl;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public SubmissionUploadResponse uploadSubmission(CoursePath coursePath, String studentId, Path filePath, String sha256)
        throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("X-File-Sha256", sha256);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("studentId", studentId);
        body.add("file", new NamedByteArrayResource(filePath.getFileName().toString(), Files.readAllBytes(filePath)));

        String url = baseUrl + "/api/v1/submissions/" + coursePath.term()
            + "/" + coursePath.courseSlug()
            + "/" + coursePath.assignmentId();
        ResponseEntity<String> response = restTemplate.postForEntity(
            url,
            new HttpEntity<>(body, headers),
            String.class
        );

        JsonNode dataNode = objectMapper.readTree(response.getBody()).path("data");
        return new SubmissionUploadResponse(
            dataNode.path("submissionId").asText(),
            dataNode.path("term").asText(null),
            dataNode.path("course").asText(null),
            dataNode.path("assignmentId").asText(),
            dataNode.path("studentId").asText(),
            dataNode.path("fileName").asText(),
            dataNode.path("sha256").asText()
        );
    }

    @Override
    public List<SubmissionSummaryResponse> listSubmissions(CoursePath coursePath) throws IOException {
        String url = baseUrl + "/api/v1/submissions?term=" + coursePath.term()
            + "&course=" + coursePath.courseSlug()
            + "&assignmentId=" + coursePath.assignmentId();
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        JsonNode dataNode = objectMapper.readTree(response.getBody()).path("data");
        List<SubmissionSummaryResponse> submissions = new ArrayList<>();
        for (JsonNode node : dataNode) {
            submissions.add(new SubmissionSummaryResponse(
                node.path("submissionId").asText(),
                node.path("term").asText(null),
                node.path("course").asText(null),
                node.path("assignmentId").asText(),
                node.path("studentId").asText(),
                node.path("fileName").asText(),
                node.path("sha256").asText(),
                node.path("status").asText()
            ));
        }
        return submissions;
    }

    @Override
    public DownloadedFile downloadSubmission(String submissionId) {
        ResponseEntity<byte[]> response = restTemplate.getForEntity(
            baseUrl + "/api/v1/submissions/" + submissionId + "/download",
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

    private static final class NamedByteArrayResource extends ByteArrayResource {

        private final String filename;

        private NamedByteArrayResource(String filename, byte[] byteArray) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
