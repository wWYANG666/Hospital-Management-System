package xmu.edu.yiyuan.config;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PortalSettings portal;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfCookies = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfCookies.setCookieName(portal.csrfCookieName());
        csrfCookies.setCookiePath("/");
        http
            .userDetailsService(userDetailsService)
            .addFilterBefore(new PortalBoundaryFilter(portal), AuthorizationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // 允许错误派发通过（避免 500 时 /error 被安全拦截而出现 Whitelabel）
                .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                .requestMatchers("/app", "/app/**", "/spa/**", "/api/auth/**", "/api/public/**").permitAll()
                .requestMatchers("/api/patient/**").hasRole("PATIENT")
                .requestMatchers("/api/doctor/**").hasRole("DOCTOR")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/register/**").denyAll()
                .requestMatchers(HttpMethod.POST, "/login").denyAll()
                // 允许访问的路径
                .requestMatchers("/", "/boom", "/error", "/error/**", "/login/**", "/register/**", "/css/**", "/js/**", "/images/**").permitAll()
                // 原有的页面路径（保留向后兼容）
                .requestMatchers(HttpMethod.GET, "/patient/**").hasAnyRole("PATIENT", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/doctor/**").hasAnyRole("DOCTOR", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/admin/**").hasRole("ADMIN")
                .requestMatchers("/patient/**", "/doctor/**", "/admin/**").denyAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form.disable())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/?logout")
                .permitAll()
            )
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, ex) -> {
                    if (request.getServletPath().startsWith("/api/")) {
                        response.setStatus(401);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"code\":401,\"msg\":\"请先登录\",\"data\":null}");
                    } else response.sendRedirect("/app/login");
                })
                .accessDeniedHandler((request, response, ex) -> {
                    if (request.getServletPath().startsWith("/api/")) {
                        response.setStatus(403);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"code\":403,\"msg\":\"没有操作权限或登录已过期，请刷新后重试\",\"data\":null}");
                    } else response.sendError(403);
                }))
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfCookies)
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()));

        return http.build();
    }
}
