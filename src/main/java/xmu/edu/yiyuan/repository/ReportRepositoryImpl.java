package xmu.edu.yiyuan.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import xmu.edu.yiyuan.entity.Report;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ReportRepositoryImpl implements ReportRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Report> reportRowMapper = (rs, rowNum) -> {
        Report report = new Report();
        java.sql.ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        java.util.Set<String> columnNames = new java.util.HashSet<>();
        for (int i = 1; i <= columnCount; i++) {
            columnNames.add(metaData.getColumnName(i).toLowerCase());
        }

        report.setId(rs.getLong("id"));
        Long appointmentId = rs.getLong("appointment_id");
        if (!rs.wasNull()) {
            report.setAppointmentId(appointmentId);
        }
        report.setPatientId(rs.getLong("patient_id"));
        Long doctorId = rs.getLong("doctor_id");
        if (!rs.wasNull()) {
            report.setDoctorId(doctorId);
        }
        Long examinationId = rs.getLong("examination_id");
        if (!rs.wasNull()) {
            report.setExaminationId(examinationId);
        }
        report.setReportType(rs.getString("report_type"));
        report.setReportContent(rs.getString("report_content"));
        Timestamp reportDate = rs.getTimestamp("report_date");
        if (reportDate != null) {
            report.setReportDate(reportDate.toLocalDateTime());
        }
        report.setStatus(Report.ReportStatus.valueOf(rs.getString("status")));

        if (columnNames.contains("paid")) {
            Object po = rs.getObject("paid");
            if (po != null) {
                report.setPaid(rs.getBoolean("paid"));
            }
        }
        if (columnNames.contains("paid_at")) {
            Timestamp pat = rs.getTimestamp("paid_at");
            if (pat != null) {
                report.setPaidAt(pat.toLocalDateTime());
            }
        }
        return report;
    };

    @Override
    public Optional<Report> findById(Long id) {
        String sql = "SELECT * FROM report WHERE id = ?";
        List<Report> reports = jdbcTemplate.query(sql, reportRowMapper, id);
        return reports.isEmpty() ? Optional.empty() : Optional.of(reports.get(0));
    }

    @Override
    public List<Report> findByPatientId(Long patientId) {
        String sql = "SELECT * FROM report WHERE patient_id = ? ORDER BY report_date DESC";
        return jdbcTemplate.query(sql, reportRowMapper, patientId);
    }

    @Override
    public List<Report> findByDoctorId(Long doctorId) {
        String sql = "SELECT * FROM report WHERE doctor_id = ? ORDER BY report_date DESC";
        return jdbcTemplate.query(sql, reportRowMapper, doctorId);
    }

    @Override
    public List<Report> findByAppointmentId(Long appointmentId) {
        String sql = "SELECT * FROM report WHERE appointment_id = ? ORDER BY report_date DESC";
        return jdbcTemplate.query(sql, reportRowMapper, appointmentId);
    }

    @Override
    public List<Report> findAll() {
        String sql = "SELECT * FROM report ORDER BY report_date DESC";
        return jdbcTemplate.query(sql, reportRowMapper);
    }

    @Override
    public Report save(Report report) {
        if (report.getId() == null) {
            boolean hasPaid = checkColumnExists("paid");
            boolean hasPaidAt = checkColumnExists("paid_at");

            String sql = "INSERT INTO report (appointment_id, patient_id, doctor_id, examination_id, report_type, report_content, report_date, status";
            StringBuilder qmarks = new StringBuilder("?, ?, ?, ?, ?, ?, ?, ?");
            java.util.List<Object> params = new java.util.ArrayList<>();
            if (report.getAppointmentId() != null) {
                params.add(report.getAppointmentId());
            } else {
                params.add(null);
            }
            params.add(report.getPatientId());
            params.add(report.getDoctorId());
            params.add(report.getExaminationId());
            params.add(report.getReportType());
            params.add(report.getReportContent());
            params.add(report.getReportDate() != null ? Timestamp.valueOf(report.getReportDate()) : Timestamp.valueOf(LocalDateTime.now()));
            params.add(report.getStatus().name());
            if (hasPaid) {
                sql += ", paid";
                qmarks.append(", ?");
                params.add(report.getPaid());
            }
            if (hasPaidAt) {
                sql += ", paid_at";
                qmarks.append(", ?");
                params.add(report.getPaidAt() != null ? Timestamp.valueOf(report.getPaidAt()) : null);
            }
            sql += ") VALUES (" + qmarks + ")";

            KeyHolder keyHolder = new GeneratedKeyHolder();
            String finalSql = sql;
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(finalSql, Statement.RETURN_GENERATED_KEYS);
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }
                return ps;
            }, keyHolder);
            report.setId(keyHolder.getKey().longValue());
        } else {
            update(report);
        }
        return report;
    }

    @Override
    public void update(Report report) {
        boolean hasPaid = checkColumnExists("paid");
        boolean hasPaidAt = checkColumnExists("paid_at");

        StringBuilder sql = new StringBuilder(
                "UPDATE report SET appointment_id = ?, patient_id = ?, doctor_id = ?, examination_id = ?, report_type = ?, report_content = ?, report_date = ?, status = ?");
        java.util.List<Object> params = new java.util.ArrayList<>();
        params.add(report.getAppointmentId());
        params.add(report.getPatientId());
        params.add(report.getDoctorId());
        params.add(report.getExaminationId());
        params.add(report.getReportType());
        params.add(report.getReportContent());
        params.add(report.getReportDate() != null ? Timestamp.valueOf(report.getReportDate()) : Timestamp.valueOf(LocalDateTime.now()));
        params.add(report.getStatus().name());
        if (hasPaid) {
            sql.append(", paid = ?");
            params.add(report.getPaid());
        }
        if (hasPaidAt) {
            sql.append(", paid_at = ?");
            params.add(report.getPaidAt() != null ? Timestamp.valueOf(report.getPaidAt()) : null);
        }
        sql.append(" WHERE id = ?");
        params.add(report.getId());

        jdbcTemplate.update(sql.toString(), params.toArray());
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM report WHERE id = ?";
        jdbcTemplate.update(sql, id);
        resetAutoIncrementIfEmpty();
    }

    private boolean checkColumnExists(String columnName) {
        try {
            String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                    + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report' AND COLUMN_NAME = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, columnName);
            return count != null && count > 0;
        } catch (Exception e) {
            try {
                jdbcTemplate.queryForObject("SELECT " + columnName + " FROM report LIMIT 1", Object.class);
                return true;
            } catch (Exception e2) {
                return false;
            }
        }
    }

    private void resetAutoIncrementIfEmpty() {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM report", Integer.class);
            if (count != null && count == 0) {
                jdbcTemplate.execute("ALTER TABLE report AUTO_INCREMENT = 1");
            }
        } catch (Exception ignored) {
        }
    }
}
