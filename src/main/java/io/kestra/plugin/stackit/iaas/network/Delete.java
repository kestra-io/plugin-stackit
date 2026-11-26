package io.kestra.plugin.stackit.iaas.network;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.VoidOutput;
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

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(title = "Delete a STACKIT network",
    description = """
        Deletes a network in a STACKIT project and region.
        """
)
@Plugin(examples = @Example(title = "Delete a network", full = true, code = """
    id: stackit_delete_network
    namespace: company.team

    tasks:
      - id: delete_network
        type: io.kestra.plugin.stackit.iaas.network.Delete
        serviceAccountKey: "{{ secret('STACKIT_SERVICE_ACCOUNT_KEY') }}"
        projectId: "{{ secret('STACKIT_PROJECT_ID') }}"
        region: eu01
        networkId: 11111111-1111-1111-1111-111111111111
    """))
public class Delete extends AbstractStackit implements RunnableTask<VoidOutput> {
    @Schema(title = "Network ID")
    @PluginProperty(group = "main")
    @NotNull
    private Property<@NotBlank String> networkId;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        var rNetworkId = uuid("networkId", runContext.render(networkId).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("networkId is required")));
        var rProjectId = uuid("projectId", runContext.render(getProjectId()).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("projectId is required")));
        var rRegion = runContext.render(getRegion()).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("region is required"));
        try (var client = iaasClient(runContext)) {
            call(() -> {
                client.api().deleteNetwork(rProjectId, rRegion, rNetworkId);
                return null;
            });
        }
        return null;
    }
}