package io.kestra.plugin.stackit.iaas.volume;

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
@Schema(title = "Delete a STACKIT volume", description = "Deletes a volume. force matches the SDK deleteVolume force flag.")
@Plugin(examples = @Example(title = "Delete a volume", full = true, code = """
    id: stackit_delete_volume
    namespace: company.team

    tasks:
      - id: delete_volume
        type: io.kestra.plugin.stackit.iaas.volume.Delete
        projectId: "{{ secret('STACKIT_PROJECT_ID') }}"
        region: eu01
        volumeId: 11111111-1111-1111-1111-111111111111
    """))
public class Delete extends AbstractStackit implements RunnableTask<Delete.Output> {
    @Schema(title = "Volume ID")
    @PluginProperty(group = "volume")
    private Property<String> volumeId;

    @Schema(title = "Force", description = "SDK deleteVolume force flag. Defaults to false.")
    @PluginProperty(group = "volume")
    private Property<Boolean> force;

    @Override
    public Output run(RunContext runContext) throws Exception {
        UUID id = requiredUuid(runContext, volumeId, "volumeId");
        Boolean renderedForce = optionalBoolean(runContext, force);
        var project = projectId(runContext);
        String stackitRegion = region(runContext);
        try (IaasClient client = iaasClient()) {
            call(() -> {
                client.api().deleteVolume(project, stackitRegion, id, renderedForce != null && renderedForce);
                return null;
            });
        }
        return Output.builder().volumeId(id.toString()).build();
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Volume ID")
        private final String volumeId;
    }
}
