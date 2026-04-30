package edu.nyu.unidrive.client.storage;

import edu.nyu.unidrive.common.workspace.CoursePath;
import java.nio.file.Path;

public record AssignmentSlot(CoursePath coursePath, Path publishDir, Path submissionsDir) {
}
