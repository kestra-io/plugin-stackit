package io.kestra.plugin.stackit.iaas.network;

import cloud.stackit.sdk.iaas.v2api.model.Network;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.common.FetchType;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.stackit.iaas.AbstractStackitList;
import io.kestra.plugin.stackit.iaas.StackitListOutput;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(
    title = "List STACKIT networks",
    description = "Lists networks in a project and region."
)
@Plugin(examples = @Example(title = "List networks", full = true, code = """
    id: stackit_list_networks
    namespace: company.team

    tasks:
      - id: list_networks
        type: io.kestra.plugin.stackit.iaas.network.List
        serviceAccountKey: "{{ secret('STACKIT_SERVICE_ACCOUNT_KEY') }}"
        projectId: "{{ secret('STACKIT_PROJECT_ID') }}"
        region: eu01
    """))
public class List extends AbstractStackitList implements RunnableTask<StackitListOutput.ListOutput> {

    @Override
    public StackitListOutput.ListOutput run(RunContext runContext) throws Exception {
        var rProjectId = uuid("projectId", runContext.render(getProjectId()).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("projectId is required")));
        var rRegion = runContext.render(getRegion()).as(String.class)
            .orElseThrow(() -> new IllegalArgumentException("region is required"));
        var rLabelSelector = runContext.render(getLabelSelector()).as(String.class).filter(value -> !value.isBlank()).orElse(null);
        var rFetchType = runContext.render(getFetchType()).as(FetchType.class).orElse(FetchType.FETCH);
        try (IaasClient client = iaasClient(runContext)) {
            var response = call(() -> client.api().listNetworks(rProjectId, rRegion, rLabelSelector));
            var ids = new ArrayList<String>();
            var names = new ArrayList<String>();
            if (response.getItems() != null) {
                for (Network network : response.getItems()) {
                    ids.add(network.getId() == null ? null : network.getId().toString());
                    names.add(network.getName());
                }
            }
            return StackitListOutput.build(runContext, rFetchType, ids, names);
        }
    }
}