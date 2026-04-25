package edu.nyu.unidrive.common.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LoginResponseTest {

    @Test
    void exposesUserIdNameAndRole() {
        LoginResponse response = new LoginResponse("rvg9395", "Rishikesh", "STUDENT");

        assertEquals("rvg9395", response.userId());
        assertEquals("Rishikesh", response.name());
        assertEquals("STUDENT", response.role());
    }
}
