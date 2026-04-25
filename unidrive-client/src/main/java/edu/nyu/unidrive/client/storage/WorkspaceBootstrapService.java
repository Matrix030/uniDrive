package edu.nyu.unidrive.client.storage;

import java.nio.file.Path;

public interface WorkspaceBootstrapService {
    ClientWorkspace bootstrap(Path rootDirectory);
}
