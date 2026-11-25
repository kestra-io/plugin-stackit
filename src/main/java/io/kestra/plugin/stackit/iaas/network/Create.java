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

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(
    title = "Create a STACKIT network",
    description = """
        Creates a network in a project and region. DHCP, routing, and prefix length use plugin defaults when omitted;
        nameserver is sent only when you set it.
        """
)
@Plugin(examples = @Example(title = "Create a network", full = true, code = """
    id: stackit_create_network
    namespace: company.team

    tasks:
      - id: create_network
        type: io.kestra.plugin.stackit.iaas.network.Create
        serviceAccountKey: "{{ secret('STACKIT_SERVICE_ACCOUNT_KEY') }}"
        projectId: "{{ secret('STACKIT_PROJECT_ID') }}"
        region: eu01
        name: kestra-network
    """))
public class Create extends AbstractStackit implements RunnableTask<Create.Output> {
    @Schema(title = "Network name")
    @PluginProperty(group = "main")
    @NotNull
    private Property<@NotBlank String> name;

    @Schema(
        title = "DHCP",
        description = """
            Enable DHCP for the network IPv4 block. Defaults to true.
            """
    )
    @PluginProperty(group = "advanced")
    @Builder.Default
    private Property<Boolean> dhcp = Property.ofValue(true);

    @Schema(
        title = "Routed",
        description = """
            Whether the network is routed. Defaults to false.
            """
    )
    @PluginProperty(group = "advanced")
    @Builder.Default
    private Property<Boolean> routed = Property.ofValue(false);

    @Schema(
        title = "IPv4 prefix length",
        description = """
            Prefix length for the network IPv4 block. Allowed range is 8–30. Defaults to 24.
            """
    )
    @PluginProperty(group = "advanced")
    @Builder.Default
    private Property<@Min(8) @Max(30) Integer> prefixLength = Property.ofValue(24);

    @Schema(
        title = "Nameserver",
        description = """
            Optional DNS resolver for the network IPv4 block. When omitted, no nameserver is sent and STACKIT applies its default.
            """
    )
    @PluginProperty(group = "advanced")
    private Property<String> nameserver;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var rName = runContext.render(name).as(String.class).orElseThrow(() -> new IllegalArgumentException("name is required"));
        var rDhcp = runContext.render(dhcp).as(Boolean.class).orElse(true);
        var rRouted = runContext.render(routed).as(Boolean.class).orElse(false);
        var rPrefix = runContext.render(prefixLength).as(Integer.class).orElse(24);
        var rNameserver = runContext.render(nameserver).as(String.class).filter(value -> !value.isBlank()).orElse(null);
        var ipv4 = new CreateNetworkIPv4WithPrefixLength().prefixLength(rPrefix.longValue());
        if (rNameserver != null) {
            ipv4.addNameserversItem(rNameserver);
        }
        var payload = new CreateNetworkPayload()
            .name(rName)
            .dhcp(rDhcp)
            .routed(rRouted)
            .ipv4(new CreateNetworkIPv4(ipv4));
        var rProjectId = uuid("projectId", runContext.render(getProjectId()).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("projectId is required")));
        var rRegion = runContext.render(getRegion()).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("region is required"));
        try (var client = iaasClient(runContext)) {
            var network = call(() -> client.api().createNetwork(rProjectId, rRegion, payload));
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