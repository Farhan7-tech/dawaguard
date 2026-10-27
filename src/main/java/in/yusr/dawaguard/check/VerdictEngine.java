package in.yusr.dawaguard.check;

import in.yusr.dawaguard.cdsco.BatchKeys;
import in.yusr.dawaguard.cdsco.FlaggedBatch;
import in.yusr.dawaguard.cdsco.FlaggedBatchRepository;
import in.yusr.dawaguard.cdsco.ListType;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/** Step 2: match the batch against CDSCO's flagged batches and check expiry. Deterministic. */
@Component
public class VerdictEngine {

    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");

    private final FlaggedBatchRepository batches;

    public VerdictEngine(FlaggedBatchRepository batches) {
        this.batches = batches;
    }

    public Assessment assess(StripReading reading) {
        return assess(reading, YearMonth.now(INDIA));
    }

    Assessment assess(StripReading reading, YearMonth today) {
        Optional<YearMonth> expiry = ExpiryDates.parse(reading.expiryDate());
        boolean expired = expiry.map(today::isAfter).orElse(false);
        String batchKey = BatchKeys.strict(reading.batchNo());

        if (!reading.looksLikeMedicine()) {
            return new Assessment(Verdict.UNREADABLE, List.of(), List.of(), List.of(), expiry.orElse(null), expired);
        }
        if (batchKey.isEmpty()) {
            Verdict v = expired ? Verdict.EXPIRED : Verdict.NEEDS_BATCH_NUMBER;
            return new Assessment(v, List.of(), List.of(), List.of(), expiry.orElse(null), expired);
        }

        return decide(reading, batches.findByBatchKey(batchKey),
                batches.findByBatchKeyLoose(BatchKeys.loose(reading.batchNo())), expiry, expired);
    }

    /**
     * Judges a medicine against a given set of flagged batches only, such as the rows in a list CDSCO just
     * published. Used to re-check saved medicines without re-reading the whole database.
     */
    public Assessment assessAgainst(StripReading reading, List<FlaggedBatch> candidates) {
        String batchKey = BatchKeys.strict(reading.batchNo());
        String looseKey = BatchKeys.loose(reading.batchNo());
        if (batchKey.isEmpty()) {
            return new Assessment(Verdict.NEEDS_BATCH_NUMBER, List.of(), List.of(), List.of(), null, false);
        }
        List<FlaggedBatch> exact = candidates.stream().filter(b -> batchKey.equals(b.getBatchKey())).toList();
        List<FlaggedBatch> loose = candidates.stream().filter(b -> looseKey.equals(b.getBatchKeyLoose())).toList();
        return decide(reading, exact, loose, Optional.empty(), false);
    }

    private static Assessment decide(StripReading reading, List<FlaggedBatch> exact, List<FlaggedBatch> loose,
                                     Optional<YearMonth> expiry, boolean expired) {
        List<FlaggedBatch> confirmed = exact.stream().filter(b -> ProductMatch.sameProduct(reading, b)).toList();
        List<FlaggedBatch> otherProducts = exact.stream().filter(b -> !confirmed.contains(b)).toList();
        List<FlaggedBatch> possible = confirmed.isEmpty()
                ? loose.stream()
                        .filter(b -> !exact.contains(b))
                        .filter(b -> ProductMatch.sameProduct(reading, b))
                        .toList()
                : List.of();

        Verdict verdict;
        if (confirmed.stream().anyMatch(b -> b.getListType() == ListType.SPURIOUS)) {
            verdict = Verdict.SUSPECTED_FAKE;
        } else if (!confirmed.isEmpty()) {
            verdict = Verdict.FAILED_QUALITY_TEST;
        } else if (!possible.isEmpty()) {
            verdict = Verdict.POSSIBLE_MATCH;
        } else if (expired) {
            verdict = Verdict.EXPIRED;
        } else {
            verdict = Verdict.NOT_FLAGGED;
        }
        return new Assessment(verdict, confirmed, possible, otherProducts, expiry.orElse(null), expired);
    }

    /**
     * @param otherProductMatches same batch code but a different product: shown for transparency, not counted
     */
    public record Assessment(Verdict verdict, List<FlaggedBatch> confirmedMatches, List<FlaggedBatch> possibleMatches,
                             List<FlaggedBatch> otherProductMatches, YearMonth expiry, boolean expired) {
    }
}
