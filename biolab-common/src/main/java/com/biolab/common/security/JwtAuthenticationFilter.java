package com.biolab.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servlet filter that extracts user identity from gateway-forwarded headers
 * and populates both Spring Security context and {@link CurrentUserContext}.
 *
 * <p>This filter is used by downstream microservices (Auth, User, Project, etc.)
 * that receive pre-authenticated requests from the API Gateway. The gateway
 * validates the JWT and forwards these headers:</p>
 * <ul>
 *   <li>{@code X-User-Id} — UUID</li>
 *   <li>{@code X-User-Email} — email</li>
 *   <li>{@code X-User-Roles} — comma-separated roles</li>
 *   <li>{@code X-User-OrgId} — organization UUID</li>
 * </ul>
 *
 * <p>If headers are absent, the request proceeds unauthenticated
 * (open endpoints like actuator/swagger).</p>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String userId = request.getHeader(SecurityConstants.HEADER_USER_ID);
            String email  = request.getHeader(SecurityConstants.HEADER_USER_EMAIL);
            String roles  = request.getHeader(SecurityConstants.HEADER_USER_ROLES);
            String orgId  = request.getHeader(SecurityConstants.HEADER_USER_ORG_ID);

            if (userId != null && !userId.isBlank()) {
                List<String> roleList = (roles != null && !roles.isBlank())
                        ? Arrays.asList(roles.split(","))
                        : Collections.emptyList();

                // Populate CurrentUserContext (ThreadLocal)
                CurrentUser currentUser = new CurrentUser(
                        UUID.fromString(userId), email, roleList, orgId);
                CurrentUserContext.set(currentUser);

                // Populate Spring Security context for @PreAuthorize
                List<SimpleGrantedAuthority> authorities = roleList.stream()
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r.trim()))
                        .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);

                log.debug("Authenticated user={} roles={} orgId={}", userId, roleList, orgId);
            }

            filterChain.doFilter(request, response);
        } finally {
            CurrentUserContext.clear();
            SecurityContextHolder.clearContext();
        }
    }
}
