package in.yusr.dawaguard.whatsapp;

import in.yusr.dawaguard.cabinet.CabinetAlerts;
import in.yusr.dawaguard.cabinet.CabinetItem;
import in.yusr.dawaguard.cabinet.CabinetItemRepository;
import in.yusr.dawaguard.cdsco.BatchKeys;
import in.yusr.dawaguard.cdsco.CdscoRow;
import in.yusr.dawaguard.cdsco.FlaggedBatch;
import in.yusr.dawaguard.cdsco.ListType;
import in.yusr.dawaguard.check.StripReading;
import in.yusr.dawaguard.config.DawaGuardProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * CDSCO publishes alert lists about once a month, too rarely to film. This simulates one new list containing
 * a saved batch, runs the real re-check and alert path, and labels the alert as simulated. The row is never
 * stored, so checks of that batch are unaffected. Requires DEMO_TOKEN; without it the endpoint answers 404.
 */
@RestController
@RequestMapping("/api/demo")
public class DemoAlertController {

    private static final DateTimeFormatter CDSCO_MONTH = DateTimeFormatter.ofPattern("MMM-yyyy", Locale.ENGLISH);

    private final DawaGuardProperties props;
    private final CabinetItemRepository items;
    private final CabinetAlerts alerts;

    public DemoAlertController(DawaGuardProperties props, CabinetItemRepository items, CabinetAlerts alerts) {
        this.props = props;
        this.items = items;
        this.alerts = alerts;
    }

    @PostMapping("/cdsco-alert")
    public ResponseEntity<Map<String, Object>> simulate(@RequestHeader(value = "X-Demo-Token", required = false) String token,
                                                        @Valid @RequestBody SimulatedAlert body) {
        if (!WebhookSignature.sameToken(props.demo().token(), token)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        List<CabinetItem> saved = items.findByBatchKey(BatchKeys.strict(body.batchNo()));
        if (saved.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No saved medicine has this batch number."));
        }
        saved.forEach(CabinetItem::resetAlert);
        items.saveAll(saved);
        StripReading reading = saved.getFirst().toReading();
        String product = Stream.of(reading.genericName(), reading.brandName())
                .filter(Objects::nonNull).collect(Collectors.joining(" "));
        CdscoRow row = new CdscoRow(product, reading.brandName(), reading.batchNo(), null, reading.expiryDate(), null,
                "Simulated result for the DawaGuard demo", "Demo", "DawaGuard demo (simulated)",
                YearMonth.now(ZoneId.of("Asia/Kolkata")).format(CDSCO_MONTH).toUpperCase(Locale.ROOT),
                "Simulated alert. Not a real CDSCO record.");
        int sent = alerts.recheck(List.of(FlaggedBatch.from(row, ListType.NSQ)));
        return ResponseEntity.ok(Map.of("alertsSent", sent));
    }

    public record SimulatedAlert(@NotBlank @Size(max = 40) String batchNo) {
    }
}
