package com.example.securestorage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class StorageProperties {

    private Storage storage = new Storage();
    private Crypto crypto = new Crypto();
    private Jwt jwt = new Jwt();

    public static class Storage {
        private String encryptedDir = "storage/encrypted";

        public String getEncryptedDir() {
            return encryptedDir;
        }

        public void setEncryptedDir(String encryptedDir) {
            this.encryptedDir = encryptedDir;
        }
    }

    public static class Crypto {
        private String masterKey = "6d795365637572654d61737465724b657946696c6553746f7261676532303236";

        public String getMasterKey() {
            return masterKey;
        }

        public void setMasterKey(String masterKey) {
            this.masterKey = masterKey;
        }
    }

    public static class Jwt {
        private String secret;
        private long expirationMs = 86400000;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpirationMs() {
            return expirationMs;
        }

        public void setExpirationMs(long expirationMs) {
            this.expirationMs = expirationMs;
        }
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public Crypto getCrypto() {
        return crypto;
    }

    public void setCrypto(Crypto crypto) {
        this.crypto = crypto;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }
}
