package io.kestra.plugin.stackit.iaas.server;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Plugin(examples = @Example(title = "Delete a server", full = true, code = """
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
public class Delete extends AbstractStackitServerById implements RunnableTask<VoidOutput> {

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        runServerOperation(runContext, (api, project, rRegion, server) ->
            api.deleteServer(project, rRegion, server));
        return null;
    }
}