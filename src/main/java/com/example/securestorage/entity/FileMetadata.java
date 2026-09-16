package com.example.securestorage.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "files")
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role targetRole;

    @Column(name = "encrypted_path", nullable = false, length = 500)
    private String encryptedPath;

    @Column(name = "file_hash", nullable = false, length = 64)
    private String fileHash;

    @Column(name = "encryption_status", nullable = false, length = 50)
    private String encryptionStatus = "ENCRYPTED";

    @Lob
    @Column(name = "encrypted_key", nullable = false, columnDefinition = "TEXT")
    private String encryptedKey;

    @Column(nullable = false, length = 64)
    private String iv;

    @Column(name = "upload_date", nullable = false)
    private LocalDateTime uploadDate = LocalDateTime.now();

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    public FileMetadata() {
    }

    public FileMetadata(String fileName, User owner, Role targetRole, String encryptedPath,
                        String fileHash, String encryptedKey, String iv, Long fileSize) {
        this.fileName = fileName;
        this.owner = owner;
        this.targetRole = targetRole;
        this.encryptedPath = encryptedPath;
        this.fileHash = fileHash;
        this.encryptedKey = encryptedKey;
        this.iv = iv;
        this.fileSize = fileSize;
        this.uploadDate = LocalDateTime.now();
        this.encryptionStatus = "ENCRYPTED";
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

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public Role getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(Role targetRole) {
        this.targetRole = targetRole;
    }

    public String getEncryptedPath() {
        return encryptedPath;
    }

    public void setEncryptedPath(String encryptedPath) {
        this.encryptedPath = encryptedPath;
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

    public String getEncryptedKey() {
        return encryptedKey;
    }

    public void setEncryptedKey(String encryptedKey) {
        this.encryptedKey = encryptedKey;
    }

    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
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
}
