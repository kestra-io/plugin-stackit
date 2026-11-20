package io.kestra.plugin.stackit.iaas.server;

import cloud.stackit.sdk.iaas.v2api.model.Server;
import cloud.stackit.sdk.iaas.v2api.model.ServerListResponse;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
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
@Schema(title = "List STACKIT servers", description = "Lists servers in a project and region.")
@Plugin(aliases = "io.kestra.plugin.stackit", examples = @Example(title = "List servers", full = true, code = """
    id: stackit_list_servers
    namespace: company.team

    tasks:
      - id: list_servers
        type: io.kestra.plugin.stackit.iaas.server.List
        serviceAccountKey: "{{ secret('STACKIT_SERVICE_ACCOUNT_KEY') }}"
        projectId: "{{ secret('STACKIT_PROJECT_ID') }}"
        region: eu01
    """))
public class List extends AbstractStackitList implements RunnableTask<List.Output> {

    @Schema(title = "Details", description = "When true, the API returns detailed server information.")
    @PluginProperty(group = "advanced")
    private Property<Boolean> details;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var project = projectId(runContext);
        String stackitRegion = region(runContext);
        Boolean renderedDetails = optionalBoolean(runContext, details);
        boolean requestDetails = renderedDetails != null && renderedDetails;
        String selector = labelSelector(runContext);
        FetchType type = renderedFetchType(runContext);
        try (IaasClient client = iaasClient(runContext)) {
            ServerListResponse response = call(() -> client.api().listServers(
                project, stackitRegion, requestDetails, selector));
            java.util.List<String> ids = new ArrayList<>();
            java.util.List<String> names = new ArrayList<>();
            if (response.getItems() != null) {
                for (Server server : response.getItems()) {
                    ids.add(server.getId() == null ? null : server.getId().toString());
                    names.add(server.getName());
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
        @Schema(title = "Server IDs", description = "Set when fetchType is FETCH.")
        private final java.util.List<String> ids;
        @Schema(title = "Server names", description = "Set when fetchType is FETCH.")
        private final java.util.List<String> names;
        @Schema(title = "First server", description = "Set when fetchType is FETCH_ONE.")
        private final Map<String, Object> row;
        @Schema(title = "Servers", description = "Set when fetchType is FETCH.")
        private final java.util.List<Map<String, Object>> rows;
        @Schema(title = "Storage URI", description = "Set when fetchType is STORE.")
        private final URI uri;
        @Schema(title = "Size", description = "Total number of servers listed.")
        private final Long size;
    }
}