package io.kestra.plugin.stackit.iaas.server;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

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
        serviceAccountKey: "{{ secret('STACKIT_SERVICE_ACCOUNT_KEY') }}"
        projectId: "{{ secret('STACKIT_PROJECT_ID') }}"
        region: eu01
        serverId: 11111111-1111-1111-1111-111111111111
    """))
public class Get extends AbstractStackitServerById implements RunnableTask<Get.Output> {

    @Override
    public Output run(RunContext runContext) throws Exception {
        var rServerId = uuid("serverId", runContext.render(getServerId()).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("serverId is required")));
        var rProjectId = uuid("projectId", runContext.render(getProjectId()).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("projectId is required")));
        var rRegion = runContext.render(getRegion()).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("region is required"));
        try (IaasClient client = iaasClient(runContext)) {
            var server = call(() -> client.api().getServer(rProjectId, rRegion, rServerId, false));
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