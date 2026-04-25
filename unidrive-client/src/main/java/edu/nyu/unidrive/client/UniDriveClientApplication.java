package edu.nyu.unidrive.client;

import edu.nyu.unidrive.client.storage.SyncStateRecord;
import edu.nyu.unidrive.client.storage.SyncStateRepository;
import edu.nyu.unidrive.client.sync.SyncDashboardSnapshot;
import edu.nyu.unidrive.client.sync.SyncDashboardSnapshotService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class UniDriveClientApplication extends Application {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault());

    private ClientRuntime runtime;
    private SyncStateRepository dashboardRepository;
    private SyncDashboardSnapshotService dashboardSnapshotService;
    private ScheduledExecutorService dashboardRefreshExecutor;
    private Label pendingLabel;
    private Label uploadingLabel;
    private Label syncedLabel;
    private Label failedLabel;
    private Label refreshLabel;
    private TableView<SyncStateRecord> syncStateTable;

    @Override
    public void start(Stage stage) {
        ClientStartupConfig config = ClientStartupConfig.fromSystemProperties();
        runtime = new ClientRuntimeService(new edu.nyu.unidrive.client.storage.FolderBootstrapService(), new DefaultSyncServiceFactory())
            .start(config.rootDirectory(), config.assignmentId(), config.studentId(), config.baseUrl());
        dashboardRepository = new SyncStateRepository(runtime.workspace().databasePath());
        dashboardSnapshotService = new SyncDashboardSnapshotService(dashboardRepository);

        pendingLabel = new Label();
        uploadingLabel = new Label();
        syncedLabel = new Label();
        failedLabel = new Label();
        refreshLabel = new Label("Status refresh pending...");
        syncStateTable = createSyncStateTable();

        HBox statusSummary = new HBox(
            16,
            pendingLabel,
            uploadingLabel,
            syncedLabel,
            failedLabel
        );

        VBox content = new VBox(
            12,
            new Label("University Drive client is running."),
            new Label("Workspace: " + runtime.workspace().rootDirectory()),
            new Label("Submissions folder: " + runtime.workspace().submissionsDirectory()),
            new Label("Student: " + config.studentId()),
            new Label("Assignment: " + config.assignmentId()),
            new Label("Server: " + config.baseUrl()),
            new Label("Background sync: active"),
            statusSummary,
            refreshLabel,
            syncStateTable
        );
        VBox.setVgrow(syncStateTable, Priority.ALWAYS);
        Scene scene = new Scene(content, 960, 520);

        stage.setTitle("University Drive");
        stage.setScene(scene);
        stage.show();

        applySnapshot(dashboardSnapshotService.loadSnapshot());
        startDashboardRefresh();
    }

    @Override
    public void stop() {
        if (dashboardRefreshExecutor != null) {
            dashboardRefreshExecutor.shutdownNow();
            dashboardRefreshExecutor = null;
        }

        if (runtime != null) {
            runtime.close();
            runtime = null;
        }

        dashboardSnapshotService = null;
        dashboardRepository = null;
    }

    public static void main(String[] args) {
        launch(args);
    }

    private TableView<SyncStateRecord> createSyncStateTable() {
        TableView<SyncStateRecord> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<SyncStateRecord, String> fileColumn = new TableColumn<>("File");
        fileColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().localPath().getFileName().toString()));

        TableColumn<SyncStateRecord, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().status().name()));

        TableColumn<SyncStateRecord, String> remoteIdColumn = new TableColumn<>("Remote ID");
        remoteIdColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(nullToDash(cell.getValue().remoteId())));

        TableColumn<SyncStateRecord, String> hashColumn = new TableColumn<>("SHA-256");
        hashColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(shortenHash(cell.getValue().sha256())));

        TableColumn<SyncStateRecord, String> lastSyncedColumn = new TableColumn<>("Last Synced");
        lastSyncedColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatLastSynced(cell.getValue().lastSynced())));

        table.getColumns().addAll(fileColumn, statusColumn, remoteIdColumn, hashColumn, lastSyncedColumn);
        return table;
    }

    private void startDashboardRefresh() {
        dashboardRefreshExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "unidrive-dashboard-refresh");
            thread.setDaemon(true);
            return thread;
        });

        dashboardRefreshExecutor.scheduleAtFixedRate(() -> {
            SyncDashboardSnapshot snapshot = dashboardSnapshotService.loadSnapshot();
            Platform.runLater(() -> applySnapshot(snapshot));
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void applySnapshot(SyncDashboardSnapshot snapshot) {
        pendingLabel.setText("Pending: " + snapshot.pendingCount());
        uploadingLabel.setText("Uploading: " + snapshot.uploadingCount());
        syncedLabel.setText("Synced: " + snapshot.syncedCount());
        failedLabel.setText("Failed: " + snapshot.failedCount());
        refreshLabel.setText("Last refresh: " + TIME_FORMATTER.format(Instant.now()));
        syncStateTable.setItems(FXCollections.observableArrayList(snapshot.rows()));
    }

    private String shortenHash(String sha256) {
        if (sha256 == null || sha256.isBlank()) {
            return "-";
        }
        return sha256.length() <= 12 ? sha256 : sha256.substring(0, 12) + "...";
    }

    private String formatLastSynced(long lastSynced) {
        if (lastSynced <= 0L) {
            return "never";
        }
        return TIME_FORMATTER.format(Instant.ofEpochMilli(lastSynced));
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
