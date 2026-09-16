package com.example.securestorage.service;

import com.example.securestorage.entity.FileMetadata;
import com.example.securestorage.entity.Role;
import com.example.securestorage.entity.RoleHierarchy;
import com.example.securestorage.entity.User;
import com.example.securestorage.repository.RoleHierarchyRepository;
import com.example.securestorage.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class RbacService {

    private final RoleHierarchyRepository roleHierarchyRepository;
    private final RoleRepository roleRepository;

    public RbacService(RoleHierarchyRepository roleHierarchyRepository, RoleRepository roleRepository) {
        this.roleHierarchyRepository = roleHierarchyRepository;
        this.roleRepository = roleRepository;
    }

    /**
     * Checks if a user has a specific role by name.
     */
    public boolean hasRole(User user, String roleName) {
        if (user == null || user.getRoles() == null) return false;
        return user.getRoles().stream()
                .anyMatch(r -> r.getName().equalsIgnoreCase(roleName));
    }

    /**
     * Checks if a user is an ADMIN.
     */
    public boolean isAdmin(User user) {
        return hasRole(user, "ROLE_ADMIN");
    }

    /**
     * Checks if a user is an AUDITOR.
     */
    public boolean isAuditor(User user) {
        return hasRole(user, "ROLE_AUDITOR");
    }

    /**
     * Checks if a user has a specific permission.
     */
    public boolean hasPermission(User user, String permissionName) {
        if (user == null) return false;
        if (isAdmin(user)) return true; // Admin has all permissions

        return user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .anyMatch(p -> p.getName().equalsIgnoreCase(permissionName));
    }

    /**
     * Returns the set of all Role IDs that the given user can access,
     * accounting for direct assignment and hierarchical inheritance (Parent -> Child).
     */
    @Transactional(readOnly = true)
    public Set<Long> getAccessibleRoleIds(User user) {
        Set<Long> accessibleRoleIds = new HashSet<>();
        if (user == null || user.getRoles() == null) {
            return accessibleRoleIds;
        }

        // If Admin, all roles are accessible
        if (isAdmin(user)) {
            roleRepository.findAll().forEach(r -> accessibleRoleIds.add(r.getId()));
            return accessibleRoleIds;
        }

        // Add user's direct roles
        for (Role role : user.getRoles()) {
            accessibleRoleIds.add(role.getId());
            // Collect child roles through the hierarchy
            collectSubordinateRoleIds(role.getId(), accessibleRoleIds);
        }

        return accessibleRoleIds;
    }

    private void collectSubordinateRoleIds(Long parentRoleId, Set<Long> visitedRoleIds) {
        List<Long> childRoleIds = roleHierarchyRepository.findDirectChildRoleIds(parentRoleId);
        for (Long childId : childRoleIds) {
            if (!visitedRoleIds.contains(childId)) {
                visitedRoleIds.add(childId);
                collectSubordinateRoleIds(childId, visitedRoleIds);
            }
        }
    }

    /**
     * Core RBAC Authorization check for files.
     * Evaluates ownership, admin privileges, and role hierarchy inheritance.
     */
    @Transactional(readOnly = true)
    public boolean canAccessFile(User user, FileMetadata file) {
        if (user == null || file == null) {
            return false;
        }

        // 1. Admin can access all files
        if (isAdmin(user)) {
            return true;
        }

        // 2. Owner can always access own files
        if (file.getOwner() != null && file.getOwner().getId() != null && Objects.equals(file.getOwner().getId(), user.getId())) {
            return true;
        }

        // 3. Auditor can view/audit files
        if (isAuditor(user)) {
            return true;
        }

        // 4. Role Hierarchy Check:
        // Does the user have a role that equals or inherits the file's target role?
        Set<Long> accessibleRoleIds = getAccessibleRoleIds(user);
        return file.getTargetRole() != null && accessibleRoleIds.contains(file.getTargetRole().getId());
    }

    /**
     * Checks if a user can delete a file.
     * Allowed if Admin or File Owner.
     */
    public boolean canDeleteFile(User user, FileMetadata file) {
        if (user == null || file == null) return false;
        if (isAdmin(user)) return true;
        return file.getOwner() != null && file.getOwner().getId() != null && Objects.equals(file.getOwner().getId(), user.getId());
    }
}
