package xmu.edu.yiyuan.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import xmu.edu.yiyuan.entity.Department;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class DepartmentRepositoryImpl implements DepartmentRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Department> departmentRowMapper = (rs, rowNum) -> {
        Department department = new Department();
        department.setId(rs.getLong("id"));
        department.setName(rs.getString("name"));
        department.setDescription(rs.getString("description"));
        return department;
    };

    @Override
    public Optional<Department> findById(Long id) {
        String sql = "SELECT * FROM department WHERE id = ?";
        List<Department> departments = jdbcTemplate.query(sql, departmentRowMapper, id);
        return departments.isEmpty() ? Optional.empty() : Optional.of(departments.get(0));
    }

    @Override
    public List<Department> findAll() {
        String sql = "SELECT * FROM department ORDER BY name";
        return jdbcTemplate.query(sql, departmentRowMapper);
    }

    @Override
    public Department save(Department department) {
        if (department.getId() == null) {
            resetAutoIncrementIfEmpty();
            String sql = "INSERT INTO department (name, description) VALUES (?, ?)";
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, department.getName());
                ps.setString(2, department.getDescription());
                return ps;
            }, keyHolder);
            department.setId(keyHolder.getKey().longValue());
        } else {
            update(department);
        }
        return department;
    }

    @Override
    public void update(Department department) {
        String sql = "UPDATE department SET name = ?, description = ? WHERE id = ?";
        jdbcTemplate.update(sql, department.getName(), department.getDescription(), department.getId());
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM department WHERE id = ?";
        jdbcTemplate.update(sql, id);
        resetAutoIncrementIfEmpty();
    }

    @Override
    public void resetAutoIncrementIfEmpty() {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM department", Integer.class);
            if (count != null && count == 0) {
                jdbcTemplate.execute("ALTER TABLE department AUTO_INCREMENT = 1");
            }
        } catch (Exception ignored) {
        }
    }
}
