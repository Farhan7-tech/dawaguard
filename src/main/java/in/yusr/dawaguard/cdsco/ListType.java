package in.yusr.dawaguard.cdsco;

/** NSQ = "Not of Standard Quality" (failed a lab test). SPURIOUS = suspected fake. */
public enum ListType {
    NSQ("nsq", "filteredNsqDrugTable"),
    SPURIOUS("spurious", "filteredSpuriousDrugTable");

    final String tab;
    final String endpoint;

    ListType(String tab, String endpoint) {
        this.tab = tab;
        this.endpoint = endpoint;
    }
}
