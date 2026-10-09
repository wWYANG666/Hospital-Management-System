package xmu.edu.yiyuan.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import xmu.edu.yiyuan.entity.User;
import xmu.edu.yiyuan.entity.Doctor;
import xmu.edu.yiyuan.entity.Patient;
import xmu.edu.yiyuan.service.DepartmentService;
import xmu.edu.yiyuan.service.RegisterService;

@Controller
@RequestMapping("/register")
public class RegisterController {

    @Autowired
    private RegisterService registerService;

    @Autowired
    private DepartmentService departmentService;

    @GetMapping
    public String registerPage() {
        return "register/register-type";
    }

    @GetMapping("/patient")
    public String patientRegisterForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("patient", new Patient());
        return "register/patient-register";
    }

    @PostMapping("/patient")
    public String registerPatient(@RequestParam String username,
                                  @RequestParam String password,
                                  @RequestParam String realName,
                                  @RequestParam(required = false) String email,
                                  @RequestParam(required = false) String phone,
                                  @RequestParam(required = false) String idCard,
                                  @RequestParam(required = false) String gender,
                                  @RequestParam(required = false) String birthday,
                                  @RequestParam(required = false) String address,
                                  @RequestParam(required = false) String emergencyContact,
                                  @RequestParam(required = false) String emergencyPhone,
                                  Model model) {
        try {
            User user = new User();
            user.setUsername(username);
            user.setPassword(password);
            user.setRealName(realName);
            user.setEmail(email);
            user.setPhone(phone);
            user.setRole(User.Role.PATIENT);
            user.setStatus(1);

            Patient patient = new Patient();
            patient.setIdCard(idCard);
            if (gender != null && !gender.isEmpty()) {
                patient.setGender(Patient.Gender.valueOf(gender));
            }
            if (birthday != null && !birthday.isEmpty()) {
                patient.setBirthday(java.time.LocalDate.parse(birthday));
            }
            patient.setAddress(address);
            patient.setEmergencyContact(emergencyContact);
            patient.setEmergencyPhone(emergencyPhone);

            // 注册用户和患者信息
            registerService.registerPatient(user, patient);

            model.addAttribute("success", true);
            model.addAttribute("message", "患者账号注册成功！请登录。");
            return "register/register-success";
        } catch (Exception e) {
            model.addAttribute("error", true);
            model.addAttribute("message", "注册失败：" + e.getMessage());
            model.addAttribute("user", new User());
            model.addAttribute("patient", new Patient());
            return "register/patient-register";
        }
    }

    @GetMapping("/doctor")
    public String doctorRegisterForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("doctor", new Doctor());
        model.addAttribute("departments", departmentService.findAll());
        return "register/doctor-register";
    }

    @PostMapping("/doctor")
    public String registerDoctor(@RequestParam String username,
                                 @RequestParam String password,
                                 @RequestParam String realName,
                                 @RequestParam(required = false) String email,
                                 @RequestParam(required = false) String phone,
                                 @RequestParam String department,
                                 @RequestParam(required = false) String title,
                                 @RequestParam(required = false) String specialty,
                                 @RequestParam(required = false) String introduction,
                                 Model model) {
        try {
            User user = new User();
            user.setUsername(username);
            user.setPassword(password);
            user.setRealName(realName);
            user.setEmail(email);
            user.setPhone(phone);
            user.setRole(User.Role.DOCTOR);
            // 自助注册的医生账号默认待审核，管理员通过后才可登录
            user.setStatus(0);

            Doctor doctor = new Doctor();
            doctor.setDepartment(department);
            doctor.setTitle(title);
            doctor.setSpecialty(specialty);
            doctor.setIntroduction(introduction);

            // 注册用户和医生信息
            registerService.registerDoctor(user, doctor);

            model.addAttribute("success", true);
            model.addAttribute("message", "医生账号注册申请已提交，待管理员审核通过后方可登录。");
            return "register/register-success";
        } catch (Exception e) {
            model.addAttribute("error", true);
            model.addAttribute("message", "注册失败：" + e.getMessage());
            model.addAttribute("user", new User());
            model.addAttribute("doctor", new Doctor());
            model.addAttribute("departments", departmentService.findAll());
            return "register/doctor-register";
        }
    }
}
