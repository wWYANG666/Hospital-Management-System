package xmu.edu.yiyuan.repository;

import xmu.edu.yiyuan.entity.MedicalRecord;
import java.util.List;
import java.util.Optional;

public interface MedicalRecordRepository {
    Optional<MedicalRecord> findById(Long id);
    Optional<MedicalRecord> findByAppointmentId(Long appointmentId);

    /** 同一挂号下可能有多条病历（历史/重复），按时间倒序 */
    List<MedicalRecord> findAllByAppointmentId(Long appointmentId);
    List<MedicalRecord> findByPatientId(Long patientId);
    List<MedicalRecord> findByDoctorId(Long doctorId);
    List<MedicalRecord> findByHospitalizationId(Long hospitalizationId);
    List<MedicalRecord> findAll();
    MedicalRecord save(MedicalRecord medicalRecord);
    void update(MedicalRecord medicalRecord);
    void deleteById(Long id);
}
