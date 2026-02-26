package com.biolab.auth.service.impl;

import com.biolab.auth.dto.request.DataAccessLogRequest;
import com.biolab.auth.dto.response.*;
import com.biolab.auth.entity.*;
import com.biolab.auth.entity.enums.DataAccessAction;
import com.biolab.auth.exception.ResourceNotFoundException;
import com.biolab.auth.repository.*;
import com.biolab.auth.service.DataAccessLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
@Service @RequiredArgsConstructor @Transactional
public class DataAccessLogServiceImpl implements DataAccessLogService {
    private final DataAccessLogRepository repo;
    private final UserRepository userRepo;
    @Override public DataAccessLogResponse log(UUID userId, DataAccessLogRequest req, String ip) {
        User user = userRepo.findById(userId).orElseThrow(()->new ResourceNotFoundException("User","id",userId));
        DataAccessLog dal = DataAccessLog.builder().user(user).resourceType(req.getResourceType())
                .resourceId(req.getResourceId()).action(DataAccessAction.valueOf(req.getAction())).ipAddress(ip).build();
        return toResp(repo.save(dal));
    }
    @Override @Transactional(readOnly=true) public PageResponse<DataAccessLogResponse> getByUserId(UUID userId, Pageable p) {
        return toPage(repo.findByUserIdOrderByCreatedAtDesc(userId, p));
    }
    @Override @Transactional(readOnly=true) public PageResponse<DataAccessLogResponse> getByResource(String type, UUID id, Pageable p) {
        return toPage(repo.findByResourceTypeAndResourceId(type, id, p));
    }
    private PageResponse<DataAccessLogResponse> toPage(Page<DataAccessLog> page) {
        return PageResponse.<DataAccessLogResponse>builder()
                .content(page.getContent().stream().map(this::toResp).toList())
                .page(page.getNumber()).size(page.getSize()).totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages()).hasNext(page.hasNext()).build();
    }
    private DataAccessLogResponse toResp(DataAccessLog d) {
        return DataAccessLogResponse.builder().id(d.getId()).userId(d.getUser().getId())
                .resourceType(d.getResourceType()).resourceId(d.getResourceId())
                .action(d.getAction().name()).ipAddress(d.getIpAddress()).createdAt(d.getCreatedAt()).build();
    }
}
