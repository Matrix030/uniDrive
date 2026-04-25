package edu.nyu.unidrive.client.storage;

import edu.nyu.unidrive.common.model.SyncStatus;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public final class SyncStateRepository {

    private final Path databasePath;
    private final String jdbcUrl;

    public SyncStateRepository(Path databasePath) {
        this.databasePath = databasePath;
        this.jdbcUrl = "jdbc:sqlite:" + databasePath;
        initializeSchema();
    }

    public void save(SyncStateRecord record) {
        String sql = """
            INSERT INTO sync_state (local_path, remote_id, sha256, status, last_synced)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(local_path) DO UPDATE SET
                remote_id = excluded.remote_id,
                sha256 = excluded.sha256,
                status = excluded.status,
                last_synced = excluded.last_synced
            """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, record.localPath().toString());
            statement.setString(2, record.remoteId());
            statement.setString(3, record.sha256());
            statement.setString(4, record.status().name());
            statement.setLong(5, record.lastSynced());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save sync state.", exception);
        }
    }

    public Optional<SyncStateRecord> findByLocalPath(Path localPath) {
        String sql = "SELECT local_path, remote_id, sha256, status, last_synced FROM sync_state WHERE local_path = ?";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, localPath.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(new SyncStateRecord(
                    Path.of(resultSet.getString("local_path")),
                    resultSet.getString("remote_id"),
                    resultSet.getString("sha256"),
                    SyncStatus.valueOf(resultSet.getString("status")),
                    resultSet.getLong("last_synced")
                ));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load sync state.", exception);
        }
    }

    public boolean databaseExists() {
        return Files.exists(databasePath);
    }

    private void initializeSchema() {
        try {
            Path parent = databasePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to create sync state database directory.", exception);
        }

        String schemaSql = """
            CREATE TABLE IF NOT EXISTS sync_state (
                local_path TEXT PRIMARY KEY,
                remote_id TEXT,
                sha256 TEXT,
                status TEXT NOT NULL,
                last_synced INTEGER NOT NULL
            )
            """;

        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(schemaSql);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to initialize sync state schema.", exception);
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }
}
