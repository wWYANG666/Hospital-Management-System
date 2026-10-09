package xmu.edu.yiyuan.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import xmu.edu.yiyuan.entity.Medicine;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class MedicineRepositoryImpl implements MedicineRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Medicine> medicineRowMapper = (rs, rowNum) -> {
        Medicine medicine = new Medicine();
        medicine.setId(rs.getLong("id"));
        medicine.setName(rs.getString("name"));
        medicine.setCode(rs.getString("code"));
        medicine.setType(rs.getString("type"));
        medicine.setSpecification(rs.getString("specification"));
        medicine.setUnit(rs.getString("unit"));
        BigDecimal price = rs.getBigDecimal("price");
        if (price != null) {
            medicine.setPrice(price);
        }
        try {
            BigDecimal purchasePrice = rs.getBigDecimal("purchase_price");
            if (purchasePrice != null) {
                medicine.setPurchasePrice(purchasePrice);
            }
        } catch (Exception e) {
            // 兼容旧数据库结构
        }
        medicine.setStock(rs.getInt("stock"));
        try {
            Integer minStockAlert = rs.getInt("min_stock_alert");
            if (!rs.wasNull()) {
                medicine.setMinStockAlert(minStockAlert);
            }
        } catch (Exception e) {
            // 兼容旧数据库结构
        }
        try {
            medicine.setManufacturer(rs.getString("manufacturer"));
            medicine.setApprovalNumber(rs.getString("approval_number"));
            medicine.setInsuranceCategory(rs.getString("insurance_category"));
        } catch (Exception e) {
            // 兼容旧数据库结构
        }
        // 兼容旧库：如果存在 department_id 列则读取
        try {
            Long deptId = rs.getLong("department_id");
            if (!rs.wasNull()) {
                medicine.setDepartmentId(deptId);
            }
        } catch (Exception ignored) {
        }
        medicine.setDescription(rs.getString("description"));
        medicine.setStatus(rs.getInt("status"));
        return medicine;
    };

    @Override
    public Optional<Medicine> findById(Long id) {
        String sql = "SELECT * FROM medicine WHERE id = ?";
        List<Medicine> medicines = jdbcTemplate.query(sql, medicineRowMapper, id);
        return medicines.isEmpty() ? Optional.empty() : Optional.of(medicines.get(0));
    }

    @Override
    public List<Medicine> findAll() {
        String sql = "SELECT * FROM medicine ORDER BY name";
        return jdbcTemplate.query(sql, medicineRowMapper);
    }

    @Override
    public List<Medicine> findAvailableMedicines() {
        String sql = "SELECT * FROM medicine WHERE status = 1 AND stock > 0 ORDER BY name";
        return jdbcTemplate.query(sql, medicineRowMapper);
    }

    @Override
    public List<Medicine> findByDepartmentIdOrDepartmentIdIsNull(Long departmentId) {
        // 兼容旧库：如果没有 department_id 列，则直接返回全部药品
        if (!checkColumnExists("department_id")) {
            return findAll();
        }
        String sql = "SELECT * FROM medicine " +
                     "WHERE (department_id = ? OR department_id IS NULL) " +
                     "ORDER BY name";
        return jdbcTemplate.query(sql, medicineRowMapper, departmentId);
    }

    @Override
    public List<Medicine> findAvailableByDepartmentIdOrDepartmentIdIsNull(Long departmentId) {
        if (!checkColumnExists("department_id")) {
            return findAvailableMedicines();
        }
        String sql = "SELECT * FROM medicine WHERE status = 1 AND stock > 0 "
                + "AND (department_id = ? OR department_id IS NULL) ORDER BY name";
        return jdbcTemplate.query(sql, medicineRowMapper, departmentId);
    }

    @Override
    public Medicine save(Medicine medicine) {
        if (medicine.getId() == null) {
            // 若表为空，则重置自增ID，从 1 开始（MySQL DELETE 清空不会自动重置）
            resetAutoIncrementIfEmpty();

            // 动态SQL：数据库存在什么列就写什么列（避免“有一列缺失就全部新字段不可用”）
            boolean hasPurchasePrice = checkColumnExists("purchase_price");
            boolean hasMinStockAlert = checkColumnExists("min_stock_alert");
            boolean hasManufacturer = checkColumnExists("manufacturer");
            boolean hasApprovalNumber = checkColumnExists("approval_number");
            boolean hasInsuranceCategory = checkColumnExists("insurance_category");
            boolean hasDepartmentId = checkColumnExists("department_id");

            java.util.List<String> columns = new java.util.ArrayList<>();
            java.util.List<Object> params = new java.util.ArrayList<>();

            columns.add("name"); params.add(medicine.getName());
            columns.add("code"); params.add(medicine.getCode());
            columns.add("type"); params.add(medicine.getType());
            columns.add("specification"); params.add(medicine.getSpecification());
            columns.add("unit"); params.add(medicine.getUnit());
            columns.add("price"); params.add(medicine.getPrice());
            columns.add("stock"); params.add(medicine.getStock() != null ? medicine.getStock() : 0);

            if (hasPurchasePrice) {
                columns.add("purchase_price");
                params.add(medicine.getPurchasePrice());
            }
            if (hasMinStockAlert) {
                columns.add("min_stock_alert");
                params.add(medicine.getMinStockAlert() != null ? medicine.getMinStockAlert() : 10);
            }
            if (hasManufacturer) {
                columns.add("manufacturer");
                params.add(medicine.getManufacturer());
            }
            if (hasApprovalNumber) {
                columns.add("approval_number");
                params.add(medicine.getApprovalNumber());
            }
            if (hasInsuranceCategory) {
                columns.add("insurance_category");
                params.add(medicine.getInsuranceCategory());
            }
            if (hasDepartmentId) {
                columns.add("department_id");
                params.add(medicine.getDepartmentId());
            }

            columns.add("description"); params.add(medicine.getDescription());
            columns.add("status"); params.add(medicine.getStatus() != null ? medicine.getStatus() : 1);

            String placeholders = String.join(", ", java.util.Collections.nCopies(columns.size(), "?"));
            String sql = "INSERT INTO medicine (" + String.join(", ", columns) + ") VALUES (" + placeholders + ")";

            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }
                return ps;
            }, keyHolder);
            if (keyHolder.getKey() != null) {
                medicine.setId(keyHolder.getKey().longValue());
            }
        } else {
            update(medicine);
        }
        return medicine;
    }

    @Override
    public void update(Medicine medicine) {
        try {
            // 检查所有新字段是否存在
            boolean hasPurchasePrice = checkColumnExists("purchase_price");
            boolean hasMinStockAlert = checkColumnExists("min_stock_alert");
            boolean hasManufacturer = checkColumnExists("manufacturer");
            boolean hasApprovalNumber = checkColumnExists("approval_number");
            boolean hasInsuranceCategory = checkColumnExists("insurance_category");
            boolean hasDepartmentId = checkColumnExists("department_id");

            // 动态SQL构建（含 department_id）
            StringBuilder sqlBuilder = new StringBuilder("UPDATE medicine SET name = ?, code = ?, type = ?, specification = ?, unit = ?, price = ?, stock = ?, description = ?, status = ?");
            java.util.List<Object> params = new java.util.ArrayList<>();
            params.add(medicine.getName());
            params.add(medicine.getCode());
            params.add(medicine.getType());
            params.add(medicine.getSpecification());
            params.add(medicine.getUnit());
            params.add(medicine.getPrice());
            params.add(medicine.getStock() != null ? medicine.getStock() : 0);
            params.add(medicine.getDescription());
            params.add(medicine.getStatus() != null ? medicine.getStatus() : 1);

            if (hasPurchasePrice) {
                sqlBuilder.append(", purchase_price = ?");
                params.add(medicine.getPurchasePrice());
            }
            if (hasMinStockAlert) {
                sqlBuilder.append(", min_stock_alert = ?");
                params.add(medicine.getMinStockAlert() != null ? medicine.getMinStockAlert() : 10);
            }
            if (hasManufacturer) {
                sqlBuilder.append(", manufacturer = ?");
                params.add(medicine.getManufacturer());
            }
            if (hasApprovalNumber) {
                sqlBuilder.append(", approval_number = ?");
                params.add(medicine.getApprovalNumber());
            }
            if (hasInsuranceCategory) {
                sqlBuilder.append(", insurance_category = ?");
                params.add(medicine.getInsuranceCategory());
            }
            if (hasDepartmentId) {
                sqlBuilder.append(", department_id = ?");
                params.add(medicine.getDepartmentId());
            }

            sqlBuilder.append(" WHERE id = ?");
            params.add(medicine.getId());

            jdbcTemplate.update(sqlBuilder.toString(), params.toArray());
        } catch (Exception e) {
            System.err.println("更新药品信息失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("更新药品信息失败: " + e.getMessage(), e);
        }
    }

    // 检查列是否存在
    private boolean checkColumnExists(String columnName) {
        try {
            // 使用INFORMATION_SCHEMA查询更可靠
            String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medicine' AND COLUMN_NAME = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, columnName);
            return count != null && count > 0;
        } catch (Exception e) {
            // 如果INFORMATION_SCHEMA查询失败，尝试直接查询列
            try {
                String sql = "SELECT " + columnName + " FROM medicine LIMIT 1";
                jdbcTemplate.queryForObject(sql, Object.class);
                return true;
            } catch (Exception e2) {
                return false;
            }
        }
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM medicine WHERE id = ?";
        jdbcTemplate.update(sql, id);
        // 删除后如果表为空，重置自增
        resetAutoIncrementIfEmpty();
    }

    @Override
    public void resetAutoIncrementIfEmpty() {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM medicine", Integer.class);
            if (count != null && count == 0) {
                jdbcTemplate.execute("ALTER TABLE medicine AUTO_INCREMENT = 1");
            }
        } catch (Exception ignored) {
            // 没有权限或数据库不支持时忽略，不影响主流程
        }
    }
}
