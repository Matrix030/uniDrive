package edu.nyu.unidrive.client.sync;

import edu.nyu.unidrive.client.net.AssignmentApiClient;
import edu.nyu.unidrive.client.net.DownloadedFile;
import edu.nyu.unidrive.client.storage.AssignmentSlot;
import edu.nyu.unidrive.client.storage.ReceivedStateRecord;
import edu.nyu.unidrive.client.storage.ReceivedStateRepository;
import edu.nyu.unidrive.client.storage.WorkspaceLayout;
import edu.nyu.unidrive.common.dto.AssignmentSummaryResponse;
import edu.nyu.unidrive.common.model.SyncStatus;
import edu.nyu.unidrive.common.util.FileHasher;
import edu.nyu.unidrive.common.workspace.CoursePath;
import edu.nyu.unidrive.common.workspace.WorkspaceRole;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AssignmentSyncService {

    private final AssignmentApiClient assignmentApiClient;
    private final ReceivedStateRepository receivedStateRepository;

    public AssignmentSyncService(AssignmentApiClient assignmentApiClient, ReceivedStateRepository receivedStateRepository) {
        this.assignmentApiClient = assignmentApiClient;
        this.receivedStateRepository = receivedStateRepository;
    }

    public int syncAssignmentsForCourse(String term, String courseSlug, Path workspaceRoot) {
        try {
            int downloadedCount = 0;
            Map<String, List<AssignmentSummaryResponse>> assignmentsById = assignmentApiClient.listAssignments(term, courseSlug)
                .stream()
                .collect(Collectors.groupingBy(AssignmentSummaryResponse::getAssignmentId, LinkedHashMap::new, Collectors.toList()));

            for (Map.Entry<String, List<AssignmentSummaryResponse>> entry : assignmentsById.entrySet()) {
                CoursePath coursePath = new CoursePath(term, courseSlug, entry.getKey());
                AssignmentSlot slot = WorkspaceLayout.ensureAssignmentSlot(workspaceRoot, coursePath, WorkspaceRole.STUDENT);
                migrateLegacyStudentPublishDirectory(workspaceRoot, coursePath, slot.publishDir());
                enforceProfessorFileMirror(slot.publishDir(), entry.getValue());

                for (AssignmentSummaryResponse assignment : entry.getValue()) {
                    Path relativeFile = relativeAssignmentFile(assignment.getFileName());
                    Path destination = slot.publishDir().resolve(relativeFile);

                    if (Files.exists(destination) && FileHasher.sha256Hex(destination).equals(assignment.getSha256())) {
                        receivedStateRepository.save(new ReceivedStateRecord(
                            destination,
                            assignment.getAssignmentId(),
                            assignment.getSha256(),
                            SyncStatus.SYNCED,
                            System.currentTimeMillis(),
                            ReceivedReconcileService.SOURCE_ASSIGNMENTS
                        ));
                        continue;
                    }

                    receivedStateRepository.save(new ReceivedStateRecord(
                        destination,
                        assignment.getAssignmentId(),
                        assignment.getSha256(),
                        SyncStatus.PENDING,
                        0L,
                        ReceivedReconcileService.SOURCE_ASSIGNMENTS
                    ));

                    try {
                        DownloadedFile download = assignmentApiClient.downloadAssignment(assignment.getAssignmentId(), assignment.getFileName());
                        Path downloadedDestination = slot.publishDir().resolve(relativeAssignmentFile(download.fileName()));
                        Files.createDirectories(downloadedDestination.getParent());
                        Files.write(downloadedDestination, download.content());
                        receivedStateRepository.save(new ReceivedStateRecord(
                            downloadedDestination,
                            assignment.getAssignmentId(),
                            assignment.getSha256(),
                            SyncStatus.SYNCED,
                            System.currentTimeMillis(),
                            ReceivedReconcileService.SOURCE_ASSIGNMENTS
                        ));
                        downloadedCount++;
                    } catch (IOException exception) {
                        receivedStateRepository.save(new ReceivedStateRecord(
                            destination,
                            assignment.getAssignmentId(),
                            assignment.getSha256(),
                            SyncStatus.FAILED,
                            0L,
                            ReceivedReconcileService.SOURCE_ASSIGNMENTS
                        ));
                    }
                }
            }
            removeStaleAssignmentFiles(term, courseSlug, workspaceRoot, assignmentsById.keySet());
            return downloadedCount;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to synchronize assignments for " + term + "/" + courseSlug, exception);
        }
    }

    private void removeStaleAssignmentFiles(String term, String courseSlug, Path workspaceRoot, Set<String> remoteAssignmentIds)
        throws IOException {
        Path courseDir = workspaceRoot.resolve(term).resolve(courseSlug);
        if (!Files.isDirectory(courseDir)) {
            return;
        }
        try (Stream<Path> assignmentDirs = Files.list(courseDir)) {
            List<Path> staleFilesDirs = assignmentDirs
                .filter(Files::isDirectory)
                .filter(assignmentDir -> !remoteAssignmentIds.contains(assignmentDir.getFileName().toString()))
                .map(assignmentDir -> assignmentDir.resolve(CoursePath.STUDENT_FILES_DIR))
                .filter(Files::isDirectory)
                .toList();
            for (Path filesDir : staleFilesDirs) {
                enforceProfessorFileMirror(filesDir, List.of());
            }
        }
    }

    private void enforceProfessorFileMirror(Path filesDir, List<AssignmentSummaryResponse> expectedAssignments) throws IOException {
        Set<Path> expectedFiles = expectedAssignments.stream()
            .map(assignment -> relativeAssignmentFile(assignment.getFileName()))
            .collect(Collectors.toSet());
        if (!Files.isDirectory(filesDir)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(filesDir)) {
            List<Path> localFiles = paths.filter(Files::isRegularFile).toList();
            for (Path localFile : localFiles) {
                Path relativeFile = filesDir.relativize(localFile);
                if (!expectedFiles.contains(relativeFile)) {
                    Files.delete(localFile);
                    receivedStateRepository.deleteByLocalPath(localFile);
                }
            }
        }
        deleteEmptyChildDirectories(filesDir);
    }

    private void migrateLegacyStudentPublishDirectory(Path workspaceRoot, CoursePath coursePath, Path filesDir) throws IOException {
        Path assignmentRoot = coursePath.resolveAgainst(workspaceRoot);
        Path legacyPublishDir = assignmentRoot.resolve(CoursePath.INSTRUCTOR_PUBLISH_DIR);
        if (!Files.isDirectory(legacyPublishDir) || legacyPublishDir.equals(filesDir)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(legacyPublishDir)) {
            List<Path> legacyFiles = paths.filter(Files::isRegularFile).toList();
            for (Path legacyFile : legacyFiles) {
                migrateLegacyFile(assignmentRoot, legacyPublishDir, filesDir, legacyFile);
            }
        }
        deleteEmptyDirectories(legacyPublishDir);
    }

    private void migrateLegacyFile(Path assignmentRoot, Path legacyPublishDir, Path filesDir, Path legacyFile) throws IOException {
        Path relativeFile = legacyPublishDir.relativize(legacyFile);
        Path destination = filesDir.resolve(relativeFile);
        if (!Files.exists(destination)) {
            Files.createDirectories(destination.getParent());
            Files.move(legacyFile, destination);
            receivedStateRepository.deleteByLocalPath(legacyFile);
            return;
        }

        if (FileHasher.sha256Hex(legacyFile).equals(FileHasher.sha256Hex(destination))) {
            Files.delete(legacyFile);
            receivedStateRepository.deleteByLocalPath(legacyFile);
            return;
        }

        Path conflictPath = uniqueConflictPath(assignmentRoot.resolve(".legacy-publish").resolve(relativeFile));
        Files.createDirectories(conflictPath.getParent());
        Files.move(legacyFile, conflictPath);
        receivedStateRepository.deleteByLocalPath(legacyFile);
    }

    private Path uniqueConflictPath(Path preferredPath) {
        if (!Files.exists(preferredPath)) {
            return preferredPath;
        }

        String fileName = preferredPath.getFileName().toString();
        Path parent = preferredPath.getParent();
        int count = 1;
        Path candidate;
        do {
            candidate = parent.resolve(fileName + ".conflict-" + count);
            count++;
        } while (Files.exists(candidate));
        return candidate;
    }

    private void deleteEmptyDirectories(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            List<Path> directories = paths.filter(Files::isDirectory)
                .sorted(Comparator.reverseOrder())
                .toList();
            for (Path directory : directories) {
                try {
                    Files.delete(directory);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void deleteEmptyChildDirectories(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            List<Path> directories = paths.filter(Files::isDirectory)
                .filter(directory -> !directory.equals(root))
                .sorted(Comparator.reverseOrder())
                .toList();
            for (Path directory : directories) {
                try {
                    Files.delete(directory);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private Path relativeAssignmentFile(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("Assignment file name must stay inside the assignment files folder: " + fileName);
        }
        Path path = Path.of(fileName).normalize();
        if (path.isAbsolute() || path.startsWith("..")) {
            throw new IllegalArgumentException("Assignment file name must stay inside the assignment files folder: " + fileName);
        }
        return path;
    }

}
