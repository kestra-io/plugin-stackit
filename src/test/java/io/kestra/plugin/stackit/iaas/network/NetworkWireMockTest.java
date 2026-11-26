package io.kestra.plugin.stackit.iaas.network;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.IdUtils;
import io.kestra.plugin.stackit.StackitTestFixtures;
import io.kestra.plugin.stackit.StackitWireMock;
import io.kestra.plugin.stackit.iaas.StackitListOutput;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@KestraTest
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
        var network = """
            {"id":"%s","name":"kestra-network","status":"AVAILABLE"}
            """.formatted(ID);
        StackitWireMock.stubJson(wireMock, "POST", "/v2/projects/.*/regions/.*/networks$", network);
        StackitWireMock.stubJson(wireMock, "GET", "/v2/projects/.*/regions/.*/networks$", "{\"items\":[%s]}".formatted(network));
        StackitWireMock.stubEmpty(wireMock, "DELETE", "/v2/projects/.*/regions/.*/networks/[^/]+$");

        var create = Create.builder()
            .id(IdUtils.create()).type(Create.class.getName())
            .serviceAccountKey(Property.ofValue(StackitTestFixtures.serviceAccountKeyJson())).projectId(Property.ofValue(PROJECT)).region(Property.ofValue("eu01")).name(Property.ofValue("kestra-network"))
            .build();
        var list = List.builder()
            .id(IdUtils.create()).type(List.class.getName())
            .serviceAccountKey(Property.ofValue(StackitTestFixtures.serviceAccountKeyJson())).projectId(Property.ofValue(PROJECT)).region(Property.ofValue("eu01"))
            .build();
        var delete = Delete.builder()
            .id(IdUtils.create()).type(Delete.class.getName())
            .serviceAccountKey(Property.ofValue(StackitTestFixtures.serviceAccountKeyJson())).projectId(Property.ofValue(PROJECT)).region(Property.ofValue("eu01")).networkId(Property.ofValue(ID))
            .build();

        assertThat(((Create.Output) StackitWireMock.run(runContextFactory, wireMock, create)).getNetworkId(), is(ID));
        var listOutput = (StackitListOutput.ListOutput) StackitWireMock.run(runContextFactory, wireMock, list);
        assertThat(listOutput.getRows().stream().map(row -> row.get("name")).toList(), hasItem("kestra-network"));
        assertThat(StackitWireMock.run(runContextFactory, wireMock, delete), nullValue());
        wireMock.verify(deleteRequestedFor(urlPathMatching("/v2/projects/.*/regions/.*/networks/%s".formatted(ID))));
    }
}
