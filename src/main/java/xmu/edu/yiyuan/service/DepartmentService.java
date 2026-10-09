package xmu.edu.yiyuan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xmu.edu.yiyuan.entity.Department;
import xmu.edu.yiyuan.repository.DepartmentRepository;

import java.util.List;
import java.util.Optional;

@Service
public class DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    public Optional<Department> findById(Long id) {
        return departmentRepository.findById(id);
    }

    public List<Department> findAll() {
        return departmentRepository.findAll();
    }

    public Department save(Department department) {
        BusinessValidation.text(department.getName(), 100, "科室名称");
        return departmentRepository.save(department);
    }

    public void update(Department department) {
        BusinessValidation.text(department.getName(), 100, "科室名称");
        if (findById(department.getId()).isEmpty()) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "科室不存在");
        departmentRepository.update(department);
    }

    public void deleteById(Long id) {
        departmentRepository.deleteById(id);
    }

    public void resetAutoIncrementIfEmpty() {
        departmentRepository.resetAutoIncrementIfEmpty();
    }
}
