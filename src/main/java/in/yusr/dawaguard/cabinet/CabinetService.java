package in.yusr.dawaguard.cabinet;

import in.yusr.dawaguard.cdsco.BatchKeys;
import in.yusr.dawaguard.check.StripReading;
import in.yusr.dawaguard.check.VerdictEngine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** A family's saved medicines, keyed by their WhatsApp number. */
@Service
public class CabinetService {

    static final int MAX_ITEMS = 25;

    private final CabinetItemRepository items;
    private final VerdictEngine verdicts;

    public CabinetService(CabinetItemRepository items, VerdictEngine verdicts) {
        this.items = items;
        this.verdicts = verdicts;
    }

    /**
     * Saves a checked medicine, or updates it if this batch is already saved. Returns empty when there is no
     * batch number to re-check later, or the cabinet is full.
     */
    @Transactional
    public Optional<CabinetItem> save(String ownerPhone, StripReading reading, String label) {
        String batchKey = BatchKeys.strict(reading.batchNo());
        if (batchKey.isEmpty()) {
            return Optional.empty();
        }
        Optional<CabinetItem> existing = items.findFirstByOwnerPhoneAndBatchKey(ownerPhone, batchKey);
        if (existing.isPresent()) {
            existing.get().update(reading, label);
            return existing;
        }
        if (items.countByOwnerPhone(ownerPhone) >= MAX_ITEMS) {
            return Optional.empty();
        }
        return Optional.of(items.save(CabinetItem.of(ownerPhone, reading, label)));
    }

    /** Saved medicines with a fresh verdict each, so expiry and new CDSCO data are always reflected. */
    @Transactional(readOnly = true)
    public List<Entry> list(String ownerPhone) {
        return items.findByOwnerPhoneOrderByAddedAtAsc(ownerPhone).stream()
                .map(item -> new Entry(item, verdicts.assess(item.toReading())))
                .toList();
    }

    /** Removes the medicine at a 1-based position in the list. */
    @Transactional
    public Optional<CabinetItem> remove(String ownerPhone, int position) {
        List<CabinetItem> mine = items.findByOwnerPhoneOrderByAddedAtAsc(ownerPhone);
        if (position < 1 || position > mine.size()) {
            return Optional.empty();
        }
        CabinetItem item = mine.get(position - 1);
        items.delete(item);
        return Optional.of(item);
    }

    @Transactional
    public void clear(String ownerPhone) {
        items.deleteByOwnerPhone(ownerPhone);
    }

    public record Entry(CabinetItem item, VerdictEngine.Assessment assessment) {
    }
}
