package com.suprsyncr.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates internal service-to-service requests (engine → backend) via a
 * shared secret in the {@code X-Service-Token} header. Only applied to the
 * /api/internal/** filter chain — never used for end-user requests.
 *
 * The token is configured at {@code internal.service-token}; an empty/blank
 * config value disables the filter (returns 503), which prevents accidental
 * exposure if the env var is missing in production.
 */
@Component
public class ServiceTokenFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Service-Token";
    public static final String ROLE = "INTERNAL_SERVICE";

    private final String configuredToken;

    public ServiceTokenFilter(@Value("${internal.service-token:}") String configuredToken) {
        this.configuredToken = configuredToken == null ? "" : configuredToken.trim();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        if (configuredToken.isEmpty()) {
            res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "internal.service-token is not configured");
            return;
        }
        String supplied = req.getHeader(HEADER);
        if (supplied == null || !constantTimeEquals(supplied, configuredToken)) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "invalid service token");
            return;
        }
        var auth = new UsernamePasswordAuthenticationToken(
                "engine", null, List.of(new SimpleGrantedAuthority("ROLE_" + ROLE)));
        SecurityContextHolder.getContext().setAuthentication(auth);
        chain.doFilter(req, res);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }
}
