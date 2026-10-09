package xmu.edu.yiyuan.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import xmu.edu.yiyuan.entity.MedicineStock;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class MedicineStockRepositoryImpl implements MedicineStockRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<MedicineStock> stockRowMapper = (rs, rowNum) -> {
        MedicineStock stock = new MedicineStock();
        stock.setId(rs.getLong("id"));
        stock.setMedicineId(rs.getLong("medicine_id"));
        stock.setBatchNumber(rs.getString("batch_number"));
        java.sql.Date prodDate = rs.getDate("production_date");
        if (prodDate != null) {
            stock.setProductionDate(prodDate.toLocalDate());
        }
        java.sql.Date expDate = rs.getDate("expiry_date");
        if (expDate != null) {
            stock.setExpiryDate(expDate.toLocalDate());
        }
        stock.setQuantity(rs.getInt("quantity"));
        BigDecimal price = rs.getBigDecimal("purchase_price");
        if (price != null) {
            stock.setPurchasePrice(price);
        }
        stock.setLocation(rs.getString("location"));
        stock.setStatus(rs.getInt("status"));
        return stock;
    };

    @Override
    public Optional<MedicineStock> findById(Long id) {
        try {
            String sql = "SELECT * FROM medicine_stock WHERE id = ?";
            List<MedicineStock> stocks = jdbcTemplate.query(sql, stockRowMapper, id);
            return stocks.isEmpty() ? Optional.empty() : Optional.of(stocks.get(0));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public List<MedicineStock> findAll() {
        try {
            String sql = "SELECT * FROM medicine_stock ORDER BY created_at DESC";
            return jdbcTemplate.query(sql, stockRowMapper);
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public List<MedicineStock> findByMedicineId(Long medicineId) {
        try {
            String sql = "SELECT * FROM medicine_stock WHERE medicine_id = ? AND status = 1 ORDER BY expiry_date ASC";
            return jdbcTemplate.query(sql, stockRowMapper, medicineId);
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public List<MedicineStock> findByLocation(String location) {
        try {
            String sql = "SELECT * FROM medicine_stock WHERE location = ? AND status = 1 ORDER BY expiry_date ASC";
            return jdbcTemplate.query(sql, stockRowMapper, location);
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public List<MedicineStock> findByBatchNumber(String batchNumber) {
        try {
            String sql = "SELECT * FROM medicine_stock WHERE batch_number = ?";
            return jdbcTemplate.query(sql, stockRowMapper, batchNumber);
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public MedicineStock save(MedicineStock stock) {
        try {
            if (stock.getId() == null) {
                String sql = "INSERT INTO medicine_stock (medicine_id, batch_number, production_date, expiry_date, quantity, purchase_price, location, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                KeyHolder keyHolder = new GeneratedKeyHolder();
                jdbcTemplate.update(connection -> {
                    PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    ps.setLong(1, stock.getMedicineId());
                    ps.setString(2, stock.getBatchNumber());
                    ps.setDate(3, stock.getProductionDate() != null ? java.sql.Date.valueOf(stock.getProductionDate()) : null);
                    ps.setDate(4, stock.getExpiryDate() != null ? java.sql.Date.valueOf(stock.getExpiryDate()) : null);
                    ps.setInt(5, stock.getQuantity());
                    ps.setBigDecimal(6, stock.getPurchasePrice());
                    ps.setString(7, stock.getLocation() != null ? stock.getLocation() : "药房");
                    ps.setInt(8, stock.getStatus() != null ? stock.getStatus() : 1);
                    return ps;
                }, keyHolder);
                if (keyHolder.getKey() != null) {
                    stock.setId(keyHolder.getKey().longValue());
                }
            } else {
                update(stock);
            }
        } catch (org.springframework.jdbc.BadSqlGrammarException e) {
            if (e.getCause() instanceof java.sql.SQLSyntaxErrorException) {
                java.sql.SQLSyntaxErrorException sqlEx = (java.sql.SQLSyntaxErrorException) e.getCause();
                if (sqlEx.getMessage().contains("doesn't exist")) {
                    System.err.println("警告：medicine_stock 表不存在，请先执行 database/create-medicine-inbound-tables.sql 创建表");
                    throw new RuntimeException("数据库表 medicine_stock 不存在，请先执行 database/create-medicine-inbound-tables.sql 创建表", e);
                }
            }
            throw e;
        }
        return stock;
    }

    @Override
    public void update(MedicineStock stock) {
        try {
            String sql = "UPDATE medicine_stock SET medicine_id = ?, batch_number = ?, production_date = ?, expiry_date = ?, quantity = ?, purchase_price = ?, location = ?, status = ? WHERE id = ?";
            jdbcTemplate.update(sql,
                    stock.getMedicineId(),
                    stock.getBatchNumber(),
                    stock.getProductionDate() != null ? java.sql.Date.valueOf(stock.getProductionDate()) : null,
                    stock.getExpiryDate() != null ? java.sql.Date.valueOf(stock.getExpiryDate()) : null,
                    stock.getQuantity(),
                    stock.getPurchasePrice(),
                    stock.getLocation(),
                    stock.getStatus(),
                    stock.getId());
        } catch (org.springframework.jdbc.BadSqlGrammarException e) {
            if (e.getCause() instanceof java.sql.SQLSyntaxErrorException) {
                java.sql.SQLSyntaxErrorException sqlEx = (java.sql.SQLSyntaxErrorException) e.getCause();
                if (sqlEx.getMessage().contains("doesn't exist")) {
                    System.err.println("警告：medicine_stock 表不存在，请先执行 database/create-medicine-inbound-tables.sql 创建表");
                    throw new RuntimeException("数据库表 medicine_stock 不存在，请先执行 database/create-medicine-inbound-tables.sql 创建表", e);
                }
            }
            throw e;
        }
    }

    @Override
    public void deleteById(Long id) {
        try {
            String sql = "DELETE FROM medicine_stock WHERE id = ?";
            jdbcTemplate.update(sql, id);
            resetAutoIncrementIfEmpty();
        } catch (Exception e) {
            // 忽略错误
        }
    }

    private void resetAutoIncrementIfEmpty() {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM medicine_stock", Integer.class);
            if (count != null && count == 0) {
                jdbcTemplate.execute("ALTER TABLE medicine_stock AUTO_INCREMENT = 1");
            }
        } catch (Exception ignored) {
        }
    }
}
