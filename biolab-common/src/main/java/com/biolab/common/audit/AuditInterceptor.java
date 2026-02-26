package com.biolab.common.audit;

import com.biolab.common.security.CurrentUser;
import com.biolab.common.security.CurrentUserContext;
import com.biolab.common.security.SecurityConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * HTTP request interceptor that logs all API operations for HIPAA audit compliance.
 *
 * <h3>Compliance (Slide 9):</h3>
 * <ul>
 *   <li>HIPAA: Complete audit trails — who accessed what, when</li>
 *   <li>FDA 21 CFR Part 11: Authority checks before operations</li>
 *   <li>Tamper-proof audit logs (append-only, 7-year retention)</li>
 * </ul>
 *
 * <p>Logs are structured JSON via SLF4J MDC, compatible with ELK Stack
 * for centralized log aggregation and compliance reporting.</p>
 *
 * @author BioLab Engineering Team
 */
public class AuditInterceptor implements HandlerInterceptor {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        // Set correlation ID for distributed tracing
        String correlationId = request.getHeader(SecurityConstants.HEADER_CORRELATION_ID);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put("correlationId", correlationId);
        MDC.put("method", request.getMethod());
        MDC.put("path", request.getRequestURI());
        MDC.put("remoteIp", getClientIp(request));

        CurrentUserContext.get().ifPresent(user -> {
            MDC.put("userId", user.userId().toString());
            MDC.put("email", user.email());
            MDC.put("roles", String.join(",", user.roles()));
        });

        request.setAttribute("_audit_start", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler, Exception ex) {
        long start = (long) request.getAttribute("_audit_start");
        long duration = System.currentTimeMillis() - start;

        MDC.put("status", String.valueOf(response.getStatus()));
        MDC.put("durationMs", String.valueOf(duration));

        String method = request.getMethod();
        String path = request.getRequestURI();

        // Log all mutating operations and data access (HIPAA requirement)
        if (isMutating(method) || isDataAccess(path)) {
            auditLog.info("AUDIT action={} path={} status={} duration={}ms user={} ip={}",
                    method, path, response.getStatus(), duration,
                    MDC.get("userId"), MDC.get("remoteIp"));
        }

        MDC.clear();
    }

    private boolean isMutating(String method) {
        return "POST".equals(method) || "PUT".equals(method)
            || "PATCH".equals(method) || "DELETE".equals(method);
    }

    private boolean isDataAccess(String path) {
        return path.contains("/documents") || path.contains("/download")
            || path.contains("/export") || path.contains("/audit");
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
