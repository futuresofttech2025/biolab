package com.biolab.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that populates SLF4J MDC with request-scoped context
 * for structured logging. Every log line emitted during a request includes:
 * <ul>
 *   <li>{@code requestId}  — unique per request (or from X-Request-ID header)</li>
 *   <li>{@code traceId}    — propagated from X-Trace-ID header if present</li>
 *   <li>{@code userId}     — extracted from X-User-ID header (set by gateway)</li>
 *   <li>{@code orgId}      — extracted from X-Org-ID header (set by gateway)</li>
 *   <li>{@code method}     — HTTP method</li>
 *   <li>{@code uri}        — request URI</li>
 *   <li>{@code clientIp}   — client IP address</li>
 * </ul>
 *
 * @author BioLab Engineering Team
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(MdcLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        try {
            // Populate MDC
            String requestId = header(request, "X-Request-ID", UUID.randomUUID().toString().substring(0, 8));
            MDC.put("requestId", requestId);
            MDC.put("traceId", header(request, "X-Trace-ID", requestId));
            MDC.put("userId", header(request, "X-User-ID", "anonymous"));
            MDC.put("orgId", header(request, "X-Org-ID", "-"));
            MDC.put("method", request.getMethod());
            MDC.put("uri", request.getRequestURI());
            MDC.put("clientIp", clientIp(request));

            // Set response header for correlation
            response.setHeader("X-Request-ID", requestId);

            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("HTTP {} {} — {} ({}ms)",
                request.getMethod(), request.getRequestURI(),
                response.getStatus(), duration);
            MDC.clear();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator") || path.equals("/favicon.ico");
    }

    private String header(HttpServletRequest req, String name, String fallback) {
        String val = req.getHeader(name);
        return (val != null && !val.isBlank()) ? val : fallback;
    }

    private String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        return (xff != null) ? xff.split(",")[0].trim() : req.getRemoteAddr();
    }
}
