package io.kestra.plugin.stackit.iaas.volume;

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
@Schema(
    title = "Delete a STACKIT volume",
    description = """
        Deletes a volume in a project and region.
        """
)
@Plugin(examples = @Example(title = "Delete a volume", full = true, code = """
    id: stackit_delete_volume
    namespace: company.team

    tasks:
      - id: delete_volume
        type: io.kestra.plugin.stackit.iaas.volume.Delete
        serviceAccountKey: "{{ secret('STACKIT_SERVICE_ACCOUNT_KEY') }}"
        projectId: "{{ secret('STACKIT_PROJECT_ID') }}"
        region: eu01
        volumeId: 11111111-1111-1111-1111-111111111111
    """))
public class Delete extends AbstractStackit implements RunnableTask<VoidOutput> {
    @Schema(title = "Volume ID")
    @PluginProperty(group = "main")
    @NotNull
    private Property<@NotBlank String> volumeId;

    @Schema(
        title = "Force",
        description = """
            Delete the volume even when it is still attached. Defaults to false.
            """
    )
    @PluginProperty(group = "advanced")
    @Builder.Default
    private Property<Boolean> force = Property.ofValue(false);

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {
        var rVolumeId = uuid("volumeId", runContext.render(volumeId).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("volumeId is required")));
        var rForce = runContext.render(force).as(Boolean.class).orElse(false);
        var rProjectId = uuid("projectId", runContext.render(getProjectId()).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("projectId is required")));
        var rRegion = runContext.render(getRegion()).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("region is required"));
        try (var client = iaasClient(runContext)) {
            call(() -> {
                client.api().deleteVolume(rProjectId, rRegion, rVolumeId, rForce);
                return null;
            });
        }
        return null;
    }
}