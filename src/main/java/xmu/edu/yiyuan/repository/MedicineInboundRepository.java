package xmu.edu.yiyuan.repository;

import xmu.edu.yiyuan.entity.MedicineInbound;
import java.util.List;
import java.util.Optional;

public interface MedicineInboundRepository {
    Optional<MedicineInbound> findById(Long id);
    List<MedicineInbound> findAll();
    List<MedicineInbound> findByMedicineId(Long medicineId);
    List<MedicineInbound> findByStatus(Integer status);
    MedicineInbound save(MedicineInbound inbound);
    void update(MedicineInbound inbound);
    void deleteById(Long id);
}
