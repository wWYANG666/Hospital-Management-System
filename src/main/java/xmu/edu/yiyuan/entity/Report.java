package xmu.edu.yiyuan.entity;

import java.time.LocalDateTime;

/**
 * 报告单实体类
 */
public class Report {
    private Long id;
    private Long appointmentId;
    private Long patientId;
    private Long doctorId;
    private Long examinationId;
    private String reportType;
    private String reportContent;
    private LocalDateTime reportDate;
    private ReportStatus status;
    /** 患者是否已缴纳检查/检验费用；未付款前医生不可审阅填写报告 */
    private Boolean paid;
    private LocalDateTime paidAt;

    public enum ReportStatus {
        PENDING, COMPLETED;

        public String getChineseName() {
            switch (this) {
                case PENDING:
                    return "待完成";
                case COMPLETED:
                    return "已完成";
                default:
                    return this.name();
            }
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

    public Long getExaminationId() {
        return examinationId;
    }

    public void setExaminationId(Long examinationId) {
        this.examinationId = examinationId;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getReportContent() {
        return reportContent;
    }

    public void setReportContent(String reportContent) {
        this.reportContent = reportContent;
    }

    public LocalDateTime getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDateTime reportDate) {
        this.reportDate = reportDate;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
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
}
