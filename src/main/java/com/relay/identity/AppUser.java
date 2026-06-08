package com.relay.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** A person who can sign in. Org-scoped (not workspace-scoped), so no RLS. */
@Entity
@Table(name = "app_user")
@Getter
@Setter
public class AppUser {

    @Id
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    /** BCrypt hash for email/password users; null for Google/OTP users. */
    @Column(name = "password_hash")
    private String passwordHash;

    /** Platform operator (Ops console access). */
    @Column(name = "is_admin", nullable = false)
    private boolean admin = false;

    /** Email verified — required before a password user can sign in. */
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
