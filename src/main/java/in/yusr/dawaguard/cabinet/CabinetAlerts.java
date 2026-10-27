package in.yusr.dawaguard.cabinet;

import in.yusr.dawaguard.cdsco.BatchKeys;
import in.yusr.dawaguard.cdsco.FlaggedBatch;
import in.yusr.dawaguard.cdsco.NewFlaggedBatches;
import in.yusr.dawaguard.check.VerdictEngine;
import in.yusr.dawaguard.config.DawaGuardProperties;
import in.yusr.dawaguard.whatsapp.WhatsAppClient;
import in.yusr.dawaguard.whatsapp.WhatsAppUser;
import in.yusr.dawaguard.whatsapp.WhatsAppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * When CDSCO publishes new flagged batches, re-checks every saved medicine against just those rows and
 * messages the family about any that are now flagged. Each alert is sent once per verdict.
 */
@Component
public class CabinetAlerts {

    private static final Logger log = LoggerFactory.getLogger(CabinetAlerts.class);

    private final CabinetItemRepository items;
    private final VerdictEngine verdicts;
    private final WhatsAppClient whatsapp;
    private final WhatsAppUserRepository users;
    private final DawaGuardProperties.WhatsApp config;

    public CabinetAlerts(CabinetItemRepository items, VerdictEngine verdicts, WhatsAppClient whatsapp,
                         WhatsAppUserRepository users, DawaGuardProperties props) {
        this.items = items;
        this.verdicts = verdicts;
        this.whatsapp = whatsapp;
        this.users = users;
        this.config = props.whatsapp();
    }

    @Async
    @EventListener
    public void onNewBatches(NewFlaggedBatches event) {
        int sent = recheck(event.batches());
        log.info("Re-checked saved medicines against {} new CDSCO rows: {} alerts sent", event.batches().size(), sent);
    }

    /** Returns how many alerts were sent. Cabinets are small, so this scans them rather than indexing loose keys. */
    @Transactional
    public synchronized int recheck(List<FlaggedBatch> newRows) {
        Set<String> strict = newRows.stream().map(FlaggedBatch::getBatchKey).collect(Collectors.toSet());
        Set<String> loose = newRows.stream().map(FlaggedBatch::getBatchKeyLoose).collect(Collectors.toSet());
        int sent = 0;
        for (CabinetItem item : items.findAll()) {
            if (item.getBatchKey() == null
                    || !strict.contains(item.getBatchKey()) && !loose.contains(BatchKeys.loose(item.getBatchNo()))) {
                continue;
            }
            VerdictEngine.Assessment a = verdicts.assessAgainst(item.toReading(), newRows);
            boolean alreadyTold = item.getAlertedVerdict() != null
                    && item.getAlertedVerdict().ordinal() <= a.verdict().ordinal();
            if (a.verdict().flagged() && !alreadyTold && notify(item, a)) {
                item.markAlerted(a.verdict());
                items.save(item);
                sent++;
            }
        }
        return sent;
    }

    /**
     * Inside WhatsApp's 24-hour window the full alert goes as text. Outside it only an approved template may be
     * sent; it names the medicine and asks the family to reply, which reopens the window.
     */
    private boolean notify(CabinetItem item, VerdictEngine.Assessment a) {
        if (!whatsapp.canSend()) {
            log.info("Medicine {} is newly flagged, but WhatsApp is not configured", item.getId());
            return false;
        }
        WhatsAppUser user = users.findById(item.getOwnerPhone()).orElse(null);
        String language = user == null || user.getLanguage() == null ? "en" : user.getLanguage();
        try {
            if (user != null && user.insideReplyWindow()) {
                whatsapp.sendText(item.getOwnerPhone(), CabinetMessages.alert(item, a, language));
            } else {
                whatsapp.sendTemplate(item.getOwnerPhone(), config.alertTemplate(), config.templateLanguage(),
                        CabinetMessages.templateParameters(item));
            }
            return true;
        } catch (RestClientException e) {
            log.warn("Could not send alert for medicine {}: {}", item.getId(), e.getMessage());
            return false;
        }
    }
}
