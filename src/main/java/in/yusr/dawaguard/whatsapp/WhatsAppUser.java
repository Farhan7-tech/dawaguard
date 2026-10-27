package in.yusr.dawaguard.whatsapp;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;

/** A family member who has messaged DawaGuard on WhatsApp. */
@Entity
@Table(name = "whatsapp_user")
public class WhatsAppUser {

    /** WhatsApp only allows free-form messages within 24 hours of the user's last message; keep a margin. */
    private static final Duration REPLY_WINDOW = Duration.ofHours(23).plusMinutes(30);

    @Id
    private String phone;
    private String language;
    private Instant firstSeenAt;
    private Instant lastInboundAt;

    protected WhatsAppUser() {
    }

    WhatsAppUser(String phone, String language) {
        this.phone = phone;
        this.language = language;
        this.firstSeenAt = Instant.now();
    }

    void touch() {
        this.lastInboundAt = Instant.now();
    }

    void setLanguage(String language) {
        this.language = language;
    }

    public boolean insideReplyWindow() {
        return lastInboundAt != null && lastInboundAt.isAfter(Instant.now().minus(REPLY_WINDOW));
    }

    public String getPhone() { return phone; }
    public String getLanguage() { return language; }
}
