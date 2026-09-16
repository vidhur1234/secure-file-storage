package com.example.securestorage.dto;

public class RoleHierarchyDto {
    private Long parentRoleId;
    private String parentRoleName;
    private Long childRoleId;
    private String childRoleName;

    public RoleHierarchyDto() {
    }

    public RoleHierarchyDto(Long parentRoleId, String parentRoleName, Long childRoleId, String childRoleName) {
        this.parentRoleId = parentRoleId;
        this.parentRoleName = parentRoleName;
        this.childRoleId = childRoleId;
        this.childRoleName = childRoleName;
    }

    public Long getParentRoleId() {
        return parentRoleId;
    }

    public void setParentRoleId(Long parentRoleId) {
        this.parentRoleId = parentRoleId;
    }

    public String getParentRoleName() {
        return parentRoleName;
    }

    public void setParentRoleName(String parentRoleName) {
        this.parentRoleName = parentRoleName;
    }

    public Long getChildRoleId() {
        return childRoleId;
    }

    public void setChildRoleId(Long childRoleId) {
        this.childRoleId = childRoleId;
    }

    public String getChildRoleName() {
        return childRoleName;
    }

    public void setChildRoleName(String childRoleName) {
        this.childRoleName = childRoleName;
    }
}
