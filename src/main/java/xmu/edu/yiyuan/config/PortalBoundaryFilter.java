package xmu.edu.yiyuan.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/** Enforces the portal boundary even before a user has logged in. */
public class PortalBoundaryFilter extends OncePerRequestFilter {
    private final PortalSettings portal;

    public PortalBoundaryFilter(PortalSettings portal) { this.portal = portal; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (portal.deniesPath(request.getServletPath())) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"msg\":\"此入口不提供该端的服务\",\"data\":null}");
            return;
        }
        chain.doFilter(request, response);
    }
}
