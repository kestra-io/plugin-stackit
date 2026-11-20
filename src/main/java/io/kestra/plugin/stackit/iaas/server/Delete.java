package io.kestra.plugin.stackit.iaas.server;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
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
@Schema(title = "Delete a STACKIT server",
    description = "Deletes a server in a STACKIT project and region."
)
@Plugin(aliases = "io.kestra.plugin.stackit", examples = @Example(title = "Delete a server", full = true, code = """
    id: stackit_delete_server
    namespace: company.team

    tasks:
      - id: delete_server
        type: io.kestra.plugin.stackit.iaas.server.Delete
        serviceAccountKey: "{{ secret('STACKIT_SERVICE_ACCOUNT_KEY') }}"
        projectId: "{{ secret('STACKIT_PROJECT_ID') }}"
        region: eu01
        serverId: 11111111-1111-1111-1111-111111111111
    """))
public class Delete extends AbstractStackitServerById implements RunnableTask<Delete.Output> {

    @Override
    public Output run(RunContext runContext) throws Exception {
        String id = runServerOperation(runContext, (api, project, stackitRegion, serverId) ->
            api.deleteServer(project, stackitRegion, serverId));
        return Output.builder().serverId(id).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Server ID")
        private final String serverId;
    }
}