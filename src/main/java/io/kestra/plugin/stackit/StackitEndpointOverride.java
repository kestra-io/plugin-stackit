package io.kestra.plugin.stackit;

/**
 * Optional STACKIT API and token URLs. Used in tests with WireMock; production tasks return {@code null} from
 * {@link AbstractStackit#stackitEndpointOverride()}.
 */
public record StackitEndpointOverride(String customEndpoint, String tokenCustomUrl) {

    public static StackitEndpointOverride wireMockOnPort(int port) {
        String base = "http://localhost:" + port;
        return new StackitEndpointOverride(base, base + "/token");
    }
}
