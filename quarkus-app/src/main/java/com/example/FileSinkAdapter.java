package com.example;

import com.example.openapi.quarkus.server.api.FileSinkApi;
import com.example.openapi.quarkus.server.model.SinkReceipt;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Stands in for a downstream service.
 *
 * <p>{@link FileUploadAdapter} relays every upload here through the generated REST client, so this
 * class is on the receiving end of a real HTTP multipart request rather than an in-process call.
 * It is a separate {@code @Path} ({@code /api/v1/files/sink}) purely so that hop is genuine.
 *
 * <p>It echoes back what it received. Asserting on the returned {@link SinkReceipt} proves the
 * bytes survived the forward — a bare 202 would still pass if the relay silently sent nothing.
 */
@RunOnVirtualThread
@ApplicationScoped
public class FileSinkAdapter implements FileSinkApi {

    private final Logger LOGGER = Logger.getLogger(FileSinkAdapter.class);

    @Override
    public SinkReceipt sinkFiles(List<FileUpload> files, String description) {
        List<String> names = new ArrayList<>();
        List<String> contents = new ArrayList<>();
        long totalBytes = 0;

        for (FileUpload file : files) {
            names.add(file.fileName());
            contents.add(readContent(file));
            totalBytes += file.size();
        }

        LOGGER.infof("sinkFiles received %d file(s): %s (%d bytes)", files.size(), names, totalBytes);

        return new SinkReceipt(names, contents, totalBytes).description(description);
    }

    /**
     * Reads the part off disk so the receipt can carry it back.
     *
     * <p>This is the one place that deliberately pulls an upload into the heap, and it is safe only
     * because these are tiny test fixtures. Production code should forward
     * {@code fileUpload.uploadedFile()} onward rather than materialising the bytes.
     */
    private String readContent(FileUpload file) {
        try {
            return Files.readString(file.uploadedFile());
        } catch (IOException e) {
            throw new IllegalStateException("Could not read received file " + file.fileName(), e);
        }
    }
}
