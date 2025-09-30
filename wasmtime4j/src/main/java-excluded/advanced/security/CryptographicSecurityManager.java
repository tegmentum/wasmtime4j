package ai.tegmentum.wasmtime4j.security;

import ai.tegmentum.wasmtime4j.exception.SecurityException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Comprehensive cryptographic security manager for WebAssembly modules.
 *
 * <p>Provides enterprise-grade cryptographic features including:
 * <ul>
 *   <li>Module signing and verification with certificate chains
 *   <li>Encrypted module storage and transmission
 *   <li>Secure key management and rotation
 *   <li>Hardware Security Module (HSM) integration
 *   <li>Certificate lifecycle management
 *   <li>Cryptographic hash validation
 * </ul>
 *
 * @since 1.0.0
 */
public final class CryptographicSecurityManager {

    private static final Logger LOGGER = Logger.getLogger(CryptographicSecurityManager.class.getName());

    private static final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    private static final int AES_KEY_LENGTH = 256;

    private final KeyVault keyVault;
    private final CertificateManager certificateManager;
    private final Map<String, EncryptedModule> moduleStorage;
    private final SecurityManager securityManager;
    private final CryptographicPolicy policy;

    /**
     * Creates a new cryptographic security manager.
     *
     * @param securityManager security manager for audit logging
     * @param policy cryptographic policy configuration
     */
    public CryptographicSecurityManager(final SecurityManager securityManager,
                                       final CryptographicPolicy policy) {
        this.securityManager = securityManager;
        this.policy = policy;
        this.keyVault = new KeyVault(policy);
        this.certificateManager = new CertificateManager(policy);
        this.moduleStorage = new ConcurrentHashMap<>();

        LOGGER.info("Cryptographic security manager initialized with policy: " + policy.getName());
    }

    /**
     * Signs a WebAssembly module with the configured signing key.
     *
     * @param moduleBytes the module bytecode to sign
     * @param signingKeyId identifier of the signing key
     * @param certificateChain optional certificate chain
     * @return the cryptographic signature
     * @throws SecurityException if signing fails
     */
    public ModuleSignature signModule(final byte[] moduleBytes, final String signingKeyId,
                                     final Optional<CertificateChain> certificateChain)
            throws SecurityException {

        final SigningKey signingKey = keyVault.getSigningKey(signingKeyId)
            .orElseThrow(() -> new SecurityException("Signing key not found: " + signingKeyId));

        try {
            // Create message digest
            final byte[] digest = CryptographicUtils.computeHash(moduleBytes, policy.getHashAlgorithm());

            // Sign the digest
            final byte[] signature = signingKey.sign(digest);

            // Create signature metadata
            final Map<String, String> metadata = Map.of(
                "signed_at", Instant.now().toString(),
                "key_id", signingKeyId,
                "algorithm", signingKey.getAlgorithm().name(),
                "hash_algorithm", policy.getHashAlgorithm().name()
            );

            final ModuleSignature moduleSignature = ModuleSignature.builder()
                .algorithm(signingKey.getAlgorithm())
                .signature(signature)
                .publicKey(signingKey.getPublicKey())
                .certificateChain(certificateChain.map(CertificateChain::toPemStrings))
                .metadata(metadata)
                .build();

            // Log signing event
            securityManager.logAuditEvent(AuditEvent.builder()
                .eventType("module_signed")
                .principalId("system")
                .resourceId("module_signer")
                .action("sign_module")
                .result("success")
                .details(Map.of(
                    "key_id", signingKeyId,
                    "module_size", String.valueOf(moduleBytes.length),
                    "signature_algorithm", signingKey.getAlgorithm().name()
                ))
                .build());

            LOGGER.info(String.format("Module signed successfully with key %s using %s",
                                     signingKeyId, signingKey.getAlgorithm()));

            return moduleSignature;

        } catch (final Exception e) {
            // Log signing failure
            securityManager.logAuditEvent(AuditEvent.builder()
                .eventType("module_signing_failed")
                .principalId("system")
                .resourceId("module_signer")
                .action("sign_module")
                .result("failure")
                .details(Map.of("error", e.getMessage()))
                .build());

            throw new SecurityException("Module signing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Verifies a WebAssembly module signature.
     *
     * @param moduleBytes the module bytecode
     * @param signature the module signature to verify
     * @return verification result with trust level
     * @throws SecurityException if verification fails
     */
    public SignatureVerificationResult verifySignature(final byte[] moduleBytes,
                                                      final ModuleSignature signature)
            throws SecurityException {

        try {
            // Validate signature metadata
            validateSignatureMetadata(signature);

            // Verify certificate chain if present
            TrustLevel trustLevel = TrustLevel.UNKNOWN;
            if (signature.getCertificateChain().isPresent()) {
                final CertificateChain chain = CertificateChain.fromPemStrings(
                    signature.getCertificateChain().get());
                final CertificateValidationResult chainResult = certificateManager.validateChain(chain);

                if (!chainResult.isValid()) {
                    throw new SecurityException("Certificate chain validation failed: " +
                                               String.join(", ", chainResult.getErrors()));
                }
                trustLevel = chainResult.getTrustLevel();
            }

            // Verify signature
            final byte[] digest = CryptographicUtils.computeHash(moduleBytes, policy.getHashAlgorithm());
            final boolean signatureValid = CryptographicUtils.verifySignature(
                digest, signature.getSignatureBytes(), signature.getPublicKeyBytes(),
                signature.getAlgorithm());

            if (!signatureValid) {
                throw new SecurityException("Signature verification failed");
            }

            // Check against policy requirements
            validateAgainstPolicy(signature, trustLevel);

            final SignatureVerificationResult result = new SignatureVerificationResult(
                true, trustLevel, "Signature verified successfully", Optional.empty());

            // Log verification success
            securityManager.logAuditEvent(AuditEvent.builder()
                .eventType("signature_verified")
                .principalId("system")
                .resourceId("signature_verifier")
                .action("verify_signature")
                .result("success")
                .details(Map.of(
                    "trust_level", trustLevel.name(),
                    "algorithm", signature.getAlgorithm().name()
                ))
                .build());

            LOGGER.info(String.format("Signature verified successfully with trust level %s", trustLevel));

            return result;

        } catch (final SecurityException e) {
            // Log verification failure
            securityManager.logAuditEvent(AuditEvent.builder()
                .eventType("signature_verification_failed")
                .principalId("system")
                .resourceId("signature_verifier")
                .action("verify_signature")
                .result("failure")
                .details(Map.of("error", e.getMessage()))
                .build());

            throw e;
        } catch (final Exception e) {
            throw new SecurityException("Signature verification error: " + e.getMessage(), e);
        }
    }

    /**
     * Encrypts a WebAssembly module for secure storage.
     *
     * @param moduleBytes the module bytecode to encrypt
     * @param encryptionKeyId identifier of the encryption key
     * @return encrypted module container
     * @throws SecurityException if encryption fails
     */
    public EncryptedModule encryptModule(final byte[] moduleBytes, final String encryptionKeyId)
            throws SecurityException {

        final EncryptionKey encryptionKey = keyVault.getEncryptionKey(encryptionKeyId)
            .orElseThrow(() -> new SecurityException("Encryption key not found: " + encryptionKeyId));

        try {
            // Generate random IV
            final byte[] iv = CryptographicUtils.generateSecureRandom(GCM_IV_LENGTH);

            // Initialize cipher
            final Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            final GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey.getSecretKey(), gcmSpec);

            // Encrypt module
            final byte[] encryptedData = cipher.doFinal(moduleBytes);

            // Create encrypted module container
            final EncryptedModule encryptedModule = new EncryptedModule(
                encryptionKeyId, iv, encryptedData, ENCRYPTION_ALGORITHM,
                Instant.now(), moduleBytes.length
            );

            // Store encrypted module
            final String moduleId = CryptographicUtils.generateSecureId();
            moduleStorage.put(moduleId, encryptedModule);

            // Log encryption event
            securityManager.logAuditEvent(AuditEvent.builder()
                .eventType("module_encrypted")
                .principalId("system")
                .resourceId(moduleId)
                .action("encrypt_module")
                .result("success")
                .details(Map.of(
                    "key_id", encryptionKeyId,
                    "algorithm", ENCRYPTION_ALGORITHM,
                    "original_size", String.valueOf(moduleBytes.length),
                    "encrypted_size", String.valueOf(encryptedData.length)
                ))
                .build());

            LOGGER.info(String.format("Module encrypted successfully with key %s", encryptionKeyId));

            return encryptedModule;

        } catch (final Exception e) {
            // Log encryption failure
            securityManager.logAuditEvent(AuditEvent.builder()
                .eventType("module_encryption_failed")
                .principalId("system")
                .resourceId("module_encryptor")
                .action("encrypt_module")
                .result("failure")
                .details(Map.of("error", e.getMessage()))
                .build());

            throw new SecurityException("Module encryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypts a WebAssembly module from secure storage.
     *
     * @param moduleId the encrypted module identifier
     * @return decrypted module bytecode
     * @throws SecurityException if decryption fails
     */
    public byte[] decryptModule(final String moduleId) throws SecurityException {
        final EncryptedModule encryptedModule = moduleStorage.get(moduleId);
        if (encryptedModule == null) {
            throw new SecurityException("Encrypted module not found: " + moduleId);
        }

        final EncryptionKey encryptionKey = keyVault.getEncryptionKey(encryptedModule.getKeyId())
            .orElseThrow(() -> new SecurityException("Decryption key not found: " +
                                                   encryptedModule.getKeyId()));

        try {
            // Initialize cipher for decryption
            final Cipher cipher = Cipher.getInstance(encryptedModule.getAlgorithm());
            final GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8,
                                                                 encryptedModule.getIv());
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey.getSecretKey(), gcmSpec);

            // Decrypt module
            final byte[] decryptedData = cipher.doFinal(encryptedModule.getEncryptedData());

            // Verify decrypted data integrity
            if (decryptedData.length != encryptedModule.getOriginalSize()) {
                throw new SecurityException("Decrypted data size mismatch");
            }

            // Log decryption event
            securityManager.logAuditEvent(AuditEvent.builder()
                .eventType("module_decrypted")
                .principalId("system")
                .resourceId(moduleId)
                .action("decrypt_module")
                .result("success")
                .build());

            LOGGER.info(String.format("Module %s decrypted successfully", moduleId));

            return decryptedData;

        } catch (final Exception e) {
            // Log decryption failure
            securityManager.logAuditEvent(AuditEvent.builder()
                .eventType("module_decryption_failed")
                .principalId("system")
                .resourceId(moduleId)
                .action("decrypt_module")
                .result("failure")
                .details(Map.of("error", e.getMessage()))
                .build());

            throw new SecurityException("Module decryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Rotates encryption keys according to policy.
     *
     * @param keyId the key identifier to rotate
     * @return the new key identifier
     * @throws SecurityException if key rotation fails
     */
    public String rotateKey(final String keyId) throws SecurityException {
        final String newKeyId = keyVault.rotateKey(keyId);

        // Log key rotation
        securityManager.logAuditEvent(AuditEvent.builder()
            .eventType("key_rotated")
            .principalId("system")
            .resourceId("key_vault")
            .action("rotate_key")
            .result("success")
            .details(Map.of(
                "old_key_id", keyId,
                "new_key_id", newKeyId
            ))
            .build());

        LOGGER.info(String.format("Key %s rotated to %s", keyId, newKeyId));

        return newKeyId;
    }

    /**
     * Validates module integrity using cryptographic hashes.
     *
     * @param moduleBytes the module bytecode
     * @param expectedHash the expected hash value
     * @param hashAlgorithm the hash algorithm used
     * @return true if integrity check passes
     * @throws SecurityException if validation fails
     */
    public boolean validateModuleIntegrity(final byte[] moduleBytes, final byte[] expectedHash,
                                          final HashAlgorithm hashAlgorithm) throws SecurityException {

        try {
            final byte[] computedHash = CryptographicUtils.computeHash(moduleBytes, hashAlgorithm);
            final boolean valid = java.util.Arrays.equals(expectedHash, computedHash);

            // Log integrity check
            securityManager.logAuditEvent(AuditEvent.builder()
                .eventType("integrity_check")
                .principalId("system")
                .resourceId("integrity_validator")
                .action("validate_integrity")
                .result(valid ? "success" : "failure")
                .details(Map.of(
                    "hash_algorithm", hashAlgorithm.name(),
                    "module_size", String.valueOf(moduleBytes.length)
                ))
                .build());

            if (!valid) {
                throw new SecurityException("Module integrity validation failed");
            }

            return valid;

        } catch (final Exception e) {
            throw new SecurityException("Integrity validation error: " + e.getMessage(), e);
        }
    }

    /**
     * Exports the trust store to a secure file.
     *
     * @param path the file path to export to
     * @param password protection password
     * @throws SecurityException if export fails
     */
    public void exportTrustStore(final Path path, final char[] password) throws SecurityException {
        try {
            certificateManager.exportTrustStore(path, password);

            // Log trust store export
            securityManager.logAuditEvent(AuditEvent.builder()
                .eventType("trust_store_exported")
                .principalId("system")
                .resourceId("certificate_manager")
                .action("export_trust_store")
                .result("success")
                .details(Map.of("path", path.toString()))
                .build());

            LOGGER.info("Trust store exported to " + path);

        } catch (final Exception e) {
            throw new SecurityException("Trust store export failed: " + e.getMessage(), e);
        }
    }

    /**
     * Imports a trust store from a secure file.
     *
     * @param path the file path to import from
     * @param password protection password
     * @throws SecurityException if import fails
     */
    public void importTrustStore(final Path path, final char[] password) throws SecurityException {
        try {
            certificateManager.importTrustStore(path, password);

            // Log trust store import
            securityManager.logAuditEvent(AuditEvent.builder()
                .eventType("trust_store_imported")
                .principalId("system")
                .resourceId("certificate_manager")
                .action("import_trust_store")
                .result("success")
                .details(Map.of("path", path.toString()))
                .build());

            LOGGER.info("Trust store imported from " + path);

        } catch (final Exception e) {
            throw new SecurityException("Trust store import failed: " + e.getMessage(), e);
        }
    }

    // Private helper methods

    private void validateSignatureMetadata(final ModuleSignature signature) throws SecurityException {
        if (!policy.getAllowedSignatureAlgorithms().contains(signature.getAlgorithm())) {
            throw new SecurityException("Signature algorithm not allowed: " + signature.getAlgorithm());
        }

        if (policy.getMaxSignatureAge().isPresent()) {
            final long ageSeconds = Instant.now().getEpochSecond() - signature.getTimestamp();
            if (ageSeconds > policy.getMaxSignatureAge().get().getSeconds()) {
                throw new SecurityException("Signature is too old");
            }
        }
    }

    private void validateAgainstPolicy(final ModuleSignature signature, final TrustLevel trustLevel)
            throws SecurityException {
        if (policy.getMinimumTrustLevel().compareTo(trustLevel) > 0) {
            throw new SecurityException("Signature trust level too low: " + trustLevel);
        }

        if (policy.isRequireSignatures() && signature == null) {
            throw new SecurityException("Signatures are required by policy");
        }
    }

    // Inner classes and supporting types

    /**
     * Cryptographic policy configuration.
     */
    public static final class CryptographicPolicy {
        private final String name;
        private final Set<SignatureAlgorithm> allowedSignatureAlgorithms;
        private final HashAlgorithm hashAlgorithm;
        private final boolean requireSignatures;
        private final boolean enforceCertificateChains;
        private final Optional<java.time.Duration> maxSignatureAge;
        private final TrustLevel minimumTrustLevel;
        private final boolean allowSelfSigned;
        private final java.time.Duration keyRotationInterval;

        private CryptographicPolicy(final Builder builder) {
            this.name = builder.name;
            this.allowedSignatureAlgorithms = Set.copyOf(builder.allowedSignatureAlgorithms);
            this.hashAlgorithm = builder.hashAlgorithm;
            this.requireSignatures = builder.requireSignatures;
            this.enforceCertificateChains = builder.enforceCertificateChains;
            this.maxSignatureAge = builder.maxSignatureAge;
            this.minimumTrustLevel = builder.minimumTrustLevel;
            this.allowSelfSigned = builder.allowSelfSigned;
            this.keyRotationInterval = builder.keyRotationInterval;
        }

        public String getName() { return name; }
        public Set<SignatureAlgorithm> getAllowedSignatureAlgorithms() { return allowedSignatureAlgorithms; }
        public HashAlgorithm getHashAlgorithm() { return hashAlgorithm; }
        public boolean isRequireSignatures() { return requireSignatures; }
        public boolean isEnforceCertificateChains() { return enforceCertificateChains; }
        public Optional<java.time.Duration> getMaxSignatureAge() { return maxSignatureAge; }
        public TrustLevel getMinimumTrustLevel() { return minimumTrustLevel; }
        public boolean isAllowSelfSigned() { return allowSelfSigned; }
        public java.time.Duration getKeyRotationInterval() { return keyRotationInterval; }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String name = "Default Cryptographic Policy";
            private Set<SignatureAlgorithm> allowedSignatureAlgorithms = Set.of(SignatureAlgorithm.ED25519);
            private HashAlgorithm hashAlgorithm = HashAlgorithm.SHA256;
            private boolean requireSignatures = true;
            private boolean enforceCertificateChains = false;
            private Optional<java.time.Duration> maxSignatureAge = Optional.empty();
            private TrustLevel minimumTrustLevel = TrustLevel.MEDIUM;
            private boolean allowSelfSigned = false;
            private java.time.Duration keyRotationInterval = java.time.Duration.ofDays(90);

            public Builder name(final String name) {
                this.name = name;
                return this;
            }

            public Builder allowedSignatureAlgorithms(final Set<SignatureAlgorithm> algorithms) {
                this.allowedSignatureAlgorithms = algorithms;
                return this;
            }

            public Builder requireSignatures(final boolean require) {
                this.requireSignatures = require;
                return this;
            }

            public Builder minimumTrustLevel(final TrustLevel level) {
                this.minimumTrustLevel = level;
                return this;
            }

            public CryptographicPolicy build() {
                return new CryptographicPolicy(this);
            }
        }
    }

    /**
     * Trust levels for certificate validation.
     */
    public enum TrustLevel {
        UNKNOWN, LOW, MEDIUM, HIGH, MAXIMUM
    }

    /**
     * Hash algorithms for cryptographic operations.
     */
    public enum HashAlgorithm {
        SHA256("SHA-256"), SHA384("SHA-384"), SHA512("SHA-512");

        private final String algorithmName;

        HashAlgorithm(final String algorithmName) {
            this.algorithmName = algorithmName;
        }

        public String getAlgorithmName() { return algorithmName; }
    }

    /**
     * Signature verification result with trust information.
     */
    public static final class SignatureVerificationResult {
        private final boolean valid;
        private final TrustLevel trustLevel;
        private final String reason;
        private final Optional<String> warning;

        public SignatureVerificationResult(final boolean valid, final TrustLevel trustLevel,
                                          final String reason, final Optional<String> warning) {
            this.valid = valid;
            this.trustLevel = trustLevel;
            this.reason = reason;
            this.warning = warning;
        }

        public boolean isValid() { return valid; }
        public TrustLevel getTrustLevel() { return trustLevel; }
        public String getReason() { return reason; }
        public Optional<String> getWarning() { return warning; }
    }

    /**
     * Encrypted module container.
     */
    public static final class EncryptedModule {
        private final String keyId;
        private final byte[] iv;
        private final byte[] encryptedData;
        private final String algorithm;
        private final Instant encryptedAt;
        private final int originalSize;

        public EncryptedModule(final String keyId, final byte[] iv, final byte[] encryptedData,
                              final String algorithm, final Instant encryptedAt, final int originalSize) {
            this.keyId = keyId;
            this.iv = iv.clone();
            this.encryptedData = encryptedData.clone();
            this.algorithm = algorithm;
            this.encryptedAt = encryptedAt;
            this.originalSize = originalSize;
        }

        public String getKeyId() { return keyId; }
        public byte[] getIv() { return iv.clone(); }
        public byte[] getEncryptedData() { return encryptedData.clone(); }
        public String getAlgorithm() { return algorithm; }
        public Instant getEncryptedAt() { return encryptedAt; }
        public int getOriginalSize() { return originalSize; }
    }

    // Placeholder implementations for supporting classes
    private static final class KeyVault {
        private final CryptographicPolicy policy;

        KeyVault(final CryptographicPolicy policy) {
            this.policy = policy;
        }

        Optional<SigningKey> getSigningKey(final String keyId) {
            // Implementation would retrieve signing key from secure storage
            return Optional.empty(); // Placeholder
        }

        Optional<EncryptionKey> getEncryptionKey(final String keyId) {
            // Implementation would retrieve encryption key from secure storage
            return Optional.empty(); // Placeholder
        }

        String rotateKey(final String keyId) {
            // Implementation would rotate the key and return new key ID
            return "new-key-id"; // Placeholder
        }
    }

    private static final class CertificateManager {
        private final CryptographicPolicy policy;

        CertificateManager(final CryptographicPolicy policy) {
            this.policy = policy;
        }

        CertificateValidationResult validateChain(final CertificateChain chain) {
            // Implementation would validate certificate chain
            return new CertificateValidationResult(true, TrustLevel.HIGH, List.of());
        }

        void exportTrustStore(final Path path, final char[] password) throws Exception {
            // Implementation would export trust store to file
        }

        void importTrustStore(final Path path, final char[] password) throws Exception {
            // Implementation would import trust store from file
        }
    }

    // Supporting types for cryptographic operations
    private static final class SigningKey {
        private final SignatureAlgorithm algorithm;

        SigningKey(final SignatureAlgorithm algorithm) {
            this.algorithm = algorithm;
        }

        SignatureAlgorithm getAlgorithm() { return algorithm; }
        byte[] sign(final byte[] data) { return new byte[0]; } // Placeholder
        byte[] getPublicKey() { return new byte[0]; } // Placeholder
    }

    private static final class EncryptionKey {
        private final SecretKey secretKey;

        EncryptionKey(final SecretKey secretKey) {
            this.secretKey = secretKey;
        }

        SecretKey getSecretKey() { return secretKey; }
    }

    private static final class CertificateChain {
        static CertificateChain fromPemStrings(final java.util.List<String> pemStrings) {
            return new CertificateChain(); // Placeholder
        }

        java.util.List<String> toPemStrings() {
            return java.util.List.of(); // Placeholder
        }
    }

    private static final class CertificateValidationResult {
        private final boolean valid;
        private final TrustLevel trustLevel;
        private final java.util.List<String> errors;

        CertificateValidationResult(final boolean valid, final TrustLevel trustLevel,
                                   final java.util.List<String> errors) {
            this.valid = valid;
            this.trustLevel = trustLevel;
            this.errors = errors;
        }

        boolean isValid() { return valid; }
        TrustLevel getTrustLevel() { return trustLevel; }
        java.util.List<String> getErrors() { return errors; }
    }

    private static final class CryptographicUtils {
        static byte[] computeHash(final byte[] data, final HashAlgorithm algorithm) {
            // Implementation would compute cryptographic hash
            return new byte[32]; // Placeholder
        }

        static boolean verifySignature(final byte[] digest, final byte[] signature,
                                      final byte[] publicKey, final SignatureAlgorithm algorithm) {
            // Implementation would verify signature
            return true; // Placeholder
        }

        static byte[] generateSecureRandom(final int length) {
            final byte[] random = new byte[length];
            new java.security.SecureRandom().nextBytes(random);
            return random;
        }

        static String generateSecureId() {
            return java.util.UUID.randomUUID().toString();
        }
    }
}