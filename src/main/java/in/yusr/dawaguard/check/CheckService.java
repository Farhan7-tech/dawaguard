package in.yusr.dawaguard.check;

import in.yusr.dawaguard.cdsco.FlaggedBatch;
import in.yusr.dawaguard.config.DawaGuardProperties;
import in.yusr.dawaguard.llm.NebiusClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Photo to verdict: read (Super VL) → match (CDSCO data, deterministic) → verify (Tavily) → explain (Ultra + Lightning).
 * Emits an event as each step starts and finishes, so the UI shows which model is doing what.
 */
@Service
public class CheckService {

    private static final Logger log = LoggerFactory.getLogger(CheckService.class);

    private final StripReader reader;
    private final VerdictEngine verdicts;
    private final EvidenceService evidence;
    private final Explainer explainer;
    private final DawaGuardProperties props;
    private final ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();

    public CheckService(StripReader reader, VerdictEngine verdicts, EvidenceService evidence, Explainer explainer,
                        DawaGuardProperties props) {
        this.reader = reader;
        this.verdicts = verdicts;
        this.evidence = evidence;
        this.explainer = explainer;
        this.props = props;
    }

    public CheckResult checkPhoto(String imageDataUrl, String language, Consumer<Map<String, Object>> events) {
        List<TraceStep> trace = new ArrayList<>();
        events.accept(step("read", "running", props.nebius().models().vision(), null));
        StripReader.Result read = reader.read(imageDataUrl);
        trace.add(TraceStep.of("read", "Read the strip photo", read.chat()));
        events.accept(step("read", "done", read.chat().model(), trace.getLast()));
        return finish(read.reading(), language, trace, events);
    }

    public CheckResult checkTyped(StripReading reading, String language, Consumer<Map<String, Object>> events) {
        List<TraceStep> trace = new ArrayList<>();
        trace.add(new TraceStep("read", "Batch number typed in by the user", "manual", 0, 0, 0));
        return finish(reading, language, trace, events);
    }

    private CheckResult finish(StripReading reading, String language, List<TraceStep> trace,
                               Consumer<Map<String, Object>> events) {
        // Matching is local and instant; the live evidence search runs alongside it.
        boolean searchable = reading.looksLikeMedicine()
                && (reading.batchNo() != null || reading.brandName() != null || reading.genericName() != null);
        events.accept(step("verify", searchable ? "running" : "skipped", "tavily", null));
        long searchStarted = System.currentTimeMillis();
        CompletableFuture<EvidenceService.Evidence> search = searchable
                ? CompletableFuture.supplyAsync(() -> evidence.find(reading), pool)
                : CompletableFuture.completedFuture(new EvidenceService.Evidence("", List.of()));

        events.accept(step("match", "running", "cdsco", null));
        long matchStarted = System.currentTimeMillis();
        VerdictEngine.Assessment assessment = verdicts.assess(reading);
        trace.add(new TraceStep("match", "Matched against CDSCO flagged batches", "cdsco-data",
                System.currentTimeMillis() - matchStarted, 0, 0));
        events.accept(step("match", "done", "cdsco", trace.getLast()));

        EvidenceService.Evidence found;
        try {
            found = search.join();
        } catch (RuntimeException e) {
            log.warn("Evidence search failed: {}", e.getMessage());
            found = new EvidenceService.Evidence("", List.of());
        }
        if (searchable) {
            trace.add(new TraceStep("verify", "Searched for newer alerts and news", "tavily",
                    System.currentTimeMillis() - searchStarted, 0, 0));
            events.accept(step("verify", "done", "tavily", trace.getLast()));
        }
        List<FlaggedBatch> records = new ArrayList<>(assessment.confirmedMatches());
        records.addAll(assessment.possibleMatches());
        found = EvidenceService.withOfficialRecords(found, records);

        Explainer.Facts facts = new Explainer.Facts(language, reading, assessment, found);
        events.accept(step("explain", "running", props.nebius().models().reasoning(), null));
        events.accept(step("message", "running", props.nebius().models().fast(), null));
        CompletableFuture<Explainer.Explained> explained = CompletableFuture.supplyAsync(() -> explainer.explain(facts), pool);
        CompletableFuture<NebiusClient.ChatResult> whatsapp = CompletableFuture.supplyAsync(() -> explainer.whatsapp(facts), pool);

        Explainer.Explanation explanation = null;
        try {
            Explainer.Explained e = explained.join();
            explanation = e.explanation();
            trace.add(TraceStep.of("explain", "Explained the verdict", e.chat()));
            events.accept(step("explain", "done", e.chat().model(), trace.getLast()));
        } catch (RuntimeException e) {
            log.warn("Explanation failed: {}", e.getMessage());
            events.accept(step("explain", "failed", props.nebius().models().reasoning(), null));
        }
        String message = null;
        try {
            NebiusClient.ChatResult w = whatsapp.join();
            message = w.content();
            trace.add(TraceStep.of("message", "Wrote the WhatsApp message", w));
            events.accept(step("message", "done", w.model(), trace.getLast()));
        } catch (RuntimeException e) {
            log.warn("WhatsApp message failed: {}", e.getMessage());
            events.accept(step("message", "failed", props.nebius().models().fast(), null));
        }

        return new CheckResult(language, reading, assessment.verdict(), assessment.confirmedMatches(),
                assessment.possibleMatches(), assessment.otherProductMatches(),
                assessment.expiry() == null ? null : assessment.expiry().toString(), assessment.expired(),
                found, explanation, message, trace);
    }

    private static Map<String, Object> step(String step, String status, String engine, TraceStep done) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("type", "step");
        e.put("step", step);
        e.put("status", status);
        e.put("engine", engine);
        if (done != null) {
            e.put("ms", done.ms());
            e.put("tokens", done.promptTokens() + done.completionTokens());
        }
        return e;
    }
}
