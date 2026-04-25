package edu.nyu.unidrive.common.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class SyncStatusTest {

    @Test
    void syncStatusesMatchProjectStateMachineOrder() {
        SyncStatus[] expected = {
            SyncStatus.PENDING,
            SyncStatus.UPLOADING,
            SyncStatus.SYNCED,
            SyncStatus.FAILED
        };

        assertArrayEquals(expected, SyncStatus.values());
    }
}
