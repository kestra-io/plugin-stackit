package io.kestra.plugin.stackit.iaas.server;

import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.IdUtils;
import io.kestra.core.utils.TestsUtils;
import io.kestra.core.junit.annotations.KestraTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Run only when real STACKIT credentials are available:
 * {@code STACKIT_LIVE_TEST=true STACKIT_SERVICE_ACCOUNT_KEY='...' STACKIT_PROJECT_ID='...' STACKIT_IMAGE_ID='...' STACKIT_NETWORK_ID='...' ./gradlew test --tests CreateLiveTest}
 */
@KestraTest
@EnabledIfEnvironmentVariable(named = "STACKIT_LIVE_TEST", matches = "true")
class CreateLiveTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void createsServerInStackit() throws Exception {
        var serviceAccountKey = requiredEnv("STACKIT_SERVICE_ACCOUNT_KEY");
        var projectId = requiredEnv("STACKIT_PROJECT_ID");
        var region = System.getenv().getOrDefault("STACKIT_REGION", "eu01");
        var zone = System.getenv().getOrDefault("STACKIT_AVAILABILITY_ZONE", "%s-1".formatted(region));
        var networkId = requiredEnv("STACKIT_NETWORK_ID");

        var task = Create.builder()
            .id(IdUtils.create())
            .type(Create.class.getName())
            .serviceAccountKey(Property.ofValue(serviceAccountKey))
            .projectId(Property.ofValue(projectId))
            .region(Property.ofValue(region))
            .name(Property.ofValue("kestra-live-%s".formatted(IdUtils.create().substring(0, 8))))
            .machineType(Property.ofValue(System.getenv().getOrDefault("STACKIT_MACHINE_TYPE", "g2i.2")))
            .availabilityZone(Property.ofValue(zone))
            .imageId(Property.ofValue(requiredEnv("STACKIT_IMAGE_ID")))
            .networkId(Property.ofValue(networkId))
            .build();

        String serverId = null;
        try {
            var runContext = TestsUtils.mockRunContext(runContextFactory, task, Map.of());
            var output = task.run(runContext);
            serverId = output.getServerId();
            assertThat(serverId, notNullValue());
            UUID.fromString(serverId);
        } finally {
            if (serverId != null) {
                var delete = Delete.builder()
                    .id(IdUtils.create())
                    .type(Delete.class.getName())
                    .serviceAccountKey(Property.ofValue(serviceAccountKey))
                    .projectId(Property.ofValue(projectId))
                    .region(Property.ofValue(region))
                    .serverId(Property.ofValue(serverId))
                    .build();
                try {
                    delete.run(TestsUtils.mockRunContext(runContextFactory, delete, Map.of()));
                } catch (Exception exception) {
                    throw new IllegalStateException("Failed to delete live test server %s".formatted(serverId), exception);
                }
            }
        }
    }

    private static String requiredEnv(String name) {
        var value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("%s must be set for CreateLiveTest.".formatted(name));
        }
        return value;
    }
}
