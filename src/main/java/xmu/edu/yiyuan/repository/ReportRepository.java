package xmu.edu.yiyuan.repository;

import xmu.edu.yiyuan.entity.Report;
import java.util.List;
import java.util.Optional;

public interface ReportRepository {
    Optional<Report> findById(Long id);
    List<Report> findByPatientId(Long patientId);
    List<Report> findByDoctorId(Long doctorId);
    List<Report> findByAppointmentId(Long appointmentId);
    List<Report> findAll();
    Report save(Report report);
    void update(Report report);
    void deleteById(Long id);
}
