package xmu.edu.yiyuan.repository;

import xmu.edu.yiyuan.entity.MedicineStock;
import java.util.List;
import java.util.Optional;

public interface MedicineStockRepository {
    Optional<MedicineStock> findById(Long id);
    List<MedicineStock> findAll();
    List<MedicineStock> findByMedicineId(Long medicineId);
    List<MedicineStock> findByLocation(String location);
    List<MedicineStock> findByBatchNumber(String batchNumber);
    MedicineStock save(MedicineStock stock);
    void update(MedicineStock stock);
    void deleteById(Long id);
}
