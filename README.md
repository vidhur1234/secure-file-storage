# Secure File Storage System with Encryption and Role-Based Access Control (RBAC)

![Java](https://img.shields.io/badge/Java-21-orange?style=flat&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.3-brightgreen?style=flat&logo=springboot)
![Security](https://img.shields.io/badge/Encryption-AES--256--GCM-blue?style=flat&logo=lock)
![Integrity](https://img.shields.io/badge/Integrity-SHA--256-blueviolet?style=flat&logo=shield)
![Database](https://img.shields.io/badge/Database-MySQL_/_H2-informational?style=flat&logo=mysql)

A complete, demonstrable, end-to-end prototype of a **Secure Cloud/Local File Storage System** designed for B.Tech project presentation and viva examination. The application demonstrates real-world cryptographic controls, zero-plaintext storage at rest, hierarchical Role-Based Access Control (RBAC), and tamper-detection alarms.

---

## 1. Project Overview & Problem Statement

Standard file storage systems store files in plaintext on disk and rely solely on perimeter authentication. If the underlying storage or database is breached, all sensitive files are compromised.

This system solves this vulnerability through defense-in-depth:
1. **Zero-Plaintext at Rest**: Files are encrypted in RAM using **AES-256-GCM** before touching storage. Plaintext is never saved to disk.
2. **Per-File Key Isolation (Envelope Encryption)**: Every uploaded file is encrypted with its own randomly generated 256-bit AES key and 12-byte IV/nonce. The per-file key is wrapped with a 256-bit Master Key.
3. **Cryptographic Integrity Verification**: A **SHA-256** hash digest of the original file is computed at upload and stored in MySQL. At download, the file is decrypted and its SHA-256 is re-computed. Any modification or ransomware corruption triggers an instant **Tampering Alarm**.
4. **Hierarchical RBAC (Role-Based Access Control)**: Enforces access inheritance (`ADMIN > MANAGER > EMPLOYEE`, plus parallel `AUDITOR` track). Backend services strictly block unauthorized access (HTTP 403 Forbidden).
5. **Security Audit Trail**: All security events (Logins, Uploads, Downloads, 403 Denials, Tamper Alarms, User Modifications) are logged with timestamps and client IPs.

---

## 2. Technology Stack

* **Backend**: Java 21, Spring Boot 3.2.3, Spring Web, Spring Security, Spring Data JPA
* **Authentication**: Stateless JWT (JSON Web Tokens) with HMAC-SHA256 & BCrypt password hashing
* **Cryptography**:
  * File Encryption: `AES/GCM/NoPadding` (256-bit key, 128-bit authentication tag, 12-byte random nonce/IV)
  * Key Protection: Envelope Encryption (Server Master AES Key wraps per-file AES keys)
  * File Integrity: `SHA-256` Message Digest with constant-time byte comparison
* **Database**: MySQL 5.5+ / 8.0+ (with zero-config H2 MySQL-compatibility fallback)
* **Frontend**: HTML5, CSS3 (Modern Slate/Cybersecurity Dark Theme), Vanilla JavaScript, Bootstrap 5.3, FontAwesome 6

---

## 3. System Architecture & Workflow

### Architectural Diagram

```
+-----------------------------------------------------------------------------------+
|                       Frontend Web Browser (HTML5 / JS / CSS)                     |
|          Bootstrap 5 Cyber-Dashboard &bull; Dynamic Role-Adaptive Navigation      |
+-----------------------------------------+-----------------------------------------+
                                          | REST API Calls (Authorization: Bearer <JWT>)
                                          v
+-----------------------------------------------------------------------------------+
|                             Spring Boot 3.x REST API                              |
|           JwtAuthenticationFilter &bull; GlobalExceptionHandler &bull; CORS       |
+-----------------------------------------+-----------------------------------------+
                                          |
          +-------------------------------+-------------------------------+
          |                               |                               |
          v                               v                               v
+-----------------------+     +-----------------------+     +-----------------------+
|   EncryptionService   |     |      RbacService      |     |     AuditService      |
| - AES-256-GCM Engine  |     | - Role Hierarchy      |     | - Immutable Logging   |
| - SHA-256 Integrity   |     | - Permission Engine   |     | - Tamper Alarms       |
| - Envelope Master Key |     | - ADMIN>MANAGER>EMPL  |     | - IP & User Tracking  |
+-----------+-----------+     +-----------+-----------+     +-----------+-----------+
            |                             |                             |
            v                             v                             v
+-----------------------+     +-----------------------------------------------------+
| Encrypted Storage Dir |     |                   MySQL Database                    |
| (storage/encrypted/)  |     | Tables: users, roles, user_roles, role_hierarchy,   |
| * ONLY .enc files *   |     |         permissions, role_permissions,              |
| No Plaintext on Disk! |     |         files (metadata + wrapped key), audit_logs  |
+-----------------------+     +-----------------------------------------------------+
```

---

## 4. Cryptographic Implementation Details

### AES-256-GCM (Authenticated Encryption)
* **Cipher Mode**: Galois/Counter Mode (`AES/GCM/NoPadding`).
* **Why GCM?**: Unlike traditional CBC mode, GCM provides both **confidentiality** and **authenticity** (built-in 128-bit authentication tag). If any byte of the ciphertext or IV is altered, the cipher immediately rejects decryption during tag validation.
* **Per-File Key**: Generated dynamically using `KeyGenerator.getInstance("AES").init(256, SecureRandom)`.
* **Unique IV / Nonce**: 12 bytes (96 bits) generated uniquely per file using `SecureRandom`. Never reused.

### Envelope Encryption (Key Management)
* In production, Master Keys are managed by Hardware Security Modules (HSM) or AWS KMS.
* In this B.Tech prototype, a 256-bit server-side Master Key (`ENCRYPTION_MASTER_KEY`) wraps the per-file AES key using AES-GCM before saving to MySQL.
* The raw AES file key is discarded from memory immediately after encryption.

### SHA-256 Cryptographic Hash Digest
* **Integrity Fingerprint**: `MessageDigest.getInstance("SHA-256")` computes a 256-bit (64 hex characters) hash of the original plaintext bytes.
* **Verification**: Upon download, the decrypted bytes are hashed again. `MessageDigest.isEqual()` performs a constant-time comparison against the stored hash to prevent timing side-channel attacks.

---

## 5. Role Hierarchy & RBAC Rules

The system implements Role-Based Access Control with transitive hierarchy inheritance:

$$\text{ROLE\_ADMIN} \longrightarrow \text{ROLE\_MANAGER} \longrightarrow \text{ROLE\_EMPLOYEE}$$

$$\text{ROLE\_AUDITOR (Compliance \& Audit Track)}$$

| Action / Resource | ADMIN | MANAGER | EMPLOYEE | AUDITOR |
| :--- | :---: | :---: | :---: | :---: |
| **Manage Users & Keys** | Yes | No | No | No |
| **Manage Roles & Hierarchy** | Yes | No | No | No |
| **Upload Files** | Yes | Yes | Yes | No |
| **Access EMPLOYEE Files** | Yes | Yes (Inherited) | Yes | Yes (Audit) |
| **Access MANAGER Files** | Yes | Yes | **NO (403 Forbidden)** | Yes (Audit) |
| **Access ADMIN Files** | Yes | **NO (403 Forbidden)** | **NO (403 Forbidden)** | Yes (Audit) |
| **View Audit Trail** | Yes | No | No | Yes |

---

## 6. Demo Accounts (Seeded Automatically)

The application automatically seeds demo accounts on first run:

| Username | Password | Assigned Role | Capabilities |
| :--- | :--- | :--- | :--- |
| **admin** | `admin123` | `ROLE_ADMIN` | Full control, User CRUD, Role management, All files |
| **manager** | `manager123` | `ROLE_MANAGER` | Department files, inherits Employee files |
| **employee** | `employee123` | `ROLE_EMPLOYEE` | Standard employee files only |
| **auditor** | `auditor123` | `ROLE_AUDITOR` | View all files and system audit trail |

> [!NOTE]
> Passwords in the database are hashed using **BCrypt** with high work factor ($2a$10$). Plaintext passwords are never stored.

---

## 7. How to Run Locally

### Prerequisites
* **Java 21** or higher
* **Maven 3.8+** (or use included `mvnw.cmd` / `mvnw`)
* **MySQL 5.5+** or **8.0+** (Optional: H2 zero-config fallback is included!)

### Option A: Running with Local MySQL (Recommended for Viva)
1. Ensure MySQL is running on `localhost:3306`.
2. (Optional) Create database:
   ```sql
   CREATE DATABASE IF NOT EXISTS secure_storage_db;
   ```
3. Set your MySQL credentials in `src/main/resources/application.properties` or via environment variables:
   ```powershell
   $env:DB_URL = "jdbc:mysql://localhost:3306/secure_storage_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
   $env:DB_USERNAME = "root"
   $env:DB_PASSWORD = "your_mysql_password"
   ```
4. Run the application:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

### Option B: Running with Zero-Config Fallback (Instant 1-Click Run)
If you don't have MySQL installed or want to run instantly without entering passwords:
```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```
* Runs an in-memory/file-based database in full **MySQL compatibility mode**.
* Seeds all default tables, roles, and demo users immediately!

### Access the Web Application
Open your browser at:
**[http://localhost:8080](http://localhost:8080)**

---

## 8. Step-by-Step Viva Demonstration Script

Follow this exact sequence during your project evaluation:

### Step 1: Admin Login & Role Hierarchy Inspection
1. Open `http://localhost:8080` (or click demo button **admin**).
2. Enter `admin` / `admin123`.
3. View the **Admin Dashboard**: shows total users, active accounts, encrypted files count, and security controls.
4. Click **Roles** in navbar: observe the interactive **Role Hierarchy Diagram** (`ADMIN > MANAGER > EMPLOYEE`).

### Step 2: Upload File as Manager
1. Logout and log in as `manager` / `manager123`.
2. Navigate to **Upload**.
3. Select a confidential document (e.g. `Quarterly_Report.pdf`).
4. Set Target Role to `ROLE_MANAGER`.
5. Click **Encrypt with AES-256 & Upload**.
6. Observe the generated **Encryption Receipt**:
   * Stored Ciphertext file: `storage/encrypted/<UUID>.enc`
   * SHA-256 Hash Digest: generated in RAM
   * AES-GCM Nonce/IV: 12-byte unique value

### Step 3: Verify Zero-Plaintext on Disk
1. Open the project folder on your machine: `storage/encrypted/`.
2. Notice only `.enc` files exist.
3. Open any `.enc` file in Notepad or VS Code:
   * It contains **pure random ciphertext bytes**.
   * No plaintext words or document headers can be read!

### Step 4: Test RBAC Authorization & 403 Forbidden Rejection
1. Logout and log in as `employee` / `employee123`.
2. Go to **Accessible Files**.
3. The file uploaded for `ROLE_MANAGER` is **NOT visible** to `employee`.
4. If the employee attempts to download or access the Manager file directly via API, the backend immediately returns:
   ```json
   {
     "success": false,
     "message": "You are not authorized to access this file."
   }
   ```
   and records a `FORBIDDEN` entry in the Audit Log!

### Step 5: Authorized Download & Integrity Verification
1. Logout and log in as `manager` (or `admin` who inherits access).
2. Go to **Accessible Files**.
3. Click **Decrypt & Verify**.
4. The system:
   * Recovers the per-file AES key using the Master Key.
   * Decrypts ciphertext using AES-256-GCM.
   * Re-computes SHA-256 and compares with stored hash.
   * Displays green banner: **"✓ File Integrity Verified!"**
   * Downloads original, intact file to your computer.

### Step 6: Live Tampering Demonstration (Corrupt Ciphertext)
1. Open `storage/encrypted/` in File Explorer.
2. Open one of the `.enc` files in Notepad.
3. Add a few random letters (e.g., `TAMPERED`) at the end of the file and save it.
4. Go back to the browser and click **Decrypt & Verify** on that file.
5. **The alarm triggers immediately!**
   * Red banner appears: **"✗ File Integrity Verification Failed: File may have been tampered with."**
   * The tampered file is NOT delivered to the user.
   * The security breach is logged in Audit Logs with status `TAMPERED`.

### Step 7: Compliance & Audit Trail Review
1. Log in as `auditor` / `auditor123`.
2. Navigate to **Audit Logs**.
3. Filter by **Tamper Alarms** or **Unauthorized Attempts**:
   * Inspect timestamps, usernames, IP addresses, and detailed findings.

---

## 9. REST API Reference

| Method | Endpoint | Access Role | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Public | Authenticates user; returns JWT token |
| `GET` | `/api/auth/me` | Authenticated | Gets current user profile & permissions |
| `POST` | `/api/auth/logout` | Authenticated | Logs out and records audit entry |
| `GET` | `/api/users` | `ROLE_ADMIN` | Lists all users |
| `POST` | `/api/users` | `ROLE_ADMIN` | Creates new user account |
| `PUT` | `/api/users/{id}` | `ROLE_ADMIN` | Updates user details / password |
| `DELETE`| `/api/users/{id}` | `ROLE_ADMIN` | Deletes user account |
| `POST` | `/api/users/{id}/activate` | `ROLE_ADMIN` | Activates suspended user |
| `POST` | `/api/users/{id}/deactivate` | `ROLE_ADMIN` | Deactivates user account |
| `POST` | `/api/users/{id}/secret-key` | `ROLE_ADMIN` | Regenerates user secret key |
| `GET` | `/api/roles` | Authenticated | Lists all security roles |
| `POST` | `/api/roles` | `ROLE_ADMIN` | Creates new role |
| `GET` | `/api/roles/hierarchy` | Authenticated | Retrieves role hierarchy tree |
| `POST` | `/api/files/upload` | Authorized Roles | Encrypts with AES-256 and uploads file |
| `GET` | `/api/files` | Authenticated | Lists files accessible by user's role |
| `GET` | `/api/files/{id}/download` | Authorized Roles | Decrypts file & verifies SHA-256 |
| `GET` | `/api/files/search?keyword=`| Authenticated | RBAC-filtered file metadata search |
| `DELETE`| `/api/files/{id}` | Owner / ADMIN | Deletes encrypted file from disk and DB |
| `GET` | `/api/audit-logs` | `ADMIN`, `AUDITOR`| Returns immutable security audit trail |
| `GET` | `/api/dashboard/stats` | Authenticated | Provides system counters & metrics |

---

## 10. Cloud Deployment Guide

The project is packaged as a standard self-contained Spring Boot application with Docker and container support.

### Environment Variables
For production deployment, supply these environment variables:

| Variable | Description | Example |
| :--- | :--- | :--- |
| `PORT` | Dynamic web port assigned by cloud platform | `8080` or `10000` |
| `DB_URL` | JDBC connection string to cloud MySQL | `jdbc:mysql://host:3306/db?useSSL=true` |
| `DB_USERNAME` | Cloud MySQL database username | `root` or `admin` |
| `DB_PASSWORD` | Cloud MySQL database password | `secret_password` |
| `ENCRYPTION_MASTER_KEY` | 32-byte (64 hex characters) Master Key | `6d7953656375...` |
| `JWT_SECRET` | HMAC-SHA256 JWT signing secret key | `4a6f75726e6579...` |

### Deploy on Render (1-Click)
1. Push this repository to your GitHub account.
2. In [Render](https://render.com), click **New +** &rarr; **Blueprint**.
3. Connect your repository. Render reads `render.yaml` and `Dockerfile` automatically.
4. Done! Your web app and MySQL database are provisioned and connected.

### Deploy on Railway / Heroku
The included `Procfile` and `Dockerfile` allow instant deployment:
```bash
git push heroku master
```

> [!WARNING]
> **Deployment Storage Note**:
> Free cloud tiers (such as Render free or Heroku dynos) have ephemeral filesystem storage (files uploaded to `storage/encrypted/` reset upon container restart). For a production deployment beyond this B.Tech prototype, the encrypted bytes can be streamed directly to an Amazon S3 or Google Cloud Storage bucket.

---

## 11. Known Prototype Simplifications & Future Work

1. **Key Management**: Uses a Master Key in application configuration (Envelope Encryption) rather than a hardware HSM (e.g. AWS CloudHSM / YubiHSM).
2. **Encrypted Search**: Implements protected metadata index filtering rather than cryptographic Homomorphic Encryption or SSE (Searchable Symmetric Encryption).
3. **Storage Tier**: Simulates cloud blob storage using local filesystem directory `storage/encrypted/`.
