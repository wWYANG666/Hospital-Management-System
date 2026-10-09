package xmu.edu.yiyuan.service;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PharmacyService {
    private final JdbcTemplate jdbc;
    public PharmacyService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional
    public void dispense(Long id, Long operator) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM prescription WHERE id=? FOR UPDATE", id);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "处方不存在");
        Map<String, Object> prescription = rows.get(0);
        if (!"PENDING".equals(prescription.get("status"))) throw new ResponseStatusException(HttpStatus.CONFLICT, "处方已处理，不能重复发药");
        Object paid = prescription.get("paid");
        if (!(Boolean.TRUE.equals(paid) || paid instanceof Number n && n.intValue() == 1)
                || prescription.get("patient_signature") == null || String.valueOf(prescription.get("patient_signature")).isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "患者尚未完成付款和签字确认");
        }
        List<Map<String, Object>> items = jdbc.queryForList("SELECT medicine_id,SUM(quantity) quantity FROM prescription_item WHERE prescription_id=? GROUP BY medicine_id ORDER BY medicine_id", id);
        if (items.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "处方没有有效药品明细");
        for (Map<String, Object> item : items) {
            if (item.get("medicine_id") == null || item.get("quantity") == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "处方药品明细不完整");
            int quantity = ((Number) item.get("quantity")).intValue();
            List<Map<String, Object>> medicines = jdbc.queryForList("SELECT stock,status,name FROM medicine WHERE id=? FOR UPDATE", item.get("medicine_id"));
            if (quantity < 1 || medicines.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "处方药品不存在或数量无效");
            Map<String, Object> med = medicines.get(0);
            if (med.get("stock") == null || ((Number) med.get("stock")).intValue() < quantity
                    || med.get("status") == null || ((Number) med.get("status")).intValue() != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, String.valueOf(med.get("name")) + "库存不足或已停用，未办理发药");
            }
        }
        for (Map<String, Object> item : items) {
            int quantity = ((Number) item.get("quantity")).intValue();
            if (jdbc.update("UPDATE medicine SET stock=stock-? WHERE id=? AND stock>=?", quantity, item.get("medicine_id"), quantity) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "库存发生变化，请重新核对");
            }
        }
        jdbc.update("UPDATE prescription SET status='DISPENSED',dispensed_at=NOW(),dispensed_by=? WHERE id=?", operator, id);
    }
}
