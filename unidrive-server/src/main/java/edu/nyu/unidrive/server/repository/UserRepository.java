package edu.nyu.unidrive.server.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<StoredUser> findById(String userId) {
        List<StoredUser> users = jdbcTemplate.query(
            "SELECT id, name, role FROM users WHERE id = ?",
            (resultSet, rowNum) -> new StoredUser(
                resultSet.getString("id"),
                resultSet.getString("name"),
                resultSet.getString("role")
            ),
            userId
        );
        return users.stream().findFirst();
    }

    public void save(String id, String name, String role) {
        jdbcTemplate.update(
            "INSERT INTO users (id, name, role) VALUES (?, ?, ?)",
            id,
            name,
            role
        );
    }

    public void updateRole(String id, String role) {
        jdbcTemplate.update("UPDATE users SET role = ? WHERE id = ?", role, id);
    }

    public record StoredUser(String id, String name, String role) {
    }
}
