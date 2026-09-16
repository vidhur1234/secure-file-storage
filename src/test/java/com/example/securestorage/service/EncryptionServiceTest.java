package com.example.securestorage.service;

import com.example.securestorage.config.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    public void setUp() {
        StorageProperties properties = new StorageProperties();
        properties.getCrypto().setMasterKey("6d795365637572654d61737465724b657946696c6553746f7261676532303236");
        encryptionService = new EncryptionService(properties);
    }

    @Test
    public void testAesGcmEncryptionDecryptionRoundtrip() {
        String plaintext = "Confidential B.Tech Project Report: Top Secret Data!";
        byte[] plainBytes = plaintext.getBytes(StandardCharsets.UTF_8);

        SecretKey fileKey = encryptionService.generateFileKey();
        byte[] iv = encryptionService.generateIv();

        byte[] cipherBytes = encryptionService.encryptData(plainBytes, fileKey, iv);
        assertNotNull(cipherBytes);
        assertNotEquals(plaintext, new String(cipherBytes, StandardCharsets.UTF_8));

        byte[] decryptedBytes = encryptionService.decryptData(cipherBytes, fileKey, iv);
        assertEquals(plaintext, new String(decryptedBytes, StandardCharsets.UTF_8));
    }

    @Test
    public void testMasterKeyEnvelopeEncryption() {
        SecretKey originalKey = encryptionService.generateFileKey();

        String wrappedKeyBase64 = encryptionService.encryptFileKeyWithMasterKey(originalKey);
        assertNotNull(wrappedKeyBase64);

        SecretKey recoveredKey = encryptionService.decryptFileKeyWithMasterKey(wrappedKeyBase64);
        assertArrayEquals(originalKey.getEncoded(), recoveredKey.getEncoded());
    }

    @Test
    public void testSha256IntegrityVerificationPass() {
        byte[] content = "Important Financial Statement".getBytes(StandardCharsets.UTF_8);
        String hash = encryptionService.calculateSha256(content);

        assertNotNull(hash);
        assertEquals(64, hash.length());
        assertTrue(encryptionService.verifySha256Integrity(content, hash));
    }

    @Test
    public void testSha256IntegrityVerificationFailsOnTamperedContent() {
        byte[] content = "Legitimate Transaction $100".getBytes(StandardCharsets.UTF_8);
        String hash = encryptionService.calculateSha256(content);

        byte[] tamperedContent = "Tampered Transaction $99999".getBytes(StandardCharsets.UTF_8);
        assertFalse(encryptionService.verifySha256Integrity(tamperedContent, hash));
    }
}
