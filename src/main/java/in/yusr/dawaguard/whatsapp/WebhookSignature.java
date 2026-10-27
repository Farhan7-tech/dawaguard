package in.yusr.dawaguard.whatsapp;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Meta signs every webhook body with the app secret (X-Hub-Signature-256: sha256=<hex HMAC>).
 * Without this check anyone who finds the URL could make DawaGuard spend credits or message strangers.
 */
final class WebhookSignature {

    private static final String PREFIX = "sha256=";

    private WebhookSignature() {
    }

    static boolean valid(String appSecret, byte[] body, String header) {
        if (appSecret == null || appSecret.isBlank() || header == null || !header.startsWith(PREFIX)) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = HexFormat.of().formatHex(mac.doFinal(body)).getBytes(StandardCharsets.US_ASCII);
            byte[] given = header.substring(PREFIX.length()).toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII);
            return MessageDigest.isEqual(expected, given);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Constant-time comparison for shared tokens (the webhook verify token, the demo token). */
    static boolean sameToken(String expected, String given) {
        return expected != null && !expected.isBlank() && given != null
                && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), given.getBytes(StandardCharsets.UTF_8));
    }
}
