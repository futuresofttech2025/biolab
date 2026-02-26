package com.biolab.auth.service.impl;

import com.biolab.auth.dto.response.PasswordHistoryResponse;
import com.biolab.auth.repository.PasswordHistoryRepository;
import com.biolab.auth.service.PasswordHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class PasswordHistoryServiceImpl implements PasswordHistoryService {
    private final PasswordHistoryRepository repo;
    @Override public List<PasswordHistoryResponse> getByUserId(UUID userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(ph -> PasswordHistoryResponse.builder().id(ph.getId()).createdAt(ph.getCreatedAt()).build()).toList();
    }
}
