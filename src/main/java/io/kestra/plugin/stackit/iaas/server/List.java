package io.kestra.plugin.stackit.iaas.server;

import cloud.stackit.sdk.iaas.v2api.model.Server;
import cloud.stackit.sdk.iaas.v2api.model.ServerListResponse;
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
@Schema(title = "List STACKIT servers", description = "Lists servers in a project and region.")
@Plugin(examples = @Example(title = "List servers", full = true, code = """
    id: stackit_list_servers
    namespace: company.team

    tasks:
      - id: list_servers
        type: io.kestra.plugin.stackit.iaas.server.List
        projectId: "{{ secret('STACKIT_PROJECT_ID') }}"
        region: eu01
    """))
public class List extends AbstractStackit implements RunnableTask<List.Output> {
    @Override
    public Output run(RunContext runContext) throws Exception {
        var project = projectId(runContext);
        String stackitRegion = region(runContext);
        try (IaasClient client = iaasClient()) {
            ServerListResponse response = call(() -> client.api().listServers(project, stackitRegion, false, null));
            java.util.List<String> ids = new ArrayList<>();
            java.util.List<String> names = new ArrayList<>();
            if (response.getItems() != null) {
                for (Server server : response.getItems()) {
                    ids.add(server.getId() == null ? null : server.getId().toString());
                    names.add(server.getName());
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
        @Schema(title = "Server IDs")
        private final java.util.List<String> ids;
        @Schema(title = "Server names")
        private final java.util.List<String> names;
    }
}
