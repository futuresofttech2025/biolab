package com.biolab.auth.service.impl;

import com.biolab.auth.dto.request.DataAccessLogRequest;
import com.biolab.auth.dto.response.DataAccessLogResponse;
import com.biolab.auth.dto.response.PageResponse;
import com.biolab.auth.entity.DataAccessLog;
import com.biolab.auth.entity.User;
import com.biolab.auth.entity.enums.DataAccessAction;
import com.biolab.auth.exception.ResourceNotFoundException;
import com.biolab.auth.repository.DataAccessLogRepository;
import com.biolab.auth.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DataAccessLogServiceImpl Unit Tests")
class DataAccessLogServiceImplTest {

    @InjectMocks private DataAccessLogServiceImpl service;
    @Mock private DataAccessLogRepository repo;
    @Mock private UserRepository userRepo;

    private User testUser;
    private DataAccessLog accessLog;
    private final UUID userId = UUID.randomUUID();
    private final UUID resourceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        testUser = User.builder().email("user@biolab.com").passwordHash("h").firstName("A").lastName("B").build();
        testUser.setId(userId);

        accessLog = DataAccessLog.builder()
                .user(testUser).resourceType("DOCUMENT").resourceId(resourceId)
                .action(DataAccessAction.VIEW).ipAddress("192.168.1.1").build();
        accessLog.setId(UUID.randomUUID());
        accessLog.setCreatedAt(Instant.now());
    }

    @Nested @DisplayName("log")
    class LogTests {
        @Test @DisplayName("[TC-AUTH-076] ✅ Should log data access successfully")
        void log_Success() {
            DataAccessLogRequest req = new DataAccessLogRequest();
            req.setResourceType("DOCUMENT"); req.setResourceId(resourceId); req.setAction("VIEW");

            when(userRepo.findById(userId)).thenReturn(Optional.of(testUser));
            when(repo.save(any())).thenReturn(accessLog);

            DataAccessLogResponse resp = service.log(userId, req, "192.168.1.1");
            assertThat(resp).isNotNull();
            assertThat(resp.getResourceType()).isEqualTo("DOCUMENT");
            assertThat(resp.getAction()).isEqualTo("VIEW");
            assertThat(resp.getIpAddress()).isEqualTo("192.168.1.1");
        }

        @Test @DisplayName("[TC-AUTH-077] ✅ Should map all actions: DOWNLOAD, EXPORT, PRINT, CREATE, UPDATE, DELETE")
        void log_AllActions() {
            for (String action : List.of("VIEW", "DOWNLOAD", "EXPORT", "PRINT", "CREATE", "UPDATE", "DELETE")) {
                DataAccessLogRequest req = new DataAccessLogRequest();
                req.setResourceType("DOCUMENT"); req.setResourceId(resourceId); req.setAction(action);

                DataAccessLog saved = DataAccessLog.builder()
                        .user(testUser).resourceType("DOCUMENT").resourceId(resourceId)
                        .action(DataAccessAction.valueOf(action)).ipAddress("ip").build();
                saved.setId(UUID.randomUUID()); saved.setCreatedAt(Instant.now());

                when(userRepo.findById(userId)).thenReturn(Optional.of(testUser));
                when(repo.save(any())).thenReturn(saved);

                DataAccessLogResponse resp = service.log(userId, req, "ip");
                assertThat(resp.getAction()).isEqualTo(action);
            }
        }

        @Test @DisplayName("[TC-AUTH-078] ❌ Should throw when user not found")
        void log_UserNotFound() {
            DataAccessLogRequest req = new DataAccessLogRequest();
            req.setResourceType("DOC"); req.setResourceId(resourceId); req.setAction("VIEW");
            when(userRepo.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.log(UUID.randomUUID(), req, "ip"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test @DisplayName("[TC-AUTH-079] ❌ Should throw on invalid action enum")
        void log_InvalidAction() {
            DataAccessLogRequest req = new DataAccessLogRequest();
            req.setResourceType("DOC"); req.setResourceId(resourceId); req.setAction("INVALID_ACTION");
            when(userRepo.findById(userId)).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> service.log(userId, req, "ip"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("[TC-AUTH-080] ✅ Should capture entity fields correctly")
        void log_CaptureFields() {
            DataAccessLogRequest req = new DataAccessLogRequest();
            req.setResourceType("PROJECT"); req.setResourceId(resourceId); req.setAction("DOWNLOAD");

            when(userRepo.findById(userId)).thenReturn(Optional.of(testUser));
            ArgumentCaptor<DataAccessLog> cap = ArgumentCaptor.forClass(DataAccessLog.class);
            when(repo.save(cap.capture())).thenReturn(accessLog);

            service.log(userId, req, "10.0.0.5");
            assertThat(cap.getValue().getResourceType()).isEqualTo("PROJECT");
            assertThat(cap.getValue().getAction()).isEqualTo(DataAccessAction.DOWNLOAD);
            assertThat(cap.getValue().getIpAddress()).isEqualTo("10.0.0.5");
            assertThat(cap.getValue().getUser().getId()).isEqualTo(userId);
        }
    }

    @Nested @DisplayName("getByUserId")
    class GetByUserIdTests {
        @Test @DisplayName("[TC-AUTH-081] ✅ Should return paginated access logs for user")
        void getByUserId_Success() {
            when(repo.findByUserIdOrderByCreatedAtDesc(eq(userId), any()))
                    .thenReturn(new PageImpl<>(List.of(accessLog)));
            PageResponse<DataAccessLogResponse> result = service.getByUserId(userId, PageRequest.of(0, 20));
            assertThat(result.getContent()).hasSize(1);
        }

        @Test @DisplayName("[TC-AUTH-082] ✅ Should return empty for user with no access logs")
        void getByUserId_Empty() {
            when(repo.findByUserIdOrderByCreatedAtDesc(any(), any())).thenReturn(Page.empty());
            assertThat(service.getByUserId(UUID.randomUUID(), PageRequest.of(0, 10)).getContent()).isEmpty();
        }
    }

    @Nested @DisplayName("getByResource")
    class GetByResourceTests {
        @Test @DisplayName("[TC-AUTH-083] ✅ Should return access logs for specific resource")
        void getByResource_Success() {
            when(repo.findByResourceTypeAndResourceId(eq("DOCUMENT"), eq(resourceId), any()))
                    .thenReturn(new PageImpl<>(List.of(accessLog)));
            PageResponse<DataAccessLogResponse> result = service.getByResource("DOCUMENT", resourceId, PageRequest.of(0, 10));
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getResourceId()).isEqualTo(resourceId);
        }

        @Test @DisplayName("[TC-AUTH-084] ✅ Should return empty when no access logs for resource")
        void getByResource_Empty() {
            when(repo.findByResourceTypeAndResourceId(any(), any(), any())).thenReturn(Page.empty());
            assertThat(service.getByResource("X", UUID.randomUUID(), PageRequest.of(0, 10)).getContent()).isEmpty();
        }
    }
}
