package io.kestra.plugin.stackit.iaas.network;

import cloud.stackit.sdk.iaas.v2api.model.CreateNetworkIPv4;
import cloud.stackit.sdk.iaas.v2api.model.CreateNetworkIPv4WithPrefixLength;
import cloud.stackit.sdk.iaas.v2api.model.CreateNetworkPayload;
import cloud.stackit.sdk.iaas.v2api.model.Network;
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

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(title = "Create a STACKIT network", description = "Creates a network using the same defaults as the STACKIT Java SDK example.")
@Plugin(examples = @Example(title = "Create a network", full = true, code = """
    id: stackit_create_network
    namespace: company.team

    tasks:
      - id: create_network
        type: io.kestra.plugin.stackit.iaas.network.Create
        projectId: "{{ secret('STACKIT_PROJECT_ID') }}"
        region: eu01
        name: kestra-network
    """))
public class Create extends AbstractStackit implements RunnableTask<Create.Output> {
    @Schema(title = "Network name")
    @PluginProperty(group = "network")
    private Property<String> name;

    @Schema(title = "DHCP", description = "Defaults to true, as in the SDK example.")
    @PluginProperty(group = "network")
    private Property<Boolean> dhcp;

    @Schema(title = "Routed", description = "Defaults to false, as in the SDK example.")
    @PluginProperty(group = "network")
    private Property<Boolean> routed;

    @Schema(title = "IPv4 prefix length", description = "Defaults to 24.")
    @PluginProperty(group = "network")
    private Property<Integer> prefixLength;

    @Schema(title = "Nameserver", description = "Defaults to 8.8.8.8, as in the SDK example.")
    @PluginProperty(group = "network")
    private Property<String> nameserver;

    @Override
    public Output run(RunContext runContext) throws Exception {
        String networkName = required(runContext, name, "name");
        Boolean renderedDhcp = optionalBoolean(runContext, dhcp);
        Boolean renderedRouted = optionalBoolean(runContext, routed);
        Integer renderedPrefix = optionalInteger(runContext, prefixLength);
        String renderedNameserver = optional(runContext, nameserver);
        CreateNetworkPayload payload = new CreateNetworkPayload()
            .name(networkName)
            .dhcp(renderedDhcp == null || renderedDhcp)
            .routed(renderedRouted != null && renderedRouted)
            .ipv4(new CreateNetworkIPv4(new CreateNetworkIPv4WithPrefixLength()
                .addNameserversItem(renderedNameserver == null ? "8.8.8.8" : renderedNameserver)
                .prefixLength(renderedPrefix == null ? 24L : renderedPrefix.longValue())));
        var project = projectId(runContext);
        String stackitRegion = region(runContext);
        try (IaasClient client = iaasClient()) {
            Network network = call(() -> client.api().createNetwork(project, stackitRegion, payload));
            return Output.builder()
                .networkId(network.getId() == null ? null : network.getId().toString())
                .name(network.getName())
                .build();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Network ID")
        private final String networkId;
        @Schema(title = "Name")
        private final String name;
    }
}
