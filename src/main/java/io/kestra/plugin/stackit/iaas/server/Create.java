package io.kestra.plugin.stackit.iaas.server;

import cloud.stackit.sdk.iaas.v2api.model.BootVolume;
import cloud.stackit.sdk.iaas.v2api.model.BootVolumeSource;
import cloud.stackit.sdk.iaas.v2api.model.CreateServerNetworking;
import cloud.stackit.sdk.iaas.v2api.model.CreateServerPayload;
import cloud.stackit.sdk.iaas.v2api.model.CreateServerPayloadAllOfNetworking;
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
@Schema(
    title = "Create a STACKIT server",
    description = "Creates a server in a STACKIT project and region."
)
@Plugin(
    examples = {
        @Example(
            title = "Create a server and log its ID",
            full = true,
            code = """
                id: stackit_create_server
                namespace: company.team

                tasks:
                  - id: create_server
                    type: io.kestra.plugin.stackit.iaas.server.Create
                    projectId: "{{ secret('STACKIT_PROJECT_ID') }}"
                    region: eu01
                    name: kestra-demo
                    machineType: t2i.1
                    imageId: 11111111-1111-1111-1111-111111111111
                    bootVolumeSizeGiB: 32

                  - id: log_server
                    type: io.kestra.plugin.core.log.Log
                    message: "Server {{ outputs.create_server.serverId }} created"
                """
        )
    }
)
public class Create extends AbstractStackit implements RunnableTask<Create.Output> {
    @Schema(
        title = "Server name",
        description = "Name of the server to create."
    )
    @PluginProperty(group = "server")
    private Property<String> name;

    @Schema(
        title = "Machine type",
        description = "Required machine type name that exists in the region. The SDK sample name is t2i.1."
    )
    @PluginProperty(group = "server")
    private Property<String> machineType;

    @Schema(
        title = "Availability zone",
        description = "Availability zone, for example eu01-1. Leave empty to let STACKIT choose."
    )
    @PluginProperty(group = "server")
    private Property<String> availabilityZone;

    @Schema(
        title = "Image ID",
        description = "Operating system image UUID used as the boot volume source. List images in the project to get a valid id."
    )
    @PluginProperty(group = "server")
    private Property<String> imageId;

    @Schema(
        title = "Boot volume size (GiB)",
        description = "Boot disk size in GiB. Defaults to 32, the size used by the STACKIT Java SDK example."
    )
    @PluginProperty(group = "server")
    private Property<Integer> bootVolumeSizeGiB;

    @Schema(
        title = "Keypair name",
        description = "Optional SSH keypair already created in the project."
    )
    @PluginProperty(group = "server")
    private Property<String> keypairName;

    @Schema(
        title = "Network ID",
        description = "Optional network UUID to attach. Leave empty to use the project default network."
    )
    @PluginProperty(group = "server")
    private Property<String> networkId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        String serverName = required(runContext, name, "name");
        String renderedMachineType = required(runContext, machineType, "machineType");
        String renderedZone = optional(runContext, availabilityZone);
        String renderedImageId = required(runContext, imageId, "imageId");
        Integer renderedSize = optionalInteger(runContext, bootVolumeSizeGiB);
        String renderedKeypair = optional(runContext, keypairName);
        String renderedNetworkId = optional(runContext, networkId);
        long bootSize = renderedSize == null ? 32L : renderedSize.longValue();

        runContext.logger().info("Creating STACKIT server '{}'.", serverName);

        CreateServerPayload payload = new CreateServerPayload()
            .name(serverName)
            .machineType(renderedMachineType)
            .bootVolume(new BootVolume()
                .deleteOnTermination(true)
                .size(bootSize)
                .source(new BootVolumeSource()
                    .id(UUID.fromString(renderedImageId))
                    .type("image")));
        if (renderedZone != null) {
            payload.availabilityZone(renderedZone);
        }
        if (renderedKeypair != null) {
            payload.keypairName(renderedKeypair);
        }
        if (renderedNetworkId != null) {
            payload.networking(new CreateServerPayloadAllOfNetworking(
                new CreateServerNetworking().networkId(UUID.fromString(renderedNetworkId))
            ));
        }

        UUID stackitProjectId = projectId(runContext);
        String stackitRegion = region(runContext);
        try (IaasClient client = iaasClient()) {
            Server server = call(() -> client.api().createServer(stackitProjectId, stackitRegion, payload));
            String serverId = server.getId() == null ? null : server.getId().toString();
            return Output.builder()
                .serverId(serverId)
                .name(server.getName())
                .status(server.getStatus())
                .machineType(server.getMachineType())
                .availabilityZone(server.getAvailabilityZone())
                .build();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Server ID",
            description = "ID of the created server."
        )
        private final String serverId;

        @Schema(
            title = "Server name",
            description = "Name returned by STACKIT."
        )
        private final String name;

        @Schema(
            title = "Status",
            description = "Server status returned by STACKIT."
        )
        private final String status;

        @Schema(
            title = "Machine type",
            description = "Machine type returned by STACKIT."
        )
        private final String machineType;

        @Schema(
            title = "Availability zone",
            description = "Availability zone returned by STACKIT."
        )
        private final String availabilityZone;
    }
}
