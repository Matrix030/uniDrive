package edu.nyu.unidrive.common.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void okFactoryCreatesSuccessEnvelope() {
        ApiResponse<String> response = ApiResponse.ok("payload", "upload accepted");

        assertEquals("ok", response.getStatus());
        assertEquals("payload", response.getData());
        assertEquals("upload accepted", response.getMessage());
    }

    @Test
    void errorFactoryCreatesErrorEnvelopeWithoutData() {
        ApiResponse<Void> response = ApiResponse.error("hash mismatch");

        assertEquals("error", response.getStatus());
        assertNull(response.getData());
        assertEquals("hash mismatch", response.getMessage());
    }
}
