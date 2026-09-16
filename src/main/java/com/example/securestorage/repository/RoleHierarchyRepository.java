package com.example.securestorage.repository;

import com.example.securestorage.entity.RoleHierarchy;
import com.example.securestorage.entity.RoleHierarchyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleHierarchyRepository extends JpaRepository<RoleHierarchy, RoleHierarchyId> {
    List<RoleHierarchy> findByParentRoleId(Long parentRoleId);
    List<RoleHierarchy> findByChildRoleId(Long childRoleId);

    @Query("SELECT rh FROM RoleHierarchy rh JOIN FETCH rh.parentRole JOIN FETCH rh.childRole")
    List<RoleHierarchy> findAllWithRoles();

    @Query("SELECT rh.childRole.id FROM RoleHierarchy rh WHERE rh.parentRole.id = :parentRoleId")
    List<Long> findDirectChildRoleIds(@Param("parentRoleId") Long parentRoleId);
}
