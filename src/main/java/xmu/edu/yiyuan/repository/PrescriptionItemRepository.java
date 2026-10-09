package xmu.edu.yiyuan.repository;

import xmu.edu.yiyuan.entity.PrescriptionItem;
import java.util.List;
import java.util.Optional;

public interface PrescriptionItemRepository {
    Optional<PrescriptionItem> findById(Long id);
    List<PrescriptionItem> findAll();
    List<PrescriptionItem> findByPrescriptionId(Long prescriptionId);
    PrescriptionItem save(PrescriptionItem item);
    void update(PrescriptionItem item);
    void deleteById(Long id);
    void deleteByPrescriptionId(Long prescriptionId);
}
