package in.yusr.dawaguard.check;

import in.yusr.dawaguard.cdsco.FlaggedBatch;

import java.util.List;

public record CheckResult(
        String language,
        StripReading reading,
        Verdict verdict,
        List<FlaggedBatch> confirmedMatches,
        List<FlaggedBatch> possibleMatches,
        List<FlaggedBatch> otherProductMatches,
        String expiry,
        boolean expired,
        EvidenceService.Evidence evidence,
        Explainer.Explanation explanation,
        String whatsappMessage,
        List<TraceStep> trace) {
}
