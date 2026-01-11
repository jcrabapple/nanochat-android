# Security Policy for NanoChat Android

## Overview

NanoChat Android is a self-hosted chat client that connects to user-controlled backend instances. This document outlines the security architecture, implemented measures, and rationale for design decisions.

## Architecture Security Model

### Self-Hosted Design

NanoChat follows a **user-controlled security model**:
- Users deploy and manage their own nanochat backend
- The app connects to user-provided backend URLs
- Users control both the client (app) and server (backend)
- No hardcoded backend endpoints or domains

### Threat Model

**Considered Threats:**
✅ Device theft/loss (mitigated by encrypted storage)
✅ Backup data exposure (mitigated by backup exclusion)
✅ Logcat data access (mitigated by release build logging disable)
✅ App signing hijacking (mitigated by secure credential management)
✅ Network interception on public WiFi (mitigated by HTTPS)

**Out-of-Scope Threats:**
❌ Compromised user backend - Users control their backend security
❌ Rooted device attacks - Assumed beyond threat model for standard app
❌ Nation-state actors - Beyond typical mobile app security requirements

## Implemented Security Measures

### 1. Credential Management ✅

**Fixed:** Hardcoded signing credentials removed from version control

- Signing credentials loaded from `keystore.properties` (excluded from git)
- Fallback to debug signing for CI/CD builds
- Example file provided at `keystore.properties.example`

**Location:** `app/build.gradle.kts`

### 2. Data Backup Protection ✅

**Fixed:** Sensitive data excluded from cloud and device backups

- All SharedPreferences excluded from backups
- Session tokens never backed up to cloud
- User credentials not included in backup data
- Device-to-device transfer also excluded

**Protected Data:**
- Session tokens (authentication credentials)
- User IDs and emails
- Backend URLs
- TTS/STT settings
- Theme preferences

**Locations:**
- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`

### 3. HTTP Logging Security ✅

**Fixed:** Verbose HTTP logging disabled in release builds

- Debug builds: Full HTTP logging (Level.BODY) for development
- Release builds: No HTTP logging (Level.NONE)
- Authorization header tokens never logged
- Request/response bodies not exposed in production

**Locations:**
- `app/src/main/java/com/nanogpt/chat/di/NetworkModule.kt`

### 4. Encrypted Storage ✅

**Implemented:** Sensitive data stored with EncryptedSharedPreferences

- Session tokens encrypted with AES256_GCM
- Master key managed by AndroidKeyStore
- User credentials protected at rest
- No API keys stored in app (server-side only)

**Location:** `app/src/main/java/com/nanogpt/chat/data/local/SecureStorage.kt`

### 5. Network Security ✅

**Implemented:** HTTPS enforced for all backend communications

- All API calls use HTTPS
- No plaintext HTTP endpoints
- Standard TLS certificate validation via Android
- No hardcoded backend domains (user-controlled)

**Location:** `app/src/main/java/com/nanogpt/chat/di/NetworkModule.kt`

## Security Decisions and Rationale

### Certificate Pinning: NOT IMPLEMENTED

**Decision:** Certificate pinning is intentionally not implemented.

**Rationale:**

1. **Architectural Incompatibility**
   - NanoChat is designed for **self-hosted backends**
   - Users connect to their own backend instances (e.g., `myserver.com`, `nanochat.local`)
   - Each deployment has different domains and certificates
   - No single "official" backend to pin against

2. **User Freedom**
   - Users may use self-signed certificates for internal networks
   - Users may use custom CAs for their infrastructure
   - Let's Encrypt certificates are common and rotate regularly
   - Pinning would break legitimate self-hosted setups

3. **Threat Model Alignment**
   - Certificate pinning protects against **rogue API servers**
   - In NanoChat, users control their own backend
   - MITM attacks require compromising user's network or backend
   - This is outside the standard app threat model

4. **Maintainability**
   - Certificate pins require updates when certificates rotate
   - Users would need app updates for their own certificate changes
   - Support burden for self-hosted users with custom certificates

**Current Security Position:**
- HTTPS with standard certificate validation is sufficient
- Users control both endpoints (app + backend)
- Backend security is user's responsibility
- Compromised backend = compromised system (by design)

### Alternative Security Measures

Instead of certificate pinning, NanoChat implements:

1. **Encrypted storage** - Data protected at rest
2. **Backup exclusion** - No cloud data exposure
3. **No logging in release** - No logcat data exposure
4. **No hardcoded credentials** - Build security maintained
5. **HTTPS enforcement** - Transport layer encryption

## Security Best Practices for Users

### For Backend Deployment

1. **Use valid TLS certificates**
   - Let's Encrypt for public backends
   - Proper CA-signed certificates for production
   - Avoid self-signed certs for public-facing instances

2. **Secure backend configuration**
   - Enable firewall rules
   - Use strong database credentials
   - Keep nanochat backend updated

3. **Network security**
   - Deploy behind reverse proxy (nginx, Caddy)
   - Enable fail2ban or similar intrusion prevention
   - Use VPN for internal network access

### For Mobile App

1. **Keep app updated**
   - Install security updates promptly
   - Use Google Play Store or verified sources

2. **Device security**
   - Use device lock screen
   - Keep Android system updated
   - Avoid rooting your device

3. **Backup security**
   - Backups are excluded from cloud storage (by design)
   - Re-authenticate on new devices
   - Sessions are device-specific

## Security Audit History

### 2025-01-11 - Security Fixes Implemented

**Commit:** `f115c2c` - security: Disable HTTP logging in release builds
- Removed verbose HTTP logging from release builds
- Conditional logging based on `BuildConfig.DEBUG`
- Removed debug logs exposing session tokens

**Commit:** `9c15363` - security: Exclude SharedPreferences from cloud and device backups
- Excluded all SharedPreferences from backups
- Protected session tokens from cloud exposure
- Added device-to-device transfer protection

**Commit:** `6d10ef5` - security: Remove hardcoded signing credentials from build config
- Eliminated hardcoded keystore passwords
- Implemented `keystore.properties` loading
- Added CI/CD fallback support

### Severity Levels Addressed

- ✅ **CRITICAL:** Hardcoded signing credentials
- ✅ **HIGH:** Backup data exposure
- ✅ **HIGH:** Verbose HTTP logging in release
- ⏸️ **MEDIUM:** Certificate pinning (architecturally incompatible - see rationale)

## Reporting Security Vulnerabilities

If you discover a security vulnerability in NanoChat Android, please:

1. **Do not** create a public GitHub issue
2. **Do** send a report to the project maintainers
3. **Include** steps to reproduce, impact assessment, and suggested fix
4. **Allow** time for the fix to be released before disclosure

### Reporting Channels

- GitHub Security Advisory (private)
- Direct email to maintainers
- Security researcher contact programs

## Security Policy Version

**Version:** 1.0.0
**Last Updated:** 2025-01-11
**Status:** Active

## Compliance and Standards

NanoChat Android follows security best practices for:

- ✅ OWASP Mobile Top 10 (2024)
- ✅ Android Security Guidelines
- ✅ Google Play Security Best Practices
- ✅ NIST Mobile Application Security Guidelines

## Disclaimer

NanoChat is provided as-is for use with self-hosted backends. Users are responsible for:
- Their backend security configuration
- Their network security
- Their device security
- Proper certificate management for their backends

The app provides security measures for the **client-side** only. Server-side security is the backend's responsibility.
