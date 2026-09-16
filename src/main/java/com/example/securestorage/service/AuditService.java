package com.example.securestorage.service;

import com.example.securestorage.dto.AuditLogDto;
import com.example.securestorage.entity.AuditLog;
import com.example.securestorage.entity.User;
import com.example.securestorage.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void logEvent(Long userId, String username, String action, Long fileId,
                         String status, String ipAddress, String details) {
        AuditLog log = new AuditLog(userId, username, action, fileId, status, ipAddress, details);
        log.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(log);
    }

    public void logLoginSuccess(User user, String ip) {
        logEvent(user.getId(), user.getUsername(), "LOGIN_SUCCESS", null, "SUCCESS", ip, "User logged in successfully");
    }

    public void logLoginFailure(String username, String ip, String reason) {
        logEvent(null, username != null ? username : "ANONYMOUS", "LOGIN_FAILED", null, "FAILURE", ip, reason);
    }

    public void logFileUpload(User user, Long fileId, String fileName, String ip) {
        logEvent(user.getId(), user.getUsername(), "FILE_UPLOAD", fileId, "SUCCESS", ip, "Uploaded and encrypted file: " + fileName);
    }

    public void logFileDownload(User user, Long fileId, String fileName, String status, String ip, String details) {
        logEvent(user.getId(), user.getUsername(), "FILE_DOWNLOAD", fileId, status, ip, details);
    }

    public void logFileTampered(User user, Long fileId, String fileName, String ip, String details) {
        logEvent(user != null ? user.getId() : null,
                user != null ? user.getUsername() : "UNKNOWN",
                "INTEGRITY_CHECK_FAILED", fileId, "TAMPERED", ip, details);
    }

    public void logAccessDenied(User user, String action, Long fileId, String ip, String details) {
        logEvent(user != null ? user.getId() : null,
                user != null ? user.getUsername() : "ANONYMOUS",
                action, fileId, "FORBIDDEN", ip, details);
    }

    public void logUserAction(User actor, String action, String targetUsername, String ip, String details) {
        logEvent(actor != null ? actor.getId() : null,
                actor != null ? actor.getUsername() : "SYSTEM",
                action, null, "SUCCESS", ip, "Target user: " + targetUsername + ". " + details);
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> getAllLogs() {
        return auditLogRepository.findTop100ByOrderByTimestampDesc().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> getUserLogs(Long userId) {
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private AuditLogDto mapToDto(AuditLog log) {
        return new AuditLogDto(
                log.getId(),
                log.getUserId(),
                log.getUsername(),
                log.getAction(),
                log.getFileId(),
                log.getStatus(),
                log.getTimestamp(),
                log.getIpAddress(),
                log.getDetails()
        );
    }
}
