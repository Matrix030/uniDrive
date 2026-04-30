package edu.nyu.unidrive.server.repository;

import edu.nyu.unidrive.common.dto.AssignmentSummaryResponse;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AssignmentRepository {

    private static final RowMapper<AssignmentSummaryResponse> SUMMARY_ROW_MAPPER = (resultSet, rowNum) ->
        new AssignmentSummaryResponse(
            resultSet.getString("id"),
            resultSet.getString("term"),
            resultSet.getString("course"),
            resultSet.getString("title"),
            Path.of(resultSet.getString("file_path")).getFileName().toString(),
            resultSet.getString("hash")
        );

    private final JdbcTemplate jdbcTemplate;

    public AssignmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(
        String assignmentId,
        String term,
        String course,
        String title,
        long publishedAt,
        String filePath,
        String sha256
    ) {
        jdbcTemplate.update(
            "INSERT INTO assignments (id, term, course, title, published_at, file_path, hash) VALUES (?, ?, ?, ?, ?, ?, ?)",
            assignmentId,
            term,
            course,
            title,
            publishedAt,
            filePath,
            sha256
        );
    }

    public List<AssignmentSummaryResponse> findByTermAndCourse(String term, String course) {
        return jdbcTemplate.query(
            "SELECT id, term, course, title, file_path, hash FROM assignments "
                + "WHERE term = ? AND course = ? ORDER BY published_at DESC",
            SUMMARY_ROW_MAPPER,
            term,
            course
        );
    }

    public List<AssignmentSummaryResponse> findAll() {
        return jdbcTemplate.query(
            "SELECT id, term, course, title, file_path, hash FROM assignments ORDER BY published_at DESC",
            SUMMARY_ROW_MAPPER
        );
    }

    public Optional<StoredAssignment> findStoredAssignmentById(String assignmentId) {
        List<StoredAssignment> assignments = jdbcTemplate.query(
            "SELECT id, term, course, title, file_path, hash FROM assignments WHERE id = ?",
            (resultSet, rowNum) -> new StoredAssignment(
                resultSet.getString("id"),
                resultSet.getString("term"),
                resultSet.getString("course"),
                resultSet.getString("title"),
                resultSet.getString("file_path"),
                resultSet.getString("hash")
            ),
            assignmentId
        );
        return assignments.stream().findFirst();
    }

    public record StoredAssignment(
        String id,
        String term,
        String course,
        String title,
        String filePath,
        String sha256
    ) {
        public String originalFileName() {
            return Path.of(filePath).getFileName().toString();
        }
    }
}
