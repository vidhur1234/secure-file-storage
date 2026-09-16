package com.example.securestorage.config;

import com.example.securestorage.entity.*;
import com.example.securestorage.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleHierarchyRepository roleHierarchyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository,
                           PermissionRepository permissionRepository,
                           RoleHierarchyRepository roleHierarchyRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.roleHierarchyRepository = roleHierarchyRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        logger.info("Initializing security data, default roles, hierarchy, and demo users...");

        // 1. Initialize Permissions
        Permission permUpload = getOrCreatePermission("FILE_UPLOAD", "Upload files into encrypted storage");
        Permission permDownload = getOrCreatePermission("FILE_DOWNLOAD", "Download and decrypt authorized files");
        Permission permDelete = getOrCreatePermission("FILE_DELETE", "Delete files from encrypted storage");
        Permission permSearch = getOrCreatePermission("FILE_SEARCH", "Search accessible encrypted files metadata");
        Permission permUserManage = getOrCreatePermission("USER_MANAGE", "Manage user accounts, keys, and activation");
        Permission permRoleManage = getOrCreatePermission("ROLE_MANAGE", "Create and assign roles and hierarchy");
        Permission permAuditView = getOrCreatePermission("AUDIT_VIEW", "View security and compliance audit logs");

        // 2. Initialize Roles
        Role adminRole = getOrCreateRole("ROLE_ADMIN", "Administrator with full system control and access");
        adminRole.getPermissions().add(permUpload);
        adminRole.getPermissions().add(permDownload);
        adminRole.getPermissions().add(permDelete);
        adminRole.getPermissions().add(permSearch);
        adminRole.getPermissions().add(permUserManage);
        adminRole.getPermissions().add(permRoleManage);
        adminRole.getPermissions().add(permAuditView);
        roleRepository.save(adminRole);

        Role managerRole = getOrCreateRole("ROLE_MANAGER", "Department Manager with team file oversight");
        managerRole.getPermissions().add(permUpload);
        managerRole.getPermissions().add(permDownload);
        managerRole.getPermissions().add(permSearch);
        roleRepository.save(managerRole);

        Role employeeRole = getOrCreateRole("ROLE_EMPLOYEE", "Standard Employee with basic file storage access");
        employeeRole.getPermissions().add(permUpload);
        employeeRole.getPermissions().add(permDownload);
        employeeRole.getPermissions().add(permSearch);
        roleRepository.save(employeeRole);

        Role auditorRole = getOrCreateRole("ROLE_AUDITOR", "Security Auditor with read and log verification rights");
        auditorRole.getPermissions().add(permAuditView);
        auditorRole.getPermissions().add(permSearch);
        roleRepository.save(auditorRole);

        // 3. Initialize Role Hierarchy: ADMIN -> MANAGER -> EMPLOYEE
        setupRoleHierarchy(adminRole, managerRole);
        setupRoleHierarchy(managerRole, employeeRole);

        // 4. Initialize Demo Users
        createDemoUserIfMissing("admin", "admin123", "System Administrator", "admin@securestorage.local", adminRole);
        createDemoUserIfMissing("manager", "manager123", "Operations Manager", "manager@securestorage.local", managerRole);
        createDemoUserIfMissing("employee", "employee123", "John Doe (Employee)", "employee@securestorage.local", employeeRole);
        createDemoUserIfMissing("auditor", "auditor123", "Compliance Auditor", "auditor@securestorage.local", auditorRole);

        logger.info("Security data initialization completed successfully.");
    }

    private Permission getOrCreatePermission(String name, String description) {
        return permissionRepository.findByName(name).orElseGet(() -> {
            Permission p = new Permission(name, description);
            return permissionRepository.save(p);
        });
    }

    private Role getOrCreateRole(String name, String description) {
        return roleRepository.findByName(name).orElseGet(() -> {
            Role r = new Role(name, description);
            return roleRepository.save(r);
        });
    }

    private void setupRoleHierarchy(Role parentRole, Role childRole) {
        RoleHierarchyId id = new RoleHierarchyId(parentRole.getId(), childRole.getId());
        if (!roleHierarchyRepository.existsById(id)) {
            RoleHierarchy rh = new RoleHierarchy(parentRole, childRole);
            roleHierarchyRepository.save(rh);
        }
    }

    private void createDemoUserIfMissing(String username, String rawPassword, String fullName, String email, Role role) {
        if (!userRepository.existsByUsername(username)) {
            User user = new User(
                    username,
                    passwordEncoder.encode(rawPassword),
                    fullName,
                    email,
                    true
            );
            Set<Role> roles = new HashSet<>();
            roles.add(role);
            user.setRoles(roles);
            user.setSecretKey("SEC-" + username.toUpperCase() + "-KEY-99");
            user.setCreatedAt(LocalDateTime.now());
            userRepository.save(user);
            logger.info("Demo user initialized: {} (Role: {})", username, role.getName());
        }
    }
}
