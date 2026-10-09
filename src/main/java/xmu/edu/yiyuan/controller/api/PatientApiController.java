package xmu.edu.yiyuan.controller.api;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import xmu.edu.yiyuan.controller.PatientController;
import xmu.edu.yiyuan.controller.PatientPageController;
import xmu.edu.yiyuan.dto.ApiResponse;
import xmu.edu.yiyuan.entity.Hospitalization;
import xmu.edu.yiyuan.entity.MedicalRecord;
import xmu.edu.yiyuan.service.BedService;
import xmu.edu.yiyuan.service.DoctorService;
import xmu.edu.yiyuan.service.ExaminationService;
import xmu.edu.yiyuan.service.UserService;
import xmu.edu.yiyuan.service.AppointmentBookingService;
import xmu.edu.yiyuan.service.PrescriptionService;
import xmu.edu.yiyuan.service.ReportService;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/patient")
public class PatientApiController {
    private final PatientController patient;
    private final PatientPageController pages;
    private final BedService beds;
    private final DoctorService doctors;
    private final UserService users;
    private final AppointmentBookingService booking;
    private final ExaminationService examinations;
    private final PrescriptionService prescriptions;
    private final ReportService reports;
    @org.springframework.beans.factory.annotation.Autowired private xmu.edu.yiyuan.service.PaymentService payments;

    public PatientApiController(PatientController patient, PatientPageController pages,
                                BedService beds, DoctorService doctors, UserService users, AppointmentBookingService booking,
                                ExaminationService examinations, PrescriptionService prescriptions, ReportService reports) {
        this.patient = patient;
        this.pages = pages;
        this.beds = beds;
        this.doctors = doctors;
        this.users = users;
        this.booking = booking;
        this.examinations = examinations;
        this.prescriptions = prescriptions;
        this.reports = reports;
    }

    private ResponseEntity<ApiResponse<Object>> result(ResponseEntity<? extends ApiResponse<?>> response, Model model) {
        return ApiSupport.response(response, model);
    }

    private RedirectAttributes redirects() { return new RedirectAttributesModelMap(); }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Object>> dashboard() { return result(patient.dashboard(), new ExtendedModelMap()); }

    @GetMapping({"/appointments", "/appointments/new"})
    public ResponseEntity<ApiResponse<Object>> appointments() { return result(patient.newAppointmentStep1(), new ExtendedModelMap()); }

    @GetMapping("/appointments/new/step2")
    public ResponseEntity<ApiResponse<Object>> doctors(@RequestParam Long departmentId) {
        return result(patient.newAppointmentStep2(departmentId), new ExtendedModelMap());
    }

    @GetMapping("/appointments/new/step3")
    public ResponseEntity<ApiResponse<Object>> times(@RequestParam Long doctorId, @RequestParam(required = false) LocalDate date,
                                                     @RequestParam(required = false) String error) {
        return result(patient.newAppointmentStep3(doctorId, date, error), new ExtendedModelMap());
    }

    @GetMapping("/appointments/new/step4")
    public ResponseEntity<ApiResponse<Object>> confirm(@RequestParam Long doctorId, @RequestParam LocalDate date,
                                                       @RequestParam String timeSlot) {
        if (!validSlot(timeSlot)) return ResponseEntity.badRequest().body(ApiResponse.error(400, "请选择有效的就诊时段"));
        return result(patient.newAppointmentStep4(doctorId, date, timeSlot), new ExtendedModelMap());
    }

    @PostMapping("/appointments")
    public ResponseEntity<ApiResponse<Object>> create(@RequestParam Long doctorId, @RequestParam LocalDate date,
                                                      @RequestParam String timeSlot,
                                                      @RequestParam(defaultValue = "GENERAL") String clinicType,
                                                      @RequestParam(required = false) String symptoms) {
        if (!validSlot(timeSlot)) return ResponseEntity.badRequest().body(ApiResponse.error(400, "请选择有效的就诊时段"));
        return result(booking.book(doctorId, date, timeSlot, clinicType, symptoms), new ExtendedModelMap());
    }

    private boolean validSlot(String value) {
        return value != null && java.util.Set.of("9:00-10:00", "10:00-11:00", "11:00-12:00", "14:00-15:00",
                "15:00-16:00", "16:00-17:00", "18:30-19:30", "19:30-20:30", "20:30-21:30").contains(value);
    }

    @GetMapping("/appointments/{id}/record")
    public ResponseEntity<ApiResponse<Object>> record(@PathVariable Long id) {
        Model model = new ExtendedModelMap();
        ResponseEntity<? extends ApiResponse<?>> response = patient.viewRecord(id, model, redirects());
        if (model.getAttribute("record") instanceof MedicalRecord medicalRecord
                && medicalRecord.getExaminationItems() != null
                && medicalRecord.getExaminationItems().trim().matches("\\d+(\\s*[,，;；]\\s*\\d+)*")) {
            java.util.List<String> names = new java.util.ArrayList<>();
            for (String item : medicalRecord.getExaminationItems().split("[,，;；]")) {
                examinations.findById(Long.valueOf(item.trim())).ifPresent(exam -> names.add(exam.getName()));
            }
            if (!names.isEmpty()) model.addAttribute("examinationDisplay", String.join("、", names));
        }
        return result(response, model);
    }

    @PostMapping("/appointments/{id}/cancel")
    public ResponseEntity<ApiResponse<Object>> cancel(@PathVariable Long id) {
        return result(patient.cancelAppointment(id, redirects()), new ExtendedModelMap());
    }

    @GetMapping("/prescriptions")
    public ResponseEntity<ApiResponse<Object>> prescriptions() {
        Model model = new ExtendedModelMap(); return result(patient.myPrescriptions(model), model);
    }

    @GetMapping("/prescriptions/{id}")
    public ResponseEntity<ApiResponse<Object>> prescription(@PathVariable Long id, @RequestParam(required = false) String returnTo) {
        Model model = new ExtendedModelMap(); return result(patient.prescriptionDetail(id, returnTo, model), model);
    }

    @PostMapping("/prescriptions/{id}/collect")
    public ResponseEntity<ApiResponse<Object>> collect(@PathVariable Long id, @RequestParam(required = false) String signature) {
        return result(patient.collectPrescription(id, signature, redirects()), new ExtendedModelMap());
    }

    @PostMapping("/prescriptions/{id}/pay")
    public ResponseEntity<ApiResponse<Object>> payPrescription(@PathVariable Long id) {
        return result(patient.payPrescription(id, redirects()), new ExtendedModelMap());
    }

    @GetMapping("/payments/prescription/{id}/alipay")
    public ResponseEntity<ApiResponse<Object>> prescriptionPayment(@PathVariable Long id) {
        Model model = new ExtendedModelMap();
        var response = patient.prescriptionAlipaySandbox(id, model, redirects());
        if (successful(response)) {
            paymentPatientName(model);
            prescriptions.findById(id).ifPresent(p -> model.addAttribute("alreadyPaid", Boolean.TRUE.equals(p.getPaid())));
        }
        return result(response, model);
    }

    @GetMapping("/payments/report/{id}/alipay")
    public ResponseEntity<ApiResponse<Object>> reportPayment(@PathVariable Long id) {
        Model model = new ExtendedModelMap();
        var response = patient.examReportAlipaySandbox(id, model, redirects());
        if (successful(response)) {
            paymentPatientName(model);
            reports.findById(id).ifPresent(report -> {
                model.addAttribute("alreadyPaid", Boolean.TRUE.equals(report.getPaid()));
                model.addAttribute("paymentType", "EXAM_REPORT");
                model.addAttribute("businessId", id);
                model.addAttribute("orderNumber", "CK-" + id);
                model.addAttribute("cancelUrl", "/patient/reports/" + id);
                model.addAttribute("examReportName", report.getReportType());
                if (report.getExaminationId() != null) examinations.findById(report.getExaminationId()).ifPresent(exam -> {
                    model.addAttribute("amount", payments.paidAmount("EXAM_REPORT", id, exam.getPrice()));
                    model.addAttribute("examReportName", exam.getName());
                });
            });
        }
        return result(response, model);
    }

    @PostMapping("/reports/{id}/pay")
    public ResponseEntity<ApiResponse<Object>> payReport(@PathVariable Long id) {
        return result(patient.payExamReport(id, redirects()), new ExtendedModelMap());
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<Object>> reports() {
        Model model = new ExtendedModelMap(); return result(patient.myReports(model), model);
    }

    @GetMapping({"/reports/{id}", "/reports/{id}/preview"})
    public ResponseEntity<ApiResponse<Object>> report(@PathVariable Long id, @RequestParam(required = false) String from) {
        Model model = new ExtendedModelMap(); return result(patient.viewReport(id, from, model), model);
    }

    @GetMapping("/reports/{id}/download")
    public void download(@PathVariable Long id, HttpServletResponse response) throws IOException {
        ApiResponse<?> authorization = patient.viewReport(id, null, new ExtendedModelMap()).getBody();
        if (authorization == null || authorization.getCode() != 200) {
            response.setStatus(authorization == null ? 500 : authorization.getCode());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":" + response.getStatus() + ",\"msg\":\"无权下载此报告\"}");
            return;
        }
        patient.downloadReportPdf(id, response);
    }

    @GetMapping("/hospitalizations")
    public ResponseEntity<ApiResponse<Object>> hospitalizations() {
        Model model = new ExtendedModelMap();
        ResponseEntity<? extends ApiResponse<?>> response = patient.myHospitalizations(model);
        Map<Long, String> doctorDisplay = new LinkedHashMap<>();
        Map<Long, Object> bedDisplay = new LinkedHashMap<>();
        for (String key : java.util.List.of("currentHospitalizations", "historyHospitalizations")) {
            if (model.getAttribute(key) instanceof Iterable<?> records) {
                for (Object value : records) {
                    if (value instanceof Hospitalization hosp) {
                        if (hosp.getDoctorId() != null) {
                            doctors.findById(hosp.getDoctorId()).flatMap(doctor -> users.findById(doctor.getUserId()))
                                    .ifPresent(user -> doctorDisplay.put(hosp.getId(), user.getRealName()));
                        }
                        if (hosp.getBedId() != null) beds.findById(hosp.getBedId()).ifPresent(bed -> bedDisplay.put(hosp.getId(), bed));
                    }
                }
            }
        }
        model.addAttribute("doctorDisplayMap", doctorDisplay);
        model.addAttribute("bedDisplayMap", bedDisplay);
        return result(response, model);
    }

    @GetMapping("/hospitalizations/{id}/detail")
    public ResponseEntity<ApiResponse<Object>> hospitalization(@PathVariable Long id) {
        Model model = new ExtendedModelMap();
        ResponseEntity<? extends ApiResponse<?>> response = patient.hospitalizationDetail(id, model);
        if (model.getAttribute("hospitalization") instanceof Hospitalization hosp && hosp.getBedId() != null) {
            beds.findById(hosp.getBedId()).ifPresent(bed -> model.addAttribute("bed", bed));
        }
        return result(response, model);
    }

    @PostMapping("/hospitalizations/{id}/request-discharge")
    public ResponseEntity<ApiResponse<Object>> discharge(@PathVariable Long id) {
        return result(patient.requestDischarge(id, redirects()), new ExtendedModelMap());
    }

    @PostMapping("/hospitalizations/{id}/cancel")
    public ResponseEntity<ApiResponse<Object>> cancelHospitalization(@PathVariable Long id) {
        return result(patient.cancelHospitalization(id, redirects()), new ExtendedModelMap());
    }

    @PostMapping("/hospitalizations/{id}/pay")
    public ResponseEntity<ApiResponse<Object>> payHospitalization(@PathVariable Long id) {
        return result(patient.payHospitalization(id, redirects()), new ExtendedModelMap());
    }

    @GetMapping("/payments/hospitalization/{id}/alipay")
    public ResponseEntity<ApiResponse<Object>> hospitalizationPayment(@PathVariable Long id) {
        Model model = new ExtendedModelMap();
        var response = patient.hospitalizationAlipaySandbox(id, model, redirects());
        if (successful(response)) {
            paymentPatientName(model);
            boolean paid = model.getAttribute("hospitalization") instanceof Hospitalization h && Boolean.TRUE.equals(h.getPaid());
            if (model.getAttribute("medicalOrders") instanceof Iterable<?> orders) {
                for (Object item : orders) if (item instanceof xmu.edu.yiyuan.entity.MedicalOrder order
                        && order.getOrderContent() != null && order.getOrderContent().contains("住院费用已支付")) paid = true;
            }
            model.addAttribute("alreadyPaid", paid);
        }
        return result(response, model);
    }

    private boolean successful(ResponseEntity<? extends ApiResponse<?>> response) {
        return response.getBody() != null && response.getBody().getCode() == 200;
    }

    private void paymentPatientName(Model model) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        users.findByUsername(auth.getName()).ifPresent(user -> model.addAttribute("patientName",
                user.getRealName() == null || user.getRealName().isBlank() ? user.getUsername() : user.getRealName()));
    }

    @GetMapping("/payments/history")
    public ResponseEntity<ApiResponse<Object>> payments() {
        Model model = new ExtendedModelMap(); return result(patient.paymentHistory(model), model);
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Object>> profile() {
        Model model = new ExtendedModelMap(); return result(patient.profile(model), model);
    }

    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<Object>> updateProfile(@RequestParam Map<String, String> form) {
        return result(patient.updateProfile(form.get("realName"), form.get("email"), form.get("phone"), form.get("idCard"),
                form.get("gender"), form.get("birthday"), form.get("address"), form.get("emergencyContact"), form.get("emergencyPhone")), new ExtendedModelMap());
    }

    @PostMapping("/ai/triage")
    public ResponseEntity<ApiResponse<Object>> triage(@RequestParam(required = false) String description) {
        return ResponseEntity.ok(ApiResponse.success(ApiSupport.sanitize(pages.triage(description))));
    }
}
