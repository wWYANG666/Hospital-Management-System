package xmu.edu.yiyuan.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import xmu.edu.yiyuan.entity.Hospitalization;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class HospitalizationRepositoryImpl implements HospitalizationRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public HospitalizationRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Hospitalization> hospitalizationRowMapper = (rs, rowNum) -> {
        Hospitalization h = new Hospitalization();
        java.sql.ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        java.util.Set<String> columnNames = new java.util.HashSet<>();
        for (int i = 1; i <= columnCount; i++) {
            columnNames.add(metaData.getColumnName(i).toLowerCase());
        }

        h.setId(rs.getLong("id"));
        h.setPatientId(rs.getLong("patient_id"));
        Long doctorId = rs.getLong("doctor_id");
        if (!rs.wasNull()) {
            h.setDoctorId(doctorId);
        }
        Long bedId = rs.getLong("bed_id");
        if (!rs.wasNull()) {
            h.setBedId(bedId);
        }
        h.setAdmissionDate(rs.getDate("admission_date").toLocalDate());
        java.sql.Date dischargeDate = rs.getDate("discharge_date");
        if (dischargeDate != null) {
            h.setDischargeDate(dischargeDate.toLocalDate());
        }
        h.setDiagnosis(rs.getString("diagnosis"));

        // 安全读取可能不存在的字段
        if (columnNames.contains("admission_reason")) {
            h.setAdmissionReason(rs.getString("admission_reason"));
        }
        if (columnNames.contains("expected_days")) {
            Integer expectedDays = rs.getInt("expected_days");
            if (!rs.wasNull()) {
                h.setExpectedDays(expectedDays);
            }
        }
        if (columnNames.contains("request_department_id")) {
            Long requestDeptId = rs.getLong("request_department_id");
            if (!rs.wasNull()) {
                h.setRequestDepartmentId(requestDeptId);
            }
        }
        if (columnNames.contains("request_time")) {
            Timestamp requestTime = rs.getTimestamp("request_time");
            if (requestTime != null) {
                h.setRequestTime(requestTime.toLocalDateTime());
            }
        }
        if (columnNames.contains("request_status")) {
            String requestStatus = rs.getString("request_status");
            if (requestStatus != null) {
                h.setRequestStatus(Hospitalization.RequestStatus.valueOf(requestStatus));
            }
        }
        if (columnNames.contains("appointment_id")) {
            long appointmentId = rs.getLong("appointment_id");
            if (!rs.wasNull()) {
                h.setAppointmentId(appointmentId);
            }
        }
        if (columnNames.contains("discharge_diagnosis")) {
            h.setDischargeDiagnosis(rs.getString("discharge_diagnosis"));
        }
        if (columnNames.contains("discharge_notes")) {
            h.setDischargeNotes(rs.getString("discharge_notes"));
        }

        h.setStatus(Hospitalization.HospitalizationStatus.valueOf(rs.getString("status")));
        BigDecimal totalCost = rs.getBigDecimal("total_cost");
        if (totalCost != null) {
            h.setTotalCost(totalCost);
        }
        if (columnNames.contains("paid")) {
            boolean paidVal = rs.getBoolean("paid");
            if (!rs.wasNull()) {
                h.setPaid(paidVal);
            }
        }
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            h.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            h.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return h;
    };

    @Override
    public Optional<Hospitalization> findById(Long id) {
        String sql = "SELECT * FROM hospitalization WHERE id = ?";
        List<Hospitalization> hospitalizations = jdbcTemplate.query(sql, hospitalizationRowMapper, id);
        return hospitalizations.isEmpty() ? Optional.empty() : Optional.of(hospitalizations.get(0));
    }

    @Override
    public List<Hospitalization> findByPatientId(Long patientId) {
        String sql = "SELECT * FROM hospitalization WHERE patient_id = ? ORDER BY admission_date DESC";
        return jdbcTemplate.query(sql, hospitalizationRowMapper, patientId);
    }

    @Override
    public List<Hospitalization> findByDoctorId(Long doctorId) {
        String sql = "SELECT * FROM hospitalization WHERE doctor_id = ? ORDER BY admission_date DESC";
        return jdbcTemplate.query(sql, hospitalizationRowMapper, doctorId);
    }

    @Override
    public List<Hospitalization> findByBedId(Long bedId) {
        String sql = "SELECT * FROM hospitalization WHERE bed_id = ? ORDER BY admission_date DESC";
        return jdbcTemplate.query(sql, hospitalizationRowMapper, bedId);
    }

    @Override
    public List<Hospitalization> findAll() {
        String sql = "SELECT * FROM hospitalization ORDER BY admission_date DESC";
        return jdbcTemplate.query(sql, hospitalizationRowMapper);
    }

    @Override
    public Hospitalization save(Hospitalization h) {
        if (h.getId() != null) { update(h); return h; }
        String sql = "INSERT INTO hospitalization (patient_id,doctor_id,appointment_id,bed_id,admission_date,"
                + "discharge_date,diagnosis,admission_reason,expected_days,request_department_id,request_time,"
                + "request_status,discharge_diagnosis,discharge_notes,status,total_cost,paid,created_at,updated_at) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW())";
        Object[] values = values(h);
        KeyHolder holder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            return statement;
        }, holder);
        h.setId(holder.getKey().longValue());
        return h;
    }

    @Override
    public void update(Hospitalization h) {
        String sql = "UPDATE hospitalization SET patient_id=?,doctor_id=?,appointment_id=?,bed_id=?,admission_date=?,"
                + "discharge_date=?,diagnosis=?,admission_reason=?,expected_days=?,request_department_id=?,request_time=?,"
                + "request_status=?,discharge_diagnosis=?,discharge_notes=?,status=?,total_cost=?,paid=?,updated_at=NOW() WHERE id=?";
        java.util.List<Object> args = new java.util.ArrayList<>(java.util.Arrays.asList(values(h)));
        args.add(h.getId());
        if (jdbcTemplate.update(sql, args.toArray()) != 1) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "住院记录不存在");
        }
    }

    private Object[] values(Hospitalization h) {
        return new Object[] { h.getPatientId(), h.getDoctorId(), h.getAppointmentId(), h.getBedId(),
                h.getAdmissionDate(), h.getDischargeDate(), h.getDiagnosis(), h.getAdmissionReason(),
                h.getExpectedDays(), h.getRequestDepartmentId(), h.getRequestTime(),
                h.getRequestStatus() == null ? null : h.getRequestStatus().name(),
                h.getDischargeDiagnosis(), h.getDischargeNotes(), h.getStatus().name(),
                h.getTotalCost(), Boolean.TRUE.equals(h.getPaid()) };
    }

    @Override
    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM hospitalization WHERE id=?", id);
    }
}
