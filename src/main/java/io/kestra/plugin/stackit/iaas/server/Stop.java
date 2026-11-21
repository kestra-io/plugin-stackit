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
@Schema(title = "Stop a STACKIT server",
    description = "Stops a server in a STACKIT project and region."
)
@Plugin(examples = @Example(title = "Stop a server", full = true, code = """
    id: stackit_stop_server
    namespace: company.team

    tasks:
      - id: stop_server
        type: io.kestra.plugin.stackit.iaas.server.Stop
        serviceAccountKey: "{{ secret('STACKIT_SERVICE_ACCOUNT_KEY') }}"
        projectId: "{{ secret('STACKIT_PROJECT_ID') }}"
        region: eu01
        serverId: 11111111-1111-1111-1111-111111111111
    """))
public class Stop extends AbstractStackitServerById implements RunnableTask<Stop.Output> {

    @Override
    public Output run(RunContext runContext) throws Exception {
        var id = runServerOperation(runContext, (api, project, rRegion, server) ->
            api.stopServer(project, rRegion, server));
        return Output.builder().serverId(id).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Server ID")
        private final String serverId;
    }
}