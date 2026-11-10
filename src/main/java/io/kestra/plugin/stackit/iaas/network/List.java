package io.kestra.plugin.stackit.iaas.network;

import cloud.stackit.sdk.iaas.v2api.model.Network;
import cloud.stackit.sdk.iaas.v2api.model.NetworkListResponse;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
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

import java.util.ArrayList;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(title = "List STACKIT networks")
@Plugin(examples = @Example(title = "List networks", full = true, code = """
    id: stackit_list_networks
    namespace: company.team

    tasks:
      - id: list_networks
        type: io.kestra.plugin.stackit.iaas.network.List
        serviceAccountKey: "{{ secret('STACKIT_SERVICE_ACCOUNT_KEY') }}"
        projectId: "{{ secret('STACKIT_PROJECT_ID') }}"
        region: eu01
    """))
public class List extends AbstractStackit implements RunnableTask<List.Output> {
    @Override
    public Output run(RunContext runContext) throws Exception {
        var project = projectId(runContext);
        String stackitRegion = region(runContext);
        try (IaasClient client = iaasClient(runContext)) {
            NetworkListResponse response = call(() -> client.api().listNetworks(project, stackitRegion, null));
            java.util.List<String> ids = new ArrayList<>();
            java.util.List<String> names = new ArrayList<>();
            if (response.getItems() != null) {
                for (Network network : response.getItems()) {
                    ids.add(network.getId() == null ? null : network.getId().toString());
                    names.add(network.getName());
                }
            }
            return Output.builder().count(ids.size()).ids(ids).names(names).build();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Count")
        private final Integer count;
        @Schema(title = "Network IDs")
        private final java.util.List<String> ids;
        @Schema(title = "Network names")
        private final java.util.List<String> names;
    }
}
