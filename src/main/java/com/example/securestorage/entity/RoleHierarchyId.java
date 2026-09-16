package com.example.securestorage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class RoleHierarchyId implements Serializable {

    @Column(name = "parent_role_id")
    private Long parentRoleId;

    @Column(name = "child_role_id")
    private Long childRoleId;

    public RoleHierarchyId() {
    }

    public RoleHierarchyId(Long parentRoleId, Long childRoleId) {
        this.parentRoleId = parentRoleId;
        this.childRoleId = childRoleId;
    }

    public Long getParentRoleId() {
        return parentRoleId;
    }

    public void setParentRoleId(Long parentRoleId) {
        this.parentRoleId = parentRoleId;
    }

    public Long getChildRoleId() {
        return childRoleId;
    }

    public void setChildRoleId(Long childRoleId) {
        this.childRoleId = childRoleId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoleHierarchyId that = (RoleHierarchyId) o;
        return Objects.equals(parentRoleId, that.parentRoleId) &&
               Objects.equals(childRoleId, that.childRoleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parentRoleId, childRoleId);
    }
}
