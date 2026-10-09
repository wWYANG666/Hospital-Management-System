package xmu.edu.yiyuan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xmu.edu.yiyuan.entity.Medicine;
import xmu.edu.yiyuan.repository.MedicineRepository;

import java.util.List;
import java.util.Optional;

@Service
public class MedicineService {

    @Autowired
    private MedicineRepository medicineRepository;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;

    public Optional<Medicine> findById(Long id) {
        return medicineRepository.findById(id);
    }

    public List<Medicine> findAll() {
        return medicineRepository.findAll();
    }

    /**
     * 查询指定科室可用的药品；如果 departmentId 为空，则返回全部
     * 同时包含全院通用（departmentId 为 null）的药品
     */
    public List<Medicine> findByDepartmentOrCommon(Long departmentId) {
        if (departmentId == null) {
            return medicineRepository.findAll();
        }
        return medicineRepository.findByDepartmentIdOrDepartmentIdIsNull(departmentId);
    }

    public List<Medicine> findAvailableMedicines() {
        return medicineRepository.findAvailableMedicines();
    }

    /**
     * 医生端住院医嘱等场景：仅显示在库可用药品，且为当前科室或全院通用。
     * departmentId 为空时返回全部在库可用药品。
     */
    public List<Medicine> findAvailableByDepartmentOrCommon(Long departmentId) {
        if (departmentId == null) {
            return medicineRepository.findAvailableMedicines();
        }
        return medicineRepository.findAvailableByDepartmentIdOrDepartmentIdIsNull(departmentId);
    }

    public Medicine save(Medicine medicine) {
        validate(medicine);
        return medicineRepository.save(medicine);
    }

    public void update(Medicine medicine) {
        validate(medicine);
        if (findById(medicine.getId()).isEmpty()) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "药品不存在");
        medicineRepository.update(medicine);
    }

    public void deleteById(Long id) {
        medicineRepository.deleteById(id);
    }

    /**
     * 药物表清空后，将主键自增重置为从 1 开始（适用于批量删除或手工清库后）
     */
    public void resetAutoIncrementIfEmpty() {
        medicineRepository.resetAutoIncrementIfEmpty();
    }

    private void validate(Medicine medicine) {
        BusinessValidation.text(medicine.getName(), 100, "药品名称");
        BusinessValidation.text(medicine.getSpecification(), 100, "药品规格");
        BusinessValidation.money(medicine.getPrice(), "药品价格");
        if (medicine.getPurchasePrice() != null) BusinessValidation.money(medicine.getPurchasePrice(), "采购价");
        BusinessValidation.stock(medicine.getStock(), "库存");
        if (medicine.getMinStockAlert() != null) BusinessValidation.stock(medicine.getMinStockAlert(), "预警库存");
        BusinessValidation.require(medicine.getStatus() != null && (medicine.getStatus() == 0 || medicine.getStatus() == 1), "药品状态无效");
        if (medicine.getDepartmentId() != null) BusinessValidation.require(jdbc.queryForObject("SELECT COUNT(*) FROM department WHERE id=?", Integer.class, medicine.getDepartmentId()) == 1, "科室不存在");
    }
}
