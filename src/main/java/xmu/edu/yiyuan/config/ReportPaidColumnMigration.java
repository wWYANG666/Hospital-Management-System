package xmu.edu.yiyuan.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * 启动时自动为 report 表增加 paid/paid_at，并回填待处理记录，避免因未执行 SQL 导致缴费与审阅逻辑不生效。
 */
@Component
@ConditionalOnProperty(name = "hospital.bootstrap.enabled", havingValue = "true", matchIfMissing = true)
@Order(1)
public class ReportPaidColumnMigration implements ApplicationRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("ALTER TABLE report ADD COLUMN paid TINYINT(1) NULL DEFAULT NULL COMMENT '患者是否已缴检查费'");
        } catch (Exception e) {
            if (!isDuplicateColumn(e)) {
                // 非“列已存在”则忽略（如无表权限等）
            }
        }
        try {
            jdbcTemplate.execute("ALTER TABLE report ADD COLUMN paid_at DATETIME NULL DEFAULT NULL COMMENT '检查费缴费时间'");
        } catch (Exception e) {
            if (!isDuplicateColumn(e)) {
            }
        }
        try {
            jdbcTemplate.update(
                    "UPDATE report r INNER JOIN examination e ON r.examination_id = e.id "
                            + "SET r.paid = 0 WHERE r.status = 'PENDING' AND e.price > 0 AND (r.paid IS NULL)");
        } catch (Exception ignored) {
        }
        try {
            jdbcTemplate.update(
                    "UPDATE report r INNER JOIN examination e ON r.examination_id = e.id "
                            + "SET r.paid = 1, r.paid_at = COALESCE(r.paid_at, NOW()) "
                            + "WHERE r.status = 'PENDING' AND (e.price IS NULL OR e.price <= 0) AND (r.paid IS NULL)");
        } catch (Exception ignored) {
        }
    }

    private static boolean isDuplicateColumn(Exception e) {
        String m = e.getMessage();
        return m != null && (m.contains("Duplicate column") || m.contains("already exists"));
    }
}
