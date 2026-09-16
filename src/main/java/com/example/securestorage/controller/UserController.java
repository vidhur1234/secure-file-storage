package com.example.securestorage.controller;

import com.example.securestorage.dto.ApiResponse;
import com.example.securestorage.dto.CreateUserRequest;
import com.example.securestorage.dto.UpdateUserRequest;
import com.example.securestorage.dto.UserDto;
import com.example.securestorage.entity.User;
import com.example.securestorage.exception.AccessDeniedCustomException;
import com.example.securestorage.security.UserPrincipal;
import com.example.securestorage.service.RbacService;
import com.example.securestorage.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final RbacService rbacService;

    public UserController(UserService userService, RbacService rbacService) {
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
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers(@AuthenticationPrincipal UserPrincipal currentUser) {
        ensureAdmin(currentUser);
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal currentUser) {
        User user = userService.findByUsername(currentUser.getUsername());
        if (!rbacService.isAdmin(user) && !currentUser.getId().equals(id)) {
            throw new AccessDeniedCustomException("You can only view your own user profile.");
        }
        UserDto userDto = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", userDto));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody CreateUserRequest request,
                                                           @AuthenticationPrincipal UserPrincipal currentUser,
                                                           HttpServletRequest httpRequest) {
        ensureAdmin(currentUser);
        User actor = userService.findByUsername(currentUser.getUsername());
        UserDto created = userService.createUser(request, actor, getClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("User created successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(@PathVariable Long id,
                                                           @Valid @RequestBody UpdateUserRequest request,
                                                           @AuthenticationPrincipal UserPrincipal currentUser,
                                                           HttpServletRequest httpRequest) {
        ensureAdmin(currentUser);
        User actor = userService.findByUsername(currentUser.getUsername());
        UserDto updated = userService.updateUser(id, request, actor, getClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id,
                                                        @AuthenticationPrincipal UserPrincipal currentUser,
                                                        HttpServletRequest httpRequest) {
        ensureAdmin(currentUser);
        User actor = userService.findByUsername(currentUser.getUsername());
        userService.deleteUser(id, actor, getClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully"));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<UserDto>> activateUser(@PathVariable Long id,
                                                             @AuthenticationPrincipal UserPrincipal currentUser,
                                                             HttpServletRequest httpRequest) {
        ensureAdmin(currentUser);
        User actor = userService.findByUsername(currentUser.getUsername());
        UserDto updated = userService.setUserActive(id, true, actor, getClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("User activated successfully", updated));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<UserDto>> deactivateUser(@PathVariable Long id,
                                                               @AuthenticationPrincipal UserPrincipal currentUser,
                                                               HttpServletRequest httpRequest) {
        ensureAdmin(currentUser);
        User actor = userService.findByUsername(currentUser.getUsername());
        UserDto updated = userService.setUserActive(id, false, actor, getClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("User deactivated successfully", updated));
    }

    @PostMapping("/{id}/secret-key")
    public ResponseEntity<ApiResponse<UserDto>> generateSecretKey(@PathVariable Long id,
                                                                  @AuthenticationPrincipal UserPrincipal currentUser,
                                                                  HttpServletRequest httpRequest) {
        ensureAdmin(currentUser);
        User actor = userService.findByUsername(currentUser.getUsername());
        UserDto updated = userService.generateSecretKey(id, actor, getClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("User secret key regenerated successfully", updated));
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isEmpty()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
