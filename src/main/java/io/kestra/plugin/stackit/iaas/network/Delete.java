package io.kestra.plugin.stackit.iaas.network;

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
@Schema(title = "Delete a STACKIT network")
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
public class Delete extends AbstractStackit implements RunnableTask<Delete.Output> {
    @Schema(title = "Network ID")
    @PluginProperty(group = "network")
    private Property<String> networkId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        UUID id = requiredUuid(runContext, networkId, "networkId");
        var project = projectId(runContext);
        String stackitRegion = region(runContext);
        try (IaasClient client = iaasClient(runContext)) {
            call(() -> {
                client.api().deleteNetwork(project, stackitRegion, id);
                return null;
            });
        }
        return Output.builder().networkId(id.toString()).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Network ID")
        private final String networkId;
    }
}
