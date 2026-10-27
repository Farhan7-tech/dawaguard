package in.yusr.dawaguard.cdsco;

import in.yusr.dawaguard.config.DawaGuardProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

/**
 * Keeps the local copy of CDSCO's flagged batches current.
 * Startup: load the bundled snapshot if the database is empty (works offline), then refresh recent months live.
 * Daily: refresh the last two months, since CDSCO publishes each month's alert a few weeks later.
 */
@Service
public class CdscoSyncService {

    private static final Logger log = LoggerFactory.getLogger(CdscoSyncService.class);
    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");

    private final CdscoClient client;
    private final FlaggedBatchRepository repository;
    private final DawaGuardProperties props;
    private final JsonMapper json;
    private volatile Instant lastSync;

    public CdscoSyncService(CdscoClient client, FlaggedBatchRepository repository,
                            DawaGuardProperties props, JsonMapper json) {
        this.client = client;
        this.repository = repository;
        this.props = props;
        this.json = json;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (!props.cdsco().syncOnStartup()) {
            return;
        }
        if (repository.count() == 0) {
            loadSnapshot();
        }
        String latest = repository.findLatestPeriod();
        YearMonth from = latest == null ? YearMonth.parse(props.cdsco().syncFrom()) : YearMonth.parse(latest).minusMonths(1);
        syncRange(from, YearMonth.now(INDIA));
    }

    @Scheduled(cron = "0 30 6 * * *", zone = "Asia/Kolkata")
    public void daily() {
        YearMonth now = YearMonth.now(INDIA);
        syncRange(now.minusMonths(2), now);
    }

    public synchronized int syncRange(YearMonth from, YearMonth to) {
        int saved = 0;
        for (YearMonth m = from; !m.isAfter(to); m = m.plusMonths(1)) {
            for (ListType type : ListType.values()) {
                try {
                    List<CdscoRow> rows = client.fetchMonth(m, type);
                    repository.saveAll(rows.stream().map(r -> FlaggedBatch.from(r, type)).toList());
                    saved += rows.size();
                    log.info("CDSCO {} {}: {} rows", type, m, rows.size());
                } catch (RuntimeException e) {
                    log.warn("CDSCO {} {} failed: {}", type, m, e.getMessage());
                }
            }
        }
        lastSync = Instant.now();
        return saved;
    }

    private void loadSnapshot() {
        try (InputStream in = new ClassPathResource("seed/cdsco-snapshot.json").getInputStream()) {
            CdscoRow.Snapshot snapshot = json.readValue(in, CdscoRow.Snapshot.class);
            repository.saveAll(snapshot.nsq().stream().map(r -> FlaggedBatch.from(r, ListType.NSQ)).toList());
            repository.saveAll(snapshot.spurious().stream().map(r -> FlaggedBatch.from(r, ListType.SPURIOUS)).toList());
            log.info("Loaded CDSCO snapshot: {} NSQ, {} spurious", snapshot.nsq().size(), snapshot.spurious().size());
        } catch (IOException | RuntimeException e) {
            log.warn("Could not load CDSCO snapshot: {}", e.getMessage());
        }
    }

    public Instant getLastSync() {
        return lastSync;
    }
}
