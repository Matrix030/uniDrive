package edu.nyu.unidrive.server.repository;

import edu.nyu.unidrive.common.dto.SubmissionSummaryResponse;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class SubmissionRepository {

    private static final RowMapper<StoredSubmission> STORED_SUBMISSION_ROW_MAPPER = (resultSet, rowNum) ->
        new StoredSubmission(
            resultSet.getString("id"),
            resultSet.getString("file_path")
        );

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

    public List<SubmissionSummaryResponse> findByFilters(String assignmentId, String studentId) {
        String sql = studentId == null || studentId.isBlank()
            ? """
            SELECT id, assignment_id, student_id, file_path, hash, status
            FROM submissions
            WHERE assignment_id = ?
            ORDER BY submitted_at DESC
            """
            : """
            SELECT id, assignment_id, student_id, file_path, hash, status
            FROM submissions
            WHERE assignment_id = ? AND student_id = ?
            ORDER BY submitted_at DESC
            """;

        return jdbcTemplate.query(
            sql,
            (resultSet, rowNum) -> {
                String submissionId = resultSet.getString("id");
                String storedFilePath = resultSet.getString("file_path");
                String storedFileName = Path.of(storedFilePath).getFileName().toString();
                String originalFileName = storedFileName.substring(submissionId.length() + 1);

                return new SubmissionSummaryResponse(
                    submissionId,
                    resultSet.getString("assignment_id"),
                    resultSet.getString("student_id"),
                    originalFileName,
                    resultSet.getString("hash"),
                    resultSet.getString("status")
                );
            },
            studentId == null || studentId.isBlank()
                ? new Object[] {assignmentId}
                : new Object[] {assignmentId, studentId}
        );
    }

    public Optional<StoredSubmission> findStoredSubmissionById(String submissionId) {
        List<StoredSubmission> submissions = jdbcTemplate.query(
            "SELECT id, file_path FROM submissions WHERE id = ?",
            STORED_SUBMISSION_ROW_MAPPER,
            submissionId
        );
        return submissions.stream().findFirst();
    }

    public Optional<StoredSubmissionDetails> findSubmissionDetailsById(String submissionId) {
        List<StoredSubmissionDetails> submissions = jdbcTemplate.query(
            "SELECT id, assignment_id, student_id, file_path, hash, status FROM submissions WHERE id = ?",
            (resultSet, rowNum) -> new StoredSubmissionDetails(
                resultSet.getString("id"),
                resultSet.getString("assignment_id"),
                resultSet.getString("student_id"),
                resultSet.getString("file_path"),
                resultSet.getString("hash"),
                resultSet.getString("status")
            ),
            submissionId
        );
        return submissions.stream().findFirst();
    }

    public record StoredSubmission(String id, String filePath) {
        public String originalFileName() {
            String storedFileName = Path.of(filePath).getFileName().toString();
            return storedFileName.substring(id.length() + 1);
        }
    }

    public record StoredSubmissionDetails(
        String id,
        String assignmentId,
        String studentId,
        String filePath,
        String sha256,
        String status
    ) {
    }
}
