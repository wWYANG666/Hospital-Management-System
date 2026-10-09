package xmu.edu.yiyuan.repository;

import xmu.edu.yiyuan.entity.Schedule;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository {
    Optional<Schedule> findById(Long id);
    List<Schedule> findByDoctorId(Long doctorId);
    List<Schedule> findByDepartmentId(Long departmentId);
    List<Schedule> findByDoctorIdAndDate(Long doctorId, LocalDate date);
    List<Schedule> findAvailableSchedules(Long departmentId, LocalDate date);
    List<Schedule> findAll();
    Schedule save(Schedule schedule);
    void update(Schedule schedule);
    void deleteById(Long id);
}
