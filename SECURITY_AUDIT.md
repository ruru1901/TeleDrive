# Security Audit Report - TeleDrive

**Date:** 2025-01-XX  
**Version:** 0.1.0  
**Status:** Under Development

---

## 🔒 Executive Summary

This document outlines security issues identified in the TeleDrive Android application. The app implements several strong security measures but has some areas that need attention before production release.

---

## ✅ Security Strengths

### 1. **Cryptography**
- ✅ Uses **Argon2id** for password hashing (OWASP recommended)
- ✅ Uses **AES-256-GCM** for encrypted preferences (AndroidX Security Crypto)
- ✅ Uses **XChaCha20-Poly1305 (IETF)** (Libsodium) for file encryption
- ✅ Uses **SQLCipher** for database encryption
- ✅ Proper use of `SecureRandom` for key generation

### 2. **Key Management**
- ✅ TDLib database key stored in encrypted preferences
- ✅ Master password never stored in plaintext
- ✅ Biometric authentication support

### 3. **Android Security**
- ✅ `allowBackup="false"` prevents backup of sensitive data
- ✅ Uses `FileProvider` for secure file sharing
- ✅ Encrypted SharedPreferences for API credentials

---

## ⚠️ Critical Security Issues

### 1. **Keystore Backup Sent to Telegram** 🔴 HIGH RISK

**Location:** `MasterPasswordService.kt` lines 30-40

```kotlin
withContext(Dispatchers.IO) {
    val msgId = runCatching { tdlib.sendMessage("#ghost_keystore — do not delete\n\n$blobB64", null) }.getOrNull()
    if (msgId != null) {
        runCatching { tdlib.pinMessage(msgId, null) }
    }
}
```

**Issue:** The encrypted keystore is sent to Telegram Saved Messages. While encrypted, this creates several risks:
- If the master password is weak, the keystore can be brute-forced
- The keystore is now stored on Telegram's servers
- If Telegram account is compromised, attacker gets the encrypted keystore
- Pinned messages are highly visible

**Recommendation:**
- Remove automatic Telegram backup or make it opt-in
- Add warning that keystore will be uploaded to Telegram
- Consider local-only backup with manual export option
- Implement key rotation mechanism

---

### 2. **Weak Argon2 Parameters** 🟡 MEDIUM RISK

**Location:** `MasterPasswordService.kt` line 103

```kotlin
argon2.hash(
    mode = Argon2Mode.ARGON2_ID,
    password = password.toByteArray(),
    salt = salt,
    tCostInIterations = 3,          // ⚠️ Too low
    mCostInKibibyte = 65536,        // 64 MB - acceptable
    parallelism = 1,
    hashLengthInBytes = 32,
)
```

**Issue:** `tCostInIterations = 3` is below OWASP recommendations for mobile devices.

**OWASP Recommendations:**
- Minimum: `t=2, m=19456 (19 MiB), p=1`
- Recommended: `t=3, m=65536 (64 MiB), p=4`

**Current Settings:**
- ✅ Memory: 64 MiB (good)
- ⚠️ Iterations: 3 (minimum acceptable)
- ⚠️ Parallelism: 1 (should be 4 for better security)

**Recommendation:**
```kotlin
tCostInIterations = 4,      // Increase to 4
mCostInKibibyte = 65536,    // Keep at 64 MB
parallelism = 4,            // Increase to 4
```

---

### 3. **ProGuard Rules Too Permissive** 🟡 MEDIUM RISK

**Location:** `proguard-rules.pro`

```proguard
-keep class com.teledrive.android.** { *; }
```

**Issue:** Keeps ALL classes and methods, defeating obfuscation entirely.

**Recommendation:**
```proguard
# Keep only necessary classes
-keep class com.teledrive.android.data.** { *; }
-keep class com.teledrive.android.telegram.TelegramGateway { *; }

# Obfuscate everything else
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Keep TDLib reflection targets
-keep class org.drinkless.tdlib.** { *; }
-keep class org.drinkless.td.libcore.telegram.** { *; }
```

---

### 4. **Overly Broad Permissions** 🟡 MEDIUM RISK

**Location:** `AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE"
    tools:ignore="ScopedStorage" />
```

**Issue:** `MANAGE_EXTERNAL_STORAGE` is a special permission that grants broad file access. Google Play may reject apps using this without valid justification.

**Recommendation:**
- Remove `MANAGE_EXTERNAL_STORAGE` permission
- Use Scoped Storage (Android 10+) with `MediaStore` API
- Use Storage Access Framework (SAF) for user-selected folders
- Current implementation already uses SAF in `TdLibTelegramGateway.kt`

---

### 5. **No Certificate Pinning** 🟢 LOW RISK

**Issue:** App doesn't implement certificate pinning for Telegram API connections.

**Impact:** Vulnerable to MITM attacks if device has compromised root certificates.

**Recommendation:**
- TDLib handles Telegram connections internally
- Consider adding network security config for additional protection
- Document that TDLib uses Telegram's MTProto protocol with built-in encryption

---

### 6. **Database Encryption Key Handling** 🟡 MEDIUM RISK

**Location:** `SecureSettings.kt` lines 67-78

```kotlin
fun tdlibDatabaseKey(): ByteArray {
    val existing = prefs.getString(KEY_TDLIB_DATABASE_KEY, null)
    if (!existing.isNullOrBlank()) {
        return Base64.decode(existing, Base64.NO_WRAP)
    }

    val key = ByteArray(32)
    SecureRandom().nextBytes(key)
    prefs.edit {
        putString(KEY_TDLIB_DATABASE_KEY, Base64.encodeToString(key, Base64.NO_WRAP))
    }
    return key
}
```

**Issue:** Database key is generated once and stored permanently. If device is compromised, key is accessible.

**Recommendation:**
- Derive database key from master password instead of storing it
- Or encrypt the database key with a key derived from master password
- Implement key rotation mechanism

---

### 7. **No Rate Limiting on Password Attempts** 🟡 MEDIUM RISK

**Location:** `MasterPasswordService.kt`

**Issue:** No rate limiting or account lockout after failed password attempts.

**Recommendation:**
```kotlin
private var failedAttempts = 0
private var lockoutUntil = 0L

suspend fun verifyMasterPassword(password: String): Boolean {
    if (System.currentTimeMillis() < lockoutUntil) {
        throw SecurityException("Too many failed attempts. Try again later.")
    }
    
    val result = verifyMasterPasswordInternal(password)
    
    if (!result) {
        failedAttempts++
        if (failedAttempts >= 5) {
            lockoutUntil = System.currentTimeMillis() + (60_000 * failedAttempts)
        }
    } else {
        failedAttempts = 0
    }
    
    return result
}
```

---

### 8. **Sensitive Data in Logs** 🟢 LOW RISK

**Issue:** No explicit log sanitization. Developers might accidentally log sensitive data during debugging.

**Recommendation:**
- Create custom logger that filters sensitive data
- Use ProGuard to remove all `Log.d()` calls in release builds
- Add lint rules to detect logging of sensitive fields

```proguard
# Remove debug logs in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
```

---

### 9. **No Root Detection** 🟢 LOW RISK

**Issue:** App doesn't detect rooted devices, which could compromise security.

**Recommendation:**
- Add root detection library (e.g., RootBeer)
- Warn users on rooted devices
- Consider disabling biometric auth on rooted devices
- Don't block app entirely (user choice)

---

### 10. **Backup Manifest Stored Unencrypted** 🟡 MEDIUM RISK

**Location:** `BackupManager.kt`

**Issue:** Backup manifests may contain file metadata that could leak information.

**Recommendation:**
- Encrypt backup manifests with master password-derived key
- Or store manifests in encrypted database instead of separate files

---

## 🔧 Recommended Security Enhancements

### 1. **Implement Secure Wipe**
```kotlin
fun secureWipe(data: ByteArray) {
    data.fill(0)
    System.gc()
}
```

### 2. **Add Security Event Logging**
- Log failed authentication attempts
- Log keystore access
- Log master password changes
- Store logs in encrypted database

### 3. **Implement App Lock Timer**
- Auto-lock after inactivity
- Require re-authentication after background

### 4. **Add Integrity Checks**
- Verify APK signature at runtime
- Detect tampering with SafetyNet/Play Integrity API

### 5. **Secure Memory Handling**
- Use `CharArray` instead of `String` for passwords
- Clear sensitive data from memory immediately after use

---

## 📋 Security Checklist for Production

- [ ] Remove or make optional Telegram keystore backup
- [ ] Increase Argon2 parameters (t=4, p=4)
- [ ] Fix ProGuard rules to enable obfuscation
- [ ] Remove `MANAGE_EXTERNAL_STORAGE` permission
- [ ] Implement rate limiting on password attempts
- [ ] Add root detection with user warning
- [ ] Encrypt backup manifests
- [ ] Add security event logging
- [ ] Implement auto-lock timer
- [ ] Add APK integrity checks
- [ ] Remove all debug logging in release builds
- [ ] Conduct penetration testing
- [ ] Third-party security audit
- [ ] Add security.txt file
- [ ] Document security architecture
- [ ] Create incident response plan

---

## 🎯 Priority Recommendations

### Immediate (Before Beta)
1. Fix Telegram keystore backup (make opt-in with warning)
2. Increase Argon2 parameters
3. Fix ProGuard rules
4. Remove MANAGE_EXTERNAL_STORAGE permission

### Before Production
5. Implement rate limiting
6. Add root detection
7. Encrypt backup manifests
8. Security audit

### Post-Launch
9. Implement secure wipe
10. Add integrity checks
11. Security event logging

---

## 📚 References

- [OWASP Mobile Security Testing Guide](https://owasp.org/www-project-mobile-security-testing-guide/)
- [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
- [Android Security Best Practices](https://developer.android.com/topic/security/best-practices)
- [Argon2 RFC 9106](https://www.rfc-editor.org/rfc/rfc9106.html)

---

**Note:** This is a living document and should be updated as security improvements are implemented.
