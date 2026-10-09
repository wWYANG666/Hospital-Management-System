package xmu.edu.yiyuan.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import xmu.edu.yiyuan.entity.Examination;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class ExaminationRepositoryImpl implements ExaminationRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Examination> examinationRowMapper = (rs, rowNum) -> {
        Examination examination = new Examination();
        examination.setId(rs.getLong("id"));
        examination.setName(rs.getString("name"));
        examination.setType(rs.getString("type"));
        examination.setDescription(rs.getString("description"));
        BigDecimal price = rs.getBigDecimal("price");
        if (price != null) {
            examination.setPrice(price);
        }
        examination.setStatus(rs.getInt("status"));
        // 兼容旧库：如果存在 department_id 列则读取
        try {
            Long deptId = rs.getLong("department_id");
            if (!rs.wasNull()) {
                examination.setDepartmentId(deptId);
            }
        } catch (Exception ignored) {
        }
        return examination;
    };

    @Override
    public Optional<Examination> findById(Long id) {
        String sql = "SELECT * FROM examination WHERE id = ?";
        List<Examination> examinations = jdbcTemplate.query(sql, examinationRowMapper, id);
        return examinations.isEmpty() ? Optional.empty() : Optional.of(examinations.get(0));
    }

    @Override
    public Optional<Examination> findByName(String name) {
        String sql = "SELECT * FROM examination WHERE name = ? LIMIT 1";
        List<Examination> examinations = jdbcTemplate.query(sql, examinationRowMapper, name);
        return examinations.isEmpty() ? Optional.empty() : Optional.of(examinations.get(0));
    }

    @Override
    public List<Examination> findAll() {
        String sql = "SELECT * FROM examination ORDER BY name";
        return jdbcTemplate.query(sql, examinationRowMapper);
    }

    @Override
    public List<Examination> findAvailableExaminations() {
        String sql = "SELECT * FROM examination WHERE status = 1 ORDER BY name";
        return jdbcTemplate.query(sql, examinationRowMapper);
    }

    @Override
    public List<Examination> findByDepartmentIdOrDepartmentIdIsNull(Long departmentId) {
        // 兼容旧库：如果没有 department_id 列，则直接返回全部检查项目
        if (!checkColumnExists("department_id")) {
            return findAvailableExaminations();
        }
        // 医生端可选项：只返回启用(status=1)的项目
        String sql = "SELECT * FROM examination " +
                     "WHERE status = 1 AND (department_id = ? OR department_id IS NULL) " +
                     "ORDER BY name";
        return jdbcTemplate.query(sql, examinationRowMapper, departmentId);
    }

    @Override
    public Examination save(Examination examination) {
        if (examination.getId() == null) {
            // 若表为空，则重置自增ID，从 1 开始（MySQL DELETE 清空不会自动重置）
            resetAutoIncrementIfEmpty();

            boolean hasDepartmentId = checkColumnExists("department_id");
            String sql;
            if (hasDepartmentId) {
                sql = "INSERT INTO examination (name, type, description, price, status, department_id) VALUES (?, ?, ?, ?, ?, ?)";
            } else {
                sql = "INSERT INTO examination (name, type, description, price, status) VALUES (?, ?, ?, ?, ?)";
            }
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, examination.getName());
                ps.setString(2, examination.getType());
                ps.setString(3, examination.getDescription());
                ps.setBigDecimal(4, examination.getPrice());
                ps.setInt(5, examination.getStatus() != null ? examination.getStatus() : 1);
                if (hasDepartmentId) {
                    ps.setObject(6, examination.getDepartmentId());
                }
                return ps;
            }, keyHolder);
            if (keyHolder.getKey() != null) {
                examination.setId(keyHolder.getKey().longValue());
            }
        } else {
            update(examination);
        }
        return examination;
    }

    @Override
    public void update(Examination examination) {
        boolean hasDepartmentId = checkColumnExists("department_id");
        String sql;
        if (hasDepartmentId) {
            sql = "UPDATE examination SET name = ?, type = ?, description = ?, price = ?, status = ?, department_id = ? WHERE id = ?";
            jdbcTemplate.update(sql,
                    examination.getName(),
                    examination.getType(),
                    examination.getDescription(),
                    examination.getPrice(),
                    examination.getStatus(),
                    examination.getDepartmentId(),
                    examination.getId());
        } else {
            sql = "UPDATE examination SET name = ?, type = ?, description = ?, price = ?, status = ? WHERE id = ?";
            jdbcTemplate.update(sql,
                    examination.getName(),
                    examination.getType(),
                    examination.getDescription(),
                    examination.getPrice(),
                    examination.getStatus(),
                    examination.getId());
        }
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM examination WHERE id = ?";
        jdbcTemplate.update(sql, id);
        // 删除后如果表为空，重置自增
        resetAutoIncrementIfEmpty();
    }

    @Override
    public void resetAutoIncrementIfEmpty() {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM examination", Integer.class);
            if (count != null && count == 0) {
                jdbcTemplate.execute("ALTER TABLE examination AUTO_INCREMENT = 1");
            }
        } catch (Exception ignored) {
            // 没有权限或数据库不支持时忽略，不影响主流程
        }
    }

    // 检查列是否存在（复用 medicine 的写法逻辑）
    private boolean checkColumnExists(String columnName) {
        try {
            String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'examination' AND COLUMN_NAME = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, columnName);
            return count != null && count > 0;
        } catch (Exception e) {
            try {
                String sql = "SELECT " + columnName + " FROM examination LIMIT 1";
                jdbcTemplate.queryForObject(sql, Object.class);
                return true;
            } catch (Exception e2) {
                return false;
            }
        }
    }
}
