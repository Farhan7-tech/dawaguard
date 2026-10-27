package in.yusr.dawaguard.cdsco;

import java.util.List;

/** Published after a sync saves batches that were not in the database before, i.e. a new CDSCO alert list. */
public record NewFlaggedBatches(List<FlaggedBatch> batches) {
}
