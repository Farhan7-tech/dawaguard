package in.yusr.dawaguard.cabinet;

import in.yusr.dawaguard.cdsco.FlaggedBatch;
import in.yusr.dawaguard.cdsco.ListType;
import in.yusr.dawaguard.check.Explainer;
import in.yusr.dawaguard.check.Verdict;
import in.yusr.dawaguard.check.VerdictEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * WhatsApp text for the cabinet and its alerts. Written by code, not a model: an alert about a failed or fake
 * medicine has to be exact every time, and it may arrive days after the family last spoke to DawaGuard.
 */
public final class CabinetMessages {

    private CabinetMessages() {
    }

    public static String alert(CabinetItem item, VerdictEngine.Assessment a, String language) {
        boolean hi = "hi".equals(language);
        FlaggedBatch record = firstRecord(a);
        StringBuilder s = new StringBuilder();
        s.append(Explainer.emojiFor(a.verdict())).append(hi ? " *DawaGuard अलर्ट*\n" : " *DawaGuard alert*\n");
        s.append(hi ? "CDSCO ने आपकी दवा अलमारी में रखी एक दवा को फ़्लैग किया है।\n\n"
                : "CDSCO has flagged a medicine saved in your family cabinet.\n\n");
        s.append('*').append(item.displayName()).append('*');
        if (item.getLabel() != null) {
            s.append(" (").append(item.getLabel()).append(')');
        }
        s.append(hi ? ", बैच *" : ", batch *").append(item.getBatchNo()).append("*\n");
        s.append(finding(a.verdict(), hi));
        if (record != null) {
            s.append(hi ? "\nरिपोर्ट: " : "\nReported by ").append(record.getReportedBy());
            s.append(hi ? ", CDSCO अलर्ट " : ", CDSCO alert ").append(record.getReportingMonth()).append('.');
        }
        s.append("\n\n").append(Explainer.Facts.messageNextStep(a.verdict(), language));
        return s.toString();
    }

    /** Parameters for the approved alert template: medicine name, batch number. */
    public static List<String> templateParameters(CabinetItem item) {
        return List.of(item.displayName(), item.getBatchNo());
    }

    public static String list(List<CabinetService.Entry> entries, String language) {
        boolean hi = "hi".equals(language);
        if (entries.isEmpty()) {
            return hi ? "आपकी दवा अलमारी खाली है। दवा की स्ट्रिप की फ़ोटो भेजें, मैं उसे जाँचकर यहाँ रख दूँगा।"
                    : "Your medicine cabinet is empty. Send a photo of a medicine strip and I'll check it and keep it here.";
        }
        List<String> lines = new ArrayList<>();
        lines.add((hi ? "🗄️ *आपके परिवार की दवा अलमारी* (" : "🗄️ *Your family medicine cabinet* (") + entries.size() + ")");
        for (int i = 0; i < entries.size(); i++) {
            CabinetItem item = entries.get(i).item();
            VerdictEngine.Assessment a = entries.get(i).assessment();
            StringBuilder line = new StringBuilder();
            line.append(i + 1).append(". ").append(Explainer.emojiFor(a.verdict())).append(' ').append(item.displayName());
            if (item.getLabel() != null) {
                line.append(" (").append(item.getLabel()).append(')');
            }
            line.append(hi ? ", बैच " : ", batch ").append(item.getBatchNo()).append(": ").append(status(a.verdict(), hi));
            FlaggedBatch record = firstRecord(a);
            if (record != null && record.getReportingMonth() != null) {
                line.append(" (").append(record.getReportingMonth()).append(')');
            }
            lines.add(line.toString());
        }
        lines.add("");
        lines.add(hi ? "जब भी CDSCO नई अलर्ट सूची जारी करेगा, मैं इन्हें दोबारा जाँचूँगा। किसी दवा को हटाने के लिए *remove 2* लिखें।"
                : "I re-check these whenever CDSCO publishes a new alert list. Reply *remove 2* to take one out.");
        return String.join("\n", lines);
    }

    public static String saved(CabinetItem item, String language) {
        if ("hi".equals(language)) {
            return "🗄️ " + item.displayName() + " आपकी दवा अलमारी में सेव हो गई। अगर CDSCO बाद में इस बैच को फ़्लैग करता है, "
                    + "तो मैं आपको तुरंत बताऊँगा। सब देखने के लिए *cabinet* लिखें।";
        }
        return "🗄️ Saved " + item.displayName() + " to your family medicine cabinet. If CDSCO flags this batch later, "
                + "I'll message you. Reply *cabinet* to see everything saved.";
    }

    private static String finding(Verdict v, boolean hi) {
        return switch (v) {
            case SUSPECTED_FAKE -> hi ? "यह बैच CDSCO की संदिग्ध नकली (spurious) दवाओं की सूची में है।"
                    : "This batch is on CDSCO's list of suspected fake (spurious) medicines.";
            case FAILED_QUALITY_TEST -> hi ? "यह बैच सरकारी लैब की गुणवत्ता जाँच में फ़ेल हुआ (Not of Standard Quality)।"
                    : "This batch failed a government lab quality test (Not of Standard Quality).";
            case POSSIBLE_MATCH -> hi ? "इस दवा का लगभग इसी बैच नंबर वाला बैच फ़्लैग हुआ है। स्ट्रिप पर बैच नंबर ध्यान से जाँचें।"
                    : "A batch of this medicine with an almost identical batch number was flagged.";
            default -> "";
        };
    }

    private static String status(Verdict v, boolean hi) {
        return switch (v) {
            case SUSPECTED_FAKE -> hi ? "CDSCO की नकली दवा सूची में" : "suspected fake (CDSCO)";
            case FAILED_QUALITY_TEST -> hi ? "CDSCO गुणवत्ता जाँच में फ़ेल" : "failed a CDSCO quality test";
            case POSSIBLE_MATCH -> hi ? "CDSCO अलर्ट से मिलता-जुलता बैच, बैच नंबर जाँचें" : "close to a CDSCO alert, check the batch number";
            case EXPIRED -> hi ? "एक्सपायर हो चुकी है" : "expired";
            case NEEDS_BATCH_NUMBER, UNREADABLE -> hi ? "बैच नंबर नहीं" : "no batch number";
            case NOT_FLAGGED -> hi ? "किसी CDSCO अलर्ट में नहीं" : "not on any CDSCO alert";
        };
    }

    private static FlaggedBatch firstRecord(VerdictEngine.Assessment a) {
        if (!a.confirmedMatches().isEmpty()) {
            // A suspected-fake verdict should cite the spurious-list record, not a quality failure of the same batch.
            return a.confirmedMatches().stream().filter(b -> b.getListType() == ListType.SPURIOUS).findFirst()
                    .orElse(a.confirmedMatches().getFirst());
        }
        return a.possibleMatches().isEmpty() ? null : a.possibleMatches().getFirst();
    }
}
