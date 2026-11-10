package io.kestra.plugin.stackit.iaas.server;

import cloud.stackit.sdk.iaas.v2api.model.Server;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.stackit.auth.AbstractStackit;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
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
@Schema(title = "Get a STACKIT server", description = "Fetches one server by id.")
@Plugin(examples = @Example(title = "Get a server", full = true, code = """
    id: stackit_get_server
    namespace: company.team

    tasks:
      - id: get_server
        type: io.kestra.plugin.stackit.iaas.server.Get
        projectId: "{{ secret('STACKIT_PROJECT_ID') }}"
        region: eu01
        serverId: 11111111-1111-1111-1111-111111111111
    """))
public class Get extends AbstractStackit implements RunnableTask<Get.Output> {
    @Schema(title = "Server ID")
    @PluginProperty(group = "server")
    private Property<String> serverId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        UUID id = requiredUuid(runContext, serverId, "serverId");
        UUID project = projectId(runContext);
        String stackitRegion = region(runContext);
        try (IaasClient client = iaasClient()) {
            Server server = call(() -> client.api().getServer(project, stackitRegion, id, false));
            return Output.builder()
                .serverId(server.getId() == null ? null : server.getId().toString())
                .name(server.getName())
                .status(server.getStatus())
                .powerStatus(server.getPowerStatus())
                .machineType(server.getMachineType())
                .availabilityZone(server.getAvailabilityZone())
                .build();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Server ID")
        private final String serverId;
        @Schema(title = "Name")
        private final String name;
        @Schema(title = "Status")
        private final String status;
        @Schema(title = "Power status")
        private final String powerStatus;
        @Schema(title = "Machine type")
        private final String machineType;
        @Schema(title = "Availability zone")
        private final String availabilityZone;
    }
}
