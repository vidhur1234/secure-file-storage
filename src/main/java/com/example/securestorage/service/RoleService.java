package com.example.securestorage.service;

import com.example.securestorage.dto.RoleDto;
import com.example.securestorage.dto.RoleHierarchyDto;
import com.example.securestorage.entity.Permission;
import com.example.securestorage.entity.Role;
import com.example.securestorage.entity.User;
import com.example.securestorage.exception.FileStorageException;
import com.example.securestorage.exception.ResourceNotFoundException;
import com.example.securestorage.repository.PermissionRepository;
import com.example.securestorage.repository.RoleHierarchyRepository;
import com.example.securestorage.repository.RoleRepository;
import com.example.securestorage.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final RoleHierarchyRepository roleHierarchyRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public RoleService(RoleRepository roleRepository,
                       RoleHierarchyRepository roleHierarchyRepository,
                       PermissionRepository permissionRepository,
                       UserRepository userRepository,
                       AuditService auditService) {
        this.roleRepository = roleRepository;
        this.roleHierarchyRepository = roleHierarchyRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<RoleDto> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public RoleDto createRole(RoleDto roleDto, User actor, String clientIp) {
        String roleName = roleDto.getName().trim().toUpperCase();
        if (!roleName.startsWith("ROLE_")) {
            roleName = "ROLE_" + roleName;
        }

        if (roleRepository.existsByName(roleName)) {
            throw new FileStorageException("Role '" + roleName + "' already exists");
        }

        Role role = new Role(roleName, roleDto.getDescription());

        if (roleDto.getPermissions() != null && !roleDto.getPermissions().isEmpty()) {
            Set<Permission> permissions = new HashSet<>();
            for (String permName : roleDto.getPermissions()) {
                permissionRepository.findByName(permName).ifPresent(permissions::add);
            }
            role.setPermissions(permissions);
        }

        role = roleRepository.save(role);
        auditService.logUserAction(actor, "ROLE_CREATED", role.getName(), clientIp, "New security role created");

        return mapToDto(role);
    }

    @Transactional(readOnly = true)
    public List<RoleHierarchyDto> getRoleHierarchy() {
        return roleHierarchyRepository.findAllWithRoles().stream()
                .map(rh -> new RoleHierarchyDto(
                        rh.getParentRole().getId(),
                        rh.getParentRole().getName(),
                        rh.getChildRole().getId(),
                        rh.getChildRole().getName()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void assignRoleToUser(Long userId, Long roleId, User actor, String clientIp) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        user.getRoles().add(role);
        userRepository.save(user);

        auditService.logUserAction(actor, "ROLE_ASSIGNED", user.getUsername(), clientIp, "Assigned role: " + role.getName());
    }

    @Transactional
    public void removeRoleFromUser(Long userId, Long roleId, User actor, String clientIp) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        if (user.getUsername().equalsIgnoreCase("admin") && role.getName().equalsIgnoreCase("ROLE_ADMIN")) {
            throw new FileStorageException("Cannot revoke ROLE_ADMIN from root admin user.");
        }

        user.getRoles().remove(role);
        userRepository.save(user);

        auditService.logUserAction(actor, "ROLE_REVOKED", user.getUsername(), clientIp, "Revoked role: " + role.getName());
    }

    public RoleDto mapToDto(Role role) {
        Set<String> perms = role.getPermissions() != null
                ? role.getPermissions().stream().map(Permission::getName).collect(Collectors.toSet())
                : Collections.emptySet();

        return new RoleDto(role.getId(), role.getName(), role.getDescription(), perms);
    }
}
