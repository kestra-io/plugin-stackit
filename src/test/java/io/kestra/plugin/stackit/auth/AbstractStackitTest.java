package io.kestra.plugin.stackit.auth;

import cloud.stackit.sdk.core.exception.ApiException;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

class AbstractStackitTest {
    @Test
    void translatesForbiddenIntoAPermissionMessage() {
        Exception translated = AbstractStackit.translate(new ApiException(403, "forbidden"));

        assertThat(translated.getMessage(), containsString("403"));
        assertThat(translated.getMessage(), containsString("Check the service account permissions"));
    }

    @Test
    void keepsTheStatusForOtherApiErrors() {
        Exception translated = AbstractStackit.translate(new ApiException(404, "missing"));

        assertThat(translated.getMessage(), containsString("404"));
        assertThat(translated.getMessage(), containsString("missing"));
    }
}
