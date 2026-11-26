package io.kestra.plugin.stackit;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.kestra.core.models.tasks.Output;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.stackit.auth.AbstractStackit;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

public final class StackitWireMock {

    public static final int WIREMOCK_PORT = 18089;

    private StackitWireMock() {
    }

    public static WireMockServer start() {
        var wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().port(WIREMOCK_PORT));
        wireMock.start();
        WireMock.configureFor("localhost", wireMock.port());
        wireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/token"))
            .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("""
                {"access_token":"wiremock-access-token","token_type":"Bearer","expires_in":3600,"refresh_token":"wiremock-refresh-token"}
                """)));
        return wireMock;
    }

    public static void stubJson(WireMockServer wireMock, String method, String path, String body) {
        wireMock.stubFor(WireMock.request(method, urlPathMatching(path))
            .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(body)));
    }

    public static void stubEmpty(WireMockServer wireMock, String method, String path) {
        wireMock.stubFor(WireMock.request(method, urlPathMatching(path)).willReturn(aResponse().withStatus(202)));
    }

    @SuppressWarnings("unused")
    public static <O extends Output, T extends AbstractStackit & RunnableTask<O>> O run(
        RunContextFactory runContextFactory,
        WireMockServer wireMock,
        T task
    ) throws Exception {
        var runContext = StackitTestRunContext.forTask(runContextFactory, task, Map.of());
        return task.run(runContext);
    }
}