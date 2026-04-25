package edu.nyu.unidrive.common.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LoginRequestTest {

    @Test
    void exposesUserIdAndRole() {
        LoginRequest request = new LoginRequest("rvg9395", "STUDENT");

        assertEquals("rvg9395", request.userId());
        assertEquals("STUDENT", request.role());
    }
}
