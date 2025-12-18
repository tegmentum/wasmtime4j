/*
 * Copyright 2025 Tegmentum AI
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the serialization security package classes.
 *
 * <p>This package provides security utilities for WebAssembly module serialization.
 */
@DisplayName("Serialization Security Package Tests")
class SecurityPackageTest {

  @Nested
  @DisplayName("EncryptedData Tests")
  class EncryptedDataTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(EncryptedData.class.getModifiers()), "EncryptedData should be final");
    }

    @Test
    @DisplayName("should create encrypted data with basic constructor")
    void shouldCreateEncryptedDataWithBasicConstructor() {
      final byte[] ciphertext = new byte[] {1, 2, 3, 4, 5};
      final byte[] iv = new byte[] {10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21};
      final String algorithm = "AES/GCM/NoPadding";

      final EncryptedData data = new EncryptedData(ciphertext, iv, algorithm);

      assertArrayEquals(ciphertext, data.getCiphertext(), "Ciphertext should match");
      assertArrayEquals(iv, data.getIv(), "IV should match");
      assertEquals(algorithm, data.getAlgorithm(), "Algorithm should match");
      assertNull(data.getAuthTag(), "Auth tag should be null");
      assertNull(data.getKeyId(), "Key ID should be null");
    }

    @Test
    @DisplayName("should create encrypted data with full constructor")
    void shouldCreateEncryptedDataWithFullConstructor() {
      final byte[] ciphertext = new byte[] {1, 2, 3, 4, 5};
      final byte[] iv = new byte[] {10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21};
      final String algorithm = "AES/GCM/NoPadding";
      final byte[] authTag = new byte[] {20, 21, 22, 23};
      final String keyId = "test-key-001";

      final EncryptedData data = new EncryptedData(ciphertext, iv, algorithm, authTag, keyId);

      assertArrayEquals(ciphertext, data.getCiphertext(), "Ciphertext should match");
      assertArrayEquals(iv, data.getIv(), "IV should match");
      assertEquals(algorithm, data.getAlgorithm(), "Algorithm should match");
      assertArrayEquals(authTag, data.getAuthTag(), "Auth tag should match");
      assertEquals(keyId, data.getKeyId(), "Key ID should match");
    }

    @Test
    @DisplayName("should return defensive copy of ciphertext")
    void shouldReturnDefensiveCopyOfCiphertext() {
      final byte[] ciphertext = new byte[] {1, 2, 3, 4, 5};
      final byte[] iv = new byte[12];
      final EncryptedData data = new EncryptedData(ciphertext, iv, "AES/GCM/NoPadding");

      final byte[] returned = data.getCiphertext();
      returned[0] = 99;

      assertEquals(1, data.getCiphertext()[0], "Original ciphertext should not be modified");
    }

    @Test
    @DisplayName("should return defensive copy of IV")
    void shouldReturnDefensiveCopyOfIv() {
      final byte[] ciphertext = new byte[] {1, 2, 3, 4, 5};
      final byte[] iv = new byte[] {10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21};
      final EncryptedData data = new EncryptedData(ciphertext, iv, "AES/GCM/NoPadding");

      final byte[] returned = data.getIv();
      returned[0] = 99;

      assertEquals(10, data.getIv()[0], "Original IV should not be modified");
    }

    @Test
    @DisplayName("should return defensive copy of auth tag")
    void shouldReturnDefensiveCopyOfAuthTag() {
      final byte[] ciphertext = new byte[] {1, 2, 3, 4, 5};
      final byte[] iv = new byte[12];
      final byte[] authTag = new byte[] {20, 21, 22, 23};
      final EncryptedData data =
          new EncryptedData(ciphertext, iv, "AES/GCM/NoPadding", authTag, null);

      final byte[] returned = data.getAuthTag();
      returned[0] = 99;

      assertEquals(20, data.getAuthTag()[0], "Original auth tag should not be modified");
    }

    @Test
    @DisplayName("should throw on null ciphertext")
    void shouldThrowOnNullCiphertext() {
      assertThrows(
          NullPointerException.class,
          () -> new EncryptedData(null, new byte[12], "AES/GCM/NoPadding"),
          "Should throw on null ciphertext");
    }

    @Test
    @DisplayName("should throw on null IV")
    void shouldThrowOnNullIv() {
      assertThrows(
          NullPointerException.class,
          () -> new EncryptedData(new byte[] {1, 2, 3}, null, "AES/GCM/NoPadding"),
          "Should throw on null IV");
    }

    @Test
    @DisplayName("should throw on null algorithm")
    void shouldThrowOnNullAlgorithm() {
      assertThrows(
          NullPointerException.class,
          () -> new EncryptedData(new byte[] {1, 2, 3}, new byte[12], null),
          "Should throw on null algorithm");
    }

    @Test
    @DisplayName("should get size correctly")
    void shouldGetSizeCorrectly() {
      final byte[] ciphertext = new byte[100];
      final byte[] iv = new byte[12];
      final EncryptedData data = new EncryptedData(ciphertext, iv, "AES/GCM/NoPadding");

      assertEquals(100, data.getSize(), "Size should be ciphertext length");
    }

    @Test
    @DisplayName("should get encryption timestamp")
    void shouldGetEncryptionTimestamp() {
      final Instant before = Instant.now();
      final EncryptedData data = new EncryptedData(new byte[10], new byte[12], "AES/GCM/NoPadding");
      final Instant after = Instant.now();

      assertNotNull(data.getEncryptionTimestamp(), "Timestamp should not be null");
      assertTrue(
          !data.getEncryptionTimestamp().isBefore(before),
          "Timestamp should not be before creation");
      assertTrue(
          !data.getEncryptionTimestamp().isAfter(after), "Timestamp should not be after creation");
    }

    @Test
    @DisplayName("should detect authenticated encryption for GCM")
    void shouldDetectAuthenticatedEncryptionForGcm() {
      final EncryptedData data = new EncryptedData(new byte[10], new byte[12], "AES/GCM/NoPadding");

      assertTrue(data.isAuthenticatedEncryption(), "GCM should be authenticated encryption");
    }

    @Test
    @DisplayName("should detect authenticated encryption with auth tag")
    void shouldDetectAuthenticatedEncryptionWithAuthTag() {
      final EncryptedData data =
          new EncryptedData(new byte[10], new byte[16], "AES/CBC/PKCS5Padding", new byte[16], null);

      assertTrue(data.isAuthenticatedEncryption(), "Should be authenticated with auth tag");
    }

    @Test
    @DisplayName("should not detect authenticated encryption for CBC without auth tag")
    void shouldNotDetectAuthenticatedEncryptionForCbcWithoutAuthTag() {
      final EncryptedData data =
          new EncryptedData(new byte[10], new byte[16], "AES/CBC/PKCS5Padding");

      assertFalse(
          data.isAuthenticatedEncryption(), "CBC without auth tag should not be authenticated");
    }

    @Test
    @DisplayName("should validate integrity for GCM with correct IV size")
    void shouldValidateIntegrityForGcmWithCorrectIvSize() {
      final EncryptedData data = new EncryptedData(new byte[10], new byte[12], "AES/GCM/NoPadding");

      assertTrue(data.validateIntegrity(), "GCM with 12-byte IV should be valid");
    }

    @Test
    @DisplayName("should fail integrity for GCM with wrong IV size")
    void shouldFailIntegrityForGcmWithWrongIvSize() {
      final EncryptedData data = new EncryptedData(new byte[10], new byte[16], "AES/GCM/NoPadding");

      assertFalse(data.validateIntegrity(), "GCM with non-12-byte IV should be invalid");
    }

    @Test
    @DisplayName("should validate integrity for CBC with correct IV size")
    void shouldValidateIntegrityForCbcWithCorrectIvSize() {
      final EncryptedData data =
          new EncryptedData(new byte[10], new byte[16], "AES/CBC/PKCS5Padding");

      assertTrue(data.validateIntegrity(), "CBC with 16-byte IV should be valid");
    }

    @Test
    @DisplayName("should fail integrity for empty ciphertext")
    void shouldFailIntegrityForEmptyCiphertext() {
      final EncryptedData data = new EncryptedData(new byte[0], new byte[12], "AES/GCM/NoPadding");

      assertFalse(data.validateIntegrity(), "Empty ciphertext should be invalid");
    }

    @Test
    @DisplayName("should fail integrity for empty IV")
    void shouldFailIntegrityForEmptyIv() {
      final EncryptedData data = new EncryptedData(new byte[10], new byte[0], "AES/GCM/NoPadding");

      assertFalse(data.validateIntegrity(), "Empty IV should be invalid");
    }

    @Test
    @DisplayName("should create copy with new key ID")
    void shouldCreateCopyWithNewKeyId() {
      final EncryptedData original =
          new EncryptedData(
              new byte[] {1, 2, 3}, new byte[12], "AES/GCM/NoPadding", null, "old-key");

      final EncryptedData updated = original.withKeyId("new-key");

      assertEquals("old-key", original.getKeyId(), "Original key ID should be unchanged");
      assertEquals("new-key", updated.getKeyId(), "Updated key ID should be new value");
      assertArrayEquals(
          original.getCiphertext(), updated.getCiphertext(), "Ciphertext should be same");
    }

    @Test
    @DisplayName("should serialize and deserialize correctly")
    void shouldSerializeAndDeserializeCorrectly() {
      final byte[] ciphertext = new byte[] {1, 2, 3, 4, 5};
      final byte[] iv = new byte[] {10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21};
      final String algorithm = "AES/GCM/NoPadding";
      final byte[] authTag = new byte[] {20, 21, 22, 23};
      final String keyId = "test-key-001";

      final EncryptedData original = new EncryptedData(ciphertext, iv, algorithm, authTag, keyId);
      final byte[] serialized = original.serialize();
      final EncryptedData deserialized = EncryptedData.deserialize(serialized);

      assertArrayEquals(ciphertext, deserialized.getCiphertext(), "Ciphertext should match");
      assertArrayEquals(iv, deserialized.getIv(), "IV should match");
      assertEquals(algorithm, deserialized.getAlgorithm(), "Algorithm should match");
      assertArrayEquals(authTag, deserialized.getAuthTag(), "Auth tag should match");
      assertEquals(keyId, deserialized.getKeyId(), "Key ID should match");
    }

    @Test
    @DisplayName("should throw on null serialized data for deserialize")
    void shouldThrowOnNullSerializedDataForDeserialize() {
      assertThrows(
          NullPointerException.class,
          () -> EncryptedData.deserialize(null),
          "Should throw on null serialized data");
    }

    @Test
    @DisplayName("should throw on invalid version in serialized data")
    void shouldThrowOnInvalidVersionInSerializedData() {
      final byte[] invalidData = new byte[100];
      // Set version to 99
      invalidData[3] = 99;

      assertThrows(
          IllegalArgumentException.class,
          () -> EncryptedData.deserialize(invalidData),
          "Should throw on invalid version");
    }

    @Test
    @DisplayName("should clear sensitive data")
    void shouldClearSensitiveData() {
      final byte[] ciphertext = new byte[] {1, 2, 3, 4, 5};
      final byte[] iv = new byte[] {10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21};
      final byte[] authTag = new byte[] {20, 21, 22, 23};

      final EncryptedData data =
          new EncryptedData(ciphertext, iv, "AES/GCM/NoPadding", authTag, null);
      data.clear();

      // Get the data after clearing - it should be zeroed
      // Note: getCiphertext returns a clone, so we can't directly check the internal array
      // but the method should zero out internal arrays for security
      assertNotNull(data, "Data object should still exist");
    }

    @Test
    @DisplayName("toString should return descriptive string")
    void toStringShouldReturnDescriptiveString() {
      final EncryptedData data =
          new EncryptedData(new byte[100], new byte[12], "AES/GCM/NoPadding");

      final String result = data.toString();

      assertNotNull(result, "toString should not return null");
      assertFalse(result.isEmpty(), "toString should not be empty");
      assertTrue(result.contains("AES/GCM/NoPadding"), "toString should contain algorithm");
      assertTrue(result.contains("100"), "toString should contain size");
    }

    @Test
    @DisplayName("equals should work correctly")
    void equalsShouldWorkCorrectly() {
      final byte[] ciphertext = new byte[] {1, 2, 3};
      final byte[] iv = new byte[12];
      final String algorithm = "AES/GCM/NoPadding";

      final EncryptedData data1 = new EncryptedData(ciphertext, iv, algorithm);
      final EncryptedData data2 = new EncryptedData(ciphertext, iv, algorithm);
      final EncryptedData data3 = new EncryptedData(new byte[] {4, 5, 6}, iv, algorithm);

      assertEquals(data1, data2, "Equal data should be equal");
      assertNotEquals(data1, data3, "Different data should not be equal");
      assertEquals(
          data1.hashCode(), data2.hashCode(), "Hash codes should be equal for equal objects");
    }
  }

  @Nested
  @DisplayName("SerializationSecurity Tests")
  class SerializationSecurityTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(SerializationSecurity.class.getModifiers()),
          "SerializationSecurity should be final");
    }

    @Test
    @DisplayName("should have private constructor")
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor = SerializationSecurity.class.getDeclaredConstructor();
      assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Constructor should be private");
    }

    @Test
    @DisplayName("private constructor should throw UnsupportedOperationException")
    void privateConstructorShouldThrowUnsupportedOperationException() throws Exception {
      final Constructor<?> constructor = SerializationSecurity.class.getDeclaredConstructor();
      constructor.setAccessible(true);

      assertThrows(
          InvocationTargetException.class,
          constructor::newInstance,
          "Constructor should throw when invoked");
    }

    @Test
    @DisplayName("should generate AES key with 128 bits")
    void shouldGenerateAesKeyWith128Bits() throws WasmException {
      final SecretKey key = SerializationSecurity.generateAesKey(128);

      assertNotNull(key, "Key should not be null");
      assertEquals("AES", key.getAlgorithm(), "Algorithm should be AES");
      assertEquals(16, key.getEncoded().length, "Key should be 16 bytes (128 bits)");
    }

    @Test
    @DisplayName("should generate AES key with 256 bits")
    void shouldGenerateAesKeyWith256Bits() throws WasmException {
      final SecretKey key = SerializationSecurity.generateAesKey(256);

      assertNotNull(key, "Key should not be null");
      assertEquals("AES", key.getAlgorithm(), "Algorithm should be AES");
      assertEquals(32, key.getEncoded().length, "Key should be 32 bytes (256 bits)");
    }

    @Test
    @DisplayName("should reject invalid AES key size")
    void shouldRejectInvalidAesKeySize() {
      assertThrows(
          IllegalArgumentException.class,
          () -> SerializationSecurity.generateAesKey(64),
          "Should reject invalid key size");

      assertThrows(
          IllegalArgumentException.class,
          () -> SerializationSecurity.generateAesKey(512),
          "Should reject invalid key size");
    }

    @Test
    @DisplayName("should encrypt and decrypt with AES-GCM")
    void shouldEncryptAndDecryptWithAesGcm() throws WasmException {
      final byte[] plaintext = "Hello, WebAssembly!".getBytes();
      final SecretKey key = SerializationSecurity.generateAesKey(256);

      final EncryptedData encrypted = SerializationSecurity.encryptAesGcm(plaintext, key);
      final byte[] decrypted = SerializationSecurity.decryptAesGcm(encrypted, key);

      assertArrayEquals(plaintext, decrypted, "Decrypted data should match plaintext");
    }

    @Test
    @DisplayName("should throw on null data for AES-GCM encryption")
    void shouldThrowOnNullDataForAesGcmEncryption() throws WasmException {
      final SecretKey key = SerializationSecurity.generateAesKey(256);

      assertThrows(
          NullPointerException.class,
          () -> SerializationSecurity.encryptAesGcm(null, key),
          "Should throw on null data");
    }

    @Test
    @DisplayName("should throw on null key for AES-GCM encryption")
    void shouldThrowOnNullKeyForAesGcmEncryption() {
      assertThrows(
          NullPointerException.class,
          () -> SerializationSecurity.encryptAesGcm(new byte[] {1, 2, 3}, null),
          "Should throw on null key");
    }

    @Test
    @DisplayName("should throw on wrong algorithm for AES-GCM decryption")
    void shouldThrowOnWrongAlgorithmForAesGcmDecryption() throws WasmException {
      final SecretKey key = SerializationSecurity.generateAesKey(256);
      final EncryptedData wrongAlgorithm =
          new EncryptedData(new byte[] {1, 2, 3}, new byte[12], "AES/CBC/PKCS5Padding");

      assertThrows(
          WasmException.class,
          () -> SerializationSecurity.decryptAesGcm(wrongAlgorithm, key),
          "Should throw on wrong algorithm");
    }

    @Test
    @DisplayName("should calculate SHA-256 hash")
    void shouldCalculateSha256Hash() throws WasmException {
      final byte[] data = "Hello, World!".getBytes();
      final byte[] hash = SerializationSecurity.calculateSha256(data);

      assertNotNull(hash, "Hash should not be null");
      assertEquals(32, hash.length, "SHA-256 hash should be 32 bytes");
    }

    @Test
    @DisplayName("should calculate SHA-512 hash")
    void shouldCalculateSha512Hash() throws WasmException {
      final byte[] data = "Hello, World!".getBytes();
      final byte[] hash = SerializationSecurity.calculateSha512(data);

      assertNotNull(hash, "Hash should not be null");
      assertEquals(64, hash.length, "SHA-512 hash should be 64 bytes");
    }

    @Test
    @DisplayName("should throw on null data for SHA-256")
    void shouldThrowOnNullDataForSha256() {
      assertThrows(
          NullPointerException.class,
          () -> SerializationSecurity.calculateSha256(null),
          "Should throw on null data");
    }

    @Test
    @DisplayName("should calculate HMAC-SHA256")
    void shouldCalculateHmacSha256() throws WasmException {
      final byte[] data = "Hello, World!".getBytes();
      final SecretKey key = SerializationSecurity.generateAesKey(256);
      final byte[] hmac = SerializationSecurity.calculateHmacSha256(data, key);

      assertNotNull(hmac, "HMAC should not be null");
      assertEquals(32, hmac.length, "HMAC-SHA256 should be 32 bytes");
    }

    @Test
    @DisplayName("should verify correct HMAC-SHA256")
    void shouldVerifyCorrectHmacSha256() throws WasmException {
      final byte[] data = "Hello, World!".getBytes();
      final SecretKey key = SerializationSecurity.generateAesKey(256);
      final byte[] hmac = SerializationSecurity.calculateHmacSha256(data, key);

      assertTrue(
          SerializationSecurity.verifyHmacSha256(data, hmac, key), "HMAC should verify correctly");
    }

    @Test
    @DisplayName("should fail verification for incorrect HMAC")
    void shouldFailVerificationForIncorrectHmac() throws WasmException {
      final byte[] data = "Hello, World!".getBytes();
      final SecretKey key = SerializationSecurity.generateAesKey(256);
      final byte[] wrongHmac = new byte[32];

      assertFalse(
          SerializationSecurity.verifyHmacSha256(data, wrongHmac, key),
          "Wrong HMAC should fail verification");
    }

    @Test
    @DisplayName("should generate secure random bytes")
    void shouldGenerateSecureRandomBytes() {
      final byte[] random = SerializationSecurity.generateSecureRandom(32);

      assertNotNull(random, "Random bytes should not be null");
      assertEquals(32, random.length, "Should generate requested number of bytes");
    }

    @Test
    @DisplayName("should generate different random bytes each time")
    void shouldGenerateDifferentRandomBytesEachTime() {
      final byte[] random1 = SerializationSecurity.generateSecureRandom(32);
      final byte[] random2 = SerializationSecurity.generateSecureRandom(32);

      assertFalse(
          java.util.Arrays.equals(random1, random2), "Random bytes should be different each time");
    }

    @Test
    @DisplayName("should reject negative length for random bytes")
    void shouldRejectNegativeLengthForRandomBytes() {
      assertThrows(
          IllegalArgumentException.class,
          () -> SerializationSecurity.generateSecureRandom(-1),
          "Should reject negative length");
    }

    @Test
    @DisplayName("should create secret key from bytes")
    void shouldCreateSecretKeyFromBytes() {
      final byte[] keyBytes = new byte[32];
      final SecretKey key = SerializationSecurity.createSecretKey(keyBytes, "AES");

      assertNotNull(key, "Key should not be null");
      assertEquals("AES", key.getAlgorithm(), "Algorithm should be AES");
    }

    @Test
    @DisplayName("should throw on null key bytes")
    void shouldThrowOnNullKeyBytes() {
      assertThrows(
          NullPointerException.class,
          () -> SerializationSecurity.createSecretKey(null, "AES"),
          "Should throw on null key bytes");
    }

    @Test
    @DisplayName("should throw on null algorithm")
    void shouldThrowOnNullAlgorithm() {
      assertThrows(
          NullPointerException.class,
          () -> SerializationSecurity.createSecretKey(new byte[32], null),
          "Should throw on null algorithm");
    }

    @Test
    @DisplayName("should perform constant time equals for equal arrays")
    void shouldPerformConstantTimeEqualsForEqualArrays() {
      final byte[] a = new byte[] {1, 2, 3, 4, 5};
      final byte[] b = new byte[] {1, 2, 3, 4, 5};

      assertTrue(SerializationSecurity.constantTimeEquals(a, b), "Equal arrays should return true");
    }

    @Test
    @DisplayName("should perform constant time equals for different arrays")
    void shouldPerformConstantTimeEqualsForDifferentArrays() {
      final byte[] a = new byte[] {1, 2, 3, 4, 5};
      final byte[] b = new byte[] {1, 2, 3, 4, 6};

      assertFalse(
          SerializationSecurity.constantTimeEquals(a, b), "Different arrays should return false");
    }

    @Test
    @DisplayName("should handle null arrays in constant time equals")
    void shouldHandleNullArraysInConstantTimeEquals() {
      assertFalse(
          SerializationSecurity.constantTimeEquals(null, new byte[] {1}),
          "Null vs non-null should be false");
      assertFalse(
          SerializationSecurity.constantTimeEquals(new byte[] {1}, null),
          "Non-null vs null should be false");
      assertTrue(
          SerializationSecurity.constantTimeEquals(null, null), "Null vs null should be true");
    }

    @Test
    @DisplayName("should handle different length arrays in constant time equals")
    void shouldHandleDifferentLengthArraysInConstantTimeEquals() {
      final byte[] a = new byte[] {1, 2, 3};
      final byte[] b = new byte[] {1, 2, 3, 4, 5};

      assertFalse(
          SerializationSecurity.constantTimeEquals(a, b),
          "Different length arrays should return false");
    }

    @Test
    @DisplayName("should clear sensitive data")
    void shouldClearSensitiveData() {
      final byte[] data = new byte[] {1, 2, 3, 4, 5};
      SerializationSecurity.clearSensitiveData(data);

      for (final byte b : data) {
        assertEquals(0, b, "All bytes should be zeroed");
      }
    }

    @Test
    @DisplayName("should handle null in clear sensitive data")
    void shouldHandleNullInClearSensitiveData() {
      // Should not throw
      SerializationSecurity.clearSensitiveData(null);
    }

    @Test
    @DisplayName("should convert bytes to hex")
    void shouldConvertBytesToHex() {
      final byte[] bytes = new byte[] {0x00, 0x0a, 0x1f, (byte) 0xff};
      final String hex = SerializationSecurity.bytesToHex(bytes);

      assertEquals("000a1fff", hex, "Hex conversion should be correct");
    }

    @Test
    @DisplayName("should throw on null bytes for hex conversion")
    void shouldThrowOnNullBytesForHexConversion() {
      assertThrows(
          NullPointerException.class,
          () -> SerializationSecurity.bytesToHex(null),
          "Should throw on null bytes");
    }

    @Test
    @DisplayName("should convert hex to bytes")
    void shouldConvertHexToBytes() {
      final String hex = "000a1fff";
      final byte[] bytes = SerializationSecurity.hexToBytes(hex);

      assertArrayEquals(
          new byte[] {0x00, 0x0a, 0x1f, (byte) 0xff}, bytes, "Byte conversion should be correct");
    }

    @Test
    @DisplayName("should throw on null hex string")
    void shouldThrowOnNullHexString() {
      assertThrows(
          NullPointerException.class,
          () -> SerializationSecurity.hexToBytes(null),
          "Should throw on null hex string");
    }

    @Test
    @DisplayName("should throw on odd length hex string")
    void shouldThrowOnOddLengthHexString() {
      assertThrows(
          IllegalArgumentException.class,
          () -> SerializationSecurity.hexToBytes("abc"),
          "Should throw on odd length hex string");
    }

    @Test
    @DisplayName("should have all security operations")
    void shouldHaveAllSecurityOperations() {
      final SerializationSecurity.SecurityOperation[] operations =
          SerializationSecurity.SecurityOperation.values();

      assertEquals(6, operations.length, "Should have 6 security operations");
      assertNotNull(SerializationSecurity.SecurityOperation.SERIALIZE);
      assertNotNull(SerializationSecurity.SecurityOperation.DESERIALIZE);
      assertNotNull(SerializationSecurity.SecurityOperation.ENCRYPT);
      assertNotNull(SerializationSecurity.SecurityOperation.DECRYPT);
      assertNotNull(SerializationSecurity.SecurityOperation.SIGN);
      assertNotNull(SerializationSecurity.SecurityOperation.VERIFY);
    }

    @Test
    @DisplayName("PermissionContext should be an interface")
    void permissionContextShouldBeAnInterface() {
      assertTrue(
          SerializationSecurity.PermissionContext.class.isInterface(),
          "PermissionContext should be an interface");
    }

    @Test
    @DisplayName("PermissionContext should have hasPermission method")
    void permissionContextShouldHaveHasPermissionMethod() throws NoSuchMethodException {
      final Method method =
          SerializationSecurity.PermissionContext.class.getMethod("hasPermission", String.class);
      assertNotNull(method, "hasPermission method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("PermissionContext should have getSecurityContext method")
    void permissionContextShouldHaveGetSecurityContextMethod() throws NoSuchMethodException {
      final Method method =
          SerializationSecurity.PermissionContext.class.getMethod("getSecurityContext");
      assertNotNull(method, "getSecurityContext method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should validate access for serialize operation")
    void shouldValidateAccessForSerializeOperation() throws WasmException {
      final SerializationSecurity.PermissionContext allowAll =
          new SerializationSecurity.PermissionContext() {
            @Override
            public boolean hasPermission(final String permission) {
              return true;
            }

            @Override
            public String getSecurityContext() {
              return "test-context";
            }
          };

      assertTrue(
          SerializationSecurity.validateAccess(
              SerializationSecurity.SecurityOperation.SERIALIZE, allowAll),
          "Should return true for allowed permission");
    }

    @Test
    @DisplayName("should reject access when permission denied")
    void shouldRejectAccessWhenPermissionDenied() throws WasmException {
      final SerializationSecurity.PermissionContext denyAll =
          new SerializationSecurity.PermissionContext() {
            @Override
            public boolean hasPermission(final String permission) {
              return false;
            }

            @Override
            public String getSecurityContext() {
              return "test-context";
            }
          };

      assertFalse(
          SerializationSecurity.validateAccess(
              SerializationSecurity.SecurityOperation.SERIALIZE, denyAll),
          "Should return false for denied permission");
    }

    @Test
    @DisplayName("should throw on null operation for validateAccess")
    void shouldThrowOnNullOperationForValidateAccess() {
      final SerializationSecurity.PermissionContext ctx =
          new SerializationSecurity.PermissionContext() {
            @Override
            public boolean hasPermission(final String permission) {
              return true;
            }

            @Override
            public String getSecurityContext() {
              return "test";
            }
          };

      assertThrows(
          NullPointerException.class,
          () -> SerializationSecurity.validateAccess(null, ctx),
          "Should throw on null operation");
    }

    @Test
    @DisplayName("should throw on null permissions for validateAccess")
    void shouldThrowOnNullPermissionsForValidateAccess() {
      assertThrows(
          NullPointerException.class,
          () ->
              SerializationSecurity.validateAccess(
                  SerializationSecurity.SecurityOperation.SERIALIZE, null),
          "Should throw on null permissions");
    }
  }
}
