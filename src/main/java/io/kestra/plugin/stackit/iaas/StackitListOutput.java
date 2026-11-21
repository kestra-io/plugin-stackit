package io.kestra.plugin.stackit.iaas;

import io.kestra.core.models.tasks.Output;
import io.kestra.core.models.tasks.common.FetchType;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.FileSerde;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StackitListOutput {

    private StackitListOutput() {
    }

    @Builder
    @Getter
    public static class ListOutput implements Output {
        @Schema(title = "Rows", description = "Set when fetchType is FETCH.")
        private final List<Map<String, Object>> rows;
        @Schema(title = "First row", description = "Set when fetchType is FETCH_ONE.")
        private final Map<String, Object> row;
        @Schema(title = "Storage URI", description = "Set when fetchType is STORE.")
        private final URI uri;
        @Schema(title = "Size", description = "Number of items listed.")
        private final Long size;
    }

    public static ListOutput build(
        RunContext runContext,
        FetchType fetchType,
        List<String> ids,
        List<String> names
    ) throws Exception {
        long count = ids.size();
        var rowMaps = toRows(ids, names);

        return switch (fetchType) {
            case FETCH -> ListOutput.builder()
                .rows(rowMaps)
                .size(count)
                .build();
            case FETCH_ONE -> ListOutput.builder()
                .row(rowMaps.isEmpty() ? null : rowMaps.getFirst())
                .size(count)
                .build();
            case NONE -> ListOutput.builder()
                .size(count)
                .build();
            case STORE -> {
                File file = runContext.workingDir().createTempFile(".ion").toFile();
                try (var output = new BufferedOutputStream(new FileOutputStream(file), FileSerde.BUFFER_SIZE)) {
                    for (Map<String, Object> row : rowMaps) {
                        FileSerde.write(output, row);
                    }
                }
                URI uri = runContext.storage().putFile(file);
                yield ListOutput.builder()
                    .uri(uri)
                    .size(count)
                    .build();
            }
        };
    }

    private static List<Map<String, Object>> toRows(List<String> ids, List<String> names) {
        return java.util.stream.IntStream.range(0, ids.size())
            .mapToObj(index -> {
                Map<String, Object> row = new HashMap<>();
                row.put("id", ids.get(index));
                row.put("name", names.get(index));
                return row;
            })
            .toList();
    }
}