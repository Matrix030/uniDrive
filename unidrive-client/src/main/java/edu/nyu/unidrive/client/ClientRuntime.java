package edu.nyu.unidrive.client;

import edu.nyu.unidrive.client.storage.ClientWorkspace;

public record ClientRuntime(ClientWorkspace workspace, SyncServiceHandle syncService) implements AutoCloseable {

    @Override
    public void close() {
        syncService.close();
    }
}
