package xmu.edu.yiyuan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/app";
    }

    @GetMapping("/boom")
    public String boom() {
        throw new RuntimeException("boom test: intentionally thrown");
    }

    @GetMapping("/login")
    public String login() {
        return "redirect:/app/login";
    }

    @GetMapping("/login/user")
    public String userLogin() {
        return "redirect:/app/login";
    }

    @GetMapping("/login/admin")
    public String adminLogin() {
        return "redirect:/app/login?role=ADMIN";
    }

    /** 医生端与患者共用同一套表单（POST /login），便于从首页或文档链接进入 */
    @GetMapping("/login/doctor")
    public String doctorLogin() {
        return "redirect:/app/login?role=DOCTOR";
    }
}
