package in.yusr.dawaguard.cdsco;

import in.yusr.dawaguard.config.DawaGuardProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Reads CDSCO's public NSQ / spurious drug tables (the data behind cdscoonline.gov.in/CDSCO/viewPublicNSQDrug). */
@Component
public class CdscoClient {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM-yyyy", Locale.ENGLISH);

    private final RestClient http;
    private final JsonMapper json;

    public CdscoClient(DawaGuardProperties props, RestClient.Builder builder, JsonMapper json) {
        this.http = builder
                .baseUrl(props.cdsco().baseUrl())
                .defaultHeader("User-Agent", "DawaGuard/0.1 (medicine safety checker)")
                .build();
        this.json = json;
    }

    public List<CdscoRow> fetchMonth(YearMonth month, ListType type) {
        // The endpoint answers with JSON but labels it text/html, so parse the body ourselves.
        String body = http.get()
                .uri(uri -> uri.path("/" + type.endpoint)
                        .queryParam("month", month.format(MONTH))
                        .queryParam("source", "All")
                        .queryParam("tab", type.tab)
                        .build())
                .retrieve()
                .body(String.class);
        if (body == null || body.isBlank()) {
            return List.of();
        }
        CdscoRow.Page page = json.readValue(body, CdscoRow.Page.class);
        return page.aaData() == null ? List.of() : page.aaData();
    }
}
