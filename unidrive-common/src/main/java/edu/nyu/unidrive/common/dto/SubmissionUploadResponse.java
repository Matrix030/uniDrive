package edu.nyu.unidrive.common.dto;

public final class SubmissionUploadResponse {

    private final String submissionId;
    private final String term;
    private final String course;
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
        this(submissionId, null, null, assignmentId, studentId, fileName, sha256);
    }

    public SubmissionUploadResponse(
        String submissionId,
        String term,
        String course,
        String assignmentId,
        String studentId,
        String fileName,
        String sha256
    ) {
        this.submissionId = submissionId;
        this.term = term;
        this.course = course;
        this.assignmentId = assignmentId;
        this.studentId = studentId;
        this.fileName = fileName;
        this.sha256 = sha256;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public String getTerm() {
        return term;
    }

    public String getCourse() {
        return course;
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
