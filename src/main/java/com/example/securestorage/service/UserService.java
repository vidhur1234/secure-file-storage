package com.example.securestorage.service;

import com.example.securestorage.dto.CreateUserRequest;
import com.example.securestorage.dto.UpdateUserRequest;
import com.example.securestorage.dto.UserDto;
import com.example.securestorage.entity.Role;
import com.example.securestorage.entity.User;
import com.example.securestorage.exception.FileStorageException;
import com.example.securestorage.exception.ResourceNotFoundException;
import com.example.securestorage.repository.RoleRepository;
import com.example.securestorage.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapToDto(user);
    }

    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }

    @Transactional
    public UserDto createUser(CreateUserRequest request, User actor, String clientIp) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new FileStorageException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new FileStorageException("Email '" + request.getEmail() + "' is already registered");
        }

        User user = new User(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                request.getEmail(),
                true
        );

        // Assign roles
        Set<Role> roles = new HashSet<>();
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            for (String roleName : request.getRoles()) {
                roleRepository.findByName(roleName).ifPresent(roles::add);
            }
        }
        if (roles.isEmpty()) {
            roleRepository.findByName("ROLE_EMPLOYEE").ifPresent(roles::add);
        }
        user.setRoles(roles);

        // Generate user secret key
        user.setSecretKey(generateRandomHexKey());

        user = userRepository.save(user);
        auditService.logUserAction(actor, "USER_CREATED", user.getUsername(), clientIp, "New user created with roles: " + request.getRoles());

        return mapToDto(user);
    }

    @Transactional
    public UserDto updateUser(Long id, UpdateUserRequest request, User actor, String clientIp) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (!user.getEmail().equalsIgnoreCase(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new FileStorageException("Email is already taken by another account");
        }

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        }

        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }

        if (request.getRoles() != null) {
            Set<Role> roles = new HashSet<>();
            for (String roleName : request.getRoles()) {
                roleRepository.findByName(roleName).ifPresent(roles::add);
            }
            user.setRoles(roles);
        }

        user = userRepository.save(user);
        auditService.logUserAction(actor, "USER_UPDATED", user.getUsername(), clientIp, "User information updated");

        return mapToDto(user);
    }

    @Transactional
    public void deleteUser(Long id, User actor, String clientIp) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (user.getUsername().equalsIgnoreCase("admin")) {
            throw new FileStorageException("Root admin user cannot be deleted.");
        }

        userRepository.delete(user);
        auditService.logUserAction(actor, "USER_DELETED", user.getUsername(), clientIp, "User permanently removed");
    }

    @Transactional
    public UserDto setUserActive(Long id, boolean active, User actor, String clientIp) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (user.getUsername().equalsIgnoreCase("admin") && !active) {
            throw new FileStorageException("Cannot deactivate the root admin account.");
        }

        user.setActive(active);
        user = userRepository.save(user);

        String action = active ? "USER_ACTIVATED" : "USER_DEACTIVATED";
        auditService.logUserAction(actor, action, user.getUsername(), clientIp, "Account active status set to " + active);

        return mapToDto(user);
    }

    @Transactional
    public UserDto generateSecretKey(Long id, User actor, String clientIp) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setSecretKey(generateRandomHexKey());
        user = userRepository.save(user);

        auditService.logUserAction(actor, "SECRET_KEY_REGENERATED", user.getUsername(), clientIp, "New user secret key generated");
        return mapToDto(user);
    }

    private String generateRandomHexKey() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public UserDto mapToDto(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.isActive(),
                user.getSecretKey(),
                user.getCreatedAt(),
                roleNames
        );
    }
}
