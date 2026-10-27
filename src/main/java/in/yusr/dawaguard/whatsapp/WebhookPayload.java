package in.yusr.dawaguard.whatsapp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.stream.Stream;

/** The parts of Meta's webhook body DawaGuard uses. Status updates (sent, delivered, read) are ignored. */
@JsonIgnoreProperties(ignoreUnknown = true)
record WebhookPayload(String object, List<Entry> entry) {

    Stream<Message> messages() {
        if (entry == null) {
            return Stream.empty();
        }
        return entry.stream()
                .filter(e -> e.changes() != null)
                .flatMap(e -> e.changes().stream())
                .filter(c -> "messages".equals(c.field()) && c.value() != null && c.value().messages() != null)
                .flatMap(c -> c.value().messages().stream());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Entry(String id, List<Change> changes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Change(String field, Value value) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Value(List<Message> messages) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(String from, String id, String type, Text text, Media image, Media document) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Text(String body) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Media(String id, @JsonProperty("mime_type") String mimeType, String caption) {
    }
}
