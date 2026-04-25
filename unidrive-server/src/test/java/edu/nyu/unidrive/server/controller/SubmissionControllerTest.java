package edu.nyu.unidrive.server.controller;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.FileSystemUtils;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "unidrive.storage.root=target/test-storage",
    "spring.datasource.url=jdbc:sqlite:target/test-submissions.db"
})
class SubmissionControllerTest {

    private static final Path STORAGE_ROOT = Path.of("target/test-storage");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearStorageRoot() throws IOException {
        FileSystemUtils.deleteRecursively(STORAGE_ROOT);
        Files.createDirectories(STORAGE_ROOT);
        jdbcTemplate.execute("DELETE FROM submissions");
    }

    @Test
    void uploadSubmissionStoresFileAndReturnsReceiptWhenHashMatches() throws Exception {
        byte[] content = "public class Hello { }".getBytes();
        String sha256 = FileHasher.sha256Hex(content);
        int submissionCountBefore = submissionCount();
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "Hello.java",
            MediaType.TEXT_PLAIN_VALUE,
            content
        );

        mockMvc.perform(
                multipart("/api/v1/submissions/{assignmentId}", "assignment-1")
                    .file(file)
                    .param("studentId", "rvg9395")
                    .header("X-File-Sha256", sha256)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.message").value("Submission uploaded successfully."))
            .andExpect(jsonPath("$.data.submissionId").value(matchesPattern("[0-9a-f\\-]{36}")))
            .andExpect(jsonPath("$.data.assignmentId").value("assignment-1"))
            .andExpect(jsonPath("$.data.studentId").value("rvg9395"))
            .andExpect(jsonPath("$.data.fileName").value("Hello.java"))
            .andExpect(jsonPath("$.data.sha256").value(sha256));

        try (Stream<Path> storedFiles = Files.walk(STORAGE_ROOT)) {
            long fileCount = storedFiles.filter(Files::isRegularFile).count();
            org.junit.jupiter.api.Assertions.assertEquals(1, fileCount);
        }

        org.junit.jupiter.api.Assertions.assertEquals(submissionCountBefore + 1, submissionCount());

        Map<String, Object> savedSubmission = jdbcTemplate.queryForMap(
            "SELECT assignment_id, student_id, hash, status FROM submissions WHERE hash = ?",
            sha256
        );
        org.junit.jupiter.api.Assertions.assertEquals("assignment-1", savedSubmission.get("assignment_id"));
        org.junit.jupiter.api.Assertions.assertEquals("rvg9395", savedSubmission.get("student_id"));
        org.junit.jupiter.api.Assertions.assertEquals(sha256, savedSubmission.get("hash"));
        org.junit.jupiter.api.Assertions.assertEquals("SYNCED", savedSubmission.get("status"));
    }

    @Test
    void uploadSubmissionRejectsHashMismatchAndDoesNotStoreFile() throws Exception {
        byte[] content = "class BrokenHash { }".getBytes();
        int submissionCountBefore = submissionCount();
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "BrokenHash.java",
            MediaType.TEXT_PLAIN_VALUE,
            content
        );

        mockMvc.perform(
                multipart("/api/v1/submissions/{assignmentId}", "assignment-1")
                    .file(file)
                    .param("studentId", "rvg9395")
                    .header("X-File-Sha256", "not-the-real-hash")
            )
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.status").value("error"))
            .andExpect(jsonPath("$.message").value("Uploaded file hash did not match the provided SHA-256."));

        try (Stream<Path> storedFiles = Files.walk(STORAGE_ROOT)) {
            long fileCount = storedFiles.filter(Files::isRegularFile).count();
            org.junit.jupiter.api.Assertions.assertEquals(0, fileCount);
        }

        org.junit.jupiter.api.Assertions.assertEquals(submissionCountBefore, submissionCount());
    }

    @Test
    void listSubmissionsReturnsOnlyRowsForRequestedAssignment() throws Exception {
        uploadSubmission("assignment-1", "rvg9395", "First.java", "class First { }".getBytes());
        uploadSubmission("assignment-2", "ow2130", "Second.java", "class Second { }".getBytes());

        mockMvc.perform(get("/api/v1/submissions").param("assignmentId", "assignment-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.message").value("Submissions retrieved successfully."))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].assignmentId").value("assignment-1"))
            .andExpect(jsonPath("$.data[0].studentId").value("rvg9395"))
            .andExpect(jsonPath("$.data[0].fileName").value("First.java"))
            .andExpect(jsonPath("$.data[0].status").value("SYNCED"));
    }

    @Test
    void listSubmissionsCanFilterByAssignmentAndStudent() throws Exception {
        uploadSubmission("assignment-1", "rvg9395", "Alpha.java", "class Alpha { }".getBytes());
        uploadSubmission("assignment-1", "ow2130", "Beta.java", "class Beta { }".getBytes());

        mockMvc.perform(
                get("/api/v1/submissions")
                    .param("assignmentId", "assignment-1")
                    .param("studentId", "rvg9395")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.message").value("Submissions retrieved successfully."))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].assignmentId").value("assignment-1"))
            .andExpect(jsonPath("$.data[0].studentId").value("rvg9395"))
            .andExpect(jsonPath("$.data[0].fileName").value("Alpha.java"))
            .andExpect(jsonPath("$.data[0].status").value("SYNCED"));
    }

    @Test
    void downloadSubmissionReturnsStoredFileContents() throws Exception {
        byte[] contentBytes = "class DownloadMe { }".getBytes();
        uploadSubmission("assignment-1", "rvg9395", "DownloadMe.java", contentBytes);

        String submissionId = jdbcTemplate.queryForObject(
            "SELECT id FROM submissions WHERE assignment_id = ? AND student_id = ? ORDER BY submitted_at DESC LIMIT 1",
            String.class,
            "assignment-1",
            "rvg9395"
        );

        mockMvc.perform(get("/api/v1/submissions/{submissionId}/download", submissionId))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", startsWith("attachment; filename=\"DownloadMe.java\"")))
            .andExpect(content().bytes(contentBytes));
    }

    @Test
    void downloadSubmissionReturns404WhenSubmissionDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/submissions/{submissionId}/download", "missing-submission-id"))
            .andExpect(status().isNotFound());
    }

    private int submissionCount() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM submissions", Integer.class);
        return count == null ? 0 : count;
    }

    private void uploadSubmission(String assignmentId, String studentId, String fileName, byte[] content) throws Exception {
        String sha256 = FileHasher.sha256Hex(content);
        MockMultipartFile file = new MockMultipartFile(
            "file",
            fileName,
            MediaType.TEXT_PLAIN_VALUE,
            content
        );

        mockMvc.perform(
                multipart("/api/v1/submissions/{assignmentId}", assignmentId)
                    .file(file)
                    .param("studentId", studentId)
                    .header("X-File-Sha256", sha256)
            )
            .andExpect(status().isOk());
    }
}
