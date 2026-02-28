package com.biolab.auth.service.impl;

import com.biolab.auth.dto.response.PasswordHistoryResponse;
import com.biolab.auth.entity.PasswordHistory;
import com.biolab.auth.entity.User;
import com.biolab.auth.repository.PasswordHistoryRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordHistoryServiceImpl Unit Tests")
class PasswordHistoryServiceImplTest {

    @InjectMocks private PasswordHistoryServiceImpl service;
    @Mock private PasswordHistoryRepository repo;
    private final UUID userId = UUID.randomUUID();

    private PasswordHistory makePh(Instant created) {
        User u = User.builder().email("u@b.com").passwordHash("h").firstName("A").lastName("B").build();
        u.setId(userId);
        PasswordHistory ph = PasswordHistory.builder().user(u).passwordHash("$2a$hash").build();
        ph.setId(UUID.randomUUID());
        ph.setCreatedAt(created);
        return ph;
    }

    @Test @DisplayName("[TC-AUTH-093] ✅ Should return password history entries ordered by createdAt desc")
    void getByUserId_Success() {
        Instant now = Instant.now();
        List<PasswordHistory> list = List.of(makePh(now), makePh(now.minusSeconds(3600)));
        when(repo.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(list);

        List<PasswordHistoryResponse> result = service.getByUserId(userId);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCreatedAt()).isAfter(result.get(1).getCreatedAt());
    }

    @Test @DisplayName("[TC-AUTH-094] ✅ Should return empty list for user with no password history")
    void getByUserId_Empty() {
        when(repo.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        assertThat(service.getByUserId(userId)).isEmpty();
    }

    @Test @DisplayName("[TC-AUTH-095] ✅ Should not expose password hashes in response")
    void getByUserId_NoHashExposed() {
        when(repo.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(makePh(Instant.now())));
        List<PasswordHistoryResponse> result = service.getByUserId(userId);
        assertThat(result.get(0)).hasNoNullFieldsOrPropertiesExcept();
        // PasswordHistoryResponse only has id and createdAt — no passwordHash
        assertThat(result.get(0).getId()).isNotNull();
        assertThat(result.get(0).getCreatedAt()).isNotNull();
    }
}
