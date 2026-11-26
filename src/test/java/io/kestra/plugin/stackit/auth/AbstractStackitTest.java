package io.kestra.plugin.stackit.auth;

import cloud.stackit.sdk.core.exception.ApiException;
import io.kestra.plugin.stackit.StackitTestFixtures;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AbstractStackitTest {
    @Test
    void translatesForbiddenIntoAPermissionMessage() {
        var translated = AbstractStackit.translate(new ApiException(403, "forbidden"));

        assertThat(translated.getMessage(), containsString("403"));
        assertThat(translated.getMessage(), containsString("Check the service account permissions"));
    }

    @Test
    void keepsTheStatusForOtherApiErrors() {
        var translated = AbstractStackit.translate(new ApiException(404, "missing"));

        assertThat(translated.getMessage(), containsString("404"));
        assertThat(translated.getMessage(), containsString("missing"));
    }

    @Test
    void acceptsServiceAccountKeyWithEmbeddedPrivateKey() {
        assertDoesNotThrow(() -> AbstractStackit.requireEmbeddedPrivateKey(StackitTestFixtures.serviceAccountKeyJson()));
    }

    @Test
    void rejectsServiceAccountKeyWithoutPrivateKey() {
        var exception = assertThrows(
            IllegalArgumentException.class,
            () -> AbstractStackit.requireEmbeddedPrivateKey("{\"id\":\"key\",\"credentials\":{}}")
        );

        assertThat(exception.getMessage(), containsString("credentials.privateKey"));
        assertThat(exception.getMessage(), containsString("STACKIT portal"));
    }
}
