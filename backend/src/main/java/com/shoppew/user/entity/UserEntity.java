package com.shoppew.user.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "app_users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, columnDefinition = "citext")
    private String email;

    @Column(length = 32)
    private String phone;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserStatus status;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "password_changed_at", nullable = false)
    private Instant passwordChangedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private Set<UserRole> roles = new LinkedHashSet<>();

    protected UserEntity() {}

    public static UserEntity register(
            String email,
            String phone,
            String passwordHash,
            UserStatus status,
            Instant now) {
        UserEntity user = new UserEntity();
        user.email = email;
        user.phone = phone;
        user.passwordHash = passwordHash;
        user.status = status;
        user.emailVerified = false;
        user.failedLoginAttempts = 0;
        user.passwordChangedAt = now;
        user.createdAt = now;
        user.updatedAt = now;
        user.roles.add(UserRole.CUSTOMER);
        return user;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserStatus getStatus() {
        return status;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public Set<UserRole> getRoles() {
        return Set.copyOf(roles);
    }

    public void recordFailedLogin(Instant now, int lockThreshold, Instant lockedUntil) {
        failedLoginAttempts += 1;
        if (failedLoginAttempts >= lockThreshold) {
            this.lockedUntil = lockedUntil;
        }
        updatedAt = now;
    }

    public void recordSuccessfulLogin(Instant now) {
        failedLoginAttempts = 0;
        lockedUntil = null;
        updatedAt = now;
    }

    public void addRole(UserRole role, Instant now) {
        if (roles.add(role)) {
            updatedAt = now;
        }
    }

    public void updateContact(String phone, Instant now) {
        this.phone = phone;
        this.updatedAt = now;
    }

    public void changeStatus(UserStatus status, Instant now) {
        this.status = status;
        this.updatedAt = now;
    }

    public void verifyEmail(Instant now) {
        this.emailVerified = true;
        if (status == UserStatus.PENDING_VERIFICATION) {
            status = UserStatus.ACTIVE;
        }
        updatedAt = now;
    }

    public void changePassword(String encodedPassword, Instant now) {
        passwordHash = encodedPassword;
        passwordChangedAt = now;
        failedLoginAttempts = 0;
        lockedUntil = null;
        updatedAt = now;
    }

    public boolean isLoginLockedAt(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }
}
