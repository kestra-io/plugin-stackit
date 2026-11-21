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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Plugin(examples = {
    @Example(
        title = "Create a server and log its ID",
        full = true,
        code = """
            id: stackit_create_server
            namespace: company.team

            tasks:
              - id: create_server
                type: io.kestra.plugin.stackit.iaas.server.Create
                serviceAccountKey: "{{ secret('STACKIT_SERVICE_ACCOUNT_KEY') }}"
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
})
public class Create extends AbstractStackit implements RunnableTask<Create.Output> {
    @Schema(
        title = "Server name",
        description = "Name of the server to create."
    )
    @PluginProperty(group = "main")
    @NotNull
    private Property<@NotBlank String> name;

    @Schema(
        title = "Machine type",
        description = """
            Required machine type name that exists in the region. The SDK sample name is t2i.1.
            """
    )
    @PluginProperty(group = "main")
    @NotNull
    private Property<@NotBlank String> machineType;

    @Schema(
        title = "Availability zone",
        description = "Availability zone, for example eu01-1. Leave empty to let STACKIT choose."
    )
    @PluginProperty(group = "advanced")
    private Property<String> availabilityZone;

    @Schema(
        title = "Image ID",
        description = "Operating system image UUID used as the boot volume source."
    )
    @PluginProperty(group = "main")
    @NotNull
    private Property<@NotBlank String> imageId;

    @Schema(
        title = "Boot volume size (GiB)",
        description = "Boot disk size in GiB. Defaults to 32, as in the STACKIT Java SDK example."
    )
    @PluginProperty(group = "advanced")
    @Builder.Default
    private Property<@Min(1) @Max(16384) Integer> bootVolumeSizeGiB = Property.ofValue(32);

    @Schema(
        title = "Keypair name",
        description = "Optional SSH keypair already created in the project."
    )
    @PluginProperty(group = "advanced")
    private Property<String> keypairName;

    @Schema(
        title = "Network ID",
        description = "Optional network UUID to attach. Leave empty to use the project default network."
    )
    @PluginProperty(group = "advanced")
    private Property<String> networkId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var rName = runContext.render(name).as(String.class).orElseThrow(() -> new IllegalArgumentException("name is required"));
        var rMachineType = runContext.render(machineType).as(String.class).orElseThrow(() -> new IllegalArgumentException("machineType is required"));
        var rZone = availabilityZone == null ? null : runContext.render(availabilityZone).as(String.class).filter(value -> !value.isBlank()).orElse(null);
        var rImageId = runContext.render(imageId).as(String.class).orElseThrow(() -> new IllegalArgumentException("imageId is required"));
        var rBootSize = runContext.render(bootVolumeSizeGiB).as(Integer.class).orElse(32);
        var rKeypair = keypairName == null ? null : runContext.render(keypairName).as(String.class).filter(value -> !value.isBlank()).orElse(null);
        var rNetworkId = networkId == null ? null : runContext.render(networkId).as(String.class).filter(value -> !value.isBlank()).orElse(null);

        runContext.logger().info("Creating STACKIT server '{}'.", rName);

        var payload = new CreateServerPayload()
            .name(rName)
            .machineType(rMachineType)
            .bootVolume(new BootVolume()
                .deleteOnTermination(true)
                .size(rBootSize.longValue())
                .source(new BootVolumeSource()
                    .id(UUID.fromString(rImageId))
                    .type("image")));
        if (rZone != null) {
            payload.availabilityZone(rZone);
        }
        if (rKeypair != null) {
            payload.keypairName(rKeypair);
        }
        if (rNetworkId != null) {
            payload.networking(new CreateServerPayloadAllOfNetworking(
                new CreateServerNetworking().networkId(UUID.fromString(rNetworkId))
            ));
        }

        var rProjectId = UUID.fromString(runContext.render(getProjectId()).as(String.class).orElseThrow(() -> new IllegalArgumentException("projectId is required")));
        var rRegion = runContext.render(getRegion()).as(String.class).orElseThrow(() -> new IllegalArgumentException("region is required"));
        try (IaasClient client = iaasClient(runContext)) {
            var server = call(() -> client.api().createServer(rProjectId, rRegion, payload));
            var serverId = server.getId() == null ? null : server.getId().toString();
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
        @Schema(title = "Server ID")
        private final String serverId;
        @Schema(title = "Server name")
        private final String name;
        @Schema(title = "Status")
        private final String status;
        @Schema(title = "Machine type")
        private final String machineType;
        @Schema(title = "Availability zone")
        private final String availabilityZone;
    }
}