package io.kestra.plugin.stackit.auth;

/**
 * Keys for worker-level plugin configuration under {@code kestra.plugins.configurations}.
 */
public final class StackitPluginConfiguration {

    public static final String TYPE_ALIAS = "io.kestra.plugin.stackit";

    public static final String CUSTOM_ENDPOINT = "customEndpoint";

    public static final String TOKEN_CUSTOM_URL = "tokenCustomUrl";

    private StackitPluginConfiguration() {
    }
}