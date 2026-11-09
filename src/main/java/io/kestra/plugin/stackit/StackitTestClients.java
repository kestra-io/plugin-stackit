package io.kestra.plugin.stackit;

import cloud.stackit.sdk.core.config.CoreConfiguration;
import cloud.stackit.sdk.iaas.v2api.api.IaasApi;

import java.io.IOException;

/**
 * Builds STACKIT SDK clients. Production code uses {@link AbstractStackit#iaasClient(io.kestra.core.runners.RunContext)};
 * tests point the SDK at WireMock via {@link StackitEndpointOverride}.
 */
public final class StackitTestClients {

    private StackitTestClients() {
    }

    static AbstractStackit.IaasClient buildIaasClient(
        String serviceAccountKeyJson,
        StackitEndpointOverride override
    ) throws IOException {
        CoreConfiguration configuration = new CoreConfiguration().serviceAccountKey(serviceAccountKeyJson);
        if (override != null) {
            configuration.customEndpoint(override.customEndpoint());
            configuration.tokenCustomUrl(override.tokenCustomUrl());
        }

        IaasApi api = new IaasApi(configuration);
        api.getApiClient().setConnectTimeout(AbstractStackit.DEFAULT_TIMEOUT_MILLIS);
        api.getApiClient().setReadTimeout(AbstractStackit.DEFAULT_TIMEOUT_MILLIS);
        api.getApiClient().setWriteTimeout(AbstractStackit.DEFAULT_TIMEOUT_MILLIS);
        return new AbstractStackit.IaasClient(api);
    }
}
