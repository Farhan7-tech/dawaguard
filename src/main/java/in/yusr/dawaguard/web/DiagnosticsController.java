package in.yusr.dawaguard.web;

import in.yusr.dawaguard.config.DawaGuardProperties;
import in.yusr.dawaguard.llm.NebiusClient;
import in.yusr.dawaguard.search.TavilyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Smoke tests that spend Nebius and Tavily credits. Only registered when DIAGNOSTICS_ENABLED=true,
 * so a deployed demo never exposes them.
 */
@RestController
@RequestMapping("/api/diagnostics")
@ConditionalOnBooleanProperty("dawaguard.diagnostics.enabled")
public class DiagnosticsController {

    private static final Logger log = LoggerFactory.getLogger(DiagnosticsController.class);

    private final NebiusClient nebius;
    private final TavilyClient tavily;
    private final DawaGuardProperties props;

    public DiagnosticsController(NebiusClient nebius, TavilyClient tavily, DawaGuardProperties props) {
        this.nebius = nebius;
        this.tavily = tavily;
        this.props = props;
    }

    /** One tiny call to each Nemotron model on Nebius Token Factory. */
    @GetMapping("/models")
    public List<Map<String, Object>> models() {
        var m = props.nebius().models();
        return List.of(ping("vision", m.vision()), ping("reasoning", m.reasoning()), ping("fast", m.fast()));
    }

    /** One Tavily search restricted to drug-alert sources. */
    @GetMapping("/search")
    public List<TavilyClient.Result> search() {
        return tavily.search("CDSCO drug alert not of standard quality batches", 3,
                List.of("cdsco.gov.in", "medicaldialogues.in", "pib.gov.in"));
    }

    private Map<String, Object> ping(String role, String model) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("role", role);
        out.put("model", model);
        try {
            var r = nebius.chat(model, null, "Reply with exactly: OK", 200);
            out.put("ok", true);
            out.put("reply", r.content());
            out.put("latencyMs", r.latencyMs());
        } catch (RuntimeException e) {
            log.warn("Model ping failed for {}: {}", model, e.getMessage());
            out.put("ok", false);
            out.put("error", e.getClass().getSimpleName());
        }
        return out;
    }
}
