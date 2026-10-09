package xmu.edu.yiyuan.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * 住院信息实体类
 */
public class Hospitalization {
    private Long id;
    private Long patientId;
    private Long doctorId;
    /** 发起住院申请时关联的挂号 ID，用于精确展示接诊诊断 */
    private Long appointmentId;
    private Long bedId;
    private LocalDate admissionDate;
    private LocalDate dischargeDate;
    private String diagnosis;
    private String admissionReason;
    private Integer expectedDays;
    private Long requestDepartmentId;
    private LocalDateTime requestTime;
    private RequestStatus requestStatus;
    private String dischargeDiagnosis;
    private String dischargeNotes;
    private HospitalizationStatus status;
    private BigDecimal totalCost;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // 是否已完成缴费（null 或 false 表示未支付，true 表示已支付）
    private Boolean paid;

    public enum HospitalizationStatus {
        ADMITTED, DISCHARGED;

        public String getChineseName() {
            return switch (this) {
                case ADMITTED -> "住院中";
                case DISCHARGED -> "已出院";
            };
        }
    }

    public enum RequestStatus {
        PENDING, APPROVED, REJECTED;

        public String getChineseName() {
            return switch (this) {
                case PENDING -> "待审批";
                case APPROVED -> "已通过";
                case REJECTED -> "已拒绝";
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

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public Long getBedId() {
        return bedId;
    }

    public void setBedId(Long bedId) {
        this.bedId = bedId;
    }

    public LocalDate getAdmissionDate() {
        return admissionDate;
    }

    public void setAdmissionDate(LocalDate admissionDate) {
        this.admissionDate = admissionDate;
    }

    public LocalDate getDischargeDate() {
        return dischargeDate;
    }

    public void setDischargeDate(LocalDate dischargeDate) {
        this.dischargeDate = dischargeDate;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public HospitalizationStatus getStatus() {
        return status;
    }

    public void setStatus(HospitalizationStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public Boolean getPaid() {
        return paid;
    }

    public void setPaid(Boolean paid) {
        this.paid = paid;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAdmissionReason() {
        return admissionReason;
    }

    public void setAdmissionReason(String admissionReason) {
        this.admissionReason = admissionReason;
    }

    public Integer getExpectedDays() {
        return expectedDays;
    }

    public void setExpectedDays(Integer expectedDays) {
        this.expectedDays = expectedDays;
    }

    public Long getRequestDepartmentId() {
        return requestDepartmentId;
    }

    public void setRequestDepartmentId(Long requestDepartmentId) {
        this.requestDepartmentId = requestDepartmentId;
    }

    public LocalDateTime getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(LocalDateTime requestTime) {
        this.requestTime = requestTime;
    }

    public RequestStatus getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(RequestStatus requestStatus) {
        this.requestStatus = requestStatus;
    }

    public String getDischargeDiagnosis() {
        return dischargeDiagnosis;
    }

    public void setDischargeDiagnosis(String dischargeDiagnosis) {
        this.dischargeDiagnosis = dischargeDiagnosis;
    }

    public String getDischargeNotes() {
        return dischargeNotes;
    }

    public void setDischargeNotes(String dischargeNotes) {
        this.dischargeNotes = dischargeNotes;
    }
}
