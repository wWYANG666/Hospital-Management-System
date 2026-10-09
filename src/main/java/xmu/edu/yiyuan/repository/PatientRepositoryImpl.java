package xmu.edu.yiyuan.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import xmu.edu.yiyuan.entity.Patient;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class PatientRepositoryImpl implements PatientRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Patient> patientRowMapper = (rs, rowNum) -> {
        Patient patient = new Patient();
        patient.setId(rs.getLong("id"));
        patient.setUserId(rs.getLong("user_id"));
        patient.setPatientCode(rs.getString("patient_code"));
        patient.setIdCard(rs.getString("id_card"));
        String gender = rs.getString("gender");
        if (gender != null) {
            patient.setGender(Patient.Gender.valueOf(gender));
        }
        java.sql.Date birthday = rs.getDate("birthday");
        if (birthday != null) {
            patient.setBirthday(birthday.toLocalDate());
        }
        patient.setAddress(rs.getString("address"));
        patient.setEmergencyContact(rs.getString("emergency_contact"));
        patient.setEmergencyPhone(rs.getString("emergency_phone"));
        return patient;
    };

    @Override
    public Optional<Patient> findById(Long id) {
        String sql = "SELECT * FROM patient WHERE id = ?";
        List<Patient> patients = jdbcTemplate.query(sql, patientRowMapper, id);
        return patients.isEmpty() ? Optional.empty() : Optional.of(patients.get(0));
    }

    @Override
    public Optional<Patient> findByUserId(Long userId) {
        String sql = "SELECT * FROM patient WHERE user_id = ?";
        List<Patient> patients = jdbcTemplate.query(sql, patientRowMapper, userId);
        return patients.isEmpty() ? Optional.empty() : Optional.of(patients.get(0));
    }

    @Override
    public List<Patient> findAll() {
        String sql = "SELECT * FROM patient";
        return jdbcTemplate.query(sql, patientRowMapper);
    }

    @Override
    public Patient save(Patient patient) {
        if (patient.getId() == null) {
            String sql = "INSERT INTO patient (user_id, patient_code, id_card, gender, birthday, address, emergency_contact, emergency_phone) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, patient.getUserId());
                ps.setString(2, patient.getPatientCode());
                ps.setString(3, patient.getIdCard());
                ps.setString(4, patient.getGender() != null ? patient.getGender().name() : null);
                ps.setDate(5, patient.getBirthday() != null ? java.sql.Date.valueOf(patient.getBirthday()) : null);
                ps.setString(6, patient.getAddress());
                ps.setString(7, patient.getEmergencyContact());
                ps.setString(8, patient.getEmergencyPhone());
                return ps;
            }, keyHolder);
            patient.setId(keyHolder.getKey().longValue());
        } else {
            update(patient);
        }
        return patient;
    }

    @Override
    public void update(Patient patient) {
        String sql = "UPDATE patient SET user_id = ?, patient_code = ?, id_card = ?, gender = ?, birthday = ?, address = ?, emergency_contact = ?, emergency_phone = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                patient.getUserId(),
                patient.getPatientCode(),
                patient.getIdCard(),
                patient.getGender() != null ? patient.getGender().name() : null,
                patient.getBirthday() != null ? java.sql.Date.valueOf(patient.getBirthday()) : null,
                patient.getAddress(),
                patient.getEmergencyContact(),
                patient.getEmergencyPhone(),
                patient.getId());
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM patient WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
}
