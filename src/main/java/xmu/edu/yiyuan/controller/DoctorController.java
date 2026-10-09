package xmu.edu.yiyuan.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;
import xmu.edu.yiyuan.entity.*;
import xmu.edu.yiyuan.service.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/doctor")
public class DoctorController {
    @Autowired private DiagnosisValidator diagnosisValidator;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private HospitalizationService hospitalizationService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private UserService userService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private BedService bedService;

    @Autowired
    private MedicineService medicineService;

    @Autowired
    private ExaminationService examinationService;

    @Autowired
    private MedicalRecordService medicalRecordService;

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private AiSuggestionService aiSuggestionService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private MedicalOrderService medicalOrderService;

    // 获取当前登录医生的ID
    private Long getCurrentDoctorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            return userService.findByUsername(username)
                    .flatMap(user -> doctorService.findByUserId(user.getId()))
                    .map(Doctor::getId)
                    .orElse(null);
        }
        return null;
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "doctor:unknown";
    }

    // Dashboard首页
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Long doctorId = getCurrentDoctorId();
        if (doctorId == null) {
            // 已登录但无 Doctor 档案时，勿重定向到登录页（易与“登录失败”混淆）
            return "doctor/account-unbound";
        }

        LocalDate today = LocalDate.now();

        // 今日排班统计（仅统计启用状态）
        List<Schedule> todaySchedules = scheduleService.findByDoctorIdAndDate(doctorId, today).stream()
                .filter(s -> s.getStatus() != null && s.getStatus() == 1)
                .collect(Collectors.toList());

        // 统计今日各时段已预约人数：基于 appointment.scheduleId（排除已取消）
        List<Appointment> doctorTodayAppointments = appointmentService.findByDoctorId(doctorId).stream()
                .filter(apt -> apt.getAppointmentDate() != null
                        && apt.getAppointmentDate().equals(today)
                        && apt.getStatus() != Appointment.AppointmentStatus.CANCELLED)
                .collect(Collectors.toList());

        // 为首页准备各时段的接诊情况数据
        java.util.List<java.util.Map<String, Object>> scheduleSummaryList = new java.util.ArrayList<>();

        // 按时段分组处理
        for (Schedule.WorkTime workTime : Schedule.WorkTime.values()) {
            List<Schedule> schedulesForTime = todaySchedules.stream()
                    .filter(s -> s.getWorkTime() == workTime)
                    .sorted((a, b) -> {
                        if (a.getId() != null && b.getId() != null) {
                            return a.getId().compareTo(b.getId());
                        }
                        return 0;
                    })
                    .collect(Collectors.toList());

            if (schedulesForTime.isEmpty()) {
                continue;
            }

            // 统计该时段的总号源和已预约
            int totalMax = schedulesForTime.stream()
                    .mapToInt(s -> s.getMaxAppointments() != null ? s.getMaxAppointments() : 0)
                    .sum();

            int totalUsed = 0;
            List<Appointment> timeAppointments = new java.util.ArrayList<>();
            java.util.Map<Long, String> appointmentTimeSlotMap = new java.util.HashMap<>();

            for (Schedule schedule : schedulesForTime) {
                if (schedule.getId() == null) {
                    continue;
                }
                List<Appointment> scheduleAppts = doctorTodayAppointments.stream()
                        .filter(a -> a.getScheduleId() != null && a.getScheduleId().equals(schedule.getId()))
                        .sorted((a, b) -> {
                            if (a.getAppointmentTime() != null && b.getAppointmentTime() != null) {
                                return a.getAppointmentTime().compareTo(b.getAppointmentTime());
                            }
                            return 0;
                        })
                        .collect(Collectors.toList());
                totalUsed += scheduleAppts.size();
                timeAppointments.addAll(scheduleAppts);

                // 为每个预约计算时间段
                for (Appointment apt : scheduleAppts) {
                    if (apt.getAppointmentTime() != null && apt.getId() != null) {
                        String timeSlot = calculateTimeSlot(apt.getAppointmentTime(), workTime);
                        appointmentTimeSlotMap.put(apt.getId(), timeSlot);
                    }
                }
            }

            String workTimeName = workTime == Schedule.WorkTime.MORNING ? "上午" :
                    (workTime == Schedule.WorkTime.AFTERNOON ? "中午" : "晚上");

            java.util.Map<String, Object> summary = new java.util.HashMap<>();
            summary.put("workTime", workTime);
            summary.put("workTimeName", workTimeName);
            summary.put("totalMax", totalMax);
            summary.put("totalUsed", totalUsed);
            summary.put("totalAvailable", totalMax - totalUsed);
            summary.put("appointments", timeAppointments);
            summary.put("appointmentTimeSlotMap", appointmentTimeSlotMap);
            scheduleSummaryList.add(summary);
        }

        // 合并所有时间段映射
        java.util.Map<Long, String> allTimeSlotMap = new java.util.HashMap<>();
        for (java.util.Map<String, Object> summary : scheduleSummaryList) {
            @SuppressWarnings("unchecked")
            java.util.Map<Long, String> map = (java.util.Map<Long, String>) summary.get("appointmentTimeSlotMap");
            if (map != null) {
                allTimeSlotMap.putAll(map);
            }
        }

        // 待处理：新挂号患者（今日待诊）
        List<Appointment> todayPendingAppointments = appointmentService.findByDoctorId(doctorId).stream()
                .filter(apt -> apt.getAppointmentDate().equals(today)
                        && (apt.getStatus() == Appointment.AppointmentStatus.PENDING
                        || apt.getStatus() == Appointment.AppointmentStatus.CONFIRMED))
                .collect(Collectors.toList());

        // 近期患者动态：已完成诊断且有病历记录的患者（最近7天）
        LocalDate sevenDaysAgo = today.minusDays(7);
        List<Appointment> recentCompletedAppointments = appointmentService.findByDoctorId(doctorId).stream()
                .filter(apt -> apt.getAppointmentDate() != null
                        && !apt.getAppointmentDate().isBefore(sevenDaysAgo)
                        && apt.getStatus() == Appointment.AppointmentStatus.COMPLETED)
                .filter(apt -> apt.getId() != null && medicalRecordService.findByAppointmentId(apt.getId()).isPresent())
                .sorted((a, b) -> {
                    // 按日期和时间倒序排列
                    if (a.getAppointmentDate() != null && b.getAppointmentDate() != null) {
                        int dateCmp = b.getAppointmentDate().compareTo(a.getAppointmentDate());
                        if (dateCmp != 0) return dateCmp;
                    }
                    if (a.getAppointmentTime() != null && b.getAppointmentTime() != null) {
                        return b.getAppointmentTime().compareTo(a.getAppointmentTime());
                    }
                    return 0;
                })
                .limit(10) // 最多显示10条
                .collect(Collectors.toList());

        // 待处理：住院申请/出院申请（合并统计）
        List<Hospitalization> pendingHospitalizations = hospitalizationService.findByDoctorId(doctorId).stream()
                .filter(h -> h.getBedId() == null)
                .collect(Collectors.toList());

        java.util.Set<Long> dischargeRequestHospIds = new java.util.HashSet<>();
        List<MedicalOrder> myOrders = medicalOrderService.findByDoctorId(doctorId);
        for (MedicalOrder o : myOrders) {
            if (o.getHospitalizationId() != null
                    && o.getOrderType() == MedicalOrder.OrderType.OTHER
                    && o.getStatus() == MedicalOrder.OrderStatus.ACTIVE
                    && o.getOrderContent() != null
                    && o.getOrderContent().contains("申请出院")) {
                dischargeRequestHospIds.add(o.getHospitalizationId());
            }
        }
        List<Hospitalization> pendingDischargeHospitalizations = hospitalizationService.findByDoctorId(doctorId).stream()
                .filter(h -> h.getId() != null
                        && dischargeRequestHospIds.contains(h.getId())
                        && h.getStatus() == Hospitalization.HospitalizationStatus.ADMITTED)
                .collect(Collectors.toList());
        int totalPendingHospTasks = pendingHospitalizations.size() + pendingDischargeHospitalizations.size();

        // 待处理：待完成报告单
        List<Report> pendingReports = reportService.findByDoctorId(doctorId).stream()
                .filter(r -> r.getStatus() == Report.ReportStatus.PENDING)
                .collect(Collectors.toList());

        // 加载患者信息（用于首页显示）
        java.util.Map<Long, Patient> patientMapForDashboard = new java.util.HashMap<>();
        java.util.Map<Long, User> userMapForDashboard = new java.util.HashMap<>();

        for (Appointment apt : doctorTodayAppointments) {
            patientService.findById(apt.getPatientId()).ifPresent(patient -> {
                patientMapForDashboard.put(apt.getPatientId(), patient);
                userService.findById(patient.getUserId()).ifPresent(user -> {
                    userMapForDashboard.put(apt.getPatientId(), user);
                });
            });
        }

        // 加载"近期患者动态"中的患者信息
        java.util.Map<Long, Patient> recentPatientMap = new java.util.HashMap<>();
        java.util.Map<Long, User> recentUserMap = new java.util.HashMap<>();
        for (Appointment apt : recentCompletedAppointments) {
            patientService.findById(apt.getPatientId()).ifPresent(patient -> {
                recentPatientMap.put(apt.getPatientId(), patient);
                userService.findById(patient.getUserId()).ifPresent(user -> {
                    recentUserMap.put(apt.getPatientId(), user);
                });
            });
        }

        model.addAttribute("scheduleSummaryList", scheduleSummaryList);
        model.addAttribute("patientMapForDashboard", patientMapForDashboard);
        model.addAttribute("userMapForDashboard", userMapForDashboard);
        model.addAttribute("allTimeSlotMap", allTimeSlotMap);
        model.addAttribute("todayPendingAppointments", todayPendingAppointments);
        model.addAttribute("recentCompletedAppointments", recentCompletedAppointments);
        model.addAttribute("recentPatientMap", recentPatientMap);
        model.addAttribute("recentUserMap", recentUserMap);
        model.addAttribute("pendingHospitalizations", pendingHospitalizations);
        model.addAttribute("pendingDischargeHospitalizations", pendingDischargeHospitalizations);
        model.addAttribute("totalPendingHospTasks", totalPendingHospTasks);
        model.addAttribute("pendingReports", pendingReports);
        return "doctor/dashboard";
    }

    // 计算预约时间段
    private String calculateTimeSlot(java.time.LocalTime appointmentTime, Schedule.WorkTime workTime) {
        int hour = appointmentTime.getHour();
        int minute = appointmentTime.getMinute();

        if (workTime == Schedule.WorkTime.MORNING) {
            // 上午：9:00-10:00, 10:00-11:00, 11:00-12:00
            if (hour == 9) {
                return "9:00-10:00";
            } else if (hour == 10) {
                return "10:00-11:00";
            } else if (hour == 11) {
                return "11:00-12:00";
            }
        } else if (workTime == Schedule.WorkTime.AFTERNOON) {
            // 下午：14:00-15:00, 15:00-16:00, 16:00-17:00
            if (hour == 14) {
                return "14:00-15:00";
            } else if (hour == 15) {
                return "15:00-16:00";
            } else if (hour == 16) {
                return "16:00-17:00";
            }
        } else if (workTime == Schedule.WorkTime.EVENING) {
            // 晚上：18:30-19:30, 19:30-20:30, 20:30-21:30
            if (hour == 18 && minute == 30) {
                return "18:30-19:30";
            } else if (hour == 19 && minute == 30) {
                return "19:30-20:30";
            } else if (hour == 20 && minute == 30) {
                return "20:30-21:30";
            }
        }

        // 如果无法匹配，返回原始时间
        return appointmentTime.toString();
    }

    // 查看今日某时段的接诊情况
    @GetMapping("/schedule-detail")
    public String scheduleDetail(@RequestParam String workTime, Model model) {
        Long doctorId = getCurrentDoctorId();
        if (doctorId == null) {
            return "redirect:/login/user";
        }

        LocalDate today = LocalDate.now();
        Schedule.WorkTime workTimeEnum;
        try {
            workTimeEnum = Schedule.WorkTime.valueOf(workTime);
        } catch (IllegalArgumentException e) {
            return "redirect:/doctor/dashboard";
        }

        // 查找该时段的排班
        List<Schedule> schedules = scheduleService.findByDoctorIdAndDate(doctorId, today).stream()
                .filter(s -> s.getStatus() != null && s.getStatus() == 1)
                .filter(s -> s.getWorkTime() == workTimeEnum)
                .sorted((a, b) -> {
                    // 按排班ID排序，确保顺序一致
                    if (a.getId() != null && b.getId() != null) {
                        return a.getId().compareTo(b.getId());
                    }
                    return 0;
                })
                .collect(Collectors.toList());

        // 获取该时段的所有预约（排除已取消）
        List<Appointment> allAppointments = appointmentService.findByDoctorId(doctorId).stream()
                .filter(apt -> apt.getAppointmentDate() != null && apt.getAppointmentDate().equals(today))
                .filter(apt -> apt.getStatus() != Appointment.AppointmentStatus.CANCELLED)
                .collect(Collectors.toList());

        // 为每个schedule创建数据结构，包含该schedule的预约列表
        java.util.List<java.util.Map<String, Object>> scheduleDetails = new java.util.ArrayList<>();
        for (Schedule schedule : schedules) {
            java.util.Map<String, Object> detail = new java.util.HashMap<>();
            detail.put("schedule", schedule);

            // 获取该schedule的预约列表
            List<Appointment> scheduleAppointments = allAppointments.stream()
                    .filter(apt -> apt.getScheduleId() != null && apt.getScheduleId().equals(schedule.getId()))
                    .sorted((a, b) -> {
                        if (a.getAppointmentTime() != null && b.getAppointmentTime() != null) {
                            return a.getAppointmentTime().compareTo(b.getAppointmentTime());
                        }
                        return 0;
                    })
                    .collect(Collectors.toList());

            detail.put("appointments", scheduleAppointments);
            int max = schedule.getMaxAppointments() != null ? schedule.getMaxAppointments() : 0;
            int used = scheduleAppointments.size();
            detail.put("maxAppointments", max);
            detail.put("usedAppointments", used);
            detail.put("availableAppointments", max - used);

            scheduleDetails.add(detail);
        }

        // 加载所有患者信息
        java.util.Map<Long, Patient> patientMap = new java.util.HashMap<>();
        java.util.Map<Long, User> userMap = new java.util.HashMap<>();
        for (Appointment apt : allAppointments) {
            patientService.findById(apt.getPatientId()).ifPresent(patient -> {
                patientMap.put(apt.getPatientId(), patient);
                userService.findById(patient.getUserId()).ifPresent(user -> {
                    userMap.put(apt.getPatientId(), user);
                });
            });
        }

        String workTimeName = workTimeEnum == Schedule.WorkTime.MORNING ? "上午" :
                (workTimeEnum == Schedule.WorkTime.AFTERNOON ? "中午" : "晚上");

        model.addAttribute("workTime", workTimeEnum);
        model.addAttribute("workTimeName", workTimeName);
        model.addAttribute("scheduleDetails", scheduleDetails);
        model.addAttribute("patientMap", patientMap);
        model.addAttribute("userMap", userMap);
        return "doctor/schedule-detail";
    }

    // 病患诊断 - 今日待诊患者列表
    @GetMapping("/diagnose")
    public String diagnoseList(Model model) {
        Long doctorId = getCurrentDoctorId();
        if (doctorId == null) {
            return "redirect:/login/user";
        }

        LocalDate today = LocalDate.now();
        List<Appointment> todayAppointments = appointmentService.findByDoctorId(doctorId).stream()
                .filter(apt -> apt.getAppointmentDate().equals(today)
                        && (apt.getStatus() == Appointment.AppointmentStatus.PENDING
                        || apt.getStatus() == Appointment.AppointmentStatus.CONFIRMED))
                .sorted((a, b) -> {
                    if (a.getAppointmentTime() != null && b.getAppointmentTime() != null) {
                        return a.getAppointmentTime().compareTo(b.getAppointmentTime());
                    }
                    return 0;
                })
                .collect(Collectors.toList());

        // 加载患者信息
        java.util.Map<Long, Patient> patientMap = new java.util.HashMap<>();
        java.util.Map<Long, User> userMap = new java.util.HashMap<>();
        for (Appointment apt : todayAppointments) {
            patientService.findById(apt.getPatientId()).ifPresent(patient -> {
                patientMap.put(apt.getPatientId(), patient);
                userService.findById(patient.getUserId()).ifPresent(user -> {
                    userMap.put(apt.getPatientId(), user);
                });
            });
        }

        model.addAttribute("appointments", todayAppointments);
        model.addAttribute("patientMap", patientMap);
        model.addAttribute("userMap", userMap);
        return "doctor/diagnose-list";
    }

    // 病患诊断 - 诊断表单
    @GetMapping("/diagnose/{id}")
    public String diagnoseForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Appointment appointment = appointmentService.findById(id).orElse(null);
        if (appointment == null) {
            return "redirect:/doctor/diagnose";
        }

        Long doctorId = getCurrentDoctorId();
        if (doctorId == null) {
            return "redirect:/login/user";
        }
        if (appointment.getDoctorId() == null || !appointment.getDoctorId().equals(doctorId)) {
            redirectAttributes.addFlashAttribute("error", "无权诊断该挂号。");
            return "redirect:/doctor/diagnose";
        }
        if (appointment.getStatus() == Appointment.AppointmentStatus.CANCELLED) {
            redirectAttributes.addFlashAttribute("error", "该挂号已取消，不能进行诊断。");
            return "redirect:/doctor/diagnose";
        }

        // 允许对已完成接诊进行“重新诊断”（从历史挂号入口进入）
        // 仍然允许从待诊列表进入正常诊断流程

        // 加载患者信息
        Patient patient = patientService.findById(appointment.getPatientId()).orElse(null);
        User patientUser = patient != null ? userService.findById(patient.getUserId()).orElse(null) : null;

        // 加载药品库和检查项目：仅显示医生所属科室或全院通用的项目
        Doctor doctor = doctorId != null ? doctorService.findById(doctorId).orElse(null) : null;
        Long deptId = null;
        if (doctor != null && doctor.getDepartment() != null) {
            deptId = departmentService.findAll().stream()
                    .filter(d -> d.getName().equals(doctor.getDepartment()))
                    .map(Department::getId)
                    .findFirst()
                    .orElse(null);
        }

        List<Medicine> medicines = medicineService.findByDepartmentOrCommon(deptId);
        List<Examination> examinations = examinationService.findByDepartmentOrCommon(deptId);

        // 加载已有电子病历（用于重新诊断时回显；取最新一条作为文本回显来源）
        List<MedicalRecord> mrForAppointment =
                appointment.getId() != null ? medicalRecordService.findAllByAppointmentId(appointment.getId()) : List.of();
        MedicalRecord existingRecord = mrForAppointment.isEmpty() ? null : mrForAppointment.get(0);

        // 已选检查 ID：合并本挂号下各条病历中的 examination_items，回显复选框（避免未勾选提交导致清空检查、误删报告）
        Set<Long> selectedExamIds = new HashSet<>();
        for (MedicalRecord mr : mrForAppointment) {
            if (mr.getExaminationItems() != null && !mr.getExaminationItems().isBlank()) {
                for (String s : mr.getExaminationItems().split(",")) {
                    if (s != null && !s.isBlank()) {
                        try {
                            selectedExamIds.add(Long.parseLong(s.trim()));
                        } catch (NumberFormatException ignored) {
                            // skip
                        }
                    }
                }
            }
        }
        // 本挂号已开药品累计数量（回显勾选与数量，与提交诊断时增量开药逻辑一致）
        Map<Long, Integer> appointmentMedicineQty =
                appointment.getId() != null ? computePreviousMedicineQuantities(appointment.getId()) : new HashMap<>();

        model.addAttribute("appointment", appointment);
        model.addAttribute("patient", patient);
        model.addAttribute("patientUser", patientUser);
        model.addAttribute("medicines", medicines);
        model.addAttribute("examinations", examinations);
        model.addAttribute("existingRecord", existingRecord);
        model.addAttribute("selectedExamIds", selectedExamIds);
        model.addAttribute("appointmentMedicineQty", appointmentMedicineQty);
        model.addAttribute("currentDoctorDepartment", doctor != null ? doctor.getDepartment() : null);
        return "doctor/diagnose-form";
    }

    // 提交诊断
    @PostMapping("/diagnose/{id}")
    @org.springframework.transaction.annotation.Transactional
    public String submitDiagnosis(@PathVariable Long id,
                                  @RequestParam String diagnosis,
                                  @RequestParam(required = false) String prescription,
                                  @RequestParam(required = false) String[] examinationItems,
                                  @RequestParam(required = false) String[] medicines,
                                  HttpServletRequest request,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        diagnosisValidator.validateAndLock(id, medicines, examinationItems, request, getCurrentDoctorId());
        Appointment appointment = appointmentService.findById(id).orElse(null);
        if (appointment == null) {
            return "redirect:/doctor/diagnose";
        }
        Long doctorId = getCurrentDoctorId();
        if (doctorId == null) {
            return "redirect:/login/user";
        }
        if (appointment.getDoctorId() == null || !appointment.getDoctorId().equals(doctorId)) {
            redirectAttributes.addFlashAttribute("error", "无权诊断该挂号。");
            return "redirect:/doctor/diagnose";
        }
        if (appointment.getStatus() == Appointment.AppointmentStatus.CANCELLED) {
            redirectAttributes.addFlashAttribute("error", "该挂号已取消，不能进行诊断。");
            return "redirect:/doctor/diagnose";
        }

        // 重新诊断前：已有检查项（用于只给「新增」检查生成报告单）、已有处方累计数量（用于「增量」开药与患者端缴费）
        MedicalRecord existingRecordBefore = medicalRecordService.findByAppointmentId(id).orElse(null);
        // 合并本挂号下所有病历行中的 examination_items（防止历史库中重复行导致旧集合不全）
        Set<String> oldExamIdSet = new HashSet<>();
        for (MedicalRecord mr : medicalRecordService.findAllByAppointmentId(id)) {
            if (mr.getExaminationItems() != null && !mr.getExaminationItems().isBlank()) {
                for (String s : mr.getExaminationItems().split(",")) {
                    if (s != null && !s.isBlank()) {
                        oldExamIdSet.add(s.trim());
                    }
                }
            }
        }
        Map<Long, Integer> prevQtyByMedicine = computePreviousMedicineQuantities(id);

        // 构建处方内容（包含选择的药品）
        StringBuilder prescriptionBuilder = new StringBuilder();
        if (prescription != null && !prescription.isEmpty()) {
            prescriptionBuilder.append(prescription);
        }
        if (medicines != null && medicines.length > 0) {
            if (prescriptionBuilder.length() > 0) {
                prescriptionBuilder.append("\n\n选择的药品：\n");
            } else {
                prescriptionBuilder.append("选择的药品：\n");
            }
            for (String medicineId : medicines) {
                try {
                    Long medId = Long.parseLong(medicineId);
                    int qty = parseMedicineQuantity(request, medicineId);
                    medicineService.findById(medId).ifPresent(medicine -> {
                        prescriptionBuilder.append("- ").append(medicine.getName())
                                .append(" (").append(medicine.getSpecification()).append(") × ").append(qty).append("\n");
                    });
                } catch (NumberFormatException e) {
                    // 忽略无效的ID
                }
            }
        }

        // 更新挂号信息
        appointment.setDiagnosis(diagnosis);
        appointment.setPrescription(prescriptionBuilder.toString());
        appointment.setStatus(Appointment.AppointmentStatus.COMPLETED);
        appointmentService.update(appointment);

        // 增量开药：相对本挂号已有处方明细，仅对「新增数量」生成新处方（重新诊断加药时患者端需再次付费）
        Map<Long, Integer> medicineDeltas = new LinkedHashMap<>();
        if (medicines != null && medicines.length > 0) {
            for (String medicineId : medicines) {
                try {
                    Long medId = Long.parseLong(medicineId);
                    int qty = parseMedicineQuantity(request, medicineId);
                    int prev = prevQtyByMedicine.getOrDefault(medId, 0);
                    int delta = qty - prev;
                    if (delta > 0) {
                        medicineDeltas.put(medId, delta);
                    }
                } catch (NumberFormatException e) {
                    // 忽略无效的ID
                }
            }
        }
        if (!medicineDeltas.isEmpty()) {
            try {
                String prescriptionNumber = "RX" + System.currentTimeMillis();

                Prescription prescriptionEntity = new Prescription();
                prescriptionEntity.setAppointmentId(id);
                prescriptionEntity.setPatientId(appointment.getPatientId());
                prescriptionEntity.setDoctorId(doctorId);
                prescriptionEntity.setPrescriptionNumber(prescriptionNumber);
                prescriptionEntity.setStatus(Prescription.PrescriptionStatus.PENDING);
                prescriptionEntity.setCreatedAt(LocalDateTime.now());

                Prescription savedPrescription = prescriptionService.save(prescriptionEntity);

                for (Map.Entry<Long, Integer> entry : medicineDeltas.entrySet()) {
                    Long medId = entry.getKey();
                    final int quantity = entry.getValue();
                    medicineService.findById(medId).ifPresent(medicine -> {
                        PrescriptionItem item = new PrescriptionItem();
                        item.setPrescriptionId(savedPrescription.getId());
                        item.setMedicineId(medId);
                        item.setMedicineName(medicine.getName());
                        item.setSpecification(medicine.getSpecification());
                        item.setQuantity(quantity);
                        item.setUnit(medicine.getUnit() != null ? medicine.getUnit() : "盒");
                        item.setDosage("按说明书");
                        item.setFrequency("每日3次");
                        item.setUsage("口服");
                        item.setPrice(medicine.getPrice() != null ? medicine.getPrice() : java.math.BigDecimal.ZERO);
                        item.setTotalPrice(item.getPrice().multiply(new java.math.BigDecimal(item.getQuantity())));
                        item.setNotes("");
                        prescriptionService.savePrescriptionItem(item);
                    });
                }
            } catch (Exception e) {
                throw new IllegalStateException("处方保存失败，诊断未提交", e);
            }
        }

        // 创建/更新电子病历（重新诊断时优先更新，避免重复记录）
        MedicalRecord medicalRecord = medicalRecordService.findByAppointmentId(id).orElse(null);
        if (medicalRecord == null) {
            medicalRecord = new MedicalRecord();
            medicalRecord.setAppointmentId(id);
            medicalRecord.setPatientId(appointment.getPatientId());
            medicalRecord.setDoctorId(doctorId);
            medicalRecord.setCreatedAt(LocalDateTime.now());
        }
        medicalRecord.setDiagnosis(diagnosis);
        medicalRecord.setPrescription(prescriptionBuilder.toString());
        if (examinationItems != null && examinationItems.length > 0) {
            medicalRecord.setExaminationItems(String.join(",", examinationItems));
        } else {
            medicalRecord.setExaminationItems(null);
        }
        medicalRecord.setMedicalRecordContent("诊断：" + diagnosis + "\n处方：" + prescriptionBuilder.toString());
        medicalRecord.setUpdatedAt(LocalDateTime.now());
        medicalRecordService.save(medicalRecord);

        // 检查/检验：
        // 1）oldExamIdSet = 更新前病历 examinationItems，用于判断「相对上次诊断是否新增」；
        // 2）本次勾选中去掉的项目：删除「待出 + 患者仍需缴费」的报告（deleteReportOnly，不删电子病历）；
        // 3）仅为「不在 oldExamIdSet 且本挂号尚无同检查报告」的项目新建 Report，避免重复建档。
        Set<String> newExamIdSet = new HashSet<>();
        if (examinationItems != null && examinationItems.length > 0) {
            for (String ex : examinationItems) {
                if (ex != null && !ex.isBlank()) {
                    newExamIdSet.add(ex.trim());
                }
            }
        }
        for (Report r : reportService.findByAppointmentId(id)) {
            if (r.getStatus() != Report.ReportStatus.PENDING || r.getExaminationId() == null) {
                continue;
            }
            String eid = String.valueOf(r.getExaminationId());
            if (!newExamIdSet.contains(eid) && reportService.needsPatientPayment(r)) {
                reportService.deleteReportOnly(r.getId());
            }
        }
        Set<Long> examIdsWithReport = reportService.findByAppointmentId(id).stream()
                .map(Report::getExaminationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!newExamIdSet.isEmpty()) {
            for (String examIdStr : newExamIdSet) {
                if (oldExamIdSet.contains(examIdStr)) {
                    continue;
                }
                try {
                    Long examIdLong = Long.parseLong(examIdStr);
                    if (examIdsWithReport.contains(examIdLong)) {
                        continue;
                    }
                    Report report = new Report();
                    report.setAppointmentId(id);
                    report.setPatientId(appointment.getPatientId());
                    report.setDoctorId(doctorId);
                    report.setExaminationId(examIdLong);
                    examinationService.findById(examIdLong).ifPresent(exam -> {
                        report.setReportType(exam.getName());
                        BigDecimal price = exam.getPrice();
                        if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                            report.setPaid(false);
                        } else {
                            report.setPaid(true);
                            report.setPaidAt(LocalDateTime.now());
                        }
                    });
                    if (report.getReportType() == null) {
                        report.setReportType("检查/检验");
                    }
                    if (report.getPaid() == null) {
                        report.setPaid(false);
                    }
                    report.setStatus(Report.ReportStatus.PENDING);
                    report.setReportDate(LocalDateTime.now());
                    reportService.save(report);
                    examIdsWithReport.add(examIdLong);
                } catch (NumberFormatException e) {
                    // 忽略无效的ID
                }
            }
        }

        boolean hasNewMedicineOrExam = !medicineDeltas.isEmpty()
                || newExamIdSet.stream().anyMatch(eid -> !oldExamIdSet.contains(eid));
        if (hasNewMedicineOrExam) {
            redirectAttributes.addFlashAttribute("success",
                    (existingRecordBefore != null ? "诊断已更新。" : "诊断已提交。")
                            + " 若有新增药品或检查/检验，请提醒患者在「买药取药」完成缴费。");
        } else {
            redirectAttributes.addFlashAttribute("success",
                    existingRecordBefore != null
                            ? "诊断已更新。"
                            : "诊断已提交，如需住院可点击「发起住院申请」。");
        }
        return "redirect:/doctor/diagnose/" + id;
    }

    // 历史挂号列表
    @GetMapping("/appointments")
    public String appointments(Model model) {
        Long doctorId = getCurrentDoctorId();
        if (doctorId == null) {
            return "redirect:/login/user";
        }

        List<Appointment> appointments = appointmentService.findByDoctorId(doctorId);

        // 加载患者和病历信息
        java.util.Map<Long, Patient> patientMap = new java.util.HashMap<>();
        java.util.Map<Long, User> userMap = new java.util.HashMap<>();
        java.util.Map<Long, MedicalRecord> recordMap = new java.util.HashMap<>();
        for (Appointment apt : appointments) {
            patientService.findById(apt.getPatientId()).ifPresent(patient -> {
                patientMap.put(apt.getPatientId(), patient);
                userService.findById(patient.getUserId()).ifPresent(user -> {
                    userMap.put(apt.getPatientId(), user);
                });
            });
            medicalRecordService.findByAppointmentId(apt.getId()).ifPresent(record -> {
                recordMap.put(apt.getId(), record);
            });
        }

        model.addAttribute("appointments", appointments);
        model.addAttribute("patientMap", patientMap);
        model.addAttribute("userMap", userMap);
        model.addAttribute("recordMap", recordMap);
        return "doctor/appointments";
    }

    // 查看历史病历
    @GetMapping("/appointments/{id}/record")
    public String viewRecord(@PathVariable Long id, Model model) {
        medicalRecordService.findByAppointmentId(id).ifPresent(record -> {
            model.addAttribute("record", record);
            appointmentService.findById(id).ifPresent(appointment -> {
                model.addAttribute("appointment", appointment);
            });
        });
        return "doctor/medical-record";
    }

    // 报告单管理
    @GetMapping("/reports")
    public String reports(@RequestParam(required = false) Long unpaidReport,
                          Model model) {
        Long doctorId = getCurrentDoctorId();
        if (doctorId == null) {
            return "redirect:/login/user";
        }

        if (unpaidReport != null) {
            model.addAttribute("error", "患者尚未缴纳检查/检验费用，待患者在「买药取药」中完成缴费后方可审阅。");
        }

        List<Report> reports = reportService.findByDoctorId(doctorId);

        // 按时间排序（最新的在前）
        reports = reports.stream()
                .sorted((r1, r2) -> {
                    if (r1.getReportDate() == null && r2.getReportDate() == null) return 0;
                    if (r1.getReportDate() == null) return 1;
                    if (r2.getReportDate() == null) return -1;
                    return r2.getReportDate().compareTo(r1.getReportDate());
                })
                .collect(Collectors.toList());

        // 加载患者、检查项目信息
        java.util.Map<Long, Patient> patientMap = new java.util.HashMap<>();
        java.util.Map<Long, User> userMap = new java.util.HashMap<>();
        java.util.Map<Long, Examination> examinationMap = new java.util.HashMap<>();
        for (Report report : reports) {
            patientService.findById(report.getPatientId()).ifPresent(patient -> {
                patientMap.put(report.getPatientId(), patient);
                userService.findById(patient.getUserId()).ifPresent(user -> {
                    userMap.put(report.getPatientId(), user);
                });
            });
            if (report.getExaminationId() != null) {
                examinationService.findById(report.getExaminationId()).ifPresent(exam -> {
                    examinationMap.put(report.getId(), exam);
                });
            }
        }

        Map<Long, Boolean> reportNeedsPayment = new HashMap<>();
        for (Report r : reports) {
            if (r.getId() != null) {
                reportNeedsPayment.put(r.getId(), reportService.needsPatientPayment(r));
            }
        }
        long pendingReviewableCount = reports.stream()
                .filter(r -> r.getStatus() == Report.ReportStatus.PENDING)
                .filter(r -> reportService.canDoctorReviewReport(r))
                .count();

        model.addAttribute("reports", reports);
        model.addAttribute("patientMap", patientMap);
        model.addAttribute("userMap", userMap);
        model.addAttribute("examinationMap", examinationMap);
        model.addAttribute("reportNeedsPayment", reportNeedsPayment);
        model.addAttribute("pendingReviewableCount", pendingReviewableCount);
        return "doctor/reports";
    }

    // 填写/编辑报告单
    @GetMapping("/reports/{id}/edit")
    public String editReportForm(@PathVariable Long id, Model model) {
        Long doctorId = getCurrentDoctorId();
        if (doctorId == null) {
            return "redirect:/login/user";
        }

        Report report = reportService.findById(id).orElse(null);
        if (report == null || !report.getDoctorId().equals(doctorId)) {
            return "redirect:/doctor/reports";
        }

        // 检查费用未缴清前不可审阅（与患者端「买药取药」缴费联动）
        if (reportService.needsPatientPayment(report)) {
            return "redirect:/doctor/reports?unpaidReport=" + id;
        }

        // 加载检查项目信息
        Examination examination = null;
        if (report.getExaminationId() != null) {
            examination = examinationService.findById(report.getExaminationId()).orElse(null);
        }

        // 加载患者信息
        Patient patient = patientService.findById(report.getPatientId()).orElse(null);
        User patientUser = null;
        if (patient != null) {
            patientUser = userService.findById(patient.getUserId()).orElse(null);
        }

        model.addAttribute("report", report);
        model.addAttribute("examination", examination);
        model.addAttribute("patient", patient);
        model.addAttribute("patientUser", patientUser);
        return "doctor/report-form";
    }

    // 保存报告单
    @PostMapping("/reports/{id}")
    public String saveReport(@PathVariable Long id,
                             @RequestParam String reportContent,
                             Model model) {
        Long doctorId = getCurrentDoctorId();
        if (doctorId == null) {
            return "redirect:/login/user";
        }

        Report report = reportService.findById(id).orElse(null);
        if (report == null || !report.getDoctorId().equals(doctorId)) {
            return "redirect:/doctor/reports";
        }

        if (reportService.needsPatientPayment(report)) {
            return "redirect:/doctor/reports?unpaidReport=" + id;
        }

        report.setReportContent(reportContent);
        report.setStatus(Report.ReportStatus.COMPLETED);
        report.setReportDate(LocalDateTime.now());
        reportService.update(report);

        return "redirect:/doctor/reports?success=true";
    }

    // 报告单预览（医生端）
    @GetMapping("/reports/{id}/preview")
    public String previewReportForDoctor(@PathVariable Long id, Model model) {
        Long doctorId = getCurrentDoctorId();
        if (doctorId == null) {
            return "redirect:/login/user";
        }

        Report report = reportService.findById(id).orElse(null);
        if (report == null || !report.getDoctorId().equals(doctorId)) {
            return "redirect:/doctor/reports";
        }

        Examination examination = null;
        if (report.getExaminationId() != null) {
            examination = examinationService.findById(report.getExaminationId()).orElse(null);
        }

        Patient patient = patientService.findById(report.getPatientId()).orElse(null);
        User patientUser = null;
        if (patient != null) {
            patientUser = userService.findById(patient.getUserId()).orElse(null);
        }

        Doctor doctor = doctorService.findById(report.getDoctorId()).orElse(null);
        User doctorUser = null;
        if (doctor != null) {
            doctorUser = userService.findById(doctor.getUserId()).orElse(null);
        }

        model.addAttribute("report", report);
        model.addAttribute("examination", examination);
        model.addAttribute("patient", patient);
        model.addAttribute("patientUser", patientUser);
        model.addAttribute("doctor", doctor);
        model.addAttribute("doctorUser", doctorUser);
        return "doctor/report-preview";
    }

    // 住院申请管理
    @GetMapping("/hospitalizations")
    public String hospitalizations(Model model) {
        Long doctorId = getCurrentDoctorId();
        if (doctorId == null) {
            return "redirect:/login/user";
        }

        List<Hospitalization> hospitalizations = hospitalizationService.findByDoctorId(doctorId);

        // 出院申请队列（医嘱标记）
        java.util.Set<Long> dischargeRequestHospIds = new java.util.HashSet<>();
        List<MedicalOrder> myOrders = medicalOrderService.findByDoctorId(doctorId);
        for (MedicalOrder o : myOrders) {
            if (o.getHospitalizationId() != null
                    && o.getOrderType() == MedicalOrder.OrderType.OTHER
                    && o.getStatus() == MedicalOrder.OrderStatus.ACTIVE
                    && o.getOrderContent() != null
                    && o.getOrderContent().contains("申请出院")) {
                dischargeRequestHospIds.add(o.getHospitalizationId());
            }
        }
        List<Hospitalization> dischargeRequestHospitalizations = hospitalizations.stream()
                .filter(h -> h.getId() != null
                        && dischargeRequestHospIds.contains(h.getId())
                        && h.getStatus() == Hospitalization.HospitalizationStatus.ADMITTED)
                .collect(Collectors.toList());

        // 加载患者信息
        java.util.Map<Long, Patient> patientMap = new java.util.HashMap<>();
        java.util.Map<Long, User> userMap = new java.util.HashMap<>();
        java.util.Map<Long, Bed> bedMap = new java.util.HashMap<>();
        for (Hospitalization hosp : hospitalizations) {
            patientService.findById(hosp.getPatientId()).ifPresent(patient -> {
                patientMap.put(hosp.getPatientId(), patient);
                userService.findById(patient.getUserId()).ifPresent(user -> {
                    userMap.put(hosp.getPatientId(), user);
                });
            });
            if (hosp.getBedId() != null) {
                bedService.findById(hosp.getBedId()).ifPresent(bed -> {
                    bedMap.put(hosp.getBedId(), bed);
                });
            }
        }

        model.addAttribute("hospitalizations", hospitalizations);
        model.addAttribute("dischargeRequestHospitalizations", dischargeRequestHospitalizations);
        model.addAttribute("dischargeRequestHospIds", dischargeRequestHospIds);
        model.addAttribute("patientMap", patientMap);
        model.addAttribute("userMap", userMap);
        model.addAttribute("bedMap", bedMap);
        return "doctor/hospitalizations";
    }

    // 分配床位
    @GetMapping("/hospitalizations/{id}/assign-bed")
    public String assignBedForm(@PathVariable Long id, Model model) {
        Long doctorId = getCurrentDoctorId();
        if (doctorId == null) {
            return "redirect:/login/user";
        }

        Hospitalization hospitalization = hospitalizationService.findById(id).orElse(null);
        if (hospitalization == null) {
            return "redirect:/doctor/hospitalizations";
        }

        // 加载患者信息
        Patient patient = patientService.findById(hospitalization.getPatientId()).orElse(null);
        User patientUser = patient != null ? userService.findById(patient.getUserId()).orElse(null) : null;

        // 获取当前医生的科室信息
        Doctor currentDoctor = doctorService.findById(doctorId).orElse(null);
        final Long doctorDepartmentId;
        if (currentDoctor != null && currentDoctor.getDepartment() != null) {
            // 根据医生科室名称查找对应的科室ID
            doctorDepartmentId = departmentService.findAll().stream()
                    .filter(dept -> dept.getName().equals(currentDoctor.getDepartment()))
                    .map(Department::getId)
                    .findFirst()
                    .orElse(null);
        } else {
            doctorDepartmentId = null;
        }

        // 获取所有可用床位，但只显示对应科室的床位（如果医生有科室分配）
        List<Bed> allAvailableBeds = bedService.findAvailableBeds();
        final Long finalDoctorDepartmentId = doctorDepartmentId;
        List<Bed> availableBeds;
        if (finalDoctorDepartmentId != null) {
            // 只显示对应科室的床位
            availableBeds = allAvailableBeds.stream()
                    .filter(bed -> bed.getDepartmentId() != null && bed.getDepartmentId().equals(finalDoctorDepartmentId))
                    .collect(Collectors.toList());
        } else {
            // 如果医生没有科室分配，显示所有可用床位
            availableBeds = allAvailableBeds;
        }

        // 按科室分组（而不是按病区）
        java.util.Map<String, List<Bed>> bedsByDepartment = new java.util.HashMap<>();
        java.util.Map<Long, Department> departmentMap = new java.util.HashMap<>();

        // 加载所有科室信息
        departmentService.findAll().forEach(dept -> {
            departmentMap.put(dept.getId(), dept);
        });

        // 按科室分组床位
        for (Bed bed : availableBeds) {
            String deptKey;
            if (bed.getDepartmentId() != null && departmentMap.containsKey(bed.getDepartmentId())) {
                deptKey = departmentMap.get(bed.getDepartmentId()).getName();
            } else {
                deptKey = "未分配科室";
            }
            bedsByDepartment.computeIfAbsent(deptKey, k -> new java.util.ArrayList<>()).add(bed);
        }

        model.addAttribute("hospitalization", hospitalization);
        model.addAttribute("patient", patient);
        model.addAttribute("patientUser", patientUser);
        model.addAttribute("availableBeds", availableBeds);
        model.addAttribute("bedsByDepartment", bedsByDepartment);
        model.addAttribute("departmentMap", departmentMap);
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("currentDoctor", currentDoctor);
        model.addAttribute("doctorDepartmentId", doctorDepartmentId);
        return "doctor/assign-bed";
    }

    @PostMapping("/hospitalizations/{id}/assign-bed")
    public String assignBed(@PathVariable Long id, @RequestParam Long bedId, RedirectAttributes redirectAttributes) {
        Long doctorId = getCurrentDoctorId();
        if (doctorId == null) {
            return "redirect:/login/user";
        }

        Hospitalization hospitalization = hospitalizationService.findById(id).orElse(null);
        if (hospitalization == null) {
            redirectAttributes.addFlashAttribute("error", "住院申请不存在");
            return "redirect:/doctor/hospitalizations";
        }

        Bed bed = bedService.findById(bedId).orElse(null);
        if (bed == null || bed.getStatus() != Bed.BedStatus.AVAILABLE) {
            redirectAttributes.addFlashAttribute("error", "床位不可用");
            return "redirect:/doctor/hospitalizations/" + id + "/assign-bed";
        }

        // 验证医生是否有权限选择该床位（必须是同一科室）
        Doctor currentDoctor = doctorService.findById(doctorId).orElse(null);
        if (currentDoctor != null && currentDoctor.getDepartment() != null && bed.getDepartmentId() != null) {
            // 检查床位科室是否与医生科室匹配
            boolean departmentMatch = departmentService.findById(bed.getDepartmentId())
                    .map(dept -> dept.getName().equals(currentDoctor.getDepartment()))
                    .orElse(false);

            if (!departmentMatch) {
                redirectAttributes.addFlashAttribute("error", "您只能选择本科室的床位");
                return "redirect:/doctor/hospitalizations/" + id + "/assign-bed";
            }
        }

        hospitalizationService.assignBed(id, bedId);
        redirectAttributes.addFlashAttribute("success", "床位已分配，住院登记已生效");
        return "redirect:/doctor/hospitalizations";
    }

    // 住院申请表单（须先完成本次接诊诊断，见 submitDiagnosis 将挂号置为 COMPLETED）
    @GetMapping("/hospitalizations/request/{appointmentId}")
    public String hospitalizationRequestForm(@PathVariable Long appointmentId, Model model,
                                             RedirectAttributes redirectAttributes) {
        Long doctorId = getCurrentDoctorId();
        if (doctorId == null) {
            return "redirect:/login/user";
        }

        Appointment appointment = appointmentService.findById(appointmentId).orElse(null);
        if (appointment == null) {
            return "redirect:/doctor/diagnose";
        }
        if (appointment.getDoctorId() == null || !appointment.getDoctorId().equals(doctorId)) {
            redirectAttributes.addFlashAttribute("error", "无权为该挂号申请住院。");
            return "redirect:/doctor/diagnose";
        }
        if (appointment.getStatus() != Appointment.AppointmentStatus.COMPLETED) {
            redirectAttributes.addFlashAttribute("error", "请先在本页提交诊断后，再申请住院。");
            return "redirect:/doctor/diagnose/" + appointmentId;
        }

        // 加载患者信息
        Patient patient = patientService.findById(appointment.getPatientId()).orElse(null);
        User patientUser = patient != null ? userService.findById(patient.getUserId()).orElse(null) : null;

        // 获取当前医生信息
        Doctor doctor = doctorService.findById(doctorId).orElse(null);
        final Long[] requestDepartmentId = {null};
        if (doctor != null && doctor.getDepartment() != null) {
            // 根据科室名称查找科室ID
            departmentService.findAll().stream()
                    .filter(dept -> dept.getName().equals(doctor.getDepartment()))
                    .findFirst()
                    .ifPresent(dept -> requestDepartmentId[0] = dept.getId());
        }

        // 常用住院原因
        List<String> commonReasons = List.of(
                "病情需要住院观察",
                "需要手术治疗",
                "需要进一步检查",
                "病情严重需要监护",
                "需要康复治疗",
                "其他"
        );

        model.addAttribute("appointment", appointment);
        model.addAttribute("patient", patient);
        model.addAttribute("patientUser", patientUser);
        model.addAttribute("doctor", doctor);
        model.addAttribute("requestDepartmentId", requestDepartmentId[0]);
        model.addAttribute("commonReasons", commonReasons);
        model.addAttribute("departments", departmentService.findAll());
        return "doctor/hospitalization-request";
    }

    // 提交住院申请
    @PostMapping("/hospitalizations/request")
    @org.springframework.transaction.annotation.Transactional
    public String submitHospitalizationRequest(@RequestParam Long appointmentId,
                                                @RequestParam String admissionReason,
                                                @RequestParam Integer expectedDays,
                                                @RequestParam Long requestDepartmentId,
                                                RedirectAttributes redirectAttributes) {
        Long doctorId = getCurrentDoctorId();
        if (doctorId == null) {
            return "redirect:/login/user";
        }

        Appointment appointment = appointmentService.findById(appointmentId).orElse(null);
        if (appointment == null) {
            return "redirect:/doctor/diagnose";
        }
        if (appointment.getDoctorId() == null || !appointment.getDoctorId().equals(doctorId)) {
            redirectAttributes.addFlashAttribute("error", "无权为该挂号申请住院。");
            return "redirect:/doctor/diagnose";
        }
        if (appointment.getStatus() != Appointment.AppointmentStatus.COMPLETED) {
            redirectAttributes.addFlashAttribute("error", "请先提交诊断后，再申请住院。");
            return "redirect:/doctor/diagnose/" + appointmentId;
        }

        // 创建住院申请（初始状态：未分配床位，待医生分配床位后才能进入待审核阶段）
        Hospitalization hospitalization = new Hospitalization();
        hospitalization.setPatientId(appointment.getPatientId());
        hospitalization.setDoctorId(doctorId);
        hospitalization.setAppointmentId(appointmentId);
        hospitalization.setAdmissionDate(LocalDate.now());
        // 与住院管理页展示一致：优先电子病历，避免挂号表仍为占位/旧值
        String diagnosisForHosp = resolveDiagnosisForAppointmentId(appointmentId);
        hospitalization.setDiagnosis(diagnosisForHosp);
        hospitalization.setAdmissionReason(admissionReason);
        hospitalization.setExpectedDays(expectedDays);
        hospitalization.setRequestDepartmentId(requestDepartmentId);
        hospitalization.setRequestTime(LocalDateTime.now());
        // 初始状态：requestStatus 为 null，表示未分配床位；分配床位后才会设置为 PENDING
        hospitalization.setRequestStatus(null);
        // 初始状态：status 设置为 ADMITTED（数据库默认值），但实际流程由 requestStatus 控制
        // 只有在管理员审核通过后，才真正生效
        hospitalization.setStatus(Hospitalization.HospitalizationStatus.ADMITTED);
        hospitalization.setTotalCost(java.math.BigDecimal.ZERO);
        hospitalization.setCreatedAt(LocalDateTime.now());
        hospitalization.setUpdatedAt(LocalDateTime.now());

        hospitalizationService.save(hospitalization);

        // 接诊已在提交诊断时完成，此处仅刷新更新时间
        appointment.setUpdatedAt(LocalDateTime.now());
        appointmentService.update(appointment);

        redirectAttributes.addFlashAttribute("success", "住院申请已提交");
        return "redirect:/doctor/hospitalizations";
    }

    // 医生个人信息
    @GetMapping("/profile")
    public String profile(Model model) {
        // 今日排班状态
        Long doctorId = getCurrentDoctorId();
        if (doctorId != null) {
            LocalDate today = LocalDate.now();
            boolean onDutyToday = scheduleService.findByDoctorIdAndDate(doctorId, today).stream()
                    .anyMatch(s -> s.getStatus() != null && s.getStatus() == 1);
            model.addAttribute("onDutyToday", onDutyToday);
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            userService.findByUsername(username).ifPresent(user -> {
                model.addAttribute("user", user);
                doctorService.findByUserId(user.getId()).ifPresent(doctor -> {
                    model.addAttribute("doctor", doctor);
                });
            });
        }
        return "doctor/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam(required = false) String title,
                               @RequestParam(required = false) String introduction,
                               @RequestParam(required = false) String department) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            String username = auth.getName();
            userService.findByUsername(username).ifPresent(user -> {
                doctorService.findByUserId(user.getId()).ifPresent(doctor -> {
                    if (title != null) doctor.setTitle(title);
                    if (introduction != null) doctor.setIntroduction(introduction);
                    if (department != null) doctor.setDepartment(department);
                    doctorService.update(doctor);
                });
            });
        }
        return "redirect:/doctor/profile?success=true";
    }

    // 我的排班列表
    @GetMapping("/schedules")
    public String mySchedules(Model model) {
        Long doctorId = getCurrentDoctorId();
        if (doctorId == null) {
            return "redirect:/login/user";
        }
        List<Schedule> schedules = scheduleService.findByDoctorId(doctorId);
        // 计算每个排班已预约人数（基于 appointment.scheduleId，排除已取消）
        List<Appointment> doctorAppointments = appointmentService.findByDoctorId(doctorId);
        for (Schedule s : schedules) {
            if (s.getId() != null) {
                long count = doctorAppointments.stream()
                        .filter(a -> a.getScheduleId() != null
                                && a.getScheduleId().equals(s.getId())
                                && a.getStatus() != Appointment.AppointmentStatus.CANCELLED)
                        .count();
                s.setCurrentAppointments((int) count);
            }
        }
        // 按日期、班次排序，方便查看
        schedules.sort((a, b) -> {
            if (a.getWorkDate() != null && b.getWorkDate() != null) {
                int cmp = a.getWorkDate().compareTo(b.getWorkDate());
                if (cmp != 0) return cmp;
            }
            if (a.getWorkTime() != null && b.getWorkTime() != null) {
                return a.getWorkTime().compareTo(b.getWorkTime());
            }
            return 0;
        });
        model.addAttribute("schedules", schedules);
        return "doctor/schedules";
    }

    // 申请请假：将排班状态改为“待审批”(2)
    @PostMapping("/schedules/{id}/leave")
    public String requestScheduleLeave(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Long doctorId = getCurrentDoctorId();
        if (doctorId == null) {
            return "redirect:/login/user";
        }
        scheduleService.findById(id).ifPresent(s -> {
            if (s.getDoctorId() != null && s.getDoctorId().equals(doctorId)) {
                if (s.getDisplayStatus() == Schedule.DisplayStatus.ENDED) {
                    redirectAttributes.addFlashAttribute("error", "该班次已结束，不能申请请假。");
                } else if (s.getDisplayStatus() != Schedule.DisplayStatus.ACTIVE) {
                    redirectAttributes.addFlashAttribute("error", "仅未结束的启用班次可以申请请假。");
                } else {
                    s.setStatus(2); // 2 = 请假审批中
                    scheduleService.update(s);
                    redirectAttributes.addFlashAttribute("success", "请假申请已提交，等待管理员审批。");
                }
            }
        });
        return "redirect:/doctor/schedules";
    }

    // 住院管理 - 查看住院详情
    @GetMapping("/hospitalizations/{id}/manage")
    public String manageHospitalization(@PathVariable Long id, Model model) {
        Long doctorId = getCurrentDoctorId();
        if (doctorId == null) {
            return "redirect:/login/user";
        }

        Hospitalization hospitalization = hospitalizationService.findById(id).orElse(null);
        if (hospitalization == null) {
            return "redirect:/doctor/hospitalizations";
        }

        // 加载患者信息
        Patient patient = patientService.findById(hospitalization.getPatientId()).orElse(null);
        User patientUser = patient != null ? userService.findById(patient.getUserId()).orElse(null) : null;

        // 加载床位信息
        Bed bed = hospitalization.getBedId() != null ? bedService.findById(hospitalization.getBedId()).orElse(null) : null;

        // 加载医嘱列表
        List<MedicalOrder> orders = medicalOrderService.findByHospitalizationId(id);

        // 加载病历记录（兼容旧数据库：medical_record 表可能没有 hospitalization_id / condition_update 列）
        List<MedicalRecord> records;
        try {
            // 首选：按 hospitalizationId 查询（需要表中有 hospitalization_id 列）
            records = medicalRecordService.findByHospitalizationId(id);

            if (records.isEmpty()) {
                // 可能原因：
                // 1）表中没有 hospitalization_id 列；
                // 2）有该列，但历史记录尚未写入此字段。
                // 为兼容旧数据，这里回退到按 patientId 查询。
                List<MedicalRecord> patientRecords = medicalRecordService.findByPatientId(hospitalization.getPatientId());

                // 如果所有记录的 hospitalizationId 都为 null，说明：
                // - 要么列不存在，要么列存在但尚未使用
                boolean anyWithHospId = patientRecords.stream()
                        .anyMatch(r -> r.getHospitalizationId() != null);

                if (!anyWithHospId) {
                    // 旧库场景：没有住院关联，直接展示该患者的全部病历记录
                    records = patientRecords;
                } else {
                    // 列已经存在且部分记录有 hospitalizationId，这里只展示当前住院的相关记录
                    records = patientRecords.stream()
                            .filter(r -> r.getHospitalizationId() != null && r.getHospitalizationId().equals(id))
                            .collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            // 极端情况：查询出错时，至少保证可以按患者维度查看病历
            records = medicalRecordService.findByPatientId(hospitalization.getPatientId());
        }

        // 当前科室：优先床位所在科室 → 申请住院科室 → 医生档案所属科室
        Doctor doctorForDept = doctorService.findById(doctorId).orElse(null);
        Long manageDepartmentId = resolveHospitalizationDepartmentId(hospitalization, bed, doctorForDept);
        String manageDepartmentName = null;
        if (manageDepartmentId != null) {
            manageDepartmentName = departmentService.findById(manageDepartmentId)
                    .map(Department::getName)
                    .orElse(null);
        } else if (doctorForDept != null && doctorForDept.getDepartment() != null) {
            manageDepartmentName = doctorForDept.getDepartment();
        }

        // 科室范围内的在库药品与启用检查项目（含全院通用 department_id 为空）
        List<Medicine> medicines = medicineService.findAvailableByDepartmentOrCommon(manageDepartmentId);
        List<Examination> examinations = examinationService.findByDepartmentOrCommon(manageDepartmentId);

        // 根据入院/出院日期计算实际住院天数（用于展示“预计天数”）
        Long stayDays = null;
        if (hospitalization != null && hospitalization.getAdmissionDate() != null) {
            java.time.LocalDate endDate =
                    hospitalization.getDischargeDate() != null
                            ? hospitalization.getDischargeDate()
                            : java.time.LocalDate.now();
            long days = java.time.temporal.ChronoUnit.DAYS.between(hospitalization.getAdmissionDate(), endDate);
            if (days < 1) {
                days = 1; // 至少显示1天，避免0天的情况
            }
            stayDays = days;
        }

        model.addAttribute("hospitalization", hospitalization);
        model.addAttribute("patient", patient);
        model.addAttribute("patientUser", patientUser);
        model.addAttribute("bed", bed);
        model.addAttribute("orders", orders);
        model.addAttribute("records", records);
        model.addAttribute("medicines", medicines);
        model.addAttribute("examinations", examinations);
        model.addAttribute("stayDays", stayDays);
        model.addAttribute("manageDepartmentId", manageDepartmentId);
        model.addAttribute("manageDepartmentName", manageDepartmentName);

        // 历史数据：表结构无 admission_reason 时曾导致 INSERT 未写入 appointment_id；推断后回填，便于后续展示与统计
        if (hospitalization.getAppointmentId() == null && hospitalization.getPatientId() != null && hospitalization.getDoctorId() != null) {
            LocalDateTime anchor = hospitalization.getRequestTime();
            if (anchor == null) {
                anchor = hospitalization.getCreatedAt();
            }
            if (anchor == null && hospitalization.getAdmissionDate() != null) {
                anchor = hospitalization.getAdmissionDate().atStartOfDay();
            }
            List<Appointment> appsForInfer = appointmentService.findByPatientId(hospitalization.getPatientId());
            inferConsultAppointmentForHospitalization(hospitalization, anchor, appsForInfer).ifPresent(a -> {
                hospitalization.setAppointmentId(a.getId());
                hospitalizationService.update(hospitalization);
            });
        }

        model.addAttribute("displayDiagnosis", buildDisplayDiagnosisForHospitalization(hospitalization, records));
        return "doctor/hospitalization-manage";
    }

    /**
     * 疑似随意填写的诊断占位（如测试数据 "123"），不作为展示依据，应优先采用电子病历/挂号中的真实诊断。
     */
    private static boolean isLikelyPlaceholderDiagnosis(String s) {
        if (s == null) {
            return true;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return true;
        }
        if (t.length() <= 6 && t.matches("\\d+")) {
            return true;
        }
        String lower = t.toLowerCase();
        return "test".equals(lower) || "无".equals(t) || "暂无".equals(t) || "n/a".equals(lower);
    }

    /** 去掉值中的「诊断」+ 冒号类前缀（兼容空格、全角冒号），避免页面再显示成「诊断：诊断：xxx」 */
    private static final Pattern LEADING_DIAGNOSIS_LABEL = Pattern.compile("^诊断\\s*[:：]\\s*");

    private static String normalizeDiagnosisText(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        for (int i = 0; i < 5; i++) {
            String next = LEADING_DIAGNOSIS_LABEL.matcher(t).replaceFirst("").trim();
            if (next.equals(t)) {
                break;
            }
            t = next;
        }
        return t.isEmpty() ? null : t;
    }

    /**
     * 从病历正文解析「诊断」段。提交格式为「诊断：xxx\\n处方：…」，诊断可能多行。
     * 用「换行 + 处方」分割，避免 indexOf("\\n处方") 在「\\n\\n处方」处截断错误只得到单字。
     */
    private static String parseDiagnosisFromMedicalRecordContent(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        int idx = content.indexOf("诊断：");
        int prefixEnd;
        if (idx >= 0) {
            prefixEnd = idx + "诊断：".length();
        } else {
            idx = content.indexOf("诊断:");
            if (idx < 0) {
                return null;
            }
            prefixEnd = idx + "诊断:".length();
        }
        if (prefixEnd >= content.length()) {
            return null;
        }
        String rest = content.substring(prefixEnd);
        // 第一个「处方」段之前均为诊断正文（允许多个空行）
        String[] parts = rest.split("\\r?\\n\\s*处方\\s*[:：]?", 2);
        String block = parts[0].trim();
        return block.isEmpty() ? null : normalizeDiagnosisText(block);
    }

    /**
     * 单条病历：优先从正文「诊断：…处方：…」解析（与提交诊断页写入一致），其次 diagnosis 字段。
     * 避免 diagnosis 列残留单字/旧值而正文已是完整诊断的情况。
     */
    private String effectiveDiagnosisFromMedicalRecord(MedicalRecord r) {
        if (r == null) {
            return null;
        }
        String fromContent = parseDiagnosisFromMedicalRecordContent(r.getMedicalRecordContent());
        if (!isLikelyPlaceholderDiagnosis(fromContent)) {
            return fromContent;
        }
        String d = normalizeDiagnosisText(r.getDiagnosis());
        if (!isLikelyPlaceholderDiagnosis(d)) {
            return d;
        }
        return null;
    }

    /**
     * 同一挂号可能对应多条病历；在病历候选与挂号 diagnosis 中取「非占位且通常更完整」的一条（优先更长文本）。
     */
    private String resolveDiagnosisForAppointmentId(Long appointmentId) {
        if (appointmentId == null) {
            return null;
        }
        List<String> candidates = new ArrayList<>();
        for (MedicalRecord r : medicalRecordService.findAllByAppointmentId(appointmentId)) {
            String s = effectiveDiagnosisFromMedicalRecord(r);
            s = normalizeDiagnosisText(s);
            if (!isLikelyPlaceholderDiagnosis(s)) {
                candidates.add(s);
            }
        }
        appointmentService.findById(appointmentId).ifPresent(a -> {
            String s = normalizeDiagnosisText(a.getDiagnosis());
            if (!isLikelyPlaceholderDiagnosis(s)) {
                candidates.add(s);
            }
        });
        if (candidates.isEmpty()) {
            return null;
        }
        String best = candidates.stream().max(Comparator.comparingInt(String::length)).orElse(null);
        // 仅单字（如误触「撒」）且与其它来源一致时，不当作有效诊断，交给后续兜底或「暂无诊断」
        if (best != null && best.length() <= 1) {
            return null;
        }
        return best;
    }

    /**
     * 无 appointment_id 时：同主治医生、已完成、且能解析出有效诊断的接诊；
     * 若有申请时间锚点，取 14 天内时间最接近的一条；无锚点则取最近一条（按 updatedAt）。
     */
    private Optional<Appointment> inferConsultAppointmentForHospitalization(Hospitalization hospitalization,
                                                                              LocalDateTime anchor,
                                                                              List<Appointment> apps) {
        if (hospitalization == null || apps == null) {
            return Optional.empty();
        }
        Long doctorId = hospitalization.getDoctorId();
        if (doctorId == null) {
            return Optional.empty();
        }
        java.util.stream.Stream<Appointment> base = apps.stream()
                .filter(a -> doctorId.equals(a.getDoctorId()))
                .filter(a -> a.getStatus() == Appointment.AppointmentStatus.COMPLETED)
                .filter(a -> a.getUpdatedAt() != null)
                .filter(a -> {
                    if (!isLikelyPlaceholderDiagnosis(a.getDiagnosis())) {
                        return true;
                    }
                    return medicalRecordService.findAllByAppointmentId(a.getId()).stream()
                            .map(this::effectiveDiagnosisFromMedicalRecord)
                            .map(DoctorController::normalizeDiagnosisText)
                            .anyMatch(d -> !isLikelyPlaceholderDiagnosis(d));
                });
        if (anchor == null) {
            return base.max(Comparator.comparing(Appointment::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        }
        final long maxGapSeconds = 14L * 24 * 3600;
        return base
                .filter(a -> Math.abs(Duration.between(a.getUpdatedAt(), anchor).getSeconds()) <= maxGapSeconds)
                .min(Comparator.comparingLong((Appointment a) ->
                                Math.abs(Duration.between(a.getUpdatedAt(), anchor).getSeconds()))
                        .thenComparing(Appointment::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
    }

    /**
     * 住院管理页展示用诊断：应与医生端对该次接诊给出的诊断一致。
     * 优先住院表 appointmentId 对应挂号/病历；无则按申请时间推断最接近的接诊；再按时间窗口与住院字段兜底。
     */
    private String buildDisplayDiagnosisForHospitalization(Hospitalization hospitalization, List<MedicalRecord> records) {
        if (hospitalization == null) {
            return "暂无诊断";
        }
        final Long patientId = hospitalization.getPatientId();
        final Long hospDoctorId = hospitalization.getDoctorId();
        final Long hospId = hospitalization.getId();

        LocalDateTime anchor = hospitalization.getRequestTime();
        if (anchor == null) {
            anchor = hospitalization.getCreatedAt();
        }
        if (anchor == null && hospitalization.getAdmissionDate() != null) {
            anchor = hospitalization.getAdmissionDate().atStartOfDay();
        }

        List<Appointment> apps = patientId != null ? appointmentService.findByPatientId(patientId) : null;

        // 0) 精确：住院表上的来源挂号
        Long linkedAppointmentId = hospitalization.getAppointmentId();
        if (linkedAppointmentId != null) {
            String resolved = resolveDiagnosisForAppointmentId(linkedAppointmentId);
            if (resolved != null) {
                return normalizeDiagnosisText(resolved);
            }
        }

        // 0b) 旧数据未写入 appointment_id：推断与申请时间最接近的本次接诊挂号（无锚点则取同医生最近已完成）
        if (linkedAppointmentId == null && apps != null) {
            Optional<Appointment> inferred = inferConsultAppointmentForHospitalization(hospitalization, anchor, apps);
            if (inferred.isPresent()) {
                String resolved = resolveDiagnosisForAppointmentId(inferred.get().getId());
                if (resolved != null) {
                    return normalizeDiagnosisText(resolved);
                }
            }
        }

        // manage 页传入的 records 常为「仅住院关联」子集，接诊时写入的病历可能不在其中；诊断解析用患者全量病历
        List<MedicalRecord> sourceRecords = records;
        if (patientId != null) {
            List<MedicalRecord> full = medicalRecordService.findByPatientId(patientId);
            if (full != null && !full.isEmpty()) {
                sourceRecords = full;
            }
        }

        // 申请与写病历可能间隔数日，窗口放宽
        final LocalDateTime windowEnd = anchor != null ? anchor.plusDays(7) : null;

        if (patientId != null && hospDoctorId != null) {
            // A) 窗口内：主治医生 + 已完成挂号上的诊断（与 diagnose 提交同源）
            if (apps != null && windowEnd != null) {
                Optional<Appointment> consultApp = apps.stream()
                        .filter(a -> a.getDoctorId() != null && a.getDoctorId().equals(hospDoctorId))
                        .filter(a -> a.getStatus() == Appointment.AppointmentStatus.COMPLETED)
                        .filter(a -> !isLikelyPlaceholderDiagnosis(a.getDiagnosis()))
                        .filter(a -> a.getUpdatedAt() != null && !a.getUpdatedAt().isAfter(windowEnd))
                        .max(Comparator.comparing(Appointment::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
                if (consultApp.isPresent()) {
                    String ad = normalizeDiagnosisText(consultApp.get().getDiagnosis());
                    if (!isLikelyPlaceholderDiagnosis(ad) && ad != null && ad.length() > 1) {
                        return ad;
                    }
                }
            } else if (apps != null) {
                // 无锚点时间：同医生最近一条已完成挂号诊断
                Optional<Appointment> consultApp = apps.stream()
                        .filter(a -> a.getDoctorId() != null && a.getDoctorId().equals(hospDoctorId))
                        .filter(a -> a.getStatus() == Appointment.AppointmentStatus.COMPLETED)
                        .filter(a -> !isLikelyPlaceholderDiagnosis(a.getDiagnosis()))
                        .max(Comparator.comparing(Appointment::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
                if (consultApp.isPresent()) {
                    String ad = normalizeDiagnosisText(consultApp.get().getDiagnosis());
                    if (!isLikelyPlaceholderDiagnosis(ad) && ad != null && ad.length() > 1) {
                        return ad;
                    }
                }
            }

            // B) 窗口内：同期电子病历诊断（接诊时写入，非住院后病情更新）
            if (sourceRecords != null && windowEnd != null) {
                Optional<MedicalRecord> consultMr = sourceRecords.stream()
                        .filter(r -> r.getPatientId() != null && r.getPatientId().equals(patientId))
                        .filter(r -> r.getDoctorId() != null && r.getDoctorId().equals(hospDoctorId))
                        .filter(r -> !isLikelyPlaceholderDiagnosis(effectiveDiagnosisFromMedicalRecord(r)))
                        .filter(r -> r.getCreatedAt() != null && !r.getCreatedAt().isAfter(windowEnd))
                        .max(Comparator.comparing(MedicalRecord::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
                if (consultMr.isPresent()) {
                    String ed = normalizeDiagnosisText(effectiveDiagnosisFromMedicalRecord(consultMr.get()));
                    if (!isLikelyPlaceholderDiagnosis(ed) && ed != null && ed.length() > 1) {
                        return ed;
                    }
                }
            }
        }

        // C) 住院表入院诊断（申请时写入，非占位则用）
        String d = normalizeDiagnosisText(hospitalization.getDiagnosis());
        if (!isLikelyPlaceholderDiagnosis(d) && d != null && d.length() > 1) {
            return d;
        }

        // D) 出院诊断
        d = normalizeDiagnosisText(hospitalization.getDischargeDiagnosis());
        if (!isLikelyPlaceholderDiagnosis(d)) {
            return d;
        }

        // E) 关联本住院的病历中带诊断字段的记录（补充）
        if (sourceRecords != null && hospId != null) {
            Optional<MedicalRecord> tied = sourceRecords.stream()
                    .filter(r -> r.getHospitalizationId() != null && r.getHospitalizationId().equals(hospId))
                    .filter(r -> !isLikelyPlaceholderDiagnosis(effectiveDiagnosisFromMedicalRecord(r)))
                    .max(Comparator.comparing(MedicalRecord::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
            if (tied.isPresent()) {
                String ed = normalizeDiagnosisText(effectiveDiagnosisFromMedicalRecord(tied.get()));
                if (!isLikelyPlaceholderDiagnosis(ed) && ed != null && ed.length() > 1) {
                    return ed;
                }
            }
        }

        // F) 同医生任意非占位病历（最新）— 可能混入其它次就诊，仅作兜底
        if (sourceRecords != null && hospDoctorId != null) {
            Optional<MedicalRecord> byDoctor = sourceRecords.stream()
                    .filter(r -> r.getDoctorId() != null && r.getDoctorId().equals(hospDoctorId))
                    .filter(r -> !isLikelyPlaceholderDiagnosis(effectiveDiagnosisFromMedicalRecord(r)))
                    .max(Comparator.comparing(MedicalRecord::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
            if (byDoctor.isPresent()) {
                String ed = normalizeDiagnosisText(effectiveDiagnosisFromMedicalRecord(byDoctor.get()));
                if (!isLikelyPlaceholderDiagnosis(ed) && ed != null && ed.length() > 1) {
                    return ed;
                }
            }
        }

        // G) 任意挂号 / 病历
        if (apps != null) {
            Optional<Appointment> anyApp = apps.stream()
                    .filter(a -> !isLikelyPlaceholderDiagnosis(a.getDiagnosis()))
                    .max(Comparator.comparing(Appointment::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
            if (anyApp.isPresent()) {
                String ad = normalizeDiagnosisText(anyApp.get().getDiagnosis());
                if (!isLikelyPlaceholderDiagnosis(ad) && ad != null && ad.length() > 1) {
                    return ad;
                }
            }
        }
        if (sourceRecords != null) {
            Optional<MedicalRecord> anyMr = sourceRecords.stream()
                    .filter(r -> !isLikelyPlaceholderDiagnosis(effectiveDiagnosisFromMedicalRecord(r)))
                    .max(Comparator.comparing(MedicalRecord::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
            if (anyMr.isPresent()) {
                String ed = normalizeDiagnosisText(effectiveDiagnosisFromMedicalRecord(anyMr.get()));
                if (!isLikelyPlaceholderDiagnosis(ed) && ed != null && ed.length() > 1) {
                    return ed;
                }
            }
        }

        return "暂无诊断";
    }

    // 添加医嘱
    @PostMapping("/hospitalizations/{id}/orders")
    @org.springframework.transaction.annotation.Transactional
    public String addMedicalOrder(@PathVariable Long id,
                                  @RequestParam String orderType,
                                  @RequestParam String orderContent,
                                  @RequestParam(required = false) Long medicineId,
                                  @RequestParam(required = false) Integer medicineQuantity,
                                  @RequestParam(required = false) Long examinationId,
                                  @RequestParam(required = false) String dosage,
                                  @RequestParam(required = false) String frequency,
                                  RedirectAttributes redirectAttributes) {
        Long doctorId = getCurrentDoctorId();
        if (doctorId == null) {
            return "redirect:/login/user";
        }

        Hospitalization hospitalization = hospitalizationService.findById(id).orElse(null);
        if (hospitalization == null) {
            return "redirect:/doctor/hospitalizations";
        }

        Bed bed = hospitalization.getBedId() != null
                ? bedService.findById(hospitalization.getBedId()).orElse(null)
                : null;
        Doctor doctor = doctorService.findById(doctorId).orElse(null);
        Long deptId = resolveHospitalizationDepartmentId(hospitalization, bed, doctor);

        MedicalOrder.OrderType ot;
        try {
            ot = MedicalOrder.OrderType.valueOf(orderType);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "无效的医嘱类型");
            return "redirect:/doctor/hospitalizations/" + id + "/manage";
        }

        hospitalizationService.requireActiveCare(id);
        if (ot == MedicalOrder.OrderType.MEDICATION && medicineId == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "用药医嘱必须选择药品");
        }
        if (ot == MedicalOrder.OrderType.EXAMINATION && examinationId == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "检查医嘱必须选择检查项目");
        }
        if (medicineQuantity != null && (medicineQuantity < 1 || medicineQuantity > 9999)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "药品数量必须为1至9999");
        }
        if (ot != MedicalOrder.OrderType.MEDICATION) {
            medicineId = null;
        }
        if (ot != MedicalOrder.OrderType.EXAMINATION) {
            examinationId = null;
        }

        BigDecimal addAmount = BigDecimal.ZERO;

        int medQty = 1;
        if (ot == MedicalOrder.OrderType.MEDICATION && medicineId != null) {
            Medicine med = medicineService.findById(medicineId).orElse(null);
            if (med == null) {
                redirectAttributes.addFlashAttribute("error", "选择的药品不存在");
                return "redirect:/doctor/hospitalizations/" + id + "/manage";
            }
            if (!isMedicineAllowedForDepartment(med, deptId)) {
                redirectAttributes.addFlashAttribute("error", "该药品不在当前科室可选范围内");
                return "redirect:/doctor/hospitalizations/" + id + "/manage";
            }
            if (medicineQuantity != null) {
                medQty = medicineQuantity;
                if (medQty < 1) {
                    medQty = 1;
                }
                if (medQty > 9999) {
                    medQty = 9999;
                }
            }
            if (med.getPrice() != null) {
                addAmount = addAmount.add(med.getPrice().multiply(BigDecimal.valueOf(medQty)));
            }
        } else if (ot == MedicalOrder.OrderType.EXAMINATION && examinationId != null) {
            Examination ex = examinationService.findById(examinationId).orElse(null);
            if (ex == null) {
                redirectAttributes.addFlashAttribute("error", "选择的检查项目不存在");
                return "redirect:/doctor/hospitalizations/" + id + "/manage";
            }
            if (!isExaminationAllowedForDepartment(ex, deptId)) {
                redirectAttributes.addFlashAttribute("error", "该检查项目不在当前科室可选范围内");
                return "redirect:/doctor/hospitalizations/" + id + "/manage";
            }
            if (ex.getPrice() != null) {
                addAmount = addAmount.add(ex.getPrice());
            }
        }

        MedicalOrder order = new MedicalOrder();
        order.setHospitalizationId(id);
        order.setPatientId(hospitalization.getPatientId());
        order.setDoctorId(doctorId);
        order.setOrderType(ot);
        order.setOrderContent(orderContent);
        order.setMedicineId(medicineId);
        if (ot == MedicalOrder.OrderType.MEDICATION && medicineId != null) {
            order.setQuantity(medQty);
        }
        order.setExaminationId(examinationId);
        order.setDosage(dosage);
        order.setFrequency(frequency);
        order.setStatus(MedicalOrder.OrderStatus.ACTIVE);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        medicalOrderService.save(order);

        hospitalizationService.addOrderCost(id, order.getId(), addAmount);

        redirectAttributes.addFlashAttribute("success", "医嘱已添加");
        return "redirect:/doctor/hospitalizations/" + id + "/manage";
    }

    // 更新病情
    @PostMapping("/hospitalizations/{id}/update-condition")
    @org.springframework.transaction.annotation.Transactional
    public String updateCondition(@PathVariable Long id, @RequestParam String conditionUpdate, RedirectAttributes redirectAttributes) {
        hospitalizationService.requireActiveCare(id);
        Long doctorId = getCurrentDoctorId();
        if (doctorId == null) {
            return "redirect:/login/user";
        }

        Hospitalization hospitalization = hospitalizationService.findById(id).orElse(null);
        if (hospitalization == null) {
            return "redirect:/doctor/hospitalizations";
        }

        // 创建或更新病历记录
        MedicalRecord record = new MedicalRecord();
        record.setHospitalizationId(id);
        record.setPatientId(hospitalization.getPatientId());
        record.setDoctorId(doctorId);
        record.setConditionUpdate(conditionUpdate);
        // 病情更新仅写入 condition_update，避免与 medical_record_content 重复（患者端会分别展示）
        record.setMedicalRecordContent(null);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        medicalRecordService.save(record);

        redirectAttributes.addFlashAttribute("success", "病情已更新");
        return "redirect:/doctor/hospitalizations/" + id + "/manage";
    }

    // 办理出院
    @GetMapping("/hospitalizations/{id}/discharge")
    public String dischargeForm(@PathVariable Long id, Model model) {
        Long doctorId = getCurrentDoctorId();
        if (doctorId == null) {
            return "redirect:/login/user";
        }

        Hospitalization hospitalization = hospitalizationService.findById(id).orElse(null);
        if (hospitalization == null || hospitalization.getStatus() != Hospitalization.HospitalizationStatus.ADMITTED) {
            return "redirect:/doctor/hospitalizations";
        }

        Patient patient = patientService.findById(hospitalization.getPatientId()).orElse(null);
        User patientUser = patient != null ? userService.findById(patient.getUserId()).orElse(null) : null;

        model.addAttribute("hospitalization", hospitalization);
        model.addAttribute("patient", patient);
        model.addAttribute("patientUser", patientUser);
        return "doctor/discharge-form";
    }

    @PostMapping("/hospitalizations/{id}/discharge")
    @org.springframework.transaction.annotation.Transactional
    public String submitDischarge(@PathVariable Long id,
                                  @RequestParam String dischargeDiagnosis,
                                  @RequestParam String dischargeNotes,
                                  RedirectAttributes redirectAttributes) {
        hospitalizationService.requireActiveCare(id);
        Long doctorId = getCurrentDoctorId();
        if (doctorId == null) {
            return "redirect:/login/user";
        }

        Hospitalization hospitalization = hospitalizationService.findById(id).orElse(null);
        if (hospitalization == null) {
            return "redirect:/doctor/hospitalizations";
        }

        hospitalization.setDischargeDiagnosis(dischargeDiagnosis);
        hospitalization.setDischargeNotes(dischargeNotes);
        hospitalization.setDischargeDate(LocalDate.now());
        hospitalization.setStatus(Hospitalization.HospitalizationStatus.DISCHARGED);
        hospitalizationService.update(hospitalization);

        // 医生同意出院后自动结账并释放床位（与管理员手动结算规则一致）
        hospitalizationService.settleAndReleaseBedAfterDischarge(id);

        // 出院后：用药/检查医嘱由「执行中」→「已完成」，并关闭「申请出院」医嘱
        medicalOrderService.completeActiveOrdersOnDischarge(id);

        // 生成出院报告
        Report report = new Report();
        report.setPatientId(hospitalization.getPatientId());
        report.setDoctorId(doctorId);
        report.setReportType("出院报告");
        report.setReportContent("出院诊断：" + dischargeDiagnosis + "\n注意事项：" + dischargeNotes);
        report.setStatus(Report.ReportStatus.COMPLETED);
        report.setReportDate(LocalDateTime.now());
        reportService.save(report);

        redirectAttributes.addFlashAttribute("success", "出院手续已办理，系统已自动完成费用结算并释放床位");
        return "redirect:/doctor/hospitalizations";
    }

    /**
     * 住院管理页「当前科室」：床位科室 &gt; 申请住院科室 &gt; 医生档案科室名对应 ID。
     */
    private Long resolveHospitalizationDepartmentId(Hospitalization hospitalization, Bed bed, Doctor doctor) {
        if (bed != null && bed.getDepartmentId() != null) {
            return bed.getDepartmentId();
        }
        if (hospitalization.getRequestDepartmentId() != null) {
            return hospitalization.getRequestDepartmentId();
        }
        if (doctor != null && doctor.getDepartment() != null && !doctor.getDepartment().isBlank()) {
            String deptName = doctor.getDepartment().trim();
            return departmentService.findAll().stream()
                    .filter(d -> deptName.equals(d.getName()))
                    .map(Department::getId)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private static boolean isMedicineAllowedForDepartment(Medicine medicine, Long departmentId) {
        if (departmentId == null) {
            return true;
        }
        return medicine.getDepartmentId() == null || medicine.getDepartmentId().equals(departmentId);
    }

    private static boolean isExaminationAllowedForDepartment(Examination examination, Long departmentId) {
        if (departmentId == null) {
            return true;
        }
        return examination.getDepartmentId() == null || examination.getDepartmentId().equals(departmentId);
    }

    /** 本挂号已开处方明细中药品累计数量（用于重新诊断时增量开药） */
    private Map<Long, Integer> computePreviousMedicineQuantities(Long appointmentId) {
        Map<Long, Integer> map = new HashMap<>();
        for (Prescription p : prescriptionService.findByAppointmentId(appointmentId)) {
            for (PrescriptionItem it : prescriptionService.getPrescriptionItems(p.getId())) {
                if (it.getMedicineId() == null) {
                    continue;
                }
                int q = it.getQuantity() != null ? it.getQuantity() : 0;
                map.merge(it.getMedicineId(), q, (oldV, newV) -> {
                    int a = oldV != null ? oldV : 0;
                    int b = newV != null ? newV : 0;
                    return a + b;
                });
            }
        }
        return map;
    }

    /** 诊断表单中药品数量字段：medicineQty_{medicineId} */
    private static int parseMedicineQuantity(HttpServletRequest request, String medicineId) {
        String v = request.getParameter("medicineQty_" + medicineId);
        if (v == null || v.isBlank()) {
            return 1;
        }
        try {
            int q = Integer.parseInt(v.trim());
            if (q < 1) {
                return 1;
            }
            if (q > 9999) {
                return 9999;
            }
            return q;
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    // ====== AI 辅助接口（诊断 / 报告草稿，仅返回文本建议）======

    @PostMapping("/ai/diagnosis-suggestion")
    @ResponseBody
    public String generateDiagnosisSuggestion(@RequestParam(required = false) String symptoms,
                                              @RequestParam(required = false) String existingDiagnosis,
                                              @RequestParam(required = false) List<Long> medicineIds,
                                              @RequestParam(required = false) List<Long> examinationIds,
                                              @RequestParam(required = false) Long appointmentId,
                                              @RequestParam(required = false) Integer age,
                                              @RequestParam(required = false) String gender,
                                              @RequestParam(required = false) String historySummary,
                                              @RequestParam(required = false) String department) {
        List<Medicine> medicines = new ArrayList<>();
        if (medicineIds != null) {
            for (Long id : medicineIds) {
                medicineService.findById(id).ifPresent(medicines::add);
            }
        }

        List<Examination> examinations = new ArrayList<>();
        if (examinationIds != null) {
            for (Long id : examinationIds) {
                examinationService.findById(id).ifPresent(examinations::add);
            }
        }
        AiSuggestionService.DiagnosisContext context = new AiSuggestionService.DiagnosisContext();
        context.age = age;
        context.gender = gender;
        context.historySummary = historySummary;
        context.department = department;

        if (appointmentId != null) {
            Appointment appointment = appointmentService.findById(appointmentId).orElse(null);
            if (appointment != null) {
                if (symptoms == null || symptoms.isBlank()) {
                    symptoms = appointment.getSymptoms();
                }
                if (appointment.getPatientId() != null) {
                    Patient p = patientService.findById(appointment.getPatientId()).orElse(null);
                    if (p != null) {
                        if (context.gender == null || context.gender.isBlank()) {
                            context.gender = p.getGender() != null ? p.getGender().name() : null;
                        }
                        if (context.age == null && p.getBirthday() != null) {
                            context.age = Period.between(p.getBirthday(), LocalDate.now()).getYears();
                        }
                        if (context.historySummary == null || context.historySummary.isBlank()) {
                            List<MedicalRecord> records = medicalRecordService.findByPatientId(p.getId());
                            StringBuilder historyBuilder = new StringBuilder();
                            for (MedicalRecord r : records) {
                                String diagnosis = r.getDiagnosis() != null ? r.getDiagnosis().trim() : "";
                                if (!diagnosis.isEmpty() && !aiSuggestionService.isUnsuitableDiagnosisForAiHistory(diagnosis)) {
                                    if (historyBuilder.length() > 0) {
                                        historyBuilder.append("；");
                                    }
                                    historyBuilder.append(diagnosis);
                                }
                                if (historyBuilder.length() > 120) {
                                    break;
                                }
                            }
                            context.historySummary = historyBuilder.toString();
                        }
                    }
                }
                if (context.department == null || context.department.isBlank()) {
                    Long doctorId = appointment.getDoctorId();
                    Doctor doctor = doctorId != null ? doctorService.findById(doctorId).orElse(null) : null;
                    context.department = doctor != null ? doctor.getDepartment() : null;
                }
            }
        }
        return aiSuggestionService.buildDiagnosisSuggestion(
                symptoms,
                existingDiagnosis,
                medicines,
                examinations,
                context,
                "doctor:" + getCurrentUsername(),
                "doctor.diagnosis"
        );
    }

    @PostMapping("/ai/report-suggestion")
    @ResponseBody
    public String generateReportSuggestion(@RequestParam(required = false) Long examinationId,
                                           @RequestParam(required = false) String rawResult,
                                           @RequestParam(required = false) String clinicalNote) {
        Examination exam = null;
        if (examinationId != null) {
            exam = examinationService.findById(examinationId).orElse(null);
        }
        return aiSuggestionService.buildReportSuggestion(
                exam,
                rawResult,
                clinicalNote,
                "doctor:" + getCurrentUsername(),
                "doctor.report"
        );
    }
}
