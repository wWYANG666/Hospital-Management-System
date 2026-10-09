package xmu.edu.yiyuan.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import xmu.edu.yiyuan.entity.PrescriptionItem;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class PrescriptionItemRepositoryImpl implements PrescriptionItemRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<PrescriptionItem> itemRowMapper = (rs, rowNum) -> {
        PrescriptionItem item = new PrescriptionItem();
        item.setId(rs.getLong("id"));
        item.setPrescriptionId(rs.getLong("prescription_id"));
        item.setMedicineId(rs.getLong("medicine_id"));
        item.setMedicineName(rs.getString("medicine_name"));
        item.setSpecification(rs.getString("specification"));
        int q = rs.getInt("quantity");
        if (rs.wasNull()) {
            item.setQuantity(null);
        } else {
            item.setQuantity(q);
        }
        item.setUnit(rs.getString("unit"));
        item.setUsage(rs.getString("usage"));
        item.setDosage(rs.getString("dosage"));
        item.setFrequency(rs.getString("frequency"));
        BigDecimal price = rs.getBigDecimal("price");
        if (price != null) {
            item.setPrice(price);
        }
        BigDecimal totalPrice = rs.getBigDecimal("total_price");
        if (totalPrice != null) {
            item.setTotalPrice(totalPrice);
        }
        item.setNotes(rs.getString("notes"));
        return item;
    };

    @Override
    public Optional<PrescriptionItem> findById(Long id) {
        try {
            String sql = "SELECT * FROM prescription_item WHERE id = ?";
            List<PrescriptionItem> items = jdbcTemplate.query(sql, itemRowMapper, id);
            return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public List<PrescriptionItem> findAll() {
        try {
            String sql = "SELECT * FROM prescription_item ORDER BY id";
            return jdbcTemplate.query(sql, itemRowMapper);
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public List<PrescriptionItem> findByPrescriptionId(Long prescriptionId) {
        try {
            String sql = "SELECT * FROM prescription_item WHERE prescription_id = ? ORDER BY id";
            return jdbcTemplate.query(sql, itemRowMapper, prescriptionId);
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public PrescriptionItem save(PrescriptionItem item) {
        try {
            if (item.getId() == null) {
                // usage 是 MySQL 保留关键字，需要用反引号括起来
                String sql = "INSERT INTO prescription_item (prescription_id, medicine_id, medicine_name, specification, quantity, unit, `usage`, dosage, frequency, price, total_price, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                KeyHolder keyHolder = new GeneratedKeyHolder();
                jdbcTemplate.update(connection -> {
                    PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    ps.setLong(1, item.getPrescriptionId());
                    ps.setLong(2, item.getMedicineId());
                    ps.setString(3, item.getMedicineName());
                    ps.setString(4, item.getSpecification());
                    ps.setInt(5, item.getQuantity() != null ? item.getQuantity() : 1);
                    ps.setString(6, item.getUnit());
                    ps.setString(7, item.getUsage());
                    ps.setString(8, item.getDosage());
                    ps.setString(9, item.getFrequency());
                    ps.setBigDecimal(10, item.getPrice() != null ? item.getPrice() : java.math.BigDecimal.ZERO);
                    ps.setBigDecimal(11, item.getTotalPrice() != null ? item.getTotalPrice() : java.math.BigDecimal.ZERO);
                    ps.setString(12, item.getNotes());
                    return ps;
                }, keyHolder);
                if (keyHolder.getKey() != null) {
                    item.setId(keyHolder.getKey().longValue());
                }
            } else {
                update(item);
            }
        } catch (org.springframework.jdbc.BadSqlGrammarException e) {
            if (e.getCause() instanceof java.sql.SQLSyntaxErrorException) {
                java.sql.SQLSyntaxErrorException sqlEx = (java.sql.SQLSyntaxErrorException) e.getCause();
                if (sqlEx.getMessage().contains("doesn't exist")) {
                    System.err.println("警告：prescription_item 表不存在，请先执行 database/create-prescription-tables.sql 创建表");
                    throw new RuntimeException("数据库表 prescription_item 不存在，请先执行 database/create-prescription-tables.sql 创建表", e);
                }
            }
            throw e;
        }
        return item;
    }

    @Override
    public void update(PrescriptionItem item) {
        try {
            // usage 是 MySQL 保留关键字，需要用反引号括起来
            String sql = "UPDATE prescription_item SET prescription_id = ?, medicine_id = ?, medicine_name = ?, specification = ?, quantity = ?, unit = ?, `usage` = ?, dosage = ?, frequency = ?, price = ?, total_price = ?, notes = ? WHERE id = ?";
            jdbcTemplate.update(sql,
                    item.getPrescriptionId(),
                    item.getMedicineId(),
                    item.getMedicineName(),
                    item.getSpecification(),
                    item.getQuantity(),
                    item.getUnit(),
                    item.getUsage(),
                    item.getDosage(),
                    item.getFrequency(),
                    item.getPrice(),
                    item.getTotalPrice(),
                    item.getNotes(),
                    item.getId());
        } catch (Exception e) {
            // 忽略错误
        }
    }

    @Override
    public void deleteById(Long id) {
        try {
            String sql = "DELETE FROM prescription_item WHERE id = ?";
            jdbcTemplate.update(sql, id);
            resetAutoIncrementIfEmpty();
        } catch (Exception e) {
            // 忽略错误
        }
    }

    @Override
    public void deleteByPrescriptionId(Long prescriptionId) {
        try {
            String sql = "DELETE FROM prescription_item WHERE prescription_id = ?";
            jdbcTemplate.update(sql, prescriptionId);
            resetAutoIncrementIfEmpty();
        } catch (Exception e) {
            // 忽略错误
        }
    }

    private void resetAutoIncrementIfEmpty() {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM prescription_item", Integer.class);
            if (count != null && count == 0) {
                jdbcTemplate.execute("ALTER TABLE prescription_item AUTO_INCREMENT = 1");
            }
        } catch (Exception ignored) {
        }
    }
}
