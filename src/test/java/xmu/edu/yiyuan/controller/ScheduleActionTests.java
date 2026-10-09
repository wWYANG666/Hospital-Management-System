package xmu.edu.yiyuan.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import xmu.edu.yiyuan.entity.Schedule;
import xmu.edu.yiyuan.service.ScheduleService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScheduleActionTests {
    private final ScheduleService service = mock(ScheduleService.class);
    private final AdminController controller = new AdminController();

    ScheduleActionTests() {
        ReflectionTestUtils.setField(controller, "scheduleService", service);
    }

    private Schedule given(boolean historical, int status) {
        Schedule s = new Schedule();
        s.setId(1L);
        s.setWorkDate(LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(historical ? -1 : 1));
        s.setWorkTime(Schedule.WorkTime.MORNING);
        s.setStatus(status);
        when(service.findById(1L)).thenReturn(Optional.of(s));
        return s;
    }

    @Test
    void historicalLeaveApprovalDoesNotChangeStoredStatus() {
        Schedule s = given(true, 2);
        var flash = new RedirectAttributesModelMap();
        controller.approveScheduleLeave(1L, null, null, flash);
        assertTrue(flash.getFlashAttributes().containsKey("error"));
        assertEquals(2, s.getStatus());
        verify(service, never()).update(any());
    }

    @Test
    void historicalRejectionAndRestorationAreRejected() {
        Schedule s = given(true, 0);
        var reject = new RedirectAttributesModelMap();
        var restore = new RedirectAttributesModelMap();
        controller.rejectScheduleLeave(1L, null, null, reject);
        controller.restoreSchedule(1L, null, null, restore);
        assertTrue(reject.getFlashAttributes().containsKey("error"));
        assertTrue(restore.getFlashAttributes().containsKey("error"));
        assertEquals(0, s.getStatus());
        verify(service, never()).update(any());
    }

    @Test
    void cannotApproveAnActiveScheduleWithoutPendingLeave() {
        Schedule s = given(false, 1);
        var flash = new RedirectAttributesModelMap();
        controller.approveScheduleLeave(1L, null, null, flash);
        assertTrue(flash.getFlashAttributes().containsKey("error"));
        assertEquals(1, s.getStatus());
        verify(service, never()).update(any());
    }

    @Test
    void futurePendingLeaveCanBeApproved() {
        Schedule s = given(false, 2);
        var flash = new RedirectAttributesModelMap();
        controller.approveScheduleLeave(1L, null, null, flash);
        assertEquals(0, s.getStatus());
        assertTrue(flash.getFlashAttributes().containsKey("success"));
        verify(service).update(s);
    }

    @Test
    void futureDisabledScheduleCanBeRestored() {
        Schedule s = given(false, 0);
        var flash = new RedirectAttributesModelMap();
        controller.restoreSchedule(1L, null, null, flash);
        assertEquals(1, s.getStatus());
        assertTrue(flash.getFlashAttributes().containsKey("success"));
        verify(service).update(s);
    }
}
