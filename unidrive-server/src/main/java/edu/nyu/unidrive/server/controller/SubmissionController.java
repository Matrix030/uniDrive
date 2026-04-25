package edu.nyu.unidrive.server.controller;

import edu.nyu.unidrive.common.dto.ApiResponse;
import edu.nyu.unidrive.common.dto.SubmissionUploadResponse;
import edu.nyu.unidrive.server.service.SubmissionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping("/api/v1/submissions/{assignmentId}")
    public ResponseEntity<ApiResponse<?>> uploadSubmission(
        @PathVariable("assignmentId") String assignmentId,
        @RequestParam("studentId") String studentId,
        @RequestHeader("X-File-Sha256") String providedSha256,
        @RequestParam("file") MultipartFile file
    ) throws Exception {
        try {
            SubmissionUploadResponse response = submissionService.storeSubmission(
                assignmentId,
                studentId,
                providedSha256,
                file
            );

            return ResponseEntity.ok(ApiResponse.ok(response, "Submission uploaded successfully."));
        } catch (SubmissionService.HashMismatchException exception) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error("Uploaded file hash did not match the provided SHA-256."));
        }
    }
}
