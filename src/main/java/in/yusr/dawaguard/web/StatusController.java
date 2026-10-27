package in.yusr.dawaguard.web;

import in.yusr.dawaguard.cdsco.BatchKeys;
import in.yusr.dawaguard.cdsco.CdscoSyncService;
import in.yusr.dawaguard.cdsco.FlaggedBatch;
import in.yusr.dawaguard.cdsco.FlaggedBatchRepository;
import in.yusr.dawaguard.cdsco.ListType;
import in.yusr.dawaguard.config.DawaGuardProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Public, free endpoints: they only read the local CDSCO copy and never call paid APIs. */
@RestController
@RequestMapping("/api")
public class StatusController {

    private final FlaggedBatchRepository batches;
    private final CdscoSyncService sync;
    private final DawaGuardProperties props;

    public StatusController(FlaggedBatchRepository batches, CdscoSyncService sync, DawaGuardProperties props) {
        this.batches = batches;
        this.sync = sync;
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
}
