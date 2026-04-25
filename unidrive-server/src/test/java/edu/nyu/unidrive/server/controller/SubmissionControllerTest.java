package edu.nyu.unidrive.server.controller;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.nyu.unidrive.common.util.FileHasher;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.FileSystemUtils;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "unidrive.storage.root=target/test-storage")
class SubmissionControllerTest {

    private static final Path STORAGE_ROOT = Path.of("target/test-storage");

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void clearStorageRoot() throws IOException {
        FileSystemUtils.deleteRecursively(STORAGE_ROOT);
        Files.createDirectories(STORAGE_ROOT);
    }

    @Test
    void uploadSubmissionStoresFileAndReturnsReceiptWhenHashMatches() throws Exception {
        byte[] content = "public class Hello { }".getBytes();
        String sha256 = FileHasher.sha256Hex(content);
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
    }

    @Test
    void uploadSubmissionRejectsHashMismatchAndDoesNotStoreFile() throws Exception {
        byte[] content = "class BrokenHash { }".getBytes();
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
    }
}
