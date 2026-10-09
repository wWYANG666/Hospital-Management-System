package xmu.edu.yiyuan.controller.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
import xmu.edu.yiyuan.dto.ApiResponse;
import xmu.edu.yiyuan.entity.User;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ApiSupportTests {
    @Test
    void stripsPasswordsFromNestedUserRecordsAndPreservesEnums() {
        User user = new User();
        user.setRealName("Synthetic User");
        user.setPassword("must-never-be-exposed");
        user.setRole(User.Role.PATIENT);
        Map<?, ?> payload = (Map<?, ?>) ApiSupport.sanitize(Map.of("userMap", Map.of(1L, user)));
        Map<?, ?> sanitized = (Map<?, ?>) ((Map<?, ?>) payload.get("userMap")).get("1");
        assertFalse(sanitized.containsKey("password"));
        assertEquals("Synthetic User", sanitized.get("realName"));
        assertEquals("PATIENT", sanitized.get("role"));
    }

    @Test
    void preservesForbiddenStatusInsteadOfRenderingEmptyDetails() {
        var response = ApiSupport.response(ResponseEntity.ok(ApiResponse.forbidden("no access")), new ExtendedModelMap());
        assertEquals(403, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertNull(response.getBody().getData());
    }
}
