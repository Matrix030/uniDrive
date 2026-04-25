package edu.nyu.unidrive.server.service;

import edu.nyu.unidrive.common.dto.SubmissionUploadResponse;
import edu.nyu.unidrive.common.util.FileHasher;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SubmissionService {

    private final Path storageRoot;

    public SubmissionService(@Value("${unidrive.storage.root:unidrive-server/target/storage}") String storageRoot) {
        this.storageRoot = Path.of(storageRoot);
    }

    public SubmissionUploadResponse storeSubmission(
        String assignmentId,
        String studentId,
        String providedSha256,
        MultipartFile file
    ) throws IOException {
        byte[] content = file.getBytes();
        String computedSha256 = FileHasher.sha256Hex(content);

        if (!computedSha256.equals(providedSha256)) {
            throw new HashMismatchException();
        }

        String submissionId = UUID.randomUUID().toString();
        String fileName = sanitizeFileName(file.getOriginalFilename());
        Path destination = storageRoot
            .resolve("submissions")
            .resolve(assignmentId)
            .resolve(studentId)
            .resolve(submissionId + "-" + fileName);

        Files.createDirectories(destination.getParent());
        Files.write(destination, content);

        return new SubmissionUploadResponse(
            submissionId,
            assignmentId,
            studentId,
            fileName,
            computedSha256
        );
    }

    private String sanitizeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "upload.bin";
        }

        Path fileNamePath = Path.of(originalFileName).getFileName();
        return fileNamePath == null ? "upload.bin" : fileNamePath.toString();
    }

    public static final class HashMismatchException extends RuntimeException {
    }
}
