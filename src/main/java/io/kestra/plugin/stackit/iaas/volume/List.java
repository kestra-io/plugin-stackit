package io.kestra.plugin.stackit.iaas.volume;

import cloud.stackit.sdk.iaas.v2api.model.Volume;
import cloud.stackit.sdk.iaas.v2api.model.VolumeListResponse;
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
@Schema(title = "List STACKIT volumes")
@Plugin(examples = @Example(title = "List volumes", full = true, code = """
    id: stackit_list_volumes
    namespace: company.team

    tasks:
      - id: list_volumes
        type: io.kestra.plugin.stackit.iaas.volume.List
        projectId: "{{ secret('STACKIT_PROJECT_ID') }}"
        region: eu01
    """))
public class List extends AbstractStackit implements RunnableTask<List.Output> {
    @Override
    public Output run(RunContext runContext) throws Exception {
        var project = projectId(runContext);
        String stackitRegion = region(runContext);
        try (IaasClient client = iaasClient()) {
            VolumeListResponse response = call(() -> client.api().listVolumes(project, stackitRegion, null));
            java.util.List<String> ids = new ArrayList<>();
            java.util.List<String> names = new ArrayList<>();
            if (response.getItems() != null) {
                for (Volume volume : response.getItems()) {
                    ids.add(volume.getId() == null ? null : volume.getId().toString());
                    names.add(volume.getName());
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
        @Schema(title = "Volume IDs")
        private final java.util.List<String> ids;
        @Schema(title = "Volume names")
        private final java.util.List<String> names;
    }
}
