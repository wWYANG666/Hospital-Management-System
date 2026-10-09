package xmu.edu.yiyuan.config;

import org.junit.jupiter.api.Test;
import xmu.edu.yiyuan.entity.User;
import static org.junit.jupiter.api.Assertions.*;

class PortalSettingsTests {
    private final PortalSettings patient = new PortalSettings("patient");
    private final PortalSettings staff = new PortalSettings("staff");

    @Test
    void loginRolesAreSeparated() {
        assertTrue(patient.accepts(User.Role.PATIENT));
        assertFalse(patient.accepts(User.Role.DOCTOR));
        assertFalse(patient.accepts(User.Role.ADMIN));
        assertFalse(staff.accepts(User.Role.PATIENT));
        assertTrue(staff.accepts(User.Role.DOCTOR));
        assertTrue(staff.accepts(User.Role.ADMIN));
    }

    @Test
    void differentCookiesAndAssetPrefixes() {
        assertEquals("/spa/patient/", patient.assetPrefix());
        assertEquals("/spa/staff/", staff.assetPrefix());
        assertNotEquals(patient.csrfCookieName(), staff.csrfCookieName());
    }

    @Test
    void patientCannotAccessStaffInterfacesOrOldPages() {
        for (String path : new String[]{"/api/doctor/schedules", "/api/admin/dashboard", "/api/auth/register/doctor",
                "/app/doctor/dashboard", "/app/admin/profile", "/doctor/profile", "/admin/patients",
                "/register/doctor", "/login/admin", "/spa/staff/index.html", "/spa/index.html"}) {
            assertTrue(patient.deniesPath(path), path);
        }
        for (String path : new String[]{"/api/patient/dashboard", "/api/public/directory", "/app/patient/profile",
                "/api/auth/login", "/spa/patient/assets/index.js"}) assertFalse(patient.deniesPath(path), path);
    }

    @Test
    void staffCannotAccessPatientInterfacesOrOldPages() {
        for (String path : new String[]{"/api/patient/dashboard", "/api/public/directory", "/api/auth/register/patient",
                "/app/patient/appointments", "/app/doctors", "/app/departments", "/app/guide", "/patient/profile",
                "/register/patient", "/spa/patient/index.html", "/spa/index.html"}) assertTrue(staff.deniesPath(path), path);
        for (String path : new String[]{"/api/doctor/diagnose", "/api/admin/patients", "/api/auth/register/doctor",
                "/api/auth/registration-options", "/app/admin/dashboard", "/api/auth/login", "/spa/staff/assets/index.js"}) {
            assertFalse(staff.deniesPath(path), path);
        }
    }

    @Test
    void invalidModeFailsStartup() {
        assertThrows(IllegalArgumentException.class, () -> new PortalSettings("mixed"));
    }
}
