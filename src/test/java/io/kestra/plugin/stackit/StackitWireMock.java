package io.kestra.plugin.stackit;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.stackit.auth.AbstractStackit;
import io.kestra.plugin.stackit.auth.StackitEndpointOverride;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

public final class StackitWireMock {
    private StackitWireMock() {
    }

    public static WireMockServer start() {
        WireMockServer wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
        WireMock.configureFor("localhost", wireMock.port());
        TestEnvironment.set("STACKIT_SERVICE_ACCOUNT_KEY", StackitTestFixtures.serviceAccountKeyJson());
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

    public static Object run(RunContextFactory runContextFactory, WireMockServer wireMock, AbstractStackit task) throws Exception {
        task.applyEndpointOverrideForTests(StackitEndpointOverride.wireMockOnPort(wireMock.port()));
        try {
            return task.getClass().getMethod("run", io.kestra.core.runners.RunContext.class)
                .invoke(task, TestsUtils.mockRunContext(runContextFactory, task, Map.of()));
        } catch (java.lang.reflect.InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception nested) {
                throw nested;
            }
            throw exception;
        }
    }
}
