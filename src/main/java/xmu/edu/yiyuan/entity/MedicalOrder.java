package xmu.edu.yiyuan.entity;

import java.time.LocalDateTime;

/**
 * 医嘱实体类
 */
public class MedicalOrder {
    private Long id;
    private Long hospitalizationId;
    private Long patientId;
    private Long doctorId;
    private OrderType orderType;
    private String orderContent;
    private Long medicineId;
    /** 用药医嘱时的药品数量（盒/支等），默认 1 */
    private Integer quantity;
    private Long examinationId;
    private String dosage;
    private String frequency;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum OrderType {
        MEDICATION, EXAMINATION, NURSING, OTHER;

        public String getChineseName() {
            return switch (this) {
                case MEDICATION -> "用药";
                case EXAMINATION -> "检查";
                case NURSING -> "护理";
                case OTHER -> "其他";
            };
        }
    }

    public enum OrderStatus {
        ACTIVE, COMPLETED, CANCELLED;

        public String getChineseName() {
            return switch (this) {
                case ACTIVE -> "执行中";
                case COMPLETED -> "已完成";
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

    public Long getHospitalizationId() {
        return hospitalizationId;
    }

    public void setHospitalizationId(Long hospitalizationId) {
        this.hospitalizationId = hospitalizationId;
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

    public OrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }

    public String getOrderContent() {
        return orderContent;
    }

    public void setOrderContent(String orderContent) {
        this.orderContent = orderContent;
    }

    public Long getMedicineId() {
        return medicineId;
    }

    public void setMedicineId(Long medicineId) {
        this.medicineId = medicineId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Long getExaminationId() {
        return examinationId;
    }

    public void setExaminationId(Long examinationId) {
        this.examinationId = examinationId;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
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
}
