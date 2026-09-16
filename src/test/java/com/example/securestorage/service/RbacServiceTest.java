package com.example.securestorage.service;

import com.example.securestorage.entity.FileMetadata;
import com.example.securestorage.entity.Role;
import com.example.securestorage.entity.User;
import com.example.securestorage.repository.RoleHierarchyRepository;
import com.example.securestorage.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

public class RbacServiceTest {

    private RoleHierarchyRepository roleHierarchyRepository;
    private RoleRepository roleRepository;
    private RbacService rbacService;

    private Role adminRole;
    private Role managerRole;
    private Role employeeRole;
    private Role auditorRole;

    @BeforeEach
    public void setUp() {
        roleHierarchyRepository = Mockito.mock(RoleHierarchyRepository.class);
        roleRepository = Mockito.mock(RoleRepository.class);
        rbacService = new RbacService(roleHierarchyRepository, roleRepository);

        adminRole = new Role("ROLE_ADMIN", "Admin");
        adminRole.setId(1L);

        managerRole = new Role("ROLE_MANAGER", "Manager");
        managerRole.setId(2L);

        employeeRole = new Role("ROLE_EMPLOYEE", "Employee");
        employeeRole.setId(3L);

        auditorRole = new Role("ROLE_AUDITOR", "Auditor");
        auditorRole.setId(4L);

        // Mock hierarchy: MANAGER (2) -> EMPLOYEE (3)
        when(roleHierarchyRepository.findDirectChildRoleIds(2L)).thenReturn(List.of(3L));
        when(roleHierarchyRepository.findDirectChildRoleIds(3L)).thenReturn(Collections.emptyList());
        when(roleHierarchyRepository.findDirectChildRoleIds(4L)).thenReturn(Collections.emptyList());
    }

    @Test
    public void testAdminCanAccessAnyFile() {
        User admin = new User("admin", "pwd", "Admin User", "admin@test.com", true);
        admin.setId(10L);
        admin.setRoles(Set.of(adminRole));

        FileMetadata managerFile = new FileMetadata();
        managerFile.setTargetRole(managerRole);
        managerFile.setOwner(new User("other", "pwd", "Other", "other@test.com", true));

        assertTrue(rbacService.canAccessFile(admin, managerFile));
    }

    @Test
    public void testManagerCanAccessEmployeeFileViaInheritance() {
        User manager = new User("manager", "pwd", "Manager User", "mgr@test.com", true);
        manager.setId(20L);
        manager.setRoles(Set.of(managerRole));

        FileMetadata employeeFile = new FileMetadata();
        employeeFile.setTargetRole(employeeRole);
        employeeFile.setOwner(new User("emp", "pwd", "Emp", "emp@test.com", true));

        assertTrue(rbacService.canAccessFile(manager, employeeFile));
    }

    @Test
    public void testEmployeeCannotAccessManagerFile() {
        User employee = new User("employee", "pwd", "Employee User", "emp@test.com", true);
        employee.setId(30L);
        employee.setRoles(Set.of(employeeRole));

        FileMetadata managerFile = new FileMetadata();
        managerFile.setTargetRole(managerRole);
        User fileOwner = new User("mgr", "pwd", "Mgr", "mgr@test.com", true);
        fileOwner.setId(99L);
        managerFile.setOwner(fileOwner);

        assertFalse(rbacService.canAccessFile(employee, managerFile));
    }

    @Test
    public void testUserCanAccessOwnFileRegardlessOfRole() {
        User employee = new User("employee", "pwd", "Employee User", "emp@test.com", true);
        employee.setId(30L);
        employee.setRoles(Set.of(employeeRole));

        FileMetadata mySpecialFile = new FileMetadata();
        mySpecialFile.setTargetRole(managerRole);
        mySpecialFile.setOwner(employee); // employee owns it

        assertTrue(rbacService.canAccessFile(employee, mySpecialFile));
    }
}
