package edu.nyu.unidrive.client.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import edu.nyu.unidrive.common.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class RestAuthApiClientTest {

    @Test
    void loginPostsJsonAndParsesResponse() throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        RestAuthApiClient client = new RestAuthApiClient("http://localhost:8080", restTemplate);

        server.expect(requestTo("http://localhost:8080/api/v1/auth/login"))
            .andExpect(method(POST))
            .andRespond(withSuccess(
                """
                {
                  "status": "ok",
                  "data": {
                    "userId": "rvg9395",
                    "name": "Rishikesh",
                    "role": "STUDENT"
                  },
                  "message": "Login successful."
                }
                """,
                MediaType.APPLICATION_JSON
            ));

        LoginResponse response = client.login("rvg9395", "STUDENT");

        assertEquals("rvg9395", response.userId());
        assertEquals("Rishikesh", response.name());
        assertEquals("STUDENT", response.role());
        server.verify();
    }
}
