package com.example.securestorage.service;

import com.example.securestorage.config.StorageProperties;
import com.example.securestorage.dto.FileMetadataDto;
import com.example.securestorage.entity.FileMetadata;
import com.example.securestorage.entity.Role;
import com.example.securestorage.entity.User;
import com.example.securestorage.exception.AccessDeniedCustomException;
import com.example.securestorage.exception.FileStorageException;
import com.example.securestorage.exception.IntegrityVerificationException;
import com.example.securestorage.exception.ResourceNotFoundException;
import com.example.securestorage.repository.FileMetadataRepository;
import com.example.securestorage.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    private final Path storageDirectory;
    private final FileMetadataRepository fileMetadataRepository;
    private final RoleRepository roleRepository;
    private final EncryptionService encryptionService;
    private final RbacService rbacService;
    private final AuditService auditService;

    public FileStorageService(StorageProperties storageProperties,
                              FileMetadataRepository fileMetadataRepository,
                              RoleRepository roleRepository,
                              EncryptionService encryptionService,
                              RbacService rbacService,
                              AuditService auditService) {
        this.storageDirectory = Paths.get(storageProperties.getStorage().getEncryptedDir())
                .toAbsolutePath().normalize();
        this.fileMetadataRepository = fileMetadataRepository;
        this.roleRepository = roleRepository;
        this.encryptionService = encryptionService;
        this.rbacService = rbacService;
        this.auditService = auditService;

        try {
            Files.createDirectories(this.storageDirectory);
        } catch (IOException e) {
            throw new FileStorageException("Could not create encrypted storage directory", e);
        }
    }

    /**
     * Uploads and encrypts a file. Plaintext is NEVER stored on disk.
     */
    @Transactional
    public FileMetadataDto uploadFile(MultipartFile multipartFile, Long targetRoleId, User currentUser, String clientIp) {
        if (multipartFile.isEmpty()) {
            throw new FileStorageException("Cannot upload an empty file");
        }

        String rawFileName = StringUtils.cleanPath(Objects.requireNonNull(multipartFile.getOriginalFilename()));
        if (rawFileName.contains("..")) {
            throw new FileStorageException("Filename contains invalid path sequence: " + rawFileName);
        }

        // Determine target role
        Role targetRole;
        if (targetRoleId != null) {
            targetRole = roleRepository.findById(targetRoleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Target role not found with id: " + targetRoleId));
        } else {
            // Default to user's first role
            targetRole = currentUser.getRoles().stream().findFirst()
                    .orElseThrow(() -> new FileStorageException("User has no assigned role for file ownership"));
        }

        try {
            byte[] originalBytes = multipartFile.getBytes();

            // 1. Calculate SHA-256 hash of original plaintext
            String fileHash = encryptionService.calculateSha256(originalBytes);

            // 2. Generate unique AES-256 key & IV for this file
            SecretKey fileKey = encryptionService.generateFileKey();
            byte[] iv = encryptionService.generateIv();

            // 3. Encrypt file payload using AES-256-GCM
            byte[] cipherBytes = encryptionService.encryptData(originalBytes, fileKey, iv);

            // 4. Encrypt the file key using Master Key
            String encryptedKeyBase64 = encryptionService.encryptFileKeyWithMasterKey(fileKey);
            String ivBase64 = Base64.getEncoder().encodeToString(iv);

            // 5. Generate secure random filename for ciphertext storage
            String storageFileName = UUID.randomUUID() + ".enc";
            Path targetPath = this.storageDirectory.resolve(storageFileName).normalize();

            // Security check: ensure path is within storageDirectory
            if (!targetPath.getParent().equals(this.storageDirectory)) {
                throw new FileStorageException("Cannot store file outside current storage directory");
            }

            // Write ONLY ciphertext to storage
            Files.write(targetPath, cipherBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // 6. Save metadata in MySQL
            FileMetadata metadata = new FileMetadata(
                    rawFileName,
                    currentUser,
                    targetRole,
                    storageFileName,
                    fileHash,
                    encryptedKeyBase64,
                    ivBase64,
                    (long) originalBytes.length
            );
            metadata = fileMetadataRepository.save(metadata);

            // 7. Audit log
            auditService.logFileUpload(currentUser, metadata.getId(), rawFileName, clientIp);

            return mapToDto(metadata);

        } catch (IOException e) {
            throw new FileStorageException("Failed to read upload file data", e);
        }
    }

    /**
     * Downloads and decrypts a file, verifying SHA-256 integrity.
     */
    @Transactional
    public DecryptedFileResult downloadFile(Long fileId, User currentUser, String clientIp) {
        FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));

        // 1. Enforce RBAC Authorization
        if (!rbacService.canAccessFile(currentUser, metadata)) {
            auditService.logAccessDenied(currentUser, "UNAUTHORIZED_DOWNLOAD_ATTEMPT", fileId, clientIp,
                    "Access denied to file '" + metadata.getFileName() + "' targeted for role: " + metadata.getTargetRole().getName());
            throw new AccessDeniedCustomException("You are not authorized to access this file.");
        }

        Path encryptedFilePath = this.storageDirectory.resolve(metadata.getEncryptedPath()).normalize();
        if (!Files.exists(encryptedFilePath)) {
            throw new ResourceNotFoundException("Encrypted file not found on storage disk: " + metadata.getEncryptedPath());
        }

        try {
            byte[] cipherBytes = Files.readAllBytes(encryptedFilePath);

            // 2. Recover per-file AES key
            SecretKey fileKey = encryptionService.decryptFileKeyWithMasterKey(metadata.getEncryptedKey());
            byte[] iv = Base64.getDecoder().decode(metadata.getIv());

            // 3. Decrypt ciphertext with AES-256-GCM
            byte[] decryptedBytes;
            try {
                decryptedBytes = encryptionService.decryptData(cipherBytes, fileKey, iv);
            } catch (Exception e) {
                // AES-GCM authentication tag mismatch indicates ciphertext modification
                auditService.logFileTampered(currentUser, fileId, metadata.getFileName(), clientIp,
                        "AES-GCM authentication tag verification failed. Ciphertext modified on disk!");
                throw new IntegrityVerificationException("Integrity Verification Failed: File may have been tampered with.");
            }

            // 4. Verify SHA-256 Integrity
            boolean integrityPassed = encryptionService.verifySha256Integrity(decryptedBytes, metadata.getFileHash());
            if (!integrityPassed) {
                auditService.logFileTampered(currentUser, fileId, metadata.getFileName(), clientIp,
                        "SHA-256 hash mismatch! Stored: " + metadata.getFileHash() + ", Computed: " + encryptionService.calculateSha256(decryptedBytes));
                throw new IntegrityVerificationException("Integrity Verification Failed: File may have been tampered with.");
            }

            // 5. Successful download & verified integrity
            auditService.logFileDownload(currentUser, fileId, metadata.getFileName(), "INTEGRITY_VERIFIED", clientIp,
                    "Decrypted successfully and SHA-256 verified (" + metadata.getFileHash().substring(0, 16) + "...)");

            return new DecryptedFileResult(metadata.getFileName(), decryptedBytes, metadata.getFileHash());

        } catch (IOException e) {
            throw new FileStorageException("Failed to read encrypted file from storage", e);
        }
    }

    /**
     * Retrieves all files accessible by the current user according to RBAC.
     */
    @Transactional(readOnly = true)
    public List<FileMetadataDto> getAccessibleFiles(User currentUser) {
        if (rbacService.isAdmin(currentUser) || rbacService.isAuditor(currentUser)) {
            return fileMetadataRepository.findAll().stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
        }

        Set<Long> accessibleRoleIds = rbacService.getAccessibleRoleIds(currentUser);
        return fileMetadataRepository.findAccessibleFiles(accessibleRoleIds, currentUser.getId()).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Basic Encrypted File Search Prototype.
     * Searches protected metadata index filtered strictly by current user's RBAC permissions.
     */
    @Transactional(readOnly = true)
    public List<FileMetadataDto> searchFiles(String keyword, User currentUser) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAccessibleFiles(currentUser);
        }

        String sanitizedKeyword = keyword.trim();
        if (rbacService.isAdmin(currentUser) || rbacService.isAuditor(currentUser)) {
            return fileMetadataRepository.searchAllFiles(sanitizedKeyword).stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
        }

        Set<Long> accessibleRoleIds = rbacService.getAccessibleRoleIds(currentUser);
        return fileMetadataRepository.searchAccessibleFiles(accessibleRoleIds, currentUser.getId(), sanitizedKeyword).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Deletes a file.
     */
    @Transactional
    public void deleteFile(Long fileId, User currentUser, String clientIp) {
        FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));

        if (!rbacService.canDeleteFile(currentUser, metadata)) {
            auditService.logAccessDenied(currentUser, "UNAUTHORIZED_DELETE_ATTEMPT", fileId, clientIp,
                    "Access denied when attempting to delete file: " + metadata.getFileName());
            throw new AccessDeniedCustomException("You are not authorized to delete this file.");
        }

        try {
            Path encryptedFilePath = this.storageDirectory.resolve(metadata.getEncryptedPath()).normalize();
            Files.deleteIfExists(encryptedFilePath);
        } catch (IOException e) {
            logger.warn("Could not delete physical encrypted file: {}", metadata.getEncryptedPath());
        }

        fileMetadataRepository.delete(metadata);
        auditService.logEvent(currentUser.getId(), currentUser.getUsername(), "FILE_DELETE", fileId, "SUCCESS", clientIp,
                "Deleted file: " + metadata.getFileName());
    }

    public FileMetadataDto getFileById(Long fileId, User currentUser) {
        FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));

        if (!rbacService.canAccessFile(currentUser, metadata)) {
            throw new AccessDeniedCustomException("You are not authorized to view this file.");
        }

        return mapToDto(metadata);
    }

    private FileMetadataDto mapToDto(FileMetadata entity) {
        return new FileMetadataDto(
                entity.getId(),
                entity.getFileName(),
                entity.getOwner() != null ? entity.getOwner().getId() : null,
                entity.getOwner() != null ? entity.getOwner().getUsername() : "UNKNOWN",
                entity.getOwner() != null ? entity.getOwner().getFullName() : "UNKNOWN",
                entity.getTargetRole() != null ? entity.getTargetRole().getId() : null,
                entity.getTargetRole() != null ? entity.getTargetRole().getName() : "UNKNOWN",
                entity.getFileHash(),
                entity.getEncryptionStatus(),
                entity.getUploadDate(),
                entity.getFileSize(),
                entity.getEncryptedPath(),
                entity.getIv()
        );
    }

    public static class DecryptedFileResult {
        private final String fileName;
        private final byte[] data;
        private final String fileHash;

        public DecryptedFileResult(String fileName, byte[] data, String fileHash) {
            this.fileName = fileName;
            this.data = data;
            this.fileHash = fileHash;
        }

        public String getFileName() {
            return fileName;
        }

        public byte[] getData() {
            return data;
        }

        public String getFileHash() {
            return fileHash;
        }
    }
}
