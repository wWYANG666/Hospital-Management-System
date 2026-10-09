package xmu.edu.yiyuan.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import xmu.edu.yiyuan.dto.ApiResponse;
import xmu.edu.yiyuan.entity.*;
import xmu.edu.yiyuan.service.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PatientController {
    @Autowired private PaymentService paymentService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private HospitalizationService hospitalizationService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private UserService userService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private ExaminationService examinationService;

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private MedicalOrderService medicalOrderService;

    @Autowired
    private MedicalRecordService medicalRecordService;

    @Autowired
    private BedService bedService;

    // 获取当前登录用户的患者ID
    private Long getCurrentPatientId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            return userService.findByUsername(username)
                    .flatMap(user -> patientService.findByUserId(user.getId()))
                    .map(Patient::getId)
                    .orElse(null);
        }
        return null;
    }

    // Dashboard首页
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard() {
        Long patientId = getCurrentPatientId();
        if (patientId == null) {
            return ResponseEntity.ok(ApiResponse.unauthorized("未登录或不是患者用户"));
        }

        // 当前登录用户信息（用于首页欢迎语等）
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = null;
        if (auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            currentUser = userService.findByUsername(username).orElse(null);
        }

        // 组装返回数据
        Map<String, Object> data = new HashMap<>();
        data.put("currentUser", currentUser);

        java.time.LocalDateTime now = java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai"));
        LocalDate today = now.toLocalDate();
        List<Appointment> patientAppointments = appointmentService.findByPatientId(patientId);
        // 今日预约和下一次待就诊安排来自同一批本人记录。
        List<Appointment> todayAppointments = patientAppointments.stream()
                .filter(apt -> today.equals(apt.getAppointmentDate())
                        && apt.getStatus() != Appointment.AppointmentStatus.CANCELLED)
                .collect(Collectors.toList());

        Appointment nextAppointment = patientAppointments.stream()
                .filter(apt -> apt.getAppointmentDate() != null && !apt.getAppointmentDate().isBefore(today)
                        && (apt.getStatus() == Appointment.AppointmentStatus.PENDING || apt.getStatus() == Appointment.AppointmentStatus.CONFIRMED)
                        && (!apt.getAppointmentDate().equals(today) || apt.getAppointmentTime() == null
                        || apt.getAppointmentTime().plusHours(1).isAfter(now.toLocalTime())))
                .min(java.util.Comparator.comparing(Appointment::getAppointmentDate)
                        .thenComparing(Appointment::getAppointmentTime, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .orElse(null);
        Map<String, Object> nextVisit = null;
        if (nextAppointment != null) {
            nextVisit = new java.util.LinkedHashMap<>();
            nextVisit.put("id", nextAppointment.getId());
            nextVisit.put("appointmentDate", nextAppointment.getAppointmentDate());
            nextVisit.put("appointmentTime", nextAppointment.getAppointmentTime());
            nextVisit.put("status", nextAppointment.getStatus());
            Doctor doctor = nextAppointment.getDoctorId() == null ? null : doctorService.findById(nextAppointment.getDoctorId()).orElse(null);
            User doctorUser = doctor == null ? null : userService.findById(doctor.getUserId()).orElse(null);
            nextVisit.put("doctorName", doctorUser == null ? null : doctorUser.getRealName());
            nextVisit.put("department", doctor == null ? null : doctor.getDepartment());
            Schedule schedule = nextAppointment.getScheduleId() == null ? null : scheduleService.findById(nextAppointment.getScheduleId()).orElse(null);
            nextVisit.put("clinicType", schedule == null || schedule.getScheduleType() == null ? null : schedule.getScheduleType().getChineseName());
        }
        data.put("nextAppointment", nextVisit);

        // 最近报告（最近5条）
        List<Report> patientReports = reportService.findByPatientId(patientId);
        List<Report> recentReports = patientReports.stream()
                .limit(5)
                .collect(Collectors.toList());

        // 当前住院状态
        List<Hospitalization> patientHospitalizations = hospitalizationService.findByPatientId(patientId);
        List<Hospitalization> currentHospitalizations = patientHospitalizations.stream()
                .filter(h -> h.getStatus() == Hospitalization.HospitalizationStatus.ADMITTED)
                .collect(Collectors.toList());

        data.put("todayAppointments", todayAppointments);
        data.put("recentReports", recentReports);
        data.put("currentHospitalizations", currentHospitalizations);
        // 简单待办统计：今日待就诊 + 当前住院记录数量
        int todoCount = todayAppointments.size() + currentHospitalizations.size();
        data.put("todoCount", todoCount);
        java.util.Set<Long> paidHospitalizations = medicalOrderService.findByPatientId(patientId).stream()
                .filter(order -> order.getHospitalizationId() != null && order.getOrderType() == MedicalOrder.OrderType.OTHER
                        && order.getStatus() == MedicalOrder.OrderStatus.COMPLETED && order.getOrderContent() != null
                        && order.getOrderContent().contains("住院费用已支付"))
                .map(MedicalOrder::getHospitalizationId).collect(Collectors.toSet());
        data.put("pendingPayments", Map.of(
                "prescriptions", prescriptionService.findByPatientId(patientId).stream()
                        .filter(p -> p.getStatus() == Prescription.PrescriptionStatus.PENDING && !Boolean.TRUE.equals(p.getPaid())).count(),
                "reports", patientReports.stream().filter(reportService::needsPatientPayment).count(),
                "hospitalizations", patientHospitalizations.stream()
                        .filter(h -> h.getStatus() == Hospitalization.HospitalizationStatus.DISCHARGED && h.getTotalCost() != null
                                && h.getTotalCost().signum() > 0 && !Boolean.TRUE.equals(h.getPaid()) && !paidHospitalizations.contains(h.getId())).count()));
        data.put("readyReportCount", patientReports.stream()
                .filter(r -> r.getStatus() == Report.ReportStatus.COMPLETED && r.getReportDate() != null
                        && !r.getReportDate().isBefore(now.minusDays(7))).count());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    // 预约挂号 - 步骤1：选择科室
    @GetMapping("/appointments/new")
    public ResponseEntity<ApiResponse<Map<String, Object>>> newAppointmentStep1() {
        Long patientId = getCurrentPatientId();
        if (patientId == null) {
            return ResponseEntity.ok(ApiResponse.unauthorized("未登录或不是患者用户"));
        }

        // 科室列表（用于预约）
        List<Department> departments = departmentService.findAll();
        // 我的挂号（合并展示在预约页面底部）
        List<Appointment> appointments = appointmentService.findByPatientId(patientId);

        Map<Long, Doctor> doctorMap = new java.util.HashMap<>();
        Map<Long, User> userMap = new java.util.HashMap<>();
        Map<Long, MedicalRecord> recordMap = new java.util.HashMap<>();
        Map<Long, String> clinicTypeMap = new java.util.HashMap<>();
        for (Appointment apt : appointments) {
            if (apt.getDoctorId() != null) {
                doctorService.findById(apt.getDoctorId()).ifPresent(doctor -> {
                    doctorMap.put(apt.getDoctorId(), doctor);
                    if (doctor.getUserId() != null) {
                        userService.findById(doctor.getUserId()).ifPresent(user -> userMap.put(apt.getDoctorId(), user));
                    }
                });
            }
            if (apt.getId() != null) {
                medicalRecordService.findByAppointmentId(apt.getId()).ifPresent(record -> {
                    if (record.getPatientId() != null && record.getPatientId().equals(patientId)) {
                        recordMap.put(apt.getId(), record);
                    }
                });
            }
            if (apt.getScheduleId() != null) {
                scheduleService.findById(apt.getScheduleId()).ifPresent(schedule -> {
                    Schedule.ScheduleType type = schedule.getScheduleType() == null ? Schedule.ScheduleType.GENERAL : schedule.getScheduleType();
                    clinicTypeMap.put(apt.getId(), type.getChineseName());
                });
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("departments", departments);
        data.put("appointments", appointments);
        data.put("doctorMap", doctorMap);
        data.put("userMap", userMap);
        data.put("recordMap", recordMap);
        data.put("clinicTypeMap", clinicTypeMap);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    // 预约挂号 - 步骤2：选择医生
    @GetMapping("/appointments/new/step2")
    public ResponseEntity<ApiResponse<Map<String, Object>>> newAppointmentStep2(@RequestParam Long departmentId) {
        Department department = departmentService.findById(departmentId).orElse(null);
        if (department == null) {
            return ResponseEntity.ok(ApiResponse.notFound("科室不存在"));
        }

        // 根据科室名称查找医生（因为Doctor表中有department字段存储科室名称）
        List<Doctor> allDoctors = doctorService.findAll().stream()
                .filter(d -> d.getDepartment() != null
                        && d.getDepartment().equals(department.getName()))
                .collect(Collectors.toList());

        // 只显示有排班的医生（排班日期是今天或未来，且状态为启用）
        LocalDate today = LocalDate.now();
        List<Doctor> doctors = allDoctors.stream()
                .filter(doctor -> {
                    List<Schedule> schedules = scheduleService.findByDoctorId(doctor.getId());
                    return schedules.stream()
                            .anyMatch(s -> s.getWorkDate() != null
                                    && !s.getWorkDate().isBefore(today)
                                    && s.getStatus() != null
                                    && s.getStatus() == 1);
                })
                .collect(Collectors.toList());

        // 加载医生的用户信息（真实姓名）
        java.util.Map<Long, User> userMap = new java.util.HashMap<>();
        for (Doctor doctor : doctors) {
            userService.findById(doctor.getUserId()).ifPresent(user -> {
                userMap.put(doctor.getId(), user);
            });
        }

        Map<String, Object> data = new HashMap<>();
        data.put("doctors", doctors);
        data.put("userMap", userMap);
        data.put("department", department);
        data.put("departmentId", departmentId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    // 预约挂号 - 步骤3：选择日期/时间段
    @GetMapping("/appointments/new/step3")
    public ResponseEntity<ApiResponse<Map<String, Object>>> newAppointmentStep3(@RequestParam Long doctorId,
                                                                                 @RequestParam(required = false) LocalDate date,
                                                                                 @RequestParam(required = false) String error) {
        Doctor doctor = doctorService.findById(doctorId).orElse(null);
        if (doctor == null) {
            return ResponseEntity.ok(ApiResponse.notFound("医生不存在"));
        }

        LocalDate today = LocalDate.now();
        LocalDate selectedDate = date != null ? date : today;

        List<Schedule> enabledSchedules = scheduleService.findByDoctorId(doctorId).stream()
                .filter(s -> s.getStatus() != null && s.getStatus() == 1)
                .filter(s -> s.getWorkDate() != null && !s.getWorkDate().isBefore(today))
                .collect(Collectors.toList());

        List<Schedule> schedulesForDate = new java.util.ArrayList<>();
        for (Schedule s : enabledSchedules) {
            if (s.getWorkDate() != null && s.getWorkDate().equals(selectedDate)) {
                schedulesForDate.add(s);
            }
        }

        final LocalDate finalDate = selectedDate;

        // 创建时间段列表：9:00-10:00, 10:00-11:00, 11:00-12:00, 14:00-15:00, 15:00-16:00, 16:00-17:00, 18:30-19:30, 19:30-20:30, 20:30-21:30
        java.util.List<java.util.Map<String, Object>> timeSlots = new java.util.ArrayList<>();
        String[] morningSlots = {"9:00-10:00", "10:00-11:00", "11:00-12:00"};
        String[] afternoonSlots = {"14:00-15:00", "15:00-16:00", "16:00-17:00"};
        String[] eveningSlots = {"18:30-19:30", "19:30-20:30", "20:30-21:30"};

        // 加载该日期所有预约，用于统计每个时间段的预约数（仅排除已取消的预约）
        List<Appointment> appointmentsForDate = appointmentService.findByDoctorId(doctorId).stream()
                .filter(apt -> apt.getAppointmentDate() != null && apt.getAppointmentDate().equals(finalDate)
                        && apt.getStatus() != Appointment.AppointmentStatus.CANCELLED)
                .collect(Collectors.toList());

        java.util.function.BiFunction<Schedule.WorkTime, Schedule.ScheduleType, Schedule> findSchedule = (wt, st) ->
                schedulesForDate.stream()
                        .filter(s -> s.getWorkTime() == wt)
                        .filter(s -> {
                            Schedule.ScheduleType t = s.getScheduleType() == null ? Schedule.ScheduleType.GENERAL : s.getScheduleType();
                            return t == st;
                        })
                        .findFirst()
                        .orElse(null);

        java.util.function.BiFunction<Long, java.time.LocalTime, Integer> countAppointmentsForScheduleAndTime = (scheduleId, slotStartTime) ->
                (int) appointmentsForDate.stream()
                        .filter(apt -> apt.getScheduleId() != null && apt.getScheduleId().equals(scheduleId))
                        .filter(apt -> apt.getAppointmentTime() != null && apt.getAppointmentTime().equals(slotStartTime))
                        .count();

        // 检查上午时间段
        for (String slot : morningSlots) {
            java.util.Map<String, Object> slotInfo = new java.util.HashMap<>();
            slotInfo.put("time", slot);
            slotInfo.put("workTime", Schedule.WorkTime.MORNING);

            // 解析时间段，获取开始时间
            String[] parts = slot.split("-")[0].split(":");
            java.time.LocalTime slotStartTime = java.time.LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));

            Schedule general = findSchedule.apply(Schedule.WorkTime.MORNING, Schedule.ScheduleType.GENERAL);
            Schedule expert = findSchedule.apply(Schedule.WorkTime.MORNING, Schedule.ScheduleType.EXPERT);

            Integer generalRemaining = null;
            Integer expertRemaining = null;
            Integer generalTotal = null;
            Integer expertTotal = null;

            if (general != null && general.getId() != null) {
                int total = general.getMaxAppointments() != null ? general.getMaxAppointments() : 0;
                int used = countAppointmentsForScheduleAndTime.apply(general.getId(), slotStartTime);
                generalTotal = total;
                generalRemaining = Math.max(0, total - used);
            }
            if (expert != null && expert.getId() != null) {
                int total = expert.getMaxAppointments() != null ? expert.getMaxAppointments() : 0;
                int used = countAppointmentsForScheduleAndTime.apply(expert.getId(), slotStartTime);
                expertTotal = total;
                expertRemaining = Math.max(0, total - used);
            }

            boolean hasAnySchedule = general != null || expert != null;
            boolean available = (generalRemaining != null && generalRemaining > 0) || (expertRemaining != null && expertRemaining > 0);

            slotInfo.put("hasSchedule", hasAnySchedule);
            slotInfo.put("available", hasAnySchedule && available);
            slotInfo.put("generalRemaining", generalRemaining);
            slotInfo.put("expertRemaining", expertRemaining);
            slotInfo.put("generalTotal", generalTotal);
            slotInfo.put("expertTotal", expertTotal);
            slotInfo.put("slotStartTime", slotStartTime.toString()); // 用于后续查找
            timeSlots.add(slotInfo);
        }

        // 检查下午时间段
        for (String slot : afternoonSlots) {
            java.util.Map<String, Object> slotInfo = new java.util.HashMap<>();
            slotInfo.put("time", slot);
            slotInfo.put("workTime", Schedule.WorkTime.AFTERNOON);

            // 解析时间段，获取开始时间
            String[] parts = slot.split("-")[0].split(":");
            java.time.LocalTime slotStartTime = java.time.LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));

            Schedule general = findSchedule.apply(Schedule.WorkTime.AFTERNOON, Schedule.ScheduleType.GENERAL);
            Schedule expert = findSchedule.apply(Schedule.WorkTime.AFTERNOON, Schedule.ScheduleType.EXPERT);

            Integer generalRemaining = null;
            Integer expertRemaining = null;
            Integer generalTotal = null;
            Integer expertTotal = null;

            if (general != null && general.getId() != null) {
                int total = general.getMaxAppointments() != null ? general.getMaxAppointments() : 0;
                int used = countAppointmentsForScheduleAndTime.apply(general.getId(), slotStartTime);
                generalTotal = total;
                generalRemaining = Math.max(0, total - used);
            }
            if (expert != null && expert.getId() != null) {
                int total = expert.getMaxAppointments() != null ? expert.getMaxAppointments() : 0;
                int used = countAppointmentsForScheduleAndTime.apply(expert.getId(), slotStartTime);
                expertTotal = total;
                expertRemaining = Math.max(0, total - used);
            }

            boolean hasAnySchedule = general != null || expert != null;
            boolean available = (generalRemaining != null && generalRemaining > 0) || (expertRemaining != null && expertRemaining > 0);

            slotInfo.put("hasSchedule", hasAnySchedule);
            slotInfo.put("available", hasAnySchedule && available);
            slotInfo.put("generalRemaining", generalRemaining);
            slotInfo.put("expertRemaining", expertRemaining);
            slotInfo.put("generalTotal", generalTotal);
            slotInfo.put("expertTotal", expertTotal);
            slotInfo.put("slotStartTime", slotStartTime.toString()); // 用于后续查找
            timeSlots.add(slotInfo);
        }

        // 检查晚上时间段
        for (String slot : eveningSlots) {
            java.util.Map<String, Object> slotInfo = new java.util.HashMap<>();
            slotInfo.put("time", slot);
            slotInfo.put("workTime", Schedule.WorkTime.EVENING);

            // 解析时间段，获取开始时间（处理30分钟的情况）
            String[] parts = slot.split("-")[0].split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            java.time.LocalTime slotStartTime = java.time.LocalTime.of(hour, minute);

            Schedule general = findSchedule.apply(Schedule.WorkTime.EVENING, Schedule.ScheduleType.GENERAL);
            Schedule expert = findSchedule.apply(Schedule.WorkTime.EVENING, Schedule.ScheduleType.EXPERT);

            Integer generalRemaining = null;
            Integer expertRemaining = null;
            Integer generalTotal = null;
            Integer expertTotal = null;

            if (general != null && general.getId() != null) {
                int total = general.getMaxAppointments() != null ? general.getMaxAppointments() : 0;
                int used = countAppointmentsForScheduleAndTime.apply(general.getId(), slotStartTime);
                generalTotal = total;
                generalRemaining = Math.max(0, total - used);
            }
            if (expert != null && expert.getId() != null) {
                int total = expert.getMaxAppointments() != null ? expert.getMaxAppointments() : 0;
                int used = countAppointmentsForScheduleAndTime.apply(expert.getId(), slotStartTime);
                expertTotal = total;
                expertRemaining = Math.max(0, total - used);
            }

            boolean hasAnySchedule = general != null || expert != null;
            boolean available = (generalRemaining != null && generalRemaining > 0) || (expertRemaining != null && expertRemaining > 0);

            slotInfo.put("hasSchedule", hasAnySchedule);
            slotInfo.put("available", hasAnySchedule && available);
            slotInfo.put("generalRemaining", generalRemaining);
            slotInfo.put("expertRemaining", expertRemaining);
            slotInfo.put("generalTotal", generalTotal);
            slotInfo.put("expertTotal", expertTotal);
            slotInfo.put("slotStartTime", slotStartTime.toString()); // 用于后续查找
            timeSlots.add(slotInfo);
        }

        // 当前患者若已在该医生、该日、该开始时间有未取消预约，则不可再点该格（避免普通/专家余号并存时仍可进入）
        Long currentPatientId = getCurrentPatientId();
        if (currentPatientId != null) {
            Set<java.time.LocalTime> patientBookedStarts = appointmentService.findByPatientId(currentPatientId).stream()
                    .filter(apt -> doctorId.equals(apt.getDoctorId()))
                    .filter(apt -> apt.getAppointmentDate() != null && apt.getAppointmentDate().equals(finalDate))
                    .filter(apt -> apt.getStatus() != Appointment.AppointmentStatus.CANCELLED)
                    .map(Appointment::getAppointmentTime)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            for (Map<String, Object> slotInfo : timeSlots) {
                Object stObj = slotInfo.get("slotStartTime");
                java.time.LocalTime st = null;
                if (stObj instanceof java.time.LocalTime) {
                    st = (java.time.LocalTime) stObj;
                } else if (stObj instanceof String) {
                    st = java.time.LocalTime.parse((String) stObj);
                }
                boolean selfBooked = st != null && patientBookedStarts.contains(st);
                if (selfBooked) {
                    slotInfo.put("available", false);
                    slotInfo.put("bookedBySelf", true);
                } else {
                    slotInfo.put("bookedBySelf", false);
                }
            }
        } else {
            for (Map<String, Object> slotInfo : timeSlots) {
                slotInfo.put("bookedBySelf", false);
            }
        }

        // 加载医生用户信息
        User doctorUser = userService.findById(doctor.getUserId()).orElse(null);

        // 获取科室信息
        Department department = null;
        if (doctor.getDepartment() != null) {
            department = departmentService.findAll().stream()
                    .filter(d -> d.getName().equals(doctor.getDepartment()))
                    .findFirst()
                    .orElse(null);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("doctor", doctor);
        data.put("doctorUser", doctorUser);
        data.put("doctorId", doctorId);
        data.put("selectedDate", finalDate);
        data.put("timeSlots", timeSlots);
        data.put("department", department);
        data.put("minDate", today.toString());
        if (error != null) {
            data.put("error", error);
        }
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    // 预约挂号 - 步骤4：确认挂号
    @GetMapping("/appointments/new/step4")
    public ResponseEntity<ApiResponse<Map<String, Object>>> newAppointmentStep4(
            @RequestParam Long doctorId,
            @RequestParam LocalDate date,
            @RequestParam String timeSlot) {
        Long patientId = getCurrentPatientId();
        if (patientId == null) {
            return ResponseEntity.ok(ApiResponse.unauthorized("未登录或不是患者用户"));
        }

        Doctor doctor = doctorService.findById(doctorId).orElse(null);
        if (doctor == null) {
            return ResponseEntity.ok(ApiResponse.notFound("医生不存在"));
        }

        // 解析时间段，确定是上午、下午还是晚上
        Schedule.WorkTime workTimeValue = null;
        java.time.LocalTime appointmentTimeValue = null;
        if (timeSlot.startsWith("9:") || timeSlot.startsWith("10:") || timeSlot.startsWith("11:")) {
            workTimeValue = Schedule.WorkTime.MORNING;
            // 解析具体时间，取开始时间
            String[] parts = timeSlot.split("-")[0].split(":");
            appointmentTimeValue = java.time.LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        } else if (timeSlot.startsWith("14:") || timeSlot.startsWith("15:") || timeSlot.startsWith("16:")) {
            workTimeValue = Schedule.WorkTime.AFTERNOON;
            // 解析具体时间，取开始时间
            String[] parts = timeSlot.split("-")[0].split(":");
            appointmentTimeValue = java.time.LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        } else if (timeSlot.startsWith("18:") || timeSlot.startsWith("19:") || timeSlot.startsWith("20:")) {
            workTimeValue = Schedule.WorkTime.EVENING;
            // 解析具体时间，取开始时间（处理30分钟的情况）
            String[] parts = timeSlot.split("-")[0].split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            appointmentTimeValue = java.time.LocalTime.of(hour, minute);
        }

        if (workTimeValue == null || appointmentTimeValue == null) {
            return ResponseEntity.ok(ApiResponse.error(400, "时间段格式错误"));
        }

        final java.time.LocalTime parsedSlotStart = appointmentTimeValue;
        boolean alreadyBookedThisSlot = appointmentService.findByPatientId(patientId).stream()
                .anyMatch(apt -> doctorId.equals(apt.getDoctorId())
                        && apt.getAppointmentDate() != null && apt.getAppointmentDate().equals(date)
                        && apt.getAppointmentTime() != null && apt.getAppointmentTime().equals(parsedSlotStart)
                        && apt.getStatus() != Appointment.AppointmentStatus.CANCELLED);
        if (alreadyBookedThisSlot) {
            return ResponseEntity.ok(ApiResponse.error(400, "您已预约该时段，请返回选择其他时间"));
        }

        final Schedule.WorkTime finalWorkTime = workTimeValue;
        final LocalDate finalDate = date;
        final java.time.LocalTime finalAppointmentTime = parsedSlotStart;

        // 查找可预约排班：普通门诊/专家诊（至少存在一个）
        List<Schedule> existingSchedules = scheduleService.findByDoctorId(doctorId).stream()
                .filter(s -> s.getWorkDate() != null && s.getWorkDate().equals(finalDate)
                        && s.getWorkTime() == finalWorkTime
                        && s.getStatus() != null && s.getStatus() == 1)
                .collect(Collectors.toList());
        Schedule generalSchedule = existingSchedules.stream()
                .filter(s -> s.getScheduleType() == null || s.getScheduleType() == Schedule.ScheduleType.GENERAL)
                .findFirst()
                .orElse(null);
        Schedule expertSchedule = existingSchedules.stream()
                .filter(s -> s.getScheduleType() == Schedule.ScheduleType.EXPERT)
                .findFirst()
                .orElse(null);

        // 如果没有排班，重定向回步骤3并显示错误
        if (generalSchedule == null && expertSchedule == null) {
            return ResponseEntity.ok(ApiResponse.error(400, "该医生在该时间段没有排班，无法预约"));
        }

        // 获取科室信息
        Department department = null;
        if (doctor.getDepartment() != null) {
            department = departmentService.findAll().stream()
                    .filter(d -> d.getName().equals(doctor.getDepartment()))
                    .findFirst()
                    .orElse(null);
        }

        // 加载医生用户信息
        User doctorUser = userService.findById(doctor.getUserId()).orElse(null);

        Integer generalCurrent = null;
        Integer generalMax = null;
        if (generalSchedule != null) {
            int current = (int) appointmentService.findByDoctorId(doctorId).stream()
                    .filter(apt -> apt.getAppointmentDate() != null && apt.getAppointmentDate().equals(finalDate)
                            && apt.getAppointmentTime() != null && apt.getAppointmentTime().equals(finalAppointmentTime)
                            && apt.getScheduleId() != null && apt.getScheduleId().equals(generalSchedule.getId())
                            && apt.getStatus() != Appointment.AppointmentStatus.CANCELLED)
                    .count();
            generalCurrent = current;
            generalMax = generalSchedule.getMaxAppointments() != null ? generalSchedule.getMaxAppointments() : 0;
        }

        Integer expertCurrent = null;
        Integer expertMax = null;
        if (expertSchedule != null) {
            int current = (int) appointmentService.findByDoctorId(doctorId).stream()
                    .filter(apt -> apt.getAppointmentDate() != null && apt.getAppointmentDate().equals(finalDate)
                            && apt.getAppointmentTime() != null && apt.getAppointmentTime().equals(finalAppointmentTime)
                            && apt.getScheduleId() != null && apt.getScheduleId().equals(expertSchedule.getId())
                            && apt.getStatus() != Appointment.AppointmentStatus.CANCELLED)
                    .count();
            expertCurrent = current;
            expertMax = expertSchedule.getMaxAppointments() != null ? expertSchedule.getMaxAppointments() : 0;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("schedule", generalSchedule != null ? generalSchedule : expertSchedule);
        data.put("generalCurrent", generalCurrent);
        data.put("generalMax", generalMax);
        data.put("expertCurrent", expertCurrent);
        data.put("expertMax", expertMax);
        data.put("doctor", doctor);
        data.put("doctorUser", doctorUser);
        data.put("department", department);
        data.put("timeSlot", timeSlot);
        data.put("appointmentTime", finalAppointmentTime);
        data.put("doctorId", doctorId);
        data.put("date", finalDate);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    // 提交预约
    @PostMapping("/appointments")
    public ResponseEntity<ApiResponse<Object>> createAppointment(
            @RequestParam Long doctorId,
            @RequestParam LocalDate date,
            @RequestParam String timeSlot,
            @RequestParam(required = false, defaultValue = "GENERAL") String clinicType,
            @RequestParam(required = false) String symptoms) {
        Long patientId = getCurrentPatientId();
        if (patientId == null) {
            return ResponseEntity.ok(ApiResponse.unauthorized("未登录或不是患者用户"));
        }

        // 解析时间段，确定是上午、下午还是晚上
        Schedule.WorkTime workTimeValue = null;
        java.time.LocalTime appointmentTime = null;
        if (timeSlot.startsWith("9:") || timeSlot.startsWith("10:") || timeSlot.startsWith("11:")) {
            workTimeValue = Schedule.WorkTime.MORNING;
            // 解析具体时间，取开始时间
            String[] parts = timeSlot.split("-")[0].split(":");
            appointmentTime = java.time.LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        } else if (timeSlot.startsWith("14:") || timeSlot.startsWith("15:") || timeSlot.startsWith("16:")) {
            workTimeValue = Schedule.WorkTime.AFTERNOON;
            // 解析具体时间，取开始时间
            String[] parts = timeSlot.split("-")[0].split(":");
            appointmentTime = java.time.LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        } else if (timeSlot.startsWith("18:") || timeSlot.startsWith("19:") || timeSlot.startsWith("20:")) {
            workTimeValue = Schedule.WorkTime.EVENING;
            // 解析具体时间，取开始时间（处理30分钟的情况）
            String[] parts = timeSlot.split("-")[0].split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            appointmentTime = java.time.LocalTime.of(hour, minute);
        }

        if (workTimeValue == null) {
            return ResponseEntity.ok(ApiResponse.error(400, "时间段格式错误"));
        }

        final Schedule.WorkTime finalWorkTime = workTimeValue;
        final LocalDate finalDate = date;
        final java.time.LocalTime finalAppointmentTime = appointmentTime;

        Schedule.ScheduleType selectedScheduleTypeTmp;
        try {
            selectedScheduleTypeTmp = Schedule.ScheduleType.valueOf(clinicType);
        } catch (Exception ignored) {
            selectedScheduleTypeTmp = Schedule.ScheduleType.GENERAL;
        }
        final Schedule.ScheduleType selectedScheduleType = selectedScheduleTypeTmp;

        // 查找排班（必须存在该类型排班才能预约）
        List<Schedule> existingSchedules = scheduleService.findByDoctorId(doctorId).stream()
                .filter(s -> s.getWorkDate() != null && s.getWorkDate().equals(finalDate)
                        && s.getWorkTime() == finalWorkTime
                        && (selectedScheduleType == (s.getScheduleType() == null ? Schedule.ScheduleType.GENERAL : s.getScheduleType()))
                        && s.getStatus() != null && s.getStatus() == 1)
                .collect(Collectors.toList());

        Schedule schedule = existingSchedules.isEmpty() ? null : existingSchedules.get(0);

        // 如果没有排班，拒绝预约
        if (schedule == null) {
            return ResponseEntity.ok(ApiResponse.error(400, "该医生在该时间段没有对应号源，无法预约"));
        }

        // 检查该时间段是否已满（按选中的普通/专家排班独立限流）
        List<Appointment> existingAppointmentsForSlot = appointmentService.findByDoctorId(doctorId).stream()
                .filter(apt -> apt.getAppointmentDate() != null && apt.getAppointmentDate().equals(finalDate)
                        && apt.getAppointmentTime() != null && apt.getAppointmentTime().equals(finalAppointmentTime)
                        && apt.getScheduleId() != null && apt.getScheduleId().equals(schedule.getId())
                        && apt.getStatus() != Appointment.AppointmentStatus.CANCELLED)
                .collect(Collectors.toList());

        // 使用排班的maxAppointments作为号源限制
        int maxAppointmentsPerSlot = schedule.getMaxAppointments() != null ? schedule.getMaxAppointments() : 5;

        // 检查是否已满（使用排班的号源限制）
        if (existingAppointmentsForSlot.size() >= maxAppointmentsPerSlot) {
            return ResponseEntity.ok(ApiResponse.error(400, "该时段已满，请选择其他时段"));
        }

        // 检查是否已经预约过该时段（同一患者，仅排除已取消的预约）
        List<Appointment> patientAppointmentsForSlot = appointmentService.findByPatientId(patientId).stream()
                .filter(apt -> apt.getAppointmentDate() != null && apt.getAppointmentDate().equals(finalDate)
                        && apt.getAppointmentTime() != null && apt.getAppointmentTime().equals(finalAppointmentTime)
                        && apt.getStatus() != Appointment.AppointmentStatus.CANCELLED)
                .collect(Collectors.toList());
        if (!patientAppointmentsForSlot.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(400, "您已经预约过该时段"));
        }

        final Schedule finalSchedule = schedule;

        Appointment appointment = new Appointment();
        appointment.setPatientId(patientId);
        appointment.setDoctorId(doctorId);
        appointment.setScheduleId(finalSchedule.getId());
        appointment.setAppointmentDate(finalDate);
        appointment.setAppointmentTime(appointmentTime);
        appointment.setStatus(Appointment.AppointmentStatus.PENDING);
        appointment.setSymptoms(symptoms);
        appointment.setCreatedAt(LocalDateTime.now());

        appointmentService.save(appointment);

        // 注意：不再更新排班的currentAppointments，因为每个时间段独立计算号源
        // 号源通过统计appointment_time相同的预约数来计算

        Map<String, Object> data = new HashMap<>();
        data.put("appointmentId", appointment.getId());
        return ResponseEntity.ok(ApiResponse.success("预约成功", data));
    }

    // 我的挂号
    @GetMapping("/appointments")
    public ResponseEntity<ApiResponse<Object>> myAppointments() {
        Long patientId = getCurrentPatientId();
        if (patientId == null) {
            return ResponseEntity.ok(ApiResponse.unauthorized("未登录或不是患者用户"));
        }
        List<Appointment> appointments = appointmentService.findByPatientId(patientId);
        return ResponseEntity.ok(ApiResponse.success(appointments));
    }

    // 查看本次就诊病历/诊断（患者端）
    @GetMapping("/appointments/{id}/record")
    public ResponseEntity<ApiResponse<Object>> viewRecord(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Long patientId = getCurrentPatientId();
        if (patientId == null) {
            return ResponseEntity.ok(ApiResponse.unauthorized("未登录或不是患者用户"));
        }

        Appointment appointment = appointmentService.findById(id).orElse(null);
        if (appointment == null || appointment.getPatientId() == null || !appointment.getPatientId().equals(patientId)) {
            return ResponseEntity.ok(ApiResponse.forbidden("挂号不存在或无权限查看"));
        }

        // 优先展示电子病历；若历史数据未生成病历，则回退展示挂号表上的诊断/处方字段
        MedicalRecord record = medicalRecordService.findByAppointmentId(id).orElse(null);
        if (record == null) {
            record = new MedicalRecord();
            record.setAppointmentId(id);
            record.setPatientId(patientId);
            record.setDoctorId(appointment.getDoctorId());
            record.setDiagnosis(appointment.getDiagnosis());
            record.setPrescription(appointment.getPrescription());
            record.setCreatedAt(appointment.getUpdatedAt() != null ? appointment.getUpdatedAt() : appointment.getCreatedAt());
            record.setMedicalRecordContent(null);
        }

        // 加载医生信息用于展示
        Doctor doctor = appointment.getDoctorId() != null ? doctorService.findById(appointment.getDoctorId()).orElse(null) : null;
        User doctorUser = (doctor != null && doctor.getUserId() != null) ? userService.findById(doctor.getUserId()).orElse(null) : null;

        model.addAttribute("record", record);
        model.addAttribute("appointment", appointment);
        model.addAttribute("doctor", doctor);
        model.addAttribute("doctorUser", doctorUser);
        return ResponseEntity.ok(ApiResponse.success(model.asMap()));
    }

    // 取消挂号
    @PostMapping("/appointments/{id}/cancel")
    public ResponseEntity<ApiResponse<Object>> cancelAppointment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Long patientId = getCurrentPatientId();
        if (patientId == null) {
            return ResponseEntity.ok(ApiResponse.unauthorized("未登录或不是患者用户"));
        }
        Appointment appointment = appointmentService.findById(id).orElse(null);
        if (appointment == null) {
            return ResponseEntity.ok(ApiResponse.notFound("预约不存在"));
        }
        if (appointment.getPatientId() == null || !appointment.getPatientId().equals(patientId)) {
            return ResponseEntity.ok(ApiResponse.forbidden("无权取消该挂号"));
        }
        if (appointment.getStatus() == Appointment.AppointmentStatus.PENDING
                || appointment.getStatus() == Appointment.AppointmentStatus.CONFIRMED) {
            appointment.setStatus(Appointment.AppointmentStatus.CANCELLED);
            appointmentService.update(appointment);
            return ResponseEntity.ok(ApiResponse.success("挂号已成功取消", null));
        }
        return ResponseEntity.ok(ApiResponse.error(400, "该挂号状态不允许取消"));
    }

    // 买药取药 - 处方列表
    @GetMapping("/prescriptions")
    public ResponseEntity<ApiResponse<Object>> myPrescriptions(Model model) {
        Long patientId = getCurrentPatientId();
        if (patientId == null) {
            return ResponseEntity.ok(ApiResponse.unauthorized("未登录或不是患者用户"));
        }

        List<Prescription> prescriptions = prescriptionService.findByPatientId(patientId);

        // 加载医生信息
        java.util.Map<Long, Doctor> doctorMap = new java.util.HashMap<>();
        java.util.Map<Long, User> userMap = new java.util.HashMap<>();
        for (Prescription prescription : prescriptions) {
            doctorService.findById(prescription.getDoctorId()).ifPresent(doctor -> {
                doctorMap.put(prescription.getDoctorId(), doctor);
                userService.findById(doctor.getUserId()).ifPresent(user -> {
                    userMap.put(prescription.getDoctorId(), user);
                });
            });
        }

        // 诊断中勾选的检查/检验：待缴费的报告单（与医生审阅联动）
        List<Report> examReportsPendingPay = reportService.findByPatientId(patientId).stream()
                .filter(r -> r.getStatus() == Report.ReportStatus.PENDING)
                .filter(r -> r.getExaminationId() != null)
                .filter(r -> reportService.needsPatientPayment(r))
                .sorted(Comparator.comparing(Report::getReportDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
        // 已缴费的检查/检验：展示在「历史处方记录」中（与处方并列）
        List<Report> examReportsPaidHistory = reportService.findByPatientId(patientId).stream()
                .filter(r -> r.getExaminationId() != null)
                .filter(r -> Boolean.TRUE.equals(r.getPaid()))
                .sorted(Comparator.comparing(Report::getPaidAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        Map<Long, Examination> examReportExaminationMap = new HashMap<>();
        for (Report r : examReportsPendingPay) {
            if (r.getExaminationId() != null) {
                examinationService.findById(r.getExaminationId())
                        .ifPresent(ex -> examReportExaminationMap.put(r.getId(), ex));
            }
        }
        for (Report r : examReportsPaidHistory) {
            if (r.getExaminationId() != null && !examReportExaminationMap.containsKey(r.getId())) {
                examinationService.findById(r.getExaminationId())
                        .ifPresent(ex -> examReportExaminationMap.put(r.getId(), ex));
            }
        }

        model.addAttribute("prescriptions", prescriptions);
        model.addAttribute("doctorMap", doctorMap);
        model.addAttribute("userMap", userMap);
        model.addAttribute("examReportsPendingPay", examReportsPendingPay);
        model.addAttribute("examReportsPaidHistory", examReportsPaidHistory);
        model.addAttribute("examReportExaminationMap", examReportExaminationMap);
        return ResponseEntity.ok(ApiResponse.success(model.asMap()));
    }

    /**
     * 检查/检验报告费用支付（模拟）
     */
    @PostMapping("/reports/{id}/pay")
    public ResponseEntity<ApiResponse<Object>> payExamReport(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Long patientId = getCurrentPatientId();
        if (patientId == null) return ResponseEntity.ok(ApiResponse.unauthorized("请先登录患者账号"));
        paymentService.payReport(id, patientId);
        return ResponseEntity.ok(ApiResponse.success("费用已确认支付", null));
    }

    /**
     * 检查/检验费用 - 沙箱支付页
     */
    @GetMapping("/payments/report/{id}/alipay")
    public ResponseEntity<ApiResponse<Object>> examReportAlipaySandbox(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Long patientId = getCurrentPatientId();
        if (patientId == null) {
            return ResponseEntity.ok(ApiResponse.unauthorized("未登录或不是患者用户"));
        }
        Report report = reportService.findById(id).orElse(null);
        if (report == null || !report.getPatientId().equals(patientId)) {
            return ResponseEntity.ok(ApiResponse.forbidden("报告单不存在或无权限"));
        }
        if (!reportService.needsPatientPayment(report)) {
            if (Boolean.TRUE.equals(report.getPaid())) {
                return ResponseEntity.ok(ApiResponse.success("该检查费用已支付", null));
            } else {
                return ResponseEntity.ok(ApiResponse.error(400, "该检查无需缴费或不可在此支付"));
            }
        }
        BigDecimal amount = BigDecimal.ZERO;
        String examName = report.getReportType() != null ? report.getReportType() : "检查/检验";
        if (report.getExaminationId() != null) {
            Examination ex = examinationService.findById(report.getExaminationId()).orElse(null);
            if (ex != null) {
                if (ex.getName() != null) {
                    examName = ex.getName();
                }
                if (ex.getPrice() != null) {
                    amount = ex.getPrice();
                }
            }
        }
        String patientName = "";
        Patient patient = patientService.findById(patientId).orElse(null);
        if (patient != null && patient.getIdCard() != null) {
            patientName = patient.getIdCard();
        }
        model.addAttribute("paymentType", "EXAM_REPORT");
        model.addAttribute("orderNumber", "CK-" + report.getId());
        model.addAttribute("amount", amount);
        model.addAttribute("patientName", patientName);
        model.addAttribute("examReportName", examName);
        model.addAttribute("businessId", id);
        model.addAttribute("cancelUrl", "/patient/prescriptions");
        return ResponseEntity.ok(ApiResponse.success(model.asMap()));
    }

    // 处方详情
    @GetMapping("/prescriptions/{id}")
    public ResponseEntity<ApiResponse<Object>> prescriptionDetail(@PathVariable Long id,
                                                                      @RequestParam(required = false) String returnTo,
                                                                      Model model) {
        Long patientId = getCurrentPatientId();
        if (patientId == null) {
            return ResponseEntity.ok(ApiResponse.unauthorized("未登录或不是患者用户"));
        }

        Prescription prescription = prescriptionService.findById(id).orElse(null);
        if (prescription == null || !prescription.getPatientId().equals(patientId)) {
            return ResponseEntity.ok(ApiResponse.forbidden("处方不存在或无权限"));
        }

        // 加载处方明细
        List<PrescriptionItem> items = prescriptionService.getPrescriptionItems(id);

        // 计算总金额
        java.math.BigDecimal totalAmount = items.stream()
                .filter(item -> item.getTotalPrice() != null)
                .map(PrescriptionItem::getTotalPrice)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        // 加载医生信息
        Doctor doctor = doctorService.findById(prescription.getDoctorId()).orElse(null);
        User doctorUser = null;
        if (doctor != null) {
            doctorUser = userService.findById(doctor.getUserId()).orElse(null);
        }

        model.addAttribute("prescription", prescription);
        model.addAttribute("items", items);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("doctor", doctor);
        model.addAttribute("doctorUser", doctorUser);
        model.addAttribute("returnTo", normalizePatientReturnTo(returnTo));
        return ResponseEntity.ok(ApiResponse.success(model.asMap()));
    }

    private String normalizePatientReturnTo(String returnTo) {
        if (returnTo == null) return null;
        String trimmed = returnTo.trim();
        if (trimmed.isEmpty()) return null;
        // avoid open redirect: only allow in-site patient paths
        if (trimmed.startsWith("/patient/")) return trimmed;
        return null;
    }

    // 扫码取药
    @PostMapping("/prescriptions/{id}/collect")
    public ResponseEntity<ApiResponse<Object>> collectPrescription(@PathVariable Long id,
                                                                      @RequestParam(required = false) String signature,
                                                                      RedirectAttributes redirectAttributes) {
        Long patientId = getCurrentPatientId();
        if (patientId == null) {
            return ResponseEntity.ok(ApiResponse.unauthorized("未登录或不是患者用户"));
        }

        Prescription prescription = prescriptionService.findById(id).orElse(null);
        if (prescription == null || !prescription.getPatientId().equals(patientId)) {
            return ResponseEntity.ok(ApiResponse.forbidden("处方不存在或无权限"));
        }

        if (prescription.getStatus() != Prescription.PrescriptionStatus.PENDING) {
            return ResponseEntity.ok(ApiResponse.error(400, "该处方已处理，无法重复取药"));
        }

        // 必须先付款才能取药确认
        if (prescription.getPaid() == null || !prescription.getPaid()) {
            return ResponseEntity.ok(ApiResponse.error(400, "请先完成付款，再确认取药"));
        }

        if (prescription.getPatientSignature() != null && !prescription.getPatientSignature().trim().isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(400, "您已确认过取药，无需重复操作"));
        }

        // 患者签字确认取药（不改变处方状态，发药由管理员完成）
        prescription.setPatientSignature(signature != null ? signature : "已确认");
        prescription.setCollectedAt(LocalDateTime.now());
        prescriptionService.update(prescription);

        return ResponseEntity.ok(ApiResponse.success("取药确认成功！请到药房窗口领取药品。", null));
    }

    // 处方付款
    @PostMapping("/prescriptions/{id}/pay")
    public ResponseEntity<ApiResponse<Object>> payPrescription(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Long patientId = getCurrentPatientId();
        if (patientId == null) return ResponseEntity.ok(ApiResponse.unauthorized("请先登录患者账号"));
        paymentService.payPrescription(id, patientId);
        return ResponseEntity.ok(ApiResponse.success("费用已确认支付", null));
    }

    /**
     * 处方支付 - 沙箱支付宝页面
     */
    @GetMapping("/payments/prescription/{id}/alipay")
    public ResponseEntity<ApiResponse<Object>> prescriptionAlipaySandbox(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Long patientId = getCurrentPatientId();
        if (patientId == null) {
            return ResponseEntity.ok(ApiResponse.unauthorized("未登录或不是患者用户"));
        }

        Prescription prescription = prescriptionService.findById(id).orElse(null);
        if (prescription == null || !prescription.getPatientId().equals(patientId)) {
            return ResponseEntity.ok(ApiResponse.forbidden("处方不存在或无权限"));
        }

        // 计算总金额
        java.util.List<PrescriptionItem> items = prescriptionService.getPrescriptionItems(id);
        java.math.BigDecimal totalAmount = items.stream()
                .filter(item -> item.getTotalPrice() != null)
                .map(PrescriptionItem::getTotalPrice)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        // 患者姓名
        String patientName = "";
        Patient patient = patientService.findById(prescription.getPatientId()).orElse(null);
        if (patient != null && patient.getIdCard() != null) {
            patientName = patient.getIdCard();
        }

        model.addAttribute("paymentType", "PRESCRIPTION");
        model.addAttribute("orderNumber", prescription.getPrescriptionNumber());
        model.addAttribute("amount", totalAmount != null ? totalAmount : java.math.BigDecimal.ZERO);
        model.addAttribute("patientName", patientName);
        model.addAttribute("prescriptionItems", items);
        model.addAttribute("businessId", id);
        model.addAttribute("cancelUrl", "/patient/prescriptions/" + id);
        return ResponseEntity.ok(ApiResponse.success(model.asMap()));
    }

    /**
     * 住院费用支付 - 沙箱支付宝页面
     */
    @GetMapping("/payments/hospitalization/{id}/alipay")
    public ResponseEntity<ApiResponse<Object>> hospitalizationAlipaySandbox(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Long patientId = getCurrentPatientId();
        if (patientId == null) {
            return ResponseEntity.ok(ApiResponse.unauthorized("未登录或不是患者用户"));
        }

        Hospitalization hospitalization = hospitalizationService.findById(id).orElse(null);
        if (hospitalization == null) {
            return ResponseEntity.ok(ApiResponse.notFound("住院记录不存在"));
        }

        if (!hospitalization.getPatientId().equals(patientId)) {
            return ResponseEntity.ok(ApiResponse.forbidden("无权操作此住院记录"));
        }

        if (hospitalization.getTotalCost() == null || hospitalization.getStatus() != Hospitalization.HospitalizationStatus.DISCHARGED) {
            return ResponseEntity.ok(ApiResponse.error(400, "该住院记录尚未生成费用，无法支付"));
        }

        // 患者姓名
        String patientName = "";
        Patient patient = patientService.findById(hospitalization.getPatientId()).orElse(null);
        if (patient != null && patient.getIdCard() != null) {
            patientName = patient.getIdCard();
        }

        // 关联的医嘱（用于展示住院期间的用药/检查等概要信息）
        java.util.List<MedicalOrder> orders = medicalOrderService.findByHospitalizationId(hospitalization.getId());

        model.addAttribute("paymentType", "HOSPITALIZATION");
        model.addAttribute("orderNumber", "HZ-" + hospitalization.getId());
        model.addAttribute("amount", hospitalization.getTotalCost());
        model.addAttribute("patientName", patientName);
        model.addAttribute("hospitalization", hospitalization);
        model.addAttribute("medicalOrders", orders);
        model.addAttribute("businessId", id);
        model.addAttribute("cancelUrl", "/patient/hospitalizations");
        return ResponseEntity.ok(ApiResponse.success(model.asMap()));
    }

    // 报告单
    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<Object>> myReports(Model model) {
        Long patientId = getCurrentPatientId();
        if (patientId == null) {
            return ResponseEntity.ok(ApiResponse.unauthorized("未登录或不是患者用户"));
        }

        List<Report> reports = reportService.findByPatientId(patientId);

        // 按时间排序（最新的在前）
        reports = reports.stream()
                .sorted((r1, r2) -> {
                    if (r1.getReportDate() == null && r2.getReportDate() == null) return 0;
                    if (r1.getReportDate() == null) return 1;
                    if (r2.getReportDate() == null) return -1;
                    return r2.getReportDate().compareTo(r1.getReportDate());
                })
                .collect(Collectors.toList());

        // 加载检查项目信息，用于显示报告名称和类型
        java.util.Map<Long, Examination> examinationMap = new java.util.HashMap<>();
        for (Report report : reports) {
            if (report.getExaminationId() != null) {
                examinationService.findById(report.getExaminationId()).ifPresent(exam -> {
                    examinationMap.put(report.getId(), exam);
                });
            }
        }

        model.addAttribute("reports", reports);
        model.addAttribute("examinationMap", examinationMap);
        return ResponseEntity.ok(ApiResponse.success(model.asMap()));
    }

    @GetMapping("/reports/{id}")
    public ResponseEntity<ApiResponse<Object>> viewReport(@PathVariable Long id,
                                                            @RequestParam(required = false) String from,
                                                            Model model) {
        Long currentPatientId = getCurrentPatientId();
        if (currentPatientId == null) {
            return ResponseEntity.ok(ApiResponse.unauthorized("未登录或不是患者用户"));
        }
        Report report = reportService.findById(id).orElse(null);
        if (report == null || !currentPatientId.equals(report.getPatientId())) {
            return ResponseEntity.ok(ApiResponse.forbidden("无权查看此报告"));
        }

        // 加载检查项目信息
        Examination examination = null;
        if (report.getExaminationId() != null) {
            examination = examinationService.findById(report.getExaminationId()).orElse(null);
        }

        // 加载患者和医生信息
        Patient patient = patientService.findById(report.getPatientId()).orElse(null);
        User patientUser = null;
        if (patient != null) {
            patientUser = userService.findById(patient.getUserId()).orElse(null);
        }

        Doctor doctor = null;
        User doctorUser = null;
        if (report.getDoctorId() != null) {
            doctor = doctorService.findById(report.getDoctorId()).orElse(null);
            if (doctor != null) {
                doctorUser = userService.findById(doctor.getUserId()).orElse(null);
            }
        }

        model.addAttribute("report", report);
        model.addAttribute("examination", examination);
        model.addAttribute("patient", patient);
        model.addAttribute("patientUser", patientUser);
        model.addAttribute("doctor", doctor);
        model.addAttribute("doctorUser", doctorUser);

        // 返回地址：买药取药 / 我的报告单（可带回列表筛选）/ 首页，防开放重定向仅允许固定来源
        String backUrl = "/patient/reports";
        if ("prescriptions".equals(from)) {
            backUrl = "/patient/prescriptions";
        } else if ("dashboard".equals(from)) {
            backUrl = "/patient/dashboard";
        } else if ("reports".equals(from)) {
            backUrl = "/patient/reports";
        }
        model.addAttribute("backUrl", backUrl);
        return ResponseEntity.ok(ApiResponse.success(model.asMap()));
    }

    // 报告单预览（重定向到详情页面）
    @GetMapping("/reports/{id}/preview")
    public ResponseEntity<ApiResponse<Object>> previewReport(@PathVariable Long id) {
        Map<String, Object> data = new HashMap<>();
        data.put("redirectTo", "/patient/reports/" + id);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    // 报告单PDF下载
    @GetMapping("/reports/{id}/download")
    public void downloadReportPdf(@PathVariable Long id,
                                  jakarta.servlet.http.HttpServletResponse response) {
        try {
            Report report = reportService.findById(id).orElse(null);
            if (report == null) {
                response.sendError(jakarta.servlet.http.HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // 加载检查项目信息
            Examination examination = null;
            if (report.getExaminationId() != null) {
                examination = examinationService.findById(report.getExaminationId()).orElse(null);
            }

            // 加载患者和医生信息
            Patient patient = patientService.findById(report.getPatientId()).orElse(null);
            User patientUser = null;
            if (patient != null) {
                patientUser = userService.findById(patient.getUserId()).orElse(null);
            }

            Doctor doctor = null;
            User doctorUser = null;
            if (report.getDoctorId() != null) {
                doctor = doctorService.findById(report.getDoctorId()).orElse(null);
                if (doctor != null) {
                    doctorUser = userService.findById(doctor.getUserId()).orElse(null);
                }
            }

            // 生成PDF
            com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4);
            response.setContentType("application/pdf");
            String reportName = examination != null ? examination.getName() : (report.getReportType() != null ? report.getReportType() : "报告单");
            String fileName = "报告单_" + reportName + "_" +
                             java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(report.getReportDate()) + ".pdf";
            response.setHeader("Content-Disposition", "attachment; filename=\"" +
                              new String(fileName.getBytes("UTF-8"), "ISO-8859-1") + "\"");

            com.lowagie.text.pdf.PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            // 使用支持中文的字体，避免中文显示为空白/乱码
            com.lowagie.text.pdf.BaseFont baseFont = resolvePdfBaseFont();
            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(baseFont, 18, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font normalFont = new com.lowagie.text.Font(baseFont, 12, com.lowagie.text.Font.NORMAL);

            // 标题
            com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph("医院报告单", titleFont);
            title.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            document.add(title);
            document.add(new com.lowagie.text.Paragraph(" "));

            // 报告信息
            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2, 5});
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);
            table.setSplitRows(true);
            table.setSplitLate(false);

            addTableRow(table, "报告名称", examination != null ? examination.getName() : (report.getReportType() != null ? report.getReportType() : "未知"), normalFont);
            addTableRow(table, "报告类型", examination != null ? (examination.getType() != null ? examination.getType() : "检查") : "检查", normalFont);
            addTableRow(table, "生成时间", report.getReportDate() != null
                ? report.getReportDate().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                : "", normalFont);
            if (patientUser != null) {
                addTableRow(table, "患者姓名", patientUser.getRealName() != null ? patientUser.getRealName() : "", normalFont);
            }
            if (doctorUser != null) {
                addTableRow(table, "医生姓名", doctorUser.getRealName() != null ? doctorUser.getRealName() : "", normalFont);
            }
            addTableRow(table, "状态", report.getStatus() != null ? report.getStatus().getChineseName() : "", normalFont);

            document.add(table);
            document.add(new com.lowagie.text.Paragraph(" "));

            // 报告内容
            com.lowagie.text.Paragraph contentTitle = new com.lowagie.text.Paragraph("报告内容：", normalFont);
            contentTitle.setSpacingBefore(10f);
            document.add(contentTitle);
            if (report.getReportContent() != null && !report.getReportContent().isEmpty()) {
                com.lowagie.text.Paragraph content = new com.lowagie.text.Paragraph(report.getReportContent(), normalFont);
                content.setSpacingBefore(5f);
                document.add(content);
            } else {
                com.lowagie.text.Paragraph noContent = new com.lowagie.text.Paragraph("无", normalFont);
                noContent.setSpacingBefore(5f);
                document.add(noContent);
            }

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                response.sendError(jakarta.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (java.io.IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void addTableRow(com.lowagie.text.pdf.PdfPTable table, String label, String value, com.lowagie.text.Font font) {
        com.lowagie.text.pdf.PdfPCell labelCell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(label, font));
        labelCell.setPadding(8f);
        table.addCell(labelCell);

        com.lowagie.text.pdf.PdfPCell valueCell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(value != null ? value : "", font));
        valueCell.setPadding(8f);
        table.addCell(valueCell);
    }

    private com.lowagie.text.pdf.BaseFont resolvePdfBaseFont() throws java.io.IOException, com.lowagie.text.DocumentException {
        String[] fontPaths = new String[] {
            "C:/Windows/Fonts/msyh.ttc,0",
            "C:/Windows/Fonts/simsun.ttc,0",
            "/System/Library/Fonts/PingFang.ttc,0",
            "/System/Library/Fonts/STHeiti Light.ttc,0",
            "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc",
            "/usr/share/fonts/truetype/arphic/ukai.ttc"
        };

        for (String fontPath : fontPaths) {
            try {
                return com.lowagie.text.pdf.BaseFont.createFont(
                        fontPath,
                        com.lowagie.text.pdf.BaseFont.IDENTITY_H,
                        com.lowagie.text.pdf.BaseFont.NOT_EMBEDDED
                );
            } catch (Exception ignored) {
                // 尝试下一个系统字体
            }
        }

        // CJK 内置回退，尽量保证中文可显示
        return com.lowagie.text.pdf.BaseFont.createFont(
                "STSong-Light",
                "UniGB-UCS2-H",
                com.lowagie.text.pdf.BaseFont.NOT_EMBEDDED
        );
    }

    // 住院信息
    @GetMapping("/hospitalizations")
    public ResponseEntity<ApiResponse<Object>> myHospitalizations(Model model) {
        Long patientId = getCurrentPatientId();
        if (patientId == null) {
            return ResponseEntity.ok(ApiResponse.unauthorized("未登录或不是患者用户"));
        }

        List<Hospitalization> hospitalizations = hospitalizationService.findByPatientId(patientId);
        List<Hospitalization> current = hospitalizations.stream()
                .filter(h -> h.getStatus() == Hospitalization.HospitalizationStatus.ADMITTED)
                .collect(Collectors.toList());
        List<Hospitalization> history = hospitalizations.stream()
                .filter(h -> h.getStatus() == Hospitalization.HospitalizationStatus.DISCHARGED)
                .collect(Collectors.toList());

        // 诊断展示兜底：部分数据可能未写入 hospitalization.diagnosis，这里从关联病历/出院诊断回填一个可展示值
        java.util.function.Consumer<Hospitalization> fillDiagnosisIfBlank = (h) -> {
            if (h == null) return;
            String diag = h.getDiagnosis();
            if (diag != null && !diag.trim().isEmpty()) return;

            // 1) 若有出院诊断，优先展示
            if (h.getDischargeDiagnosis() != null && !h.getDischargeDiagnosis().trim().isEmpty()) {
                h.setDiagnosis(h.getDischargeDiagnosis());
                return;
            }

            // 2) 尝试从住院关联病历里取最近一条诊断
            if (h.getId() != null) {
                try {
                    List<MedicalRecord> records = medicalRecordService.findByHospitalizationId(h.getId());
                    for (MedicalRecord r : records) {
                        if (r != null && r.getDiagnosis() != null && !r.getDiagnosis().trim().isEmpty()) {
                            h.setDiagnosis(r.getDiagnosis());
                            return;
                        }
                    }
                } catch (Exception ignored) {
                    // ignore
                }
            }
        };
        current.forEach(fillDiagnosisIfBlank);
        history.forEach(fillDiagnosisIfBlank);

        // 统计每条住院记录是否已提交过出院申请（用于前端禁用按钮）
        java.util.Map<Long, Boolean> dischargeRequestedMap = new java.util.HashMap<>();
        // 统计每条住院记录是否已支付（双重来源：paid 字段 + “住院费用已支付”医嘱）
        java.util.Map<Long, Boolean> paidMap = new java.util.HashMap<>();
        List<MedicalOrder> patientOrders = medicalOrderService.findByPatientId(patientId);
        for (MedicalOrder order : patientOrders) {
            if (order.getHospitalizationId() != null
                    && order.getOrderType() == MedicalOrder.OrderType.OTHER
                    && order.getStatus() == MedicalOrder.OrderStatus.ACTIVE
                    && order.getOrderContent() != null
                    && order.getOrderContent().contains("申请出院")) {
                dischargeRequestedMap.put(order.getHospitalizationId(), true);
            }
            if (order.getHospitalizationId() != null
                    && order.getOrderType() == MedicalOrder.OrderType.OTHER
                    && order.getStatus() == MedicalOrder.OrderStatus.COMPLETED
                    && order.getOrderContent() != null
                    && order.getOrderContent().contains("住院费用已支付")) {
                paidMap.put(order.getHospitalizationId(), true);
            }
        }
        // 同时合并实体上的 paid 字段（如果数据库已支持）
        for (Hospitalization h : hospitalizations) {
            if (h.getId() != null && Boolean.TRUE.equals(h.getPaid())) {
                paidMap.put(h.getId(), true);
            }
        }

        model.addAttribute("currentHospitalizations", current);
        model.addAttribute("historyHospitalizations", history);
        model.addAttribute("dischargeRequestedMap", dischargeRequestedMap);
        model.addAttribute("paidMap", paidMap);
        return ResponseEntity.ok(ApiResponse.success(model.asMap()));
    }

    /**
     * 患者查看单次住院的医嘱列表与病历记录（仅本人）
     */
    @GetMapping("/hospitalizations/{id}/detail")
    public ResponseEntity<ApiResponse<Object>> hospitalizationDetail(@PathVariable Long id, Model model) {
        Long patientId = getCurrentPatientId();
        if (patientId == null) {
            return ResponseEntity.ok(ApiResponse.unauthorized("未登录或不是患者用户"));
        }
        Hospitalization hospitalization = hospitalizationService.findById(id).orElse(null);
        if (hospitalization == null || !patientId.equals(hospitalization.getPatientId())) {
            return ResponseEntity.ok(ApiResponse.forbidden("无权查看此住院记录"));
        }

        List<MedicalOrder> orders = medicalOrderService.findByHospitalizationId(id);
        orders.sort(Comparator.comparing(MedicalOrder::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        List<MedicalRecord> records;
        try {
            records = medicalRecordService.findByHospitalizationId(id);
        } catch (Exception e) {
            records = medicalRecordService.findByPatientId(patientId).stream()
                    .filter(r -> r.getHospitalizationId() != null && r.getHospitalizationId().equals(id))
                    .collect(Collectors.toList());
        }
        records.sort(Comparator.comparing(MedicalRecord::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        String doctorDisplay = null;
        if (hospitalization.getDoctorId() != null) {
            doctorDisplay = doctorService.findById(hospitalization.getDoctorId())
                    .map(d -> {
                        if (d.getUserId() != null) {
                            return userService.findById(d.getUserId())
                                    .map(User::getRealName)
                                    .filter(n -> n != null && !n.isBlank())
                                    .orElse("医生#" + d.getId());
                        }
                        return d.getDoctorCode() != null ? d.getDoctorCode() : "医生#" + d.getId();
                    })
                    .orElse(null);
        }

        model.addAttribute("hospitalization", hospitalization);
        model.addAttribute("orders", orders);
        model.addAttribute("records", records);
        model.addAttribute("doctorDisplay", doctorDisplay);
        return ResponseEntity.ok(ApiResponse.success(model.asMap()));
    }

    // 申请出院（患者端）
    @PostMapping("/hospitalizations/{id}/request-discharge")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<ApiResponse<Object>> requestDischarge(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        hospitalizationService.lock(id);
        Long patientId = getCurrentPatientId();
        if (patientId == null) {
            return ResponseEntity.ok(ApiResponse.unauthorized("未登录或不是患者用户"));
        }

        Hospitalization hospitalization = hospitalizationService.findById(id).orElse(null);
        if (hospitalization == null || !hospitalization.getPatientId().equals(patientId)) {
            return ResponseEntity.ok(ApiResponse.forbidden("住院记录不存在或无权限"));
        }

        if (hospitalization.getStatus() != Hospitalization.HospitalizationStatus.ADMITTED) {
            return ResponseEntity.ok(ApiResponse.error(400, "仅住院中的记录可以申请出院"));
        }

        // 关键业务规则：未分配床位（住院未正式生效）时，不允许申请出院，只能取消住院申请
        boolean hasBed = hospitalization.getBedId() != null;
        if (!hasBed) {
            return ResponseEntity.ok(ApiResponse.error(400, "住院申请尚未完成床位分配，无法申请出院"));
        }

        // 检查是否已提交过出院申请（通过“其他”类型医嘱标记）
        List<MedicalOrder> orders = medicalOrderService.findByHospitalizationId(hospitalization.getId());
        boolean alreadyRequested = orders.stream()
                .anyMatch(o -> o.getOrderType() == MedicalOrder.OrderType.OTHER
                        && o.getStatus() == MedicalOrder.OrderStatus.ACTIVE
                        && o.getOrderContent() != null
                        && o.getOrderContent().contains("申请出院"));
        if (alreadyRequested) {
            return ResponseEntity.ok(ApiResponse.error(400, "您已提交过出院申请，请等待医生处理"));
        }

        MedicalOrder order = new MedicalOrder();
        order.setHospitalizationId(hospitalization.getId());
        order.setPatientId(patientId);
        order.setDoctorId(hospitalization.getDoctorId());
        order.setOrderType(MedicalOrder.OrderType.OTHER);
        order.setOrderContent("患者申请出院");
        order.setStatus(MedicalOrder.OrderStatus.ACTIVE);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        medicalOrderService.save(order);

        return ResponseEntity.ok(ApiResponse.success("出院申请已提交，请等待医生处理", null));
    }

    // 取消住院申请（患者端）：仅允许在未完成床位分配/管理员审核前取消
    @PostMapping("/hospitalizations/{id}/cancel")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<ApiResponse<Object>> cancelHospitalization(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        hospitalizationService.lock(id);
        Long patientId = getCurrentPatientId();
        if (patientId == null) {
            return ResponseEntity.ok(ApiResponse.unauthorized("未登录或不是患者用户"));
        }

        Hospitalization hospitalization = hospitalizationService.findById(id).orElse(null);
        if (hospitalization == null || hospitalization.getPatientId() == null || !hospitalization.getPatientId().equals(patientId)) {
            return ResponseEntity.ok(ApiResponse.forbidden("住院记录不存在或无权限"));
        }

        // 已审核通过且已分配床位：住院正式生效，不允许直接取消
        boolean hasBed = hospitalization.getBedId() != null;
        if (hasBed) {
            return ResponseEntity.ok(ApiResponse.error(400, "当前住院已生效，无法取消，请改为申请出院"));
        }

        // 若存在医生预留床位（OCCUPIED），取消时释放床位
        if (hospitalization.getBedId() != null) {
            bedService.findById(hospitalization.getBedId()).ifPresent(bed -> {
                if (bed.getStatus() == Bed.BedStatus.OCCUPIED) {
                    bed.setStatus(Bed.BedStatus.AVAILABLE);
                    bedService.update(bed);
                }
            });
        }

        // 取消：直接删除该住院申请记录（未生效的申请）
        hospitalizationService.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("住院申请已取消", null));
    }

    // 住院费用支付（患者端）
    @PostMapping("/hospitalizations/{id}/pay")
    public ResponseEntity<ApiResponse<Object>> payHospitalization(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Long patientId = getCurrentPatientId();
        if (patientId == null) return ResponseEntity.ok(ApiResponse.unauthorized("请先登录患者账号"));
        paymentService.payHospitalization(id, patientId);
        return ResponseEntity.ok(ApiResponse.success("费用已确认支付", null));
    }

    // 付款记录
    @GetMapping("/payments/history")
    public ResponseEntity<ApiResponse<Object>> paymentHistory(Model model) {
        Long patientId = getCurrentPatientId();
        if (patientId == null) return ResponseEntity.ok(ApiResponse.unauthorized("请先登录患者账号"));
        List<PaymentRecordView> records = new java.util.ArrayList<>();
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        for (Map<String, Object> payment : paymentService.history(patientId)) {
            String kind = String.valueOf(payment.get("business_type"));
            long id = ((Number) payment.get("business_id")).longValue();
            java.math.BigDecimal amount = (java.math.BigDecimal) payment.get("amount");
            Object paidAt = payment.get("paid_at");
            java.time.LocalDateTime time = paidAt instanceof java.time.LocalDateTime value ? value
                    : ((java.sql.Timestamp) paidAt).toLocalDateTime();
            String type, number, detail;
            switch (kind) {
                case "PRESCRIPTION" -> { type = "处方缴费"; number = "RX-" + id; detail = "/patient/prescriptions/" + id; }
                case "EXAM_REPORT" -> { type = "检查缴费"; number = "CK-" + id; detail = "/patient/reports/" + id; }
                case "HOSPITALIZATION" -> { type = "住院缴费"; number = "HZ-" + id; detail = "/patient/hospitalizations/" + id + "/detail"; }
                default -> { continue; }
            }
            records.add(new PaymentRecordView(type, number, amount, time, detail, type));
            total = total.add(amount);
        }
        model.addAttribute("paymentRecords", records);
        model.addAttribute("totalPaid", total);
        return ResponseEntity.ok(ApiResponse.success(model.asMap()));
    }

    // 说明：患者端不再提供“申请住院”，仅医生端可发起住院申请

    // 个人信息
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Object>> profile(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            userService.findByUsername(username).ifPresent(user -> {
                model.addAttribute("user", user);
                patientService.findByUserId(user.getId()).ifPresent(patient -> {
                    model.addAttribute("patient", patient);
                });
            });
        }
        return ResponseEntity.ok(ApiResponse.success(model.asMap()));
    }

    @PostMapping("/profile")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<ApiResponse<Object>> updateProfile(@RequestParam(required = false) String realName,
                               @RequestParam(required = false) String email,
                               @RequestParam(required = false) String phone,
                               @RequestParam(required = false) String idCard,
                               @RequestParam(required = false) String gender,
                               @RequestParam(required = false) String birthday,
                               @RequestParam(required = false) String address,
                               @RequestParam(required = false) String emergencyContact,
                               @RequestParam(required = false) String emergencyPhone) {
        if (realName != null) BusinessValidation.text(realName, 50, "姓名");
        BusinessValidation.optional(email, 100, "邮箱");
        BusinessValidation.optional(address, 200, "地址");
        BusinessValidation.optional(emergencyContact, 50, "紧急联系人");
        if (phone != null && !phone.isBlank()) BusinessValidation.require(phone.matches("1[3-9]\\d{9}"), "手机号格式不正确");
        if (emergencyPhone != null && !emergencyPhone.isBlank()) BusinessValidation.require(emergencyPhone.matches("1[3-9]\\d{9}"), "紧急联系人手机号格式不正确");
        if (idCard != null && !idCard.isBlank()) BusinessValidation.require(idCard.matches("\\d{17}[0-9Xx]"), "身份证格式不正确");
        if (email != null && !email.isBlank()) BusinessValidation.require(email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"), "邮箱格式不正确");
        final Patient.Gender parsedGender = gender == null || gender.isBlank() ? null : Patient.Gender.valueOf(gender);
        final LocalDate parsedBirthday = birthday == null || birthday.isBlank() ? null : LocalDate.parse(birthday);
        BusinessValidation.require(parsedBirthday == null || !parsedBirthday.isAfter(LocalDate.now()), "出生日期不能晚于今天");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.ok(ApiResponse.unauthorized("未登录或不是患者用户"));
        }

        if (auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            userService.findByUsername(username).ifPresent(existingUser -> {
                if (realName != null) existingUser.setRealName(realName);
                if (email != null) existingUser.setEmail(email);
                if (phone != null) existingUser.setPhone(phone);
                userService.update(existingUser);

                patientService.findByUserId(existingUser.getId()).ifPresent(existingPatient -> {
                    if (idCard != null) existingPatient.setIdCard(idCard);
                    if (parsedGender != null) existingPatient.setGender(parsedGender);
                    if (birthday != null) existingPatient.setBirthday(parsedBirthday);
                    if (address != null) existingPatient.setAddress(address);
                    if (emergencyContact != null) existingPatient.setEmergencyContact(emergencyContact);
                    if (emergencyPhone != null) existingPatient.setEmergencyPhone(emergencyPhone);
                    patientService.update(existingPatient);
                });
            });
        }
        return ResponseEntity.ok(ApiResponse.success("更新成功", null));
    }

    public static class PaymentRecordView {
        private final String type;
        private final String orderNo;
        private final java.math.BigDecimal amount;
        private final java.time.LocalDateTime payTime;
        private final String detailUrl;
        private final String description;

        public PaymentRecordView(String type, String orderNo, java.math.BigDecimal amount,
                                 java.time.LocalDateTime payTime, String detailUrl, String description) {
            this.type = type;
            this.orderNo = orderNo;
            this.amount = amount;
            this.payTime = payTime;
            this.detailUrl = detailUrl;
            this.description = description;
        }

        public String getType() {
            return type;
        }

        public String getOrderNo() {
            return orderNo;
        }

        public java.math.BigDecimal getAmount() {
            return amount;
        }

        public java.time.LocalDateTime getPayTime() {
            return payTime;
        }

        public String getDetailUrl() {
            return detailUrl;
        }

        public String getDescription() {
            return description;
        }
    }

}
