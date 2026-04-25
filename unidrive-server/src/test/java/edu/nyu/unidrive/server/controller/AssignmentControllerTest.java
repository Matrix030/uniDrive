package edu.nyu.unidrive.server.controller;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.nyu.unidrive.common.util.FileHasher;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.FileSystemUtils;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "unidrive.storage.root=target/test-assignment-storage",
    "spring.datasource.url=jdbc:sqlite:target/test-assignments.db"
})
class AssignmentControllerTest {

    private static final Path STORAGE_ROOT = Path.of("target/test-assignment-storage");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearState() throws IOException {
        FileSystemUtils.deleteRecursively(STORAGE_ROOT);
        Files.createDirectories(STORAGE_ROOT);
        jdbcTemplate.execute("DELETE FROM assignments");
    }

    @Test
    void publishAssignmentStoresFileAndMetadata() throws Exception {
        byte[] content = "assignment instructions".getBytes();
        String sha256 = FileHasher.sha256Hex(content);
        MockMultipartFile file = new MockMultipartFile("file", "Assignment1.txt", MediaType.TEXT_PLAIN_VALUE, content);

        mockMvc.perform(
                multipart("/api/v1/instructor/assignments")
                    .file(file)
                    .param("title", "Assignment 1")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.message").value("Assignment published successfully."))
            .andExpect(jsonPath("$.data.assignmentId").value(matchesPattern("[0-9a-f\\-]{36}")))
            .andExpect(jsonPath("$.data.title").value("Assignment 1"))
            .andExpect(jsonPath("$.data.fileName").value("Assignment1.txt"))
            .andExpect(jsonPath("$.data.sha256").value(sha256));

        Map<String, Object> saved = jdbcTemplate.queryForMap(
            "SELECT title, hash FROM assignments WHERE title = ?",
            "Assignment 1"
        );
        org.junit.jupiter.api.Assertions.assertEquals("Assignment 1", saved.get("title"));
        org.junit.jupiter.api.Assertions.assertEquals(sha256, saved.get("hash"));

        try (Stream<Path> storedFiles = Files.walk(STORAGE_ROOT)) {
            org.junit.jupiter.api.Assertions.assertEquals(1, storedFiles.filter(Files::isRegularFile).count());
        }
    }

    @Test
    void listAssignmentsReturnsPublishedAssignments() throws Exception {
        publishAssignment("Assignment 1", "Assignment1.txt", "one".getBytes());
        publishAssignment("Assignment 2", "Assignment2.txt", "two".getBytes());

        mockMvc.perform(get("/api/v1/assignments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.message").value("Assignments retrieved successfully."))
            .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void downloadAssignmentReturnsStoredFileContents() throws Exception {
        byte[] content = "assignment file".getBytes();
        publishAssignment("Assignment 1", "Assignment1.txt", content);
        String assignmentId = jdbcTemplate.queryForObject(
            "SELECT id FROM assignments WHERE title = ?",
            String.class,
            "Assignment 1"
        );

        mockMvc.perform(get("/api/v1/assignments/{assignmentId}/download", assignmentId))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", startsWith("attachment; filename=\"Assignment1.txt\"")))
            .andExpect(content().bytes(content));
    }

    private void publishAssignment(String title, String fileName, byte[] content) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", fileName, MediaType.TEXT_PLAIN_VALUE, content);
        mockMvc.perform(
                multipart("/api/v1/instructor/assignments")
                    .file(file)
                    .param("title", title)
            )
            .andExpect(status().isOk());
    }
}
