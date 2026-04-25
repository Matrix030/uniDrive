package edu.nyu.unidrive.client.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.nyu.unidrive.common.dto.LoginRequest;
import edu.nyu.unidrive.common.dto.LoginResponse;
import java.io.IOException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public final class RestAuthApiClient implements AuthApiClient {

    private final String baseUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public RestAuthApiClient(String baseUrl, RestTemplate restTemplate) {
        this.baseUrl = baseUrl;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public LoginResponse login(String userId, String role) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> entity = new HttpEntity<>(new LoginRequest(userId, role), headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/api/v1/auth/login",
            entity,
            String.class
        );
        JsonNode dataNode = objectMapper.readTree(response.getBody()).path("data");
        return new LoginResponse(
            dataNode.path("userId").asText(),
            dataNode.path("name").asText(),
            dataNode.path("role").asText()
        );
    }
}
