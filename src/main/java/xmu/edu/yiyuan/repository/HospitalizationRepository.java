package xmu.edu.yiyuan.repository;

import xmu.edu.yiyuan.entity.Hospitalization;
import java.util.List;
import java.util.Optional;

public interface HospitalizationRepository {
    Optional<Hospitalization> findById(Long id);
    List<Hospitalization> findByPatientId(Long patientId);
    List<Hospitalization> findByDoctorId(Long doctorId);
    List<Hospitalization> findByBedId(Long bedId);
    List<Hospitalization> findAll();
    Hospitalization save(Hospitalization hospitalization);
    void update(Hospitalization hospitalization);
    void deleteById(Long id);
}
