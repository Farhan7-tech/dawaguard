package in.yusr.dawaguard.check;

/** Decided by code from the CDSCO data and the expiry date, never by a language model. Ordered by severity. */
public enum Verdict {
    /** Batch and product match a CDSCO spurious (suspected fake) alert. */
    SUSPECTED_FAKE,
    /** Batch and product match a CDSCO "Not of Standard Quality" alert. */
    FAILED_QUALITY_TEST,
    /** A near-identical batch code (allowing for misreads) matches this product; the user should confirm. */
    POSSIBLE_MATCH,
    /** Past its expiry date. */
    EXPIRED,
    /** No batch number could be read, so nothing could be checked. */
    NEEDS_BATCH_NUMBER,
    /** The photo doesn't show a readable medicine pack. */
    UNREADABLE,
    /** Not in any CDSCO alert in the loaded data, and not expired. */
    NOT_FLAGGED;

    /** On a CDSCO alert (or nearly so): the family should hear about it. */
    public boolean flagged() {
        return this == SUSPECTED_FAKE || this == FAILED_QUALITY_TEST || this == POSSIBLE_MATCH;
    }
}
