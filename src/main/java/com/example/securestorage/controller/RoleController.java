package com.example.securestorage.controller;

import com.example.securestorage.dto.ApiResponse;
import com.example.securestorage.dto.RoleDto;
import com.example.securestorage.dto.RoleHierarchyDto;
import com.example.securestorage.entity.User;
import com.example.securestorage.exception.AccessDeniedCustomException;
import com.example.securestorage.security.UserPrincipal;
import com.example.securestorage.service.RbacService;
import com.example.securestorage.service.RoleService;
import com.example.securestorage.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;
    private final UserService userService;
    private final RbacService rbacService;

    public RoleController(RoleService roleService, UserService userService, RbacService rbacService) {
        this.roleService = roleService;
        this.userService = userService;
        this.rbacService = rbacService;
    }

    private void ensureAdmin(UserPrincipal currentUser) {
        User user = userService.findByUsername(currentUser.getUsername());
        if (!rbacService.isAdmin(user)) {
            throw new AccessDeniedCustomException("Administrator privileges required for this action.");
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleDto>>> getAllRoles() {
        List<RoleDto> roles = roleService.getAllRoles();
        return ResponseEntity.ok(ApiResponse.success("Roles retrieved successfully", roles));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoleDto>> createRole(@Valid @RequestBody RoleDto roleDto,
                                                           @AuthenticationPrincipal UserPrincipal currentUser,
                                                           HttpServletRequest httpRequest) {
        ensureAdmin(currentUser);
        User actor = userService.findByUsername(currentUser.getUsername());
        RoleDto created = roleService.createRole(roleDto, actor, getClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("Role created successfully", created));
    }

    @GetMapping("/hierarchy")
    public ResponseEntity<ApiResponse<List<RoleHierarchyDto>>> getRoleHierarchy() {
        List<RoleHierarchyDto> hierarchy = roleService.getRoleHierarchy();
        return ResponseEntity.ok(ApiResponse.success("Role hierarchy retrieved successfully", hierarchy));
    }

    @PostMapping("/users/{userId}/roles")
    public ResponseEntity<ApiResponse<Void>> assignRoleToUser(@PathVariable Long userId,
                                                              @RequestParam Long roleId,
                                                              @AuthenticationPrincipal UserPrincipal currentUser,
                                                              HttpServletRequest httpRequest) {
        ensureAdmin(currentUser);
        User actor = userService.findByUsername(currentUser.getUsername());
        roleService.assignRoleToUser(userId, roleId, actor, getClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("Role assigned successfully"));
    }

    @DeleteMapping("/users/{userId}/roles/{roleId}")
    public ResponseEntity<ApiResponse<Void>> removeRoleFromUser(@PathVariable Long userId,
                                                                @PathVariable Long roleId,
                                                                @AuthenticationPrincipal UserPrincipal currentUser,
                                                                HttpServletRequest httpRequest) {
        ensureAdmin(currentUser);
        User actor = userService.findByUsername(currentUser.getUsername());
        roleService.removeRoleFromUser(userId, roleId, actor, getClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("Role revoked successfully"));
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isEmpty()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
