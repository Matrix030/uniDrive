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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ReceivedStateRepository {

    private final Path databasePath;
    private final String jdbcUrl;

    public ReceivedStateRepository(Path databasePath) {
        this.databasePath = databasePath;
        this.jdbcUrl = "jdbc:sqlite:" + databasePath;
        initializeSchema();
    }

    public void save(ReceivedStateRecord record) {
        String sql = """
            INSERT INTO received_state (local_path, remote_id, sha256, status, last_synced, source)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(local_path) DO UPDATE SET
                remote_id = excluded.remote_id,
                sha256 = excluded.sha256,
                status = excluded.status,
                last_synced = excluded.last_synced,
                source = excluded.source
            """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, record.localPath().toString());
            statement.setString(2, record.remoteId());
            statement.setString(3, record.sha256());
            statement.setString(4, record.status().name());
            statement.setLong(5, record.lastSynced());
            statement.setString(6, record.source());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save received state.", exception);
        }
    }

    public Optional<ReceivedStateRecord> findByLocalPath(Path localPath) {
        String sql = "SELECT local_path, remote_id, sha256, status, last_synced, source FROM received_state WHERE local_path = ?";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, localPath.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(resultSet));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load received state.", exception);
        }
    }

    public List<ReceivedStateRecord> findAll() {
        String sql = "SELECT local_path, remote_id, sha256, status, last_synced, source FROM received_state ORDER BY local_path ASC";
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            List<ReceivedStateRecord> records = new ArrayList<>();
            while (resultSet.next()) {
                records.add(mapRow(resultSet));
            }
            return records;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load all received state rows.", exception);
        }
    }

    private ReceivedStateRecord mapRow(ResultSet resultSet) throws SQLException {
        return new ReceivedStateRecord(
            Path.of(resultSet.getString("local_path")),
            resultSet.getString("remote_id"),
            resultSet.getString("sha256"),
            SyncStatus.valueOf(resultSet.getString("status")),
            resultSet.getLong("last_synced"),
            resultSet.getString("source")
        );
    }

    private void initializeSchema() {
        try {
            Path parent = databasePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to create received state database directory.", exception);
        }

        String schemaSql = """
            CREATE TABLE IF NOT EXISTS received_state (
                local_path TEXT PRIMARY KEY,
                remote_id TEXT,
                sha256 TEXT,
                status TEXT NOT NULL,
                last_synced INTEGER NOT NULL,
                source TEXT NOT NULL
            )
            """;

        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(schemaSql);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to initialize received state schema.", exception);
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }
}

