package in.yusr.dawaguard.whatsapp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import in.yusr.dawaguard.config.DawaGuardProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Sends messages and fetches photos through Meta's WhatsApp Cloud API (Graph API). */
@Component
public class WhatsAppClient {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppClient.class);
    /** WhatsApp's own cap on a text message body. */
    private static final int MAX_TEXT = 4096;

    private final DawaGuardProperties.WhatsApp config;
    private final RestClient http;

    public WhatsAppClient(DawaGuardProperties props, RestClient.Builder builder) {
        this.config = props.whatsapp();
        this.http = builder
                .baseUrl(config.graphUrl())
                .defaultHeader("Authorization", "Bearer " + config.accessToken())
                .build();
    }

    public boolean canSend() {
        return config.canSend();
    }

    public void sendText(String to, String body) {
        String text = body.length() > MAX_TEXT ? body.substring(0, MAX_TEXT - 1) + "…" : body;
        send(Map.of("messaging_product", "whatsapp", "recipient_type", "individual", "to", to,
                "type", "text", "text", Map.of("preview_url", false, "body", text)));
    }

    /** An approved template, the only kind of message allowed outside the 24-hour reply window. */
    public void sendTemplate(String to, String name, String language, List<String> bodyParameters) {
        List<Map<String, String>> parameters = bodyParameters.stream()
                .map(p -> Map.of("type", "text", "text", p))
                .toList();
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("name", name);
        template.put("language", Map.of("code", language));
        template.put("components", List.of(Map.of("type", "body", "parameters", parameters)));
        send(Map.of("messaging_product", "whatsapp", "to", to, "type", "template", "template", template));
    }

    /** Blue ticks plus "typing…", so the family knows a check is under way. Best effort. */
    public void markReadAndShowTyping(String messageId) {
        try {
            send(Map.of("messaging_product", "whatsapp", "status", "read", "message_id", messageId,
                    "typing_indicator", Map.of("type", "text")));
        } catch (RestClientException e) {
            log.debug("Could not mark message read: {}", e.getMessage());
        }
    }

    /** Downloads a photo the user sent. Media URLs are short-lived and need the access token. */
    public Media download(String mediaId, long maxBytes) {
        MediaInfo info = http.get().uri("/{id}", mediaId).retrieve().body(MediaInfo.class);
        if (info == null || info.url() == null) {
            throw new IllegalStateException("No URL for media " + mediaId);
        }
        if (info.fileSize() != null && info.fileSize() > maxBytes) {
            throw new MediaTooLargeException();
        }
        byte[] bytes = http.get().uri(URI.create(info.url())).retrieve().body(byte[].class);
        if (bytes == null || bytes.length > maxBytes) {
            throw new MediaTooLargeException();
        }
        return new Media(bytes, info.mimeType());
    }

    private void send(Map<String, Object> body) {
        http.post()
                .uri("/{phoneNumberId}/messages", config.phoneNumberId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    public record Media(byte[] bytes, String mimeType) {
    }

    public static class MediaTooLargeException extends RuntimeException {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MediaInfo(String url, @JsonProperty("mime_type") String mimeType, @JsonProperty("file_size") Long fileSize) {
    }
}
