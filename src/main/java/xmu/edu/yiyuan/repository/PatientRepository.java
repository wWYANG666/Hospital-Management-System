package xmu.edu.yiyuan.repository;

import xmu.edu.yiyuan.entity.Patient;
import java.util.List;
import java.util.Optional;

public interface PatientRepository {
    Optional<Patient> findById(Long id);
    Optional<Patient> findByUserId(Long userId);
    List<Patient> findAll();
    Patient save(Patient patient);
    void update(Patient patient);
    void deleteById(Long id);
}
