package edu.nyu.unidrive.server.service;

import edu.nyu.unidrive.common.dto.AssignmentSummaryResponse;
import edu.nyu.unidrive.common.util.FileHasher;
import edu.nyu.unidrive.server.repository.AssignmentRepository;
import edu.nyu.unidrive.server.repository.AssignmentRepository.StoredAssignment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AssignmentService {

    private final Path storageRoot;
    private final AssignmentRepository assignmentRepository;

    public AssignmentService(
        @Value("${unidrive.storage.root:unidrive-server/target/storage}") String storageRoot,
        AssignmentRepository assignmentRepository
    ) {
        this.storageRoot = Path.of(storageRoot);
        this.assignmentRepository = assignmentRepository;
    }

    public AssignmentSummaryResponse publishAssignment(
        String term,
        String course,
        String assignmentId,
        String title,
        MultipartFile file
    ) throws IOException {
        byte[] content = file.getBytes();
        String sha256 = FileHasher.sha256Hex(content);
        String fileName = sanitizeFileName(file.getOriginalFilename());
        Path destination = storageRoot
            .resolve(term)
            .resolve(course)
            .resolve(assignmentId)
            .resolve("publish")
            .resolve(fileName);

        Files.createDirectories(destination.getParent());
        Files.write(destination, content);
        assignmentRepository.save(
            assignmentId,
            fileName,
            term,
            course,
            title,
            System.currentTimeMillis(),
            destination.toString(),
            sha256
        );

        return new AssignmentSummaryResponse(assignmentId, term, course, title, fileName, sha256);
    }

    public List<AssignmentSummaryResponse> listAssignments(String term, String course) {
        return assignmentRepository.findByTermAndCourse(term, course);
    }

    public DownloadedAssignment loadAssignment(String assignmentId, String fileName) throws IOException {
        StoredAssignment assignment = assignmentRepository.findStoredAssignmentByIdAndFileName(assignmentId, fileName)
            .orElseThrow(AssignmentNotFoundException::new);
        return new DownloadedAssignment(assignment.originalFileName(), Files.readAllBytes(Path.of(assignment.filePath())));
    }

    private String sanitizeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "assignment.bin";
        }
        Path fileNamePath = Path.of(originalFileName).getFileName();
        return fileNamePath == null ? "assignment.bin" : fileNamePath.toString();
    }

    public static final class AssignmentNotFoundException extends RuntimeException {
    }

    public record DownloadedAssignment(String fileName, byte[] content) {
    }
}
