package io.kestra.plugin.stackit.iaas;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kestra.core.models.tasks.common.FetchType;
import io.kestra.core.runners.RunContext;
import lombok.Builder;
import lombok.Getter;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StackitListOutput {

    private static final ObjectMapper JSON = new ObjectMapper();

    private StackitListOutput() {
    }

    @Builder
    @Getter
    public static class Result {
        private final Integer count;
        private final List<String> ids;
        private final List<String> names;
        private final Map<String, Object> row;
        private final List<Map<String, Object>> rows;
        private final URI uri;
        private final Long size;
    }

    public static Result build(
        RunContext runContext,
        FetchType fetchType,
        java.util.List<String> ids,
        java.util.List<String> names
    ) throws Exception {
        int count = ids.size();
        java.util.List<Map<String, Object>> rowMaps = toRows(ids, names);

        return switch (fetchType) {
            case FETCH -> Result.builder()
                .count(count)
                .ids(ids)
                .names(names)
                .rows(rowMaps)
                .size((long) count)
                .build();
            case FETCH_ONE -> Result.builder()
                .count(count)
                .row(rowMaps.isEmpty() ? null : rowMaps.getFirst())
                .size((long) count)
                .build();
            case NONE -> Result.builder()
                .count(count)
                .size((long) count)
                .build();
            case STORE -> {
                File file = runContext.workingDir().createTempFile(".json").toFile();
                JSON.writeValue(file, rowMaps);
                URI uri = runContext.storage().putFile(file);
                yield Result.builder()
                    .count(count)
                    .uri(uri)
                    .size((long) count)
                    .build();
            }
        };
    }

    private static java.util.List<Map<String, Object>> toRows(java.util.List<String> ids, java.util.List<String> names) {
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