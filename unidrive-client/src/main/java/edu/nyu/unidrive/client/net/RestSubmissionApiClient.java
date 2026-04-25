package edu.nyu.unidrive.client.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.nyu.unidrive.common.dto.SubmissionUploadResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    public SubmissionUploadResponse uploadSubmission(String assignmentId, String studentId, Path filePath, String sha256)
        throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("X-File-Sha256", sha256);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("studentId", studentId);
        body.add("file", new NamedByteArrayResource(filePath.getFileName().toString(), Files.readAllBytes(filePath)));

        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/api/v1/submissions/" + assignmentId,
            new HttpEntity<>(body, headers),
            String.class
        );

        JsonNode dataNode = objectMapper.readTree(response.getBody()).path("data");
        return new SubmissionUploadResponse(
            dataNode.path("submissionId").asText(),
            dataNode.path("assignmentId").asText(),
            dataNode.path("studentId").asText(),
            dataNode.path("fileName").asText(),
            dataNode.path("sha256").asText()
        );
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
