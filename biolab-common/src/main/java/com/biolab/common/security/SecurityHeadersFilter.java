package com.biolab.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Adds OWASP-recommended security headers to all HTTP responses.
 *
 * <h3>Headers Applied (Slide 10 — Input Validation &amp; Network Security):</h3>
 * <ul>
 *   <li>{@code X-Content-Type-Options: nosniff} — prevents MIME sniffing</li>
 *   <li>{@code X-Frame-Options: DENY} — prevents clickjacking</li>
 *   <li>{@code X-XSS-Protection: 0} — legacy XSS filter (CSP preferred)</li>
 *   <li>{@code Strict-Transport-Security} — enforces HTTPS (HSTS)</li>
 *   <li>{@code Content-Security-Policy} — restricts resource loading</li>
 *   <li>{@code Referrer-Policy: strict-origin-when-cross-origin}</li>
 *   <li>{@code Permissions-Policy} — restricts browser features</li>
 *   <li>{@code Cache-Control: no-store} — prevents caching of sensitive data</li>
 * </ul>
 *
 * @author BioLab Engineering Team
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // OWASP Security Headers
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "0"); // Modern CSP-based protection
        response.setHeader("Strict-Transport-Security",
                "max-age=31536000; includeSubDomains; preload");
        response.setHeader("Content-Security-Policy",
                "default-src 'self'; frame-ancestors 'none'");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Permissions-Policy",
                "camera=(), microphone=(), geolocation=(), payment=()");

        // Prevent caching of API responses containing sensitive data
        if (request.getRequestURI().startsWith("/api/")) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            response.setHeader("Pragma", "no-cache");
        }

        filterChain.doFilter(request, response);
    }
}
