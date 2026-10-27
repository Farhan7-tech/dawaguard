package in.yusr.dawaguard.check;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import in.yusr.dawaguard.cdsco.FlaggedBatch;
import in.yusr.dawaguard.config.DawaGuardProperties;
import in.yusr.dawaguard.llm.NebiusClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Step 4: turn the decided verdict into words a worried family member can act on.
 * Nemotron 3 Ultra writes the full explanation; Nemotron 3.5 Lightning writes the short WhatsApp message.
 * Both receive the same fixed facts and are told not to change the verdict or add facts.
 */
@Component
public class Explainer {

    private static final String RULES = """
            Hard rules:
            - The verdict is already decided by DawaGuard's checks. Never change, soften or contradict it.
            - Use only the facts provided. Do not invent batch numbers, dates, test results, laws or phone numbers.
            - A failed CDSCO test applies to that specific batch, not the whole brand or company. Say so when relevant.
            - Never tell anyone to stop a medicine or treatment on their own. For a flagged batch, the safe advice is to
              ask their pharmacist to replace it with a different batch, and to speak to their doctor or pharmacist first.
            - This is a safety check, not medical advice.
            - Write in %s. Keep medicine names, batch numbers and dates exactly as given.
            """;

    private static final String EXPLAIN_PROMPT = """
            You are DawaGuard, a calm medicine-safety assistant for Indian families.
            Return ONLY a JSON object: {"headline": string (max 12 words), "explanation": string (2-4 short sentences),
            "next_steps": [2-4 short imperative steps]}.
            Citations: write [n] (the source's "ref") only right after a claim that source itself states.
            Official CDSCO records are marked "official". That a batch is NOT on any alert comes from DawaGuard's own
            check of the CDSCO data, not from a web page: never cite web sources for it. If nothing supports a sentence,
            leave it uncited.
            """;

    private static final String WHATSAPP_PROMPT = """
            You write one WhatsApp message (max 320 characters, plain text, no markdown) telling a family member
            the result of a medicine check. Start with one fitting emoji. Include the medicine name and batch number.
            End with the single most important next step.
            """;

    private final NebiusClient nebius;
    private final DawaGuardProperties props;
    private final JsonMapper json;

    public Explainer(NebiusClient nebius, DawaGuardProperties props, JsonMapper json) {
        this.nebius = nebius;
        this.props = props;
        this.json = json;
    }

    public Explained explain(Facts facts) {
        NebiusClient.ChatResult chat = nebius.chat(props.nebius().models().reasoning(),
                EXPLAIN_PROMPT + RULES.formatted(facts.languageName()), json.writeValueAsString(facts.asPromptInput()), 1500);
        return new Explained(json.readValue(JsonText.firstObject(chat.content()), Explanation.class), chat);
    }

    public NebiusClient.ChatResult whatsapp(Facts facts) {
        NebiusClient.ChatResult chat = nebius.chat(props.nebius().models().fast(),
                WHATSAPP_PROMPT + RULES.formatted(facts.languageName()), json.writeValueAsString(facts.asPromptInput()),
                null, 300, false);
        return new NebiusClient.ChatResult(chat.model(), toWhatsAppText(chat.content()), chat.promptTokens(),
                chat.completionTokens(), chat.latencyMs());
    }

    /** WhatsApp bolds with *single* asterisks; models tend to write Markdown's **double**. */
    static String toWhatsAppText(String text) {
        String s = text.replace("**", "*").replaceAll("[ \\t]+\\n", "\n").replaceAll("\\n{3,}", "\n\n").trim();
        return s.length() > 600 ? s.substring(0, 600).trim() + "…" : s;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Explanation(String headline, String explanation, @JsonProperty("next_steps") List<String> nextSteps) {
    }

    public record Explained(Explanation explanation, NebiusClient.ChatResult chat) {
    }

    /** Everything the models are allowed to know. */
    public record Facts(String language, StripReading reading, VerdictEngine.Assessment assessment,
                        EvidenceService.Evidence evidence) {

        String languageName() {
            return "hi".equals(language) ? "simple Hindi (Devanagari script)" : "simple English";
        }

        Map<String, Object> asPromptInput() {
            Map<String, Object> in = new LinkedHashMap<>();
            in.put("verdict", assessment.verdict().name());
            in.put("verdict_meaning", meaning(assessment.verdict()));
            in.put("medicine_as_read", reading);
            in.put("expiry_parsed", assessment.expiry() == null ? null : assessment.expiry().toString());
            in.put("is_expired", assessment.expired());
            in.put("cdsco_confirmed_matches", assessment.confirmedMatches().stream().map(Facts::match).toList());
            in.put("cdsco_possible_matches", assessment.possibleMatches().stream().map(Facts::match).toList());
            List<Map<String, Object>> sources = new ArrayList<>();
            if (evidence != null) {
                for (int i = 0; i < evidence.sources().size(); i++) {
                    EvidenceService.Source s = evidence.sources().get(i);
                    sources.add(Map.of("ref", i + 1, "official", s.official(), "title", s.title(),
                            "summary", s.snippet(), "mentions_this_batch", s.mentionsBatch()));
                }
            }
            in.put("evidence_sources", sources);
            in.put("reporting_options", List.of(
                    "Return the strip to the pharmacy where it was bought and show them the CDSCO alert.",
                    "Report a suspected bad or fake medicine to CDSCO or your State Drugs Controller.",
                    "Report a side effect to the Pharmacovigilance Programme of India (PvPI) helpline 1800-180-3024."));
            return in;
        }

        private static Map<String, Object> match(FlaggedBatch b) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("list", b.getListType().name());
            m.put("product", b.getProductName());
            m.put("brand", b.getBrandName());
            m.put("batch_no", b.getBatchNo());
            m.put("manufacturer", b.getManufacturer());
            m.put("test_result", b.getResult());
            m.put("reported_by", b.getReportedBy());
            m.put("cdsco_alert_month", b.getReportingMonth());
            return m;
        }

        private static String meaning(Verdict v) {
            return switch (v) {
                case SUSPECTED_FAKE -> "This batch is on CDSCO's list of suspected spurious (fake) medicines.";
                case FAILED_QUALITY_TEST -> "This batch failed a government lab quality test (CDSCO 'Not of Standard Quality').";
                case POSSIBLE_MATCH -> "A nearly identical batch number for this medicine is flagged; the user should re-check the batch number on the strip.";
                case EXPIRED -> "Not on any CDSCO alert, but the medicine is past its expiry date.";
                case NEEDS_BATCH_NUMBER -> "The batch number could not be read, so the check could not run. Ask for a clearer photo or the typed batch number.";
                case UNREADABLE -> "The photo does not show a readable medicine pack.";
                case NOT_FLAGGED -> "This batch is not on any CDSCO quality or spurious alert in DawaGuard's data, and it is not expired.";
            };
        }
    }
}
