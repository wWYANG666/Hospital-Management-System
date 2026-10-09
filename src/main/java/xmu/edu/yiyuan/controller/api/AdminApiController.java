package xmu.edu.yiyuan.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import xmu.edu.yiyuan.controller.AdminController;
import xmu.edu.yiyuan.dto.ApiResponse;
import xmu.edu.yiyuan.entity.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.function.BiFunction;

/** JSON adapters reuse the existing business operations and their validation. */
@RestController
@RequestMapping("/api/admin")
public class AdminApiController {
    private final AdminController legacy;
    private final xmu.edu.yiyuan.service.AppointmentService appointmentService;
    private final xmu.edu.yiyuan.service.PatientService patientService;
    private final xmu.edu.yiyuan.service.DoctorService doctorService;
    private final xmu.edu.yiyuan.service.UserService userService;

    public AdminApiController(AdminController legacy,
                              xmu.edu.yiyuan.service.AppointmentService appointmentService,
                              xmu.edu.yiyuan.service.PatientService patientService,
                              xmu.edu.yiyuan.service.DoctorService doctorService,
                              xmu.edu.yiyuan.service.UserService userService) {
        this.legacy = legacy;
        this.appointmentService = appointmentService;
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.userService = userService;
    }

    private Object page(BiFunction<ExtendedModelMap, RedirectAttributesModelMap, String> action) {
        ExtendedModelMap model = new ExtendedModelMap();
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        return ApiSupport.page(action.apply(model, redirect), model, redirect);
    }

    private void addDoctorUsers(ExtendedModelMap model) {
        java.util.Map<String, Object> users = new java.util.LinkedHashMap<>();
        Object departments = model.get("departments");
        if (departments instanceof Iterable<?> values) {
            for (Object value : values) {
                if (value instanceof Department department) {
                    ExtendedModelMap detail = new ExtendedModelMap();
                    legacy.editDepartment(department.getId(), detail);
                    if (detail.get("doctorUserMap") instanceof java.util.Map<?, ?> map) {
                        map.forEach((id, user) -> users.put(String.valueOf(id), user));
                    }
                }
            }
        }
        model.addAttribute("doctorUserMap", users);
    }

    @GetMapping("/dashboard")
    public Object dashboard() { return page((m, r) -> legacy.dashboard(m)); }

    @PostMapping("/ai/summary")
    public Object summary() { return ResponseEntity.ok(ApiResponse.success(java.util.Map.of("summary", legacy.generateAdminSummary()))); }

    @GetMapping("/profile")
    public Object profile(Principal principal) { return page((m, r) -> legacy.adminProfile(m, principal)); }

    @GetMapping("/ai-logs")
    public Object logs(@RequestParam(required = false) String whoUser, @RequestParam(required = false) String whereScene,
                       @RequestParam(required = false) String fromTime, @RequestParam(required = false) String toTime,
                       @RequestParam(defaultValue = "200") Integer limit) {
        return page((m, r) -> legacy.aiLogs(whoUser, whereScene, fromTime, toTime, limit, m));
    }

    @PostMapping("/ai-logs/bulk-delete")
    public Object deleteLogs(@RequestParam(required = false) Long[] ids, @RequestParam(required = false) String whoUser,
                             @RequestParam(required = false) String whereScene, @RequestParam(required = false) String fromTime,
                             @RequestParam(required = false) String toTime, @RequestParam(defaultValue = "200") Integer limit) {
        return page((m, r) -> legacy.bulkDeleteAiLogs(ids, whoUser, whereScene, fromTime, toTime, limit, r));
    }

    @GetMapping("/departments")
    public Object departments() { return page((m, r) -> legacy.departments(m)); }
    @GetMapping("/departments/new")
    public Object departmentNew() { return page((m, r) -> legacy.newDepartment(m)); }
    @GetMapping("/departments/{id}/edit")
    public Object department(@PathVariable Long id) { return page((m, r) -> legacy.editDepartment(id, m)); }
    @PostMapping("/departments")
    public Object departmentCreate(@ModelAttribute Department department) { return page((m, r) -> legacy.createDepartment(department)); }
    @PostMapping("/departments/{id}")
    public Object departmentUpdate(@PathVariable Long id, @ModelAttribute Department department) { return page((m, r) -> legacy.updateDepartment(id, department)); }
    @PostMapping("/departments/{id}/delete")
    public Object departmentDelete(@PathVariable Long id) { return page((m, r) -> legacy.deleteDepartment(id)); }
    @PostMapping("/departments/bulk-delete")
    public Object departmentsDelete(@RequestParam(required = false) Long[] ids) { return page((m, r) -> legacy.bulkDeleteDepartments(ids, r)); }

    @GetMapping({"/doctors", "/doctors/new"})
    public Object doctors() { return page((m, r) -> legacy.doctorsLegacy()); }
    @GetMapping("/departments/{departmentId}/doctors/{doctorId}/schedules")
    public Object doctorSchedules(@PathVariable Long departmentId, @PathVariable Long doctorId) {
        return page((m, r) -> legacy.doctorSchedules(departmentId, doctorId, m));
    }
    @PostMapping("/departments/{departmentId}/doctors")
    public Object createDoctor(@PathVariable Long departmentId, @RequestParam String username, @RequestParam String password,
                               @RequestParam String realName, @RequestParam(required = false) String email,
                               @RequestParam(required = false) String phone, @RequestParam String department,
                               @RequestParam(required = false) String title, @RequestParam(required = false) String specialty,
                               @RequestParam(required = false) String introduction) {
        return page((m, r) -> legacy.createDoctor(departmentId, username, password, realName, email, phone, department, title, specialty, introduction, r));
    }
    @PostMapping("/doctors/{doctorId}/approve")
    public Object approveDoctor(@PathVariable Long doctorId, @RequestParam Long departmentId) {
        return page((m, r) -> legacy.approveDoctor(doctorId, departmentId, r));
    }
    @PostMapping("/doctors/{doctorId}/delete")
    public Object deleteDoctor(@PathVariable Long doctorId, @RequestParam Long departmentId) {
        return page((m, r) -> legacy.deleteDoctor(doctorId, departmentId, r));
    }
    @GetMapping("/users/{id}/change-password")
    public Object passwordForm(@PathVariable Long id, @RequestParam(required = false) Long departmentId,
                               @RequestParam(required = false) String returnTo) {
        return page((m, r) -> legacy.changePasswordForm(id, departmentId, returnTo, m));
    }
    @PostMapping("/users/{id}/change-password")
    public Object password(@PathVariable Long id, @RequestParam String newPassword,
                           @RequestParam(required = false) Long departmentId, @RequestParam(required = false) String returnTo) {
        return page((m, r) -> legacy.changePassword(id, newPassword, departmentId, returnTo, r));
    }
    @PostMapping("/users/{id}/reset-password")
    public Object resetPassword(@PathVariable Long id, @RequestParam(required = false) String newPassword,
                                @RequestParam(required = false) Long departmentId, @RequestParam(required = false) String returnTo) {
        return page((m, r) -> legacy.resetPasswordLegacy(id, newPassword, departmentId, returnTo, r));
    }

    @GetMapping("/patients")
    public Object patients(@RequestParam(required = false) String keyword) { return page((m, r) -> legacy.patients(keyword, m)); }
    @GetMapping("/patients/{id}")
    public Object patient(@PathVariable Long id) { return page((m, r) -> legacy.patientDetail(id, m)); }
    @PostMapping("/patients/{patientId}/appointments/bulk-delete")
    public Object patientAppointmentsDelete(@PathVariable Long patientId, @RequestParam(required = false) Long[] ids) {
        return page((m, r) -> legacy.bulkDeletePatientAppointments(patientId, ids, r));
    }
    @PostMapping("/patients/{patientId}/reports/bulk-delete")
    public Object patientReportsDelete(@PathVariable Long patientId, @RequestParam(required = false) Long[] ids) {
        return page((m, r) -> legacy.bulkDeletePatientReports(patientId, ids, r));
    }
    @PostMapping("/patients/{patientId}/hospitalizations/{id}/status")
    public Object hospitalizationStatus(@PathVariable Long patientId, @PathVariable Long id, @RequestParam String status) {
        return page((m, r) -> legacy.updatePatientHospitalizationStatus(patientId, id, status, r));
    }
    @PostMapping("/patients/{patientId}/hospitalizations/bulk-delete")
    public Object patientHospitalizationsDelete(@PathVariable Long patientId, @RequestParam(required = false) Long[] ids) {
        return page((m, r) -> legacy.bulkDeletePatientHospitalizations(patientId, ids, r));
    }
    /** The legacy page redirects to the patient center; the SPA needs an all-hospital queue. */
    @GetMapping("/appointments")
    public Object appointments() {
        java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
        java.util.List<Appointment> appointments = appointmentService.findAll();
        java.util.Map<Long, User> patientUserMap = new java.util.LinkedHashMap<>();
        java.util.Map<Long, User> doctorUserMap = new java.util.LinkedHashMap<>();
        for (Appointment appointment : appointments) {
            if (appointment.getPatientId() != null) {
                patientService.findById(appointment.getPatientId()).ifPresent(patient ->
                        userService.findById(patient.getUserId()).ifPresent(user -> patientUserMap.put(appointment.getPatientId(), user)));
            }
            if (appointment.getDoctorId() != null) {
                doctorService.findById(appointment.getDoctorId()).ifPresent(doctor ->
                        userService.findById(doctor.getUserId()).ifPresent(user -> doctorUserMap.put(appointment.getDoctorId(), user)));
            }
        }
        data.put("appointments", appointments);
        data.put("patientUserMap", patientUserMap);
        data.put("doctorUserMap", doctorUserMap);
        data.put("totalAppointments", appointments.size());
        data.put("pendingAppointments", appointments.stream().filter(a -> a.getStatus() == Appointment.AppointmentStatus.PENDING).count());
        data.put("confirmedAppointments", appointments.stream().filter(a -> a.getStatus() == Appointment.AppointmentStatus.CONFIRMED).count());
        data.put("completedAppointments", appointments.stream().filter(a -> a.getStatus() == Appointment.AppointmentStatus.COMPLETED).count());
        return ResponseEntity.ok(ApiResponse.success(ApiSupport.sanitize(data)));
    }
    @PostMapping("/appointments/{id}/status")
    public Object appointmentStatus(@PathVariable Long id, @RequestParam String status, @RequestParam(required = false) Long patientId) {
        return page((m, r) -> legacy.updateAppointmentStatus(id, status, patientId));
    }
    @PostMapping("/appointments/{id}/cancel")
    public Object appointmentCancel(@PathVariable Long id, @RequestParam(required = false) Long patientId) {
        return page((m, r) -> legacy.cancelAppointment(id, patientId));
    }
    @PostMapping("/appointments/bulk-delete")
    public Object appointmentDelete(@RequestParam(required = false) Long[] ids, @RequestParam(required = false) Long patientId) {
        return page((m, r) -> legacy.bulkDeleteAppointments(ids, patientId));
    }
    @GetMapping("/appointments/{id}/reports")
    public Object appointmentReports(@PathVariable Long id) { return page((m, r) -> legacy.appointmentReports(id, m)); }
    @GetMapping("/appointments/{id}/prescriptions")
    public Object appointmentPrescriptions(@PathVariable Long id) { return page((m, r) -> legacy.appointmentPrescriptions(id, m)); }

    @GetMapping("/medicines")
    public Object medicines() { return page((m, r) -> legacy.medicines(m)); }
    @GetMapping("/medicines/new")
    public Object medicineNew() { return page((m, r) -> legacy.newMedicine(m)); }
    @GetMapping("/medicines/{id}/edit")
    public Object medicine(@PathVariable Long id) { return page((m, r) -> legacy.editMedicine(id, m)); }
    @PostMapping("/medicines")
    public Object medicineCreate(@ModelAttribute Medicine medicine, @RequestParam(required = false) Long departmentId) {
        return page((m, r) -> legacy.createMedicine(medicine, departmentId, r));
    }
    @PostMapping("/medicines/{id}")
    public Object medicineUpdate(@PathVariable Long id, @ModelAttribute Medicine medicine, @RequestParam(required = false) Long departmentId) {
        return page((m, r) -> legacy.updateMedicine(id, medicine, departmentId, r));
    }
    @PostMapping("/medicines/{id}/delete")
    public Object medicineDelete(@PathVariable Long id) { return page((m, r) -> legacy.deleteMedicine(id)); }
    @PostMapping("/medicines/bulk-delete")
    public Object medicinesDelete(@RequestParam(required = false) Long[] ids) { return page((m, r) -> legacy.bulkDeleteMedicines(ids, r)); }
    @GetMapping("/medicines/{id}/inbound")
    public Object inboundForm(@PathVariable Long id) { return page((m, r) -> legacy.inboundMedicineForm(id, m)); }
    @PostMapping("/medicines/{id}/inbound")
    public Object inbound(@PathVariable Long id, @RequestParam String batchNumber,
                          @RequestParam(required = false) LocalDate productionDate, @RequestParam LocalDate expiryDate,
                          @RequestParam Integer quantity, @RequestParam BigDecimal purchasePrice,
                          @RequestParam(defaultValue = "药房") String location, @RequestParam(required = false) String operator) {
        return page((m, r) -> legacy.submitInbound(id, batchNumber, productionDate, expiryDate, quantity, purchasePrice, location, operator, r));
    }
    @GetMapping("/medicines/inbounds")
    public Object inbounds() { return page((m, r) -> legacy.inboundList(m)); }

    @GetMapping("/examinations")
    public Object examinations() { return page((m, r) -> legacy.examinations(m)); }
    @GetMapping("/examinations/new")
    public Object examinationNew() { return page((m, r) -> legacy.newExamination(m)); }
    @GetMapping("/examinations/{id}/edit")
    public Object examination(@PathVariable Long id) { return page((m, r) -> legacy.editExamination(id, m)); }
    @PostMapping("/examinations")
    public Object examinationCreate(@ModelAttribute Examination examination, @RequestParam Long departmentId) {
        return page((m, r) -> legacy.createExamination(examination, departmentId));
    }
    @PostMapping("/examinations/{id}")
    public Object examinationUpdate(@PathVariable Long id, @ModelAttribute Examination examination, @RequestParam Long departmentId) {
        return page((m, r) -> legacy.updateExamination(id, examination, departmentId));
    }
    @PostMapping("/examinations/{id}/delete")
    public Object examinationDelete(@PathVariable Long id) { return page((m, r) -> legacy.deleteExamination(id)); }
    @PostMapping("/examinations/bulk-delete")
    public Object examinationsDelete(@RequestParam(required = false) Long[] ids) { return page((m, r) -> legacy.bulkDeleteExaminations(ids, r)); }

    @GetMapping("/beds")
    public Object beds() { return page((m, r) -> legacy.beds(m)); }
    @GetMapping("/beds/new")
    public Object bedNew() { return page((m, r) -> legacy.newBed(m)); }
    @GetMapping("/beds/{id}/edit")
    public Object bed(@PathVariable Long id) { return page((m, r) -> legacy.editBed(id, m)); }
    @PostMapping("/beds")
    public Object bedCreate(@ModelAttribute Bed bed) { return page((m, r) -> legacy.createBed(bed)); }
    @PostMapping("/beds/{id}")
    public Object bedUpdate(@PathVariable Long id, @ModelAttribute Bed bed) { return page((m, r) -> legacy.updateBed(id, bed)); }
    @PostMapping("/beds/{id}/delete")
    public Object bedDelete(@PathVariable Long id) { return page((m, r) -> legacy.deleteBed(id)); }
    @PostMapping("/beds/bulk-delete")
    public Object bedsDelete(@RequestParam(required = false) Long[] ids) { return page((m, r) -> legacy.bulkDeleteBeds(ids, r)); }
    @PostMapping("/beds/assign")
    public Object assignBed(@RequestParam Long bedId, @RequestParam Long hospitalizationId) { return page((m, r) -> legacy.assignBed(bedId, hospitalizationId)); }
    @PostMapping("/beds/reject")
    public Object rejectBed(@RequestParam Long hospitalizationId) { return page((m, r) -> legacy.rejectBedAssignment(hospitalizationId, r)); }
    @GetMapping("/hospitalizations")
    public Object hospitalizations() { return page((m, r) -> legacy.hospitalizations(m)); }
    @GetMapping("/hospitalizations/{id}/settle")
    public Object settleForm(@PathVariable Long id) { return page((m, r) -> legacy.settleForm(id, m)); }
    @PostMapping("/hospitalizations/{id}/settle")
    public Object settle(@PathVariable Long id) { return page((m, r) -> legacy.submitSettle(id, r)); }
    @PostMapping("/hospitalizations/{id}/delete")
    public Object hospitalizationDelete(@PathVariable Long id) { return page((m, r) -> legacy.deleteHospitalization(id, r)); }
    @PostMapping("/hospitalizations/bulk-delete")
    public Object hospitalizationsDelete(@RequestParam(required = false) Long[] ids) { return page((m, r) -> legacy.bulkDeleteHospitalizations(ids, r)); }

    @GetMapping("/schedules")
    public Object schedules() { return page((m, r) -> { String view = legacy.schedules(m); addDoctorUsers(m); return view; }); }
    @GetMapping("/schedules/new")
    public Object scheduleNew(@RequestParam(required = false) Long doctorId, @RequestParam(required = false) Long departmentId) {
        return page((m, r) -> { String view = legacy.newSchedule(doctorId, departmentId, m); addDoctorUsers(m); return view; });
    }
    @PostMapping("/schedules")
    public Object scheduleCreate(@RequestParam Long doctorId, @RequestParam Long departmentId, @RequestParam String startDate,
                                 @RequestParam String endDate, @RequestParam String workTime, @RequestParam Integer maxAppointments,
                                 @RequestParam(defaultValue = "0") Integer expertMaxAppointments) {
        return page((m, r) -> legacy.createSchedule(doctorId, departmentId, startDate, endDate, workTime, maxAppointments, expertMaxAppointments, r));
    }
    @PostMapping("/schedules/{id}/delete")
    public Object scheduleDelete(@PathVariable Long id, @RequestParam(required = false) Long departmentId, @RequestParam(required = false) Long doctorId) {
        return page((m, r) -> legacy.deleteSchedule(id, departmentId, doctorId, r));
    }
    @PostMapping("/schedules/bulk-delete")
    public Object schedulesDelete(@RequestParam(required = false) Long[] ids, @RequestParam(required = false) Long departmentId,
                                  @RequestParam(required = false) Long doctorId) {
        return page((m, r) -> legacy.bulkDeleteSchedules(ids, departmentId, doctorId, r));
    }
    @PostMapping("/schedules/{id}/approve-leave")
    public Object approveLeave(@PathVariable Long id, @RequestParam(required = false) Long departmentId, @RequestParam(required = false) Long doctorId) {
        return page((m, r) -> legacy.approveScheduleLeave(id, departmentId, doctorId, r));
    }
    @PostMapping("/schedules/{id}/reject-leave")
    public Object rejectLeave(@PathVariable Long id, @RequestParam(required = false) Long departmentId, @RequestParam(required = false) Long doctorId) {
        return page((m, r) -> legacy.rejectScheduleLeave(id, departmentId, doctorId, r));
    }
    @PostMapping("/schedules/{id}/restore")
    public Object restore(@PathVariable Long id, @RequestParam(required = false) Long departmentId, @RequestParam(required = false) Long doctorId) {
        return page((m, r) -> legacy.restoreSchedule(id, departmentId, doctorId, r));
    }
    @GetMapping("/reports")
    public Object reports() { return page((m, r) -> legacy.reports(m)); }
    @GetMapping("/reports/{id}/preview")
    public Object report(@PathVariable Long id, @RequestParam(required = false) Long returnPatientId) {
        return page((m, r) -> legacy.previewReport(id, returnPatientId, m));
    }
    @PostMapping("/reports/{id}/delete")
    public Object reportDelete(@PathVariable Long id) { return page((m, r) -> legacy.deleteReport(id)); }
    @PostMapping("/reports/bulk-delete")
    public Object reportsDelete(@RequestParam(required = false) Long[] ids) { return page((m, r) -> legacy.bulkDeleteReports(ids)); }
    @GetMapping({"/pharmacy", "/prescriptions"})
    public Object pharmacy() { return page((m, r) -> legacy.pharmacyWindow()); }
    @GetMapping("/pharmacy/{id}")
    public Object prescription(@PathVariable Long id, @RequestParam(required = false) Long returnPatientId) {
        return page((m, r) -> legacy.pharmacyPrescriptionDetail(id, returnPatientId, m));
    }
    @PostMapping("/prescriptions/{id}/delete")
    public Object prescriptionDelete(@PathVariable Long id) { return page((m, r) -> legacy.deletePrescription(id, r)); }
    @PostMapping("/prescriptions/bulk-delete")
    public Object prescriptionsDelete(@RequestParam(required = false) Long[] ids) { return page((m, r) -> legacy.bulkDeletePrescriptions(ids, r)); }
    @PostMapping("/pharmacy/{id}/delete")
    public Object pharmacyDelete(@PathVariable Long id, @RequestParam(required = false) Long patientId) {
        return page((m, r) -> legacy.deletePrescriptionFromPharmacy(id, patientId, r));
    }
    @PostMapping("/pharmacy/bulk-delete")
    public Object pharmacyBulkDelete(@RequestParam(required = false) Long[] ids, @RequestParam(required = false) Long patientId) {
        return page((m, r) -> legacy.bulkDeletePrescriptionsFromPharmacy(ids, patientId, r));
    }
    @PostMapping("/pharmacy/{id}/dispense")
    public Object dispense(@PathVariable Long id, @RequestParam(required = false) String signature) {
        return page((m, r) -> legacy.dispensePrescription(id, signature, r));
    }
}
