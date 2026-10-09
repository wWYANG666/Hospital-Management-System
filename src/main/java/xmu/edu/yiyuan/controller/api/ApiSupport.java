package xmu.edu.yiyuan.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import xmu.edu.yiyuan.dto.ApiResponse;

import java.beans.Introspector;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.time.temporal.TemporalAccessor;
import java.util.*;

/** JSON boundary for the existing business services during the frontend migration. */
public final class ApiSupport {
    private ApiSupport() {}

    public static ResponseEntity<ApiResponse<Object>> response(
            ResponseEntity<? extends ApiResponse<?>> legacy, Model model) {
        ApiResponse<?> body = legacy.getBody();
        if (body == null) return error(500, "页面数据加载失败，请重试");
        if (body.getCode() != 200) return error(body.getCode(), body.getMsg());
        Map<String, Object> data = new LinkedHashMap<>();
        if (model != null) data.putAll(model.asMap());
        if (body.getData() instanceof Map<?, ?> map) {
            map.forEach((key, value) -> data.put(String.valueOf(key), value));
        } else if (body.getData() != null) {
            data.put("data", body.getData());
        }
        return ResponseEntity.ok(ApiResponse.success(body.getMsg(), sanitize(data)));
    }

    public static ResponseEntity<ApiResponse<Object>> page(String view, Model model, RedirectAttributes redirect) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (model != null) data.putAll(model.asMap());
        Map<String, ?> flash = redirect == null ? Map.of() : redirect.getFlashAttributes();
        Object error = flash.containsKey("error") ? flash.get("error") : data.get("error");
        if (error != null && !Boolean.FALSE.equals(error)) return error(400, String.valueOf(error));
        if (view != null && view.startsWith("redirect:")) {
            String target = view.substring(9);
            if (target.startsWith("/login")) return error(401, "请重新登录");
            if (target.contains("unpaidReport")) return error(400, "请先完成检查缴费，再填写报告");
            if (target.matches(".*[?&]error=true(?:&.*)?$")) return error(400, "操作未完成，请检查填写内容");
            data.put("__redirect", target);
        }
        Object message = flash.get("success");
        if (message == null) message = flash.get("info");
        if (!flash.isEmpty()) data.put("flash", flash);
        return ResponseEntity.ok(ApiResponse.success(message == null ? "success" : String.valueOf(message), sanitize(data)));
    }

    public static ResponseEntity<ApiResponse<Object>> error(int code, String message) {
        int status = code >= 400 && code <= 599 ? code : 400;
        return ResponseEntity.status(status).body(ApiResponse.error(status, message));
    }

    public static Object sanitize(Object value) {
        return sanitize(value, new IdentityHashMap<>());
    }

    private static Object sanitize(Object value, IdentityHashMap<Object, Boolean> seen) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) return value;
        if (value instanceof Enum<?> e) return e.name();
        if (value instanceof TemporalAccessor || value instanceof Date || value instanceof UUID) return value.toString();
        if (value instanceof Optional<?> optional) return sanitize(optional.orElse(null), seen);
        if (seen.put(value, true) != null) return null;
        try {
            if (value instanceof Map<?, ?> map) {
                Map<String, Object> copy = new LinkedHashMap<>();
                map.forEach((key, item) -> {
                    String name = String.valueOf(key);
                    if (!sensitive(name) && !name.startsWith("org.springframework.")) copy.put(name, sanitize(item, seen));
                });
                return copy;
            }
            if (value instanceof Iterable<?> items) {
                List<Object> copy = new ArrayList<>();
                items.forEach(item -> copy.add(sanitize(item, seen)));
                return copy;
            }
            if (value.getClass().isArray()) {
                List<Object> copy = new ArrayList<>();
                for (int i = 0; i < Array.getLength(value); i++) copy.add(sanitize(Array.get(value, i), seen));
                return copy;
            }
            Map<String, Object> copy = new LinkedHashMap<>();
            for (var descriptor : Introspector.getBeanInfo(value.getClass(), Object.class).getPropertyDescriptors()) {
                var getter = descriptor.getReadMethod();
                if (getter != null && !sensitive(descriptor.getName())) {
                    copy.put(descriptor.getName(), sanitize(getter.invoke(value), seen));
                }
            }
            for (var field : value.getClass().getFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && !sensitive(field.getName())) {
                    copy.putIfAbsent(field.getName(), sanitize(field.get(value), seen));
                }
            }
            return copy;
        } catch (ReflectiveOperationException | java.beans.IntrospectionException ex) {
            throw new IllegalStateException("无法读取业务数据", ex);
        } finally {
            seen.remove(value);
        }
    }

    private static boolean sensitive(String name) {
        return Set.of("password", "passwordHash", "credentials", "apiKey").contains(name);
    }
}
