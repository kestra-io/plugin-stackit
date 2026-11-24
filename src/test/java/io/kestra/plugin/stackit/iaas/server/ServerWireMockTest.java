package io.kestra.plugin.stackit.iaas.server;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.common.FetchType;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.IdUtils;
import io.kestra.plugin.stackit.StackitTestFixtures;
import io.kestra.plugin.stackit.StackitWireMock;
import io.kestra.plugin.stackit.iaas.StackitListOutput;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@KestraTest
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
            .id(IdUtils.create()).type(Get.class.getName())
            .serviceAccountKey(Property.ofValue(StackitTestFixtures.serviceAccountKeyJson())).projectId(Property.ofValue(PROJECT)).region(Property.ofValue("eu01")).serverId(Property.ofValue(ID))
            .build();
        List list = List.builder()
            .id(IdUtils.create()).type(List.class.getName())
            .serviceAccountKey(Property.ofValue(StackitTestFixtures.serviceAccountKeyJson())).projectId(Property.ofValue(PROJECT)).region(Property.ofValue("eu01"))
            .build();
        Start start = Start.builder()
            .id(IdUtils.create()).type(Start.class.getName())
            .serviceAccountKey(Property.ofValue(StackitTestFixtures.serviceAccountKeyJson())).projectId(Property.ofValue(PROJECT)).region(Property.ofValue("eu01")).serverId(Property.ofValue(ID))
            .build();
        Stop stop = Stop.builder()
            .id(IdUtils.create()).type(Stop.class.getName())
            .serviceAccountKey(Property.ofValue(StackitTestFixtures.serviceAccountKeyJson())).projectId(Property.ofValue(PROJECT)).region(Property.ofValue("eu01")).serverId(Property.ofValue(ID))
            .build();
        Delete delete = Delete.builder()
            .id(IdUtils.create()).type(Delete.class.getName())
            .serviceAccountKey(Property.ofValue(StackitTestFixtures.serviceAccountKeyJson())).projectId(Property.ofValue(PROJECT)).region(Property.ofValue("eu01")).serverId(Property.ofValue(ID))
            .build();

        assertThat(StackitWireMock.run(runContextFactory, wireMock, get).getStatus(), is("ACTIVE"));
        var listOutput = StackitWireMock.run(runContextFactory, wireMock, list);
        assertThat(listOutput.getRows().stream().map(row -> row.get("id")).toList(), hasItem(ID));
        assertThat(StackitWireMock.run(runContextFactory, wireMock, start), nullValue());
        assertThat(StackitWireMock.run(runContextFactory, wireMock, stop), nullValue());
        assertThat(StackitWireMock.run(runContextFactory, wireMock, delete), nullValue());
    }

    @Test
    void fetchOneReturnsFirstRowOnly() throws Exception {
        String server = """
            {"id":"%s","name":"kestra-demo","status":"ACTIVE","powerStatus":"RUNNING","machineType":"g2i.2","availabilityZone":"eu01-1"}
            """.formatted(ID);
        StackitWireMock.stubJson(wireMock, "GET", "/v2/projects/.*/regions/.*/servers$", "{\"items\":[" + server + "]}");

        List list = List.builder()
            .id(IdUtils.create()).type(List.class.getName())
            .serviceAccountKey(Property.ofValue(StackitTestFixtures.serviceAccountKeyJson()))
            .projectId(Property.ofValue(PROJECT))
            .region(Property.ofValue("eu01"))
            .fetchType(Property.ofValue(FetchType.FETCH_ONE))
            .build();

        StackitListOutput.ListOutput output = StackitWireMock.run(runContextFactory, wireMock, list);
        assertThat(output.getSize(), is(1L));
        assertThat(output.getRow().get("id"), is(ID));
        assertThat(output.getRows(), nullValue());
        assertThat(output.getUri(), nullValue());
    }

    @Test
    void storeWritesListToInternalStorage() throws Exception {
        String server = """
            {"id":"%s","name":"kestra-demo","status":"ACTIVE","powerStatus":"RUNNING","machineType":"g2i.2","availabilityZone":"eu01-1"}
            """.formatted(ID);
        StackitWireMock.stubJson(wireMock, "GET", "/v2/projects/.*/regions/.*/servers$", "{\"items\":[" + server + "]}");

        List list = List.builder()
            .id(IdUtils.create()).type(List.class.getName())
            .serviceAccountKey(Property.ofValue(StackitTestFixtures.serviceAccountKeyJson()))
            .projectId(Property.ofValue(PROJECT))
            .region(Property.ofValue("eu01"))
            .fetchType(Property.ofValue(FetchType.STORE))
            .build();

        StackitListOutput.ListOutput output = StackitWireMock.run(runContextFactory, wireMock, list);
        assertThat(output.getSize(), is(1L));
        assertThat(output.getUri(), notNullValue());
        assertThat(output.getRows(), nullValue());
        assertThat(output.getRow(), nullValue());
    }
}