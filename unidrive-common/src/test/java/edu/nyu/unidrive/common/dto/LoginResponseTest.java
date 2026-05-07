package edu.nyu.unidrive.common.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LoginResponseTest {

    @Test
    void exposesUserIdentityAndAccessToken() {
        LoginResponse response = new LoginResponse(
            "rvg9395",
            "Rishikesh",
            "student@nyu.edu",
            "STUDENT",
            "mock-token"
        );

        assertEquals("rvg9395", response.userId());
        assertEquals("Rishikesh", response.name());
        assertEquals("student@nyu.edu", response.email());
        assertEquals("STUDENT", response.role());
        assertEquals("mock-token", response.accessToken());
    }
}
