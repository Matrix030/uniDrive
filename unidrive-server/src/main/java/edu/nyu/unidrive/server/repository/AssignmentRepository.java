package edu.nyu.unidrive.server.repository;

import edu.nyu.unidrive.common.dto.AssignmentSummaryResponse;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AssignmentRepository {

    private final JdbcTemplate jdbcTemplate;

    public AssignmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(String assignmentId, String title, long publishedAt, String filePath, String sha256) {
        jdbcTemplate.update(
            "INSERT INTO assignments (id, title, published_at, file_path, hash) VALUES (?, ?, ?, ?, ?)",
            assignmentId,
            title,
            publishedAt,
            filePath,
            sha256
        );
    }

    public List<AssignmentSummaryResponse> findAll() {
        return jdbcTemplate.query(
            "SELECT id, title, file_path, hash FROM assignments ORDER BY published_at DESC",
            (resultSet, rowNum) -> new AssignmentSummaryResponse(
                resultSet.getString("id"),
                resultSet.getString("title"),
                Path.of(resultSet.getString("file_path")).getFileName().toString().substring(37),
                resultSet.getString("hash")
            )
        );
    }

    public Optional<StoredAssignment> findStoredAssignmentById(String assignmentId) {
        List<StoredAssignment> assignments = jdbcTemplate.query(
            "SELECT id, title, file_path, hash FROM assignments WHERE id = ?",
            (resultSet, rowNum) -> new StoredAssignment(
                resultSet.getString("id"),
                resultSet.getString("title"),
                resultSet.getString("file_path"),
                resultSet.getString("hash")
            ),
            assignmentId
        );
        return assignments.stream().findFirst();
    }

    public record StoredAssignment(String id, String title, String filePath, String sha256) {
        public String originalFileName() {
            String storedFileName = Path.of(filePath).getFileName().toString();
            return storedFileName.substring(id.length() + 1);
        }
    }
}
