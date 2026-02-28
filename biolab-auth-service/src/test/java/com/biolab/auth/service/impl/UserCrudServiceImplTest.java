package com.biolab.auth.service.impl;

import com.biolab.auth.dto.request.*;
import com.biolab.auth.dto.response.*;
import com.biolab.auth.entity.User;
import com.biolab.auth.exception.*;
import com.biolab.auth.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserCrudServiceImpl Unit Tests")
class UserCrudServiceImplTest {

    @InjectMocks private UserCrudServiceImpl service;
    @Mock private UserRepository repo;
    @Mock private PasswordEncoder passwordEncoder;

    private User testUser;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("user@biolab.com").passwordHash("$2a$10$encoded")
                .firstName("Jane").lastName("Doe").phone("+1234567890")
                .isActive(true).isEmailVerified(false).isLocked(false)
                .build();
        testUser.setId(userId);
        testUser.setCreatedAt(Instant.now());
    }

    // ── CREATE ──

    @Test
    @DisplayName("[TC-AUTH-143] ✅ Create user successfully")
    void create_Success() {
        UserCreateRequest req = new UserCreateRequest();
        req.setEmail("new@biolab.com"); req.setPassword("Pass@123");
        req.setFirstName("New"); req.setLastName("User");

        when(repo.existsByEmailIgnoreCase("new@biolab.com")).thenReturn(false);
        when(passwordEncoder.encode("Pass@123")).thenReturn("encoded");
        when(repo.save(any(User.class))).thenReturn(testUser);

        UserResponse resp = service.create(req);

        assertThat(resp).isNotNull();
        assertThat(resp.getId()).isEqualTo(userId);
        verify(repo).save(any(User.class));
    }

    @Test
    @DisplayName("[TC-AUTH-144] ❌ Create user with duplicate email")
    void create_DuplicateEmail() {
        UserCreateRequest req = new UserCreateRequest();
        req.setEmail("existing@biolab.com"); req.setPassword("Pass@123");
        req.setFirstName("Dup"); req.setLastName("User");

        when(repo.existsByEmailIgnoreCase("existing@biolab.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(DuplicateResourceException.class);
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("[TC-AUTH-145] ✅ Create user defaults isActive to true")
    void create_DefaultsActive() {
        UserCreateRequest req = new UserCreateRequest();
        req.setEmail("a@b.com"); req.setPassword("P@1"); req.setFirstName("A"); req.setLastName("B");

        when(repo.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("enc");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(repo.save(captor.capture())).thenReturn(testUser);

        service.create(req);
        assertThat(captor.getValue().getIsActive()).isTrue();
    }

    // ── GET BY ID ──

    @Test
    @DisplayName("[TC-AUTH-146] ✅ Get user by ID successfully")
    void getById_Success() {
        when(repo.findById(userId)).thenReturn(Optional.of(testUser));

        UserResponse resp = service.getById(userId);

        assertThat(resp.getEmail()).isEqualTo("user@biolab.com");
        assertThat(resp.getFirstName()).isEqualTo("Jane");
    }

    @Test
    @DisplayName("[TC-AUTH-147] ❌ Get user by non-existent ID")
    void getById_NotFound() {
        UUID randomId = UUID.randomUUID();
        when(repo.findById(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(randomId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── GET BY EMAIL ──

    @Test
    @DisplayName("[TC-AUTH-148] ✅ Get user by email successfully")
    void getByEmail_Success() {
        when(repo.findByEmailIgnoreCase("user@biolab.com")).thenReturn(Optional.of(testUser));

        UserResponse resp = service.getByEmail("user@biolab.com");
        assertThat(resp.getId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("[TC-AUTH-149] ❌ Get user by non-existent email")
    void getByEmail_NotFound() {
        when(repo.findByEmailIgnoreCase("nobody@nowhere.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByEmail("nobody@nowhere.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── SEARCH ──

    @Test
    @DisplayName("[TC-AUTH-150] ✅ Search users returns paginated results")
    void search_ReturnsPaginated() {
        Page<User> page = new PageImpl<>(List.of(testUser), PageRequest.of(0, 10), 1);
        when(repo.searchUsers("jane", true, PageRequest.of(0, 10))).thenReturn(page);

        PageResponse<UserResponse> resp = service.search("jane", true, PageRequest.of(0, 10));

        assertThat(resp.getContent()).hasSize(1);
        assertThat(resp.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("[TC-AUTH-151] ✅ Search users returns empty page when no matches")
    void search_Empty() {
        when(repo.searchUsers("xyz", null, PageRequest.of(0, 10))).thenReturn(Page.empty());

        PageResponse<UserResponse> resp = service.search("xyz", null, PageRequest.of(0, 10));

        assertThat(resp.getContent()).isEmpty();
        assertThat(resp.getTotalElements()).isEqualTo(0);
    }

    // ── UPDATE ──

    @Test
    @DisplayName("[TC-AUTH-152] ✅ Update user partial fields")
    void update_PartialSuccess() {
        UserUpdateRequest req = new UserUpdateRequest();
        req.setFirstName("Updated"); req.setPhone("+9999");

        when(repo.findById(userId)).thenReturn(Optional.of(testUser));
        when(repo.save(any())).thenReturn(testUser);

        UserResponse resp = service.update(userId, req);

        assertThat(resp).isNotNull();
        verify(repo).save(any(User.class));
    }

    @Test
    @DisplayName("[TC-AUTH-153] ❌ Update non-existent user")
    void update_NotFound() {
        UUID randomId = UUID.randomUUID();
        when(repo.findById(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(randomId, new UserUpdateRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("[TC-AUTH-154] ✅ Update preserves unchanged fields when null")
    void update_NullFieldsIgnored() {
        UserUpdateRequest req = new UserUpdateRequest();
        req.setFirstName("Changed");

        when(repo.findById(userId)).thenReturn(Optional.of(testUser));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(repo.save(captor.capture())).thenReturn(testUser);

        service.update(userId, req);

        assertThat(captor.getValue().getLastName()).isEqualTo("Doe"); // unchanged
        assertThat(captor.getValue().getFirstName()).isEqualTo("Changed");
    }

    // ── DELETE (SOFT) ──

    @Test
    @DisplayName("[TC-AUTH-155] ✅ Soft delete user sets isActive=false")
    void delete_SoftDelete() {
        when(repo.findById(userId)).thenReturn(Optional.of(testUser));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(repo.save(captor.capture())).thenReturn(testUser);

        service.delete(userId);

        assertThat(captor.getValue().getIsActive()).isFalse();
    }

    @Test
    @DisplayName("[TC-AUTH-156] ❌ Delete non-existent user")
    void delete_NotFound() {
        UUID randomId = UUID.randomUUID();
        when(repo.findById(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(randomId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── RESPONSE MAPPING ──

    @Test
    @DisplayName("[TC-AUTH-157] ✅ Response should not expose password hash")
    void response_NoPasswordHash() {
        when(repo.findById(userId)).thenReturn(Optional.of(testUser));

        UserResponse resp = service.getById(userId);

        // UserResponse should not have passwordHash field
        assertThat(resp).hasNoNullFieldsOrPropertiesExcept(
                "phone", "avatarUrl", "lastLoginAt", "updatedAt"
        ).extracting("email").isEqualTo("user@biolab.com");
    }
}
