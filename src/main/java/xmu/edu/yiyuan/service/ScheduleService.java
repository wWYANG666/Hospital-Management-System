package xmu.edu.yiyuan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xmu.edu.yiyuan.entity.Schedule;
import xmu.edu.yiyuan.repository.ScheduleRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ScheduleService {

    @Autowired
    private ScheduleRepository scheduleRepository;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;

    public Optional<Schedule> findById(Long id) {
        return scheduleRepository.findById(id);
    }

    public List<Schedule> findByDoctorId(Long doctorId) {
        return scheduleRepository.findByDoctorId(doctorId);
    }

    public List<Schedule> findByDepartmentId(Long departmentId) {
        return scheduleRepository.findByDepartmentId(departmentId);
    }

    public List<Schedule> findByDoctorIdAndDate(Long doctorId, LocalDate date) {
        return scheduleRepository.findByDoctorIdAndDate(doctorId, date);
    }

    public List<Schedule> findAvailableSchedules(Long departmentId, LocalDate date) {
        return scheduleRepository.findAvailableSchedules(departmentId, date);
    }

    public List<Schedule> findAll() {
        return scheduleRepository.findAll();
    }

    @org.springframework.transaction.annotation.Transactional
    public Schedule save(Schedule schedule) {
        BusinessValidation.stock(schedule.getMaxAppointments(), "号源容量");
        BusinessValidation.require(schedule.getWorkDate() != null && schedule.getWorkTime() != null && schedule.getScheduleType() != null, "请完整填写排班日期和类型");
        if (jdbc.queryForList("SELECT id FROM doctor WHERE id=? FOR UPDATE", Long.class, schedule.getDoctorId()).isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "医生不存在");
        }
        Integer matches = jdbc.queryForObject("SELECT COUNT(*) FROM doctor d JOIN department p ON d.department=p.name WHERE d.id=? AND p.id=?", Integer.class, schedule.getDoctorId(), schedule.getDepartmentId());
        BusinessValidation.require(matches != null && matches == 1, "医生与排班科室不匹配");
        Integer duplicate = jdbc.queryForObject("SELECT COUNT(*) FROM schedule WHERE doctor_id=? AND work_date=? AND work_time=? AND schedule_type=? AND (? IS NULL OR id<>?)", Integer.class,
                schedule.getDoctorId(), schedule.getWorkDate(), schedule.getWorkTime().name(), schedule.getScheduleType().name(), schedule.getId(), schedule.getId());
        if (duplicate != null && duplicate > 0) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "该医生同日同班次号源已存在");
        return scheduleRepository.save(schedule);
    }

    public void update(Schedule schedule) {
        scheduleRepository.update(schedule);
    }

    public void deleteById(Long id) {
        scheduleRepository.deleteById(id);
    }
}
