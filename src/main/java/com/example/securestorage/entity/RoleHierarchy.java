package com.example.securestorage.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "role_hierarchy")
public class RoleHierarchy {

    @EmbeddedId
    private RoleHierarchyId id;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("parentRoleId")
    @JoinColumn(name = "parent_role_id")
    private Role parentRole;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("childRoleId")
    @JoinColumn(name = "child_role_id")
    private Role childRole;

    public RoleHierarchy() {
    }

    public RoleHierarchy(Role parentRole, Role childRole) {
        this.parentRole = parentRole;
        this.childRole = childRole;
        this.id = new RoleHierarchyId(parentRole.getId(), childRole.getId());
    }

    public RoleHierarchyId getId() {
        return id;
    }

    public void setId(RoleHierarchyId id) {
        this.id = id;
    }

    public Role getParentRole() {
        return parentRole;
    }

    public void setParentRole(Role parentRole) {
        this.parentRole = parentRole;
        if (this.id == null) {
            this.id = new RoleHierarchyId();
        }
        if (parentRole != null) {
            this.id.setParentRoleId(parentRole.getId());
        }
    }

    public Role getChildRole() {
        return childRole;
    }

    public void setChildRole(Role childRole) {
        this.childRole = childRole;
        if (this.id == null) {
            this.id = new RoleHierarchyId();
        }
        if (childRole != null) {
            this.id.setChildRoleId(childRole.getId());
        }
    }
}
