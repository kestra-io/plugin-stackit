package io.kestra.plugin.stackit.iaas.volume;

import cloud.stackit.sdk.iaas.v2api.model.CreateVolumePayload;
import cloud.stackit.sdk.iaas.v2api.model.Volume;
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

@SuperBuilder
@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@NoArgsConstructor
@Schema(title = "Create a STACKIT volume", description = "Creates a data volume. size is GiB, matching CreateVolumePayload.")
@Plugin(examples = @Example(title = "Create a volume", full = true, code = """
    id: stackit_create_volume
    namespace: company.team

    tasks:
      - id: create_volume
        type: io.kestra.plugin.stackit.iaas.volume.Create
        projectId: "{{ secret('STACKIT_PROJECT_ID') }}"
        region: eu01
        name: kestra-data
        sizeGiB: 10
        availabilityZone: eu01-1
    """))
public class Create extends AbstractStackit implements RunnableTask<Create.Output> {
    @Schema(title = "Volume name")
    @PluginProperty(group = "volume")
    private Property<String> name;

    @Schema(title = "Size in GiB", description = "SDK field CreateVolumePayload.size.")
    @PluginProperty(group = "volume")
    private Property<Integer> sizeGiB;

    @Schema(title = "Availability zone", description = "SDK field availabilityZone, for example eu01-1.")
    @PluginProperty(group = "volume")
    private Property<String> availabilityZone;

    @Schema(title = "Performance class")
    @PluginProperty(group = "volume")
    private Property<String> performanceClass;

    @Override
    public Output run(RunContext runContext) throws Exception {
        String volumeName = required(runContext, name, "name");
        Integer size = optionalInteger(runContext, sizeGiB);
        if (size == null) {
            throw new IllegalArgumentException("sizeGiB is required.");
        }
        CreateVolumePayload payload = new CreateVolumePayload().name(volumeName).size(size.longValue());
        String zone = optional(runContext, availabilityZone);
        String performance = optional(runContext, performanceClass);
        if (zone != null) {
            payload.availabilityZone(zone);
        }
        if (performance != null) {
            payload.performanceClass(performance);
        }
        var project = projectId(runContext);
        String stackitRegion = region(runContext);
        try (IaasClient client = iaasClient()) {
            Volume volume = call(() -> client.api().createVolume(project, stackitRegion, payload));
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
