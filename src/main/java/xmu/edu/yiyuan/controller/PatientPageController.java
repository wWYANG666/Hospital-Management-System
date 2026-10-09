package xmu.edu.yiyuan.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import xmu.edu.yiyuan.dto.ApiResponse;
import xmu.edu.yiyuan.service.AiSuggestionService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/patient")
public class PatientPageController {

    @Autowired
    private PatientController patientApi;

    @Autowired
    private AiSuggestionService aiSuggestionService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        return render(patientApi.dashboard(), model, "patient/dashboard");
    }

    @GetMapping("/appointments/new")
    public String newAppointmentStep1(Model model) {
        return render(patientApi.newAppointmentStep1(), model, "patient/appointment-step1");
    }

    @GetMapping("/appointments/new/step2")
    public String newAppointmentStep2(@RequestParam Long departmentId, Model model) {
        return render(patientApi.newAppointmentStep2(departmentId), model, "patient/appointment-step2");
    }

    @GetMapping("/appointments/new/step3")
    public String newAppointmentStep3(@RequestParam Long doctorId,
                                      @RequestParam(required = false) LocalDate date,
                                      @RequestParam(required = false) String error,
                                      Model model) {
        return render(patientApi.newAppointmentStep3(doctorId, date, error), model, "patient/appointment-step3");
    }

    @GetMapping("/appointments/new/step4")
    public String newAppointmentStep4(@RequestParam Long doctorId,
                                      @RequestParam LocalDate date,
                                      @RequestParam String timeSlot,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        ResponseEntity<? extends ApiResponse<?>> resp = patientApi.newAppointmentStep4(doctorId, date, timeSlot);
        ApiResponse<?> api = resp.getBody();
        if (api == null) {
            redirectAttributes.addFlashAttribute("error", "页面数据加载失败");
            return "redirect:/patient/appointments/new/step3?doctorId=" + doctorId + "&date=" + date;
        }
        if (api.getCode() == 401) {
            return "redirect:/login/user";
        }
        if (api.getCode() != 200) {
            redirectAttributes.addFlashAttribute("error", api.getMsg());
            return "redirect:/patient/appointments/new/step3?doctorId=" + doctorId + "&date=" + date;
        }
        return render(resp, model, "patient/appointment-step4");
    }

    @PostMapping("/appointments")
    public String createAppointment(@RequestParam Long doctorId,
                                    @RequestParam LocalDate date,
                                    @RequestParam String timeSlot,
                                    @RequestParam(required = false, defaultValue = "GENERAL") String clinicType,
                                    @RequestParam(required = false) String symptoms,
                                    RedirectAttributes redirectAttributes) {
        ApiResponse<Object> resp = unwrap(patientApi.createAppointment(doctorId, date, timeSlot, clinicType, symptoms));
        if (resp.getCode() == 200) {
            redirectAttributes.addFlashAttribute("success", "预约成功");
            return "redirect:/patient/appointments/new";
        }
        redirectAttributes.addFlashAttribute("error", resp.getMsg());
        return "redirect:/patient/appointments/new/step4?doctorId=" + doctorId + "&date=" + date + "&timeSlot=" + timeSlot;
    }

    @PostMapping("/appointments/{id}/cancel")
    public String cancelAppointment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        ApiResponse<Object> resp = unwrap(patientApi.cancelAppointment(id, redirectAttributes));
        redirectAttributes.addFlashAttribute(resp.getCode() == 200 ? "success" : "error", resp.getMsg());
        return "redirect:/patient/appointments/new#my-appointments";
    }

    @GetMapping("/appointments/{id}/record")
    public String viewRecord(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return render(patientApi.viewRecord(id, model, redirectAttributes), model, "patient/medical-record");
    }

    @GetMapping("/prescriptions")
    public String prescriptions(Model model) {
        return render(patientApi.myPrescriptions(model), model, "patient/prescriptions");
    }

    @GetMapping("/prescriptions/{id}")
    public String prescriptionDetail(@PathVariable Long id,
                                     @RequestParam(required = false) String returnTo,
                                     Model model) {
        return render(patientApi.prescriptionDetail(id, returnTo, model), model, "patient/prescription-detail");
    }

    @PostMapping("/prescriptions/{id}/collect")
    public String collectPrescription(@PathVariable Long id,
                                      @RequestParam(required = false) String signature,
                                      RedirectAttributes redirectAttributes) {
        ApiResponse<Object> resp = unwrap(patientApi.collectPrescription(id, signature, redirectAttributes));
        redirectAttributes.addFlashAttribute(resp.getCode() == 200 ? "success" : "error", resp.getMsg());
        return "redirect:/patient/prescriptions/" + id;
    }

    @PostMapping("/prescriptions/{id}/pay")
    public String payPrescription(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        ApiResponse<Object> resp = unwrap(patientApi.payPrescription(id, redirectAttributes));
        redirectAttributes.addFlashAttribute(resp.getCode() == 200 ? "success" : "error", resp.getMsg());
        return "redirect:/patient/prescriptions/" + id;
    }

    @GetMapping("/payments/prescription/{id}/alipay")
    public String prescriptionAlipay(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return render(patientApi.prescriptionAlipaySandbox(id, model, redirectAttributes), model, "patient/alipay-sandbox");
    }

    @PostMapping("/reports/{id}/pay")
    public String payReport(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        ApiResponse<Object> resp = unwrap(patientApi.payExamReport(id, redirectAttributes));
        redirectAttributes.addFlashAttribute(resp.getCode() == 200 ? "success" : "error", resp.getMsg());
        return "redirect:/patient/prescriptions";
    }

    @GetMapping("/payments/report/{id}/alipay")
    public String reportAlipay(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return render(patientApi.examReportAlipaySandbox(id, model, redirectAttributes), model, "patient/alipay-sandbox");
    }

    @GetMapping("/reports")
    public String reports(Model model) {
        return render(patientApi.myReports(model), model, "patient/reports");
    }

    @GetMapping("/reports/{id}")
    public String reportDetail(@PathVariable Long id,
                               @RequestParam(required = false) String from,
                               Model model) {
        return render(patientApi.viewReport(id, from, model), model, "patient/report-detail");
    }

    @GetMapping("/reports/{id}/preview")
    public String reportPreview(@PathVariable Long id) {
        return "redirect:/patient/reports/" + id;
    }

    @GetMapping("/reports/{id}/download")
    public void downloadReport(@PathVariable Long id, HttpServletResponse response) {
        patientApi.downloadReportPdf(id, response);
    }

    @GetMapping("/hospitalizations")
    public String hospitalizations(Model model) {
        return render(patientApi.myHospitalizations(model), model, "patient/hospitalizations");
    }

    // 兼容旧页面：患者端不再支持直接发起住院申请
    @GetMapping("/hospitalizations/request")
    public String hospitalizationRequestLegacy(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "住院申请请由医生诊断后发起。");
        return "redirect:/patient/hospitalizations";
    }

    @GetMapping("/hospitalizations/{id}/detail")
    public String hospitalizationDetail(@PathVariable Long id, Model model) {
        return render(patientApi.hospitalizationDetail(id, model), model, "patient/hospitalization-detail");
    }

    @PostMapping("/hospitalizations/{id}/request-discharge")
    public String requestDischarge(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        ApiResponse<Object> resp = unwrap(patientApi.requestDischarge(id, redirectAttributes));
        redirectAttributes.addFlashAttribute(resp.getCode() == 200 ? "success" : "error", resp.getMsg());
        return "redirect:/patient/hospitalizations";
    }

    // 兼容旧模板 action：拦截并回到住院列表，避免 404
    @PostMapping("/hospitalizations/request")
    public String submitHospitalizationRequestLegacy(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "住院申请请由医生诊断后发起。");
        return "redirect:/patient/hospitalizations";
    }

    @PostMapping("/hospitalizations/{id}/cancel")
    public String cancelHospitalization(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        ApiResponse<Object> resp = unwrap(patientApi.cancelHospitalization(id, redirectAttributes));
        redirectAttributes.addFlashAttribute(resp.getCode() == 200 ? "success" : "error", resp.getMsg());
        return "redirect:/patient/hospitalizations";
    }

    @PostMapping("/hospitalizations/{id}/pay")
    public String payHospitalization(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        ApiResponse<Object> resp = unwrap(patientApi.payHospitalization(id, redirectAttributes));
        redirectAttributes.addFlashAttribute(resp.getCode() == 200 ? "success" : "error", resp.getMsg());
        return "redirect:/patient/hospitalizations";
    }

    @GetMapping("/payments/hospitalization/{id}/alipay")
    public String hospitalizationAlipay(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return render(patientApi.hospitalizationAlipaySandbox(id, model, redirectAttributes), model, "patient/alipay-sandbox");
    }

    @GetMapping("/payments/history")
    public String paymentHistory(Model model) {
        return render(patientApi.paymentHistory(model), model, "patient/payment-history");
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        return render(patientApi.profile(model), model, "patient/profile");
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam(required = false) String realName,
                                @RequestParam(required = false) String email,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String idCard,
                                @RequestParam(required = false) String gender,
                                @RequestParam(required = false) String birthday,
                                @RequestParam(required = false) String address,
                                @RequestParam(required = false) String emergencyContact,
                                @RequestParam(required = false) String emergencyPhone,
                                RedirectAttributes redirectAttributes) {
        ApiResponse<Object> resp = unwrap(patientApi.updateProfile(
                realName, email, phone, idCard, gender, birthday, address, emergencyContact, emergencyPhone
        ));
        redirectAttributes.addFlashAttribute(resp.getCode() == 200 ? "success" : "error", resp.getMsg());
        return "redirect:/patient/profile";
    }

    @PostMapping("/ai/triage")
    @ResponseBody
    public Map<String, String> triage(@RequestParam(required = false) String description) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.isAuthenticated()) ? auth.getName() : "unknown";
        AiSuggestionService.TriageResult r = aiSuggestionService.buildTriageSuggestion(
                description,
                "patient:" + username,
                "patient.triage"
        );
        Map<String, String> resp = new HashMap<>();
        resp.put("department", r.department);
        resp.put("urgency", r.urgency);
        resp.put("advice", r.advice);
        return resp;
    }

    private String render(ResponseEntity<? extends ApiResponse<?>> response, Model model, String viewName) {
        ApiResponse<?> api = response.getBody();
        if (api == null) {
            model.addAttribute("error", "页面数据加载失败");
            return viewName;
        }
        if (api.getCode() == 401) {
            return "redirect:/login/user";
        }
        if (api.getCode() != 200) {
            model.addAttribute("error", api.getMsg());
            return viewName;
        }
        Object data = api.getData();
        if (data instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String key) {
                    model.addAttribute(key, entry.getValue());
                }
            }
        } else if (data != null) {
            model.addAttribute("data", data);
        }
        return viewName;
    }

    @SuppressWarnings("unchecked")
    private ApiResponse<Object> unwrap(ResponseEntity<? extends ApiResponse<?>> response) {
        ApiResponse<?> api = response.getBody();
        if (api == null) {
            return ApiResponse.error("请求失败");
        }
        return (ApiResponse<Object>) api;
    }
}
