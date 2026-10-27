package in.yusr.dawaguard.check;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** What the vision model read off a medicine strip. Any field may be null if it wasn't legible. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StripReading(
        @JsonProperty("brand_name") String brandName,
        @JsonProperty("generic_name") String genericName,
        @JsonProperty("strength") String strength,
        @JsonProperty("batch_no") String batchNo,
        @JsonProperty("mfg_date") String mfgDate,
        @JsonProperty("expiry_date") String expiryDate,
        @JsonProperty("manufacturer") String manufacturer,
        @JsonProperty("is_medicine_pack") Boolean isMedicinePack,
        @JsonProperty("legibility") String legibility,
        @JsonProperty("notes") String notes) {

    /** For checks typed in by hand (or corrected after a misread), where there is no photo. */
    public static StripReading manual(String brandName, String genericName, String batchNo, String expiryDate) {
        return new StripReading(brandName, genericName, null, batchNo, null, expiryDate, null, true, "manual", null);
    }

    public boolean looksLikeMedicine() {
        return !Boolean.FALSE.equals(isMedicinePack) && !"unreadable".equalsIgnoreCase(legibility);
    }
}
