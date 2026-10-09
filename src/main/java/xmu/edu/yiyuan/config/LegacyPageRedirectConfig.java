package xmu.edu.yiyuan.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Existing bookmarks enter their matching Vue screen without rendering legacy templates. */
@Configuration
public class LegacyPageRedirectConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                if (!"GET".equals(request.getMethod())) return true;
                String path = request.getServletPath();
                String query = request.getQueryString();
                String suffix = query == null ? "" : "?" + query;
                if (path.startsWith("/register")) {
                    response.sendRedirect("/app/register" + (path.contains("doctor") ? "?role=DOCTOR" : ""));
                } else if (path.matches("/patient/reports/\\d+/download")) {
                    response.sendRedirect("/api" + path);
                } else {
                    response.sendRedirect("/app" + path + suffix);
                }
                return false;
            }
        }).addPathPatterns("/patient/**", "/doctor/**", "/admin/**", "/register", "/register/**");
    }
}
