package edu.nyu.unidrive.common.dto;

public final class AssignmentSummaryResponse {

    private final String assignmentId;
    private final String term;
    private final String course;
    private final String title;
    private final String fileName;
    private final String sha256;

    public AssignmentSummaryResponse(String assignmentId, String title, String fileName, String sha256) {
        this(assignmentId, null, null, title, fileName, sha256);
    }

    public AssignmentSummaryResponse(
        String assignmentId,
        String term,
        String course,
        String title,
        String fileName,
        String sha256
    ) {
        this.assignmentId = assignmentId;
        this.term = term;
        this.course = course;
        this.title = title;
        this.fileName = fileName;
        this.sha256 = sha256;
    }

    public String getAssignmentId() {
        return assignmentId;
    }

    public String getTerm() {
        return term;
    }

    public String getCourse() {
        return course;
    }

    public String getTitle() {
        return title;
    }

    public String getFileName() {
        return fileName;
    }

    public String getSha256() {
        return sha256;
    }
}
