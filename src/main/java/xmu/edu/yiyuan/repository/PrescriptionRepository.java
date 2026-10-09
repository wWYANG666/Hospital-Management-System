package xmu.edu.yiyuan.repository;

import xmu.edu.yiyuan.entity.Prescription;
import java.util.List;
import java.util.Optional;

public interface PrescriptionRepository {
    Optional<Prescription> findById(Long id);
    List<Prescription> findAll();
    List<Prescription> findByPatientId(Long patientId);
    List<Prescription> findByPatientIdAndStatus(Long patientId, Prescription.PrescriptionStatus status);
    List<Prescription> findByAppointmentId(Long appointmentId);
    List<Prescription> findByStatus(Prescription.PrescriptionStatus status);
    List<Prescription> findByPrescriptionNumber(String prescriptionNumber);
    Prescription save(Prescription prescription);
    void update(Prescription prescription);
    void deleteById(Long id);
}
