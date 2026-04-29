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
import edu.nyu.unidrive.client.storage.ReceivedStateRecord;
import edu.nyu.unidrive.client.storage.ReceivedStateRepository;
import edu.nyu.unidrive.client.storage.SyncStateRepository;
import edu.nyu.unidrive.client.storage.SyncStateRecord;
import edu.nyu.unidrive.client.sync.ReceivedReconcileService;
import edu.nyu.unidrive.client.sync.SyncDashboardSnapshot;
import edu.nyu.unidrive.client.sync.SyncDashboardSnapshotService;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
    private Path assignmentsDirectory;
    private Path feedbackDirectory;
    private Path publishDirectory;
    private Path databasePath;
    private SyncStateRepository dashboardRepository;
    private ReceivedStateRepository receivedStateRepository;
    private SyncDashboardSnapshotService dashboardSnapshotService;
    private ScheduledExecutorService dashboardRefreshExecutor;
    private SyncDashboardSnapshot latestSnapshot;
    private Label pendingLabel;
    private Label uploadingLabel;
    private Label syncedLabel;
    private Label failedLabel;
    private Label refreshLabel;
    private Label activeFolderLabel;
    private TableView<DashboardRow> syncStateTable;
    private StudentView studentView = StudentView.SUBMITTED;
    private Button submittedButton;
    private Button assignmentsButton;
    private Button feedbackButton;
    private InstructorView instructorView = InstructorView.PUBLISHED;
    private Button publishedButton;
    private Button instructorSubmissionsButton;
    private Button instructorFeedbacksButton;
    private static final String VIEW_BUTTON_STYLE =
        "-fx-background-color: #f4f4f4; -fx-border-color: #c7c7c7; -fx-border-radius: 3; -fx-background-radius: 3;";
    private static final String VIEW_BUTTON_STYLE_ACTIVE =
        "-fx-background-color: #dbeafe; -fx-border-color: #2563eb; -fx-text-fill: #1e3a8a; -fx-font-weight: bold; -fx-border-radius: 3; -fx-background-radius: 3;";

    public DashboardScene(SessionConfig session, Runnable onLogout) {
        this.session = session;
        this.onLogout = onLogout;
    }

    public Scene build() {
        startRuntime();

        dashboardRepository = new SyncStateRepository(databasePath);
        receivedStateRepository = new ReceivedStateRepository(databasePath);
        dashboardSnapshotService = new SyncDashboardSnapshotService(dashboardRepository);

        pendingLabel = new Label();
        uploadingLabel = new Label();
        syncedLabel = new Label();
        failedLabel = new Label();
        refreshLabel = new Label("Status refresh pending...");
        activeFolderLabel = new Label();
        syncStateTable = createSyncStateTable();

        HBox studentViewToggle = createStudentViewToggleIfApplicable();

        Button switchUserButton = new Button("Switch User");
        switchUserButton.setOnAction(event -> onLogout.run());

        HBox statusSummary = new HBox(16, pendingLabel, uploadingLabel, syncedLabel, failedLabel);

        VBox content = new VBox(
            12,
            new Label("University Drive (" + session.role().name().toLowerCase() + " mode)"),
            new Label("Workspace: " + workspaceRoot),
            activeFolderLabel,
            new Label("User: " + session.userId()),
            new Label("Assignment: " + session.assignmentId()),
            new Label("Server: " + session.baseUrl()),
            new Label("Background sync: active"),
            statusSummary,
            refreshLabel,
            studentViewToggle,
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
            assignmentsDirectory = workspace.assignmentsDirectory();
            feedbackDirectory = workspace.feedbackDirectory();
            databasePath = workspace.databasePath();
        } else {
            instructorWorkspace = new InstructorFolderBootstrapService().bootstrap(session.workspaceDirectory());
            instructorSyncHandle = new InstructorSyncServiceFactory()
                .create(instructorWorkspace, session.assignmentId(), session.baseUrl());
            instructorSyncHandle.start();
            workspaceRoot = instructorWorkspace.rootDirectory();
            publishDirectory = instructorWorkspace.publishDirectory();
            submissionsDirectory = instructorWorkspace.submissionsDirectory();
            feedbackDirectory = instructorWorkspace.feedbackDirectory();
            databasePath = instructorWorkspace.databasePath();
        }
    }

    private TableView<DashboardRow> createSyncStateTable() {
        TableView<DashboardRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<DashboardRow, String> fileColumn = new TableColumn<>("File");
        fileColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().fileName()));

        TableColumn<DashboardRow, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(nullToEmpty(cell.getValue().status())));

        TableColumn<DashboardRow, String> remoteIdColumn = new TableColumn<>("Remote ID");
        remoteIdColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(nullToEmpty(cell.getValue().remoteId())));

        TableColumn<DashboardRow, String> hashColumn = new TableColumn<>("SHA-256");
        hashColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(nullToEmpty(shortenHash(cell.getValue().sha256()))));

        TableColumn<DashboardRow, String> lastSyncedColumn = new TableColumn<>("Last Synced");
        lastSyncedColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(nullToEmpty(formatLastSynced(cell.getValue().lastSynced()))));

        table.getColumns().add(fileColumn);
        table.getColumns().add(statusColumn);
        table.getColumns().add(remoteIdColumn);
        table.getColumns().add(hashColumn);
        table.getColumns().add(lastSyncedColumn);
        return table;
    }

    private HBox createStudentViewToggleIfApplicable() {
        if (session.role() == UserRole.INSTRUCTOR) {
            return createInstructorViewToggle();
        }
        if (session.role() != UserRole.STUDENT) {
            return new HBox();
        }

        submittedButton = new Button("Submissions");
        assignmentsButton = new Button("Assignments");
        feedbackButton = new Button("Feedbacks");

        submittedButton.setFocusTraversable(false);
        assignmentsButton.setFocusTraversable(false);
        feedbackButton.setFocusTraversable(false);

        submittedButton.setOnAction(event -> setStudentView(StudentView.SUBMITTED));
        assignmentsButton.setOnAction(event -> setStudentView(StudentView.ASSIGNMENTS));
        feedbackButton.setOnAction(event -> setStudentView(StudentView.FEEDBACK));

        HBox box = new HBox(12, submittedButton, assignmentsButton, feedbackButton);
        setStudentView(StudentView.SUBMITTED);
        return box;
    }

    private HBox createInstructorViewToggle() {
        publishedButton = new Button("Published");
        instructorSubmissionsButton = new Button("Submissions");
        instructorFeedbacksButton = new Button("Feedbacks");

        publishedButton.setFocusTraversable(false);
        instructorSubmissionsButton.setFocusTraversable(false);
        instructorFeedbacksButton.setFocusTraversable(false);

        publishedButton.setOnAction(event -> setInstructorView(InstructorView.PUBLISHED));
        instructorSubmissionsButton.setOnAction(event -> setInstructorView(InstructorView.SUBMISSIONS));
        instructorFeedbacksButton.setOnAction(event -> setInstructorView(InstructorView.FEEDBACKS));

        HBox box = new HBox(12, publishedButton, instructorSubmissionsButton, instructorFeedbacksButton);
        setInstructorView(InstructorView.PUBLISHED);
        return box;
    }

    private void setInstructorView(InstructorView view) {
        if (session.role() != UserRole.INSTRUCTOR) {
            return;
        }
        this.instructorView = view;
        if (publishedButton != null && instructorSubmissionsButton != null && instructorFeedbacksButton != null) {
            publishedButton.setStyle(view == InstructorView.PUBLISHED ? VIEW_BUTTON_STYLE_ACTIVE : VIEW_BUTTON_STYLE);
            instructorSubmissionsButton.setStyle(view == InstructorView.SUBMISSIONS ? VIEW_BUTTON_STYLE_ACTIVE : VIEW_BUTTON_STYLE);
            instructorFeedbacksButton.setStyle(view == InstructorView.FEEDBACKS ? VIEW_BUTTON_STYLE_ACTIVE : VIEW_BUTTON_STYLE);
        }

        if (view == InstructorView.PUBLISHED) {
            activeFolderLabel.setText("Active folder: " + publishDirectory);
            if (latestSnapshot != null) {
                syncStateTable.setItems(FXCollections.observableArrayList(mapSnapshotRows(latestSnapshot.rows())));
            }
        } else if (view == InstructorView.SUBMISSIONS) {
            activeFolderLabel.setText("Active folder: " + submissionsDirectory);
            syncStateTable.setItems(FXCollections.observableArrayList(loadInstructorRows(ReceivedReconcileService.SOURCE_INSTRUCTOR_SUBMISSIONS, submissionsDirectory)));
        } else {
            activeFolderLabel.setText("Active folder: " + feedbackDirectory);
            syncStateTable.setItems(FXCollections.observableArrayList(loadInstructorRows(ReceivedReconcileService.SOURCE_INSTRUCTOR_FEEDBACKS, feedbackDirectory)));
        }

        syncStateTable.requestFocus();
        refreshStatusSummary();
    }

    private void setStudentView(StudentView view) {
        if (session.role() != UserRole.STUDENT) {
            return;
        }
        this.studentView = view;
        updateStudentViewButtonStyles();

        if (view == StudentView.SUBMITTED) {
            activeFolderLabel.setText("Active folder: " + submissionsDirectory);
            if (latestSnapshot != null) {
                syncStateTable.setItems(FXCollections.observableArrayList(mapSnapshotRows(latestSnapshot.rows())));
            }
        } else if (view == StudentView.ASSIGNMENTS) {
            activeFolderLabel.setText("Active folder: " + assignmentsDirectory);
            syncStateTable.setItems(FXCollections.observableArrayList(loadReceivedRows("ASSIGNMENTS")));
        } else {
            activeFolderLabel.setText("Active folder: " + feedbackDirectory);
            syncStateTable.setItems(FXCollections.observableArrayList(loadReceivedRows("FEEDBACK")));
        }

        syncStateTable.requestFocus();
        refreshStatusSummary();
    }

    private void updateStudentViewButtonStyles() {
        if (submittedButton == null || assignmentsButton == null || feedbackButton == null) {
            return;
        }
        submittedButton.setStyle(studentView == StudentView.SUBMITTED ? VIEW_BUTTON_STYLE_ACTIVE : VIEW_BUTTON_STYLE);
        assignmentsButton.setStyle(studentView == StudentView.ASSIGNMENTS ? VIEW_BUTTON_STYLE_ACTIVE : VIEW_BUTTON_STYLE);
        feedbackButton.setStyle(studentView == StudentView.FEEDBACK ? VIEW_BUTTON_STYLE_ACTIVE : VIEW_BUTTON_STYLE);
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
        latestSnapshot = snapshot;
        refreshLabel.setText("Last refresh: " + TIME_FORMATTER.format(Instant.now()));
        if (session.role() == UserRole.INSTRUCTOR && instructorView != InstructorView.PUBLISHED) {
            String source = instructorView == InstructorView.SUBMISSIONS ? ReceivedReconcileService.SOURCE_INSTRUCTOR_SUBMISSIONS : ReceivedReconcileService.SOURCE_INSTRUCTOR_FEEDBACKS;
            Path base = instructorView == InstructorView.SUBMISSIONS ? submissionsDirectory : feedbackDirectory;
            syncStateTable.setItems(FXCollections.observableArrayList(loadInstructorRows(source, base)));
            refreshStatusSummary();
            return;
        }
        if (session.role() == UserRole.STUDENT && studentView != StudentView.SUBMITTED) {
            String source = studentView == StudentView.ASSIGNMENTS ? "ASSIGNMENTS" : "FEEDBACK";
            syncStateTable.setItems(FXCollections.observableArrayList(loadReceivedRows(source)));
        } else {
            syncStateTable.setItems(FXCollections.observableArrayList(mapSnapshotRows(snapshot.rows())));
        }
        refreshStatusSummary();
    }

    private void refreshStatusSummary() {
        if (session.role() == UserRole.INSTRUCTOR && instructorView != InstructorView.PUBLISHED) {
            if (instructorView == InstructorView.SUBMISSIONS) {
                int receivedCount = syncStateTable.getItems() == null ? 0 : syncStateTable.getItems().size();
                pendingLabel.setText("Received: " + receivedCount);

                uploadingLabel.setVisible(false);
                uploadingLabel.setManaged(false);
                syncedLabel.setVisible(false);
                syncedLabel.setManaged(false);
                failedLabel.setVisible(false);
                failedLabel.setManaged(false);
                return;
            }

            InstructorCounts counts = countStatuses(syncStateTable.getItems());
            pendingLabel.setText("Pending: " + counts.pending);
            uploadingLabel.setText("Uploading: " + counts.uploading);
            syncedLabel.setText("Synced: " + counts.synced);
            failedLabel.setText("Failed: " + counts.failed);

            uploadingLabel.setVisible(true);
            uploadingLabel.setManaged(true);
            syncedLabel.setVisible(true);
            syncedLabel.setManaged(true);
            failedLabel.setVisible(true);
            failedLabel.setManaged(true);
            return;
        }
        if (session.role() == UserRole.STUDENT && studentView != StudentView.SUBMITTED) {
            int receivedCount = syncStateTable.getItems() == null ? 0 : syncStateTable.getItems().size();
            pendingLabel.setText("Received: " + receivedCount);

            uploadingLabel.setVisible(false);
            uploadingLabel.setManaged(false);
            syncedLabel.setVisible(false);
            syncedLabel.setManaged(false);
            failedLabel.setVisible(false);
            failedLabel.setManaged(false);
            return;
        }

        if (latestSnapshot != null) {
            pendingLabel.setText("Pending: " + latestSnapshot.pendingCount());
            uploadingLabel.setText("Uploading: " + latestSnapshot.uploadingCount());
            syncedLabel.setText("Synced: " + latestSnapshot.syncedCount());
            failedLabel.setText("Failed: " + latestSnapshot.failedCount());
        }
        uploadingLabel.setVisible(true);
        uploadingLabel.setManaged(true);
        syncedLabel.setVisible(true);
        syncedLabel.setManaged(true);
        failedLabel.setVisible(true);
        failedLabel.setManaged(true);
    }

    private InstructorCounts countStatuses(List<DashboardRow> rows) {
        int pending = 0;
        int synced = 0;
        int failed = 0;
        if (rows != null) {
            for (DashboardRow row : rows) {
                if (row == null || row.status() == null) {
                    continue;
                }
                switch (row.status()) {
                    case "PENDING" -> pending++;
                    case "SYNCED" -> synced++;
                    case "FAILED" -> failed++;
                    default -> {
                    }
                }
            }
        }
        return new InstructorCounts(pending, 0, synced, failed);
    }

    private record InstructorCounts(int pending, int uploading, int synced, int failed) {
    }

    private List<DashboardRow> mapSnapshotRows(List<SyncStateRecord> rows) {
        return rows.stream().map(DashboardScene::fromSyncStateRecord).toList();
    }

    private static DashboardRow fromSyncStateRecord(SyncStateRecord record) {
        String fileName = record.localPath().getFileName() == null ? record.localPath().toString()
            : record.localPath().getFileName().toString();
        return new DashboardRow(fileName, record.status().name(), record.remoteId(), record.sha256(), record.lastSynced());
    }

    private List<DashboardRow> loadReceivedRows(String source) {
        return receivedStateRepository.findAll().stream()
            .filter(row -> source.equals(row.source()))
            .filter(row -> !isIgnoredReceivedFile(row.localPath()))
            .map(DashboardScene::fromReceivedStateRecord)
            .toList();
    }

    private boolean isIgnoredReceivedFile(Path path) {
        Path name = path.getFileName();
        return name != null && "desktop.ini".equalsIgnoreCase(name.toString());
    }

    private static DashboardRow fromReceivedStateRecord(ReceivedStateRecord record) {
        String fileName = record.localPath().getFileName() == null ? record.localPath().toString()
            : record.localPath().getFileName().toString();
        return new DashboardRow(fileName, record.status().name(), record.remoteId(), record.sha256(), record.lastSynced());
    }

    private List<DashboardRow> loadInstructorRows(String source, Path baseDirectory) {
        return receivedStateRepository.findAll().stream()
            .filter(row -> source.equals(row.source()))
            .filter(row -> !isIgnoredReceivedFile(row.localPath()))
            .map(row -> fromReceivedStateRecordWithRelativePath(row, baseDirectory))
            .toList();
    }

    private static DashboardRow fromReceivedStateRecordWithRelativePath(ReceivedStateRecord record, Path baseDirectory) {
        String fileName;
        try {
            fileName = baseDirectory == null ? record.localPath().toString()
                : baseDirectory.relativize(record.localPath()).toString().replace('\\', '/');
        } catch (RuntimeException e) {
            fileName = record.localPath().toString();
        }
        return new DashboardRow(fileName, record.status().name(), record.remoteId(), record.sha256(), record.lastSynced());
    }

    private String shortenHash(String sha256) {
        if (sha256 == null || sha256.isBlank()) {
            return "";
        }
        return sha256.length() <= 12 ? sha256 : sha256.substring(0, 12) + "...";
    }

    private String formatLastSynced(long lastSynced) {
        if (lastSynced <= 0L) {
            return "";
        }
        return TIME_FORMATTER.format(Instant.ofEpochMilli(lastSynced));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private enum StudentView {
        SUBMITTED,
        ASSIGNMENTS,
        FEEDBACK
    }

    private enum InstructorView {
        PUBLISHED,
        SUBMISSIONS,
        FEEDBACKS
    }

    private record DashboardRow(
        String fileName,
        String status,
        String remoteId,
        String sha256,
        long lastSynced
    ) {
    }
}
