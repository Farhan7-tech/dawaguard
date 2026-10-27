package in.yusr.dawaguard.cdsco;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;

/** A medicine batch that CDSCO reported as failing a quality test, or as suspected fake. */
@Entity
@Table(name = "flagged_batch", indexes = {
        @Index(name = "idx_batch_key", columnList = "batchKey"),
        @Index(name = "idx_batch_key_loose", columnList = "batchKeyLoose"),
        @Index(name = "idx_period", columnList = "reportingPeriod")
})
public class FlaggedBatch {

    private static final DateTimeFormatter CDSCO_MONTH = DateTimeFormatter.ofPattern("MMM-yyyy", Locale.ENGLISH);

    /** Hash of the identifying fields, so re-syncing a month is idempotent. */
    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    private ListType listType;

    @Column(length = 500)
    private String productName;
    @Column(length = 300)
    private String brandName;
    private String batchNo;
    private String batchKey;
    private String batchKeyLoose;
    private String mfgDate;
    private String expiryDate;
    @Column(length = 1000)
    private String manufacturer;
    @Column(length = 2000)
    private String result;
    private String reportingSource;
    private String reportedBy;
    /** As CDSCO writes it, e.g. "SEP-2026". */
    private String reportingMonth;
    /** Sortable form, e.g. "2026-09". */
    private String reportingPeriod;
    @Column(length = 1000)
    private String remarks;

    protected FlaggedBatch() {
    }

    public static FlaggedBatch from(CdscoRow row, ListType type) {
        FlaggedBatch b = new FlaggedBatch();
        b.listType = type;
        b.productName = clean(row.productName());
        b.brandName = clean(row.brandName());
        b.batchNo = clean(row.batchNo());
        b.batchKey = BatchKeys.strict(row.batchNo());
        b.batchKeyLoose = BatchKeys.loose(row.batchNo());
        b.mfgDate = clean(row.mfgDate());
        b.expiryDate = clean(row.expiryDate());
        b.manufacturer = clean(row.manufacturer());
        b.result = clean(row.result());
        b.reportingSource = clean(row.reportingSource());
        b.reportedBy = clean(row.reportedBy());
        b.reportingMonth = clean(row.reportingMonth());
        b.reportingPeriod = toPeriod(b.reportingMonth);
        b.remarks = clean(row.remarks());
        b.id = sha256(String.join("|", type.name(), b.batchKey, String.valueOf(b.productName),
                String.valueOf(b.reportingMonth), String.valueOf(b.reportedBy)));
        return b;
    }

    private static String toPeriod(String reportingMonth) {
        if (reportingMonth == null || reportingMonth.isBlank()) {
            return null;
        }
        String normalized = reportingMonth.charAt(0) + reportingMonth.substring(1).toLowerCase(Locale.ROOT);
        try {
            return YearMonth.parse(normalized, CDSCO_MONTH).toString();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String clean(String s) {
        return s == null ? null : s.replaceAll("\\s+", " ").trim();
    }

    private static String sha256(String s) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public String getId() { return id; }
    public ListType getListType() { return listType; }
    public String getProductName() { return productName; }
    public String getBrandName() { return brandName; }
    public String getBatchNo() { return batchNo; }
    public String getBatchKey() { return batchKey; }
    public String getBatchKeyLoose() { return batchKeyLoose; }
    public String getMfgDate() { return mfgDate; }
    public String getExpiryDate() { return expiryDate; }
    public String getManufacturer() { return manufacturer; }
    public String getResult() { return result; }
    public String getReportingSource() { return reportingSource; }
    public String getReportedBy() { return reportedBy; }
    public String getReportingMonth() { return reportingMonth; }
    public String getReportingPeriod() { return reportingPeriod; }
    public String getRemarks() { return remarks; }
}
