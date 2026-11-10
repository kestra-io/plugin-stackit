package io.kestra.plugin.stackit;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.UUID;

public final class StackitTestFixtures {

    public static final UUID PROJECT_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    public static final UUID SERVER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final String SERVICE_ACCOUNT_KEY_JSON = buildServiceAccountKeyJson();

    private StackitTestFixtures() {
    }

    public static String serviceAccountKeyJson() {
        return SERVICE_ACCOUNT_KEY_JSON;
    }

    private static String buildServiceAccountKeyJson() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            String privateKeyPem = toPem(keyPair.getPrivate().getEncoded());
            String escapedPem = privateKeyPem.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
            return """
                {
                  "id": "test-key-id",
                  "publicKey": "test-public-key",
                  "createdAt": "2023-08-24T14:15:22Z",
                  "validUntil": "2099-08-24T14:15:22Z",
                  "keyType": "USER_MANAGED",
                  "keyOrigin": "USER_PROVIDED",
                  "keyAlgorithm": "RSA_2048",
                  "active": true,
                  "credentials": {
                    "kid": "test-kid",
                    "iss": "test@sa.stackit.cloud",
                    "sub": "test-key-id",
                    "aud": "test-audience",
                    "privateKey": "%s"
                  }
                }
                """.formatted(escapedPem);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not build test service account key", exception);
        }
    }

    private static String toPem(byte[] pkcs8) {
        String body = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(pkcs8);
        return "-----BEGIN PRIVATE KEY-----\n" + body + "\n-----END PRIVATE KEY-----";
    }
}
