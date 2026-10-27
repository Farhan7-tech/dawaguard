package in.yusr.dawaguard.check;

import in.yusr.dawaguard.cdsco.BatchKeys;
import in.yusr.dawaguard.cdsco.CdscoRow;
import in.yusr.dawaguard.cdsco.FlaggedBatch;
import in.yusr.dawaguard.cdsco.FlaggedBatchRepository;
import in.yusr.dawaguard.cdsco.ListType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VerdictEngineTest {

    private static final YearMonth TODAY = YearMonth.of(2026, 10);

    private FlaggedBatchRepository repo;
    private VerdictEngine engine;

    @BeforeEach
    void setUp() {
        repo = mock(FlaggedBatchRepository.class);
        when(repo.findByBatchKey(anyString())).thenReturn(List.of());
        when(repo.findByBatchKeyLoose(anyString())).thenReturn(List.of());
        engine = new VerdictEngine(repo);
    }

    @Test
    void sameBatchAndSameProductFailsQualityTest() {
        FlaggedBatch flagged = batch("Pantoprazole Gastro Resistant Tablets IP", "Pansid-40 Tablets", "TG25-1814A", ListType.NSQ);
        when(repo.findByBatchKey(BatchKeys.strict("TG25-1814A"))).thenReturn(List.of(flagged));

        var result = engine.assess(reading("Pansid-40 Tablets", "Pantoprazole Gastro-resistant Tablets IP", "TG25-1814A", "SEP.2027"), TODAY);

        assertThat(result.verdict()).isEqualTo(Verdict.FAILED_QUALITY_TEST);
        assertThat(result.confirmedMatches()).containsExactly(flagged);
    }

    @Test
    void sameBatchCodeOnADifferentProductIsNotAMatch() {
        FlaggedBatch other = batch("Cetirizine Tablets IP", "Cetrizet", "AB1234", ListType.NSQ);
        when(repo.findByBatchKey("AB1234")).thenReturn(List.of(other));

        var result = engine.assess(reading("Demopar 500", "Paracetamol Tablets IP", "AB-1234", "MAY.2028"), TODAY);

        assertThat(result.verdict()).isEqualTo(Verdict.NOT_FLAGGED);
        assertThat(result.otherProductMatches()).containsExactly(other);
    }

    @Test
    void spuriousOutranksQualityFailure() {
        FlaggedBatch nsq = batch("Buprenorphine Injection IP", null, "LM1888", ListType.NSQ);
        FlaggedBatch fake = batch("Buprenorphine Injection IP", null, "LM1888", ListType.SPURIOUS);
        when(repo.findByBatchKey("LM1888")).thenReturn(List.of(nsq, fake));

        var result = engine.assess(reading(null, "Buprenorphine Injection", "LM1888", "MAY.2029"), TODAY);

        assertThat(result.verdict()).isEqualTo(Verdict.SUSPECTED_FAKE);
    }

    @Test
    void misreadBatchOfSameProductIsAPossibleMatch() {
        FlaggedBatch flagged = batch("Paracetamol Paediatric Oral Suspension IP", "Aqua P Mol 125", "PAMLA002", ListType.NSQ);
        when(repo.findByBatchKeyLoose(BatchKeys.loose("PAMLAOO2"))).thenReturn(List.of(flagged));

        var result = engine.assess(reading("Aqua P Mol 125", "Paracetamol Suspension", "PAMLAOO2", "JUL.2027"), TODAY);

        assertThat(result.verdict()).isEqualTo(Verdict.POSSIBLE_MATCH);
    }

    @Test
    void expiredWhenPastExpiryMonth() {
        var result = engine.assess(reading("Democet 10", "Cetirizine Tablets", "DC23K201", "OCT.2025"), TODAY);
        assertThat(result.verdict()).isEqualTo(Verdict.EXPIRED);
        assertThat(result.expired()).isTrue();
    }

    @Test
    void stillValidDuringItsExpiryMonth() {
        var result = engine.assess(reading("Demopar 500", "Paracetamol Tablets", "DG26B047", "OCT.2026"), TODAY);
        assertThat(result.verdict()).isEqualTo(Verdict.NOT_FLAGGED);
    }

    @Test
    void missingBatchNeedsBatchNumber() {
        var result = engine.assess(reading("Demopar 500", "Paracetamol Tablets", null, "MAY.2028"), TODAY);
        assertThat(result.verdict()).isEqualTo(Verdict.NEEDS_BATCH_NUMBER);
    }

    @Test
    void nonMedicinePhotoIsUnreadable() {
        StripReading cat = new StripReading(null, null, null, null, null, null, null, false, "clear", "a cat");
        assertThat(engine.assess(cat, TODAY).verdict()).isEqualTo(Verdict.UNREADABLE);
    }

    @Test
    void parsesCommonExpiryFormats() {
        assertThat(ExpiryDates.parse("SEP.2027")).contains(YearMonth.of(2027, 9));
        assertThat(ExpiryDates.parse("Exp. Sep-27")).contains(YearMonth.of(2027, 9));
        assertThat(ExpiryDates.parse("09/2027")).contains(YearMonth.of(2027, 9));
        assertThat(ExpiryDates.parse("EXP 5/26")).contains(YearMonth.of(2026, 5));
        assertThat(ExpiryDates.parse("not a date")).isEmpty();
    }

    @Test
    void extractsJsonFromChattyModelOutput() {
        String text = "Here you go:\n```json\n{\"batch_no\": \"A{1}\", \"nested\": {\"x\": 1}}\n```";
        assertThat(JsonText.firstObject(text)).isEqualTo("{\"batch_no\": \"A{1}\", \"nested\": {\"x\": 1}}");
    }

    private static StripReading reading(String brand, String generic, String batch, String expiry) {
        return new StripReading(brand, generic, null, batch, null, expiry, null, true, "clear", null);
    }

    private static FlaggedBatch batch(String product, String brand, String batchNo, ListType type) {
        return FlaggedBatch.from(new CdscoRow(product, brand, batchNo, "Jan-2025", "Dec-2027", "Some Pharma",
                "Does not conform", "State lab", "LUCKNOW", "JAN-2026", null), type);
    }
}
