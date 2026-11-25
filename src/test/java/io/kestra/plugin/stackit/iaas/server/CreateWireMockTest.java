package io.kestra.plugin.stackit.iaas.server;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.stackit.StackitTestFixtures;
import io.kestra.plugin.stackit.StackitTestRunContext;
import io.kestra.plugin.stackit.StackitWireMock;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.utils.IdUtils;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KestraTest
class CreateWireMockTest {

    @Inject
    private RunContextFactory runContextFactory;

    private WireMockServer wireMock;

    @BeforeEach
    void startWireMock() {
        wireMock = StackitWireMock.start();
    }

    @AfterEach
    void stopWireMock() {
        if (wireMock != null) {
            wireMock.stop();
        }
    }

    @Test
    void createsServerThroughWireMock() throws Exception {
        stubCreateServerSuccess();

        Create task = baseTask();
        RunContext runContext = StackitTestRunContext.forTask(runContextFactory, task, Map.of());

        Create.Output output = task.run(runContext);

        assertThat(output.getServerId(), is(StackitTestFixtures.SERVER_ID.toString()));
        assertThat(output.getName(), is("kestra-demo"));
        assertThat(output.getStatus(), is("ACTIVE"));
        wireMock.verify(postRequestedFor(urlPathMatching("/v2/projects/.*/regions/.*/servers")));
    }

    @Test
    void rejectsNonUuidProjectId() throws Exception {
        stubCreateServerSuccess();

        Create task = Create.builder()
            .id(IdUtils.create())
            .type(Create.class.getName())
            .serviceAccountKey(Property.ofValue(StackitTestFixtures.serviceAccountKeyJson()))
            .projectId(Property.ofValue("not-a-uuid"))
            .region(Property.ofValue("eu01"))
            .name(Property.ofValue("kestra-demo"))
            .machineType(Property.ofValue("g2i.2"))
            .availabilityZone(Property.ofValue("eu01-1"))
            .imageId(Property.ofValue(StackitTestFixtures.SERVER_ID.toString()))
            .networkId(Property.ofValue(StackitTestFixtures.SERVER_ID.toString()))
            .bootVolumeSizeGiB(Property.ofValue(32))
            .build();
        RunContext runContext = StackitTestRunContext.forTask(runContextFactory, task, Map.of());

        var failure = assertThrows(IllegalArgumentException.class, () -> task.run(runContext));
        assertThat(failure.getMessage(), containsString("projectId must be a valid UUID"));
    }

    @Test
    void mapsForbiddenToPermissionMessage() throws Exception {
        wireMock.stubFor(WireMock.post(urlPathMatching("/v2/projects/.*/regions/.*/servers"))
            .willReturn(aResponse().withStatus(403).withBody("{\"message\":\"forbidden\"}")));

        Create task = baseTask();
        RunContext runContext = StackitTestRunContext.forTask(runContextFactory, task, Map.of());

        Exception failure = assertThrows(Exception.class, () -> task.run(runContext));
        assertThat(failure.getMessage(), containsString("Check the service account permissions"));
    }

    private static Create baseTask() {
        return Create.builder()
            .id(IdUtils.create())
            .type(Create.class.getName())
            .serviceAccountKey(Property.ofValue(StackitTestFixtures.serviceAccountKeyJson()))
            .projectId(Property.ofValue(StackitTestFixtures.PROJECT_ID.toString()))
            .region(Property.ofValue("eu01"))
            .name(Property.ofValue("kestra-demo"))
            .machineType(Property.ofValue("g2i.2"))
            .availabilityZone(Property.ofValue("eu01-1"))
            .imageId(Property.ofValue(StackitTestFixtures.SERVER_ID.toString()))
            .networkId(Property.ofValue(StackitTestFixtures.SERVER_ID.toString()))
            .bootVolumeSizeGiB(Property.ofValue(32))
            .build();
    }

    private void stubCreateServerSuccess() {
        wireMock.stubFor(WireMock.post(urlPathMatching("/v2/projects/.*/regions/.*/servers"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "id": "%s",
                      "name": "kestra-demo",
                      "status": "ACTIVE",
                      "machineType": "g2i.2",
                      "availabilityZone": "eu01-1"
                    }
                    """.formatted(StackitTestFixtures.SERVER_ID))));
    }
}