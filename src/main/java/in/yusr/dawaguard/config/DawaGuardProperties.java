package in.yusr.dawaguard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dawaguard")
public record DawaGuardProperties(Nebius nebius, Tavily tavily, Cdsco cdsco, Limits limits) {

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
}
