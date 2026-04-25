package edu.nyu.unidrive.server.service;

import edu.nyu.unidrive.common.dto.SubmissionSummaryResponse;
import edu.nyu.unidrive.common.dto.SubmissionUploadResponse;
import edu.nyu.unidrive.common.model.SyncStatus;
import edu.nyu.unidrive.server.repository.SubmissionRepository.StoredSubmission;
import edu.nyu.unidrive.common.util.FileHasher;
import edu.nyu.unidrive.server.repository.SubmissionRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SubmissionService {

    private final Path storageRoot;
    private final SubmissionRepository submissionRepository;

    public SubmissionService(
        @Value("${unidrive.storage.root:unidrive-server/target/storage}") String storageRoot,
        SubmissionRepository submissionRepository
    ) {
        this.storageRoot = Path.of(storageRoot);
        this.submissionRepository = submissionRepository;
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
        submissionRepository.save(
            submissionId,
            assignmentId,
            studentId,
            destination.toString(),
            computedSha256,
            System.currentTimeMillis(),
            SyncStatus.SYNCED.name()
        );

        return new SubmissionUploadResponse(
            submissionId,
            assignmentId,
            studentId,
            fileName,
            computedSha256
        );
    }

    public List<SubmissionSummaryResponse> listSubmissions(String assignmentId, String studentId) {
        return submissionRepository.findByFilters(assignmentId, studentId);
    }

    public DownloadedSubmission loadSubmission(String submissionId) throws IOException {
        StoredSubmission storedSubmission = submissionRepository.findStoredSubmissionById(submissionId)
            .orElseThrow(SubmissionNotFoundException::new);

        Path filePath = Path.of(storedSubmission.filePath());
        byte[] content = Files.readAllBytes(filePath);
        return new DownloadedSubmission(storedSubmission.originalFileName(), content);
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

    public static final class SubmissionNotFoundException extends RuntimeException {
    }

    public record DownloadedSubmission(String fileName, byte[] content) {
    }
}
