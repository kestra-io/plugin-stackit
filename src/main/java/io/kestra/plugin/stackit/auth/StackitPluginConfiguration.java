package io.kestra.plugin.stackit.auth;

/**
 * Keys for worker-level plugin configuration under {@code kestra.plugins.configurations}.
 * Use each STACKIT task's fully qualified type as {@code type} (repeat the same values for every task type you use).
 */
public final class StackitPluginConfiguration {

    public static final String CUSTOM_ENDPOINT = "customEndpoint";

    public static final String TOKEN_CUSTOM_URL = "tokenCustomUrl";

    private StackitPluginConfiguration() {
    }
}