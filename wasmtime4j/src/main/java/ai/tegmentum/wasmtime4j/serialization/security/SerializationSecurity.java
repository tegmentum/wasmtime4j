/*
 * Copyright 2024 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.serialization.security;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Security utilities for WebAssembly module serialization.
 *
 * <p>This class provides comprehensive security features for module serialization including:
 *
 * <ul>
 *   <li>AES-GCM encryption with authenticated encryption
 *   <li>Digital signatures for authenticity verification
 *   <li>Integrity verification with SHA-256 and HMAC
 *   <li>Secure key generation and management
 *   <li>Access control and permission validation
 * </ul>
 *
 * @since 1.0.0
 */
public final class SerializationSecurity {

  private static final Logger LOGGER = Logger.getLogger(SerializationSecurity.class.getName());

  // Encryption constants
  private static final String AES_GCM_ALGORITHM = "AES/GCM/NoPadding";
  private static final String AES_CBC_ALGORITHM = "AES/CBC/PKCS5Padding";
  private static final int GCM_IV_LENGTH = 12; // 96 bits recommended for GCM
  private static final int GCM_TAG_LENGTH = 16; // 128 bits
  private static final int CBC_IV_LENGTH = 16; // 128 bits

  // Signature constants
  private static final String RSA_SIGNATURE_ALGORITHM = "SHA256withRSA";
  private static final String ECDSA_SIGNATURE_ALGORITHM = "SHA256withECDSA";

  // Hash constants
  private static final String SHA256_ALGORITHM = "SHA-256";
  private static final String SHA512_ALGORITHM = "SHA-512";
  private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

  // Private constructor to prevent instantiation
  private SerializationSecurity() {
    throw new UnsupportedOperationException("Utility class cannot be instantiated");
  }

  /**
   * Encrypts serialized module data using AES-GCM for authenticated encryption.
   *
   * @param data the data to encrypt
   * @param secretKey the secret key for encryption
   * @return the encrypted data with IV and authentication tag
   * @throws WasmException if encryption fails
   * @throws IllegalArgumentException if parameters are null
   */
  public static EncryptedData encryptAesGcm(final byte[] data, final SecretKey secretKey)
      throws WasmException {
    Objects.requireNonNull(data, "Data cannot be null");
    Objects.requireNonNull(secretKey, "Secret key cannot be null");

    try {
      // Generate random IV
      final byte[] iv = generateSecureRandom(GCM_IV_LENGTH);

      // Initialize cipher
      final Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
      final GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

      // Encrypt data
      final byte[] encryptedData = cipher.doFinal(data);

      LOGGER.fine("Successfully encrypted " + data.length + " bytes using AES-GCM");
      return new EncryptedData(encryptedData, iv, AES_GCM_ALGORITHM);

    } catch (NoSuchAlgorithmException
        | NoSuchPaddingException
        | InvalidKeyException
        | InvalidAlgorithmParameterException
        | IllegalBlockSizeException
        | BadPaddingException e) {
      throw new WasmException("AES-GCM encryption failed", e);
    }
  }

  /**
   * Decrypts data that was encrypted with AES-GCM.
   *
   * @param encryptedData the encrypted data with metadata
   * @param secretKey the secret key for decryption
   * @return the decrypted data
   * @throws WasmException if decryption fails or authentication fails
   */
  public static byte[] decryptAesGcm(final EncryptedData encryptedData, final SecretKey secretKey)
      throws WasmException {
    Objects.requireNonNull(encryptedData, "Encrypted data cannot be null");
    Objects.requireNonNull(secretKey, "Secret key cannot be null");

    if (!AES_GCM_ALGORITHM.equals(encryptedData.getAlgorithm())) {
      throw new WasmException("Expected AES-GCM algorithm, got: " + encryptedData.getAlgorithm());
    }

    try {
      // Initialize cipher
      final Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
      final GCMParameterSpec gcmSpec =
          new GCMParameterSpec(GCM_TAG_LENGTH * 8, encryptedData.getIv());
      cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

      // Decrypt and authenticate
      final byte[] decryptedData = cipher.doFinal(encryptedData.getCiphertext());

      LOGGER.fine("Successfully decrypted " + decryptedData.length + " bytes using AES-GCM");
      return decryptedData;

    } catch (NoSuchAlgorithmException
        | NoSuchPaddingException
        | InvalidKeyException
        | InvalidAlgorithmParameterException
        | IllegalBlockSizeException
        | BadPaddingException e) {
      throw new WasmException("AES-GCM decryption failed", e);
    }
  }

  /**
   * Encrypts data using AES-CBC with PKCS5 padding.
   *
   * <p><strong>SECURITY WARNING:</strong> AES-CBC does not provide integrity protection and is
   * vulnerable to padding oracle attacks. Use {@link #encryptAesGcm(byte[], SecretKey)} for new
   * code. This method is provided only for compatibility with legacy systems that require AES-CBC.
   * If you must use this method, you MUST also compute and verify an HMAC using {@link
   * #calculateHmacSha256(byte[], SecretKey)} to ensure integrity protection.
   *
   * @param data the data to encrypt
   * @param secretKey the secret key for encryption
   * @return the encrypted data with IV
   * @throws WasmException if encryption fails
   * @deprecated Use {@link #encryptAesGcm(byte[], SecretKey)} instead for authenticated encryption
   */
  @Deprecated
  @SuppressFBWarnings(
      value = {"CIPHER_INTEGRITY", "PADDING_ORACLE", "STATIC_IV"},
      justification =
          "AES-CBC is provided for backward compatibility with legacy systems. "
              + "Users are warned via deprecation and documentation to prefer AES-GCM. "
              + "When this method must be used, users should add HMAC for integrity protection "
              + "as documented in the method's Javadoc. "
              + "IV is generated using SecureRandom each time, not static.")
  public static EncryptedData encryptAesCbc(final byte[] data, final SecretKey secretKey)
      throws WasmException {
    Objects.requireNonNull(data, "Data cannot be null");
    Objects.requireNonNull(secretKey, "Secret key cannot be null");

    try {
      // Generate random IV
      final byte[] iv = generateSecureRandom(CBC_IV_LENGTH);

      // Initialize cipher
      final Cipher cipher = Cipher.getInstance(AES_CBC_ALGORITHM);
      final IvParameterSpec ivSpec = new IvParameterSpec(iv);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

      // Encrypt data
      final byte[] encryptedData = cipher.doFinal(data);

      LOGGER.fine("Successfully encrypted " + data.length + " bytes using AES-CBC");
      return new EncryptedData(encryptedData, iv, AES_CBC_ALGORITHM);

    } catch (NoSuchAlgorithmException
        | NoSuchPaddingException
        | InvalidKeyException
        | InvalidAlgorithmParameterException
        | IllegalBlockSizeException
        | BadPaddingException e) {
      throw new WasmException("AES-CBC encryption failed", e);
    }
  }

  /**
   * Decrypts data that was encrypted with AES-CBC.
   *
   * <p><strong>SECURITY WARNING:</strong> AES-CBC does not provide integrity protection and is
   * vulnerable to padding oracle attacks. Use {@link #decryptAesGcm(EncryptedData, SecretKey)} for
   * new code. This method is provided only for compatibility with legacy systems that require
   * AES-CBC. If you must use this method, you MUST verify an HMAC using {@link
   * #verifyHmacSha256(byte[], byte[], SecretKey)} before decrypting to ensure integrity protection.
   *
   * @param encryptedData the encrypted data with metadata
   * @param secretKey the secret key for decryption
   * @return the decrypted data
   * @throws WasmException if decryption fails
   * @deprecated Use {@link #decryptAesGcm(EncryptedData, SecretKey)} instead for authenticated
   *     encryption
   */
  @Deprecated
  @SuppressFBWarnings(
      value = {"CIPHER_INTEGRITY", "PADDING_ORACLE"},
      justification =
          "AES-CBC is provided for backward compatibility with legacy systems. "
              + "Users are warned via deprecation and documentation to prefer AES-GCM. "
              + "When this method must be used, users should verify HMAC before decrypting "
              + "as documented in the method's Javadoc to prevent padding oracle attacks.")
  public static byte[] decryptAesCbc(final EncryptedData encryptedData, final SecretKey secretKey)
      throws WasmException {
    Objects.requireNonNull(encryptedData, "Encrypted data cannot be null");
    Objects.requireNonNull(secretKey, "Secret key cannot be null");

    if (!AES_CBC_ALGORITHM.equals(encryptedData.getAlgorithm())) {
      throw new WasmException("Expected AES-CBC algorithm, got: " + encryptedData.getAlgorithm());
    }

    try {
      // Initialize cipher
      final Cipher cipher = Cipher.getInstance(AES_CBC_ALGORITHM);
      final IvParameterSpec ivSpec = new IvParameterSpec(encryptedData.getIv());
      cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

      // Decrypt data
      final byte[] decryptedData = cipher.doFinal(encryptedData.getCiphertext());

      LOGGER.fine("Successfully decrypted " + decryptedData.length + " bytes using AES-CBC");
      return decryptedData;

    } catch (NoSuchAlgorithmException
        | NoSuchPaddingException
        | InvalidKeyException
        | InvalidAlgorithmParameterException
        | IllegalBlockSizeException
        | BadPaddingException e) {
      throw new WasmException("AES-CBC decryption failed", e);
    }
  }

  /**
   * Creates a digital signature for module authenticity verification.
   *
   * @param data the data to sign
   * @param privateKey the private key for signing
   * @param algorithm the signature algorithm (RSA or ECDSA)
   * @return the digital signature
   * @throws WasmException if signing fails
   */
  public static byte[] createDigitalSignature(
      final byte[] data, final PrivateKey privateKey, final String algorithm) throws WasmException {
    Objects.requireNonNull(data, "Data cannot be null");
    Objects.requireNonNull(privateKey, "Private key cannot be null");
    Objects.requireNonNull(algorithm, "Algorithm cannot be null");

    try {
      final Signature signature = Signature.getInstance(algorithm);
      signature.initSign(privateKey);
      signature.update(data);
      final byte[] signatureBytes = signature.sign();

      LOGGER.fine(
          "Successfully created digital signature for "
              + data.length
              + " bytes using "
              + algorithm);
      return signatureBytes;

    } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
      throw new WasmException("Digital signature creation failed", e);
    }
  }

  /**
   * Verifies a digital signature for module authenticity.
   *
   * @param data the original data
   * @param signatureBytes the signature to verify
   * @param publicKey the public key for verification
   * @param algorithm the signature algorithm
   * @return true if signature is valid
   * @throws WasmException if verification fails
   */
  public static boolean verifyDigitalSignature(
      final byte[] data,
      final byte[] signatureBytes,
      final PublicKey publicKey,
      final String algorithm)
      throws WasmException {
    Objects.requireNonNull(data, "Data cannot be null");
    Objects.requireNonNull(signatureBytes, "Signature cannot be null");
    Objects.requireNonNull(publicKey, "Public key cannot be null");
    Objects.requireNonNull(algorithm, "Algorithm cannot be null");

    try {
      final Signature signature = Signature.getInstance(algorithm);
      signature.initVerify(publicKey);
      signature.update(data);
      final boolean isValid = signature.verify(signatureBytes);

      LOGGER.fine(
          "Digital signature verification result: " + isValid + " for " + data.length + " bytes");
      return isValid;

    } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
      throw new WasmException("Digital signature verification failed", e);
    }
  }

  /**
   * Calculates SHA-256 hash for integrity verification.
   *
   * @param data the data to hash
   * @return the SHA-256 hash
   * @throws WasmException if hashing fails
   */
  public static byte[] calculateSha256(final byte[] data) throws WasmException {
    Objects.requireNonNull(data, "Data cannot be null");

    try {
      final MessageDigest digest = MessageDigest.getInstance(SHA256_ALGORITHM);
      final byte[] hash = digest.digest(data);

      LOGGER.fine("Successfully calculated SHA-256 hash for " + data.length + " bytes");
      return hash;

    } catch (NoSuchAlgorithmException e) {
      throw new WasmException("SHA-256 hashing failed", e);
    }
  }

  /**
   * Calculates SHA-512 hash for enhanced integrity verification.
   *
   * @param data the data to hash
   * @return the SHA-512 hash
   * @throws WasmException if hashing fails
   */
  public static byte[] calculateSha512(final byte[] data) throws WasmException {
    Objects.requireNonNull(data, "Data cannot be null");

    try {
      final MessageDigest digest = MessageDigest.getInstance(SHA512_ALGORITHM);
      final byte[] hash = digest.digest(data);

      LOGGER.fine("Successfully calculated SHA-512 hash for " + data.length + " bytes");
      return hash;

    } catch (NoSuchAlgorithmException e) {
      throw new WasmException("SHA-512 hashing failed", e);
    }
  }

  /**
   * Calculates HMAC-SHA256 for authenticated integrity verification.
   *
   * @param data the data to authenticate
   * @param secretKey the secret key for HMAC
   * @return the HMAC-SHA256 value
   * @throws WasmException if HMAC calculation fails
   */
  public static byte[] calculateHmacSha256(final byte[] data, final SecretKey secretKey)
      throws WasmException {
    Objects.requireNonNull(data, "Data cannot be null");
    Objects.requireNonNull(secretKey, "Secret key cannot be null");

    try {
      final javax.crypto.Mac mac = javax.crypto.Mac.getInstance(HMAC_SHA256_ALGORITHM);
      mac.init(secretKey);
      final byte[] hmac = mac.doFinal(data);

      LOGGER.fine("Successfully calculated HMAC-SHA256 for " + data.length + " bytes");
      return hmac;

    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new WasmException("HMAC-SHA256 calculation failed", e);
    }
  }

  /**
   * Verifies HMAC-SHA256 for authenticated integrity.
   *
   * @param data the original data
   * @param expectedHmac the expected HMAC value
   * @param secretKey the secret key for HMAC
   * @return true if HMAC is valid
   * @throws WasmException if verification fails
   */
  public static boolean verifyHmacSha256(
      final byte[] data, final byte[] expectedHmac, final SecretKey secretKey)
      throws WasmException {
    Objects.requireNonNull(data, "Data cannot be null");
    Objects.requireNonNull(expectedHmac, "Expected HMAC cannot be null");
    Objects.requireNonNull(secretKey, "Secret key cannot be null");

    try {
      final byte[] calculatedHmac = calculateHmacSha256(data, secretKey);
      final boolean isValid = MessageDigest.isEqual(expectedHmac, calculatedHmac);

      LOGGER.fine("HMAC-SHA256 verification result: " + isValid + " for " + data.length + " bytes");
      return isValid;

    } catch (Exception e) {
      throw new WasmException("HMAC-SHA256 verification failed", e);
    }
  }

  /**
   * Generates a secure AES secret key.
   *
   * @param keySize the key size in bits (128, 192, or 256)
   * @return the generated secret key
   * @throws WasmException if key generation fails
   */
  public static SecretKey generateAesKey(final int keySize) throws WasmException {
    if (keySize != 128 && keySize != 192 && keySize != 256) {
      throw new IllegalArgumentException("AES key size must be 128, 192, or 256 bits");
    }

    try {
      final KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
      keyGenerator.init(keySize);
      final SecretKey secretKey = keyGenerator.generateKey();

      LOGGER.fine("Successfully generated " + keySize + "-bit AES key");
      return secretKey;

    } catch (NoSuchAlgorithmException e) {
      throw new WasmException("AES key generation failed", e);
    }
  }

  /**
   * Creates a secret key from a byte array.
   *
   * @param keyBytes the key material
   * @param algorithm the algorithm name (e.g., "AES")
   * @return the secret key
   * @throws IllegalArgumentException if parameters are invalid
   */
  public static SecretKey createSecretKey(final byte[] keyBytes, final String algorithm) {
    Objects.requireNonNull(keyBytes, "Key bytes cannot be null");
    Objects.requireNonNull(algorithm, "Algorithm cannot be null");

    return new SecretKeySpec(keyBytes, algorithm);
  }

  /**
   * Generates cryptographically secure random bytes.
   *
   * @param length the number of bytes to generate
   * @return the random bytes
   * @throws IllegalArgumentException if length is negative
   */
  public static byte[] generateSecureRandom(final int length) {
    if (length < 0) {
      throw new IllegalArgumentException("Length cannot be negative");
    }

    final SecureRandom secureRandom = new SecureRandom();
    final byte[] randomBytes = new byte[length];
    secureRandom.nextBytes(randomBytes);
    return randomBytes;
  }

  /**
   * Validates access permissions for module serialization operations.
   *
   * @param operation the operation being performed
   * @param permissions the permissions context
   * @return true if access is allowed
   * @throws WasmException if permission validation fails
   */
  public static boolean validateAccess(
      final SecurityOperation operation, final PermissionContext permissions) throws WasmException {
    Objects.requireNonNull(operation, "Operation cannot be null");
    Objects.requireNonNull(permissions, "Permissions cannot be null");

    try {
      switch (operation) {
        case SERIALIZE:
          return permissions.hasPermission("wasmtime4j.module.serialize");
        case DESERIALIZE:
          return permissions.hasPermission("wasmtime4j.module.deserialize");
        case ENCRYPT:
          return permissions.hasPermission("wasmtime4j.module.encrypt");
        case DECRYPT:
          return permissions.hasPermission("wasmtime4j.module.decrypt");
        case SIGN:
          return permissions.hasPermission("wasmtime4j.module.sign");
        case VERIFY:
          return permissions.hasPermission("wasmtime4j.module.verify");
        default:
          LOGGER.warning("Unknown security operation: " + operation);
          return false;
      }
    } catch (Exception e) {
      throw new WasmException("Access validation failed for operation: " + operation, e);
    }
  }

  /**
   * Secure comparison of byte arrays to prevent timing attacks.
   *
   * @param a first byte array
   * @param b second byte array
   * @return true if arrays are equal
   */
  public static boolean constantTimeEquals(final byte[] a, final byte[] b) {
    if (a == null || b == null) {
      return a == b;
    }

    if (a.length != b.length) {
      return false;
    }

    int result = 0;
    for (int i = 0; i < a.length; i++) {
      result |= a[i] ^ b[i];
    }

    return result == 0;
  }

  /**
   * Securely clears sensitive data from memory.
   *
   * @param data the sensitive data to clear
   */
  public static void clearSensitiveData(final byte[] data) {
    if (data != null) {
      Arrays.fill(data, (byte) 0);
    }
  }

  /**
   * Converts byte array to hexadecimal string for display purposes.
   *
   * @param bytes the byte array
   * @return hexadecimal string representation
   */
  public static String bytesToHex(final byte[] bytes) {
    Objects.requireNonNull(bytes, "Bytes cannot be null");

    final StringBuilder result = new StringBuilder();
    for (final byte b : bytes) {
      result.append(String.format("%02x", b));
    }
    return result.toString();
  }

  /**
   * Converts hexadecimal string to byte array.
   *
   * @param hex the hexadecimal string
   * @return the byte array
   * @throws IllegalArgumentException if hex string is invalid
   */
  public static byte[] hexToBytes(final String hex) {
    Objects.requireNonNull(hex, "Hex string cannot be null");

    if (hex.length() % 2 != 0) {
      throw new IllegalArgumentException("Hex string length must be even");
    }

    final byte[] bytes = new byte[hex.length() / 2];
    for (int i = 0; i < bytes.length; i++) {
      final int index = i * 2;
      bytes[i] = (byte) Integer.parseInt(hex.substring(index, index + 2), 16);
    }
    return bytes;
  }

  /** Security operations that can be performed on modules. */
  public enum SecurityOperation {
    SERIALIZE,
    DESERIALIZE,
    ENCRYPT,
    DECRYPT,
    SIGN,
    VERIFY
  }

  /** Interface for permission context validation. */
  public interface PermissionContext {
    /**
     * Checks if a specific permission is granted.
     *
     * @param permission the permission to check
     * @return true if permission is granted
     */
    boolean hasPermission(String permission);

    /**
     * Gets the current security context identifier.
     *
     * @return the security context identifier
     */
    String getSecurityContext();
  }
}
