package in.yusr.dawaguard.cdsco;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BatchKeysTest {

    @Test
    void strictRemovesWhitespaceAndPunctuation() {
        // Real CDSCO rows contain line breaks inside batch numbers.
        assertThat(BatchKeys.strict("03BF226\n0")).isEqualTo("03BF2260");
        assertThat(BatchKeys.strict("prt2401-01")).isEqualTo("PRT240101");
        assertThat(BatchKeys.strict(null)).isEmpty();
    }

    @Test
    void looseFoldsCharactersOcrConfuses() {
        assertThat(BatchKeys.loose("SG034B0 3")).isEqualTo(BatchKeys.loose("56O34803"));
        assertThat(BatchKeys.loose("AT25-0938A")).isEqualTo(BatchKeys.loose("AT2S0938A"));
    }

    @Test
    void parsesCdscoMonthIntoSortablePeriod() {
        CdscoRow row = new CdscoRow("Paracetamol Tablets IP", "Demo", "AB 12", "Aug-2025", "Jul-2027",
                "Demo Pharma", "Does not conform", "State lab", "LUCKNOW", "SEP-2026", null);
        FlaggedBatch batch = FlaggedBatch.from(row, ListType.NSQ);
        assertThat(batch.getReportingPeriod()).isEqualTo("2026-09");
        assertThat(batch.getBatchNo()).isEqualTo("AB 12");
    }
}
