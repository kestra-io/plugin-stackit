package io.kestra.plugin.stackit.iaas.server;

import cloud.stackit.sdk.iaas.v2api.model.CreateServerPayload;
import cloud.stackit.sdk.iaas.v2api.model.Server;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.stackit.AbstractStackit;
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
                    serviceAccountKey: "{{ secret('STACKIT_SERVICE_ACCOUNT_KEY') }}"
                    projectId: "{{ secret('STACKIT_PROJECT_ID') }}"
                    region: eu01
                    name: kestra-demo
                    machineType: g2i.2
                    availabilityZone: eu01-1
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
        description = "STACKIT machine type, for example g2i.2."
    )
    @PluginProperty(group = "server")
    private Property<String> machineType;

    @Schema(
        title = "Availability zone",
        description = "Availability zone, for example eu01-1."
    )
    @PluginProperty(group = "server")
    private Property<String> availabilityZone;

    @Schema(
        title = "Image ID",
        description = "Operating system image UUID. Leave empty to let STACKIT apply its default."
    )
    @PluginProperty(group = "server")
    private Property<String> imageId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        String serverName = renderRequired(runContext, name, "name");
        String renderedMachineType = renderRequired(runContext, machineType, "machineType");
        String renderedZone = renderRequired(runContext, availabilityZone, "availabilityZone");
        String renderedImageId = renderOptional(runContext, imageId);

        runContext.logger().info("Creating STACKIT server '{}'.", serverName);

        CreateServerPayload payload = new CreateServerPayload()
            .name(serverName)
            .machineType(renderedMachineType)
            .availabilityZone(renderedZone);
        if (renderedImageId != null) {
            payload.imageId(UUID.fromString(renderedImageId));
        }

        UUID stackitProjectId = projectId(runContext);
        String stackitRegion = region(runContext);
        try (IaasClient client = iaasClient(runContext)) {
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

    private static String renderRequired(RunContext runContext, Property<String> property, String propertyName) throws Exception {
        String rendered = renderOptional(runContext, property);
        if (rendered == null) {
            throw new IllegalArgumentException(propertyName + " is required.");
        }
        return rendered;
    }

    private static String renderOptional(RunContext runContext, Property<String> property) throws Exception {
        if (property == null) {
            return null;
        }
        return runContext.render(property).as(String.class)
            .filter(value -> !value.isBlank())
            .orElse(null);
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
