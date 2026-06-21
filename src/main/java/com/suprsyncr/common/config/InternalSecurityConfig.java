package com.suprsyncr.common.config;

import com.suprsyncr.common.security.ServiceTokenFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Separate Spring Security filter chain for service-to-service traffic under
 * {@code /api/internal/**}. Higher precedence than the main chain so internal
 * routes are matched here and authenticated via the {@link ServiceTokenFilter}
 * shared-secret header, not via JWT.
 *
 * Intentionally isolated from {@link SecurityConfig} so that changes to the
 * end-user auth surface (e.g. tightening permitAll lists) do not affect the
 * engine ↔ backend path.
 */
@Configuration
public class InternalSecurityConfig {

    private final ServiceTokenFilter serviceTokenFilter;

    public InternalSecurityConfig(ServiceTokenFilter serviceTokenFilter) {
        this.serviceTokenFilter = serviceTokenFilter;
    }

    @Bean
    @Order(0)
    public SecurityFilterChain internalFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/internal/**")
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a.anyRequest().hasRole(ServiceTokenFilter.ROLE))
            .addFilterBefore(serviceTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * CRITICAL: disable Spring Boot's auto-registration of
     * {@link ServiceTokenFilter} into the global servlet filter chain.
     *
     * Because the filter is a {@code @Component} that extends
     * {@code OncePerRequestFilter}, Spring Boot would otherwise wrap it in a
     * {@code FilterRegistrationBean} and run it on EVERY request — including
     * {@code /api/v1/auth/login}, where no service token is supplied, so it
     * would reject every public request with 401 "invalid service token".
     *
     * Disabling the auto-registration leaves the filter usable only where we
     * explicitly add it via {@code .addFilterBefore(...)} above (i.e. the
     * {@code /api/internal/**} chain). The end-user-facing main filter chain
     * never sees it.
     */
    @Bean
    public FilterRegistrationBean<ServiceTokenFilter> disableServiceTokenFilterAutoRegistration(
            ServiceTokenFilter filter) {
        FilterRegistrationBean<ServiceTokenFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }
}
