package com.biolab.common.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Logs request entry/exit with timing. Works with MdcLoggingFilter
 * for comprehensive request tracing in ELK.
 */
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);
    private static final String START_TIME = "x-start-time";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME, System.currentTimeMillis());
        log.debug("→ {} {} (params: {})", request.getMethod(), request.getRequestURI(), request.getQueryString());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Long start = (Long) request.getAttribute(START_TIME);
        long duration = start != null ? System.currentTimeMillis() - start : -1;
        if (ex != null) {
            log.error("← {} {} — {} ({}ms) ERROR: {}", request.getMethod(), request.getRequestURI(),
                response.getStatus(), duration, ex.getMessage());
        } else if (response.getStatus() >= 400) {
            log.warn("← {} {} — {} ({}ms)", request.getMethod(), request.getRequestURI(),
                response.getStatus(), duration);
        }
    }
}
