package xmu.edu.yiyuan.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import xmu.edu.yiyuan.entity.MedicalOrder;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class MedicalOrderRepositoryImpl implements MedicalOrderRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** 缓存：表是否有 quantity 列（老库迁移见 docs/DATABASE.md） */
    private Boolean hasQuantityColumnMemo;

    private boolean hasQuantityColumn() {
        if (hasQuantityColumnMemo == null) {
            hasQuantityColumnMemo = checkColumnExists("quantity");
        }
        if (!hasQuantityColumnMemo) {
            throw new IllegalStateException("数据库缺少 medical_order.quantity，请先完成业务数据库迁移");
        }
        return hasQuantityColumnMemo;
    }

    private boolean checkColumnExists(String columnName) {
        try {
            String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                    + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical_order' AND COLUMN_NAME = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, columnName);
            return count != null && count > 0;
        } catch (Exception e) {
            try {
                jdbcTemplate.queryForObject("SELECT " + columnName + " FROM medical_order LIMIT 1", Object.class);
                return true;
            } catch (Exception e2) {
                return false;
            }
        }
    }

    private final RowMapper<MedicalOrder> medicalOrderRowMapper = (rs, rowNum) -> {
        MedicalOrder order = new MedicalOrder();
        java.sql.ResultSetMetaData metaData = rs.getMetaData();
        java.util.Set<String> columnNames = new java.util.HashSet<>();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            columnNames.add(metaData.getColumnName(i).toLowerCase());
        }
        order.setId(rs.getLong("id"));
        order.setHospitalizationId(rs.getLong("hospitalization_id"));
        order.setPatientId(rs.getLong("patient_id"));
        Long doctorId = rs.getLong("doctor_id");
        if (!rs.wasNull()) {
            order.setDoctorId(doctorId);
        }
        order.setOrderType(MedicalOrder.OrderType.valueOf(rs.getString("order_type")));
        order.setOrderContent(rs.getString("order_content"));
        Long medicineId = rs.getLong("medicine_id");
        if (!rs.wasNull()) {
            order.setMedicineId(medicineId);
        }
        if (columnNames.contains("quantity")) {
            int q = rs.getInt("quantity");
            if (!rs.wasNull()) {
                order.setQuantity(q);
            }
        }
        Long examinationId = rs.getLong("examination_id");
        if (!rs.wasNull()) {
            order.setExaminationId(examinationId);
        }
        order.setDosage(rs.getString("dosage"));
        order.setFrequency(rs.getString("frequency"));
        Timestamp startTime = rs.getTimestamp("start_time");
        if (startTime != null) {
            order.setStartTime(startTime.toLocalDateTime());
        }
        Timestamp endTime = rs.getTimestamp("end_time");
        if (endTime != null) {
            order.setEndTime(endTime.toLocalDateTime());
        }
        order.setStatus(MedicalOrder.OrderStatus.valueOf(rs.getString("status")));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            order.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            order.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return order;
    };

    @Override
    public Optional<MedicalOrder> findById(Long id) {
        String sql = "SELECT * FROM medical_order WHERE id = ?";
        List<MedicalOrder> orders = jdbcTemplate.query(sql, medicalOrderRowMapper, id);
        return orders.isEmpty() ? Optional.empty() : Optional.of(orders.get(0));
    }

    @Override
    public List<MedicalOrder> findByHospitalizationId(Long hospitalizationId) {
        try {
            String sql = "SELECT * FROM medical_order WHERE hospitalization_id = ? ORDER BY created_at DESC";
            return jdbcTemplate.query(sql, medicalOrderRowMapper, hospitalizationId);
        } catch (DataAccessException e) {
            // 如果表不存在，返回空列表
            if (e.getMessage() != null && e.getMessage().contains("doesn't exist")) {
                System.err.println("警告: medical_order 表不存在，请按 docs/DATABASE.md 执行 database/init.sql");
                return new ArrayList<>();
            }
            throw e;
        }
    }

    @Override
    public List<MedicalOrder> findByPatientId(Long patientId) {
        try {
            String sql = "SELECT * FROM medical_order WHERE patient_id = ? ORDER BY created_at DESC";
            return jdbcTemplate.query(sql, medicalOrderRowMapper, patientId);
        } catch (DataAccessException e) {
            if (e.getMessage() != null && e.getMessage().contains("doesn't exist")) {
                return new ArrayList<>();
            }
            throw e;
        }
    }

    @Override
    public List<MedicalOrder> findByDoctorId(Long doctorId) {
        try {
            String sql = "SELECT * FROM medical_order WHERE doctor_id = ? ORDER BY created_at DESC";
            return jdbcTemplate.query(sql, medicalOrderRowMapper, doctorId);
        } catch (DataAccessException e) {
            if (e.getMessage() != null && e.getMessage().contains("doesn't exist")) {
                return new ArrayList<>();
            }
            throw e;
        }
    }

    @Override
    public List<MedicalOrder> findAll() {
        try {
            String sql = "SELECT * FROM medical_order ORDER BY created_at DESC";
            return jdbcTemplate.query(sql, medicalOrderRowMapper);
        } catch (DataAccessException e) {
            if (e.getMessage() != null && e.getMessage().contains("doesn't exist")) {
                return new ArrayList<>();
            }
            throw e;
        }
    }

    @Override
    public MedicalOrder save(MedicalOrder medicalOrder) {
        if (medicalOrder.getId() == null) {
            final boolean hq = hasQuantityColumn();
            String sql = hq
                    ? "INSERT INTO medical_order (hospitalization_id, patient_id, doctor_id, order_type, order_content, medicine_id, quantity, examination_id, dosage, frequency, start_time, end_time, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    : "INSERT INTO medical_order (hospitalization_id, patient_id, doctor_id, order_type, order_content, medicine_id, examination_id, dosage, frequency, start_time, end_time, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                int i = 1;
                ps.setLong(i++, medicalOrder.getHospitalizationId());
                ps.setLong(i++, medicalOrder.getPatientId());
                if (medicalOrder.getDoctorId() != null) {
                    ps.setLong(i++, medicalOrder.getDoctorId());
                } else {
                    ps.setNull(i++, java.sql.Types.BIGINT);
                }
                ps.setString(i++, medicalOrder.getOrderType().name());
                ps.setString(i++, medicalOrder.getOrderContent());
                if (medicalOrder.getMedicineId() != null) {
                    ps.setLong(i++, medicalOrder.getMedicineId());
                } else {
                    ps.setNull(i++, java.sql.Types.BIGINT);
                }
                if (hq) {
                    if (medicalOrder.getQuantity() != null) {
                        ps.setInt(i++, medicalOrder.getQuantity());
                    } else {
                        ps.setNull(i++, java.sql.Types.INTEGER);
                    }
                }
                if (medicalOrder.getExaminationId() != null) {
                    ps.setLong(i++, medicalOrder.getExaminationId());
                } else {
                    ps.setNull(i++, java.sql.Types.BIGINT);
                }
                ps.setString(i++, medicalOrder.getDosage());
                ps.setString(i++, medicalOrder.getFrequency());
                if (medicalOrder.getStartTime() != null) {
                    ps.setTimestamp(i++, Timestamp.valueOf(medicalOrder.getStartTime()));
                } else {
                    ps.setNull(i++, java.sql.Types.TIMESTAMP);
                }
                if (medicalOrder.getEndTime() != null) {
                    ps.setTimestamp(i++, Timestamp.valueOf(medicalOrder.getEndTime()));
                } else {
                    ps.setNull(i++, java.sql.Types.TIMESTAMP);
                }
                ps.setString(i++, medicalOrder.getStatus().name());
                Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                ps.setTimestamp(i++, now);
                ps.setTimestamp(i++, now);
                return ps;
            }, keyHolder);
            medicalOrder.setId(keyHolder.getKey().longValue());
        } else {
            update(medicalOrder);
        }
        return medicalOrder;
    }

    @Override
    public void update(MedicalOrder medicalOrder) {
        final boolean hq = hasQuantityColumn();
        String sql = hq
                ? "UPDATE medical_order SET hospitalization_id = ?, patient_id = ?, doctor_id = ?, order_type = ?, order_content = ?, medicine_id = ?, quantity = ?, examination_id = ?, dosage = ?, frequency = ?, start_time = ?, end_time = ?, status = ?, updated_at = ? WHERE id = ?"
                : "UPDATE medical_order SET hospitalization_id = ?, patient_id = ?, doctor_id = ?, order_type = ?, order_content = ?, medicine_id = ?, examination_id = ?, dosage = ?, frequency = ?, start_time = ?, end_time = ?, status = ?, updated_at = ? WHERE id = ?";
        if (hq) {
            jdbcTemplate.update(sql,
                    medicalOrder.getHospitalizationId(),
                    medicalOrder.getPatientId(),
                    medicalOrder.getDoctorId(),
                    medicalOrder.getOrderType().name(),
                    medicalOrder.getOrderContent(),
                    medicalOrder.getMedicineId(),
                    medicalOrder.getQuantity(),
                    medicalOrder.getExaminationId(),
                    medicalOrder.getDosage(),
                    medicalOrder.getFrequency(),
                    medicalOrder.getStartTime() != null ? Timestamp.valueOf(medicalOrder.getStartTime()) : null,
                    medicalOrder.getEndTime() != null ? Timestamp.valueOf(medicalOrder.getEndTime()) : null,
                    medicalOrder.getStatus().name(),
                    Timestamp.valueOf(LocalDateTime.now()),
                    medicalOrder.getId());
        } else {
            jdbcTemplate.update(sql,
                    medicalOrder.getHospitalizationId(),
                    medicalOrder.getPatientId(),
                    medicalOrder.getDoctorId(),
                    medicalOrder.getOrderType().name(),
                    medicalOrder.getOrderContent(),
                    medicalOrder.getMedicineId(),
                    medicalOrder.getExaminationId(),
                    medicalOrder.getDosage(),
                    medicalOrder.getFrequency(),
                    medicalOrder.getStartTime() != null ? Timestamp.valueOf(medicalOrder.getStartTime()) : null,
                    medicalOrder.getEndTime() != null ? Timestamp.valueOf(medicalOrder.getEndTime()) : null,
                    medicalOrder.getStatus().name(),
                    Timestamp.valueOf(LocalDateTime.now()),
                    medicalOrder.getId());
        }
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM medical_order WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
}
