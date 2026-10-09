package xmu.edu.yiyuan.controller.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;
import xmu.edu.yiyuan.dto.ApiResponse;

@RestControllerAdvice(basePackages = "xmu.edu.yiyuan.controller.api")
public class ApiExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiResponse<Object>> invalid(Exception ex) {
        return ApiSupport.error(400, "输入格式不正确，请检查必填项、日期和数量");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> denied(Exception ex) {
        return ApiSupport.error(403, "没有权限访问这条记录");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Object>> status(ResponseStatusException ex) {
        return ApiSupport.error(ex.getStatusCode().value(), ex.getReason() == null ? "请求无法完成" : ex.getReason());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> conflict(Exception ex) {
        LOG.warn("Business data constraint rejected an API operation", ex);
        return ApiSupport.error(409, "记录已存在或仍被其他业务使用，请检查后重试");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> unexpected(Exception ex) {
        LOG.error("API operation failed", ex);
        return ApiSupport.error(500, "服务暂时不可用，请稍后重试");
    }
}
