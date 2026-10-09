package xmu.edu.yiyuan.service;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PharmacyServiceTests {
    @Test
    void shortageDoesNotWriteAnyStockOrPrescription() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForList("SELECT * FROM prescription WHERE id=? FOR UPDATE", 1L))
                .thenReturn(List.of(Map.of("status", "PENDING", "paid", true, "patient_signature", "Test Patient")));
        when(jdbc.queryForList("SELECT medicine_id,SUM(quantity) quantity FROM prescription_item WHERE prescription_id=? GROUP BY medicine_id ORDER BY medicine_id", 1L))
                .thenReturn(List.of(Map.of("medicine_id", 2L, "quantity", 9)));
        when(jdbc.queryForList("SELECT stock,status,name FROM medicine WHERE id=? FOR UPDATE", 2L))
                .thenReturn(List.of(Map.of("stock", 8, "status", 1, "name", "Test Medicine")));
        ResponseStatusException failure = assertThrows(ResponseStatusException.class, () -> new PharmacyService(jdbc).dispense(1L, 10L));
        assertEquals(409, failure.getStatusCode().value());
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void dispensedPrescriptionIsRejectedBeforeStockRead() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForList("SELECT * FROM prescription WHERE id=? FOR UPDATE", 1L))
                .thenReturn(List.of(Map.of("status", "DISPENSED")));
        assertThrows(ResponseStatusException.class, () -> new PharmacyService(jdbc).dispense(1L, 10L));
        verify(jdbc, never()).queryForList("SELECT medicine_id,SUM(quantity) quantity FROM prescription_item WHERE prescription_id=? GROUP BY medicine_id ORDER BY medicine_id", 1L);
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }
}
