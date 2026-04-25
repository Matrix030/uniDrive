package edu.nyu.unidrive.client;

public final class CompositeSyncServiceHandle implements SyncServiceHandle {

    private final SyncServiceHandle[] handles;

    public CompositeSyncServiceHandle(SyncServiceHandle... handles) {
        this.handles = handles;
    }

    @Override
    public void start() {
        for (SyncServiceHandle handle : handles) {
            handle.start();
        }
    }

    @Override
    public void close() {
        IllegalStateException failure = null;
        for (int index = handles.length - 1; index >= 0; index--) {
            try {
                handles[index].close();
            } catch (Exception exception) {
                if (failure == null) {
                    failure = new IllegalStateException("Failed to close sync handle.", exception);
                }
            }
        }

        if (failure != null) {
            throw failure;
        }
    }
}
