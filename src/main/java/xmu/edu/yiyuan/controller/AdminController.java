package xmu.edu.yiyuan.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import xmu.edu.yiyuan.entity.*;
import xmu.edu.yiyuan.service.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {
    @Autowired private PharmacyService pharmacyService;

    @Autowired
    private UserService userService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private HospitalizationService hospitalizationService;

    @Autowired
    private MedicineService medicineService;

    @Autowired
    private ExaminationService examinationService;

    @Autowired
    private BedService bedService;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private RegisterService registerService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private AiSuggestionService aiSuggestionService;

    @Autowired
    private MedicineInboundService medicineInboundService;

    @Autowired
    private MedicineStockService medicineStockService;

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private MedicalOrderService medicalOrderService;

    @Autowired
    private AiGenerationAuditService aiGenerationAuditService;

    // ============ 管理员个人信息 ============
    @GetMapping("/profile")
    public String adminProfile(Model model, java.security.Principal principal) {
        if (principal != null) {
            userService.findByUsername(principal.getName()).ifPresent(admin -> model.addAttribute("admin", admin));
        }
        return "admin/profile";
    }

    // ============ 医生信息管理 / 审核医生注册 ============

    // ============ 仪表盘 ============
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        LocalDate today = LocalDate.now();

        long todayAppointments = appointmentService.findAll().stream()
                .filter(a -> a.getAppointmentDate() != null && a.getAppointmentDate().equals(today))
                .count();

        long inHospitalCount = hospitalizationService.findAll().stream()
                .filter(h -> h.getStatus() == Hospitalization.HospitalizationStatus.ADMITTED)
                .count();

        List<Medicine> lowStockMedicines = medicineService.findAll().stream()
                .filter(m -> m.getStock() != null && m.getStock() < 10)
                .collect(Collectors.toList());

        List<Bed> allBeds = bedService.findAll();
        long occupiedBeds = allBeds.stream()
                .filter(b -> b.getStatus() == Bed.BedStatus.OCCUPIED)
                .count();
        long totalBeds = allBeds.size();
        double bedOccupancyRate = totalBeds == 0 ? 0.0 : (occupiedBeds * 100.0 / totalBeds);

        long pendingHospitalizations = hospitalizationService.findAll().stream()
                .filter(h -> h.getRequestStatus() == Hospitalization.RequestStatus.PENDING)
                .count();

        model.addAttribute("todayAppointments", todayAppointments);
        model.addAttribute("inHospitalCount", inHospitalCount);
        model.addAttribute("lowStockMedicines", lowStockMedicines);
        model.addAttribute("occupiedBeds", occupiedBeds);
        model.addAttribute("totalBeds", totalBeds);
        model.addAttribute("bedOccupancyRate", String.format("%.1f", bedOccupancyRate));
        model.addAttribute("pendingHospitalizations", pendingHospitalizations);
        return "admin/dashboard";
    }

    @PostMapping("/ai/summary")
    @ResponseBody
    public String generateAdminSummary() {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.isAuthenticated()) ? auth.getName() : "unknown";
        LocalDate today = LocalDate.now();

        long todayAppointments = appointmentService.findAll().stream()
                .filter(a -> a.getAppointmentDate() != null && a.getAppointmentDate().equals(today))
                .count();
        long inHospitalCount = hospitalizationService.findAll().stream()
                .filter(h -> h.getStatus() == Hospitalization.HospitalizationStatus.ADMITTED)
                .count();
        long pendingHospitalizations = hospitalizationService.findAll().stream()
                .filter(h -> h.getRequestStatus() == Hospitalization.RequestStatus.PENDING)
                .count();
        List<Medicine> lowStockMedicines = medicineService.findAll().stream()
                .filter(m -> m.getStock() != null && m.getStock() < 10)
                .collect(Collectors.toList());
        List<Bed> allBeds = bedService.findAll();
        long occupiedBeds = allBeds.stream()
                .filter(b -> b.getStatus() == Bed.BedStatus.OCCUPIED)
                .count();
        long totalBeds = allBeds.size();

        return aiSuggestionService.buildAdminSummary(
                todayAppointments,
                inHospitalCount,
                pendingHospitalizations,
                occupiedBeds,
                totalBeds,
                lowStockMedicines.size(),
                "admin:" + username,
                "admin.summary"
        );
    }

    @GetMapping("/ai-logs")
    public String aiLogs(@RequestParam(required = false) String whoUser,
                         @RequestParam(required = false) String whereScene,
                         @RequestParam(required = false) String fromTime,
                         @RequestParam(required = false) String toTime,
                         @RequestParam(required = false, defaultValue = "200") Integer limit,
                         Model model) {
        LocalDateTime from = parseDateTime(fromTime);
        LocalDateTime to = parseDateTime(toTime);
        List<AiGenerationAuditService.AiGenerationLogItem> logs =
                aiGenerationAuditService.queryLogs(whoUser, whereScene, from, to, limit == null ? 200 : limit);
        List<String> triggerUsers = aiGenerationAuditService.findAllTriggerUsers();
        List<AiLogView> logViews = new ArrayList<>();
        for (AiGenerationAuditService.AiGenerationLogItem it : logs) {
            AiLogView v = new AiLogView();
            v.id = it.getId();
            v.whoUser = it.getWhoUser();
            v.whoUserZh = toWhoZh(it.getWhoUser());
            v.whereScene = it.getWhereScene();
            v.whereSceneZh = toSceneZh(it.getWhereScene());
            v.inputPayloadZh = toInputPayloadZh(it.getInputPayload(), it.getWhereScene());
            v.outputText = it.getOutputText();
            v.createdAt = it.getCreatedAt();
            logViews.add(v);
        }

        model.addAttribute("logs", logViews);
        model.addAttribute("triggerUsers", triggerUsers);
        model.addAttribute("whoUser", whoUser);
        model.addAttribute("whereScene", whereScene);
        model.addAttribute("fromTime", fromTime);
        model.addAttribute("toTime", toTime);
        model.addAttribute("limit", limit);
        return "admin/ai-logs";
    }

    @PostMapping("/ai-logs/bulk-delete")
    public String bulkDeleteAiLogs(@RequestParam(required = false) Long[] ids,
                                   @RequestParam(required = false) String whoUser,
                                   @RequestParam(required = false) String whereScene,
                                   @RequestParam(required = false) String fromTime,
                                   @RequestParam(required = false) String toTime,
                                   @RequestParam(required = false, defaultValue = "200") Integer limit,
                                   RedirectAttributes redirectAttributes) {
        if (ids == null || ids.length == 0) {
            redirectAttributes.addFlashAttribute("error", "请至少选择一条日志");
        } else {
            int deleted = aiGenerationAuditService.deleteByIds(java.util.Arrays.asList(ids));
            redirectAttributes.addFlashAttribute("success", "已批量删除 " + deleted + " 条日志");
        }

        redirectAttributes.addAttribute("whoUser", whoUser);
        redirectAttributes.addAttribute("whereScene", whereScene);
        redirectAttributes.addAttribute("fromTime", fromTime);
        redirectAttributes.addAttribute("toTime", toTime);
        redirectAttributes.addAttribute("limit", limit);
        return "redirect:/admin/ai-logs";
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private String toWhoZh(String who) {
        if (who == null || who.isBlank()) {
            return "未知触发人";
        }
        if (who.startsWith("doctor:")) {
            return "医生（" + who.substring("doctor:".length()) + "）";
        }
        if (who.startsWith("patient:")) {
            return "患者（" + who.substring("patient:".length()) + "）";
        }
        if (who.startsWith("admin:")) {
            return "管理员（" + who.substring("admin:".length()) + "）";
        }
        return who;
    }

    private String toSceneZh(String scene) {
        if (scene == null) {
            return "未知场景";
        }
        return switch (scene) {
            case "doctor.diagnosis" -> "医生端-诊断建议";
            case "doctor.report" -> "医生端-报告草稿";
            case "patient.triage" -> "患者端-导诊建议";
            case "admin.summary" -> "管理端-运营摘要";
            default -> scene;
        };
    }

    private String toInputPayloadZh(String payload, String scene) {
        Map<String, String> keyMap = new LinkedHashMap<>();
        keyMap.put("description", "症状描述");
        keyMap.put("symptoms", "主诉");
        keyMap.put("existingDiagnosis", "已有诊断");
        keyMap.put("medicineCount", "药品数量");
        keyMap.put("examinationCount", "检查数量");
        keyMap.put("context", "结构化上下文");
        keyMap.put("examName", "检查项目");
        keyMap.put("examType", "检查类型");
        keyMap.put("rawResult", "原始结果");
        keyMap.put("clinicalNote", "临床备注");
        keyMap.put("todayAppointments", "今日门诊量");
        keyMap.put("inHospitalCount", "在院人数");
        keyMap.put("pendingHospitalizations", "待审批住院");
        keyMap.put("occupiedBeds", "已占床位");
        keyMap.put("totalBeds", "总床位");
        keyMap.put("lowStockCount", "低库存药品数");
        keyMap.put("matchedKeywords", "命中关键词");

        if (payload == null || payload.isBlank()) {
            return "无输入参数";
        }
        String text = payload.trim();
        if (text.startsWith("{") && text.endsWith("}")) {
            text = text.substring(1, text.length() - 1);
        }
        if (text.isBlank()) {
            return "无输入参数";
        }

        String[] parts = text.split(", ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            int idx = part.indexOf('=');
            if (idx <= 0) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(part);
                continue;
            }
            String k = part.substring(0, idx);
            String v = part.substring(idx + 1);
            String kZh = keyMap.getOrDefault(k, k);
            if (sb.length() > 0) sb.append("\n");
            sb.append(kZh).append("：").append(v);
        }
        return sb.toString();
    }

    public static class AiLogView {
        public Long id;
        public String whoUser;
        public String whoUserZh;
        public String whereScene;
        public String whereSceneZh;
        public String inputPayloadZh;
        public String outputText;
        public LocalDateTime createdAt;
    }

    // ============ 科室管理 ============
    @GetMapping("/departments")
    public String departments(Model model) {
        List<Department> departments = departmentService.findAll();

        // 只统计每个科室的医生数量（科室列表页不再展开显示医生明细）
        java.util.Map<String, Long> deptNameToId = new java.util.HashMap<>();
        for (Department dept : departments) {
            if (dept.getName() != null) {
                deptNameToId.put(dept.getName(), dept.getId());
            }
        }

        java.util.Map<Long, Integer> departmentDoctorCountMap = new java.util.HashMap<>();
        List<Doctor> allDoctors = doctorService.findAll();
        for (Doctor doctor : allDoctors) {
            if (doctor.getDepartment() == null) continue;
            Long departmentId = deptNameToId.get(doctor.getDepartment());
            if (departmentId == null) continue;
            departmentDoctorCountMap.put(departmentId, departmentDoctorCountMap.getOrDefault(departmentId, 0) + 1);
        }

        model.addAttribute("departments", departments);
        model.addAttribute("departmentDoctorCountMap", departmentDoctorCountMap);
        return "admin/departments";
    }

    @GetMapping("/departments/new")
    public String newDepartment(Model model) {
        model.addAttribute("department", new Department());
        return "admin/department-form";
    }

    @PostMapping("/departments")
    public String createDepartment(@ModelAttribute Department department) {
        if (department.getName() == null || department.getName().trim().isEmpty()) {
            return "redirect:/admin/departments?error=true";
        }
        department.setName(department.getName().trim());
        departmentService.save(department);
        return "redirect:/admin/departments";
    }

    @GetMapping("/departments/{id}/edit")
    public String editDepartment(@PathVariable Long id, Model model) {
        Department department = departmentService.findById(id).orElse(new Department());

        // 加载该科室的医生列表
        List<Doctor> departmentDoctors = doctorService.findAll().stream()
                .filter(doctor -> doctor.getDepartment() != null && doctor.getDepartment().equals(department.getName()))
                .collect(Collectors.toList());

        java.util.Map<Long, User> doctorUserMap = new java.util.HashMap<>();
        for (Doctor doctor : departmentDoctors) {
            userService.findById(doctor.getUserId()).ifPresent(user -> {
                doctorUserMap.put(doctor.getId(), user);
            });
        }

        model.addAttribute("department", department);
        model.addAttribute("departmentDoctors", departmentDoctors);
        model.addAttribute("doctorUserMap", doctorUserMap);
        model.addAttribute("departments", departmentService.findAll());
        return "admin/department-form";
    }

    @PostMapping("/departments/{id}")
    public String updateDepartment(@PathVariable Long id, @ModelAttribute Department department) {
        Department existingDept = departmentService.findById(id).orElse(null);
        if (existingDept == null) {
            return "redirect:/admin/departments";
        }

        String oldName = existingDept.getName();
        department.setId(id);
        if (department.getName() != null) {
            department.setName(department.getName().trim());
        }
        departmentService.update(department);

        // 如果科室名称改变，更新该科室下所有医生的科室字段
        if (!oldName.equals(department.getName())) {
            List<Doctor> doctors = doctorService.findAll().stream()
                    .filter(doctor -> oldName.equals(doctor.getDepartment()))
                    .collect(Collectors.toList());
            for (Doctor doctor : doctors) {
                doctor.setDepartment(department.getName());
                doctorService.update(doctor);
            }
        }

        return "redirect:/admin/departments";
    }

    @PostMapping("/departments/{id}/delete")
    public String deleteDepartment(@PathVariable Long id) {
        departmentService.deleteById(id);
        return "redirect:/admin/departments";
    }

    @PostMapping("/departments/bulk-delete")
    public String bulkDeleteDepartments(@RequestParam(required = false) Long[] ids,
                                        RedirectAttributes redirectAttributes) {
        if (ids == null || ids.length == 0) {
            redirectAttributes.addFlashAttribute("error", "请至少选择一个科室");
            return "redirect:/admin/departments";
        }
        for (Long id : ids) {
            departmentService.deleteById(id);
        }
        departmentService.resetAutoIncrementIfEmpty();
        redirectAttributes.addFlashAttribute("success", "已批量删除 " + ids.length + " 个科室");
        return "redirect:/admin/departments";
    }

    // 查看医生的排班
    @GetMapping("/departments/{departmentId}/doctors/{doctorId}/schedules")
    public String doctorSchedules(@PathVariable Long departmentId,
                                  @PathVariable Long doctorId,
                                  Model model) {
        Department department = departmentService.findById(departmentId).orElse(null);
        Doctor doctor = doctorService.findById(doctorId).orElse(null);

        if (department == null || doctor == null) {
            return "redirect:/admin/departments";
        }

        // 加载医生用户信息
        User doctorUser = null;
        if (doctor.getUserId() != null) {
            doctorUser = userService.findById(doctor.getUserId()).orElse(null);
        }

        // 加载该医生的所有排班
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

        // 按日期排序
        schedules.sort((s1, s2) -> {
            if (s1.getWorkDate() == null && s2.getWorkDate() == null) return 0;
            if (s1.getWorkDate() == null) return 1;
            if (s2.getWorkDate() == null) return -1;
            return s2.getWorkDate().compareTo(s1.getWorkDate());
        });

        model.addAttribute("department", department);
        model.addAttribute("doctor", doctor);
        model.addAttribute("doctorUser", doctorUser);
        model.addAttribute("schedules", schedules);
        return "admin/doctor-schedules";
    }

    // ============ 医生信息管理（整合到科室管理中）===========
    @PostMapping("/departments/{departmentId}/doctors")
    public String createDoctor(@PathVariable Long departmentId,
                               @RequestParam String username,
                               @RequestParam String password,
                               @RequestParam String realName,
                               @RequestParam(required = false) String email,
                               @RequestParam(required = false) String phone,
                               @RequestParam String department,
                               @RequestParam(required = false) String title,
                               @RequestParam(required = false) String specialty,
                               @RequestParam(required = false) String introduction,
                               RedirectAttributes redirectAttributes) {
        Department dept = departmentService.findById(departmentId).orElse(null);
        if (dept == null) {
            redirectAttributes.addFlashAttribute("error", "科室不存在");
            return "redirect:/admin/departments";
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRealName(realName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setRole(User.Role.DOCTOR);
        user.setStatus(1);

        Doctor doctor = new Doctor();
        doctor.setDepartment(department);
        doctor.setTitle(title);
        doctor.setSpecialty(specialty);
        doctor.setIntroduction(introduction);

        registerService.registerDoctor(user, doctor);
        redirectAttributes.addFlashAttribute("success", "医生添加成功");
        return "redirect:/admin/departments/" + departmentId + "/edit";
    }

    // 审核通过自助注册的医生账号
    @PostMapping("/doctors/{doctorId}/approve")
    public String approveDoctor(@PathVariable Long doctorId,
                                @RequestParam Long departmentId,
                                RedirectAttributes redirectAttributes) {
        Doctor doctor = doctorService.findById(doctorId).orElse(null);
        if (doctor == null || doctor.getUserId() == null) {
            redirectAttributes.addFlashAttribute("error", "待审核医生不存在");
            return "redirect:/admin/departments/" + departmentId + "/edit";
        }
        userService.findById(doctor.getUserId()).ifPresent(u -> {
            u.setStatus(1);
            userService.update(u);
        });
        redirectAttributes.addFlashAttribute("success", "已通过医生注册申请");
        return "redirect:/admin/departments/" + departmentId + "/edit";
    }

    // 删除医生及其账号、排班
    @PostMapping("/doctors/{doctorId}/delete")
    public String deleteDoctor(@PathVariable Long doctorId,
                               @RequestParam Long departmentId,
                               RedirectAttributes redirectAttributes) {
        Doctor doctor = doctorService.findById(doctorId).orElse(null);
        if (doctor == null) {
            redirectAttributes.addFlashAttribute("error", "医生不存在或已被删除");
            return "redirect:/admin/departments/" + departmentId + "/edit";
        }

        // 删除医生账号
        if (doctor.getUserId() != null) {
            userService.deleteById(doctor.getUserId());
        }

        // 删除该医生的排班
        List<Schedule> schedules = scheduleService.findByDoctorId(doctorId);
        for (Schedule s : schedules) {
            if (s.getId() != null) {
                scheduleService.deleteById(s.getId());
            }
        }

        // 删除医生记录
        doctorService.deleteById(doctorId);

        redirectAttributes.addFlashAttribute("success", "医生及其账号已删除");
        return "redirect:/admin/departments/" + departmentId + "/edit";
    }

    // 兼容旧页面入口：医生管理已整合到科室管理
    @GetMapping("/doctors")
    public String doctorsLegacy() {
        return "redirect:/admin/departments";
    }

    // 兼容旧页面入口：新增医生通过科室编辑页完成
    @GetMapping("/doctors/new")
    public String newDoctorLegacy() {
        return "redirect:/admin/departments";
    }

    @GetMapping("/users/{id}/change-password")
    public String changePasswordForm(@PathVariable Long id,
                                     @RequestParam(required = false) Long departmentId,
                                     @RequestParam(required = false) String returnTo,
                                     Model model) {
        User user = userService.findById(id).orElse(null);
        if (user == null) {
            return "redirect:/admin/departments";
        }
        model.addAttribute("user", user);
        model.addAttribute("departmentId", departmentId);
        model.addAttribute("returnTo", normalizeAdminReturnTo(returnTo));
        return "admin/change-password-form";
    }

    @PostMapping("/users/{id}/change-password")
    public String changePassword(@PathVariable Long id,
                                 @RequestParam String newPassword,
                                 @RequestParam(required = false) Long departmentId,
                                 @RequestParam(required = false) String returnTo,
                                 RedirectAttributes redirectAttributes) {
        String normalizedReturnTo = normalizeAdminReturnTo(returnTo);
        if (newPassword == null || newPassword.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "新密码不能为空");
            if (normalizedReturnTo != null) {
                return "redirect:" + normalizedReturnTo;
            }
            if (departmentId != null) {
                return "redirect:/admin/departments/" + departmentId + "/edit";
            }
            return "redirect:/admin/departments";
        }

        userService.findById(id).ifPresent(u -> {
            u.setPassword(newPassword.trim());
            userService.update(u);
        });
        redirectAttributes.addFlashAttribute("success", "密码修改成功");
        if (normalizedReturnTo != null) {
            return "redirect:" + normalizedReturnTo;
        }
        if (departmentId != null) {
            return "redirect:/admin/departments/" + departmentId + "/edit";
        }
        return "redirect:/admin/departments";
    }

    // 兼容旧模板 action：重置密码接口统一转到修改密码处理
    @PostMapping("/users/{id}/reset-password")
    public String resetPasswordLegacy(@PathVariable Long id,
                                      @RequestParam(required = false) String newPassword,
                                      @RequestParam(required = false) Long departmentId,
                                      @RequestParam(required = false) String returnTo,
                                      RedirectAttributes redirectAttributes) {
        return changePassword(id, newPassword, departmentId, returnTo, redirectAttributes);
    }

    private String normalizeAdminReturnTo(String returnTo) {
        if (returnTo == null) return null;
        String trimmed = returnTo.trim();
        if (trimmed.isEmpty()) return null;
        // avoid open redirect: only allow in-site admin paths
        if (trimmed.startsWith("/admin/")) return trimmed;
        return null;
    }

    // 患者信息管理：仅列表页面
    @GetMapping("/patients")
    public String patients(@RequestParam(required = false) String keyword,
                           Model model) {
        List<Patient> patients = patientService.findAll();
        java.util.Map<Long, User> userMap = new java.util.HashMap<>();
        for (Patient p : patients) {
            userService.findById(p.getUserId()).ifPresent(u -> userMap.put(p.getId(), u));
        }

        if (keyword != null && !keyword.isEmpty()) {
            String k = keyword.trim();
            patients = patients.stream().filter(p -> {
                User u = userMap.get(p.getId());
                boolean matchName = u != null && u.getRealName() != null && u.getRealName().contains(k);
                boolean matchIdCard = p.getIdCard() != null && p.getIdCard().contains(k);
                return matchName || matchIdCard;
            }).collect(Collectors.toList());
        }

        model.addAttribute("patients", patients);
        model.addAttribute("userMap", userMap);
        model.addAttribute("keyword", keyword);
        return "admin/patients";
    }

    // 患者详情：新页面展示该患者的挂号、报告单、处方（处方区内嵌药房窗口功能）
    @GetMapping("/patients/{id}")
    public String patientDetail(@PathVariable Long id, Model model) {
        Patient patient = patientService.findById(id).orElse(null);
        if (patient == null) {
            return "redirect:/admin/patients";
        }

        User patientUser = null;
        if (patient.getUserId() != null) {
            patientUser = userService.findById(patient.getUserId()).orElse(null);
        }

        // 该患者的挂号列表
        List<Appointment> appointments = appointmentService.findByPatientId(id);
        java.util.Map<Long, User> doctorUserMap = new java.util.HashMap<>();
        for (Appointment a : appointments) {
            if (a.getDoctorId() != null) {
                doctorService.findById(a.getDoctorId()).ifPresent(d -> {
                    if (d.getUserId() != null) {
                        userService.findById(d.getUserId()).ifPresent(u -> doctorUserMap.put(a.getId(), u));
                    }
                });
            }
        }

        // 报告单汇总
        List<Report> reports = reportService.findByPatientId(id);
        java.util.Map<Long, Examination> examinationMap = new java.util.HashMap<>();
        for (Report r : reports) {
            if (r.getExaminationId() != null) {
                examinationService.findById(r.getExaminationId()).ifPresent(exam -> examinationMap.put(r.getId(), exam));
            }
        }

        // 处方汇总
        List<Prescription> prescriptions = prescriptionService.findByPatientId(id);
        java.util.Map<Long, User> prescriptionDoctorUserMap = new java.util.HashMap<>();
        for (Prescription p : prescriptions) {
            if (p.getDoctorId() != null) {
                doctorService.findById(p.getDoctorId()).ifPresent(d -> {
                    if (d.getUserId() != null) {
                        userService.findById(d.getUserId()).ifPresent(u -> prescriptionDoctorUserMap.put(p.getId(), u));
                    }
                });
            }
        }

        // 住院记录汇总
        List<Hospitalization> hospitalizations = hospitalizationService.findByPatientId(id);
        java.util.Map<Long, User> hospitalizationDoctorUserMap = new java.util.HashMap<>();
        java.util.Map<Long, Bed> hospitalizationBedMap = new java.util.HashMap<>();
        for (Hospitalization h : hospitalizations) {
            if (h.getDoctorId() != null) {
                doctorService.findById(h.getDoctorId()).ifPresent(d -> {
                    if (d.getUserId() != null) {
                        userService.findById(d.getUserId()).ifPresent(u -> hospitalizationDoctorUserMap.put(h.getId(), u));
                    }
                });
            }
            if (h.getBedId() != null) {
                bedService.findById(h.getBedId()).ifPresent(bed -> hospitalizationBedMap.put(h.getId(), bed));
            }
        }

        model.addAttribute("patient", patient);
        model.addAttribute("patientUser", patientUser);
        model.addAttribute("appointments", appointments);
        model.addAttribute("doctorUserMap", doctorUserMap);
        model.addAttribute("reports", reports);
        model.addAttribute("examinationMap", examinationMap);
        model.addAttribute("prescriptions", prescriptions);
        model.addAttribute("prescriptionDoctorUserMap", prescriptionDoctorUserMap);
        model.addAttribute("hospitalizations", hospitalizations);
        model.addAttribute("hospitalizationDoctorUserMap", hospitalizationDoctorUserMap);
        model.addAttribute("hospitalizationBedMap", hospitalizationBedMap);
        return "admin/patient-detail";
    }

    // 患者就诊信息页：批量删除挂号
    @PostMapping("/patients/{patientId}/appointments/bulk-delete")
    public String bulkDeletePatientAppointments(@PathVariable Long patientId,
                                                @RequestParam(required = false) Long[] ids,
                                                RedirectAttributes redirectAttributes) {
        if (ids == null || ids.length == 0) {
            redirectAttributes.addFlashAttribute("error", "请至少选择一条挂号记录");
            return "redirect:/admin/patients/" + patientId;
        }
        int deleted = 0;
        for (Long id : ids) {
            try {
                appointmentService.deleteById(id);
                deleted++;
            } catch (Exception ignored) {
            }
        }
        redirectAttributes.addFlashAttribute("success", "已批量删除 " + deleted + " 条挂号记录");
        return "redirect:/admin/patients/" + patientId;
    }

    // 患者就诊信息页：批量删除报告单
    @PostMapping("/patients/{patientId}/reports/bulk-delete")
    public String bulkDeletePatientReports(@PathVariable Long patientId,
                                           @RequestParam(required = false) Long[] ids,
                                           RedirectAttributes redirectAttributes) {
        if (ids == null || ids.length == 0) {
            redirectAttributes.addFlashAttribute("error", "请至少选择一份报告单");
            return "redirect:/admin/patients/" + patientId;
        }
        int deleted = 0;
        for (Long id : ids) {
            try {
                reportService.deleteById(id);
                deleted++;
            } catch (Exception ignored) {
            }
        }
        redirectAttributes.addFlashAttribute("success", "已批量删除 " + deleted + " 份报告单");
        return "redirect:/admin/patients/" + patientId;
    }

    // 患者就诊信息页：更新单条住院状态
    @PostMapping("/patients/{patientId}/hospitalizations/{id}/status")
    public String updatePatientHospitalizationStatus(@PathVariable Long patientId,
                                                     @PathVariable Long id,
                                                     @RequestParam String status,
                                                     RedirectAttributes redirectAttributes) {
        Hospitalization hospitalization = hospitalizationService.findById(id).orElse(null);
        if (hospitalization == null || hospitalization.getPatientId() == null || !hospitalization.getPatientId().equals(patientId)) {
            redirectAttributes.addFlashAttribute("error", "住院记录不存在或不属于该患者");
            return "redirect:/admin/patients/" + patientId;
        }
        try {
            Hospitalization.HospitalizationStatus newStatus = Hospitalization.HospitalizationStatus.valueOf(status);
            hospitalization.setStatus(newStatus);
            if (newStatus == Hospitalization.HospitalizationStatus.DISCHARGED && hospitalization.getDischargeDate() == null) {
                hospitalization.setDischargeDate(LocalDate.now());
            }
            hospitalizationService.update(hospitalization);
            if (newStatus == Hospitalization.HospitalizationStatus.DISCHARGED) {
                medicalOrderService.completeActiveOrdersOnDischarge(id);
            }
            redirectAttributes.addFlashAttribute("success", "住院状态更新成功");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "住院状态值无效");
        }
        return "redirect:/admin/patients/" + patientId;
    }

    // 患者就诊信息页：批量删除住院记录
    @PostMapping("/patients/{patientId}/hospitalizations/bulk-delete")
    public String bulkDeletePatientHospitalizations(@PathVariable Long patientId,
                                                    @RequestParam(required = false) Long[] ids,
                                                    RedirectAttributes redirectAttributes) {
        if (ids == null || ids.length == 0) {
            redirectAttributes.addFlashAttribute("error", "请至少选择一条住院记录");
            return "redirect:/admin/patients/" + patientId;
        }
        int deleted = 0;
        for (Long id : ids) {
            Hospitalization hospitalization = hospitalizationService.findById(id).orElse(null);
            if (hospitalization == null || hospitalization.getPatientId() == null || !hospitalization.getPatientId().equals(patientId)) {
                continue;
            }
            try {
                // 若当前住院且占用床位，删除前释放床位
                if (hospitalization.getBedId() != null && hospitalization.getStatus() == Hospitalization.HospitalizationStatus.ADMITTED) {
                    bedService.findById(hospitalization.getBedId()).ifPresent(bed -> {
                        bed.setStatus(Bed.BedStatus.AVAILABLE);
                        bedService.update(bed);
                    });
                }
                hospitalizationService.deleteById(id);
                deleted++;
            } catch (Exception ignored) {
            }
        }
        redirectAttributes.addFlashAttribute("success", "已批量删除 " + deleted + " 条住院记录");
        return "redirect:/admin/patients/" + patientId;
    }

    // 挂号信息管理
    @GetMapping("/appointments")
    public String appointments() {
        // 已合并到 patients 页面：挂号入口统一回到患者列表
        return "redirect:/admin/patients";
    }

    @PostMapping("/appointments/{id}/status")
    public String updateAppointmentStatus(@PathVariable Long id,
                                          @RequestParam String status,
                                          @RequestParam(required = false) Long patientId) {
        appointmentService.findById(id).ifPresent(a -> {
            a.setStatus(Appointment.AppointmentStatus.valueOf(status));
            appointmentService.update(a);
        });
        if (patientId != null) {
            return "redirect:/admin/patients?selectedId=" + patientId + "#detail";
        }
        return "redirect:/admin/patients";
    }

    @PostMapping("/appointments/{id}/cancel")
    public String cancelAppointment(@PathVariable Long id,
                                    @RequestParam(required = false) Long patientId) {
        appointmentService.findById(id).ifPresent(a -> {
            a.setStatus(Appointment.AppointmentStatus.CANCELLED);
            appointmentService.update(a);
        });
        if (patientId != null) {
            return "redirect:/admin/patients?selectedId=" + patientId + "#detail";
        }
        return "redirect:/admin/patients";
    }

    @PostMapping("/appointments/bulk-delete")
    public String bulkDeleteAppointments(@RequestParam(required = false) Long[] ids,
                                         @RequestParam(required = false) Long patientId) {
        if (ids != null) {
            for (Long id : ids) {
                appointmentService.deleteById(id);
            }
        }
        if (patientId != null) {
            return "redirect:/admin/patients?selectedId=" + patientId + "#detail";
        }
        return "redirect:/admin/patients";
    }

    // 查看挂号对应的报告单
    @GetMapping("/appointments/{id}/reports")
    public String appointmentReports(@PathVariable Long id, Model model) {
        Appointment appointment = appointmentService.findById(id).orElse(null);
        if (appointment == null) {
            return "redirect:/admin/appointments";
        }

        // 加载患者和医生信息
        Patient patient = patientService.findById(appointment.getPatientId()).orElse(null);
        User patientUser = null;
        if (patient != null && patient.getUserId() != null) {
            patientUser = userService.findById(patient.getUserId()).orElse(null);
        }

        Doctor doctor = doctorService.findById(appointment.getDoctorId()).orElse(null);
        User doctorUser = null;
        if (doctor != null && doctor.getUserId() != null) {
            doctorUser = userService.findById(doctor.getUserId()).orElse(null);
        }

        // 加载该挂号对应的报告单
        List<Report> reports = reportService.findByAppointmentId(id);
        java.util.Map<Long, Examination> examinationMap = new java.util.HashMap<>();
        for (Report report : reports) {
            if (report.getExaminationId() != null) {
                examinationService.findById(report.getExaminationId()).ifPresent(exam -> {
                    examinationMap.put(report.getId(), exam);
                });
            }
        }

        model.addAttribute("appointment", appointment);
        model.addAttribute("patient", patient);
        model.addAttribute("patientUser", patientUser);
        model.addAttribute("doctor", doctor);
        model.addAttribute("doctorUser", doctorUser);
        model.addAttribute("reports", reports);
        model.addAttribute("examinationMap", examinationMap);
        return "admin/appointment-reports";
    }

    // 查看挂号对应的处方
    @GetMapping("/appointments/{id}/prescriptions")
    public String appointmentPrescriptions(@PathVariable Long id, Model model) {
        Appointment appointment = appointmentService.findById(id).orElse(null);
        if (appointment == null) {
            return "redirect:/admin/appointments";
        }

        // 加载患者和医生信息
        Patient patient = patientService.findById(appointment.getPatientId()).orElse(null);
        User patientUser = null;
        if (patient != null && patient.getUserId() != null) {
            patientUser = userService.findById(patient.getUserId()).orElse(null);
        }

        Doctor doctor = doctorService.findById(appointment.getDoctorId()).orElse(null);
        User doctorUser = null;
        if (doctor != null && doctor.getUserId() != null) {
            doctorUser = userService.findById(doctor.getUserId()).orElse(null);
        }

        // 加载该挂号对应的处方
        List<Prescription> prescriptions = prescriptionService.findByAppointmentId(id);
        java.util.Map<Long, List<PrescriptionItem>> itemsMap = new java.util.HashMap<>();
        java.util.Map<Long, java.math.BigDecimal> totalAmountMap = new java.util.HashMap<>();
        java.util.Map<Long, User> prescriptionPatientUserMap = new java.util.HashMap<>();
        java.util.Map<Long, User> prescriptionDoctorUserMap = new java.util.HashMap<>();

        for (Prescription prescription : prescriptions) {
            // 加载处方明细
            List<PrescriptionItem> items = prescriptionService.getPrescriptionItems(prescription.getId());
            itemsMap.put(prescription.getId(), items);

            // 计算总金额
            java.math.BigDecimal totalAmount = items.stream()
                    .map(item -> item.getTotalPrice() != null ? item.getTotalPrice() : java.math.BigDecimal.ZERO)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            totalAmountMap.put(prescription.getId(), totalAmount);

            // 加载患者和医生信息
            if (prescription.getPatientId() != null) {
                patientService.findById(prescription.getPatientId()).ifPresent(p -> {
                    if (p.getUserId() != null) {
                        userService.findById(p.getUserId()).ifPresent(u -> {
                            prescriptionPatientUserMap.put(prescription.getId(), u);
                        });
                    }
                });
            }
            if (prescription.getDoctorId() != null) {
                doctorService.findById(prescription.getDoctorId()).ifPresent(d -> {
                    if (d.getUserId() != null) {
                        userService.findById(d.getUserId()).ifPresent(u -> {
                            prescriptionDoctorUserMap.put(prescription.getId(), u);
                        });
                    }
                });
            }
        }

        model.addAttribute("appointment", appointment);
        model.addAttribute("patient", patient);
        model.addAttribute("patientUser", patientUser);
        model.addAttribute("doctor", doctor);
        model.addAttribute("doctorUser", doctorUser);
        model.addAttribute("prescriptions", prescriptions);
        model.addAttribute("itemsMap", itemsMap);
        model.addAttribute("totalAmountMap", totalAmountMap);
        model.addAttribute("prescriptionPatientUserMap", prescriptionPatientUserMap);
        model.addAttribute("prescriptionDoctorUserMap", prescriptionDoctorUserMap);
        return "admin/appointment-prescriptions";
    }

    // 药物信息管理
    @GetMapping("/medicines")
    public String medicines(Model model) {
        List<Medicine> medicines = medicineService.findAll();
        model.addAttribute("medicines", medicines);
        return "admin/medicines";
    }

    @GetMapping("/medicines/new")
    public String newMedicine(Model model) {
        model.addAttribute("medicine", new Medicine());
        model.addAttribute("departments", departmentService.findAll());
        return "admin/medicine-form";
    }

    @PostMapping("/medicines")
    public String createMedicine(@ModelAttribute Medicine medicine,
                                 @RequestParam(required = false) Long departmentId,
                                 RedirectAttributes redirectAttributes) {
        try {
            medicine.setDepartmentId(departmentId);
            if (medicine.getStatus() == null) medicine.setStatus(1);
            if (medicine.getStock() == null) medicine.setStock(0);
            medicineService.save(medicine);
            redirectAttributes.addFlashAttribute("success", "药品创建成功！");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "创建失败：" + e.getMessage());
        }
        return "redirect:/admin/medicines";
    }

    @GetMapping("/medicines/{id}/edit")
    public String editMedicine(@PathVariable Long id, Model model) {
        model.addAttribute("medicine", medicineService.findById(id).orElse(new Medicine()));
        model.addAttribute("departments", departmentService.findAll());
        return "admin/medicine-form";
    }

    @PostMapping("/medicines/{id}")
    public String updateMedicine(@PathVariable Long id,
                                @ModelAttribute Medicine medicine,
                                @RequestParam(required = false) Long departmentId,
                                RedirectAttributes redirectAttributes) {
        try {
            medicine.setId(id);
            medicine.setDepartmentId(departmentId);
            if (medicine.getStatus() == null) medicine.setStatus(1);

            // 确保库存不为null
            if (medicine.getStock() == null) {
                Medicine existing = medicineService.findById(id).orElse(null);
                if (existing != null) {
                    medicine.setStock(existing.getStock());
                } else {
                    medicine.setStock(0);
                }
            }

            medicineService.update(medicine);
            redirectAttributes.addFlashAttribute("success", "药品信息更新成功！");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "更新失败：" + e.getMessage());
        }
        return "redirect:/admin/medicines";
    }

    @PostMapping("/medicines/{id}/delete")
    public String deleteMedicine(@PathVariable Long id) {
        medicineService.deleteById(id);
        return "redirect:/admin/medicines";
    }

    @PostMapping("/medicines/bulk-delete")
    public String bulkDeleteMedicines(@RequestParam(required = false) Long[] ids,
                                      RedirectAttributes redirectAttributes) {
        if (ids == null || ids.length == 0) {
            redirectAttributes.addFlashAttribute("error", "请至少选择一个药品");
            return "redirect:/admin/medicines";
        }
        int deleted = 0;
        for (Long id : ids) {
            try {
                medicineService.deleteById(id);
                deleted++;
            } catch (Exception ignored) {
            }
        }
        // 全部清空后确保主键自增从 1 开始（MySQL DELETE 不会自动重置 AUTO_INCREMENT）
        medicineService.resetAutoIncrementIfEmpty();
        redirectAttributes.addFlashAttribute("success", "已批量删除 " + deleted + " 个药品");
        return "redirect:/admin/medicines";
    }

    // 药品入库管理
    @GetMapping("/medicines/{id}/inbound")
    public String inboundMedicineForm(@PathVariable Long id, Model model) {
        Medicine medicine = medicineService.findById(id).orElse(null);
        if (medicine == null) {
            return "redirect:/admin/medicines";
        }
        model.addAttribute("medicine", medicine);
        model.addAttribute("inbound", new MedicineInbound());
        return "admin/medicine-inbound-form";
    }

    @PostMapping("/medicines/{id}/inbound")
    public String submitInbound(@PathVariable Long id,
                               @RequestParam String batchNumber,
                               @RequestParam(required = false) LocalDate productionDate,
                               @RequestParam LocalDate expiryDate,
                               @RequestParam Integer quantity,
                               @RequestParam java.math.BigDecimal purchasePrice,
                               @RequestParam(required = false, defaultValue = "药房") String location,
                               @RequestParam(required = false) String operator,
                               RedirectAttributes redirectAttributes) {
        try {
            Medicine medicine = medicineService.findById(id).orElse(null);
            if (medicine == null) {
                redirectAttributes.addFlashAttribute("error", "药品不存在");
                return "redirect:/admin/medicines";
            }

            MedicineInbound inbound = new MedicineInbound();
            inbound.setMedicineId(id);
            inbound.setBatchNumber(batchNumber);
            inbound.setProductionDate(productionDate);
            inbound.setExpiryDate(expiryDate);
            inbound.setQuantity(quantity);
            inbound.setPurchasePrice(purchasePrice);
            inbound.setLocation(location);
            inbound.setOperator(operator);
            inbound.setStatus(0); // 先保存为待审核状态

            // 保存入库记录
            String currentOperator = operator != null && !operator.trim().isEmpty() ? operator : "系统";
            medicineInboundService.receive(inbound, currentOperator);

            redirectAttributes.addFlashAttribute("success", "药品入库成功！库存已更新。");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "入库失败：" + e.getMessage());
        }
        return "redirect:/admin/medicines";
    }

    // 入库记录列表
    @GetMapping("/medicines/inbounds")
    public String inboundList(Model model) {
        List<MedicineInbound> inbounds = medicineInboundService.findAll();

        // 加载药品信息
        java.util.Map<Long, Medicine> medicineMap = new java.util.HashMap<>();
        for (MedicineInbound inbound : inbounds) {
            medicineService.findById(inbound.getMedicineId()).ifPresent(med -> {
                medicineMap.put(inbound.getMedicineId(), med);
            });
        }

        model.addAttribute("inbounds", inbounds);
        model.addAttribute("medicineMap", medicineMap);
        return "admin/medicine-inbounds";
    }

    // 检查项目管理
    @GetMapping("/examinations")
    public String examinations(Model model) {
        model.addAttribute("examinations", examinationService.findAll());
        model.addAttribute("departments", departmentService.findAll());
        return "admin/examinations";
    }

    @GetMapping("/examinations/new")
    public String newExamination(Model model) {
        model.addAttribute("examination", new Examination());
        model.addAttribute("departments", departmentService.findAll());
        return "admin/examination-form";
    }

    @PostMapping("/examinations")
    public String createExamination(@ModelAttribute Examination examination,
                                    @RequestParam Long departmentId) {
        examination.setDepartmentId(departmentId);
        if (examination.getStatus() == null) examination.setStatus(1);
        examinationService.save(examination);
        return "redirect:/admin/examinations";
    }

    @GetMapping("/examinations/{id}/edit")
    public String editExamination(@PathVariable Long id, Model model) {
        model.addAttribute("examination", examinationService.findById(id).orElse(new Examination()));
        model.addAttribute("departments", departmentService.findAll());
        return "admin/examination-form";
    }

    @PostMapping("/examinations/{id}")
    public String updateExamination(@PathVariable Long id,
                                    @ModelAttribute Examination examination,
                                    @RequestParam Long departmentId) {
        examination.setId(id);
        examination.setDepartmentId(departmentId);
        if (examination.getStatus() == null) examination.setStatus(1);
        examinationService.update(examination);
        return "redirect:/admin/examinations";
    }

    @PostMapping("/examinations/{id}/delete")
    public String deleteExamination(@PathVariable Long id) {
        examinationService.deleteById(id);
        return "redirect:/admin/examinations";
    }

    @PostMapping("/examinations/bulk-delete")
    public String bulkDeleteExaminations(@RequestParam(required = false) Long[] ids,
                                         RedirectAttributes redirectAttributes) {
        if (ids == null || ids.length == 0) {
            redirectAttributes.addFlashAttribute("error", "请至少选择一个检查项");
            return "redirect:/admin/examinations";
        }
        for (Long id : ids) {
            examinationService.deleteById(id);
        }
        examinationService.resetAutoIncrementIfEmpty();
        redirectAttributes.addFlashAttribute("success", "已批量删除 " + ids.length + " 个检查项");
        return "redirect:/admin/examinations";
    }

    // 病床信息管理
    @GetMapping("/beds")
    public String beds(Model model) {
        List<Bed> allBeds = bedService.findAll();

        // 按科室分组
        java.util.Map<String, List<Bed>> bedsByDepartment = new java.util.HashMap<>();
        java.util.Map<Long, Department> departmentMap = new java.util.HashMap<>();

        // 加载所有科室信息
        departmentService.findAll().forEach(dept -> {
            departmentMap.put(dept.getId(), dept);
        });

        // 按科室分组床位
        for (Bed bed : allBeds) {
            String deptKey;
            if (bed.getDepartmentId() != null && departmentMap.containsKey(bed.getDepartmentId())) {
                deptKey = departmentMap.get(bed.getDepartmentId()).getName();
            } else {
                deptKey = "未分配科室";
            }
            bedsByDepartment.computeIfAbsent(deptKey, k -> new java.util.ArrayList<>()).add(bed);
        }

        // 加载每个床位占用的患者信息（当前住院中）
        java.util.Map<Long, Hospitalization> bedHospitalizationMap = new java.util.HashMap<>();
        java.util.Map<Long, Patient> patientMap = new java.util.HashMap<>();
        java.util.Map<Long, User> userMap = new java.util.HashMap<>();

        // 加载每个病床的当前住院记录（仅 ADMITTED，不含历史记录）
        java.util.Map<Long, List<Hospitalization>> bedAllHospitalizationsMap = new java.util.HashMap<>();
        java.util.Map<Long, User> doctorUserMap = new java.util.HashMap<>();

        for (Bed bed : allBeds) {
            // 当前占用的住院记录
            if (bed.getStatus() == Bed.BedStatus.OCCUPIED) {
                hospitalizationService.findAll().stream()
                        .filter(h -> h.getBedId() != null && h.getBedId().equals(bed.getId())
                                && h.getStatus() == Hospitalization.HospitalizationStatus.ADMITTED)
                        .findFirst()
                        .ifPresent(hosp -> {
                            bedHospitalizationMap.put(bed.getId(), hosp);
                            patientService.findById(hosp.getPatientId()).ifPresent(patient -> {
                                patientMap.put(hosp.getPatientId(), patient);
                                userService.findById(patient.getUserId()).ifPresent(user -> {
                                    userMap.put(hosp.getPatientId(), user);
                                });
                            });
                        });
            }

            // 该病床的当前住院记录（仅住院中）
            List<Hospitalization> bedHospitalizations = hospitalizationService.findAll().stream()
                    .filter(h -> h.getBedId() != null && h.getBedId().equals(bed.getId()))
                    .filter(h -> h.getStatus() == Hospitalization.HospitalizationStatus.ADMITTED)
                    .sorted((h1, h2) -> {
                        if (h1.getAdmissionDate() != null && h2.getAdmissionDate() != null) {
                            return h2.getAdmissionDate().compareTo(h1.getAdmissionDate());
                        }
                        return 0;
                    })
                    .collect(Collectors.toList());
            bedAllHospitalizationsMap.put(bed.getId(), bedHospitalizations);

            // 加载医生信息
            for (Hospitalization hosp : bedHospitalizations) {
                if (hosp.getDoctorId() != null) {
                    doctorService.findById(hosp.getDoctorId()).ifPresent(doctor -> {
                        userService.findById(doctor.getUserId()).ifPresent(user -> {
                            doctorUserMap.put(hosp.getId(), user);
                        });
                    });
                }
                // 加载患者信息
                patientService.findById(hosp.getPatientId()).ifPresent(patient -> {
                    patientMap.put(hosp.getPatientId(), patient);
                    userService.findById(patient.getUserId()).ifPresent(user -> {
                        userMap.put(hosp.getPatientId(), user);
                    });
                });
            }
        }

        // 加载待审批的住院申请
        List<Hospitalization> pendingRequests = hospitalizationService.findAll().stream()
                .filter(h -> h.getRequestStatus() == Hospitalization.RequestStatus.PENDING)
                .collect(Collectors.toList());

        java.util.Map<Long, Patient> requestPatientMap = new java.util.HashMap<>();
        java.util.Map<Long, User> requestUserMap = new java.util.HashMap<>();
        for (Hospitalization req : pendingRequests) {
            patientService.findById(req.getPatientId()).ifPresent(patient -> {
                requestPatientMap.put(req.getPatientId(), patient);
                userService.findById(patient.getUserId()).ifPresent(user -> {
                    requestUserMap.put(req.getPatientId(), user);
                });
            });
        }

        model.addAttribute("beds", allBeds);
        model.addAttribute("bedsByDepartment", bedsByDepartment);
        model.addAttribute("departmentMap", departmentMap);
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("bedHospitalizationMap", bedHospitalizationMap);
        model.addAttribute("bedAllHospitalizationsMap", bedAllHospitalizationsMap);
        model.addAttribute("patientMap", patientMap);
        model.addAttribute("userMap", userMap);
        model.addAttribute("doctorUserMap", doctorUserMap);
        model.addAttribute("pendingRequests", pendingRequests);
        model.addAttribute("requestPatientMap", requestPatientMap);
        model.addAttribute("requestUserMap", requestUserMap);
        return "admin/beds";
    }

    // 审核住院申请（分配床位并审核通过）
    @PostMapping("/beds/assign")
    public String assignBed(@RequestParam Long bedId, @RequestParam Long hospitalizationId) {
        hospitalizationService.assignBed(hospitalizationId, bedId);
        return "redirect:/admin/beds";
    }

    // 审核住院申请（拒绝）
    @PostMapping("/beds/reject")
    @org.springframework.transaction.annotation.Transactional
    public String rejectBedAssignment(@RequestParam Long hospitalizationId, RedirectAttributes redirectAttributes) {
        Hospitalization hospitalization = hospitalizationService.lock(hospitalizationId);
        if (hospitalization.getRequestStatus() == Hospitalization.RequestStatus.APPROVED
                || hospitalization.getStatus() != Hospitalization.HospitalizationStatus.ADMITTED) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "不能拒绝已生效或已出院的住院记录");
        }
        if (hospitalization == null) {
            redirectAttributes.addFlashAttribute("error", "住院申请不存在");
            return "redirect:/admin/beds";
        }

        // 如果医生已预留床位（OCCUPIED），拒绝时释放
        if (hospitalization.getBedId() != null) {
            Bed bed = bedService.findById(hospitalization.getBedId()).orElse(null);
            if (bed != null && bed.getStatus() == Bed.BedStatus.OCCUPIED) {
                bed.setStatus(Bed.BedStatus.AVAILABLE);
                bedService.update(bed);
            }
        }

        hospitalization.setRequestStatus(Hospitalization.RequestStatus.REJECTED);
        hospitalizationService.update(hospitalization);
        redirectAttributes.addFlashAttribute("success", "已拒绝该住院申请");
        return "redirect:/admin/beds";
    }

    @GetMapping("/beds/new")
    public String newBed(Model model) {
        model.addAttribute("bed", new Bed());
        model.addAttribute("departments", departmentService.findAll());
        return "admin/bed-form";
    }

    @PostMapping("/beds")
    public String createBed(@ModelAttribute Bed bed) {
        if (bed.getStatus() == null) bed.setStatus(Bed.BedStatus.AVAILABLE);
        if (bed.getBedType() == null) bed.setBedType(Bed.BedType.GENERAL);
        bedService.save(bed);
        return "redirect:/admin/beds";
    }

    @GetMapping("/beds/{id}/edit")
    public String editBed(@PathVariable Long id, Model model) {
        model.addAttribute("bed", bedService.findById(id).orElse(new Bed()));
        model.addAttribute("departments", departmentService.findAll());
        return "admin/bed-form";
    }

    @PostMapping("/beds/{id}")
    public String updateBed(@PathVariable Long id, @ModelAttribute Bed bed) {
        bed.setId(id);
        bedService.update(bed);
        return "redirect:/admin/beds";
    }

    @PostMapping("/beds/{id}/delete")
    public String deleteBed(@PathVariable Long id) {
        bedService.deleteById(id);
        return "redirect:/admin/beds";
    }

    @PostMapping("/beds/bulk-delete")
    public String bulkDeleteBeds(@RequestParam(required = false) Long[] ids,
                                 RedirectAttributes redirectAttributes) {
        if (ids == null || ids.length == 0) {
            redirectAttributes.addFlashAttribute("error", "请至少选择一个床位");
            return "redirect:/admin/beds";
        }
        int deleted = 0;
        int rejected = 0;
        for (Long id : ids) {
            try {
                bedService.deleteById(id);
                deleted++;
            } catch (Exception ignored) {
                rejected++;
            }
        }
        if (rejected > 0) {
            redirectAttributes.addFlashAttribute("error", "已删除 " + deleted + " 个床位；" + rejected + " 个不存在或正在使用，未删除");
            return "redirect:/admin/beds";
        }
        redirectAttributes.addFlashAttribute("success", "已批量删除 " + deleted + " 个床位");
        return "redirect:/admin/beds";
    }

    // 住院管理
    @GetMapping("/hospitalizations")
    public String hospitalizations(Model model) {
        List<Hospitalization> allHospitalizations = hospitalizationService.findAll();

        // 加载患者和医生信息
        java.util.Map<Long, Patient> patientMap = new java.util.HashMap<>();
        java.util.Map<Long, User> patientUserMap = new java.util.HashMap<>();
        java.util.Map<Long, User> doctorUserMap = new java.util.HashMap<>();
        java.util.Map<Long, Bed> bedMap = new java.util.HashMap<>();

        for (Hospitalization hosp : allHospitalizations) {
            patientService.findById(hosp.getPatientId()).ifPresent(patient -> {
                patientMap.put(hosp.getPatientId(), patient);
                userService.findById(patient.getUserId()).ifPresent(user -> {
                    patientUserMap.put(hosp.getPatientId(), user);
                });
            });
            if (hosp.getDoctorId() != null) {
                doctorService.findById(hosp.getDoctorId()).ifPresent(doctor -> {
                    userService.findById(doctor.getUserId()).ifPresent(user -> {
                        doctorUserMap.put(hosp.getId(), user);
                    });
                });
            }
            if (hosp.getBedId() != null) {
                bedService.findById(hosp.getBedId()).ifPresent(bed -> {
                    bedMap.put(hosp.getBedId(), bed);
                });
            }
        }

        // 加载待审批的住院申请（用于在住院管理页面展示审核入口）
        List<Hospitalization> pendingRequests = allHospitalizations.stream()
                .filter(h -> h.getRequestStatus() == Hospitalization.RequestStatus.PENDING)
                .collect(Collectors.toList());

        java.util.Map<Long, Patient> requestPatientMap = new java.util.HashMap<>();
        java.util.Map<Long, User> requestUserMap = new java.util.HashMap<>();
        for (Hospitalization req : pendingRequests) {
            patientService.findById(req.getPatientId()).ifPresent(patient -> {
                requestPatientMap.put(req.getPatientId(), patient);
                userService.findById(patient.getUserId()).ifPresent(user -> {
                    requestUserMap.put(req.getPatientId(), user);
                });
            });
        }

        model.addAttribute("hospitalizations", allHospitalizations);
        model.addAttribute("patientMap", patientMap);
        model.addAttribute("patientUserMap", patientUserMap);
        model.addAttribute("doctorUserMap", doctorUserMap);
        model.addAttribute("bedMap", bedMap);
        model.addAttribute("pendingRequests", pendingRequests);
        model.addAttribute("requestPatientMap", requestPatientMap);
        model.addAttribute("requestUserMap", requestUserMap);
        return "admin/hospitalizations";
    }

    // 出院结算
    @GetMapping("/hospitalizations/{id}/settle")
    public String settleForm(@PathVariable Long id, Model model) {
        Hospitalization hospitalization = hospitalizationService.findById(id).orElseThrow(() ->
                new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "住院记录不存在"));
        if (hospitalization.getStatus() != Hospitalization.HospitalizationStatus.DISCHARGED) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "患者尚未出院");
        }
        Patient patient = patientService.findById(hospitalization.getPatientId()).orElse(null);
        model.addAttribute("hospitalization", hospitalization);
        model.addAttribute("patient", patient);
        model.addAttribute("patientUser", patient == null ? null : userService.findById(patient.getUserId()).orElse(null));
        model.addAttribute("bed", hospitalization.getBedId() == null ? null : bedService.findById(hospitalization.getBedId()).orElse(null));
        model.addAllAttributes(hospitalizationService.settlementDetails(id));
        return "admin/settle-form";
    }

    @PostMapping("/hospitalizations/{id}/settle")
    public String submitSettle(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Hospitalization hospitalization = hospitalizationService.findById(id).orElse(null);
        if (hospitalization == null || hospitalization.getStatus() != Hospitalization.HospitalizationStatus.DISCHARGED) {
            redirectAttributes.addFlashAttribute("error", "只能对已出院的患者进行结算");
            return "redirect:/admin/beds";
        }

        // 与医生出院后自动结算共用同一逻辑
        hospitalizationService.settleAndReleaseBedAfterDischarge(id);

        redirectAttributes.addFlashAttribute("success", "结算完成，床位已释放");
        return "redirect:/admin/beds";
    }

    // 删除住院记录
    @PostMapping("/hospitalizations/{id}/delete")
    public String deleteHospitalization(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Hospitalization hospitalization = hospitalizationService.findById(id).orElse(null);
        if (hospitalization == null) {
            redirectAttributes.addFlashAttribute("error", "住院记录不存在");
            return "redirect:/admin/beds";
        }

        // 如果住院中，需要先释放床位
        if (hospitalization.getBedId() != null && hospitalization.getStatus() == Hospitalization.HospitalizationStatus.ADMITTED) {
            bedService.findById(hospitalization.getBedId()).ifPresent(bed -> {
                bed.setStatus(Bed.BedStatus.AVAILABLE);
                bedService.update(bed);
            });
        }

        hospitalizationService.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "住院记录已删除");
        return "redirect:/admin/beds";
    }

    // 批量删除住院记录
    @PostMapping("/hospitalizations/bulk-delete")
    public String bulkDeleteHospitalizations(@RequestParam(required = false) Long[] ids, RedirectAttributes redirectAttributes) {
        if (ids == null || ids.length == 0) {
            redirectAttributes.addFlashAttribute("error", "请至少选择一个住院记录");
            return "redirect:/admin/beds";
        }

        int deletedCount = 0;
        for (Long id : ids) {
            Hospitalization hospitalization = hospitalizationService.findById(id).orElse(null);
            if (hospitalization != null) {
                // 如果住院中，需要先释放床位
                if (hospitalization.getBedId() != null && hospitalization.getStatus() == Hospitalization.HospitalizationStatus.ADMITTED) {
                    bedService.findById(hospitalization.getBedId()).ifPresent(bed -> {
                        bed.setStatus(Bed.BedStatus.AVAILABLE);
                        bedService.update(bed);
                    });
                }
                hospitalizationService.deleteById(id);
                deletedCount++;
            }
        }

        redirectAttributes.addFlashAttribute("success", "已成功删除 " + deletedCount + " 条住院记录");
        return "redirect:/admin/beds";
    }

    // 排班信息管理
    @GetMapping("/schedules")
    public String schedules(Model model) {
        List<Schedule> schedules = scheduleService.findAll();
        // 计算每个排班已预约人数（基于 appointment.scheduleId，排除已取消）
        List<Appointment> allAppointments = appointmentService.findAll();
        for (Schedule s : schedules) {
            if (s.getId() != null) {
                long count = allAppointments.stream()
                        .filter(a -> a.getScheduleId() != null
                                && a.getScheduleId().equals(s.getId())
                                && a.getStatus() != Appointment.AppointmentStatus.CANCELLED)
                        .count();
                s.setCurrentAppointments((int) count);
            }
        }
        model.addAttribute("schedules", schedules);
        model.addAttribute("doctors", doctorService.findAll());
        model.addAttribute("departments", departmentService.findAll());
        return "admin/schedules";
    }

    @GetMapping("/schedules/new")
    public String newSchedule(@RequestParam(required = false) Long doctorId,
                             @RequestParam(required = false) Long departmentId,
                             Model model) {
        model.addAttribute("schedule", new Schedule());
        model.addAttribute("doctors", doctorService.findAll());
        model.addAttribute("departments", departmentService.findAll());
        if (doctorId != null) {
            model.addAttribute("selectedDoctorId", doctorId);
        }
        if (departmentId != null) {
            model.addAttribute("selectedDepartmentId", departmentId);
        }
        return "admin/schedule-form";
    }

    @PostMapping("/schedules")
    @org.springframework.transaction.annotation.Transactional
    public String createSchedule(@RequestParam Long doctorId,
                                 @RequestParam Long departmentId,
                                 @RequestParam String startDate,
                                 @RequestParam String endDate,
                                 @RequestParam String workTime,
                                 @RequestParam Integer maxAppointments,
                                 @RequestParam(required = false, defaultValue = "0") Integer expertMaxAppointments,
                                 RedirectAttributes redirectAttributes) {
        BusinessValidation.stock(maxAppointments, "普通号源");
        BusinessValidation.stock(expertMaxAppointments, "专家号源");
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        BusinessValidation.require(!end.isBefore(start), "结束日期不能早于开始日期");
        BusinessValidation.require(java.time.temporal.ChronoUnit.DAYS.between(start, end) <= 365, "一次排班范围不能超过366天");

        Schedule first = new Schedule();
        first.setWorkDate(start);
        first.setWorkTime(Schedule.WorkTime.valueOf(workTime));
        first.setStatus(1);
        if (first.getDisplayStatus() == Schedule.DisplayStatus.ENDED) {
            redirectAttributes.addFlashAttribute("error", "不能创建已结束的班次，请调整开始日期或班次。");
            return "redirect:/admin/schedules/new";
        }

        int created = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            Schedule schedule = new Schedule();
            schedule.setDoctorId(doctorId);
            schedule.setDepartmentId(departmentId);
            schedule.setWorkDate(d);
            schedule.setWorkTime(Schedule.WorkTime.valueOf(workTime));
            schedule.setMaxAppointments(maxAppointments);
            schedule.setCurrentAppointments(0);
            schedule.setStatus(1);
            schedule.setScheduleType(Schedule.ScheduleType.GENERAL);
            scheduleService.save(schedule);
            created++;

            if (expertMaxAppointments != null && expertMaxAppointments > 0) {
                Schedule expertSchedule = new Schedule();
                expertSchedule.setDoctorId(doctorId);
                expertSchedule.setDepartmentId(departmentId);
                expertSchedule.setWorkDate(d);
                expertSchedule.setWorkTime(Schedule.WorkTime.valueOf(workTime));
                expertSchedule.setMaxAppointments(expertMaxAppointments);
                expertSchedule.setCurrentAppointments(0);
                expertSchedule.setStatus(1);
                expertSchedule.setScheduleType(Schedule.ScheduleType.EXPERT);
                scheduleService.save(expertSchedule);
                created++;
            }
        }

        redirectAttributes.addFlashAttribute("success", "排班创建成功，共生成 " + created + " 条排班记录。");
        return "redirect:/admin/departments/" + departmentId + "/doctors/" + doctorId + "/schedules";
    }

    @PostMapping("/schedules/{id}/delete")
    public String deleteSchedule(@PathVariable Long id,
                                @RequestParam(required = false) Long departmentId,
                                @RequestParam(required = false) Long doctorId,
                                RedirectAttributes redirectAttributes) {
        Schedule schedule = scheduleService.findById(id).orElse(null);
        scheduleService.deleteById(id);

        redirectAttributes.addFlashAttribute("success", "排班删除成功！");

        // 如果是从医生排班页面跳转过来的，返回医生排班页面
        if (departmentId != null && doctorId != null && schedule != null) {
            return "redirect:/admin/departments/" + departmentId + "/doctors/" + doctorId + "/schedules";
        }
        return "redirect:/admin/schedules";
    }

    @PostMapping("/schedules/bulk-delete")
    public String bulkDeleteSchedules(@RequestParam(required = false) Long[] ids,
                                      @RequestParam(required = false) Long departmentId,
                                      @RequestParam(required = false) Long doctorId,
                                      RedirectAttributes redirectAttributes) {
        if (ids == null || ids.length == 0) {
            redirectAttributes.addFlashAttribute("error", "请至少选择一条排班");
            if (departmentId != null && doctorId != null) {
                return "redirect:/admin/departments/" + departmentId + "/doctors/" + doctorId + "/schedules";
            }
            return "redirect:/admin/schedules";
        }
        for (Long id : ids) {
            scheduleService.deleteById(id);
        }
        redirectAttributes.addFlashAttribute("success", "已批量删除 " + ids.length + " 条排班");
        if (departmentId != null && doctorId != null) {
            return "redirect:/admin/departments/" + departmentId + "/doctors/" + doctorId + "/schedules";
        }
        return "redirect:/admin/schedules";
    }

    private boolean transitionScheduleStatus(Long id, int expectedStatus, int nextStatus, RedirectAttributes attributes) {
        Schedule schedule = scheduleService.findById(id).orElse(null);
        if (schedule == null) {
            attributes.addFlashAttribute("error", "排班不存在。");
            return false;
        }
        if (schedule.getDisplayStatus() == Schedule.DisplayStatus.ENDED) {
            attributes.addFlashAttribute("error", "该班次已结束，不能审批请假或恢复出诊。");
            return false;
        }
        if (schedule.getDisplayStatus() == Schedule.DisplayStatus.UNKNOWN || !Integer.valueOf(expectedStatus).equals(schedule.getStatus())) {
            attributes.addFlashAttribute("error", "排班状态已变更，请刷新后重试。");
            return false;
        }
        schedule.setStatus(nextStatus);
        scheduleService.update(schedule);
        return true;
    }

    // 审批医生请假：同意请假 -> 将排班改为停用状态
    @PostMapping("/schedules/{id}/approve-leave")
    public String approveScheduleLeave(@PathVariable Long id,
                                       @RequestParam(required = false) Long departmentId,
                                       @RequestParam(required = false) Long doctorId,
                                       RedirectAttributes redirectAttributes) {
        if (transitionScheduleStatus(id, 2, 0, redirectAttributes)) {
            redirectAttributes.addFlashAttribute("success", "已同意该排班的请假申请，并将排班设为停用。");
        }
        if (departmentId != null && doctorId != null) {
            return "redirect:/admin/departments/" + departmentId + "/doctors/" + doctorId + "/schedules";
        }
        return "redirect:/admin/schedules";
    }

    // 审批医生请假：驳回请假 -> 维持/恢复为启用状态
    @PostMapping("/schedules/{id}/reject-leave")
    public String rejectScheduleLeave(@PathVariable Long id,
                                      @RequestParam(required = false) Long departmentId,
                                      @RequestParam(required = false) Long doctorId,
                                      RedirectAttributes redirectAttributes) {
        if (transitionScheduleStatus(id, 2, 1, redirectAttributes)) {
            redirectAttributes.addFlashAttribute("success", "已驳回该排班的请假申请，排班保持启用。");
        }
        if (departmentId != null && doctorId != null) {
            return "redirect:/admin/departments/" + departmentId + "/doctors/" + doctorId + "/schedules";
        }
        return "redirect:/admin/schedules";
    }

    // 医生复工：将停用排班改回启用状态
    @PostMapping("/schedules/{id}/restore")
    public String restoreSchedule(@PathVariable Long id,
                                  @RequestParam(required = false) Long departmentId,
                                  @RequestParam(required = false) Long doctorId,
                                  RedirectAttributes redirectAttributes) {
        if (transitionScheduleStatus(id, 0, 1, redirectAttributes)) {
            redirectAttributes.addFlashAttribute("success", "该排班已复工，状态恢复为启用。");
        }
        if (departmentId != null && doctorId != null) {
            return "redirect:/admin/departments/" + departmentId + "/doctors/" + doctorId + "/schedules";
        }
        return "redirect:/admin/schedules";
    }

    // 报告单管理
    @GetMapping("/reports")
    public String reports(Model model) {
        List<Report> reports = reportService.findAll();

        // 按时间排序（最新的在前）
        reports = reports.stream()
                .sorted((r1, r2) -> {
                    if (r1.getReportDate() == null && r2.getReportDate() == null) return 0;
                    if (r1.getReportDate() == null) return 1;
                    if (r2.getReportDate() == null) return -1;
                    return r2.getReportDate().compareTo(r1.getReportDate());
                })
                .collect(Collectors.toList());

        // 加载患者、医生、检查项目信息
        java.util.Map<Long, Patient> patientMap = new java.util.HashMap<>();
        java.util.Map<Long, User> patientUserMap = new java.util.HashMap<>();
        java.util.Map<Long, Doctor> doctorMap = new java.util.HashMap<>();
        java.util.Map<Long, User> doctorUserMap = new java.util.HashMap<>();
        java.util.Map<Long, Examination> examinationMap = new java.util.HashMap<>();

        for (Report report : reports) {
            patientService.findById(report.getPatientId()).ifPresent(patient -> {
                patientMap.put(report.getPatientId(), patient);
                userService.findById(patient.getUserId()).ifPresent(user -> {
                    patientUserMap.put(report.getPatientId(), user);
                });
            });
            if (report.getDoctorId() != null) {
                doctorService.findById(report.getDoctorId()).ifPresent(doctor -> {
                    doctorMap.put(report.getDoctorId(), doctor);
                    userService.findById(doctor.getUserId()).ifPresent(user -> {
                        doctorUserMap.put(report.getDoctorId(), user);
                    });
                });
            }
            if (report.getExaminationId() != null) {
                examinationService.findById(report.getExaminationId()).ifPresent(exam -> {
                    examinationMap.put(report.getId(), exam);
                });
            }
        }

        model.addAttribute("reports", reports);
        model.addAttribute("patientMap", patientMap);
        model.addAttribute("patientUserMap", patientUserMap);
        model.addAttribute("doctorMap", doctorMap);
        model.addAttribute("doctorUserMap", doctorUserMap);
        model.addAttribute("examinationMap", examinationMap);
        return "admin/reports";
    }

    @PostMapping("/reports/{id}/delete")
    public String deleteReport(@PathVariable Long id) {
        reportService.deleteById(id);
        return "redirect:/admin/reports";
    }

    @PostMapping("/reports/bulk-delete")
    public String bulkDeleteReports(@RequestParam(required = false) Long[] ids) {
        if (ids != null) {
            for (Long id : ids) {
                reportService.deleteById(id);
            }
        }
        return "redirect:/admin/reports";
    }

    // 管理员查看报告单详情
    @GetMapping("/reports/{id}/preview")
    public String previewReport(@PathVariable Long id,
                                @RequestParam(required = false) Long returnPatientId,
                                Model model) {
        Report report = reportService.findById(id).orElse(null);
        if (report == null) {
            return "redirect:/admin/reports";
        }
        if (returnPatientId != null && (report.getPatientId() == null || !returnPatientId.equals(report.getPatientId()))) {
            returnPatientId = null;
        }
        model.addAttribute("adminBackUrl", returnPatientId != null
                ? "/admin/patients/" + returnPatientId
                : "/admin/reports");

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

        // 加载该报告所属挂号的处方列表（用于在预览页下方展示）
        List<Prescription> prescriptions = null;
        java.util.Map<Long, java.util.List<PrescriptionItem>> itemsMap = new java.util.HashMap<>();
        java.util.Map<Long, java.math.BigDecimal> totalAmountMap = new java.util.HashMap<>();
        if (report.getAppointmentId() != null) {
            prescriptions = prescriptionService.findByAppointmentId(report.getAppointmentId());
            for (Prescription p : prescriptions) {
                // 明细
                java.util.List<PrescriptionItem> items = prescriptionService.getPrescriptionItems(p.getId());
                itemsMap.put(p.getId(), items);

                // 合计
                java.math.BigDecimal totalAmount = items.stream()
                        .map(item -> item.getTotalPrice() != null ? item.getTotalPrice() : java.math.BigDecimal.ZERO)
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                totalAmountMap.put(p.getId(), totalAmount);
            }
        }

        model.addAttribute("report", report);
        model.addAttribute("examination", examination);
        model.addAttribute("patient", patient);
        model.addAttribute("patientUser", patientUser);
        model.addAttribute("doctor", doctor);
        model.addAttribute("doctorUser", doctorUser);
        model.addAttribute("prescriptions", prescriptions);
        model.addAttribute("itemsMap", itemsMap);
        model.addAttribute("totalAmountMap", totalAmountMap);
        model.addAttribute("isAdmin", true); // 标记为管理员访问
        return "admin/report-preview";
    }

    // ============ 药房窗口 ============
    @GetMapping("/pharmacy")
    public String pharmacyWindow() {
        // 依需求：删除 /admin/pharmacy 的“处方发药管理”功能界面
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/pharmacy/{id}")
    public String pharmacyPrescriptionDetail(@PathVariable Long id,
                                             @RequestParam(required = false) Long returnPatientId,
                                             Model model) {
        Prescription prescription = prescriptionService.findById(id).orElse(null);
        if (prescription == null) {
            return "redirect:/admin/pharmacy";
        }
        if (returnPatientId != null && (prescription.getPatientId() == null || !returnPatientId.equals(prescription.getPatientId()))) {
            returnPatientId = null;
        }
        model.addAttribute("adminBackUrl", returnPatientId != null
                ? "/admin/patients/" + returnPatientId
                : "/admin/medicines");

        // 加载患者信息
        Patient patient = null;
        User patientUser = null;
        if (prescription.getPatientId() != null) {
            patient = patientService.findById(prescription.getPatientId()).orElse(null);
            if (patient != null && patient.getUserId() != null) {
                patientUser = userService.findById(patient.getUserId()).orElse(null);
            }
        }

        // 加载医生信息
        Doctor doctor = null;
        User doctorUser = null;
        if (prescription.getDoctorId() != null) {
            doctor = doctorService.findById(prescription.getDoctorId()).orElse(null);
            if (doctor != null && doctor.getUserId() != null) {
                doctorUser = userService.findById(doctor.getUserId()).orElse(null);
            }
        }

        // 加载处方明细
        List<PrescriptionItem> items = prescriptionService.getPrescriptionItems(id);

        // 计算总金额
        java.math.BigDecimal totalAmount = items.stream()
                .map(item -> item.getTotalPrice() != null ? item.getTotalPrice() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        model.addAttribute("prescription", prescription);
        model.addAttribute("patient", patient);
        model.addAttribute("patientUser", patientUser);
        model.addAttribute("doctor", doctor);
        model.addAttribute("doctorUser", doctorUser);
        model.addAttribute("items", items);
        model.addAttribute("totalAmount", totalAmount);
        return "admin/pharmacy-detail";
    }

    // ============ 处方列表（挂号入口） ============
    @GetMapping("/prescriptions")
    public String prescriptions() {
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/prescriptions/{id}/delete")
    public String deletePrescription(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            prescriptionService.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "处方删除成功！");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "删除失败：" + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/prescriptions/bulk-delete")
    public String bulkDeletePrescriptions(@RequestParam(required = false) Long[] ids, RedirectAttributes redirectAttributes) {
        if (ids == null || ids.length == 0) {
            redirectAttributes.addFlashAttribute("error", "请至少选择一份处方");
            return "redirect:/admin/dashboard";
        }
        int deleted = 0;
        for (Long id : ids) {
            try {
                prescriptionService.deleteById(id);
                deleted++;
            } catch (Exception ignored) {
            }
        }
        redirectAttributes.addFlashAttribute("success", "已批量删除 " + deleted + " 份处方");
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/pharmacy/{id}/delete")
    public String deletePrescriptionFromPharmacy(@PathVariable Long id,
                                                @RequestParam(required = false) Long patientId,
                                                RedirectAttributes redirectAttributes) {
        try {
            prescriptionService.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "处方删除成功！");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "删除失败：" + e.getMessage());
        }
        if (patientId != null) {
            return "redirect:/admin/patients/" + patientId;
        }
        return "redirect:/admin/pharmacy";
    }

    @PostMapping("/pharmacy/bulk-delete")
    public String bulkDeletePrescriptionsFromPharmacy(@RequestParam(required = false) Long[] ids,
                                                      @RequestParam(required = false) Long patientId,
                                                      RedirectAttributes redirectAttributes) {
        if (ids == null || ids.length == 0) {
            redirectAttributes.addFlashAttribute("error", "请至少选择一份处方");
            if (patientId != null) {
                return "redirect:/admin/patients/" + patientId;
            }
            return "redirect:/admin/pharmacy";
        }
        int deleted = 0;
        for (Long id : ids) {
            try {
                prescriptionService.deleteById(id);
                deleted++;
            } catch (Exception ignored) {
            }
        }
        redirectAttributes.addFlashAttribute("success", "已批量删除 " + deleted + " 份处方");
        if (patientId != null) {
            return "redirect:/admin/patients/" + patientId;
        }
        return "redirect:/admin/pharmacy";
    }

    @PostMapping("/pharmacy/{id}/dispense")
    public String dispensePrescription(@PathVariable Long id,
                                       @RequestParam(required = false) String signature,
                                       RedirectAttributes redirectAttributes) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Long operator = userService.findByUsername(username).orElseThrow().getId();
        pharmacyService.dispense(id, operator);
        redirectAttributes.addFlashAttribute("success", "发药完成，库存已扣减");
        return "redirect:/admin/pharmacy/" + id;
    }
}
