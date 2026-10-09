package xmu.edu.yiyuan.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import xmu.edu.yiyuan.entity.User;

@Component
public class PortalSettings {
    private final String mode;

    public PortalSettings(@Value("${hospital.portal.mode:patient}") String mode) {
        if (!"patient".equals(mode) && !"staff".equals(mode)) {
            throw new IllegalArgumentException("hospital.portal.mode must be patient or staff");
        }
        this.mode = mode;
    }

    public String getMode() { return mode; }
    public boolean isPatient() { return "patient".equals(mode); }
    public String assetPrefix() { return "/spa/" + mode + "/"; }
    public String csrfCookieName() { return isPatient() ? "PATIENT_XSRF_TOKEN" : "STAFF_XSRF_TOKEN"; }

    public boolean accepts(User.Role role) {
        return isPatient() ? role == User.Role.PATIENT : role == User.Role.DOCTOR || role == User.Role.ADMIN;
    }

    public boolean accepts(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream().anyMatch(authority ->
                isPatient() ? "ROLE_PATIENT".equals(authority.getAuthority())
                        : "ROLE_DOCTOR".equals(authority.getAuthority()) || "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    public boolean deniesPath(String path) {
        if (path.startsWith("/spa/")) return !path.startsWith(assetPrefix());
        if (isPatient()) {
            return path.startsWith("/api/doctor/") || path.startsWith("/api/admin/")
                    || path.equals("/api/auth/register/doctor") || path.equals("/api/auth/registration-options")
                    || path.equals("/app/doctor") || path.startsWith("/app/doctor/")
                    || path.equals("/app/admin") || path.startsWith("/app/admin/")
                    || path.equals("/doctor") || path.startsWith("/doctor/")
                    || path.equals("/admin") || path.startsWith("/admin/")
                    || path.equals("/login/admin") || path.equals("/login/doctor") || path.equals("/register/doctor");
        }
        return path.startsWith("/api/patient/") || path.startsWith("/api/public/")
                || path.equals("/api/auth/register/patient")
                || path.equals("/app/patient") || path.startsWith("/app/patient/")
                || path.equals("/patient") || path.startsWith("/patient/")
                || path.equals("/app/departments") || path.equals("/app/doctors") || path.equals("/app/guide")
                || path.equals("/register/patient");
    }
}
