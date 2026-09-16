package com.example.securestorage.dto;

import java.time.LocalDateTime;

public class FileMetadataDto {
    private Long id;
    private String fileName;
    private Long ownerId;
    private String ownerUsername;
    private String ownerFullName;
    private Long targetRoleId;
    private String targetRoleName;
    private String fileHash;
    private String encryptionStatus;
    private LocalDateTime uploadDate;
    private Long fileSize;
    private String encryptedPath;
    private String iv;

    public FileMetadataDto() {
    }

    public FileMetadataDto(Long id, String fileName, Long ownerId, String ownerUsername, String ownerFullName,
                           Long targetRoleId, String targetRoleName, String fileHash, String encryptionStatus,
                           LocalDateTime uploadDate, Long fileSize, String encryptedPath, String iv) {
        this.id = id;
        this.fileName = fileName;
        this.ownerId = ownerId;
        this.ownerUsername = ownerUsername;
        this.ownerFullName = ownerFullName;
        this.targetRoleId = targetRoleId;
        this.targetRoleName = targetRoleName;
        this.fileHash = fileHash;
        this.encryptionStatus = encryptionStatus;
        this.uploadDate = uploadDate;
        this.fileSize = fileSize;
        this.encryptedPath = encryptedPath;
        this.iv = iv;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public String getOwnerFullName() {
        return ownerFullName;
    }

    public void setOwnerFullName(String ownerFullName) {
        this.ownerFullName = ownerFullName;
    }

    public Long getTargetRoleId() {
        return targetRoleId;
    }

    public void setTargetRoleId(Long targetRoleId) {
        this.targetRoleId = targetRoleId;
    }

    public String getTargetRoleName() {
        return targetRoleName;
    }

    public void setTargetRoleName(String targetRoleName) {
        this.targetRoleName = targetRoleName;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public String getEncryptionStatus() {
        return encryptionStatus;
    }

    public void setEncryptionStatus(String encryptionStatus) {
        this.encryptionStatus = encryptionStatus;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getEncryptedPath() {
        return encryptedPath;
    }

    public void setEncryptedPath(String encryptedPath) {
        this.encryptedPath = encryptedPath;
    }

    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }
}
