package io.kestra.plugin.stackit.iaas.network;

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
class NetworkWireMockTest {
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
    void createsListsAndDeletesANetwork() throws Exception {
        String network = """
            {"id":"%s","name":"kestra-network","status":"AVAILABLE"}
            """.formatted(ID);
        StackitWireMock.stubJson(wireMock, "POST", "/v2/projects/.*/regions/.*/networks$", network);
        StackitWireMock.stubJson(wireMock, "GET", "/v2/projects/.*/regions/.*/networks$", "{\"items\":[" + network + "]}");
        StackitWireMock.stubEmpty(wireMock, "DELETE", "/v2/projects/.*/regions/.*/networks/[^/]+$");

        Create create = Create.builder()
            .id("create").type(Create.class.getName())
            .projectId(Property.ofValue(PROJECT)).region(Property.ofValue("eu01")).name(Property.ofValue("kestra-network"))
            .build();
        List list = List.builder()
            .id("list").type(List.class.getName())
            .projectId(Property.ofValue(PROJECT)).region(Property.ofValue("eu01"))
            .build();
        Delete delete = Delete.builder()
            .id("delete").type(Delete.class.getName())
            .projectId(Property.ofValue(PROJECT)).region(Property.ofValue("eu01")).networkId(Property.ofValue(ID))
            .build();

        assertThat(((Create.Output) StackitWireMock.run(runContextFactory, wireMock, create)).getNetworkId(), is(ID));
        assertThat(((List.Output) StackitWireMock.run(runContextFactory, wireMock, list)).getNames(), hasItem("kestra-network"));
        assertThat(((Delete.Output) StackitWireMock.run(runContextFactory, wireMock, delete)).getNetworkId(), is(ID));
    }
}
