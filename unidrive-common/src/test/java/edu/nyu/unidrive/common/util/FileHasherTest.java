package edu.nyu.unidrive.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileHasherTest {

    @Test
    void sha256HexReturnsExpectedDigestForKnownInput() {
        String digest = FileHasher.sha256Hex("abc".getBytes());

        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", digest);
    }

    @Test
    void sha256HexReturnsSameDigestForBytesAndFile(@TempDir Path tempDir) throws IOException {
        byte[] content = "submission content".getBytes();
        Path file = tempDir.resolve("sample.txt");
        Files.write(file, content);

        String bytesDigest = FileHasher.sha256Hex(content);
        String fileDigest = FileHasher.sha256Hex(file);

        assertEquals(bytesDigest, fileDigest);
    }
}
