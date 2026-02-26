package com.biolab.auth.service;

import com.biolab.auth.dto.request.DataAccessLogRequest;
import com.biolab.auth.dto.response.*;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
public interface DataAccessLogService {
    DataAccessLogResponse log(UUID userId, DataAccessLogRequest request, String ipAddress);
    PageResponse<DataAccessLogResponse> getByUserId(UUID userId, Pageable pageable);
    PageResponse<DataAccessLogResponse> getByResource(String resourceType, UUID resourceId, Pageable pageable);
}
