package io.kestra.plugin.stackit.iaas.server;

import cloud.stackit.sdk.core.exception.ApiException;
import cloud.stackit.sdk.iaas.v2api.api.IaasApi;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.stackit.auth.AbstractStackit;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
public abstract class AbstractStackitServerById extends AbstractStackit {

    @Schema(title = "Server ID")
    @PluginProperty(group = "main")
    @NotNull
    private Property<@NotBlank String> serverId;

    @FunctionalInterface
    protected interface ServerOperation {
        void execute(IaasApi api, UUID project, String region, UUID serverId) throws ApiException;
    }

    protected UUID serverUuid(RunContext runContext) throws IllegalVariableEvaluationException {
        return UUID.fromString(runContext.render(serverId).as(String.class).orElseThrow());
    }

    protected String runServerOperation(RunContext runContext, ServerOperation operation) throws Exception {
        UUID id = serverUuid(runContext);
        var project = projectId(runContext);
        String stackitRegion = region(runContext);
        try (IaasClient client = iaasClient(runContext)) {
            call(() -> {
                operation.execute(client.api(), project, stackitRegion, id);
                return null;
            });
        }
        return id.toString();
    }
}