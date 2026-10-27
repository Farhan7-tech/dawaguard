package in.yusr.dawaguard.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.yusr.dawaguard.config.DawaGuardProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Live web evidence: fresh CDSCO alerts, recalls and news that the monthly dataset may not have yet. */
@Component
public class TavilyClient {

    private final RestClient http;

    public TavilyClient(DawaGuardProperties props, RestClient.Builder builder) {
        this.http = builder
                .baseUrl(props.tavily().baseUrl())
                .defaultHeader("Authorization", "Bearer " + props.tavily().apiKey())
                .build();
    }

    public List<Result> search(String query, int maxResults, List<String> includeDomains) {
        Map<String, Object> body = new HashMap<>();
        body.put("query", query);
        body.put("max_results", maxResults);
        body.put("search_depth", "basic");
        if (includeDomains != null && !includeDomains.isEmpty()) {
            body.put("include_domains", includeDomains);
        }
        SearchResponse response = http.post()
                .uri("/search")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(SearchResponse.class);
        return response == null || response.results() == null ? List.of() : response.results();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(String title, String url, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SearchResponse(List<Result> results) {
    }
}
