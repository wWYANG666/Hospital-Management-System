package xmu.edu.yiyuan.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.List;

/**
 * 启动时若下列表无数据，将 AUTO_INCREMENT 置为 1，避免清空数据后新建仍从大号主键开始。
 */
@Component
@ConditionalOnProperty(name = "hospital.bootstrap.enabled", havingValue = "true", matchIfMissing = true)
@Order(2000)
public class EmptyTableAutoIncrementReset implements ApplicationRunner {

    private static final List<String> TABLES = List.of(
            "hospitalization",
            "medical_record",
            "medicine_inbound",
            "medicine_stock",
            "prescription_item",
            "schedule"
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        for (String table : TABLES) {
            resetIfEmpty(table);
        }
    }

    private void resetIfEmpty(String tableName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM `" + tableName + "`", Integer.class);
            if (count != null && count == 0) {
                jdbcTemplate.execute("ALTER TABLE `" + tableName + "` AUTO_INCREMENT = 1");
            }
        } catch (Exception ignored) {
            // 表不存在、无权限或非 MySQL 时忽略
        }
    }
}
