package in.yusr.dawaguard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dawaguard")
public record DawaGuardProperties(Nebius nebius, Tavily tavily, Cdsco cdsco, Limits limits, WhatsApp whatsapp,
                                  Demo demo) {

    /** Caps on paid checks (each one calls Nebius and Tavily). */
    public record Limits(int checksPerClientPerHour, int checksPerDay) {
    }

    public record Nebius(String baseUrl, String apiKey, Models models) {
    }

    /** Which NVIDIA Nemotron model handles which job. */
    public record Models(String vision, String reasoning, String fast) {
    }

    public record Tavily(String baseUrl, String apiKey) {
    }

    /** syncFrom is the first reporting month to pull, as yyyy-MM. */
    public record Cdsco(String baseUrl, String syncFrom, boolean syncOnStartup) {
    }

    /**
     * Meta WhatsApp Cloud API. appSecret signs incoming webhooks; verifyToken is the string Meta echoes back
     * when the webhook is registered. alertTemplate is an approved template used for alerts sent more than
     * 24 hours after the family last wrote, when WhatsApp allows only templates.
     */
    public record WhatsApp(boolean enabled, String graphUrl, String phoneNumberId, String accessToken,
                           String verifyToken, String appSecret, String alertTemplate, String templateLanguage) {

        public boolean canSend() {
            return enabled && notBlank(phoneNumberId) && notBlank(accessToken);
        }

        private static boolean notBlank(String s) {
            return s != null && !s.isBlank();
        }
    }

    /** token unlocks the endpoint that simulates a new CDSCO alert for the demo video. Blank disables it. */
    public record Demo(String token) {
    }
}
