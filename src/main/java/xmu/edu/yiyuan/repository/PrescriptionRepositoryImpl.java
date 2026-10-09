package xmu.edu.yiyuan.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import xmu.edu.yiyuan.entity.Prescription;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class PrescriptionRepositoryImpl implements PrescriptionRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Prescription> prescriptionRowMapper = (rs, rowNum) -> {
        java.sql.ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        java.util.Set<String> columnNames = new java.util.HashSet<>();
        for (int i = 1; i <= columnCount; i++) {
            columnNames.add(metaData.getColumnName(i).toLowerCase());
        }

        Prescription prescription = new Prescription();
        prescription.setId(rs.getLong("id"));
        Long appointmentId = rs.getLong("appointment_id");
        if (!rs.wasNull()) {
            prescription.setAppointmentId(appointmentId);
        }
        prescription.setPatientId(rs.getLong("patient_id"));
        prescription.setDoctorId(rs.getLong("doctor_id"));
        prescription.setPrescriptionNumber(rs.getString("prescription_number"));

        String statusStr = rs.getString("status");
        if (statusStr != null) {
            try {
                prescription.setStatus(Prescription.PrescriptionStatus.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                prescription.setStatus(Prescription.PrescriptionStatus.PENDING);
            }
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            prescription.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp dispensedAt = rs.getTimestamp("dispensed_at");
        if (dispensedAt != null) {
            prescription.setDispensedAt(dispensedAt.toLocalDateTime());
        }

        Long dispensedBy = rs.getLong("dispensed_by");
        if (!rs.wasNull()) {
            prescription.setDispensedBy(dispensedBy);
        }

        prescription.setPatientSignature(rs.getString("patient_signature"));

        // 兼容旧表：paid/paid_at/collected_at 可能不存在
        if (columnNames.contains("paid")) {
            boolean paidVal = rs.getBoolean("paid");
            if (!rs.wasNull()) {
                prescription.setPaid(paidVal);
            }
        }
        if (columnNames.contains("paid_at")) {
            Timestamp paidAt = rs.getTimestamp("paid_at");
            if (paidAt != null) {
                prescription.setPaidAt(paidAt.toLocalDateTime());
            }
        }
        if (columnNames.contains("collected_at")) {
            Timestamp collectedAt = rs.getTimestamp("collected_at");
            if (collectedAt != null) {
                prescription.setCollectedAt(collectedAt.toLocalDateTime());
            }
        }
        return prescription;
    };

    private boolean checkColumnExists(String columnName) {
        try {
            // 使用 INFORMATION_SCHEMA 更可靠
            String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'prescription' AND COLUMN_NAME = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, columnName);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Optional<Prescription> findById(Long id) {
        try {
            String sql = "SELECT * FROM prescription WHERE id = ?";
            List<Prescription> prescriptions = jdbcTemplate.query(sql, prescriptionRowMapper, id);
            return prescriptions.isEmpty() ? Optional.empty() : Optional.of(prescriptions.get(0));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Prescription> findAll() {
        try {
            String sql = "SELECT * FROM prescription ORDER BY created_at DESC";
            return jdbcTemplate.query(sql, prescriptionRowMapper);
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public List<Prescription> findByPatientId(Long patientId) {
        try {
            String sql = "SELECT * FROM prescription WHERE patient_id = ? ORDER BY created_at DESC";
            return jdbcTemplate.query(sql, prescriptionRowMapper, patientId);
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public List<Prescription> findByPatientIdAndStatus(Long patientId, Prescription.PrescriptionStatus status) {
        try {
            String sql = "SELECT * FROM prescription WHERE patient_id = ? AND status = ? ORDER BY created_at DESC";
            return jdbcTemplate.query(sql, prescriptionRowMapper, patientId, status.name());
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public List<Prescription> findByAppointmentId(Long appointmentId) {
        try {
            String sql = "SELECT * FROM prescription WHERE appointment_id = ? ORDER BY created_at DESC";
            return jdbcTemplate.query(sql, prescriptionRowMapper, appointmentId);
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public List<Prescription> findByStatus(Prescription.PrescriptionStatus status) {
        try {
            String sql = "SELECT * FROM prescription WHERE status = ? ORDER BY created_at DESC";
            return jdbcTemplate.query(sql, prescriptionRowMapper, status.name());
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public List<Prescription> findByPrescriptionNumber(String prescriptionNumber) {
        try {
            String sql = "SELECT * FROM prescription WHERE prescription_number = ?";
            return jdbcTemplate.query(sql, prescriptionRowMapper, prescriptionNumber);
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public Prescription save(Prescription prescription) {
        try {
            if (prescription.getId() == null) {
                boolean hasPaid = checkColumnExists("paid");
                boolean hasPaidAt = checkColumnExists("paid_at");
                boolean hasCollectedAt = checkColumnExists("collected_at");

                java.util.List<String> cols = new java.util.ArrayList<>();
                java.util.List<Object> params = new java.util.ArrayList<>();

                cols.add("appointment_id"); params.add(prescription.getAppointmentId());
                cols.add("patient_id"); params.add(prescription.getPatientId());
                cols.add("doctor_id"); params.add(prescription.getDoctorId());
                cols.add("prescription_number");
                String prescriptionNumber = prescription.getPrescriptionNumber();
                if (prescriptionNumber == null || prescriptionNumber.trim().isEmpty()) {
                    prescriptionNumber = "RX" + System.currentTimeMillis();
                }
                params.add(prescriptionNumber);
                cols.add("status"); params.add(prescription.getStatus() != null ? prescription.getStatus().name() : Prescription.PrescriptionStatus.PENDING.name());
                cols.add("created_at"); params.add(prescription.getCreatedAt() != null ? Timestamp.valueOf(prescription.getCreatedAt()) : Timestamp.valueOf(LocalDateTime.now()));
                cols.add("dispensed_at"); params.add(prescription.getDispensedAt() != null ? Timestamp.valueOf(prescription.getDispensedAt()) : null);
                cols.add("dispensed_by"); params.add(prescription.getDispensedBy());
                cols.add("patient_signature"); params.add(prescription.getPatientSignature());

                if (hasPaid) { cols.add("paid"); params.add(prescription.getPaid() != null ? prescription.getPaid() : false); }
                if (hasPaidAt) { cols.add("paid_at"); params.add(prescription.getPaidAt() != null ? Timestamp.valueOf(prescription.getPaidAt()) : null); }
                if (hasCollectedAt) { cols.add("collected_at"); params.add(prescription.getCollectedAt() != null ? Timestamp.valueOf(prescription.getCollectedAt()) : null); }

                String placeholders = cols.stream().map(c -> "?").reduce((a, b) -> a + ", " + b).orElse("?");
                String sql = "INSERT INTO prescription (" + String.join(", ", cols) + ") VALUES (" + placeholders + ")";
                KeyHolder keyHolder = new GeneratedKeyHolder();
                jdbcTemplate.update(connection -> {
                    PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    for (int i = 0; i < params.size(); i++) {
                        Object val = params.get(i);
                        int idx = i + 1;
                        if (val instanceof Timestamp) {
                            ps.setTimestamp(idx, (Timestamp) val);
                        } else if (val instanceof Boolean) {
                            ps.setBoolean(idx, (Boolean) val);
                        } else if (val instanceof Long) {
                            ps.setObject(idx, val, java.sql.Types.BIGINT);
                        } else {
                            ps.setObject(idx, val);
                        }
                    }
                    return ps;
                }, keyHolder);
                if (keyHolder.getKey() != null) {
                    prescription.setId(keyHolder.getKey().longValue());
                }
            } else {
                update(prescription);
            }
        } catch (org.springframework.jdbc.BadSqlGrammarException e) {
            System.err.println("SQL错误详情: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("根本原因: " + e.getCause().getMessage());
            }
            if (e.getCause() instanceof java.sql.SQLSyntaxErrorException) {
                java.sql.SQLSyntaxErrorException sqlEx = (java.sql.SQLSyntaxErrorException) e.getCause();
                String errorMsg = sqlEx.getMessage();
                System.err.println("SQL语法错误: " + errorMsg);
                if (errorMsg != null && errorMsg.contains("doesn't exist")) {
                    System.err.println("警告：prescription 表不存在，请先执行 database/create-prescription-tables.sql 创建表");
                    throw new RuntimeException("数据库表 prescription 不存在，请先执行 database/create-prescription-tables.sql 创建表", e);
                }
            }
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            System.err.println("创建处方时发生未知错误: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        return prescription;
    }

    @Override
    public void update(Prescription prescription) {
        try {
            boolean hasPaid = checkColumnExists("paid");
            boolean hasPaidAt = checkColumnExists("paid_at");
            boolean hasCollectedAt = checkColumnExists("collected_at");

            StringBuilder sql = new StringBuilder("UPDATE prescription SET appointment_id = ?, patient_id = ?, doctor_id = ?, prescription_number = ?, status = ?, created_at = ?, dispensed_at = ?, dispensed_by = ?, patient_signature = ?");
            java.util.List<Object> params = new java.util.ArrayList<>();
            params.add(prescription.getAppointmentId());
            params.add(prescription.getPatientId());
            params.add(prescription.getDoctorId());
            params.add(prescription.getPrescriptionNumber());
            params.add(prescription.getStatus() != null ? prescription.getStatus().name() : Prescription.PrescriptionStatus.PENDING.name());
            params.add(prescription.getCreatedAt() != null ? Timestamp.valueOf(prescription.getCreatedAt()) : Timestamp.valueOf(LocalDateTime.now()));
            params.add(prescription.getDispensedAt() != null ? Timestamp.valueOf(prescription.getDispensedAt()) : null);
            params.add(prescription.getDispensedBy());
            params.add(prescription.getPatientSignature());

            if (hasPaid) { sql.append(", paid = ?"); params.add(prescription.getPaid() != null ? prescription.getPaid() : false); }
            if (hasPaidAt) { sql.append(", paid_at = ?"); params.add(prescription.getPaidAt() != null ? Timestamp.valueOf(prescription.getPaidAt()) : null); }
            if (hasCollectedAt) { sql.append(", collected_at = ?"); params.add(prescription.getCollectedAt() != null ? Timestamp.valueOf(prescription.getCollectedAt()) : null); }

            sql.append(" WHERE id = ?");
            params.add(prescription.getId());
            jdbcTemplate.update(sql.toString(), params.toArray());
        } catch (Exception e) {
            // 忽略错误
        }
    }

    @Override
    public void deleteById(Long id) {
        try {
            String sql = "DELETE FROM prescription WHERE id = ?";
            jdbcTemplate.update(sql, id);
        } catch (Exception e) {
            // 忽略错误
        }
    }
}
