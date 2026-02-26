package com.biolab.auth.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity → {@code sec_schema.password_history}.
 * Prevents password reuse — stores hashed passwords (NIST 800-63B).
 *
 * @author BioLab Engineering Team
 */
@Entity
@Table(name = "password_history", schema = "sec_schema")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PasswordHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
}
