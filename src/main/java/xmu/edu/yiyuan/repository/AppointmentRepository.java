package xmu.edu.yiyuan.repository;

import xmu.edu.yiyuan.entity.Appointment;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository {
    Optional<Appointment> findById(Long id);
    List<Appointment> findByPatientId(Long patientId);
    List<Appointment> findByDoctorId(Long doctorId);
    List<Appointment> findAll();
    Appointment save(Appointment appointment);
    void update(Appointment appointment);
    void deleteById(Long id);
}
