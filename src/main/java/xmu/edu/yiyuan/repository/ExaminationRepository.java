package xmu.edu.yiyuan.repository;

import xmu.edu.yiyuan.entity.Examination;
import java.util.List;
import java.util.Optional;

public interface ExaminationRepository {
    Optional<Examination> findById(Long id);
    Optional<Examination> findByName(String name);
    List<Examination> findAll();

    /**
     * 按科室查询检查项目，允许 departmentId 为空表示全院通用
     */
    List<Examination> findByDepartmentIdOrDepartmentIdIsNull(Long departmentId);
    List<Examination> findAvailableExaminations();
    Examination save(Examination examination);
    void update(Examination examination);
    void deleteById(Long id);

    /**
     * 若表内已无记录，将 AUTO_INCREMENT 重置为 1
     */
    void resetAutoIncrementIfEmpty();
}
