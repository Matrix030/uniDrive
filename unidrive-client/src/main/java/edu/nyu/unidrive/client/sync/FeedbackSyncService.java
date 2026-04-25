package edu.nyu.unidrive.client.sync;

import edu.nyu.unidrive.client.net.DownloadedFile;
import edu.nyu.unidrive.client.net.FeedbackApiClient;
import edu.nyu.unidrive.common.dto.FeedbackSummaryResponse;
import edu.nyu.unidrive.common.util.FileHasher;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FeedbackSyncService {

    private final FeedbackApiClient feedbackApiClient;

    public FeedbackSyncService(FeedbackApiClient feedbackApiClient) {
        this.feedbackApiClient = feedbackApiClient;
    }

    public int syncFeedback(String studentId, Path feedbackDirectory) {
        try {
            Files.createDirectories(feedbackDirectory);
            int downloadedCount = 0;
            for (FeedbackSummaryResponse feedback : feedbackApiClient.listFeedback(studentId)) {
                Path destination = feedbackDirectory.resolve(feedback.getFileName());
                if (Files.exists(destination) && FileHasher.sha256Hex(destination).equals(feedback.getSha256())) {
                    continue;
                }

                DownloadedFile download = feedbackApiClient.downloadFeedback(feedback.getFeedbackId());
                Files.write(feedbackDirectory.resolve(download.fileName()), download.content());
                downloadedCount++;
            }
            return downloadedCount;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to synchronize feedback.", exception);
        }
    }
}
