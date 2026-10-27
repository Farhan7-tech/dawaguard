package in.yusr.dawaguard.whatsapp;

import in.yusr.dawaguard.config.DawaGuardProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class WhatsAppTest {

    private static final String SECRET = "test-app-secret";
    private static final String BODY = """
            {"object":"whatsapp_business_account","entry":[{"id":"1","changes":[{"field":"messages","value":{
            "messaging_product":"whatsapp","messages":[{"from":"919800000000","id":"wamid.A","timestamp":"1",
            "type":"text","text":{"body":"cabinet"}}]}}]}]}""";

    @Test
    void parsesCommandsInEnglishAndHindi() {
        assertThat(Replies.parse("Hindi")).isEqualTo(new Replies.SetLanguage("hi"));
        assertThat(Replies.parse("हिंदी")).isEqualTo(new Replies.SetLanguage("hi"));
        assertThat(Replies.parse(" cabinet ")).isEqualTo(new Replies.ShowCabinet());
        assertThat(Replies.parse("remove 2")).isEqualTo(new Replies.Remove(2));
        assertThat(Replies.parse("hello")).isEqualTo(new Replies.Help());
        assertThat(Replies.parse("STOP")).isEqualTo(new Replies.DeleteMyData());
    }

    @Test
    void findsTheBatchNumberInTypedText() {
        assertThat(Replies.parse("TG25-1814A Pansid-40")).isEqualTo(new Replies.CheckBatch("TG25-1814A", "Pansid-40"));
        assertThat(Replies.parse("Pansid-40 TG25-1814A")).isEqualTo(new Replies.CheckBatch("TG25-1814A", "Pansid-40"));
        assertThat(Replies.parse("batch: AB12 of Dolo 650")).isEqualTo(new Replies.CheckBatch("AB12", "Dolo 650"));
        assertThat(Replies.parse("DG26B047")).isEqualTo(new Replies.CheckBatch("DG26B047", null));
        assertThat(Replies.parse("Paracetamol")).isEqualTo(new Replies.Help());
    }

    @Test
    void detectsHindiFromDevanagari() {
        assertThat(Replies.detectLanguage("नमस्ते")).isEqualTo("hi");
        assertThat(Replies.detectLanguage("namaste")).isEqualTo("en");
        assertThat(Replies.detectLanguage(null)).isEqualTo("en");
    }

    @Test
    void acceptsOnlyBodiesSignedWithTheAppSecret() throws Exception {
        byte[] body = BODY.getBytes(StandardCharsets.UTF_8);
        String good = sign(body, SECRET);
        assertThat(WebhookSignature.valid(SECRET, body, good)).isTrue();
        assertThat(WebhookSignature.valid(SECRET, body, sign(body, "other-secret"))).isFalse();
        assertThat(WebhookSignature.valid(SECRET, "{}".getBytes(StandardCharsets.UTF_8), good)).isFalse();
        assertThat(WebhookSignature.valid(SECRET, body, null)).isFalse();
        assertThat(WebhookSignature.valid("", body, good)).isFalse();
    }

    @Test
    void webhookHandshakeNeedsTheVerifyToken() {
        WhatsAppWebhookController controller = controller(mock(WhatsAppBot.class));
        assertThat(controller.verify("subscribe", "verify-me", "12345").getBody()).isEqualTo("12345");
        assertThat(controller.verify("subscribe", "wrong", "12345").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(controller.verify("subscribe", "verify-me", "<script>").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void webhookPassesSignedMessagesToTheBotAndRejectsUnsignedOnes() throws Exception {
        WhatsAppBot bot = mock(WhatsAppBot.class);
        WhatsAppWebhookController controller = controller(bot);
        byte[] body = BODY.getBytes(StandardCharsets.UTF_8);

        assertThat(controller.receive(body, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(bot, never()).accept(any());

        assertThat(controller.receive(body, sign(body, SECRET)).getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(bot).accept(new WebhookPayload.Message("919800000000", "wamid.A", "text",
                new WebhookPayload.Text("cabinet"), null, null));
    }

    private static WhatsAppWebhookController controller(WhatsAppBot bot) {
        DawaGuardProperties props = new DawaGuardProperties(null, null, null, null,
                new DawaGuardProperties.WhatsApp(true, "https://graph.example", "1", "token", "verify-me", SECRET,
                        "dawaguard_batch_alert", "en"), null);
        return new WhatsAppWebhookController(props, bot, JsonMapper.builder().build());
    }

    private static String sign(byte[] body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
    }
}
