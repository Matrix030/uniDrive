package edu.nyu.unidrive.client;

import edu.nyu.unidrive.client.storage.ClientWorkspace;

public interface SyncServiceFactory {

    SyncServiceHandle create(ClientWorkspace workspace, String assignmentId, String studentId, String baseUrl);
}
