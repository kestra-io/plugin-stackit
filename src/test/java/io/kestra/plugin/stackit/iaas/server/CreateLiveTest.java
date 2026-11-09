package io.kestra.plugin.stackit.iaas.server;

import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.IdUtils;
import io.kestra.core.utils.TestsUtils;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Run only when real STACKIT credentials are available:
 * {@code STACKIT_LIVE_TEST=true STACKIT_SERVICE_ACCOUNT_KEY='...' STACKIT_PROJECT_ID='...' ./gradlew test --tests CreateLiveTest}
 */
@MicronautTest
@EnabledIfEnvironmentVariable(named = "STACKIT_LIVE_TEST", matches = "true")
class CreateLiveTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void createsServerInStackit() throws Exception {
        String serviceAccountKey = requiredEnv("STACKIT_SERVICE_ACCOUNT_KEY");
        String projectId = requiredEnv("STACKIT_PROJECT_ID");
        String region = System.getenv().getOrDefault("STACKIT_REGION", "eu01");
        String zone = System.getenv().getOrDefault("STACKIT_AVAILABILITY_ZONE", region + "-1");

        Create task = Create.builder()
            .id("live-create")
            .type(Create.class.getName())
            .serviceAccountKey(Property.ofValue(serviceAccountKey))
            .projectId(Property.ofValue(projectId))
            .region(Property.ofValue(region))
            .name(Property.ofValue("kestra-live-" + IdUtils.create().substring(0, 8)))
            .machineType(Property.ofValue(System.getenv().getOrDefault("STACKIT_MACHINE_TYPE", "g2i.2")))
            .availabilityZone(Property.ofValue(zone))
            .build();

        RunContext runContext = TestsUtils.mockRunContext(runContextFactory, task, Map.of());
        Create.Output output = task.run(runContext);

        assertThat(output.getServerId(), notNullValue());
        UUID.fromString(output.getServerId());
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be set for CreateLiveTest.");
        }
        return value;
    }
}
