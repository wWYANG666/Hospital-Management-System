package xmu.edu.yiyuan.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import xmu.edu.yiyuan.entity.Schedule;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class ScheduleRepositoryImpl implements ScheduleRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 用于兼容旧库：旧的 schedule 表没有 schedule_type 字段。
     * 这里采用“尝试写入新字段，失败则降级”的策略，避免上线时因未执行升级脚本导致系统不可用。
     */
    private volatile Boolean scheduleTypeColumnSupported = null;

    private final RowMapper<Schedule> scheduleRowMapper = (rs, rowNum) -> {
        Schedule schedule = new Schedule();
        schedule.setId(rs.getLong("id"));
        schedule.setDoctorId(rs.getLong("doctor_id"));
        Long departmentId = rs.getLong("department_id");
        if (!rs.wasNull()) {
            schedule.setDepartmentId(departmentId);
        }
        schedule.setWorkDate(rs.getDate("work_date").toLocalDate());
        schedule.setWorkTime(Schedule.WorkTime.valueOf(rs.getString("work_time")));
        schedule.setMaxAppointments(rs.getInt("max_appointments"));
        schedule.setCurrentAppointments(rs.getInt("current_appointments"));
        schedule.setStatus(rs.getInt("status"));
        try {
            String scheduleType = rs.getString("schedule_type");
            schedule.setScheduleType(scheduleType != null ? Schedule.ScheduleType.valueOf(scheduleType) : Schedule.ScheduleType.GENERAL);
        } catch (Exception ignored) {
            schedule.setScheduleType(Schedule.ScheduleType.GENERAL);
        }
        return schedule;
    };

    @Override
    public Optional<Schedule> findById(Long id) {
        String sql = "SELECT * FROM schedule WHERE id = ?";
        List<Schedule> schedules = jdbcTemplate.query(sql, scheduleRowMapper, id);
        return schedules.isEmpty() ? Optional.empty() : Optional.of(schedules.get(0));
    }

    @Override
    public List<Schedule> findByDoctorId(Long doctorId) {
        String sql = "SELECT * FROM schedule WHERE doctor_id = ? ORDER BY work_date DESC, work_time";
        return jdbcTemplate.query(sql, scheduleRowMapper, doctorId);
    }

    @Override
    public List<Schedule> findByDepartmentId(Long departmentId) {
        String sql = "SELECT * FROM schedule WHERE department_id = ? ORDER BY work_date DESC, work_time";
        return jdbcTemplate.query(sql, scheduleRowMapper, departmentId);
    }

    @Override
    public List<Schedule> findByDoctorIdAndDate(Long doctorId, LocalDate date) {
        String sql = "SELECT * FROM schedule WHERE doctor_id = ? AND work_date = ? ORDER BY work_time";
        return jdbcTemplate.query(sql, scheduleRowMapper, doctorId, java.sql.Date.valueOf(date));
    }

    @Override
    public List<Schedule> findAvailableSchedules(Long departmentId, LocalDate date) {
        String sql = "SELECT * FROM schedule WHERE department_id = ? AND work_date = ? AND status = 1 AND current_appointments < max_appointments ORDER BY work_time";
        return jdbcTemplate.query(sql, scheduleRowMapper, departmentId, java.sql.Date.valueOf(date));
    }

    @Override
    public List<Schedule> findAll() {
        String sql = "SELECT * FROM schedule ORDER BY work_date DESC, work_time";
        return jdbcTemplate.query(sql, scheduleRowMapper);
    }

    @Override
    public Schedule save(Schedule schedule) {
        if (schedule.getId() == null) {
            boolean tryNew = scheduleTypeColumnSupported == null || Boolean.TRUE.equals(scheduleTypeColumnSupported);
            if (tryNew) {
                try {
                    String sql = "INSERT INTO schedule (doctor_id, department_id, work_date, work_time, max_appointments, current_appointments, status, schedule_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                    KeyHolder keyHolder = new GeneratedKeyHolder();
                    jdbcTemplate.update(connection -> {
                        PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                        ps.setLong(1, schedule.getDoctorId());
                        if (schedule.getDepartmentId() != null) {
                            ps.setLong(2, schedule.getDepartmentId());
                        } else {
                            ps.setNull(2, java.sql.Types.BIGINT);
                        }
                        ps.setDate(3, java.sql.Date.valueOf(schedule.getWorkDate()));
                        ps.setString(4, schedule.getWorkTime().name());
                        ps.setInt(5, schedule.getMaxAppointments());
                        ps.setInt(6, schedule.getCurrentAppointments());
                        ps.setInt(7, schedule.getStatus());
                        ps.setString(8, schedule.getScheduleType() != null ? schedule.getScheduleType().name() : Schedule.ScheduleType.GENERAL.name());
                        return ps;
                    }, keyHolder);
                    schedule.setId(keyHolder.getKey().longValue());
                    scheduleTypeColumnSupported = true;
                    return schedule;
                } catch (BadSqlGrammarException e) {
                    // 典型场景：Unknown column 'schedule_type' in 'field list'
                    scheduleTypeColumnSupported = false;
                }
            }

            // 旧库降级写入（无 schedule_type）
            String sqlOld = "INSERT INTO schedule (doctor_id, department_id, work_date, work_time, max_appointments, current_appointments, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
            KeyHolder keyHolderOld = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sqlOld, Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, schedule.getDoctorId());
                if (schedule.getDepartmentId() != null) {
                    ps.setLong(2, schedule.getDepartmentId());
                } else {
                    ps.setNull(2, java.sql.Types.BIGINT);
                }
                ps.setDate(3, java.sql.Date.valueOf(schedule.getWorkDate()));
                ps.setString(4, schedule.getWorkTime().name());
                ps.setInt(5, schedule.getMaxAppointments());
                ps.setInt(6, schedule.getCurrentAppointments());
                ps.setInt(7, schedule.getStatus());
                return ps;
            }, keyHolderOld);
            schedule.setId(keyHolderOld.getKey().longValue());
        } else {
            update(schedule);
        }
        return schedule;
    }

    @Override
    public void update(Schedule schedule) {
        boolean tryNew = scheduleTypeColumnSupported == null || Boolean.TRUE.equals(scheduleTypeColumnSupported);
        if (tryNew) {
            try {
                String sql = "UPDATE schedule SET doctor_id = ?, department_id = ?, work_date = ?, work_time = ?, max_appointments = ?, current_appointments = ?, status = ?, schedule_type = ? WHERE id = ?";
                jdbcTemplate.update(sql,
                        schedule.getDoctorId(),
                        schedule.getDepartmentId(),
                        java.sql.Date.valueOf(schedule.getWorkDate()),
                        schedule.getWorkTime().name(),
                        schedule.getMaxAppointments(),
                        schedule.getCurrentAppointments(),
                        schedule.getStatus(),
                        schedule.getScheduleType() != null ? schedule.getScheduleType().name() : Schedule.ScheduleType.GENERAL.name(),
                        schedule.getId());
                scheduleTypeColumnSupported = true;
                return;
            } catch (BadSqlGrammarException e) {
                scheduleTypeColumnSupported = false;
            }
        }

        // 旧库降级更新（无 schedule_type）
        String sqlOld = "UPDATE schedule SET doctor_id = ?, department_id = ?, work_date = ?, work_time = ?, max_appointments = ?, current_appointments = ?, status = ? WHERE id = ?";
        jdbcTemplate.update(sqlOld,
                schedule.getDoctorId(),
                schedule.getDepartmentId(),
                java.sql.Date.valueOf(schedule.getWorkDate()),
                schedule.getWorkTime().name(),
                schedule.getMaxAppointments(),
                schedule.getCurrentAppointments(),
                schedule.getStatus(),
                schedule.getId());
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM schedule WHERE id = ?";
        jdbcTemplate.update(sql, id);
        resetAutoIncrementIfEmpty();
    }

    private void resetAutoIncrementIfEmpty() {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM schedule", Integer.class);
            if (count != null && count == 0) {
                jdbcTemplate.execute("ALTER TABLE schedule AUTO_INCREMENT = 1");
            }
        } catch (Exception ignored) {
        }
    }
}
