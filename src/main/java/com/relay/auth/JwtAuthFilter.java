package com.relay.auth;

import com.relay.shared.WorkspaceContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Validates the Bearer token, binds the user to the SecurityContext and the workspace to
 * {@link WorkspaceContext} (so Postgres RLS scopes every query). Invalid/missing tokens simply
 * leave the request unauthenticated — SecurityConfig then rejects protected endpoints.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;

    public JwtAuthFilter(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        boolean bound = false;
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims c = jwt.parse(header.substring(7));
                UUID workspaceId = UUID.fromString(c.get("workspace", String.class));
                boolean admin = Boolean.TRUE.equals(c.get("admin", Boolean.class));
                AuthPrincipal principal = new AuthPrincipal(
                    UUID.fromString(c.getSubject()),
                    c.get("email", String.class),
                    c.get("name", String.class),
                    workspaceId,
                    c.get("workspaceName", String.class),
                    admin);
                var authorities = new java.util.ArrayList<SimpleGrantedAuthority>();
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                if (admin) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                }
                var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
                WorkspaceContext.set(workspaceId);
                bound = true;
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }
        try {
            chain.doFilter(request, response);
        } finally {
            if (bound) {
                WorkspaceContext.clear();
            }
        }
    }
}
