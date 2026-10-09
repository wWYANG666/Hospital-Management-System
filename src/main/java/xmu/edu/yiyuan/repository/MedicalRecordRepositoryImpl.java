package xmu.edu.yiyuan.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import xmu.edu.yiyuan.entity.MedicalRecord;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class MedicalRecordRepositoryImpl implements MedicalRecordRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<MedicalRecord> medicalRecordRowMapper = (rs, rowNum) -> {
        MedicalRecord record = new MedicalRecord();
        java.sql.ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        java.util.Set<String> columnNames = new java.util.HashSet<>();
        for (int i = 1; i <= columnCount; i++) {
            columnNames.add(metaData.getColumnName(i).toLowerCase());
        }

        record.setId(rs.getLong("id"));
        Long appointmentId = rs.getLong("appointment_id");
        if (!rs.wasNull()) {
            record.setAppointmentId(appointmentId);
        }

        // 安全读取可能不存在的字段
        if (columnNames.contains("hospitalization_id")) {
            Long hospitalizationId = rs.getLong("hospitalization_id");
            if (!rs.wasNull()) {
                record.setHospitalizationId(hospitalizationId);
            }
        }

        record.setPatientId(rs.getLong("patient_id"));
        Long doctorId = rs.getLong("doctor_id");
        if (!rs.wasNull()) {
            record.setDoctorId(doctorId);
        }
        record.setDiagnosis(rs.getString("diagnosis"));
        record.setPrescription(rs.getString("prescription"));
        record.setExaminationItems(rs.getString("examination_items"));
        record.setMedicalRecordContent(rs.getString("medical_record_content"));

        // 安全读取可能不存在的字段
        if (columnNames.contains("condition_update")) {
            record.setConditionUpdate(rs.getString("condition_update"));
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            record.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            record.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return record;
    };

    @Override
    public Optional<MedicalRecord> findById(Long id) {
        String sql = "SELECT * FROM medical_record WHERE id = ?";
        List<MedicalRecord> records = jdbcTemplate.query(sql, medicalRecordRowMapper, id);
        return records.isEmpty() ? Optional.empty() : Optional.of(records.get(0));
    }

    @Override
    public Optional<MedicalRecord> findByAppointmentId(Long appointmentId) {
        List<MedicalRecord> records = findAllByAppointmentId(appointmentId);
        return records.isEmpty() ? Optional.empty() : Optional.of(records.get(0));
    }

    @Override
    public List<MedicalRecord> findAllByAppointmentId(Long appointmentId) {
        String sql = "SELECT * FROM medical_record WHERE appointment_id = ? ORDER BY COALESCE(updated_at, created_at) DESC, id DESC";
        return jdbcTemplate.query(sql, medicalRecordRowMapper, appointmentId);
    }

    @Override
    public List<MedicalRecord> findByPatientId(Long patientId) {
        String sql = "SELECT * FROM medical_record WHERE patient_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, medicalRecordRowMapper, patientId);
    }

    @Override
    public List<MedicalRecord> findByDoctorId(Long doctorId) {
        String sql = "SELECT * FROM medical_record WHERE doctor_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, medicalRecordRowMapper, doctorId);
    }

    @Override
    public List<MedicalRecord> findByHospitalizationId(Long hospitalizationId) {
        try {
            // 检查是否有 hospitalization_id 列
            boolean hasHospitalizationId = checkColumnExists("hospitalization_id");
            if (hasHospitalizationId) {
                String sql = "SELECT * FROM medical_record WHERE hospitalization_id = ? ORDER BY created_at DESC";
                return jdbcTemplate.query(sql, medicalRecordRowMapper, hospitalizationId);
            } else {
                // 如果列不存在，返回空列表
                return new java.util.ArrayList<>();
            }
        } catch (Exception e) {
            // 如果查询失败，返回空列表
            return new java.util.ArrayList<>();
        }
    }

    @Override
    public List<MedicalRecord> findAll() {
        String sql = "SELECT * FROM medical_record ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, medicalRecordRowMapper);
    }

    @Override
    public MedicalRecord save(MedicalRecord medicalRecord) {
        if (medicalRecord.getId() == null) {
            boolean hasHospitalizationId = checkColumnExists("hospitalization_id");
            boolean hasConditionUpdate = checkColumnExists("condition_update");

            String sql;
            if (hasHospitalizationId && hasConditionUpdate) {
                sql = "INSERT INTO medical_record (appointment_id, hospitalization_id, patient_id, doctor_id, diagnosis, prescription, examination_items, medical_record_content, condition_update, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            } else if (hasHospitalizationId) {
                sql = "INSERT INTO medical_record (appointment_id, hospitalization_id, patient_id, doctor_id, diagnosis, prescription, examination_items, medical_record_content, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            } else if (hasConditionUpdate) {
                sql = "INSERT INTO medical_record (appointment_id, patient_id, doctor_id, diagnosis, prescription, examination_items, medical_record_content, condition_update, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            } else {
                sql = "INSERT INTO medical_record (appointment_id, patient_id, doctor_id, diagnosis, prescription, examination_items, medical_record_content, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            }

            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                int paramIndex = 1;
                if (medicalRecord.getAppointmentId() != null) {
                    ps.setLong(paramIndex++, medicalRecord.getAppointmentId());
                } else {
                    ps.setNull(paramIndex++, java.sql.Types.BIGINT);
                }
                if (hasHospitalizationId) {
                    if (medicalRecord.getHospitalizationId() != null) {
                        ps.setLong(paramIndex++, medicalRecord.getHospitalizationId());
                    } else {
                        ps.setNull(paramIndex++, java.sql.Types.BIGINT);
                    }
                }
                ps.setLong(paramIndex++, medicalRecord.getPatientId());
                if (medicalRecord.getDoctorId() != null) {
                    ps.setLong(paramIndex++, medicalRecord.getDoctorId());
                } else {
                    ps.setNull(paramIndex++, java.sql.Types.BIGINT);
                }
                ps.setString(paramIndex++, medicalRecord.getDiagnosis());
                ps.setString(paramIndex++, medicalRecord.getPrescription());
                ps.setString(paramIndex++, medicalRecord.getExaminationItems());
                ps.setString(paramIndex++, medicalRecord.getMedicalRecordContent());
                if (hasConditionUpdate) {
                    ps.setString(paramIndex++, medicalRecord.getConditionUpdate());
                }
                Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                ps.setTimestamp(paramIndex++, now);
                ps.setTimestamp(paramIndex++, now);
                return ps;
            }, keyHolder);
            medicalRecord.setId(keyHolder.getKey().longValue());
        } else {
            update(medicalRecord);
        }
        return medicalRecord;
    }

    @Override
    public void update(MedicalRecord medicalRecord) {
        boolean hasHospitalizationId = checkColumnExists("hospitalization_id");
        boolean hasConditionUpdate = checkColumnExists("condition_update");

        String sql;
        if (hasHospitalizationId && hasConditionUpdate) {
            sql = "UPDATE medical_record SET appointment_id = ?, hospitalization_id = ?, patient_id = ?, doctor_id = ?, diagnosis = ?, prescription = ?, examination_items = ?, medical_record_content = ?, condition_update = ?, updated_at = ? WHERE id = ?";
            jdbcTemplate.update(sql,
                    medicalRecord.getAppointmentId(),
                    medicalRecord.getHospitalizationId(),
                    medicalRecord.getPatientId(),
                    medicalRecord.getDoctorId(),
                    medicalRecord.getDiagnosis(),
                    medicalRecord.getPrescription(),
                    medicalRecord.getExaminationItems(),
                    medicalRecord.getMedicalRecordContent(),
                    medicalRecord.getConditionUpdate(),
                    Timestamp.valueOf(LocalDateTime.now()),
                    medicalRecord.getId());
        } else if (hasHospitalizationId) {
            sql = "UPDATE medical_record SET appointment_id = ?, hospitalization_id = ?, patient_id = ?, doctor_id = ?, diagnosis = ?, prescription = ?, examination_items = ?, medical_record_content = ?, updated_at = ? WHERE id = ?";
            jdbcTemplate.update(sql,
                    medicalRecord.getAppointmentId(),
                    medicalRecord.getHospitalizationId(),
                    medicalRecord.getPatientId(),
                    medicalRecord.getDoctorId(),
                    medicalRecord.getDiagnosis(),
                    medicalRecord.getPrescription(),
                    medicalRecord.getExaminationItems(),
                    medicalRecord.getMedicalRecordContent(),
                    Timestamp.valueOf(LocalDateTime.now()),
                    medicalRecord.getId());
        } else if (hasConditionUpdate) {
            sql = "UPDATE medical_record SET appointment_id = ?, patient_id = ?, doctor_id = ?, diagnosis = ?, prescription = ?, examination_items = ?, medical_record_content = ?, condition_update = ?, updated_at = ? WHERE id = ?";
            jdbcTemplate.update(sql,
                    medicalRecord.getAppointmentId(),
                    medicalRecord.getPatientId(),
                    medicalRecord.getDoctorId(),
                    medicalRecord.getDiagnosis(),
                    medicalRecord.getPrescription(),
                    medicalRecord.getExaminationItems(),
                    medicalRecord.getMedicalRecordContent(),
                    medicalRecord.getConditionUpdate(),
                    Timestamp.valueOf(LocalDateTime.now()),
                    medicalRecord.getId());
        } else {
            sql = "UPDATE medical_record SET appointment_id = ?, patient_id = ?, doctor_id = ?, diagnosis = ?, prescription = ?, examination_items = ?, medical_record_content = ?, updated_at = ? WHERE id = ?";
            jdbcTemplate.update(sql,
                    medicalRecord.getAppointmentId(),
                    medicalRecord.getPatientId(),
                    medicalRecord.getDoctorId(),
                    medicalRecord.getDiagnosis(),
                    medicalRecord.getPrescription(),
                    medicalRecord.getExaminationItems(),
                    medicalRecord.getMedicalRecordContent(),
                    Timestamp.valueOf(LocalDateTime.now()),
                    medicalRecord.getId());
        }
    }

    // 检查列是否存在
    private boolean checkColumnExists(String columnName) {
        try {
            String sql = "SELECT " + columnName + " FROM medical_record LIMIT 1";
            jdbcTemplate.queryForObject(sql, Object.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM medical_record WHERE id = ?";
        jdbcTemplate.update(sql, id);
        resetAutoIncrementIfEmpty();
    }

    private void resetAutoIncrementIfEmpty() {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM medical_record", Integer.class);
            if (count != null && count == 0) {
                jdbcTemplate.execute("ALTER TABLE medical_record AUTO_INCREMENT = 1");
            }
        } catch (Exception ignored) {
        }
    }
}
