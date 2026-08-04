package com.example;

import com.example.openapi.quarkus.client.api.FileSinkApi;
import com.example.openapi.quarkus.client.api.FileUploadApi;
import com.example.openapi.quarkus.client.model.SinkReceipt;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Exercises the generated multipart upload endpoints against a running server.
 *
 * <p>These tests pin the generator fix for arrays of {@code format: binary} properties. Previously
 * {@code type: array} was dropped, so the multi-file endpoint was generated with the same scalar
 * signature as the single-file one and only one file could ever be bound.
 *
 * <p>Each upload is additionally relayed by {@link FileUploadAdapter} to {@code /api/v1/files/sink}
 * through the generated {@code FileSinkApi} client, so every test covers two hops: the inbound
 * request (client encodes → server binds) and the forward (server relays → sink binds). Asserting
 * on the sink's receipt is what makes the forward meaningful — a status-code-only assertion would
 * pass even if the relay sent nothing.
 */
@QuarkusTest
class FileUploadApiTest extends QuarkusRestClientTestBase {

    FileUploadApi uploadApi;

    /** The sink, called directly so its receipt can be asserted on over HTTP. */
    FileSinkApi sinkApi;

    @BeforeEach
    void setupClient() {
        uploadApi = client(FileUploadApi.class);
        sinkApi = client(FileSinkApi.class);
    }

    private static File tempFile(String name, String content) throws IOException {
        Path path = Files.createTempFile(name, ".txt");
        Files.writeString(path, content);
        path.toFile().deleteOnExit();
        return path.toFile();
    }

    // ---------------------------------------------------------------
    // Every request below goes through the generated FileUploadApi client, so each test
    // exercises both halves of the fix at once: the client serialises the parts from the
    // generated signature, and the server binds them from its own generated signature.
    // ---------------------------------------------------------------

    @Test
    void singleUpload_shouldBindAScalarFile() throws IOException {
        File file = tempFile("single", "hello single");

        assertDoesNotThrow(() -> uploadApi.uploadSingleFile(file, "one file"));
    }

    /**
     * The key regression test: three files all share the {@code files} part name. Before the fix
     * both the client and the server parameter were scalars, so this call would not compile — and
     * at most one file could ever have been sent or bound.
     */
    @Test
    void multipleUpload_shouldSendAndBindSeveralFilesUnderOnePartName() throws IOException {
        List<File> files = List.of(
                tempFile("a", "alpha"),
                tempFile("b", "bravo"),
                tempFile("c", "charlie"));

        assertDoesNotThrow(() -> uploadApi.uploadMultipleFiles(files, "three files"));
    }

    @Test
    void multipleUpload_shouldAcceptASingleElementArray() throws IOException {
        List<File> files = List.of(tempFile("only", "just one"));

        assertDoesNotThrow(() -> uploadApi.uploadMultipleFiles(files, "one file in an array"));
    }

    /**
     * A file next to a non-file array: the binary binding must apply only to the file, leaving
     * {@code tags} as a plain {@code List<String>} on both the client and the server.
     */
    @Test
    void mixedUpload_shouldBindTheFileAndTheNonFileArraySeparately() throws IOException {
        File file = tempFile("mixed", "mixed content");

        assertDoesNotThrow(() -> uploadApi.uploadMixed(
                file, List.of("alpha", "beta"), "a file plus two tags"));
    }

    // ---------------------------------------------------------------
    // The sink itself, called directly. The /uploads/* endpoints relay to this same path through
    // the generated client, so these assertions pin the contract that the relay depends on. The
    // receipt comes back in the HTTP response, which means they hold equally against a server
    // started by @QuarkusTest and one already running elsewhere.
    // ---------------------------------------------------------------

    /**
     * The sink echoes back what it received, so this proves the files arrived intact — same count,
     * same bytes, same byte total — rather than merely that a status code came back.
     */
    @Test
    void sink_shouldReceiveEveryFileIntact() throws Exception {
        List<File> files = List.of(
                tempFile("first", "first payload"),
                tempFile("second", "second payload"));

        SinkReceipt receipt = sinkApi.sinkFiles(files, "relayed downstream");

        assertEquals(2, receipt.getFileNames().size(), "sink received the wrong number of files");
        assertEquals(List.of("first payload", "second payload"), receipt.getContents());
        assertEquals("relayed downstream", receipt.getDescription(),
                "the description should travel with the files");
        assertEquals(
                "first payload".length() + "second payload".length(),
                receipt.getTotalBytes(),
                "byte total should match what was sent");
    }

    /**
     * The scalar upload endpoint relays through this same {@code List<File>} parameter, so a single
     * file must be accepted as a one-element array.
     */
    @Test
    void sink_shouldAcceptASingleElementArray() throws Exception {
        File file = tempFile("solo", "solo payload");

        SinkReceipt receipt = sinkApi.sinkFiles(List.of(file), "one relayed file");

        assertEquals(1, receipt.getFileNames().size());
        assertEquals(List.of("solo payload"), receipt.getContents());
        assertEquals("solo payload".length(), receipt.getTotalBytes());
    }
}
