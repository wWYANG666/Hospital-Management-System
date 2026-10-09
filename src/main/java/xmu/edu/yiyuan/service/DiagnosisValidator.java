package xmu.edu.yiyuan.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DiagnosisValidator {
    private final JdbcTemplate jdbc;
    public DiagnosisValidator(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void validateAndLock(Long appointmentId, String[] medicineIds, String[] examinationIds,
            HttpServletRequest request, Long doctorId) {
        List<Map<String, Object>> appointments = jdbc.queryForList("SELECT doctor_id,status FROM appointment WHERE id=? FOR UPDATE", appointmentId);
        if (appointments.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "挂号不存在");
        Map<String, Object> appointment = appointments.get(0);
        if (doctorId == null || !doctorId.equals(((Number) appointment.get("doctor_id")).longValue())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权诊断该挂号");
        }
        if ("CANCELLED".equals(appointment.get("status"))) throw new ResponseStatusException(HttpStatus.CONFLICT, "挂号已取消");
        String department = jdbc.queryForObject("SELECT department FROM doctor WHERE id=?", String.class, doctorId);
        validateItems("medicine", medicineIds, department);
        validateItems("examination", examinationIds, department);
        if (medicineIds != null) for (String id : medicineIds) {
            String raw = request.getParameter("medicineQty_" + id);
            int quantity = raw == null || raw.isBlank() ? 1 : Integer.parseInt(raw.trim());
            BusinessValidation.require(quantity >= 1 && quantity <= 9999, "药品数量必须为1至9999");
        }
    }

    private void validateItems(String table, String[] ids, String doctorDepartment) {
        if (ids == null) return;
        HashSet<Long> seen = new HashSet<>();
        for (String value : ids) {
            Long id = Long.valueOf(value);
            BusinessValidation.require(seen.add(id), "不能重复选择同一项目");
            List<Map<String, Object>> rows = jdbc.queryForList("SELECT i.status,i.price,d.name department FROM " + table
                    + " i LEFT JOIN department d ON i.department_id=d.id WHERE i.id=?", id);
            BusinessValidation.require(!rows.isEmpty(), "所选药品或检查项目不存在");
            Map<String, Object> row = rows.get(0);
            BusinessValidation.require(row.get("status") instanceof Number status && status.intValue() == 1, "所选项目已停用");
            BusinessValidation.require(row.get("department") == null || row.get("department").equals(doctorDepartment), "所选项目不属于当前科室");
            BusinessValidation.money((java.math.BigDecimal) row.get("price"), "项目价格");
        }
    }
}
