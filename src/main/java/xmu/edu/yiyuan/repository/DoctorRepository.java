package xmu.edu.yiyuan.repository;

import xmu.edu.yiyuan.entity.Doctor;
import java.util.List;
import java.util.Optional;

public interface DoctorRepository {
    Optional<Doctor> findById(Long id);
    Optional<Doctor> findByUserId(Long userId);
    List<Doctor> findAll();
    Doctor save(Doctor doctor);
    void update(Doctor doctor);
    void deleteById(Long id);
}
