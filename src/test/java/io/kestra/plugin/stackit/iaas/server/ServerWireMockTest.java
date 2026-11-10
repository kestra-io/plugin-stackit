package io.kestra.plugin.stackit.iaas.server;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.stackit.StackitTestFixtures;
import io.kestra.plugin.stackit.StackitWireMock;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

@MicronautTest
class ServerWireMockTest {
    private static final String ID = StackitTestFixtures.SERVER_ID.toString();
    private static final String PROJECT = StackitTestFixtures.PROJECT_ID.toString();

    @Inject
    private RunContextFactory runContextFactory;

    private WireMockServer wireMock;

    @BeforeEach
    void startWireMock() {
        wireMock = StackitWireMock.start();
    }

    @AfterEach
    void stopWireMock() {
        wireMock.stop();
    }

    @Test
    void getsListsStartsStopsAndDeletesAServer() throws Exception {
        String server = """
            {"id":"%s","name":"kestra-demo","status":"ACTIVE","powerStatus":"RUNNING","machineType":"g2i.2","availabilityZone":"eu01-1"}
            """.formatted(ID);
        StackitWireMock.stubJson(wireMock, "GET", "/v2/projects/.*/regions/.*/servers/[^/]+$", server);
        StackitWireMock.stubJson(wireMock, "GET", "/v2/projects/.*/regions/.*/servers$", "{\"items\":[" + server + "]}");
        StackitWireMock.stubEmpty(wireMock, "POST", "/v2/projects/.*/regions/.*/servers/[^/]+/start");
        StackitWireMock.stubEmpty(wireMock, "POST", "/v2/projects/.*/regions/.*/servers/[^/]+/stop");
        StackitWireMock.stubEmpty(wireMock, "DELETE", "/v2/projects/.*/regions/.*/servers/[^/]+$");

        Get get = Get.builder()
            .id("get").type(Get.class.getName())
            .projectId(Property.ofValue(PROJECT)).region(Property.ofValue("eu01")).serverId(Property.ofValue(ID))
            .build();
        List list = List.builder()
            .id("list").type(List.class.getName())
            .projectId(Property.ofValue(PROJECT)).region(Property.ofValue("eu01"))
            .build();
        Start start = Start.builder()
            .id("start").type(Start.class.getName())
            .projectId(Property.ofValue(PROJECT)).region(Property.ofValue("eu01")).serverId(Property.ofValue(ID))
            .build();
        Stop stop = Stop.builder()
            .id("stop").type(Stop.class.getName())
            .projectId(Property.ofValue(PROJECT)).region(Property.ofValue("eu01")).serverId(Property.ofValue(ID))
            .build();
        Delete delete = Delete.builder()
            .id("delete").type(Delete.class.getName())
            .projectId(Property.ofValue(PROJECT)).region(Property.ofValue("eu01")).serverId(Property.ofValue(ID))
            .build();

        assertThat(((Get.Output) StackitWireMock.run(runContextFactory, wireMock, get)).getStatus(), is("ACTIVE"));
        assertThat(((List.Output) StackitWireMock.run(runContextFactory, wireMock, list)).getIds(), hasItem(ID));
        assertThat(((Start.Output) StackitWireMock.run(runContextFactory, wireMock, start)).getServerId(), is(ID));
        assertThat(((Stop.Output) StackitWireMock.run(runContextFactory, wireMock, stop)).getServerId(), is(ID));
        assertThat(((Delete.Output) StackitWireMock.run(runContextFactory, wireMock, delete)).getServerId(), is(ID));
    }
}
