package xmu.edu.yiyuan.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiGenerationAuditService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${hospital.bootstrap.enabled:true}")
    private boolean bootstrapEnabled;

    @PostConstruct
    public void ensureAuditTable() {
        if (!bootstrapEnabled) return;
        String sql = """
                CREATE TABLE IF NOT EXISTS ai_generation_log (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    who_user VARCHAR(100) NOT NULL,
                    where_scene VARCHAR(100) NOT NULL,
                    input_payload TEXT,
                    output_text TEXT,
                    created_at DATETIME NOT NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """;
        jdbcTemplate.execute(sql);
    }

    public void log(String whoUser, String whereScene, Map<String, Object> input, String output) {
        String payload = stringify(input == null ? Map.of() : input);
        jdbcTemplate.update(
                "INSERT INTO ai_generation_log (who_user, where_scene, input_payload, output_text, created_at) VALUES (?, ?, ?, ?, ?)",
                blankToUnknown(whoUser),
                blankToUnknown(whereScene),
                payload,
                output,
                LocalDateTime.now()
        );
    }

    public List<AiGenerationLogItem> queryLogs(String whoUser,
                                               String whereScene,
                                               LocalDateTime fromTime,
                                               LocalDateTime toTime,
                                               int limit) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, who_user, where_scene, input_payload, output_text, created_at FROM ai_generation_log WHERE 1=1"
        );
        List<Object> args = new ArrayList<>();

        if (whoUser != null && !whoUser.isBlank()) {
            sql.append(" AND who_user LIKE ?");
            args.add("%" + whoUser.trim() + "%");
        }
        if (whereScene != null && !whereScene.isBlank()) {
            sql.append(" AND where_scene = ?");
            args.add(whereScene.trim());
        }
        if (fromTime != null) {
            sql.append(" AND created_at >= ?");
            args.add(fromTime);
        }
        if (toTime != null) {
            sql.append(" AND created_at <= ?");
            args.add(toTime);
        }
        int safeLimit = Math.max(1, Math.min(limit, 500));
        sql.append(" ORDER BY created_at DESC LIMIT ").append(safeLimit);

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            AiGenerationLogItem item = new AiGenerationLogItem();
            item.setId(rs.getLong("id"));
            item.setWhoUser(rs.getString("who_user"));
            item.setWhereScene(rs.getString("where_scene"));
            item.setInputPayload(rs.getString("input_payload"));
            item.setOutputText(rs.getString("output_text"));
            java.sql.Timestamp ts = rs.getTimestamp("created_at");
            item.setCreatedAt(ts != null ? ts.toLocalDateTime() : null);
            return item;
        }, args.toArray());
    }

    public List<String> findAllTriggerUsers() {
        return jdbcTemplate.query(
                "SELECT DISTINCT who_user FROM ai_generation_log WHERE who_user IS NOT NULL AND who_user <> '' ORDER BY who_user ASC",
                (rs, rowNum) -> rs.getString("who_user")
        );
    }

    public int deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        List<Long> validIds = ids.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (validIds.isEmpty()) {
            return 0;
        }

        String placeholders = validIds.stream().map(i -> "?").collect(Collectors.joining(","));
        String sql = "DELETE FROM ai_generation_log WHERE id IN (" + placeholders + ")";
        return jdbcTemplate.update(sql, validIds.toArray());
    }

    private String blankToUnknown(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim();
    }

    private String stringify(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(e.getKey()).append("=").append(String.valueOf(e.getValue()));
        }
        sb.append("}");
        return sb.toString();
    }

    public static class AiGenerationLogItem {
        private Long id;
        private String whoUser;
        private String whereScene;
        private String inputPayload;
        private String outputText;
        private LocalDateTime createdAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getWhoUser() {
            return whoUser;
        }

        public void setWhoUser(String whoUser) {
            this.whoUser = whoUser;
        }

        public String getWhereScene() {
            return whereScene;
        }

        public void setWhereScene(String whereScene) {
            this.whereScene = whereScene;
        }

        public String getInputPayload() {
            return inputPayload;
        }

        public void setInputPayload(String inputPayload) {
            this.inputPayload = inputPayload;
        }

        public String getOutputText() {
            return outputText;
        }

        public void setOutputText(String outputText) {
            this.outputText = outputText;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }
}
