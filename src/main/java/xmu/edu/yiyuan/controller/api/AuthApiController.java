package xmu.edu.yiyuan.controller.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.bind.annotation.*;
import xmu.edu.yiyuan.controller.RegisterController;
import xmu.edu.yiyuan.dto.ApiResponse;
import xmu.edu.yiyuan.service.UserService;
import xmu.edu.yiyuan.service.DepartmentService;
import xmu.edu.yiyuan.config.PortalSettings;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private UserService userService;
    @Autowired private RegisterController registerController;
    @Autowired private PortalSettings portal;
    @Autowired private DepartmentService departments;
    private final HttpSessionSecurityContextRepository contexts = new HttpSessionSecurityContextRepository();

    @GetMapping("/session")
    public ApiResponse<Object> session(Authentication auth, CsrfToken token) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("user", auth == null ? null : userService.findByUsername(auth.getName()).map(ApiSupport::sanitize).orElse(null));
        data.put("csrfToken", token.getToken());
        data.put("portalMode", portal.getMode());
        return ApiResponse.success(data);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Object>> login(@RequestParam String username, @RequestParam String password,
            HttpServletRequest request, HttpServletResponse response) {
        try {
            Authentication auth = authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(username.trim(), password));
            if (!portal.accepts(auth)) {
                return ApiSupport.error(403, portal.isPatient() ? "此入口仅供患者登录，请使用医务工作台登录医生或管理员账号" : "此入口仅供医生和管理员登录，请使用患者服务平台");
            }
            if (request.getSession(false) != null) request.changeSessionId();
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            contexts.saveContext(context, request, response);
            Object user = userService.findByUsername(auth.getName()).map(ApiSupport::sanitize).orElse(null);
            return ResponseEntity.ok(ApiResponse.success("登录成功", user));
        } catch (DisabledException ex) {
            return ApiSupport.error(403, "账号待审核或已停用，请联系管理员");
        } catch (AuthenticationException ex) {
            return ApiSupport.error(401, "用户名或密码错误");
        }
    }

    @PostMapping("/logout")
    public ApiResponse<Object> logout(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ApiResponse.success("已退出登录", null);
    }

    @PostMapping("/register/patient")
    public ResponseEntity<ApiResponse<Object>> registerPatient(@RequestParam String username, @RequestParam String password,
            @RequestParam String realName, @RequestParam(required = false) String email, @RequestParam(required = false) String phone,
            @RequestParam(required = false) String idCard, @RequestParam(required = false) String gender,
            @RequestParam(required = false) String birthday, @RequestParam(required = false) String address,
            @RequestParam(required = false) String emergencyContact, @RequestParam(required = false) String emergencyPhone) {
        if (!portal.isPatient()) return ApiSupport.error(403, "请到患者服务平台注册");
        if (username.isBlank() || realName.isBlank() || password.length() < 6) return ApiSupport.error(400, "请填写姓名和账号，密码至少 6 位");
        var model = new ExtendedModelMap();
        registerController.registerPatient(username, password, realName, email, phone, idCard, gender, birthday, address, emergencyContact, emergencyPhone, model);
        if (Boolean.TRUE.equals(model.get("error"))) return ApiSupport.error(400, String.valueOf(model.get("message")));
        return ResponseEntity.ok(ApiResponse.success("患者账号注册成功，请登录", null));
    }

    @PostMapping("/register/doctor")
    public ResponseEntity<ApiResponse<Object>> registerDoctor(@RequestParam String username, @RequestParam String password,
            @RequestParam String realName, @RequestParam String department, @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone, @RequestParam(required = false) String title,
            @RequestParam(required = false) String specialty, @RequestParam(required = false) String introduction) {
        if (portal.isPatient()) return ApiSupport.error(403, "请到医务工作台提交医生账号申请");
        if (username.isBlank() || realName.isBlank() || password.length() < 6) return ApiSupport.error(400, "请填写姓名和账号，密码至少 6 位");
        var model = new ExtendedModelMap();
        registerController.registerDoctor(username, password, realName, email, phone, department, title, specialty, introduction, model);
        if (Boolean.TRUE.equals(model.get("error"))) return ApiSupport.error(400, String.valueOf(model.get("message")));
        return ResponseEntity.ok(ApiResponse.success("申请已提交，管理员审核通过后可登录", null));
    }

    @GetMapping("/registration-options")
    public ResponseEntity<ApiResponse<Object>> registrationOptions() {
        if (portal.isPatient()) return ApiSupport.error(403, "医生申请信息仅在医务端提供");
        return ResponseEntity.ok(ApiResponse.success(Map.of("departments", ApiSupport.sanitize(departments.findAll()))));
    }
}
