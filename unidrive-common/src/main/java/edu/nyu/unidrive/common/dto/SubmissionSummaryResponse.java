package edu.nyu.unidrive.common.dto;

public final class SubmissionSummaryResponse {

    private final String submissionId;
    private final String assignmentId;
    private final String studentId;
    private final String fileName;
    private final String sha256;
    private final String status;

    public SubmissionSummaryResponse(
        String submissionId,
        String assignmentId,
        String studentId,
        String fileName,
        String sha256,
        String status
    ) {
        this.submissionId = submissionId;
        this.assignmentId = assignmentId;
        this.studentId = studentId;
        this.fileName = fileName;
        this.sha256 = sha256;
        this.status = status;
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

    public String getStatus() {
        return status;
    }
}
