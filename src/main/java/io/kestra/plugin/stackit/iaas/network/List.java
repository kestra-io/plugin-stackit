package io.kestra.plugin.stackit.iaas.network;

import cloud.stackit.sdk.iaas.v2api.model.Network;
import cloud.stackit.sdk.iaas.v2api.model.NetworkListResponse;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.common.FetchType;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.stackit.iaas.AbstractStackitList;
import io.kestra.plugin.stackit.iaas.StackitListOutput;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(title = "List STACKIT networks",
    description = "Lists networks in a project and region."
)
@Plugin(aliases = "io.kestra.plugin.stackit", examples = @Example(title = "List networks", full = true, code = """
    id: stackit_list_networks
    namespace: company.team

    tasks:
      - id: list_networks
        type: io.kestra.plugin.stackit.iaas.network.List
        serviceAccountKey: "{{ secret('STACKIT_SERVICE_ACCOUNT_KEY') }}"
        projectId: "{{ secret('STACKIT_PROJECT_ID') }}"
        region: eu01
    """))
public class List extends AbstractStackitList implements RunnableTask<List.Output> {

    @Override
    public Output run(RunContext runContext) throws Exception {
        var project = projectId(runContext);
        String stackitRegion = region(runContext);
        String selector = labelSelector(runContext);
        FetchType type = renderedFetchType(runContext);
        try (IaasClient client = iaasClient(runContext)) {
            NetworkListResponse response = call(() -> client.api().listNetworks(project, stackitRegion, selector));
            java.util.List<String> ids = new ArrayList<>();
            java.util.List<String> names = new ArrayList<>();
            if (response.getItems() != null) {
                for (Network network : response.getItems()) {
                    ids.add(network.getId() == null ? null : network.getId().toString());
                    names.add(network.getName());
                }
            }
            StackitListOutput.Result result = StackitListOutput.build(runContext, type, ids, names);
            return Output.builder()
                .count(result.getCount())
                .ids(result.getIds())
                .names(result.getNames())
                .row(result.getRow())
                .rows(result.getRows())
                .uri(result.getUri())
                .size(result.getSize())
                .build();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Count")
        private final Integer count;
        @Schema(title = "Network IDs", description = "Set when fetchType is FETCH.")
        private final java.util.List<String> ids;
        @Schema(title = "Network names", description = "Set when fetchType is FETCH.")
        private final java.util.List<String> names;
        @Schema(title = "First network", description = "Set when fetchType is FETCH_ONE.")
        private final Map<String, Object> row;
        @Schema(title = "Networks", description = "Set when fetchType is FETCH.")
        private final java.util.List<Map<String, Object>> rows;
        @Schema(title = "Storage URI", description = "Set when fetchType is STORE.")
        private final URI uri;
        @Schema(title = "Size", description = "Total number of networks listed.")
        private final Long size;
    }
}