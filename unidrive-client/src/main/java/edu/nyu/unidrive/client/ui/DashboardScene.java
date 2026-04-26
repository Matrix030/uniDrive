package edu.nyu.unidrive.client.ui;

import edu.nyu.unidrive.client.ClientRuntime;
import edu.nyu.unidrive.client.ClientRuntimeService;
import edu.nyu.unidrive.client.DefaultSyncServiceFactory;
import edu.nyu.unidrive.client.InstructorSyncServiceFactory;
import edu.nyu.unidrive.client.SyncServiceHandle;
import edu.nyu.unidrive.client.session.SessionConfig;
import edu.nyu.unidrive.client.session.UserRole;
import edu.nyu.unidrive.client.storage.ClientWorkspace;
import edu.nyu.unidrive.client.storage.FolderBootstrapService;
import edu.nyu.unidrive.client.storage.InstructorFolderBootstrapService;
import edu.nyu.unidrive.client.storage.InstructorWorkspace;
import edu.nyu.unidrive.client.storage.SyncStateRepository;
import edu.nyu.unidrive.client.storage.SyncStateRecord;
import edu.nyu.unidrive.client.sync.SyncDashboardSnapshot;
import edu.nyu.unidrive.client.sync.SyncDashboardSnapshotService;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class DashboardScene {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault());

    private final SessionConfig session;
    private final Runnable onLogout;
    private ClientRuntime studentRuntime;
    private SyncServiceHandle instructorSyncHandle;
    private InstructorWorkspace instructorWorkspace;
    private Path workspaceRoot;
    private Path submissionsDirectory;
    private Path databasePath;
    private SyncStateRepository dashboardRepository;
    private SyncDashboardSnapshotService dashboardSnapshotService;
    private ScheduledExecutorService dashboardRefreshExecutor;
    private Label pendingLabel;
    private Label uploadingLabel;
    private Label syncedLabel;
    private Label failedLabel;
    private Label refreshLabel;
    private TableView<SyncStateRecord> syncStateTable;

    public DashboardScene(SessionConfig session, Runnable onLogout) {
        this.session = session;
        this.onLogout = onLogout;
    }

    public Scene build() {
        startRuntime();

        dashboardRepository = new SyncStateRepository(databasePath);
        dashboardSnapshotService = new SyncDashboardSnapshotService(dashboardRepository);

        pendingLabel = new Label();
        uploadingLabel = new Label();
        syncedLabel = new Label();
        failedLabel = new Label();
        refreshLabel = new Label("Status refresh pending...");
        syncStateTable = createSyncStateTable();

        Button switchUserButton = new Button("Switch User");
        switchUserButton.setOnAction(event -> onLogout.run());

        HBox statusSummary = new HBox(16, pendingLabel, uploadingLabel, syncedLabel, failedLabel);

        VBox content = new VBox(
            12,
            new Label("University Drive (" + session.role().name().toLowerCase() + " mode)"),
            new Label("Workspace: " + workspaceRoot),
            new Label("Active folder: " + submissionsDirectory),
            new Label("User: " + session.userId()),
            new Label("Assignment: " + session.assignmentId()),
            new Label("Server: " + session.baseUrl()),
            new Label("Background sync: active"),
            statusSummary,
            refreshLabel,
            syncStateTable,
            switchUserButton
        );
        VBox.setVgrow(syncStateTable, Priority.ALWAYS);
        Scene scene = new Scene(content, 960, 520);

        applySnapshot(dashboardSnapshotService.loadSnapshot());
        startDashboardRefresh();
        return scene;
    }

    public void shutdown() {
        if (dashboardRefreshExecutor != null) {
            dashboardRefreshExecutor.shutdownNow();
            dashboardRefreshExecutor = null;
        }
        if (studentRuntime != null) {
            studentRuntime.close();
            studentRuntime = null;
        }
        if (instructorSyncHandle != null) {
            instructorSyncHandle.close();
            instructorSyncHandle = null;
        }
        dashboardSnapshotService = null;
        dashboardRepository = null;
    }

    private void startRuntime() {
        if (session.role() == UserRole.STUDENT) {
            studentRuntime = new ClientRuntimeService(new FolderBootstrapService(), new DefaultSyncServiceFactory())
                .start(session.workspaceDirectory(), session.assignmentId(), session.userId(), session.baseUrl());
            ClientWorkspace workspace = studentRuntime.workspace();
            workspaceRoot = workspace.rootDirectory();
            submissionsDirectory = workspace.submissionsDirectory();
            databasePath = workspace.databasePath();
        } else {
            instructorWorkspace = new InstructorFolderBootstrapService().bootstrap(session.workspaceDirectory());
            instructorSyncHandle = new InstructorSyncServiceFactory()
                .create(instructorWorkspace, session.assignmentId(), session.baseUrl());
            instructorSyncHandle.start();
            workspaceRoot = instructorWorkspace.rootDirectory();
            submissionsDirectory = instructorWorkspace.publishDirectory();
            databasePath = instructorWorkspace.databasePath();
        }
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
