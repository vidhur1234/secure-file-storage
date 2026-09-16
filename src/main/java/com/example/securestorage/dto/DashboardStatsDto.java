package com.example.securestorage.dto;

public class DashboardStatsDto {
    private long totalUsers;
    private long activeUsers;
    private long totalFiles;
    private long totalRoles;
    private long totalStorageBytes;

    public DashboardStatsDto() {
    }

    public DashboardStatsDto(long totalUsers, long activeUsers, long totalFiles, long totalRoles, long totalStorageBytes) {
        this.totalUsers = totalUsers;
        this.activeUsers = activeUsers;
        this.totalFiles = totalFiles;
        this.totalRoles = totalRoles;
        this.totalStorageBytes = totalStorageBytes;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(long activeUsers) {
        this.activeUsers = activeUsers;
    }

    public long getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(long totalFiles) {
        this.totalFiles = totalFiles;
    }

    public long getTotalRoles() {
        return totalRoles;
    }

    public void setTotalRoles(long totalRoles) {
        this.totalRoles = totalRoles;
    }

    public long getTotalStorageBytes() {
        return totalStorageBytes;
    }

    public void setTotalStorageBytes(long totalStorageBytes) {
        this.totalStorageBytes = totalStorageBytes;
    }
}
