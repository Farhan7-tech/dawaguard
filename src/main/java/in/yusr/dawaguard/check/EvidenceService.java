package in.yusr.dawaguard.check;

import in.yusr.dawaguard.cdsco.BatchKeys;
import in.yusr.dawaguard.cdsco.FlaggedBatch;
import in.yusr.dawaguard.cdsco.ListType;
import in.yusr.dawaguard.search.TavilyClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Step 3: Tavily looks for anything newer than the monthly CDSCO data (fresh alerts, recalls, news) that mentions
 * this batch or product. Evidence is shown and cited; it never changes the verdict by itself.
 */
@Component
public class EvidenceService {

    static final String CDSCO_PUBLIC_LIST = "https://cdscoonline.gov.in/CDSCO/viewPublicNSQDrug";

    static final List<String> SOURCES = List.of("cdsco.gov.in", "cdscoonline.gov.in", "pib.gov.in",
            "medicaldialogues.in", "pharmabiz.com", "expresspharma.in");

    private final TavilyClient tavily;

    public EvidenceService(TavilyClient tavily) {
        this.tavily = tavily;
    }

    public Evidence find(StripReading reading) {
        String product = firstNonBlank(reading.brandName(), reading.genericName(), "medicine");
        String batch = reading.batchNo() == null ? "" : "\"" + reading.batchNo().trim() + "\" ";
        String query = batch + product + " CDSCO not of standard quality OR spurious OR recall";
        List<TavilyClient.Result> results = tavily.search(query, 5, SOURCES);

        String batchKey = BatchKeys.strict(reading.batchNo());
        List<Source> sources = results.stream().map(r -> {
            String text = (r.title() + " " + r.content());
            boolean mentionsBatch = !batchKey.isEmpty() && BatchKeys.strict(text).contains(batchKey);
            return new Source(r.title(), r.url(), snippet(r.content()), mentionsBatch, false);
        }).toList();
        return new Evidence(query, sources);
    }

    /** Puts the official CDSCO records behind the verdict first, so they are source [1], [2]... */
    static Evidence withOfficialRecords(Evidence web, List<FlaggedBatch> records) {
        List<Source> sources = new ArrayList<>();
        for (FlaggedBatch b : records) {
            String list = b.getListType() == ListType.SPURIOUS ? "spurious drug alert" : "Not of Standard Quality alert";
            sources.add(new Source(
                    "CDSCO " + list + ", " + b.getReportingMonth() + ": " + b.getProductName() + ", batch " + b.getBatchNo(),
                    CDSCO_PUBLIC_LIST,
                    "Result: " + b.getResult() + " Manufacturer: " + b.getManufacturer() + ". Reported by: "
                            + b.getReportedBy() + ".",
                    true, true));
        }
        sources.addAll(web.sources());
        return new Evidence(web.query(), sources);
    }

    private static String snippet(String content) {
        if (content == null) {
            return "";
        }
        // Tavily page extracts often open with a "Title: ..." line that repeats the title.
        String s = content.replaceFirst("^\\s*Title:[^\\n]*\\n", "").replaceAll("\\s+", " ").trim();
        return s.length() > 280 ? s.substring(0, 280) + "…" : s;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank() && !v.toLowerCase(Locale.ROOT).equals("null")) {
                return v.trim();
            }
        }
        return "";
    }

    public record Evidence(String query, List<Source> sources) {
        public boolean anyMentionsBatch() {
            return sources.stream().anyMatch(Source::mentionsBatch);
        }
    }

    /** official = a CDSCO record from DawaGuard's dataset rather than a web search result. */
    public record Source(String title, String url, String snippet, boolean mentionsBatch, boolean official) {
    }
}
