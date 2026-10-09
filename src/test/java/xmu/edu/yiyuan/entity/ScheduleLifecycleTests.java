package xmu.edu.yiyuan.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import static org.junit.jupiter.api.Assertions.*;

class ScheduleLifecycleTests {
    private Schedule schedule(String date, Schedule.WorkTime time, int status) {
        Schedule schedule = new Schedule();
        schedule.setWorkDate(LocalDate.parse(date));
        schedule.setWorkTime(time);
        schedule.setStatus(status);
        return schedule;
    }

    @Test
    void historicalSchedulesAreEndedRegardlessOfConfiguration() {
        ZonedDateTime now = ZonedDateTime.parse("2026-10-08T09:00:00+08:00");
        for (int config : new int[]{0, 1, 2}) {
            Schedule s = schedule("2026-03-17", Schedule.WorkTime.EVENING, config);
            assertEquals(Schedule.DisplayStatus.ENDED, s.displayStatusAt(now));
            assertEquals(config, s.getStatus());
        }
    }

    @Test
    void currentDayEndsAtTheConfiguredShiftBoundary() {
        Schedule morning = schedule("2026-10-08", Schedule.WorkTime.MORNING, 1);
        Schedule afternoon = schedule("2026-10-08", Schedule.WorkTime.AFTERNOON, 1);
        Schedule evening = schedule("2026-10-08", Schedule.WorkTime.EVENING, 1);
        assertEquals(Schedule.DisplayStatus.ACTIVE, morning.displayStatusAt(ZonedDateTime.parse("2026-10-08T11:59:59+08:00")));
        assertEquals(Schedule.DisplayStatus.ENDED, morning.displayStatusAt(ZonedDateTime.parse("2026-10-08T12:00:00+08:00")));
        assertEquals(Schedule.DisplayStatus.ACTIVE, afternoon.displayStatusAt(ZonedDateTime.parse("2026-10-08T16:59:59+08:00")));
        assertEquals(Schedule.DisplayStatus.ENDED, afternoon.displayStatusAt(ZonedDateTime.parse("2026-10-08T17:00:00+08:00")));
        assertEquals(Schedule.DisplayStatus.ACTIVE, evening.displayStatusAt(ZonedDateTime.parse("2026-10-08T21:29:59+08:00")));
        assertEquals(Schedule.DisplayStatus.ENDED, evening.displayStatusAt(ZonedDateTime.parse("2026-10-08T21:30:00+08:00")));
    }

    @Test
    void futureSchedulesKeepConfigurationStatus() {
        ZonedDateTime now = ZonedDateTime.parse("2026-10-08T22:00:00+08:00");
        assertEquals(Schedule.DisplayStatus.ACTIVE, schedule("2026-10-09", Schedule.WorkTime.MORNING, 1).displayStatusAt(now));
        assertEquals(Schedule.DisplayStatus.LEAVE_PENDING, schedule("2026-10-09", Schedule.WorkTime.MORNING, 2).displayStatusAt(now));
        assertEquals(Schedule.DisplayStatus.DISABLED, schedule("2026-10-09", Schedule.WorkTime.MORNING, 0).displayStatusAt(now));
    }

    @Test
    void usesShanghaiDateInsteadOfTheCallersZone() {
        Schedule s = schedule("2026-10-08", Schedule.WorkTime.MORNING, 1);
        assertEquals(Schedule.DisplayStatus.ACTIVE, s.displayStatusAt(ZonedDateTime.parse("2026-10-07T16:00:00Z")));
        assertEquals(Schedule.DisplayStatus.ENDED, s.displayStatusAt(ZonedDateTime.parse("2026-10-08T04:00:00Z")));
    }

    @Test
    void missingDateIsNotReportedAsActive() {
        Schedule s = new Schedule();
        s.setStatus(1);
        assertEquals(Schedule.DisplayStatus.UNKNOWN, s.displayStatusAt(ZonedDateTime.parse("2026-10-08T09:00:00+08:00")));
    }
}
