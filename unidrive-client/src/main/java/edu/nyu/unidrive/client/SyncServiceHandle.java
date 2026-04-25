package edu.nyu.unidrive.client;

public interface SyncServiceHandle extends AutoCloseable {

    void start();

    @Override
    void close();
}
