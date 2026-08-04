package com.example;

import com.example.openapi.quarkus.client.api.ApiException;
import com.example.openapi.quarkus.client.api.FileSinkApi;
import com.example.openapi.quarkus.client.model.SinkReceipt;
import com.example.openapi.quarkus.server.api.FileUploadApi;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.util.List;

/**
 * Implements the generated multipart upload endpoints and relays each upload downstream.
 *
 * <p>The inbound signatures are the point of the endpoints themselves: {@code uploadMultipleFiles}
 * receives a {@code List<FileUpload>} while {@code uploadSingleFile} receives a scalar
 * {@code FileUpload}. Before the generator fix an array of {@code format: binary} items was
 * collapsed onto the scalar file type, so both methods had the same signature and only one file was
 * ever bindable.
 *
 * <p><strong>Forwarding pattern.</strong> Every upload is relayed to {@code /api/v1/files/sink}
 * through the <em>generated</em> {@link FileSinkApi} REST client, standing in for a call to a
 * separate downstream service. The relay never reads the file: Quarkus has already streamed each
 * part to a temp file, and {@link FileUpload#uploadedFile()} hands back that {@link java.nio.file.Path},
 * so {@code .toFile()} is a plain handle conversion with no I/O and no copy. The REST client then
 * streams that file into the outbound multipart body. Heap usage stays flat regardless of file
 * size — the bytes travel disk-to-socket and are never materialised in the JVM.
 *
 * <p><strong>Why this may be synchronous.</strong> {@code deleteUploadedFilesOnEnd} defaults to
 * {@code true}, so the inbound temp file is removed as soon as this request ends. Forwarding
 * synchronously means the downstream call completes while the file is still on disk. Handing the
 * {@code File} to an async stage or a retry queue would race that deletion. {@code @RunOnVirtualThread}
 * is what makes blocking here acceptable: the carrier thread is released while the downstream call
 * is in flight, so no event-loop thread is ever blocked.
 */
@RunOnVirtualThread
@ApplicationScoped
public class FileUploadAdapter implements FileUploadApi {

    private final Logger LOGGER = Logger.getLogger(FileUploadAdapter.class);

    /**
     * The generated downstream client, injected by CDI. The generator already annotates the
     * interface with {@code @RegisterRestClient}, so nothing here implements it — {@code @RestClient}
     * asks Quarkus for the proxy it built from that interface, and the base URI comes from the
     * {@code quarkus.rest-client."…FileSinkApi".url} property in {@code application.properties}.
     */
    private final FileSinkApi fileSinkApi;

    @Inject
    public FileUploadAdapter(@RestClient FileSinkApi fileSinkApi) {
        this.fileSinkApi = fileSinkApi;
    }

    @Override
    public void uploadSingleFile(FileUpload fileUpload, String description) {
        LOGGER.infof("uploadSingleFile received 1 file: %s", fileUpload.fileName());
        forward(List.of(fileUpload), description);
    }

    @Override
    public void uploadMultipleFiles(List<FileUpload> files, String description) {
        // All parts share the "files" part name; without the fix only one of them arrived.
        LOGGER.infof("uploadMultipleFiles received %d file(s): %s",
                files.size(), files.stream().map(FileUpload::fileName).toList());
        forward(files, description);
    }

    @Override
    public void uploadMixed(FileUpload fileUpload, List<String> tags, String description) {
        // `tags` is a non-file array and must stay a plain List<String>: the binary binding
        // applies only to the file parameter.
        LOGGER.infof("uploadMixed received 1 file: %s with %d tag(s): %s",
                fileUpload.fileName(), tags == null ? 0 : tags.size(), tags);
        forward(List.of(fileUpload), description);
    }

    /**
     * Relays the uploads to the sink through the generated client.
     *
     * <p>{@code uploadedFile()} is the temp file Quarkus already streamed to disk, so mapping it to
     * a {@link java.io.File} copies nothing — this is the whole point of the pattern.
     */
    private void forward(List<FileUpload> files, String description) {
        List<java.io.File> payload = files.stream()
                .map(file -> file.uploadedFile().toFile())
                .toList();

        SinkReceipt receipt;
        try {
            receipt = fileSinkApi.sinkFiles(payload, description);
        } catch (ApiException e) {
            // `throws ApiException, ProcessingException` is hardcoded in the microprofile
            // api.mustache, so it cannot be switched off by configuration. In this project it is
            // also unreachable: microprofileRegisterExceptionMapper and
            // microprofileGlobalExceptionMapper are both false, so ApiExceptionMapper is never
            // registered and downstream failures arrive as the REST Client's own unchecked
            // ClientWebApplicationException instead. The catch exists only to satisfy the
            // compiler; a downstream failure is not the caller's fault, hence 502 either way.
            LOGGER.errorf(e, "sink rejected %d forwarded file(s)", payload.size());
            throw new WebApplicationException("Downstream sink rejected the upload", e, 502);
        }

        LOGGER.infof("forwarded %d file(s) to the sink, which acknowledged %d (%d bytes)",
                payload.size(), receipt.getFileNames().size(), receipt.getTotalBytes());
    }
}
