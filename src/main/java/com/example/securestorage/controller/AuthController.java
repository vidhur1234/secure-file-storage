package com.example.securestorage.controller;

import com.example.securestorage.dto.ApiResponse;
import com.example.securestorage.dto.AuthRequest;
import com.example.securestorage.dto.AuthResponse;
import com.example.securestorage.dto.UserDto;
import com.example.securestorage.entity.User;
import com.example.securestorage.security.JwtTokenProvider;
import com.example.securestorage.security.UserPrincipal;
import com.example.securestorage.service.AuditService;
import com.example.securestorage.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    private final AuditService auditService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider tokenProvider,
                          UserService userService,
                          AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userService = userService;
        this.auditService = auditService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> authenticateUser(
            @Valid @RequestBody AuthRequest loginRequest,
            HttpServletRequest request) {

        String clientIp = getClientIp(request);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername().trim(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            User user = userService.findByUsername(userPrincipal.getUsername());

            auditService.logLoginSuccess(user, clientIp);

            AuthResponse response = new AuthResponse(
                    jwt,
                    userPrincipal.getId(),
                    userPrincipal.getUsername(),
                    userPrincipal.getFullName(),
                    userPrincipal.getEmail(),
                    userPrincipal.getRoles(),
                    userPrincipal.getPermissions()
            );

            return ResponseEntity.ok(ApiResponse.success("Login successful", response));

        } catch (BadCredentialsException ex) {
            auditService.logLoginFailure(loginRequest.getUsername(), clientIp, "Invalid credentials provided");
            throw ex;
        } catch (Exception ex) {
            auditService.logLoginFailure(loginRequest.getUsername(), clientIp, ex.getMessage());
            throw ex;
        }
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(@AuthenticationPrincipal UserPrincipal currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }
        UserDto dto = userService.getUserById(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("User profile fetched", dto));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logoutUser(@AuthenticationPrincipal UserPrincipal currentUser, HttpServletRequest request) {
        if (currentUser != null) {
            auditService.logEvent(currentUser.getId(), currentUser.getUsername(), "LOGOUT", null, "SUCCESS", getClientIp(request), "User logged out");
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isEmpty()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
