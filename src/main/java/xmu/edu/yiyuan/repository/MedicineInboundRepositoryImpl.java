package xmu.edu.yiyuan.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import xmu.edu.yiyuan.entity.MedicineInbound;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class MedicineInboundRepositoryImpl implements MedicineInboundRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<MedicineInbound> inboundRowMapper = (rs, rowNum) -> {
        MedicineInbound inbound = new MedicineInbound();
        inbound.setId(rs.getLong("id"));
        inbound.setMedicineId(rs.getLong("medicine_id"));
        inbound.setBatchNumber(rs.getString("batch_number"));
        java.sql.Date prodDate = rs.getDate("production_date");
        if (prodDate != null) {
            inbound.setProductionDate(prodDate.toLocalDate());
        }
        java.sql.Date expDate = rs.getDate("expiry_date");
        if (expDate != null) {
            inbound.setExpiryDate(expDate.toLocalDate());
        }
        inbound.setQuantity(rs.getInt("quantity"));
        BigDecimal price = rs.getBigDecimal("purchase_price");
        if (price != null) {
            inbound.setPurchasePrice(price);
        }
        BigDecimal total = rs.getBigDecimal("total_amount");
        if (total != null) {
            inbound.setTotalAmount(total);
        }
        inbound.setLocation(rs.getString("location"));
        inbound.setOperator(rs.getString("operator"));
        Timestamp inboundDate = rs.getTimestamp("inbound_date");
        if (inboundDate != null) {
            inbound.setInboundDate(inboundDate.toLocalDateTime());
        }
        inbound.setStatus(rs.getInt("status"));
        return inbound;
    };

    @Override
    public Optional<MedicineInbound> findById(Long id) {
        try {
            String sql = "SELECT * FROM medicine_inbound WHERE id = ?";
            List<MedicineInbound> inbounds = jdbcTemplate.query(sql, inboundRowMapper, id);
            return inbounds.isEmpty() ? Optional.empty() : Optional.of(inbounds.get(0));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public List<MedicineInbound> findAll() {
        try {
            String sql = "SELECT * FROM medicine_inbound ORDER BY inbound_date DESC";
            return jdbcTemplate.query(sql, inboundRowMapper);
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public List<MedicineInbound> findByMedicineId(Long medicineId) {
        try {
            String sql = "SELECT * FROM medicine_inbound WHERE medicine_id = ? ORDER BY inbound_date DESC";
            return jdbcTemplate.query(sql, inboundRowMapper, medicineId);
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public List<MedicineInbound> findByStatus(Integer status) {
        try {
            String sql = "SELECT * FROM medicine_inbound WHERE status = ? ORDER BY inbound_date DESC";
            return jdbcTemplate.query(sql, inboundRowMapper, status);
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public MedicineInbound save(MedicineInbound inbound) {
        try {
            if (inbound.getId() == null) {
                String sql = "INSERT INTO medicine_inbound (medicine_id, batch_number, production_date, expiry_date, quantity, purchase_price, total_amount, location, operator, inbound_date, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                KeyHolder keyHolder = new GeneratedKeyHolder();
                jdbcTemplate.update(connection -> {
                    PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    ps.setLong(1, inbound.getMedicineId());
                    ps.setString(2, inbound.getBatchNumber());
                    ps.setDate(3, inbound.getProductionDate() != null ? java.sql.Date.valueOf(inbound.getProductionDate()) : null);
                    ps.setDate(4, inbound.getExpiryDate() != null ? java.sql.Date.valueOf(inbound.getExpiryDate()) : null);
                    ps.setInt(5, inbound.getQuantity());
                    ps.setBigDecimal(6, inbound.getPurchasePrice());
                    ps.setBigDecimal(7, inbound.getTotalAmount());
                    ps.setString(8, inbound.getLocation() != null ? inbound.getLocation() : "药房");
                    ps.setString(9, inbound.getOperator());
                    ps.setTimestamp(10, inbound.getInboundDate() != null ? Timestamp.valueOf(inbound.getInboundDate()) : Timestamp.valueOf(LocalDateTime.now()));
                    ps.setInt(11, inbound.getStatus() != null ? inbound.getStatus() : 0);
                    return ps;
                }, keyHolder);
                if (keyHolder.getKey() != null) {
                    inbound.setId(keyHolder.getKey().longValue());
                }
            } else {
                update(inbound);
            }
        } catch (org.springframework.jdbc.BadSqlGrammarException e) {
            if (e.getCause() instanceof java.sql.SQLSyntaxErrorException) {
                java.sql.SQLSyntaxErrorException sqlEx = (java.sql.SQLSyntaxErrorException) e.getCause();
                if (sqlEx.getMessage().contains("doesn't exist")) {
                    System.err.println("警告：medicine_inbound 表不存在，请先执行 database/create-medicine-inbound-tables.sql 创建表");
                    throw new RuntimeException("数据库表 medicine_inbound 不存在，请先执行 database/create-medicine-inbound-tables.sql 创建表", e);
                }
            }
            throw e;
        }
        return inbound;
    }

    @Override
    public void update(MedicineInbound inbound) {
        try {
            String sql = "UPDATE medicine_inbound SET medicine_id = ?, batch_number = ?, production_date = ?, expiry_date = ?, quantity = ?, purchase_price = ?, total_amount = ?, location = ?, operator = ?, inbound_date = ?, status = ? WHERE id = ?";
            jdbcTemplate.update(sql,
                    inbound.getMedicineId(),
                    inbound.getBatchNumber(),
                    inbound.getProductionDate() != null ? java.sql.Date.valueOf(inbound.getProductionDate()) : null,
                    inbound.getExpiryDate() != null ? java.sql.Date.valueOf(inbound.getExpiryDate()) : null,
                    inbound.getQuantity(),
                    inbound.getPurchasePrice(),
                    inbound.getTotalAmount(),
                    inbound.getLocation(),
                    inbound.getOperator(),
                    inbound.getInboundDate() != null ? Timestamp.valueOf(inbound.getInboundDate()) : Timestamp.valueOf(LocalDateTime.now()),
                    inbound.getStatus(),
                    inbound.getId());
        } catch (org.springframework.jdbc.BadSqlGrammarException e) {
            if (e.getCause() instanceof java.sql.SQLSyntaxErrorException) {
                java.sql.SQLSyntaxErrorException sqlEx = (java.sql.SQLSyntaxErrorException) e.getCause();
                if (sqlEx.getMessage().contains("doesn't exist")) {
                    System.err.println("警告：medicine_inbound 表不存在，请先执行 database/create-medicine-inbound-tables.sql 创建表");
                    throw new RuntimeException("数据库表 medicine_inbound 不存在，请先执行 database/create-medicine-inbound-tables.sql 创建表", e);
                }
            }
            throw e;
        }
    }

    @Override
    public void deleteById(Long id) {
        try {
            String sql = "DELETE FROM medicine_inbound WHERE id = ?";
            jdbcTemplate.update(sql, id);
            resetAutoIncrementIfEmpty();
        } catch (Exception e) {
            // 忽略错误
        }
    }

    private void resetAutoIncrementIfEmpty() {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM medicine_inbound", Integer.class);
            if (count != null && count == 0) {
                jdbcTemplate.execute("ALTER TABLE medicine_inbound AUTO_INCREMENT = 1");
            }
        } catch (Exception ignored) {
        }
    }
}
