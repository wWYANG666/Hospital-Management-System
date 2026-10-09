package xmu.edu.yiyuan.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import xmu.edu.yiyuan.entity.Bed;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class BedRepositoryImpl implements BedRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Bed> bedRowMapper = (rs, rowNum) -> {
        Bed bed = new Bed();
        java.sql.ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        java.util.Set<String> columnNames = new java.util.HashSet<>();
        for (int i = 1; i <= columnCount; i++) {
            columnNames.add(metaData.getColumnName(i).toLowerCase());
        }

        bed.setId(rs.getLong("id"));
        bed.setBedNumber(rs.getString("bed_number"));
        bed.setRoomNumber(rs.getString("room_number"));

        // 安全读取可能不存在的字段
        if (columnNames.contains("ward")) {
            bed.setWard(rs.getString("ward"));
        }

        Long departmentId = rs.getLong("department_id");
        if (!rs.wasNull()) {
            bed.setDepartmentId(departmentId);
        }
        String bedType = rs.getString("bed_type");
        if (bedType != null) {
            bed.setBedType(Bed.BedType.valueOf(bedType));
        }
        String status = rs.getString("status");
        if (status != null) {
            bed.setStatus(Bed.BedStatus.valueOf(status));
        }
        BigDecimal price = rs.getBigDecimal("price_per_day");
        if (price != null) {
            bed.setPricePerDay(price);
        }
        return bed;
    };

    @Override
    public Optional<Bed> findById(Long id) {
        String sql = "SELECT * FROM bed WHERE id = ?";
        List<Bed> beds = jdbcTemplate.query(sql, bedRowMapper, id);
        return beds.isEmpty() ? Optional.empty() : Optional.of(beds.get(0));
    }

    @Override
    public List<Bed> findAll() {
        String sql = "SELECT * FROM bed ORDER BY bed_number";
        return jdbcTemplate.query(sql, bedRowMapper);
    }

    @Override
    public List<Bed> findByStatus(Bed.BedStatus status) {
        String sql = "SELECT * FROM bed WHERE status = ? ORDER BY bed_number";
        return jdbcTemplate.query(sql, bedRowMapper, status.name());
    }

    @Override
    public List<Bed> findAvailableBeds() {
        String sql = "SELECT * FROM bed WHERE status = 'AVAILABLE' ORDER BY bed_number";
        return jdbcTemplate.query(sql, bedRowMapper);
    }

    @Override
    public List<Bed> findByDepartmentId(Long departmentId) {
        String sql = "SELECT * FROM bed WHERE department_id = ? ORDER BY bed_number";
        return jdbcTemplate.query(sql, bedRowMapper, departmentId);
    }

    @Override
    public Bed save(Bed bed) {
        if (bed.getId() == null) {
            // 检查新字段是否存在
            boolean hasWardField = checkColumnExists("ward");
            String sql;
            if (hasWardField) {
                sql = "INSERT INTO bed (bed_number, room_number, ward, department_id, bed_type, status, price_per_day) VALUES (?, ?, ?, ?, ?, ?, ?)";
            } else {
                sql = "INSERT INTO bed (bed_number, room_number, department_id, bed_type, status, price_per_day) VALUES (?, ?, ?, ?, ?, ?)";
            }
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                int paramIndex = 1;
                ps.setString(paramIndex++, bed.getBedNumber());
                ps.setString(paramIndex++, bed.getRoomNumber());

                if (hasWardField) {
                    ps.setString(paramIndex++, bed.getWard());
                }

                if (bed.getDepartmentId() != null) {
                    ps.setLong(paramIndex++, bed.getDepartmentId());
                } else {
                    ps.setNull(paramIndex++, java.sql.Types.BIGINT);
                }
                ps.setString(paramIndex++, bed.getBedType() != null ? bed.getBedType().name() : Bed.BedType.GENERAL.name());
                ps.setString(paramIndex++, bed.getStatus() != null ? bed.getStatus().name() : Bed.BedStatus.AVAILABLE.name());
                ps.setBigDecimal(paramIndex++, bed.getPricePerDay());
                return ps;
            }, keyHolder);
            bed.setId(keyHolder.getKey().longValue());
        } else {
            update(bed);
        }
        return bed;
    }

    @Override
    public void update(Bed bed) {
        boolean hasWardField = checkColumnExists("ward");
        String sql;
        if (hasWardField) {
            sql = "UPDATE bed SET bed_number = ?, room_number = ?, ward = ?, department_id = ?, bed_type = ?, status = ?, price_per_day = ? WHERE id = ?";
            jdbcTemplate.update(sql,
                    bed.getBedNumber(),
                    bed.getRoomNumber(),
                    bed.getWard(),
                    bed.getDepartmentId(),
                    bed.getBedType() != null ? bed.getBedType().name() : Bed.BedType.GENERAL.name(),
                    bed.getStatus() != null ? bed.getStatus().name() : Bed.BedStatus.AVAILABLE.name(),
                    bed.getPricePerDay(),
                    bed.getId());
        } else {
            sql = "UPDATE bed SET bed_number = ?, room_number = ?, department_id = ?, bed_type = ?, status = ?, price_per_day = ? WHERE id = ?";
            jdbcTemplate.update(sql,
                    bed.getBedNumber(),
                    bed.getRoomNumber(),
                    bed.getDepartmentId(),
                    bed.getBedType() != null ? bed.getBedType().name() : Bed.BedType.GENERAL.name(),
                    bed.getStatus() != null ? bed.getStatus().name() : Bed.BedStatus.AVAILABLE.name(),
                    bed.getPricePerDay(),
                    bed.getId());
        }
    }

    // 检查列是否存在
    private boolean checkColumnExists(String columnName) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='bed' AND column_name=?", Integer.class, columnName);
        return count != null && count > 0;
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM bed WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
}
