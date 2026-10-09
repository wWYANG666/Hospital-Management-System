package xmu.edu.yiyuan.dto;

import xmu.edu.yiyuan.entity.Doctor;
import xmu.edu.yiyuan.entity.User;

/**
 * 管理仪表盘：待审核医生展示用
 */
public class PendingDoctorApprovalView {
    private final Doctor doctor;
    private final User user;
    /** 与医生科室名称匹配的科室 ID，用于快速审批；为空时需去科室管理处理 */
    private final Long departmentId;

    public PendingDoctorApprovalView(Doctor doctor, User user, Long departmentId) {
        this.doctor = doctor;
        this.user = user;
        this.departmentId = departmentId;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public User getUser() {
        return user;
    }

    public Long getDepartmentId() {
        return departmentId;
    }
}
