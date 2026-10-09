package xmu.edu.yiyuan.service;

import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class BusinessValidation {
    private BusinessValidation() {}
    public static void require(boolean condition, String message) {
        if (!condition) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
    public static void text(String value, int max, String label) {
        require(value != null && !value.isBlank() && value.length() <= max, label + "不能为空且长度不能超过" + max);
    }
    public static void optional(String value, int max, String label) {
        require(value == null || value.length() <= max, label + "长度不能超过" + max);
    }
    public static void money(BigDecimal value, String label) {
        require(value != null && value.signum() >= 0 && value.compareTo(new BigDecimal("99999999.99")) <= 0, label + "必须为有效的非负金额");
    }
    public static void stock(Integer value, String label) {
        require(value != null && value >= 0 && value <= 1000000, label + "必须为0至1000000的整数");
    }
}
