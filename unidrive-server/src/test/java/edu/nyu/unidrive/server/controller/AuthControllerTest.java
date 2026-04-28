package edu.nyu.unidrive.server.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:sqlite:target/test-auth.db"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearUsers() {
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    void loginReturnsExistingUser() throws Exception {
        jdbcTemplate.update("INSERT INTO users (id, name, role) VALUES (?, ?, ?)",
            "rvg9395", "Rishikesh", "STUDENT");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"rvg9395\",\"role\":\"STUDENT\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.data.userId").value("rvg9395"))
            .andExpect(jsonPath("$.data.name").value("Rishikesh"))
            .andExpect(jsonPath("$.data.role").value("STUDENT"));
    }

    @Test
    void loginUpdatesRoleForExistingUser() throws Exception {
        jdbcTemplate.update("INSERT INTO users (id, name, role) VALUES (?, ?, ?)",
            "ins1", "ins1", "STUDENT");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"ins1\",\"role\":\"INSTRUCTOR\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.role").value("INSTRUCTOR"));

        String role = jdbcTemplate.queryForObject(
            "SELECT role FROM users WHERE id = ?", String.class, "ins1");
        org.junit.jupiter.api.Assertions.assertEquals("INSTRUCTOR", role);
    }

    @Test
    void loginAutoCreatesUserWhenAbsent() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"newuser\",\"role\":\"INSTRUCTOR\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.userId").value("newuser"))
            .andExpect(jsonPath("$.data.role").value("INSTRUCTOR"));

        String role = jdbcTemplate.queryForObject(
            "SELECT role FROM users WHERE id = ?", String.class, "newuser");
        org.junit.jupiter.api.Assertions.assertEquals("INSTRUCTOR", role);
    }

    @Test
    void loginRejectsInvalidRole() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"foo\",\"role\":\"ADMIN\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void loginRejectsBlankUserId() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"\",\"role\":\"STUDENT\"}"))
            .andExpect(status().isBadRequest());
    }
}
