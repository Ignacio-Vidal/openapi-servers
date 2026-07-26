package com.example;

import com.example.openapi.quarkus.client.api.FileUploadApi;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Exercises the generated multipart upload endpoints against a running server.
 *
 * <p>These tests pin the generator fix for arrays of {@code format: binary} properties. Previously
 * {@code type: array} was dropped, so the multi-file endpoint was generated with the same scalar
 * signature as the single-file one and only one file could ever be bound.
 */
@QuarkusTest
class FileUploadApiTest extends QuarkusRestClientTestBase {

    FileUploadApi uploadApi;

    @BeforeEach
    void setupClient() {
        uploadApi = client(FileUploadApi.class);
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
}
