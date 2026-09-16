package com.example.securestorage.service;

import com.example.securestorage.config.StorageProperties;
import com.example.securestorage.exception.FileStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class EncryptionService {

    private static final Logger logger = LoggerFactory.getLogger(EncryptionService.class);

    private static final String AES_ALGORITHM = "AES";
    private static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_IV_LENGTH_BYTES = 12; // 96-bit recommended nonce for GCM
    private static final int AES_KEY_SIZE_BITS = 256;

    private final SecureRandom secureRandom = new SecureRandom();
    private final byte[] masterKeyBytes;

    public EncryptionService(StorageProperties storageProperties) {
        String masterKeyHex = storageProperties.getCrypto().getMasterKey();
        this.masterKeyBytes = HexFormat.of().parseHex(masterKeyHex);
        if (this.masterKeyBytes.length != 32) {
            throw new IllegalArgumentException("Master key must be exactly 32 bytes (256 bits) in hex format.");
        }
    }

    /**
     * Generates a cryptographically strong, random 256-bit AES key for a file.
     */
    public SecretKey generateFileKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALGORITHM);
            keyGen.init(AES_KEY_SIZE_BITS, secureRandom);
            return keyGen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new FileStorageException("Failed to generate AES-256 key", e);
        }
    }

    /**
     * Generates a cryptographically strong random IV (12 bytes) for AES-GCM.
     */
    public byte[] generateIv() {
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);
        return iv;
    }

    /**
     * Encrypts plaintext bytes using AES-256-GCM.
     */
    public byte[] encryptData(byte[] plaintext, SecretKey fileKey, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, fileKey, parameterSpec);
            return cipher.doFinal(plaintext);
        } catch (Exception e) {
            logger.error("Error encrypting data with AES-256-GCM", e);
            throw new FileStorageException("Encryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypts ciphertext bytes using AES-256-GCM.
     */
    public byte[] decryptData(byte[] ciphertext, SecretKey fileKey, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, fileKey, parameterSpec);
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            logger.error("Error decrypting data with AES-256-GCM", e);
            throw new FileStorageException("Decryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Protects the per-file AES key by encrypting it with the Master Key (Envelope Encryption).
     * Format: Base64(IV + Ciphertext)
     */
    public String encryptFileKeyWithMasterKey(SecretKey fileKey) {
        try {
            byte[] iv = generateIv();
            SecretKeySpec masterKeySpec = new SecretKeySpec(masterKeyBytes, AES_ALGORITHM);
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, masterKeySpec, parameterSpec);

            byte[] encryptedKeyBytes = cipher.doFinal(fileKey.getEncoded());
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encryptedKeyBytes.length);
            buffer.put(iv);
            buffer.put(encryptedKeyBytes);

            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            logger.error("Failed to wrap file key with master key", e);
            throw new FileStorageException("Key protection failed: " + e.getMessage(), e);
        }
    }

    /**
     * Recovers the per-file AES key by decrypting with the Master Key.
     */
    public SecretKey decryptFileKeyWithMasterKey(String base64ProtectedKey) {
        try {
            byte[] combined = Base64.getDecoder().decode(base64ProtectedKey);
            ByteBuffer buffer = ByteBuffer.wrap(combined);

            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            buffer.get(iv);

            byte[] encryptedKeyBytes = new byte[buffer.remaining()];
            buffer.get(encryptedKeyBytes);

            SecretKeySpec masterKeySpec = new SecretKeySpec(masterKeyBytes, AES_ALGORITHM);
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, masterKeySpec, parameterSpec);

            byte[] plainKeyBytes = cipher.doFinal(encryptedKeyBytes);
            return new SecretKeySpec(plainKeyBytes, AES_ALGORITHM);
        } catch (Exception e) {
            logger.error("Failed to unwrap file key with master key", e);
            throw new FileStorageException("Key recovery failed: " + e.getMessage(), e);
        }
    }

    /**
     * Computes the SHA-256 hash of a byte array and returns as hex string.
     */
    public String calculateSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data);
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new FileStorageException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Verifies data integrity using constant-time comparison against stored SHA-256 hash.
     */
    public boolean verifySha256Integrity(byte[] decryptedData, String expectedHash) {
        String actualHash = calculateSha256(decryptedData);
        byte[] expectedBytes = expectedHash.toLowerCase().getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actualHash.toLowerCase().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }
}
