package io.kestra.plugin.stackit.iaas.volume;

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
class VolumeWireMockTest {
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
    void createsListsAndDeletesAVolume() throws Exception {
        String volume = """
            {"id":"%s","name":"kestra-data","size":10,"status":"AVAILABLE","availabilityZone":"eu01-1"}
            """.formatted(ID);
        StackitWireMock.stubJson(wireMock, "POST", "/v2/projects/.*/regions/.*/volumes$", volume);
        StackitWireMock.stubJson(wireMock, "GET", "/v2/projects/.*/regions/.*/volumes$", "{\"items\":[" + volume + "]}");
        StackitWireMock.stubEmpty(wireMock, "DELETE", "/v2/projects/.*/regions/.*/volumes/[^/]+$");

        Create create = Create.builder()
            .id("create").type(Create.class.getName())
            .serviceAccountKey(Property.ofValue(StackitTestFixtures.serviceAccountKeyJson())).projectId(Property.ofValue(PROJECT)).region(Property.ofValue("eu01"))
            .name(Property.ofValue("kestra-data")).sizeGiB(Property.ofValue(10)).availabilityZone(Property.ofValue("eu01-1"))
            .build();
        List list = List.builder()
            .id("list").type(List.class.getName())
            .serviceAccountKey(Property.ofValue(StackitTestFixtures.serviceAccountKeyJson())).projectId(Property.ofValue(PROJECT)).region(Property.ofValue("eu01"))
            .build();
        Delete delete = Delete.builder()
            .id("delete").type(Delete.class.getName())
            .serviceAccountKey(Property.ofValue(StackitTestFixtures.serviceAccountKeyJson())).projectId(Property.ofValue(PROJECT)).region(Property.ofValue("eu01")).volumeId(Property.ofValue(ID))
            .build();

        assertThat(((Create.Output) StackitWireMock.run(runContextFactory, wireMock, create)).getVolumeId(), is(ID));
        assertThat(((List.Output) StackitWireMock.run(runContextFactory, wireMock, list)).getNames(), hasItem("kestra-data"));
        assertThat(((Delete.Output) StackitWireMock.run(runContextFactory, wireMock, delete)).getVolumeId(), is(ID));
    }
}
