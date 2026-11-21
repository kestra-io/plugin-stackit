package io.kestra.plugin.stackit;

import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.DefaultRunContext;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.TestsUtils;

import java.util.Map;

public final class StackitTestRunContext {

    private StackitTestRunContext() {
    }

    public static RunContext forTask(RunContextFactory runContextFactory, Task task, Map<String, Object> inputs) {
        DefaultRunContext runContext = (DefaultRunContext) TestsUtils.mockRunContext(runContextFactory, task, inputs);
        return runContext.cloneForPlugin(task);
    }
}