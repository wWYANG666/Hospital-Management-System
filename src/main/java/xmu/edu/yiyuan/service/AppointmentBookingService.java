package xmu.edu.yiyuan.service;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xmu.edu.yiyuan.controller.PatientController;
import xmu.edu.yiyuan.dto.ApiResponse;
import java.time.LocalDate;
import java.util.List;

/** Serializes capacity checks with the appointment insert on the same database transaction. */
@Service
public class AppointmentBookingService {
    private final JdbcTemplate jdbc;
    private final PatientController patient;

    public AppointmentBookingService(JdbcTemplate jdbc, PatientController patient) {
        this.jdbc = jdbc;
        this.patient = patient;
    }

    @Transactional
    public ResponseEntity<ApiResponse<Object>> book(Long doctorId, LocalDate date, String slot, String type, String symptoms) {
        if (date.isBefore(LocalDate.now())) return ResponseEntity.badRequest().body(ApiResponse.error(400, "不能预约已过去的日期"));
        if (!List.of("GENERAL", "EXPERT").contains(type)) return ResponseEntity.badRequest().body(ApiResponse.error(400, "请选择有效的门诊类型"));
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Long> patients = jdbc.queryForList("SELECT p.id FROM patient p JOIN `user` u ON p.user_id=u.id WHERE u.username=? FOR UPDATE", Long.class, username);
        if (patients.isEmpty()) return ResponseEntity.status(403).body(ApiResponse.forbidden("当前账号未关联就诊人"));
        String workTime = slot.startsWith("9:") || slot.startsWith("10:") || slot.startsWith("11:") ? "MORNING"
                : slot.startsWith("14:") || slot.startsWith("15:") || slot.startsWith("16:") ? "AFTERNOON" : "EVENING";
        jdbc.queryForList("SELECT id FROM schedule WHERE doctor_id=? AND work_date=? AND work_time=? AND schedule_type=? AND status=1 ORDER BY id FOR UPDATE",
                Long.class, doctorId, date, workTime, type);
        return patient.createAppointment(doctorId, date, slot, type, symptoms);
    }
}
