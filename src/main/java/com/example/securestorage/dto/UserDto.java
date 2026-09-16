package com.example.securestorage.dto;

import java.time.LocalDateTime;
import java.util.Set;

public class UserDto {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private boolean active;
    private String secretKey;
    private LocalDateTime createdAt;
    private Set<String> roles;

    public UserDto() {
    }

    public UserDto(Long id, String username, String fullName, String email, boolean active, String secretKey, LocalDateTime createdAt, Set<String> roles) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.active = active;
        this.secretKey = secretKey;
        this.createdAt = createdAt;
        this.roles = roles;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}
