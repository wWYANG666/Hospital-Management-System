package xmu.edu.yiyuan.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import xmu.edu.yiyuan.entity.Doctor;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class DoctorRepositoryImpl implements DoctorRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Doctor> doctorRowMapper = (rs, rowNum) -> {
        Doctor doctor = new Doctor();
        doctor.setId(rs.getLong("id"));
        doctor.setUserId(rs.getLong("user_id"));
        doctor.setDoctorCode(rs.getString("doctor_code"));
        doctor.setDepartment(rs.getString("department"));
        doctor.setTitle(rs.getString("title"));
        doctor.setSpecialty(rs.getString("specialty"));
        doctor.setIntroduction(rs.getString("introduction"));
        return doctor;
    };

    @Override
    public Optional<Doctor> findById(Long id) {
        String sql = "SELECT * FROM doctor WHERE id = ?";
        List<Doctor> doctors = jdbcTemplate.query(sql, doctorRowMapper, id);
        return doctors.isEmpty() ? Optional.empty() : Optional.of(doctors.get(0));
    }

    @Override
    public Optional<Doctor> findByUserId(Long userId) {
        String sql = "SELECT * FROM doctor WHERE user_id = ?";
        List<Doctor> doctors = jdbcTemplate.query(sql, doctorRowMapper, userId);
        return doctors.isEmpty() ? Optional.empty() : Optional.of(doctors.get(0));
    }

    @Override
    public List<Doctor> findAll() {
        String sql = "SELECT * FROM doctor";
        return jdbcTemplate.query(sql, doctorRowMapper);
    }

    @Override
    public Doctor save(Doctor doctor) {
        if (doctor.getId() == null) {
            String sql = "INSERT INTO doctor (user_id, doctor_code, department, title, specialty, introduction) VALUES (?, ?, ?, ?, ?, ?)";
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, doctor.getUserId());
                ps.setString(2, doctor.getDoctorCode());
                ps.setString(3, doctor.getDepartment());
                ps.setString(4, doctor.getTitle());
                ps.setString(5, doctor.getSpecialty());
                ps.setString(6, doctor.getIntroduction());
                return ps;
            }, keyHolder);
            doctor.setId(keyHolder.getKey().longValue());
        } else {
            update(doctor);
        }
        return doctor;
    }

    @Override
    public void update(Doctor doctor) {
        String sql = "UPDATE doctor SET user_id = ?, doctor_code = ?, department = ?, title = ?, specialty = ?, introduction = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                doctor.getUserId(),
                doctor.getDoctorCode(),
                doctor.getDepartment(),
                doctor.getTitle(),
                doctor.getSpecialty(),
                doctor.getIntroduction(),
                doctor.getId());
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM doctor WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
}
