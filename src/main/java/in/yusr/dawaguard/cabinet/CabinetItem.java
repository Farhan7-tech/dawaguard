package in.yusr.dawaguard.cabinet;

import in.yusr.dawaguard.cdsco.BatchKeys;
import in.yusr.dawaguard.check.StripReading;
import in.yusr.dawaguard.check.Verdict;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/** A medicine a family has checked and kept, so it can be re-checked when CDSCO publishes a new alert list. */
@Entity
@Table(name = "cabinet_item", indexes = {
        @Index(name = "idx_cabinet_owner", columnList = "ownerPhone"),
        @Index(name = "idx_cabinet_batch", columnList = "batchKey")
})
public class CabinetItem {

    @Id
    @GeneratedValue
    private Long id;
    /** The family member's WhatsApp number, as Meta sends it (country code, digits only). */
    private String ownerPhone;
    /** Who or what it is for, from the photo caption, e.g. "Papa's BP tablet". */
    @Column(length = 60)
    private String label;
    @Column(length = 120)
    private String brandName;
    @Column(length = 200)
    private String genericName;
    @Column(length = 60)
    private String strength;
    @Column(length = 40)
    private String batchNo;
    private String batchKey;
    @Column(length = 30)
    private String expiryDate;
    /** The most serious verdict the family has already been alerted about, so each alert is sent once. */
    @Enumerated(EnumType.STRING)
    private Verdict alertedVerdict;
    private Instant addedAt;
    private Instant alertedAt;

    protected CabinetItem() {
    }

    static CabinetItem of(String ownerPhone, StripReading reading, String label) {
        CabinetItem item = new CabinetItem();
        item.ownerPhone = ownerPhone;
        item.addedAt = Instant.now();
        item.update(reading, label);
        return item;
    }

    void update(StripReading reading, String label) {
        if (label != null && !label.isBlank()) {
            this.label = truncate(label.strip(), 60);
        }
        this.brandName = truncate(reading.brandName(), 120);
        this.genericName = truncate(reading.genericName(), 200);
        this.strength = truncate(reading.strength(), 60);
        this.batchNo = truncate(reading.batchNo(), 40);
        this.batchKey = BatchKeys.strict(reading.batchNo());
        this.expiryDate = truncate(reading.expiryDate(), 30);
    }

    void markAlerted(Verdict verdict) {
        this.alertedVerdict = verdict;
        this.alertedAt = Instant.now();
    }

    /** Lets the demo alert be sent again for a new recording. */
    public void resetAlert() {
        this.alertedVerdict = null;
        this.alertedAt = null;
    }

    public StripReading toReading() {
        return new StripReading(brandName, genericName, strength, batchNo, null, expiryDate, null, true, "saved", null);
    }

    /** Brand name if known, else the generic name, with the strength unless the name already includes it. */
    public String displayName() {
        String name = brandName != null ? brandName : genericName != null ? genericName : "Medicine";
        return strength != null && !name.contains(strength.replaceAll("\s+", "")) && !name.contains(strength)
                ? name + " " + strength : name;
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    public Long getId() { return id; }
    public String getOwnerPhone() { return ownerPhone; }
    public String getLabel() { return label; }
    public String getBatchNo() { return batchNo; }
    public String getBatchKey() { return batchKey; }
    public Verdict getAlertedVerdict() { return alertedVerdict; }
    public Instant getAddedAt() { return addedAt; }
}
