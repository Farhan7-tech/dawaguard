package in.yusr.dawaguard.cdsco;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FlaggedBatchRepository extends JpaRepository<FlaggedBatch, String> {

    List<FlaggedBatch> findByBatchKey(String batchKey);

    List<FlaggedBatch> findByBatchKeyLoose(String batchKeyLoose);

    long countByListType(ListType listType);

    @Query("select max(b.reportingPeriod) from FlaggedBatch b")
    String findLatestPeriod();
}
