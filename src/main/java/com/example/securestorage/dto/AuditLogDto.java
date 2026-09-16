package com.example.securestorage.dto;

import java.time.LocalDateTime;

public class AuditLogDto {
    private Long id;
    private Long userId;
    private String username;
    private String action;
    private Long fileId;
    private String status;
    private LocalDateTime timestamp;
    private String ipAddress;
    private String details;

    public AuditLogDto() {
    }

    public AuditLogDto(Long id, Long userId, String username, String action, Long fileId, String status, LocalDateTime timestamp, String ipAddress, String details) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.action = action;
        this.fileId = fileId;
        this.status = status;
        this.timestamp = timestamp;
        this.ipAddress = ipAddress;
        this.details = details;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
