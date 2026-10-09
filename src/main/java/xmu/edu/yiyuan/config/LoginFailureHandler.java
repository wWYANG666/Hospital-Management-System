package xmu.edu.yiyuan.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String reason = "bad_credentials";
        if (exception instanceof DisabledException) {
            reason = "disabled";
        } else if (exception instanceof LockedException) {
            reason = "locked";
        } else if (exception instanceof CredentialsExpiredException) {
            reason = "expired";
        } else if (exception instanceof BadCredentialsException) {
            reason = "bad_credentials";
        }

        String loginType = request.getParameter("loginType");
        String target = "/login";
        if ("admin".equals(loginType)) {
            target = "/login/admin";
        } else if ("user".equals(loginType)) {
            target = "/login/user";
        }

        response.sendRedirect(target + "?error=true&reason=" + reason);
    }
}
