package ru.mokrischev.vendingsupply.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import ru.mokrischev.vendingsupply.model.enums.Role;

import java.io.IOException;
import java.util.Collection;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        for (GrantedAuthority authority : authorities) {
            if (authority.getAuthority().equals(Role.OWNER.name())) {
                response.sendRedirect("/admin/dashboard");
                return;
            } else if (authority.getAuthority().equals(Role.FRANCHISEE.name())) {
                response.sendRedirect("/franchisee/dashboard");
                return;
            }
        }
        response.sendRedirect("/");
    }
}
