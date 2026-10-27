package in.yusr.dawaguard.whatsapp;

import in.yusr.dawaguard.config.DawaGuardProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

/** Meta's webhook for the WhatsApp Business number. Registered only when WHATSAPP_ENABLED=true. */
@RestController
@RequestMapping("/webhook/whatsapp")
@ConditionalOnBooleanProperty("dawaguard.whatsapp.enabled")
public class WhatsAppWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final DawaGuardProperties.WhatsApp config;
    private final WhatsAppBot bot;
    private final JsonMapper json;

    public WhatsAppWebhookController(DawaGuardProperties props, WhatsAppBot bot, JsonMapper json) {
        this.config = props.whatsapp();
        this.bot = bot;
        this.json = json;
    }

    /** The one-time handshake when the webhook URL is saved in the Meta app dashboard. */
    @GetMapping
    public ResponseEntity<String> verify(@RequestParam(name = "hub.mode", required = false) String mode,
                                         @RequestParam(name = "hub.verify_token", required = false) String token,
                                         @RequestParam(name = "hub.challenge", required = false) String challenge) {
        if ("subscribe".equals(mode) && WebhookSignature.sameToken(config.verifyToken(), token)
                && challenge != null && challenge.matches("[A-Za-z0-9._-]{1,100}")) {
            return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(challenge);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody byte[] body,
                                        @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature) {
        if (!WebhookSignature.valid(config.appSecret(), body, signature)) {
            log.warn("Rejected a WhatsApp webhook with a missing or wrong signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            WebhookPayload payload = json.readValue(body, WebhookPayload.class);
            if (payload.entry() != null) {
                // entry.id is the WhatsApp Business Account the message arrived on (templates live under it).
                payload.entry().forEach(e -> log.info("WhatsApp webhook from business account {}", e.id()));
            }
            payload.messages().forEach(bot::accept);
        } catch (RuntimeException e) {
            // Answer 200 anyway: an error makes Meta redeliver the same unreadable body for days.
            log.warn("Could not read a WhatsApp webhook: {}", e.getMessage());
        }
        return ResponseEntity.ok().build();
    }
}
