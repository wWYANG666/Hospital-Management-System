package xmu.edu.yiyuan.entity;

import java.time.LocalDateTime;

/**
 * 处方实体类
 */
public class Prescription {
    private Long id;
    private Long appointmentId; // 挂号ID
    private Long patientId;
    private Long doctorId;
    private String prescriptionNumber; // 处方编号
    private PrescriptionStatus status; // 状态：待发药/已发药/已取消
    private LocalDateTime createdAt;
    private LocalDateTime dispensedAt; // 发药时间
    private Long dispensedBy; // 发药人ID
    private String patientSignature; // 患者签字确认
    private Boolean paid; // 是否已付款
    private LocalDateTime paidAt; // 付款时间
    private LocalDateTime collectedAt; // 取药确认时间（患者签字时间）

    public enum PrescriptionStatus {
        PENDING, // 待发药
        DISPENSED, // 已发药
        CANCELLED // 已取消
        ;

        /** 管理端 / 药房展示 */
        public String getChineseName() {
            return switch (this) {
                case PENDING -> "待发药";
                case DISPENSED -> "已发药";
                case CANCELLED -> "已取消";
            };
        }

        /** 患者端展示 */
        public String getPatientChineseName() {
            return switch (this) {
                case PENDING -> "待取药";
                case DISPENSED -> "已取药";
                case CANCELLED -> "已取消";
            };
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public String getPrescriptionNumber() {
        return prescriptionNumber;
    }

    public void setPrescriptionNumber(String prescriptionNumber) {
        this.prescriptionNumber = prescriptionNumber;
    }

    public PrescriptionStatus getStatus() {
        return status;
    }

    public void setStatus(PrescriptionStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getDispensedAt() {
        return dispensedAt;
    }

    public void setDispensedAt(LocalDateTime dispensedAt) {
        this.dispensedAt = dispensedAt;
    }

    public Long getDispensedBy() {
        return dispensedBy;
    }

    public void setDispensedBy(Long dispensedBy) {
        this.dispensedBy = dispensedBy;
    }

    public String getPatientSignature() {
        return patientSignature;
    }

    public void setPatientSignature(String patientSignature) {
        this.patientSignature = patientSignature;
    }

    public Boolean getPaid() {
        return paid;
    }

    public void setPaid(Boolean paid) {
        this.paid = paid;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(LocalDateTime collectedAt) {
        this.collectedAt = collectedAt;
    }
}
