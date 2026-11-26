package io.kestra.plugin.stackit.iaas;

import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.common.FetchType;
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
        description = """
            FETCH returns rows in outputs. FETCH_ONE returns the first item only.
            STORE writes all items to internal storage as ION and returns a URI. NONE returns only the size.
            """
    )
    @PluginProperty(group = "advanced")
    @NotNull
    @Builder.Default
    private Property<FetchType> fetchType = Property.ofValue(FetchType.FETCH);

    @Schema(
        title = "Label selector",
        description = """
            Optional STACKIT label selector filter.
            """
    )
    @PluginProperty(group = "advanced")
    private Property<String> labelSelector;
}