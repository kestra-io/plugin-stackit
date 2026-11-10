package io.kestra.plugin.stackit.iaas.server;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.stackit.TestEnvironment;
import io.kestra.plugin.stackit.auth.StackitEndpointOverride;
import io.kestra.plugin.stackit.StackitTestFixtures;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
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

@MicronautTest
class CreateWireMockTest {

    @Inject
    private RunContextFactory runContextFactory;

    private WireMockServer wireMock;

    @BeforeEach
    void startWireMock() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
        WireMock.configureFor("localhost", wireMock.port());
        TestEnvironment.set("STACKIT_SERVICE_ACCOUNT_KEY", StackitTestFixtures.serviceAccountKeyJson());
    }

    @AfterEach
    void stopWireMock() {
        if (wireMock != null) {
            wireMock.stop();
        }
    }

    @Test
    void createsServerThroughWireMock() throws Exception {
        stubTokenSuccess();
        stubCreateServerSuccess();

        Create task = baseTask();
        task.applyEndpointOverrideForTests(StackitEndpointOverride.wireMockOnPort(wireMock.port()));
        RunContext runContext = TestsUtils.mockRunContext(runContextFactory, task, Map.of());

        Create.Output output = task.run(runContext);

        assertThat(output.getServerId(), is(StackitTestFixtures.SERVER_ID.toString()));
        assertThat(output.getName(), is("kestra-demo"));
        assertThat(output.getStatus(), is("ACTIVE"));
        wireMock.verify(postRequestedFor(urlPathMatching("/v2/projects/.*/regions/.*/servers")));
    }

    @Test
    void mapsForbiddenToPermissionMessage() throws Exception {
        stubTokenSuccess();
        wireMock.stubFor(WireMock.post(urlPathMatching("/v2/projects/.*/regions/.*/servers"))
            .willReturn(aResponse().withStatus(403).withBody("{\"message\":\"forbidden\"}")));

        Create task = baseTask();
        task.applyEndpointOverrideForTests(StackitEndpointOverride.wireMockOnPort(wireMock.port()));
        RunContext runContext = TestsUtils.mockRunContext(runContextFactory, task, Map.of());

        Exception failure = assertThrows(Exception.class, () -> task.run(runContext));
        assertThat(failure.getMessage(), containsString("Check the service account permissions"));
    }

    private static Create baseTask() {
        return Create.builder()
            .id("wiremock-create")
            .type(Create.class.getName())
            .projectId(Property.ofValue(StackitTestFixtures.PROJECT_ID.toString()))
            .region(Property.ofValue("eu01"))
            .name(Property.ofValue("kestra-demo"))
            .machineType(Property.ofValue("g2i.2"))
            .availabilityZone(Property.ofValue("eu01-1"))
            .imageId(Property.ofValue(StackitTestFixtures.SERVER_ID.toString()))
            .bootVolumeSizeGiB(Property.ofValue(32))
            .build();
    }

    private void stubTokenSuccess() {
        wireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/token"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "access_token": "wiremock-access-token",
                      "token_type": "Bearer",
                      "expires_in": 3600,
                      "refresh_token": "wiremock-refresh-token"
                    }
                    """)));
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
