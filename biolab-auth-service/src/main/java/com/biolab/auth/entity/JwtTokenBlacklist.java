package com.biolab.auth.entity;

import com.biolab.auth.entity.enums.TokenType;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * JPA entity → {@code sec_schema.jwt_token_blacklist}.
 * Revoked access tokens — checked on every authenticated request.
 *
 * @author BioLab Engineering Team
 */
@Entity
@Table(name = "jwt_token_blacklist", schema = "sec_schema")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JwtTokenBlacklist extends BaseEntity {

    @Column(name = "jti", nullable = false, unique = true, length = 255)
    private String jti;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false, length = 20)
    private TokenType tokenType;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "blacklisted_at", nullable = false)
    @Builder.Default
    private Instant blacklistedAt = Instant.now();

    @Column(name = "reason", length = 255)
    private String reason;
}
