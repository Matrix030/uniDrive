package edu.nyu.unidrive.client.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import edu.nyu.unidrive.common.dto.SubmissionUploadResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class RestSubmissionApiClientTest {

    @Test
    void uploadSubmissionPostsMultipartRequestAndParsesResponse(@TempDir Path tempDir) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        RestSubmissionApiClient client = new RestSubmissionApiClient("http://localhost:8080", restTemplate);
        Path file = tempDir.resolve("Hello.java");
        Files.writeString(file, "class Hello {}\n");

        server.expect(requestTo("http://localhost:8080/api/v1/submissions/assignment-1"))
            .andExpect(method(POST))
            .andExpect(header("X-File-Sha256", "abc123"))
            .andRespond(withSuccess(
                """
                {
                  "status": "ok",
                  "data": {
                    "submissionId": "submission-1",
                    "assignmentId": "assignment-1",
                    "studentId": "rvg9395",
                    "fileName": "Hello.java",
                    "sha256": "abc123"
                  },
                  "message": "Submission uploaded successfully."
                }
                """,
                MediaType.APPLICATION_JSON
            ));

        SubmissionUploadResponse response = client.uploadSubmission("assignment-1", "rvg9395", file, "abc123");

        assertEquals("submission-1", response.getSubmissionId());
        assertEquals("assignment-1", response.getAssignmentId());
        assertEquals("rvg9395", response.getStudentId());
        assertEquals("Hello.java", response.getFileName());
        assertEquals("abc123", response.getSha256());
        server.verify();
    }
}
