package io.kestra.plugin.stackit.auth;

/**
 * Test-only API and token URLs. Production leaves this unset so the SDK uses STACKIT's public hosts.
 */
public record StackitEndpointOverride(String customEndpoint, String tokenCustomUrl) {

    public static StackitEndpointOverride wireMockOnPort(int port) {
        String base = "http://localhost:" + port;
        return new StackitEndpointOverride(base, base + "/token");
    }
}
