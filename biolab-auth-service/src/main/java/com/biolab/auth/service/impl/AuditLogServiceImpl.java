package com.biolab.auth.service.impl;

import com.biolab.auth.dto.response.*;
import com.biolab.auth.entity.LoginAuditLog;
import com.biolab.auth.repository.LoginAuditLogRepository;
import com.biolab.auth.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class AuditLogServiceImpl implements AuditLogService {
    private final LoginAuditLogRepository repo;
    @Override public PageResponse<LoginAuditLogResponse> getByUserId(UUID userId, Pageable p) { return toPage(repo.findByUserIdOrderByCreatedAtDesc(userId,p)); }
    @Override public PageResponse<LoginAuditLogResponse> getByEmail(String email, Pageable p) { return toPage(repo.findByEmailOrderByCreatedAtDesc(email,p)); }
    @Override public PageResponse<LoginAuditLogResponse> getAll(Pageable p) { return toPage(repo.findAll(p)); }
    private PageResponse<LoginAuditLogResponse> toPage(Page<LoginAuditLog> page) {
        return PageResponse.<LoginAuditLogResponse>builder()
                .content(page.getContent().stream().map(l->LoginAuditLogResponse.builder()
                        .id(l.getId()).userId(l.getUser()!=null?l.getUser().getId():null).email(l.getEmail())
                        .ipAddress(l.getIpAddress()).action(l.getAction().name()).status(l.getStatus().name())
                        .mfaUsed(l.getMfaUsed()).failureReason(l.getFailureReason()).createdAt(l.getCreatedAt()).build()).toList())
                .page(page.getNumber()).size(page.getSize()).totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages()).hasNext(page.hasNext()).build();
    }
}
