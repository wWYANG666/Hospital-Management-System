package xmu.edu.yiyuan.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * 排班信息实体类
 */
public class Schedule {
    private static final ZoneId HOSPITAL_ZONE = ZoneId.of("Asia/Shanghai");
    private Long id;
    private Long doctorId;
    private Long departmentId;
    private LocalDate workDate;
    private WorkTime workTime;
    private Integer maxAppointments;
    private Integer currentAppointments;
    private Integer status;
    private ScheduleType scheduleType;

    public enum DisplayStatus {
        ACTIVE, LEAVE_PENDING, DISABLED, ENDED, UNKNOWN
    }

    /** Lifecycle is derived from hospital time; the stored status keeps its configuration meaning. */
    public DisplayStatus getDisplayStatus() {
        return displayStatusAt(ZonedDateTime.now(HOSPITAL_ZONE));
    }

    public DisplayStatus displayStatusAt(ZonedDateTime now) {
        LocalDate today = now.withZoneSameInstant(HOSPITAL_ZONE).toLocalDate();
        if (workDate == null) return DisplayStatus.UNKNOWN;
        if (workDate.isBefore(today)) return DisplayStatus.ENDED;
        if (workDate.equals(today) && workTime != null) {
            LocalTime end = switch (workTime) {
                case MORNING -> LocalTime.of(12, 0);
                case AFTERNOON -> LocalTime.of(17, 0);
                case EVENING -> LocalTime.of(21, 30);
            };
            if (!now.withZoneSameInstant(HOSPITAL_ZONE).toLocalTime().isBefore(end)) return DisplayStatus.ENDED;
        }
        if (status == null) return DisplayStatus.UNKNOWN;
        return switch (status) {
            case 1 -> DisplayStatus.ACTIVE;
            case 2 -> DisplayStatus.LEAVE_PENDING;
            case 0 -> DisplayStatus.DISABLED;
            default -> DisplayStatus.UNKNOWN;
        };
    }

    public enum WorkTime {
        MORNING, AFTERNOON, EVENING;

        public String getChineseName() {
            return switch (this) {
                case MORNING -> "上午";
                case AFTERNOON -> "下午";
                case EVENING -> "晚上";
            };
        }
    }

    public enum ScheduleType {
        GENERAL, EXPERT;

        public String getChineseName() {
            return this == EXPERT ? "专家诊" : "普通门诊";
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
    }

    public WorkTime getWorkTime() {
        return workTime;
    }

    public void setWorkTime(WorkTime workTime) {
        this.workTime = workTime;
    }

    public Integer getMaxAppointments() {
        return maxAppointments;
    }

    public void setMaxAppointments(Integer maxAppointments) {
        this.maxAppointments = maxAppointments;
    }

    public Integer getCurrentAppointments() {
        return currentAppointments;
    }

    public void setCurrentAppointments(Integer currentAppointments) {
        this.currentAppointments = currentAppointments;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public ScheduleType getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(ScheduleType scheduleType) {
        this.scheduleType = scheduleType;
    }

    /** 排班启用状态：1 启用、0 停用、2 请假待审批 */
    public String getStatusChineseName() {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case 1 -> "启用";
            case 0 -> "停用";
            case 2 -> "请假待审批";
            default -> "未知";
        };
    }
}
