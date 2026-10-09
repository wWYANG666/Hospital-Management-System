package xmu.edu.yiyuan.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import xmu.edu.yiyuan.entity.Appointment;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class AppointmentRepositoryImpl implements AppointmentRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Appointment> appointmentRowMapper = (rs, rowNum) -> {
        Appointment appointment = new Appointment();
        appointment.setId(rs.getLong("id"));
        appointment.setPatientId(rs.getLong("patient_id"));
        appointment.setDoctorId(rs.getLong("doctor_id"));
        Long scheduleId = rs.getLong("schedule_id");
        if (!rs.wasNull()) {
            appointment.setScheduleId(scheduleId);
        }
        appointment.setAppointmentDate(rs.getDate("appointment_date").toLocalDate());
        java.sql.Time time = rs.getTime("appointment_time");
        if (time != null) {
            appointment.setAppointmentTime(time.toLocalTime());
        }
        appointment.setStatus(Appointment.AppointmentStatus.valueOf(rs.getString("status")));
        appointment.setSymptoms(rs.getString("symptoms"));
        appointment.setDiagnosis(rs.getString("diagnosis"));
        appointment.setPrescription(rs.getString("prescription"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            appointment.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            appointment.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return appointment;
    };

    @Override
    public Optional<Appointment> findById(Long id) {
        String sql = "SELECT * FROM appointment WHERE id = ?";
        List<Appointment> appointments = jdbcTemplate.query(sql, appointmentRowMapper, id);
        return appointments.isEmpty() ? Optional.empty() : Optional.of(appointments.get(0));
    }

    @Override
    public List<Appointment> findByPatientId(Long patientId) {
        String sql = "SELECT * FROM appointment WHERE patient_id = ? ORDER BY appointment_date DESC, appointment_time DESC";
        return jdbcTemplate.query(sql, appointmentRowMapper, patientId);
    }

    @Override
    public List<Appointment> findByDoctorId(Long doctorId) {
        String sql = "SELECT * FROM appointment WHERE doctor_id = ? ORDER BY appointment_date DESC, appointment_time DESC";
        return jdbcTemplate.query(sql, appointmentRowMapper, doctorId);
    }

    @Override
    public List<Appointment> findAll() {
        String sql = "SELECT * FROM appointment ORDER BY appointment_date DESC, appointment_time DESC";
        return jdbcTemplate.query(sql, appointmentRowMapper);
    }

    @Override
    public Appointment save(Appointment appointment) {
        if (appointment.getId() == null) {
            String sql = "INSERT INTO appointment (patient_id, doctor_id, schedule_id, appointment_date, appointment_time, status, symptoms, diagnosis, prescription, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, appointment.getPatientId());
                ps.setLong(2, appointment.getDoctorId());
                if (appointment.getScheduleId() != null) {
                    ps.setLong(3, appointment.getScheduleId());
                } else {
                    ps.setNull(3, java.sql.Types.BIGINT);
                }
                ps.setDate(4, java.sql.Date.valueOf(appointment.getAppointmentDate()));
                if (appointment.getAppointmentTime() != null) {
                    ps.setTime(5, java.sql.Time.valueOf(appointment.getAppointmentTime()));
                } else {
                    ps.setNull(5, java.sql.Types.TIME);
                }
                ps.setString(6, appointment.getStatus().name());
                ps.setString(7, appointment.getSymptoms());
                ps.setString(8, appointment.getDiagnosis());
                ps.setString(9, appointment.getPrescription());
                Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                ps.setTimestamp(10, now);
                ps.setTimestamp(11, now);
                return ps;
            }, keyHolder);
            appointment.setId(keyHolder.getKey().longValue());
        } else {
            update(appointment);
        }
        return appointment;
    }

    @Override
    public void update(Appointment appointment) {
        String sql = "UPDATE appointment SET patient_id = ?, doctor_id = ?, schedule_id = ?, appointment_date = ?, appointment_time = ?, status = ?, symptoms = ?, diagnosis = ?, prescription = ?, updated_at = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                appointment.getPatientId(),
                appointment.getDoctorId(),
                appointment.getScheduleId(),
                java.sql.Date.valueOf(appointment.getAppointmentDate()),
                appointment.getAppointmentTime() != null ? java.sql.Time.valueOf(appointment.getAppointmentTime()) : null,
                appointment.getStatus().name(),
                appointment.getSymptoms(),
                appointment.getDiagnosis(),
                appointment.getPrescription(),
                Timestamp.valueOf(LocalDateTime.now()),
                appointment.getId());
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM appointment WHERE id = ?";
        jdbcTemplate.update(sql, id);
        // 删除后如果表为空，重置自增
        resetAutoIncrementIfEmpty();
    }

    private void resetAutoIncrementIfEmpty() {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM appointment", Integer.class);
            if (count != null && count == 0) {
                jdbcTemplate.execute("ALTER TABLE appointment AUTO_INCREMENT = 1");
            }
        } catch (Exception ignored) {
            // 没有权限或数据库不支持时忽略，不影响主流程
        }
    }
}
