package edu.nyu.unidrive.common.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LoginRequestTest {

    @Test
    void exposesEmailAndPassword() {
        LoginRequest request = new LoginRequest("student@nyu.edu", "password123");

        assertEquals("student@nyu.edu", request.email());
        assertEquals("password123", request.password());
    }
}
