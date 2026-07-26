package com.example;

import com.example.openapi.quarkus.server.api.FileUploadApi;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements the generated multipart upload endpoints.
 *
 * <p>The signatures below are the point of this class: {@code uploadMultipleFiles} receives a
 * {@code List<FileUpload>} while {@code uploadSingleFile} receives a scalar {@code FileUpload}.
 * Before the generator fix an array of {@code format: binary} items was collapsed onto the scalar
 * file type, so both methods had the same signature and only one file was ever bindable.
 *
 * <p>Each upload is read back so the tests can assert on the received content, not merely on the
 * status code.
 */
public class FileUploadAdapter implements FileUploadApi {

    private final Logger LOGGER = Logger.getLogger(FileUploadAdapter.class);

    /** Records what the server actually received, so tests can assert against it. */
    static final List<String> RECEIVED_FILE_NAMES = new ArrayList<>();
    static final List<String> RECEIVED_CONTENTS = new ArrayList<>();
    static volatile String lastDescription;

    static void reset() {
        RECEIVED_FILE_NAMES.clear();
        RECEIVED_CONTENTS.clear();
        lastDescription = null;
    }

    @Override
    public void uploadSingleFile(FileUpload file, String description) {
        lastDescription = description;
        record(file);
        LOGGER.infof("uploadSingleFile received 1 file: %s", file.fileName());
    }

    @Override
    public void uploadMultipleFiles(List<FileUpload> files, String description) {
        lastDescription = description;
        // All parts share the "files" part name; without the fix only one of them arrived.
        for (FileUpload file : files) {
            record(file);
        }
        LOGGER.infof("uploadMultipleFiles received %d file(s): %s",
                files.size(), files.stream().map(FileUpload::fileName).toList());
    }

    @Override
    public void uploadMixed(FileUpload file, List<String> tags, String description) {
        lastDescription = description;
        record(file);
        // `tags` is a non-file array and must stay a plain List<String>: the binary binding
        // applies only to the file parameter.
        LOGGER.infof("uploadMixed received 1 file: %s with %d tag(s): %s",
                file.fileName(), tags == null ? 0 : tags.size(), tags);
    }

    private void record(FileUpload file) {
        RECEIVED_FILE_NAMES.add(file.fileName());
        try {
            RECEIVED_CONTENTS.add(Files.readString(file.uploadedFile()));
        } catch (IOException e) {
            throw new IllegalStateException("Could not read uploaded file " + file.fileName(), e);
        }
    }
}
