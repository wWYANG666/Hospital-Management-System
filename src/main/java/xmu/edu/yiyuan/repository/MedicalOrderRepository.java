package xmu.edu.yiyuan.repository;

import xmu.edu.yiyuan.entity.MedicalOrder;

import java.util.List;
import java.util.Optional;

public interface MedicalOrderRepository {
    Optional<MedicalOrder> findById(Long id);
    List<MedicalOrder> findByHospitalizationId(Long hospitalizationId);
    List<MedicalOrder> findByPatientId(Long patientId);
    List<MedicalOrder> findByDoctorId(Long doctorId);
    List<MedicalOrder> findAll();
    MedicalOrder save(MedicalOrder medicalOrder);
    void update(MedicalOrder medicalOrder);
    void deleteById(Long id);
}
