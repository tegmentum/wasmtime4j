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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link EncryptedData}.
 *
 * <p>EncryptedData is a public final class that serves as a container for encrypted WebAssembly
 * module data. It encapsulates ciphertext, initialization vector, algorithm, optional authentication
 * tag, and key identifier with defensive copying on all byte array inputs and outputs.
 */
@DisplayName("EncryptedData Tests")
class EncryptedDataTest {

  private static final byte[] SAMPLE_CIPHERTEXT = {1, 2, 3, 4, 5, 6, 7, 8};
  private static final byte[] SAMPLE_IV_12 = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120};
  private static final byte[] SAMPLE_IV_16 = {
    10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, (byte) 130, (byte) 140, (byte) 150,
    (byte) 160
  };
  private static final byte[] SAMPLE_AUTH_TAG = {(byte) 0xAA, (byte) 0xBB, (byte) 0xCC};
  private static final String AES_GCM = "AES/GCM/NoPadding";
  private static final String AES_CBC = "AES/CBC/PKCS5Padding";
  private static final String CHACHA20 = "ChaCha20-Poly1305";

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(EncryptedData.class.getModifiers()),
          "EncryptedData should be public");
      assertTrue(
          Modifier.isFinal(EncryptedData.class.getModifiers()),
          "EncryptedData should be final");
    }

    @Test
    @DisplayName("should have three-arg constructor")
    void shouldHaveThreeArgConstructor() throws NoSuchMethodException {
      assertNotNull(
          EncryptedData.class.getConstructor(byte[].class, byte[].class, String.class),
          "Three-arg constructor should exist");
    }

    @Test
    @DisplayName("should have five-arg constructor")
    void shouldHaveFiveArgConstructor() throws NoSuchMethodException {
      assertNotNull(
          EncryptedData.class.getConstructor(
              byte[].class, byte[].class, String.class, byte[].class, String.class),
          "Five-arg constructor should exist");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("three-arg constructor should set ciphertext, iv, and algorithm")
    void threeArgConstructorShouldSetFields() {
      final EncryptedData data = new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM);

      assertArrayEquals(
          SAMPLE_CIPHERTEXT, data.getCiphertext(), "Ciphertext should match input");
      assertArrayEquals(SAMPLE_IV_12, data.getIv(), "IV should match input");
      assertEquals(AES_GCM, data.getAlgorithm(), "Algorithm should match input");
      assertNull(data.getAuthTag(), "AuthTag should be null for three-arg constructor");
      assertNull(data.getKeyId(), "KeyId should be null for three-arg constructor");
    }

    @Test
    @DisplayName("five-arg constructor should set all fields including authTag and keyId")
    void fiveArgConstructorShouldSetAllFields() {
      final EncryptedData data =
          new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM, SAMPLE_AUTH_TAG, "key-001");

      assertArrayEquals(
          SAMPLE_CIPHERTEXT, data.getCiphertext(), "Ciphertext should match input");
      assertArrayEquals(SAMPLE_IV_12, data.getIv(), "IV should match input");
      assertEquals(AES_GCM, data.getAlgorithm(), "Algorithm should match input");
      assertArrayEquals(SAMPLE_AUTH_TAG, data.getAuthTag(), "AuthTag should match input");
      assertEquals("key-001", data.getKeyId(), "KeyId should match input");
    }

    @Test
    @DisplayName("should throw NullPointerException for null ciphertext")
    void shouldThrowForNullCiphertext() {
      final NullPointerException exception =
          assertThrows(
              NullPointerException.class,
              () -> new EncryptedData(null, SAMPLE_IV_12, AES_GCM),
              "Null ciphertext should throw NullPointerException");
      assertTrue(
          exception.getMessage().contains("Ciphertext"),
          "Message should mention ciphertext: " + exception.getMessage());
    }

    @Test
    @DisplayName("should throw NullPointerException for null IV")
    void shouldThrowForNullIv() {
      final NullPointerException exception =
          assertThrows(
              NullPointerException.class,
              () -> new EncryptedData(SAMPLE_CIPHERTEXT, null, AES_GCM),
              "Null IV should throw NullPointerException");
      assertTrue(
          exception.getMessage().contains("IV"),
          "Message should mention IV: " + exception.getMessage());
    }

    @Test
    @DisplayName("should throw NullPointerException for null algorithm")
    void shouldThrowForNullAlgorithm() {
      final NullPointerException exception =
          assertThrows(
              NullPointerException.class,
              () -> new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, null),
              "Null algorithm should throw NullPointerException");
      assertTrue(
          exception.getMessage().contains("Algorithm"),
          "Message should mention algorithm: " + exception.getMessage());
    }

    @Test
    @DisplayName("should accept null authTag without throwing")
    void shouldAcceptNullAuthTag() {
      final EncryptedData data =
          new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM, null, "key-001");
      assertNull(data.getAuthTag(), "Null authTag should be stored as null");
    }

    @Test
    @DisplayName("should accept null keyId without throwing")
    void shouldAcceptNullKeyId() {
      final EncryptedData data =
          new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM, SAMPLE_AUTH_TAG, null);
      assertNull(data.getKeyId(), "Null keyId should be stored as null");
    }

    @Test
    @DisplayName("should set encryption timestamp on construction")
    void shouldSetEncryptionTimestamp() {
      final Instant before = Instant.now();
      final EncryptedData data = new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM);
      final Instant after = Instant.now();

      assertNotNull(data.getEncryptionTimestamp(), "Timestamp should not be null");
      assertFalse(
          data.getEncryptionTimestamp().isBefore(before),
          "Timestamp should not be before construction start");
      assertFalse(
          data.getEncryptionTimestamp().isAfter(after),
          "Timestamp should not be after construction end");
    }
  }

  @Nested
  @DisplayName("Defensive Copy Tests")
  class DefensiveCopyTests {

    @Test
    @DisplayName("modifying original ciphertext array should not affect stored value")
    void modifyingOriginalCiphertextShouldNotAffectStored() {
      final byte[] originalCiphertext = {1, 2, 3, 4};
      final EncryptedData data = new EncryptedData(originalCiphertext, SAMPLE_IV_12, AES_GCM);

      originalCiphertext[0] = (byte) 99;

      assertEquals(
          (byte) 1,
          data.getCiphertext()[0],
          "Stored ciphertext should not be affected by modifying original array");
    }

    @Test
    @DisplayName("modifying returned ciphertext should not affect stored value")
    void modifyingReturnedCiphertextShouldNotAffectStored() {
      final EncryptedData data = new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM);

      final byte[] returned = data.getCiphertext();
      returned[0] = (byte) 99;

      assertEquals(
          SAMPLE_CIPHERTEXT[0],
          data.getCiphertext()[0],
          "Stored ciphertext should not be affected by modifying returned array");
    }

    @Test
    @DisplayName("modifying original IV array should not affect stored value")
    void modifyingOriginalIvShouldNotAffectStored() {
      final byte[] originalIv = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120};
      final EncryptedData data = new EncryptedData(SAMPLE_CIPHERTEXT, originalIv, AES_GCM);

      originalIv[0] = (byte) 99;

      assertEquals(
          (byte) 10,
          data.getIv()[0],
          "Stored IV should not be affected by modifying original array");
    }

    @Test
    @DisplayName("modifying returned IV should not affect stored value")
    void modifyingReturnedIvShouldNotAffectStored() {
      final EncryptedData data = new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM);

      final byte[] returned = data.getIv();
      returned[0] = (byte) 99;

      assertEquals(
          SAMPLE_IV_12[0],
          data.getIv()[0],
          "Stored IV should not be affected by modifying returned array");
    }

    @Test
    @DisplayName("modifying original authTag array should not affect stored value")
    void modifyingOriginalAuthTagShouldNotAffectStored() {
      final byte[] originalAuthTag = {(byte) 0xAA, (byte) 0xBB, (byte) 0xCC};
      final EncryptedData data =
          new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM, originalAuthTag, null);

      originalAuthTag[0] = (byte) 99;

      assertEquals(
          (byte) 0xAA,
          data.getAuthTag()[0],
          "Stored authTag should not be affected by modifying original array");
    }

    @Test
    @DisplayName("modifying returned authTag should not affect stored value")
    void modifyingReturnedAuthTagShouldNotAffectStored() {
      final EncryptedData data =
          new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM, SAMPLE_AUTH_TAG, null);

      final byte[] returned = data.getAuthTag();
      returned[0] = (byte) 99;

      assertEquals(
          SAMPLE_AUTH_TAG[0],
          data.getAuthTag()[0],
          "Stored authTag should not be affected by modifying returned array");
    }

    @Test
    @DisplayName("getCiphertext should return a new array each time")
    void getCiphertextShouldReturnNewArrayEachTime() {
      final EncryptedData data = new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM);

      final byte[] first = data.getCiphertext();
      final byte[] second = data.getCiphertext();

      assertNotSame(first, second, "getCiphertext should return a new array each call");
      assertArrayEquals(first, second, "Both arrays should have the same content");
    }

    @Test
    @DisplayName("getIv should return a new array each time")
    void getIvShouldReturnNewArrayEachTime() {
      final EncryptedData data = new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM);

      final byte[] first = data.getIv();
      final byte[] second = data.getIv();

      assertNotSame(first, second, "getIv should return a new array each call");
      assertArrayEquals(first, second, "Both arrays should have the same content");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getSize should return ciphertext length")
    void getSizeShouldReturnCiphertextLength() {
      final EncryptedData data = new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM);
      assertEquals(
          SAMPLE_CIPHERTEXT.length,
          data.getSize(),
          "getSize should return ciphertext array length");
    }

    @Test
    @DisplayName("getSize should return 0 for empty ciphertext")
    void getSizeShouldReturnZeroForEmptyCiphertext() {
      final EncryptedData data = new EncryptedData(new byte[0], SAMPLE_IV_12, AES_GCM);
      assertEquals(0, data.getSize(), "getSize should return 0 for empty ciphertext");
    }

    @Test
    @DisplayName("getEncryptionTimestamp should return non-null value")
    void getEncryptionTimestampShouldReturnNonNull() {
      final EncryptedData data = new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM);
      assertNotNull(data.getEncryptionTimestamp(), "Encryption timestamp should not be null");
    }
  }

  @Nested
  @DisplayName("Authenticated Encryption Tests")
  class AuthenticatedEncryptionTests {

    @Test
    @DisplayName("should return true when authTag is present regardless of algorithm")
    void shouldReturnTrueWhenAuthTagPresent() {
      final EncryptedData data =
          new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_16, AES_CBC, SAMPLE_AUTH_TAG, null);
      assertTrue(
          data.isAuthenticatedEncryption(),
          "Should be authenticated when authTag is present even for CBC");
    }

    @Test
    @DisplayName("should return true for GCM algorithm even without authTag")
    void shouldReturnTrueForGcmWithoutAuthTag() {
      final EncryptedData data = new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM);
      assertTrue(
          data.isAuthenticatedEncryption(),
          "Should be authenticated for GCM algorithm even without explicit authTag");
    }

    @Test
    @DisplayName("should return false for CBC without authTag")
    void shouldReturnFalseForCbcWithoutAuthTag() {
      final EncryptedData data = new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_16, AES_CBC);
      assertFalse(
          data.isAuthenticatedEncryption(),
          "Should not be authenticated for CBC without authTag");
    }

    @Test
    @DisplayName("should return false for unknown algorithm without authTag")
    void shouldReturnFalseForUnknownAlgorithmWithoutAuthTag() {
      final EncryptedData data =
          new EncryptedData(SAMPLE_CIPHERTEXT, new byte[] {1, 2, 3, 4, 5, 6, 7, 8}, "DES/ECB");
      assertFalse(
          data.isAuthenticatedEncryption(),
          "Should not be authenticated for unknown algorithm without authTag");
    }
  }

  @Nested
  @DisplayName("Validate Integrity Tests")
  class ValidateIntegrityTests {

    @Test
    @DisplayName("should return false for empty ciphertext")
    void shouldReturnFalseForEmptyCiphertext() {
      final EncryptedData data = new EncryptedData(new byte[0], SAMPLE_IV_12, AES_GCM);
      assertFalse(data.validateIntegrity(), "Empty ciphertext should fail integrity validation");
    }

    @Test
    @DisplayName("should return false for empty IV")
    void shouldReturnFalseForEmptyIv() {
      final EncryptedData data = new EncryptedData(SAMPLE_CIPHERTEXT, new byte[0], AES_GCM);
      assertFalse(data.validateIntegrity(), "Empty IV should fail integrity validation");
    }

    @Test
    @DisplayName("should return true for GCM with 12-byte IV")
    void shouldReturnTrueForGcmWith12ByteIv() {
      final EncryptedData data = new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM);
      assertTrue(
          data.validateIntegrity(), "GCM with 12-byte IV should pass integrity validation");
    }

    @Test
    @DisplayName("should return false for GCM with non-12-byte IV")
    void shouldReturnFalseForGcmWithNon12ByteIv() {
      final EncryptedData data = new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_16, AES_GCM);
      assertFalse(
          data.validateIntegrity(), "GCM with 16-byte IV should fail integrity validation");
    }

    @Test
    @DisplayName("should return true for CBC with 16-byte IV")
    void shouldReturnTrueForCbcWith16ByteIv() {
      final EncryptedData data = new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_16, AES_CBC);
      assertTrue(
          data.validateIntegrity(), "CBC with 16-byte IV should pass integrity validation");
    }

    @Test
    @DisplayName("should return false for CBC with non-16-byte IV")
    void shouldReturnFalseForCbcWithNon16ByteIv() {
      final EncryptedData data = new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_CBC);
      assertFalse(
          data.validateIntegrity(), "CBC with 12-byte IV should fail integrity validation");
    }

    @Test
    @DisplayName("should return true for unknown algorithm with non-empty ciphertext and IV")
    void shouldReturnTrueForUnknownAlgorithm() {
      final EncryptedData data =
          new EncryptedData(SAMPLE_CIPHERTEXT, new byte[] {1, 2, 3}, CHACHA20);
      assertTrue(
          data.validateIntegrity(),
          "Unknown algorithm should pass integrity if ciphertext and IV are non-empty");
    }
  }

  @Nested
  @DisplayName("Serialization Tests")
  class SerializationTests {

    @Test
    @DisplayName("serialize and deserialize should preserve ciphertext")
    void serializeDeserializeShouldPreserveCiphertext() {
      final EncryptedData original =
          new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM);

      final byte[] serialized = original.serialize();
      final EncryptedData deserialized = EncryptedData.deserialize(serialized);

      assertArrayEquals(
          original.getCiphertext(),
          deserialized.getCiphertext(),
          "Ciphertext should be preserved through serialization roundtrip");
    }

    @Test
    @DisplayName("serialize and deserialize should preserve IV")
    void serializeDeserializeShouldPreserveIv() {
      final EncryptedData original =
          new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM);

      final byte[] serialized = original.serialize();
      final EncryptedData deserialized = EncryptedData.deserialize(serialized);

      assertArrayEquals(
          original.getIv(),
          deserialized.getIv(),
          "IV should be preserved through serialization roundtrip");
    }

    @Test
    @DisplayName("serialize and deserialize should preserve algorithm")
    void serializeDeserializeShouldPreserveAlgorithm() {
      final EncryptedData original =
          new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM);

      final byte[] serialized = original.serialize();
      final EncryptedData deserialized = EncryptedData.deserialize(serialized);

      assertEquals(
          original.getAlgorithm(),
          deserialized.getAlgorithm(),
          "Algorithm should be preserved through serialization roundtrip");
    }

    @Test
    @DisplayName("serialize and deserialize should preserve authTag when present")
    void serializeDeserializeShouldPreserveAuthTag() {
      final EncryptedData original =
          new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM, SAMPLE_AUTH_TAG, null);

      final byte[] serialized = original.serialize();
      final EncryptedData deserialized = EncryptedData.deserialize(serialized);

      assertArrayEquals(
          original.getAuthTag(),
          deserialized.getAuthTag(),
          "AuthTag should be preserved through serialization roundtrip");
    }

    @Test
    @DisplayName("serialize and deserialize should preserve null authTag")
    void serializeDeserializeShouldPreserveNullAuthTag() {
      final EncryptedData original = new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM);

      final byte[] serialized = original.serialize();
      final EncryptedData deserialized = EncryptedData.deserialize(serialized);

      assertNull(
          deserialized.getAuthTag(),
          "Null authTag should remain null after serialization roundtrip");
    }

    @Test
    @DisplayName("serialize and deserialize should preserve keyId when present")
    void serializeDeserializeShouldPreserveKeyId() {
      final EncryptedData original =
          new EncryptedData(
              SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM, SAMPLE_AUTH_TAG, "my-key-123");

      final byte[] serialized = original.serialize();
      final EncryptedData deserialized = EncryptedData.deserialize(serialized);

      assertEquals(
          "my-key-123",
          deserialized.getKeyId(),
          "KeyId should be preserved through serialization roundtrip");
    }

    @Test
    @DisplayName("serialize and deserialize should preserve null keyId")
    void serializeDeserializeShouldPreserveNullKeyId() {
      final EncryptedData original = new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM);

      final byte[] serialized = original.serialize();
      final EncryptedData deserialized = EncryptedData.deserialize(serialized);

      assertNull(
          deserialized.getKeyId(),
          "Null keyId should remain null after serialization roundtrip");
    }

    @Test
    @DisplayName("deserialize should throw NullPointerException for null input")
    void deserializeShouldThrowForNullInput() {
      assertThrows(
          NullPointerException.class,
          () -> EncryptedData.deserialize(null),
          "Deserializing null should throw NullPointerException");
    }

    @Test
    @DisplayName("deserialize should throw IllegalArgumentException for invalid version")
    void deserializeShouldThrowForInvalidVersion() {
      final java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(4);
      buffer.putInt(99);

      assertThrows(
          IllegalArgumentException.class,
          () -> EncryptedData.deserialize(buffer.array()),
          "Deserializing data with version 99 should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("deserialize should throw for truncated data")
    void deserializeShouldThrowForTruncatedData() {
      final java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(8);
      buffer.putInt(1); // version
      buffer.putInt(100); // algorithm length (but no actual data follows)

      assertThrows(
          IllegalArgumentException.class,
          () -> EncryptedData.deserialize(buffer.array()),
          "Deserializing truncated data should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("serialize should produce non-null non-empty byte array")
    void serializeShouldProduceNonEmptyArray() {
      final EncryptedData data = new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM);
      final byte[] serialized = data.serialize();

      assertNotNull(serialized, "Serialized data should not be null");
      assertTrue(serialized.length > 0, "Serialized data should not be empty");
    }

    @Test
    @DisplayName("serialize should produce correctly sized byte array")
    void serializeShouldProduceCorrectlySizedByteArray() {
      final EncryptedData data =
          new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM, SAMPLE_AUTH_TAG, "key-001");
      final byte[] serialized = data.serialize();

      // Calculate expected size:
      // - 4 bytes version
      // - 4 bytes + algorithm length
      // - 4 bytes + iv length
      // - 4 bytes + ciphertext length
      // - 4 bytes + authTag length
      // - 4 bytes + keyId length
      // - 4 bytes + timestamp length
      final byte[] algorithmBytes = AES_GCM.getBytes(java.nio.charset.StandardCharsets.UTF_8);
      final byte[] keyIdBytes = "key-001".getBytes(java.nio.charset.StandardCharsets.UTF_8);
      final byte[] timestampBytes =
          data.getEncryptionTimestamp().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

      final int expectedSize =
          4  // version
              + 4
              + algorithmBytes.length  // algorithm
              + 4
              + SAMPLE_IV_12.length  // iv
              + 4
              + SAMPLE_CIPHERTEXT.length  // ciphertext
              + 4
              + SAMPLE_AUTH_TAG.length  // authTag
              + 4
              + keyIdBytes.length  // keyId
              + 4
              + timestampBytes.length;  // timestamp

      assertEquals(
          expectedSize,
          serialized.length,
          "Serialized data size should match expected: version(4) + algorithm(4+"
              + algorithmBytes.length
              + ") + iv(4+"
              + SAMPLE_IV_12.length
              + ") + ciphertext(4+"
              + SAMPLE_CIPHERTEXT.length
              + ") + authTag(4+"
              + SAMPLE_AUTH_TAG.length
              + ") + keyId(4+"
              + keyIdBytes.length
              + ") + timestamp(4+"
              + timestampBytes.length
              + ") = "
              + expectedSize);
    }

    @Test
    @DisplayName("serialized data should have correct byte structure")
    void serializedDataShouldHaveCorrectByteStructure() {
      final EncryptedData data =
          new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM, SAMPLE_AUTH_TAG, "test-key");
      final byte[] serialized = data.serialize();

      final java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(serialized);

      // Version should be 1
      assertEquals(1, buffer.getInt(), "Version should be 1");

      // Algorithm length and content
      final int algorithmLen = buffer.getInt();
      final byte[] algorithmBytes = AES_GCM.getBytes(java.nio.charset.StandardCharsets.UTF_8);
      assertEquals(
          algorithmBytes.length, algorithmLen, "Algorithm length should match expected");

      final byte[] readAlgorithm = new byte[algorithmLen];
      buffer.get(readAlgorithm);
      assertEquals(AES_GCM, new String(readAlgorithm, java.nio.charset.StandardCharsets.UTF_8));

      // IV length and content
      final int ivLen = buffer.getInt();
      assertEquals(SAMPLE_IV_12.length, ivLen, "IV length should match expected");

      final byte[] readIv = new byte[ivLen];
      buffer.get(readIv);
      assertArrayEquals(SAMPLE_IV_12, readIv, "IV content should match");

      // Ciphertext length and content
      final int ciphertextLen = buffer.getInt();
      assertEquals(SAMPLE_CIPHERTEXT.length, ciphertextLen, "Ciphertext length should match");

      final byte[] readCiphertext = new byte[ciphertextLen];
      buffer.get(readCiphertext);
      assertArrayEquals(SAMPLE_CIPHERTEXT, readCiphertext, "Ciphertext content should match");

      // AuthTag length and content
      final int authTagLen = buffer.getInt();
      assertEquals(SAMPLE_AUTH_TAG.length, authTagLen, "AuthTag length should match");

      final byte[] readAuthTag = new byte[authTagLen];
      buffer.get(readAuthTag);
      assertArrayEquals(SAMPLE_AUTH_TAG, readAuthTag, "AuthTag content should match");

      // KeyId length and content
      final int keyIdLen = buffer.getInt();
      assertTrue(keyIdLen > 0, "KeyId length should be positive");

      final byte[] readKeyId = new byte[keyIdLen];
      buffer.get(readKeyId);
      assertEquals("test-key", new String(readKeyId, java.nio.charset.StandardCharsets.UTF_8));

      // Timestamp length
      final int timestampLen = buffer.getInt();
      assertTrue(timestampLen > 0, "Timestamp length should be positive");

      // Should have consumed all bytes without BufferUnderflowException
      final byte[] readTimestamp = new byte[timestampLen];
      buffer.get(readTimestamp);

      // Buffer should now be fully consumed
      assertEquals(0, buffer.remaining(), "Buffer should be fully consumed");
    }

    @Test
    @DisplayName("deserialize should handle boundary case of timestampLength zero")
    void deserializeShouldHandleBoundaryTimestampLengthZero() {
      // Create a custom serialized buffer with timestamp length of 0
      final byte[] algorithmBytes = AES_GCM.getBytes(java.nio.charset.StandardCharsets.UTF_8);

      final int size =
          4 + 4 + algorithmBytes.length + 4 + SAMPLE_IV_12.length + 4 + SAMPLE_CIPHERTEXT.length
              + 4 + 0  // no authTag
              + 4 + 0  // no keyId
              + 4 + 0; // no timestamp

      final java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(size);
      buffer.putInt(1);  // version
      buffer.putInt(algorithmBytes.length);
      buffer.put(algorithmBytes);
      buffer.putInt(SAMPLE_IV_12.length);
      buffer.put(SAMPLE_IV_12);
      buffer.putInt(SAMPLE_CIPHERTEXT.length);
      buffer.put(SAMPLE_CIPHERTEXT);
      buffer.putInt(0);  // no authTag
      buffer.putInt(0);  // no keyId
      buffer.putInt(0);  // no timestamp

      final EncryptedData data = EncryptedData.deserialize(buffer.array());
      assertNotNull(data);
      assertArrayEquals(SAMPLE_CIPHERTEXT, data.getCiphertext());
    }
  }

  @Nested
  @DisplayName("WithKeyId Tests")
  class WithKeyIdTests {

    @Test
    @DisplayName("should create new instance with updated keyId")
    void shouldCreateNewInstanceWithUpdatedKeyId() {
      final EncryptedData original =
          new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM, SAMPLE_AUTH_TAG, "key-001");

      final EncryptedData updated = original.withKeyId("key-002");

      assertEquals("key-002", updated.getKeyId(), "Updated keyId should be 'key-002'");
      assertEquals("key-001", original.getKeyId(), "Original keyId should remain 'key-001'");
    }

    @Test
    @DisplayName("withKeyId should preserve ciphertext and iv")
    void withKeyIdShouldPreserveCiphertextAndIv() {
      final EncryptedData original =
          new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM, SAMPLE_AUTH_TAG, "key-001");

      final EncryptedData updated = original.withKeyId("key-002");

      assertArrayEquals(
          original.getCiphertext(),
          updated.getCiphertext(),
          "Ciphertext should be preserved");
      assertArrayEquals(original.getIv(), updated.getIv(), "IV should be preserved");
      assertEquals(
          original.getAlgorithm(), updated.getAlgorithm(), "Algorithm should be preserved");
      assertArrayEquals(
          original.getAuthTag(), updated.getAuthTag(), "AuthTag should be preserved");
    }

    @Test
    @DisplayName("withKeyId should accept null keyId")
    void withKeyIdShouldAcceptNull() {
      final EncryptedData original =
          new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM, null, "key-001");

      final EncryptedData updated = original.withKeyId(null);

      assertNull(updated.getKeyId(), "Updated keyId should be null");
    }
  }

  @Nested
  @DisplayName("Clear Tests")
  class ClearTests {

    @Test
    @DisplayName("clear should zero out ciphertext")
    void clearShouldZeroCiphertext() {
      final EncryptedData data =
          new EncryptedData(
              new byte[] {1, 2, 3, 4}, SAMPLE_IV_12, AES_GCM);

      data.clear();

      final byte[] clearedCiphertext = data.getCiphertext();
      for (int i = 0; i < clearedCiphertext.length; i++) {
        assertEquals(
            (byte) 0, clearedCiphertext[i], "Ciphertext byte " + i + " should be zeroed");
      }
    }

    @Test
    @DisplayName("clear should zero out IV")
    void clearShouldZeroIv() {
      final EncryptedData data =
          new EncryptedData(SAMPLE_CIPHERTEXT, new byte[] {10, 20, 30, 40}, AES_CBC);

      data.clear();

      final byte[] clearedIv = data.getIv();
      for (int i = 0; i < clearedIv.length; i++) {
        assertEquals((byte) 0, clearedIv[i], "IV byte " + i + " should be zeroed");
      }
    }

    @Test
    @DisplayName("clear should zero out authTag when present")
    void clearShouldZeroAuthTag() {
      final EncryptedData data =
          new EncryptedData(
              SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM, new byte[] {1, 2, 3}, null);

      data.clear();

      final byte[] clearedAuthTag = data.getAuthTag();
      assertNotNull(clearedAuthTag, "AuthTag should still be non-null after clear");
      for (int i = 0; i < clearedAuthTag.length; i++) {
        assertEquals((byte) 0, clearedAuthTag[i], "AuthTag byte " + i + " should be zeroed");
      }
    }

    @Test
    @DisplayName("clear should not throw when authTag is null")
    void clearShouldNotThrowWhenAuthTagIsNull() {
      final EncryptedData data = new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM);
      data.clear(); // Should not throw
      assertNull(data.getAuthTag(), "AuthTag should remain null after clear");
    }

    @Test
    @DisplayName("getSize should still return original length after clear")
    void getSizeShouldReturnOriginalLengthAfterClear() {
      final byte[] ciphertext = new byte[] {1, 2, 3, 4, 5};
      final EncryptedData data = new EncryptedData(ciphertext, SAMPLE_IV_12, AES_GCM);

      data.clear();

      assertEquals(5, data.getSize(), "getSize should return original length even after clear");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("equal objects should have same hashCode")
    void equalObjectsShouldHaveSameHashCode() {
      final EncryptedData data1 =
          new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM, SAMPLE_AUTH_TAG, "key-001");
      final EncryptedData data2 =
          new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM, SAMPLE_AUTH_TAG, "key-001");

      assertEquals(data1, data2, "Objects with same fields should be equal");
      assertEquals(
          data1.hashCode(),
          data2.hashCode(),
          "Equal objects should have the same hashCode");
    }

    @Test
    @DisplayName("objects with different ciphertext should not be equal")
    void objectsWithDifferentCiphertextShouldNotBeEqual() {
      final EncryptedData data1 =
          new EncryptedData(new byte[] {1, 2, 3}, SAMPLE_IV_12, AES_GCM);
      final EncryptedData data2 =
          new EncryptedData(new byte[] {4, 5, 6}, SAMPLE_IV_12, AES_GCM);

      assertNotEquals(data1, data2, "Different ciphertext should make objects not equal");
    }

    @Test
    @DisplayName("objects with different IV should not be equal")
    void objectsWithDifferentIvShouldNotBeEqual() {
      final byte[] iv1 = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
      final byte[] iv2 = {12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1};
      final EncryptedData data1 = new EncryptedData(SAMPLE_CIPHERTEXT, iv1, AES_GCM);
      final EncryptedData data2 = new EncryptedData(SAMPLE_CIPHERTEXT, iv2, AES_GCM);

      assertNotEquals(data1, data2, "Different IV should make objects not equal");
    }

    @Test
    @DisplayName("objects with different algorithm should not be equal")
    void objectsWithDifferentAlgorithmShouldNotBeEqual() {
      final EncryptedData data1 =
          new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM);
      final EncryptedData data2 =
          new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, CHACHA20);

      assertNotEquals(data1, data2, "Different algorithm should make objects not equal");
    }

    @Test
    @DisplayName("objects with different keyId should not be equal")
    void objectsWithDifferentKeyIdShouldNotBeEqual() {
      final EncryptedData data1 =
          new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM, null, "key-001");
      final EncryptedData data2 =
          new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM, null, "key-002");

      assertNotEquals(data1, data2, "Different keyId should make objects not equal");
    }

    @Test
    @DisplayName("objects with different authTag should not be equal")
    void objectsWithDifferentAuthTagShouldNotBeEqual() {
      final EncryptedData data1 =
          new EncryptedData(
              SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM, new byte[] {1, 2, 3}, null);
      final EncryptedData data2 =
          new EncryptedData(
              SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM, new byte[] {4, 5, 6}, null);

      assertNotEquals(data1, data2, "Different authTag should make objects not equal");
    }

    @Test
    @DisplayName("equals should ignore timestamp differences")
    void equalsShouldIgnoreTimestampDifferences() {
      final EncryptedData data1 =
          new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM);
      final EncryptedData data2 =
          new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM);

      assertEquals(
          data1,
          data2,
          "Objects with different timestamps but same content should be equal");
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      final EncryptedData data = new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM);
      assertNotEquals(null, data, "EncryptedData should not be equal to null");
    }

    @Test
    @DisplayName("should not be equal to different type")
    void shouldNotBeEqualToDifferentType() {
      final EncryptedData data = new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM);
      assertNotEquals("not encrypted data", data, "EncryptedData should not be equal to String");
    }

    @Test
    @DisplayName("should be equal to itself")
    void shouldBeEqualToItself() {
      final EncryptedData data = new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM);
      assertEquals(data, data, "EncryptedData should be equal to itself");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain algorithm")
    void toStringShouldContainAlgorithm() {
      final EncryptedData data = new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM);
      assertTrue(
          data.toString().contains(AES_GCM),
          "toString should contain the algorithm name");
    }

    @Test
    @DisplayName("toString should contain size")
    void toStringShouldContainSize() {
      final EncryptedData data = new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM);
      assertTrue(
          data.toString().contains(String.valueOf(SAMPLE_CIPHERTEXT.length)),
          "toString should contain the ciphertext size");
    }

    @Test
    @DisplayName("toString should contain authenticated status")
    void toStringShouldContainAuthenticatedStatus() {
      final EncryptedData data = new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM);
      assertTrue(
          data.toString().contains("true"),
          "toString should contain authenticated=true for GCM");
    }

    @Test
    @DisplayName("toString should contain keyId or none")
    void toStringShouldContainKeyIdOrNone() {
      final EncryptedData withKey =
          new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM, null, "my-key");
      assertTrue(
          withKey.toString().contains("my-key"),
          "toString should contain the keyId when present");

      final EncryptedData withoutKey =
          new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM);
      assertTrue(
          withoutKey.toString().contains("none"),
          "toString should contain 'none' when keyId is null");
    }

    @Test
    @DisplayName("toString should contain timestamp")
    void toStringShouldContainTimestamp() {
      final EncryptedData data = new EncryptedData(SAMPLE_CIPHERTEXT, SAMPLE_IV_12, AES_GCM);
      assertNotNull(data.getEncryptionTimestamp(), "Timestamp should be set");
      assertTrue(
          data.toString().contains("timestamp="),
          "toString should contain timestamp information");
    }
  }
}
