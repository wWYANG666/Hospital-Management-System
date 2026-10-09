package xmu.edu.yiyuan.entity;

import java.math.BigDecimal;

/**
 * 病床信息实体类
 */
public class Bed {
    private Long id;
    private String bedNumber;
    private String roomNumber;
    private String ward;
    private Long departmentId;
    private BedType bedType;
    private BedStatus status;
    private BigDecimal pricePerDay;

    public enum BedType {
        GENERAL, VIP, ICU;

        public String getChineseName() {
            return switch (this) {
                case GENERAL -> "普通";
                case VIP -> "VIP";
                case ICU -> "ICU";
            };
        }
    }

    public enum BedStatus {
        AVAILABLE, OCCUPIED, MAINTENANCE;

        public String getChineseName() {
            return switch (this) {
                case AVAILABLE -> "空闲";
                case OCCUPIED -> "占用";
                case MAINTENANCE -> "维修";
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

    public String getBedNumber() {
        return bedNumber;
    }

    public void setBedNumber(String bedNumber) {
        this.bedNumber = bedNumber;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public BedType getBedType() {
        return bedType;
    }

    public void setBedType(BedType bedType) {
        this.bedType = bedType;
    }

    public BedStatus getStatus() {
        return status;
    }

    public void setStatus(BedStatus status) {
        this.status = status;
    }

    public BigDecimal getPricePerDay() {
        return pricePerDay;
    }

    public void setPricePerDay(BigDecimal pricePerDay) {
        this.pricePerDay = pricePerDay;
    }

    public String getWard() {
        return ward;
    }

    public void setWard(String ward) {
        this.ward = ward;
    }
}
