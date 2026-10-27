package in.yusr.dawaguard.check;

import in.yusr.dawaguard.config.DawaGuardProperties;
import in.yusr.dawaguard.llm.NebiusClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Step 1: Nemotron 3.5 Super VL reads the strip photo into structured fields. */
@Component
public class StripReader {

    static final String SYSTEM_PROMPT = """
            You read photos of Indian medicine strips, bottles and packs. Return ONLY a JSON object, no prose, with keys:
            brand_name, generic_name, strength, batch_no, mfg_date, expiry_date, manufacturer,
            is_medicine_pack (boolean), legibility ("clear" | "partial" | "unreadable"), notes.
            Rules:
            - Copy batch_no exactly as printed, keeping letters, digits and hyphens. It usually follows "B.No.", "Batch" or "Lot".
            - Copy dates as printed (for example "SEP.2027" or "09/2027").
            - Use null for anything you cannot read with confidence. Never guess a batch number.
            - If the photo is not a medicine pack, set is_medicine_pack to false.
            """;

    private final NebiusClient nebius;
    private final DawaGuardProperties props;
    private final JsonMapper json;

    public StripReader(NebiusClient nebius, DawaGuardProperties props, JsonMapper json) {
        this.nebius = nebius;
        this.props = props;
        this.json = json;
    }

    public Result read(String imageDataUrl) {
        NebiusClient.ChatResult chat = nebius.chat(props.nebius().models().vision(), SYSTEM_PROMPT,
                "Read this medicine strip.", imageDataUrl, 2000);
        return new Result(json.readValue(JsonText.firstObject(chat.content()), StripReading.class), chat);
    }

    public record Result(StripReading reading, NebiusClient.ChatResult chat) {
    }
}
