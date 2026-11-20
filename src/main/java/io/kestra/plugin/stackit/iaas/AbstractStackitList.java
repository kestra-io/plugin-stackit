package io.kestra.plugin.stackit.iaas;

import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.common.FetchType;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.stackit.auth.AbstractStackit;
import io.swagger.v3.oas.annotations.media.Schema;
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
public abstract class AbstractStackitList extends AbstractStackit {

    @Schema(
        title = "Fetch type",
        description = "FETCH returns ids and names in outputs. FETCH_ONE returns the first item only. "
            + "STORE writes all items to internal storage and returns a URI. NONE returns only the count."
    )
    @PluginProperty(group = "advanced")
    @NotNull
    @Builder.Default
    private Property<FetchType> fetchType = Property.ofValue(FetchType.FETCH);

    @Schema(title = "Label selector", description = "Optional STACKIT label selector filter.")
    @PluginProperty(group = "advanced")
    private Property<String> labelSelector;

    protected FetchType renderedFetchType(RunContext runContext) throws IllegalVariableEvaluationException {
        if (fetchType == null) {
            return FetchType.FETCH;
        }
        return runContext.render(fetchType).as(FetchType.class).orElse(FetchType.FETCH);
    }

    protected String labelSelector(RunContext runContext) throws IllegalVariableEvaluationException {
        return optional(runContext, labelSelector);
    }
}