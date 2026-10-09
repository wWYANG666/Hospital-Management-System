package xmu.edu.yiyuan.repository;

import xmu.edu.yiyuan.entity.Medicine;
import java.util.List;
import java.util.Optional;

public interface MedicineRepository {
    Optional<Medicine> findById(Long id);
    List<Medicine> findAll();

    /**
     * 按科室查询药品，允许 departmentId 为空表示全院通用
     */
    List<Medicine> findByDepartmentIdOrDepartmentIdIsNull(Long departmentId);
    /**
     * 在院可用药品：status=1 且 stock&gt;0，且属于指定科室或全院通用（department_id IS NULL）
     */
    List<Medicine> findAvailableByDepartmentIdOrDepartmentIdIsNull(Long departmentId);
    List<Medicine> findAvailableMedicines();
    Medicine save(Medicine medicine);
    void update(Medicine medicine);
    void deleteById(Long id);

    /**
     * 若表内已无记录，将 AUTO_INCREMENT 重置为 1（清空数据后新增可从 1 开始）
     */
    void resetAutoIncrementIfEmpty();
}
