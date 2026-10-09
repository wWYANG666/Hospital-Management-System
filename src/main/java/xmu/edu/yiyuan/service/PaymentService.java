package xmu.edu.yiyuan.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Internal payment confirmation. A unique business key freezes the paid amount. */
@Service
public class PaymentService {
    private final JdbcTemplate jdbc;
    public PaymentService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional
    public void payPrescription(Long id, Long patientId) {
        Map<String, Object> row = own("prescription", id, patientId);
        if (!"PENDING".equals(row.get("status")) && !paid(row)) conflict("处方已处理，不能支付");
        BigDecimal amount = jdbc.queryForObject("SELECT COALESCE(SUM(total_price),0) FROM prescription_item WHERE prescription_id=?", BigDecimal.class, id);
        record("PRESCRIPTION", id, patientId, amount);
        jdbc.update("UPDATE prescription SET paid=1,paid_at=COALESCE(paid_at,NOW()) WHERE id=?", id);
    }

    @Transactional
    public void payReport(Long id, Long patientId) {
        Map<String, Object> row = own("report", id, patientId);
        if (row.get("examination_id") == null) conflict("该报告没有检查收费项目");
        if (!"PENDING".equals(row.get("status")) && !paid(row)) conflict("该报告已处理，不能支付");
        BigDecimal amount = jdbc.queryForObject("SELECT price FROM examination WHERE id=?", BigDecimal.class, row.get("examination_id"));
        if (amount == null) conflict("检查费用尚未维护");
        record("EXAM_REPORT", id, patientId, amount);
        jdbc.update("UPDATE report SET paid=1,paid_at=COALESCE(paid_at,NOW()) WHERE id=?", id);
    }

    @Transactional
    public void payHospitalization(Long id, Long patientId) {
        Map<String, Object> row = own("hospitalization", id, patientId);
        if (!"DISCHARGED".equals(row.get("status")) || row.get("settled_at") == null) conflict("请在出院费用结算完成后支付");
        BigDecimal amount = (BigDecimal) row.get("total_cost");
        record("HOSPITALIZATION", id, patientId, amount);
        if (!paid(row)) {
            jdbc.update("UPDATE hospitalization SET paid=1 WHERE id=?", id);
            jdbc.update("INSERT INTO medical_order (hospitalization_id,patient_id,doctor_id,order_type,order_content,status,created_at,updated_at) "
                    + "SELECT id,patient_id,doctor_id,'OTHER','住院费用已支付','COMPLETED',NOW(),NOW() FROM hospitalization WHERE id=?", id);
        }
    }

    private Map<String, Object> own(String table, Long id, Long patientId) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM " + table + " WHERE id=? FOR UPDATE", id);
        if (rows.isEmpty() || patientId == null || ((Number) rows.get(0).get("patient_id")).longValue() != patientId.longValue()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "记录不存在或无权限");
        }
        return rows.get(0);
    }

    private boolean paid(Map<String, Object> row) {
        Object value = row.get("paid");
        return Boolean.TRUE.equals(value) || value instanceof Number number && number.intValue() == 1;
    }

    private void record(String type, Long id, Long patientId, BigDecimal amount) {
        if (amount == null || amount.signum() < 0) conflict("费用未结算或金额无效");
        // The locked business row serializes repeat/concurrent confirmation requests.
        List<Map<String, Object>> existing = jdbc.queryForList("SELECT amount FROM payment_record WHERE business_type=? AND business_id=?", type, id);
        if (!existing.isEmpty()) {
            if (((BigDecimal) existing.get(0).get("amount")).compareTo(amount) != 0) conflict("已支付费用与当前费用不一致，请联系工作人员");
            return;
        }
        jdbc.update("INSERT INTO payment_record (patient_id,business_type,business_id,amount,paid_at) VALUES (?,?,?,?,NOW())", patientId, type, id, amount);
    }

    public List<Map<String, Object>> history(Long patientId) {
        return jdbc.queryForList("SELECT business_type,business_id,amount,paid_at FROM payment_record WHERE patient_id=? ORDER BY paid_at DESC,id DESC", patientId);
    }

    public BigDecimal paidAmount(String type, Long id, BigDecimal fallback) {
        List<BigDecimal> amounts = jdbc.queryForList("SELECT amount FROM payment_record WHERE business_type=? AND business_id=?", BigDecimal.class, type, id);
        return amounts.isEmpty() ? fallback : amounts.get(0);
    }

    private void conflict(String message) { throw new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
