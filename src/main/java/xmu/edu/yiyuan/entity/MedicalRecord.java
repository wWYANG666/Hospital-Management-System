package xmu.edu.yiyuan.entity;

import java.time.LocalDateTime;

/**
 * 电子病历实体类
 */
public class MedicalRecord {
    private Long id;
    private Long appointmentId;
    private Long hospitalizationId;
    private Long patientId;
    private Long doctorId;
    private String diagnosis;
    private String prescription;
    private String examinationItems;
    private String medicalRecordContent;
    private String conditionUpdate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getPrescription() {
        return prescription;
    }

    public void setPrescription(String prescription) {
        this.prescription = prescription;
    }

    public String getExaminationItems() {
        return examinationItems;
    }

    public void setExaminationItems(String examinationItems) {
        this.examinationItems = examinationItems;
    }

    public String getMedicalRecordContent() {
        return medicalRecordContent;
    }

    public void setMedicalRecordContent(String medicalRecordContent) {
        this.medicalRecordContent = medicalRecordContent;
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

    public Long getHospitalizationId() {
        return hospitalizationId;
    }

    public void setHospitalizationId(Long hospitalizationId) {
        this.hospitalizationId = hospitalizationId;
    }

    public String getConditionUpdate() {
        return conditionUpdate;
    }

    public void setConditionUpdate(String conditionUpdate) {
        this.conditionUpdate = conditionUpdate;
    }
}
