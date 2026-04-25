package edu.nyu.unidrive.common.dto;

public final class SubmissionUploadResponse {

    private final String submissionId;
    private final String assignmentId;
    private final String studentId;
    private final String fileName;
    private final String sha256;

    public SubmissionUploadResponse(
        String submissionId,
        String assignmentId,
        String studentId,
        String fileName,
        String sha256
    ) {
        this.submissionId = submissionId;
        this.assignmentId = assignmentId;
        this.studentId = studentId;
        this.fileName = fileName;
        this.sha256 = sha256;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public String getAssignmentId() {
        return assignmentId;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getSha256() {
        return sha256;
    }
}
