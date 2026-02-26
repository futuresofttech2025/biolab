package com.biolab.auth.service;

import com.biolab.auth.dto.response.*;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
public interface AuditLogService {
    PageResponse<LoginAuditLogResponse> getByUserId(UUID userId, Pageable pageable);
    PageResponse<LoginAuditLogResponse> getByEmail(String email, Pageable pageable);
    PageResponse<LoginAuditLogResponse> getAll(Pageable pageable);
}
