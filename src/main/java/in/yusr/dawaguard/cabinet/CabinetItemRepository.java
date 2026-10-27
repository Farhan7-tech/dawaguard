package in.yusr.dawaguard.cabinet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CabinetItemRepository extends JpaRepository<CabinetItem, Long> {

    List<CabinetItem> findByOwnerPhoneOrderByAddedAtAsc(String ownerPhone);

    Optional<CabinetItem> findFirstByOwnerPhoneAndBatchKey(String ownerPhone, String batchKey);

    List<CabinetItem> findByBatchKey(String batchKey);

    long countByOwnerPhone(String ownerPhone);

    void deleteByOwnerPhone(String ownerPhone);
}
