package io.kestra.plugin.stackit.iaas.volume;

import cloud.stackit.sdk.iaas.v2api.model.CreateVolumePayload;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.stackit.auth.AbstractStackit;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
    title = "Create a STACKIT volume",
    description = """
        Creates a data volume in GiB.
        """
)
@Plugin(examples = @Example(title = "Create a volume", full = true, code = """
    id: stackit_create_volume
    namespace: company.team

    tasks:
      - id: create_volume
        type: io.kestra.plugin.stackit.iaas.volume.Create
        serviceAccountKey: "{{ secret('STACKIT_SERVICE_ACCOUNT_KEY') }}"
        projectId: "{{ secret('STACKIT_PROJECT_ID') }}"
        region: eu01
        name: kestra-data
        sizeGiB: 10
        availabilityZone: eu01-1
    """))
public class Create extends AbstractStackit implements RunnableTask<Create.Output> {
    @Schema(title = "Volume name")
    @PluginProperty(group = "main")
    @NotNull
    private Property<@NotBlank String> name;

    @Schema(
        title = "Size in GiB",
        description = """
            Volume capacity in GiB. Allowed range is 1 to 16384.
            """
    )
    @PluginProperty(group = "main")
    @NotNull
    private Property<@Min(1) @Max(16384) Integer> sizeGiB;

    @Schema(
        title = "Availability zone",
        description = """
            Availability zone, for example eu01-1.
            """
    )
    @PluginProperty(group = "main")
    @NotNull
    private Property<@NotBlank String> availabilityZone;

    @Schema(title = "Performance class")
    @PluginProperty(group = "advanced")
    private Property<String> performanceClass;

    @Override
    public Output run(RunContext runContext) throws Exception {
        var rName = runContext.render(name).as(String.class).orElseThrow(() -> new IllegalArgumentException("name is required"));
        var rSize = runContext.render(sizeGiB).as(Integer.class).orElseThrow(() -> new IllegalArgumentException("sizeGiB is required"));
        var rZone = runContext.render(availabilityZone).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("availabilityZone is required"));
        var payload = new CreateVolumePayload().name(rName).size(rSize.longValue()).availabilityZone(rZone);
        var rPerformance = runContext.render(performanceClass).as(String.class).filter(value -> !value.isBlank()).orElse(null);
        if (rPerformance != null) {
            payload.performanceClass(rPerformance);
        }
        var rProjectId = uuid("projectId", runContext.render(getProjectId()).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("projectId is required")));
        var rRegion = runContext.render(getRegion()).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("region is required"));
        try (var client = iaasClient(runContext)) {
            var volume = call(() -> client.api().createVolume(rProjectId, rRegion, payload));
            return Output.builder()
                .volumeId(volume.getId() == null ? null : volume.getId().toString())
                .name(volume.getName())
                .sizeGiB(volume.getSize() == null ? null : volume.getSize().intValue())
                .status(volume.getStatus())
                .build();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Volume ID")
        private final String volumeId;
        @Schema(title = "Name")
        private final String name;
        @Schema(title = "Size in GiB")
        private final Integer sizeGiB;
        @Schema(title = "Status")
        private final String status;
    }
}