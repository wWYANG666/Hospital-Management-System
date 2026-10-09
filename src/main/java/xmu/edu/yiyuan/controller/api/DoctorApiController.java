package xmu.edu.yiyuan.controller.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import xmu.edu.yiyuan.controller.DoctorController;
import xmu.edu.yiyuan.dto.ApiResponse;
import xmu.edu.yiyuan.entity.Doctor;
import xmu.edu.yiyuan.entity.Hospitalization;
import xmu.edu.yiyuan.entity.MedicalRecord;
import xmu.edu.yiyuan.service.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.BiFunction;

@RestController
@RequestMapping("/api/doctor")
public class DoctorApiController {
    private final DoctorController legacy;
    private final UserService users;
    private final DoctorService doctors;
    private final AppointmentService appointments;
    private final HospitalizationService hospitalizations;
    private final ReportService reports;
    private final ScheduleService schedules;
    private final ExaminationService examinations;

    public DoctorApiController(DoctorController legacy, UserService users, DoctorService doctors,
                               AppointmentService appointments, HospitalizationService hospitalizations,
                               ReportService reports, ScheduleService schedules, ExaminationService examinations) {
        this.legacy = legacy;
        this.users = users;
        this.doctors = doctors;
        this.appointments = appointments;
        this.hospitalizations = hospitalizations;
        this.reports = reports;
        this.schedules = schedules;
        this.examinations = examinations;
    }

    private Object page(BiFunction<ExtendedModelMap, RedirectAttributesModelMap, String> action) {
        ExtendedModelMap model = new ExtendedModelMap();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        String view = action.apply(model, redirect);
        if ("doctor/account-unbound".equals(view)) model.addAttribute("accountUnbound", true);
        return ApiSupport.page(view, model, redirect);
    }

    private Long currentDoctorId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return users.findByUsername(authentication.getName())
                .flatMap(user -> doctors.findByUserId(user.getId()))
                .map(Doctor::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "账号未绑定医生档案"));
    }

    private void requireOwner(Long ownerId) {
        if (!Objects.equals(currentDoctorId(), ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权访问该诊疗记录");
        }
    }

    private void appointmentOwner(Long id) {
        requireOwner(appointments.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "挂号不存在")).getDoctorId());
    }

    private void hospitalizationOwner(Long id) {
        requireOwner(hospitalizations.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "住院记录不存在")).getDoctorId());
    }

    private void reportOwner(Long id) {
        requireOwner(reports.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "报告不存在")).getDoctorId());
    }

    private void activeHospitalization(Long id) {
        Hospitalization hospitalization = hospitalizations.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "住院记录不存在"));
        requireOwner(hospitalization.getDoctorId());
        if (hospitalization.getStatus() != Hospitalization.HospitalizationStatus.ADMITTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "患者已出院，不能修改住院诊疗记录");
        }
    }

    @GetMapping("/dashboard")
    public Object dashboard() { return page((m, r) -> legacy.dashboard(m)); }

    @GetMapping("/diagnose")
    public Object diagnoseList() { return page((m, r) -> legacy.diagnoseList(m)); }

    @GetMapping("/diagnose/{id}")
    public Object diagnose(@PathVariable Long id) {
        appointmentOwner(id);
        return page((m, r) -> legacy.diagnoseForm(id, m, r));
    }

    @PostMapping("/diagnose/{id}")
    public Object submitDiagnosis(@PathVariable Long id, @RequestParam String diagnosis,
                                 @RequestParam(required = false) String prescription,
                                 @RequestParam(required = false) String[] examinationItems,
                                 @RequestParam(required = false) String[] medicines, HttpServletRequest request) {
        appointmentOwner(id);
        if (diagnosis.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写诊断结果");
        return page((m, r) -> legacy.submitDiagnosis(id, diagnosis, prescription, examinationItems, medicines, request, m, r));
    }

    @GetMapping("/appointments")
    public Object appointments() { return page((m, r) -> legacy.appointments(m)); }

    @GetMapping("/appointments/{id}/record")
    public Object record(@PathVariable Long id) {
        appointmentOwner(id);
        return page((m, r) -> {
            String view = legacy.viewRecord(id, m);
            if (m.getAttribute("record") instanceof MedicalRecord record && record.getExaminationItems() != null) {
                List<String> labels = new ArrayList<>();
                for (String value : record.getExaminationItems().split(",")) {
                    if (value.isBlank()) continue;
                    try {
                        examinations.findById(Long.parseLong(value.trim())).ifPresent(exam -> labels.add(exam.getName()));
                    } catch (NumberFormatException ignored) {
                        labels.add(value.trim());
                    }
                }
                m.addAttribute("examinationLabels", labels);
            }
            return view;
        });
    }

    @GetMapping("/schedule-detail")
    public Object scheduleDetail(@RequestParam String workTime) {
        if (!List.of("MORNING", "AFTERNOON", "EVENING").contains(workTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效班次");
        }
        return page((m, r) -> legacy.scheduleDetail(workTime, m));
    }

    @GetMapping("/schedules")
    public Object schedules() { return page((m, r) -> legacy.mySchedules(m)); }

    @PostMapping("/schedules/{id}/leave")
    public Object scheduleLeave(@PathVariable Long id) {
        requireOwner(schedules.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "排班不存在")).getDoctorId());
        return page((m, r) -> legacy.requestScheduleLeave(id, r));
    }

    @GetMapping("/reports")
    public Object reports(@RequestParam(required = false) Long unpaidReport) {
        return page((m, r) -> legacy.reports(unpaidReport, m));
    }

    @GetMapping("/reports/{id}/edit")
    public Object editReport(@PathVariable Long id) {
        reportOwner(id);
        return page((m, r) -> legacy.editReportForm(id, m));
    }

    @GetMapping("/reports/{id}/preview")
    public Object previewReport(@PathVariable Long id) {
        reportOwner(id);
        return page((m, r) -> legacy.previewReportForDoctor(id, m));
    }

    @PostMapping("/reports/{id}")
    public Object saveReport(@PathVariable Long id, @RequestParam String reportContent) {
        reportOwner(id);
        if (reportContent.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写报告内容");
        return page((m, r) -> legacy.saveReport(id, reportContent, m));
    }

    @GetMapping("/hospitalizations")
    public Object hospitalizations() { return page((m, r) -> legacy.hospitalizations(m)); }

    @GetMapping("/hospitalizations/request/{appointmentId}")
    public Object hospitalizationRequest(@PathVariable Long appointmentId) {
        appointmentOwner(appointmentId);
        return page((m, r) -> legacy.hospitalizationRequestForm(appointmentId, m, r));
    }

    @PostMapping("/hospitalizations/request")
    public Object submitHospitalizationRequest(@RequestParam Long appointmentId, @RequestParam String admissionReason,
                                              @RequestParam Integer expectedDays, @RequestParam Long requestDepartmentId) {
        appointmentOwner(appointmentId);
        if (expectedDays < 1 || expectedDays > 365 || admissionReason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请核对住院原因和预计天数");
        }
        return page((m, r) -> legacy.submitHospitalizationRequest(appointmentId, admissionReason, expectedDays, requestDepartmentId, r));
    }

    @GetMapping("/hospitalizations/{id}/assign-bed")
    public Object assignBedForm(@PathVariable Long id) {
        activeHospitalization(id);
        return page((m, r) -> legacy.assignBedForm(id, m));
    }

    @PostMapping("/hospitalizations/{id}/assign-bed")
    public Object assignBed(@PathVariable Long id, @RequestParam Long bedId) {
        activeHospitalization(id);
        return page((m, r) -> {
            String view = legacy.assignBed(id, bedId, r);
            if (r.getFlashAttributes().containsKey("success")) {
                r.addFlashAttribute("success", "床位已分配，住院登记已生效");
            }
            return view;
        });
    }

    @GetMapping("/hospitalizations/{id}/manage")
    public Object hospitalizationManage(@PathVariable Long id) {
        hospitalizationOwner(id);
        return page((m, r) -> legacy.manageHospitalization(id, m));
    }

    @PostMapping("/hospitalizations/{id}/orders")
    public Object addOrder(@PathVariable Long id, @RequestParam String orderType, @RequestParam String orderContent,
                           @RequestParam(required = false) Long medicineId,
                           @RequestParam(required = false) Integer medicineQuantity,
                           @RequestParam(required = false) Long examinationId,
                           @RequestParam(required = false) String dosage,
                           @RequestParam(required = false) String frequency) {
        activeHospitalization(id);
        if (orderContent.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写医嘱内容");
        return page((m, r) -> legacy.addMedicalOrder(id, orderType, orderContent, medicineId, medicineQuantity, examinationId, dosage, frequency, r));
    }

    @PostMapping("/hospitalizations/{id}/update-condition")
    public Object updateCondition(@PathVariable Long id, @RequestParam String conditionUpdate) {
        activeHospitalization(id);
        if (conditionUpdate.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写病情记录");
        return page((m, r) -> legacy.updateCondition(id, conditionUpdate, r));
    }

    @GetMapping("/hospitalizations/{id}/discharge")
    public Object dischargeForm(@PathVariable Long id) {
        hospitalizationOwner(id);
        return page((m, r) -> legacy.dischargeForm(id, m));
    }

    @PostMapping("/hospitalizations/{id}/discharge")
    public Object discharge(@PathVariable Long id, @RequestParam String dischargeDiagnosis, @RequestParam String dischargeNotes) {
        activeHospitalization(id);
        if (dischargeDiagnosis.isBlank() || dischargeNotes.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写出院诊断及注意事项");
        }
        return page((m, r) -> legacy.submitDischarge(id, dischargeDiagnosis, dischargeNotes, r));
    }

    @GetMapping("/profile")
    public Object profile() { return page((m, r) -> legacy.profile(m)); }

    @PostMapping("/profile")
    public Object updateProfile(@RequestParam(required = false) String title,
                                @RequestParam(required = false) String introduction,
                                @RequestParam(required = false) String department) {
        currentDoctorId();
        return page((m, r) -> legacy.updateProfile(title, introduction, department));
    }

    @PostMapping("/ai/diagnosis-suggestion")
    public Object diagnosisSuggestion(@RequestParam(required = false) String symptoms,
                                       @RequestParam(required = false) String existingDiagnosis,
                                       @RequestParam(required = false) List<Long> medicineIds,
                                       @RequestParam(required = false) List<Long> examinationIds,
                                       @RequestParam(required = false) Long appointmentId,
                                       @RequestParam(required = false) Integer age,
                                       @RequestParam(required = false) String gender,
                                       @RequestParam(required = false) String historySummary,
                                       @RequestParam(required = false) String department) {
        currentDoctorId();
        if (appointmentId != null) appointmentOwner(appointmentId);
        return ApiResponse.success(legacy.generateDiagnosisSuggestion(symptoms, existingDiagnosis, medicineIds,
                examinationIds, appointmentId, age, gender, historySummary, department));
    }

    @PostMapping("/ai/report-suggestion")
    public Object reportSuggestion(@RequestParam(required = false) Long examinationId,
                                    @RequestParam(required = false) String rawResult,
                                    @RequestParam(required = false) String clinicalNote) {
        currentDoctorId();
        return ApiResponse.success(legacy.generateReportSuggestion(examinationId, rawResult, clinicalNote));
    }
}
