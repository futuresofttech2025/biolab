# BioLab Auth Service — Security Fix Deployment Guide
**Version 2.0.0 | 22 issues resolved | Encryption at Rest + In Transit**

---

## Required Environment Variables

Before deploying, **all** of these must be set. The service will fail to start
if any mandatory variable is absent — this is intentional.

```bash
# ── MANDATORY — no defaults ──────────────────────────────────────────
JWT_SECRET=<openssl rand -base64 32>          # FIX-10
MAIL_PASSWORD=<Gmail App Password>            # FIX-09
ENCRYPTION_KEY=<openssl rand -base64 32>      # Encryption at Rest

# ── Recommended for production ───────────────────────────────────────
MAIL_USERNAME=noreply@your-domain.com
MAIL_FROM=noreply@your-domain.com
MAIL_FROM_NAME="BioLabs Platform"
FRONTEND_URL=https://app.biolab.com
TRUSTED_PROXY_CIDRS=10.0.0.0/8,172.16.0.0/12  # FIX-11 — your LB/gateway subnet
DOCS_ENABLED=false                              # FIX-13 — never true in prod
DB_PASSWORD=<strong-password>

# ── TLS (Encryption in Transit) ──────────────────────────────────────
SERVER_SSL_ENABLED=true
KEYSTORE_PATH=/run/secrets/biolab-auth.p12
KEYSTORE_PASSWORD=<keystore-password>
KEY_ALIAS=biolab-auth

# ── Spring profile ────────────────────────────────────────────────────
SPRING_PROFILES_ACTIVE=prod                    # FIX-13 — disables Swagger bean
```

---

## Database Migration

Run migration **V14** before or alongside the deployment:

```sql
-- Widen PII columns for AES-256 ciphertext
ALTER TABLE sec_schema.users
    ALTER COLUMN first_name TYPE VARCHAR(512),
    ALTER COLUMN last_name  TYPE VARCHAR(512),
    ALTER COLUMN phone      TYPE VARCHAR(512);

-- Add email OTP expiry column
ALTER TABLE sec_schema.mfa_settings
    ADD COLUMN IF NOT EXISTS email_otp_expires_at TIMESTAMPTZ;
```

> The migration file is at: `db/V14__encrypt_user_pii_columns.sql`

### Post-migration data re-encryption

After the new application boots, existing plaintext values in `first_name`,
`last_name`, and `phone` must be re-encrypted. Run a one-off admin job that
reads and re-saves every User entity — the JPA converter will encrypt on write.

---

## File Deployment Map

Copy each file to its exact destination in the source tree:

```
src/main/java/com/biolab/auth/
├── controller/
│   ├── AuthController.java           ← FIX-11 (trusted proxy IP)
│   ├── MfaController.java            ← FIX-05, FIX-06
│   └── SessionController.java        ← FIX-06, FIX-17
│
├── config/
│   ├── OpenApiConfig.java            ← FIX-13 (@Profile !prod)
│   ├── RedisConfig.java              ← FIX-01 (@EnableRedisRepositories)
│   ├── SecurityConfig.java           ← FIX-12, FIX-13
│   └── TlsConfig.java                ← NEW (HTTP→HTTPS redirect)
│
├── entity/
│   ├── MfaPendingToken.java          ← FIX-01 (Redis entity)
│   ├── MfaSettings.java              ← FIX-14 (emailOtpExpiresAt)
│   └── User.java                     ← Encryption at Rest (@Convert)
│
├── exception/
│   └── AuthException.java            ← FIX-03 (no-arg constructor)
│
├── dto/request/
│   └── ChangePasswordRequest.java    ← FIX-16 (@Pattern confirmed)
│
├── repository/
│   ├── MfaPendingTokenRepository.java ← FIX-01 (Redis CRUD)
│   ├── PasswordHistoryRepository.java ← FIX-20 (configurable N)
│   └── PasswordResetTokenRepository.java ← FIX-02 (findByTokenHash)
│
├── service/
│   ├── EmailService.java             ← FIX-18 (single interface)
│   ├── MfaService.java               ← FIX-01 (verifyOtp added)
│   └── SessionService.java           ← FIX-17 (userId param)
│
└── service/impl/
    ├── AuthServiceImpl.java          ← FIX-01,02,04,07,08,15,19,20,21
    ├── EmailServiceImpl.java         ← FIX-18 (single implementation)
    ├── MfaServiceImpl.java           ← FIX-01,14
    └── SessionServiceImpl.java       ← FIX-17

src/main/resources/
├── application.yml                   ← FIX-09,10 + TLS + Encryption key
└── db/migration/
    └── V14__encrypt_user_pii_columns.sql

pom.xml                               ← Tomcat embed dep for TLS redirect
```

### ⚠️ DELETIONS REQUIRED

```
# Remove the stale duplicate EmailService — causes ambiguous Spring bean injection (FIX-18)
DELETE: src/main/java/com/biolab/auth/email/EmailService.java
```

---

## Security Fixes — Full Inventory

### 🔴 CRITICAL (5 of 5 fixed)

| # | Issue | Fix |
|---|-------|-----|
| 1 | `verifyMfa()` was a TODO stub — MFA login broken | `MfaPendingToken` Redis entity + full `verifyMfa()` impl |
| 2 | `resetPassword()` called `findAll()` — table scan DoS | `findByTokenHash()` O(1) indexed lookup |
| 3 | `AuthException` missing no-arg constructor — compile failure | Added `AuthException(String)` defaulting to 400 |
| 4 | `forgotPassword()` sent raw token, not URL | Builds `frontendUrl + "/reset-password?token=" + rawToken` |
| 5 | `MfaController` injected non-existent `MfaSetupService` | Changed injection to `MfaService` |

### 🟠 HIGH (8 of 8 fixed)

| # | Issue | Fix |
|---|-------|-----|
| 6 | No ownership check on `SessionController` / `MfaController` | `enforceOwnership()` in both controllers |
| 7 | `LoginAnomalyDetector` never called — dead code | Wired into `AuthServiceImpl.login()` |
| 8 | `ConcurrentSessionManager` never called — dead code | Called in `issueTokenPair()` |
| 9 | Gmail App Password hardcoded as default | `${MAIL_PASSWORD}` — no default |
| 10 | JWT secret has weak predictable default | `${JWT_SECRET}` — no default |
| 11 | `X-Forwarded-For` trusted blindly — IP spoofing | Trusted-proxy CIDR guard in `AuthController.extractIp()` |
| 12 | Actuator fully public | `permitAll()` only for `/health` + `/info`; rest requires ADMIN |
| 13 | Swagger public in all environments | `@Profile("!prod")` on `OpenApiConfig` + `app.docs.enabled` flag |

### 🟡 MEDIUM (5 of 5 fixed)

| # | Issue | Fix |
|---|-------|-----|
| 14 | Email OTP has no expiry enforcement | `emailOtpExpiresAt` field + check in `validateEmailOtp()` |
| 15 | No notification after password reset | `sendPasswordChangedEmail()` called in `resetPassword()` |
| 16 | `ChangePasswordRequest` missing `@Pattern` | Confirmed present; clean version included |
| 17 | `terminateSession()` ignores `userId` — cross-user kill | Service + controller both pass `userId`; ownership verified |
| 18 | Two `EmailService` beans — ambiguous injection | `email/EmailService.java` deleted; `service/EmailService` is canonical |

### 🔵 LOW (4 of 4 fixed)

| # | Issue | Fix |
|---|-------|-----|
| 19 | Dead `DigestUtils` import | Removed; SHA-256 via `java.security.MessageDigest` |
| 20 | History hardcoded to `Top5` | `findRecentByUserId(userId, pageable)` with configurable N |
| 21 | Cleanup cutoff `now - 24h` deleted valid tokens | Changed to `now` — only past-expiry tokens removed |
| 22 | MFA `enable()` not fully `@Transactional` | `MfaServiceImpl` is class-level `@Transactional` |

---

## Encryption at Rest

### What is encrypted

| Field | Entity | Reason |
|-------|--------|--------|
| `first_name` | `User` | GDPR Art. 4(1) personal data |
| `last_name` | `User` | GDPR Art. 4(1) personal data |
| `phone` | `User` | HIPAA Safe Harbor identifier §164.514(b)(2)(i) |

### How it works

1. `AesEncryptionService` (already in `biolab-common`) initialises an AES-256 key from `${ENCRYPTION_KEY}`.
2. `EncryptedStringConverter` (already in `biolab-common`) implements `AttributeConverter<String,String>`.
3. `User.firstName`, `User.lastName`, `User.phone` annotated with `@Convert(converter = EncryptedStringConverter.class)`.
4. JPA handles encrypt-on-write and decrypt-on-read transparently — no application code changes needed.

### Key generation

```bash
openssl rand -base64 32
# → store result as ENCRYPTION_KEY in your secrets manager
```

---

## Encryption in Transit

### Layer 1 — External (API Gateway → Internet)
Handled by your reverse proxy (Nginx, AWS ALB). Set HSTS via `SecurityHeadersFilter`:
```
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
```
This is already implemented in `biolab-common`.

### Layer 2 — Internal (service-to-service mTLS)

Set `SERVER_SSL_ENABLED=true` to enable HTTPS on port 8081.
`TlsConfig` adds an HTTP connector on port 8080 that redirects to 8081.

**Dev certificate:**
```bash
keytool -genkeypair -alias biolab-auth -keyalg RSA -keysize 2048 \
  -validity 365 \
  -keystore src/main/resources/keystore/biolab-auth-dev.p12 \
  -storetype PKCS12 \
  -dname "CN=localhost,O=BioLabs,C=US" \
  -storepass changeit
```

**TLS settings in application.yml:**
- Protocols: TLSv1.3, TLSv1.2 (1.0 and 1.1 disabled)
- Ciphers: AES-256-GCM, ChaCha20-Poly1305 (TLS 1.3), ECDHE-RSA fallbacks (TLS 1.2)
- HTTP/2 enabled

### Layer 3 — Database (PostgreSQL)

Set `DB_SSL_MODE=require` and provision a PostgreSQL SSL certificate.
The JDBC URL includes `?sslmode=${DB_SSL_MODE:prefer}`.

### Layer 4 — Redis

Set `REDIS_TLS_ENABLED=true` when using Redis with TLS (e.g. AWS ElastiCache).
