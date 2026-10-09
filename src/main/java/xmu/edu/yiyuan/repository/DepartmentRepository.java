package xmu.edu.yiyuan.repository;

import xmu.edu.yiyuan.entity.Department;
import java.util.List;
import java.util.Optional;

public interface DepartmentRepository {
    Optional<Department> findById(Long id);
    List<Department> findAll();
    Department save(Department department);
    void update(Department department);
    void deleteById(Long id);

    void resetAutoIncrementIfEmpty();
}
