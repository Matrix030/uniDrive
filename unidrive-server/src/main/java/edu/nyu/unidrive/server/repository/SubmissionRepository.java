package edu.nyu.unidrive.server.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SubmissionRepository {

    private final JdbcTemplate jdbcTemplate;

    public SubmissionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(
        String submissionId,
        String assignmentId,
        String studentId,
        String filePath,
        String sha256,
        long submittedAt,
        String status
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO submissions (id, assignment_id, student_id, file_path, hash, submitted_at, status)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            submissionId,
            assignmentId,
            studentId,
            filePath,
            sha256,
            submittedAt,
            status
        );
    }
}
