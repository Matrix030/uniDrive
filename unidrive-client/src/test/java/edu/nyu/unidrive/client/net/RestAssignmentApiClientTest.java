package edu.nyu.unidrive.client.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import edu.nyu.unidrive.common.dto.AssignmentSummaryResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class RestAssignmentApiClientTest {

    @Test
    void listAssignmentsParsesResponseEnvelope() throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        RestAssignmentApiClient client = new RestAssignmentApiClient("http://localhost:8080", restTemplate);

        server.expect(requestTo("http://localhost:8080/api/v1/assignments"))
            .andExpect(method(GET))
            .andRespond(withSuccess(
                """
                {
                  "status": "ok",
                  "data": [
                    {
                      "assignmentId": "assignment-1",
                      "title": "Assignment 1",
                      "fileName": "Assignment1.txt",
                      "sha256": "hash-1"
                    }
                  ],
                  "message": "Assignments retrieved successfully."
                }
                """,
                MediaType.APPLICATION_JSON
            ));

        List<AssignmentSummaryResponse> assignments = client.listAssignments();

        assertEquals(1, assignments.size());
        assertEquals("assignment-1", assignments.getFirst().getAssignmentId());
        assertEquals("Assignment 1", assignments.getFirst().getTitle());
        server.verify();
    }

    @Test
    void downloadAssignmentFetchesBytesAndFilename() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        RestAssignmentApiClient client = new RestAssignmentApiClient("http://localhost:8080", restTemplate);

        server.expect(requestTo("http://localhost:8080/api/v1/assignments/assignment-1/download"))
            .andExpect(method(GET))
            .andRespond(withSuccess("assignment bytes", MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"Assignment1.txt\""));

        DownloadedFile file = client.downloadAssignment("assignment-1");

        assertEquals("Assignment1.txt", file.fileName());
        assertEquals("assignment bytes", new String(file.content()));
        server.verify();
    }
}
