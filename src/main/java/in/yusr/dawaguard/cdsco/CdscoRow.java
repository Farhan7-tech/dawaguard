package in.yusr.dawaguard.cdsco;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** One row as returned by the public CDSCO NSQ / spurious drug tables. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CdscoRow(
        @JsonProperty("str_product_name") @JsonAlias("product_name_from_dtl") String productName,
        @JsonProperty("str_brand_name") String brandName,
        @JsonProperty("str_batch_no") String batchNo,
        @JsonProperty("dt_manufacturing_date") String mfgDate,
        @JsonProperty("dt_expiry_date") String expiryDate,
        @JsonProperty("str_manufactured_by") String manufacturer,
        @JsonProperty("str_nsq_result") String result,
        @JsonProperty("str_reporting_source") String reportingSource,
        @JsonProperty("str_reported_by_lab_or_state") String reportedBy,
        @JsonProperty("dt_reporting_month_year") String reportingMonth,
        @JsonProperty("str_nsq_remarks") String remarks) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Page(List<CdscoRow> aaData) {
    }

    /** Shape of the bundled offline snapshot. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Snapshot(List<CdscoRow> nsq, List<CdscoRow> spurious) {
    }
}
