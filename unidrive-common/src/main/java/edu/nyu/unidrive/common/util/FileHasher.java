package edu.nyu.unidrive.common.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class FileHasher {

    private FileHasher() {
    }

    public static String sha256Hex(byte[] content) {
        MessageDigest digest = newSha256Digest();
        byte[] hashBytes = digest.digest(content);
        return toHex(hashBytes);
    }

    public static String sha256Hex(Path path) throws IOException {
        return sha256Hex(Files.readAllBytes(path));
    }

    private static MessageDigest newSha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
