package io.kestra.plugin.stackit.auth;

import cloud.stackit.sdk.core.config.CoreConfiguration;
import cloud.stackit.sdk.core.exception.ApiException;
import cloud.stackit.sdk.iaas.v2api.api.IaasApi;
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
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.util.UUID;

/**
 * Shared STACKIT connection.
 *
 * <p>The service account key JSON is a task property. Kestra renders {@code serviceAccountKey} and this class
 * passes that string to {@link CoreConfiguration#serviceAccountKey(String)}, which keeps the key in memory.
 */
@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractStackit extends Task {
    public static final int DEFAULT_TIMEOUT_MILLIS = 30_000;

    @Schema(
        title = "Service account key",
        description = "Service account key JSON. Pass it with a Kestra secret, for example {{ secret('STACKIT_SERVICE_ACCOUNT_KEY') }}."
    )
    @PluginProperty(group = "connection")
    @ToString.Exclude
    private Property<String> serviceAccountKey;

    @Schema(title = "Project ID", description = "STACKIT project UUID.")
    @PluginProperty(group = "connection")
    private Property<String> projectId;

    @Schema(title = "Region", description = "STACKIT region, for example eu01.")
    @PluginProperty(group = "connection")
    private Property<String> region;

    private transient StackitEndpointOverride endpointOverride;

    public void applyEndpointOverrideForTests(StackitEndpointOverride override) {
        this.endpointOverride = override;
    }

    protected IaasClient iaasClient(RunContext runContext) throws IOException, IllegalVariableEvaluationException {
        CoreConfiguration configuration = new CoreConfiguration()
            .serviceAccountKey(required(runContext, this.serviceAccountKey, "serviceAccountKey"));
        if (endpointOverride != null) {
            configuration.customEndpoint(endpointOverride.customEndpoint());
            configuration.tokenCustomUrl(endpointOverride.tokenCustomUrl());
        }
        IaasApi api = new IaasApi(configuration);
        api.getApiClient().setConnectTimeout(DEFAULT_TIMEOUT_MILLIS);
        api.getApiClient().setReadTimeout(DEFAULT_TIMEOUT_MILLIS);
        api.getApiClient().setWriteTimeout(DEFAULT_TIMEOUT_MILLIS);
        return new IaasClient(api);
    }

    protected UUID projectId(RunContext runContext) throws IllegalVariableEvaluationException {
        return UUID.fromString(required(runContext, this.projectId, "projectId"));
    }

    protected String region(RunContext runContext) throws IllegalVariableEvaluationException {
        return required(runContext, this.region, "region");
    }

    protected UUID requiredUuid(RunContext runContext, Property<String> property, String name) throws IllegalVariableEvaluationException {
        return UUID.fromString(required(runContext, property, name));
    }

    protected String required(RunContext runContext, Property<String> property, String name) throws IllegalVariableEvaluationException {
        String rendered = optional(runContext, property);
        if (rendered == null) {
            throw new IllegalArgumentException(name + " is required.");
        }
        return rendered;
    }

    protected String optional(RunContext runContext, Property<String> property) throws IllegalVariableEvaluationException {
        if (property == null) {
            return null;
        }
        return runContext.render(property).as(String.class).filter(value -> !value.isBlank()).orElse(null);
    }

    protected Integer optionalInteger(RunContext runContext, Property<Integer> property) throws IllegalVariableEvaluationException {
        if (property == null) {
            return null;
        }
        return runContext.render(property).as(Integer.class).orElse(null);
    }

    protected Boolean optionalBoolean(RunContext runContext, Property<Boolean> property) throws IllegalVariableEvaluationException {
        if (property == null) {
            return null;
        }
        return runContext.render(property).as(Boolean.class).orElse(null);
    }

    protected <T> T call(StackitCall<T> action) throws Exception {
        try {
            return action.run();
        } catch (ApiException e) {
            throw translate(e);
        }
    }

    public static Exception translate(ApiException exception) {
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
