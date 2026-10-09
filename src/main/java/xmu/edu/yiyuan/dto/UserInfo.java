package xmu.edu.yiyuan.dto;

import xmu.edu.yiyuan.entity.User;

import java.util.Map;

/**
 * 用户信息 DTO
 */
public class UserInfo {
    private Long id;
    private String username;
    private String realName;
    private String role;
    private Map<String, Object> additionalInfo; // 患者/医生/管理员的额外信息

    public UserInfo() {
    }

    public UserInfo(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.realName = user.getRealName();
        this.role = user.getRole() != null ? user.getRole().name() : null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Map<String, Object> getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(Map<String, Object> additionalInfo) {
        this.additionalInfo = additionalInfo;
    }
}
