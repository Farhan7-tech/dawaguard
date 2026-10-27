package in.yusr.dawaguard.web;

import in.yusr.dawaguard.cdsco.BatchKeys;
import in.yusr.dawaguard.cdsco.CdscoSyncService;
import in.yusr.dawaguard.cdsco.FlaggedBatch;
import in.yusr.dawaguard.cdsco.FlaggedBatchRepository;
import in.yusr.dawaguard.cdsco.ListType;
import in.yusr.dawaguard.config.DawaGuardProperties;
import in.yusr.dawaguard.llm.NebiusClient;
import in.yusr.dawaguard.search.TavilyClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StatusController {

    private final FlaggedBatchRepository batches;
    private final CdscoSyncService sync;
    private final NebiusClient nebius;
    private final TavilyClient tavily;
    private final DawaGuardProperties props;

    public StatusController(FlaggedBatchRepository batches, CdscoSyncService sync, NebiusClient nebius,
                            TavilyClient tavily, DawaGuardProperties props) {
        this.batches = batches;
        this.sync = sync;
        this.nebius = nebius;
        this.tavily = tavily;
        this.props = props;
    }

    /** How much CDSCO data is loaded and how fresh it is. */
    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("nsqBatches", batches.countByListType(ListType.NSQ));
        out.put("spuriousBatches", batches.countByListType(ListType.SPURIOUS));
        out.put("latestReportingPeriod", batches.findLatestPeriod());
        out.put("lastSync", sync.getLastSync());
        out.put("models", props.nebius().models());
        return out;
    }

    /** Smoke test: one tiny call to each Nemotron model on Nebius Token Factory. */
    @GetMapping("/status/models")
    public List<Map<String, Object>> models() {
        var m = props.nebius().models();
        return List.of(ping("vision", m.vision()), ping("reasoning", m.reasoning()), ping("fast", m.fast()));
    }

    /** Smoke test for Tavily live search. */
    @GetMapping("/status/search")
    public List<TavilyClient.Result> search() {
        return tavily.search("CDSCO drug alert not of standard quality batches", 3,
                List.of("cdsco.gov.in", "medicaldialogues.in", "pib.gov.in"));
    }

    /** Deterministic lookup of a batch number against CDSCO flagged batches. */
    @GetMapping("/batches/{batchNo}")
    public Map<String, Object> lookup(@PathVariable String batchNo) {
        List<FlaggedBatch> exact = batches.findByBatchKey(BatchKeys.strict(batchNo));
        List<FlaggedBatch> loose = exact.isEmpty() ? batches.findByBatchKeyLoose(BatchKeys.loose(batchNo)) : List.of();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("query", batchNo);
        out.put("exactMatches", exact);
        out.put("possibleMatches", loose);
        return out;
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
            out.put("ok", false);
            out.put("error", e.getMessage());
        }
        return out;
    }
}
