package io.kestra.plugin.stackit;

import cloud.stackit.sdk.core.exception.ApiException;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import cloud.stackit.sdk.iaas.v2api.api.IaasApi;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractStackit extends Task {
    public static final int DEFAULT_TIMEOUT_MILLIS = 30_000;
    private static final String KESTRA_URI_PREFIX = "kestra://";

    @Schema(
        title = "Service account key",
        description = "Service account key JSON. Pass the JSON text, a {{ secret('...') }} expression, or a kestra:// URI whose file contains the JSON."
    )
    @PluginProperty(group = "connection", secret = true)
    @ToString.Exclude
    private Property<String> serviceAccountKey;

    @Schema(
        title = "Project ID",
        description = "STACKIT project UUID."
    )
    @PluginProperty(group = "connection")
    private Property<String> projectId;

    @Schema(
        title = "Region",
        description = "STACKIT region, for example eu01."
    )
    @PluginProperty(group = "connection")
    private Property<String> region;

    private transient StackitEndpointOverride endpointOverride;

    /**
     * Test-only hook so WireMock can stand in for STACKIT without subclassing {@code @Plugin} tasks.
     */
    public void applyEndpointOverrideForTests(StackitEndpointOverride override) {
        this.endpointOverride = override;
    }

    protected StackitEndpointOverride stackitEndpointOverride() {
        return endpointOverride;
    }

    protected IaasClient iaasClient(RunContext runContext) throws Exception {
        return StackitTestClients.buildIaasClient(readServiceAccountKey(runContext), stackitEndpointOverride());
    }

    protected String region(RunContext runContext) throws IllegalVariableEvaluationException {
        return required(runContext, this.region, "region");
    }

    protected UUID projectId(RunContext runContext) throws IllegalVariableEvaluationException {
        String rendered = required(runContext, this.projectId, "projectId");
        try {
            return UUID.fromString(rendered);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("projectId must be a UUID, got '" + rendered + "'.", e);
        }
    }

    protected <T> T call(StackitCall<T> action) throws Exception {
        try {
            return action.run();
        } catch (ApiException e) {
            throw translate(e);
        }
    }

    static Exception translate(ApiException exception) {
        if (exception.getCode() == 403) {
            return new StackitClientException(
                "STACKIT returned 403. Check the service account permissions.",
                exception
            );
        }
        return new StackitClientException(
            "STACKIT request failed with status " + exception.getCode() + ": " + exception.getMessage(),
            exception
        );
    }

    private String readServiceAccountKey(RunContext runContext) throws Exception {
        String rendered = required(runContext, this.serviceAccountKey, "serviceAccountKey");
        if (!rendered.startsWith(KESTRA_URI_PREFIX)) {
            return rendered;
        }
        try (InputStream input = runContext.storage().getFile(URI.create(rendered))) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String required(RunContext runContext, Property<String> property, String name) throws IllegalVariableEvaluationException {
        String rendered = property == null
            ? null
            : runContext.render(property).as(String.class).orElse(null);
        if (rendered == null || rendered.isBlank()) {
            throw new IllegalArgumentException(name + " is required.");
        }
        return rendered;
    }

    @FunctionalInterface
    protected interface StackitCall<T> {
        T run() throws ApiException, IOException;
    }

    public static final class IaasClient implements AutoCloseable {
        private final IaasApi api;

        IaasClient(IaasApi api) {
            this.api = api;
        }

        public IaasApi api() {
            return api;
        }

        @Override
        public void close() {
            OkHttpClient httpClient = api.getApiClient().getHttpClient();
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
            if (httpClient.cache() != null) {
                try {
                    httpClient.cache().close();
                } catch (IOException ignored) {
                    // The HTTP cache is optional and the API call has already finished.
                }
            }
        }
    }

    public static class StackitClientException extends Exception {
        public StackitClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
