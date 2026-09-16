package com.example.securestorage.controller;

import com.example.securestorage.dto.ApiResponse;
import com.example.securestorage.dto.AuditLogDto;
import com.example.securestorage.entity.User;
import com.example.securestorage.exception.AccessDeniedCustomException;
import com.example.securestorage.security.UserPrincipal;
import com.example.securestorage.service.AuditService;
import com.example.securestorage.service.RbacService;
import com.example.securestorage.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditService auditService;
    private final UserService userService;
    private final RbacService rbacService;

    public AuditLogController(AuditService auditService, UserService userService, RbacService rbacService) {
        this.auditService = auditService;
        this.userService = userService;
        this.rbacService = rbacService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AuditLogDto>>> getAllAuditLogs(@AuthenticationPrincipal UserPrincipal currentUser) {
        User user = userService.findByUsername(currentUser.getUsername());
        if (!rbacService.isAdmin(user) && !rbacService.isAuditor(user)) {
            throw new AccessDeniedCustomException("Only Administrator and Auditor roles can view system audit logs.");
        }

        List<AuditLogDto> logs = auditService.getAllLogs();
        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved successfully", logs));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<AuditLogDto>>> getMyAuditLogs(@AuthenticationPrincipal UserPrincipal currentUser) {
        List<AuditLogDto> logs = auditService.getUserLogs(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Personal audit history retrieved", logs));
    }
}
