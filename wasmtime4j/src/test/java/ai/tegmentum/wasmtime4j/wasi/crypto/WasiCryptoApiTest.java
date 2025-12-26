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

package ai.tegmentum.wasmtime4j.wasi.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive API tests for the WASI Crypto package.
 *
 * <p>Tests the structure, contracts, and behavior of all classes in the wasi.crypto package using
 * reflection-based testing to verify API contracts without requiring runtime initialization.
 *
 * @since 1.0.0
 */
@DisplayName("WASI Crypto API Tests")
class WasiCryptoApiTest {

  private static final Logger LOGGER = Logger.getLogger(WasiCryptoApiTest.class.getName());

  // ==================== HashAlgorithm Tests ====================

  @Nested
  @DisplayName("HashAlgorithm Enum Tests")
  class HashAlgorithmTests {

    @Test
    @DisplayName("Should have exactly 10 hash algorithms")
    void shouldHaveExactly10HashAlgorithms() {
      HashAlgorithm[] values = HashAlgorithm.values();
      assertEquals(10, values.length, "HashAlgorithm should have exactly 10 values");
      LOGGER.info("HashAlgorithm has " + values.length + " values: " + Arrays.toString(values));
    }

    @Test
    @DisplayName("Should contain all expected SHA algorithms")
    void shouldContainAllExpectedShaAlgorithms() {
      Set<String> expected =
          new HashSet<>(
              Arrays.asList(
                  "SHA_256",
                  "SHA_384",
                  "SHA_512",
                  "SHA_512_256",
                  "SHA3_256",
                  "SHA3_384",
                  "SHA3_512"));
      for (String name : expected) {
        assertDoesNotThrow(
            () -> HashAlgorithm.valueOf(name), "HashAlgorithm should contain " + name);
      }
    }

    @Test
    @DisplayName("Should contain all expected BLAKE algorithms")
    void shouldContainAllExpectedBlakeAlgorithms() {
      Set<String> expected = new HashSet<>(Arrays.asList("BLAKE2B", "BLAKE2S", "BLAKE3"));
      for (String name : expected) {
        assertDoesNotThrow(
            () -> HashAlgorithm.valueOf(name), "HashAlgorithm should contain " + name);
      }
    }

    @Test
    @DisplayName("Should have correct algorithm names")
    void shouldHaveCorrectAlgorithmNames() {
      assertEquals("SHA-256", HashAlgorithm.SHA_256.getAlgorithmName());
      assertEquals("SHA-384", HashAlgorithm.SHA_384.getAlgorithmName());
      assertEquals("SHA-512", HashAlgorithm.SHA_512.getAlgorithmName());
      assertEquals("SHA-512/256", HashAlgorithm.SHA_512_256.getAlgorithmName());
      assertEquals("SHA3-256", HashAlgorithm.SHA3_256.getAlgorithmName());
      assertEquals("SHA3-384", HashAlgorithm.SHA3_384.getAlgorithmName());
      assertEquals("SHA3-512", HashAlgorithm.SHA3_512.getAlgorithmName());
      assertEquals("BLAKE2b", HashAlgorithm.BLAKE2B.getAlgorithmName());
      assertEquals("BLAKE2s", HashAlgorithm.BLAKE2S.getAlgorithmName());
      assertEquals("BLAKE3", HashAlgorithm.BLAKE3.getAlgorithmName());
    }

    @Test
    @DisplayName("Should have correct output sizes")
    void shouldHaveCorrectOutputSizes() {
      assertEquals(256, HashAlgorithm.SHA_256.getOutputSize());
      assertEquals(384, HashAlgorithm.SHA_384.getOutputSize());
      assertEquals(512, HashAlgorithm.SHA_512.getOutputSize());
      assertEquals(256, HashAlgorithm.SHA_512_256.getOutputSize());
      assertEquals(256, HashAlgorithm.SHA3_256.getOutputSize());
      assertEquals(384, HashAlgorithm.SHA3_384.getOutputSize());
      assertEquals(512, HashAlgorithm.SHA3_512.getOutputSize());
      assertEquals(512, HashAlgorithm.BLAKE2B.getOutputSize());
      assertEquals(256, HashAlgorithm.BLAKE2S.getOutputSize());
      assertEquals(256, HashAlgorithm.BLAKE3.getOutputSize());
    }

    @Test
    @DisplayName("Should have getAlgorithmName method")
    void shouldHaveGetAlgorithmNameMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = HashAlgorithm.class.getDeclaredMethod("getAlgorithmName");
            assertEquals(String.class, method.getReturnType());
            assertTrue(Modifier.isPublic(method.getModifiers()));
          });
    }

    @Test
    @DisplayName("Should have getOutputSize method")
    void shouldHaveGetOutputSizeMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = HashAlgorithm.class.getDeclaredMethod("getOutputSize");
            assertEquals(int.class, method.getReturnType());
            assertTrue(Modifier.isPublic(method.getModifiers()));
          });
    }
  }

  // ==================== SymmetricAlgorithm Tests ====================

  @Nested
  @DisplayName("SymmetricAlgorithm Enum Tests")
  class SymmetricAlgorithmTests {

    @Test
    @DisplayName("Should have exactly 7 symmetric algorithms")
    void shouldHaveExactly7SymmetricAlgorithms() {
      SymmetricAlgorithm[] values = SymmetricAlgorithm.values();
      assertEquals(7, values.length, "SymmetricAlgorithm should have exactly 7 values");
      LOGGER.info(
          "SymmetricAlgorithm has " + values.length + " values: " + Arrays.toString(values));
    }

    @Test
    @DisplayName("Should contain all expected AES algorithms")
    void shouldContainAllExpectedAesAlgorithms() {
      Set<String> expected = new HashSet<>(Arrays.asList("AES_128", "AES_192", "AES_256"));
      for (String name : expected) {
        assertDoesNotThrow(
            () -> SymmetricAlgorithm.valueOf(name), "SymmetricAlgorithm should contain " + name);
      }
    }

    @Test
    @DisplayName("Should contain all expected ChaCha algorithms")
    void shouldContainAllExpectedChaChaAlgorithms() {
      Set<String> expected =
          new HashSet<>(
              Arrays.asList("CHACHA20", "XCHACHA20", "CHACHA20_POLY1305", "XCHACHA20_POLY1305"));
      for (String name : expected) {
        assertDoesNotThrow(
            () -> SymmetricAlgorithm.valueOf(name), "SymmetricAlgorithm should contain " + name);
      }
    }

    @Test
    @DisplayName("Should have correct key sizes")
    void shouldHaveCorrectKeySizes() {
      assertEquals(128, SymmetricAlgorithm.AES_128.getKeySize());
      assertEquals(192, SymmetricAlgorithm.AES_192.getKeySize());
      assertEquals(256, SymmetricAlgorithm.AES_256.getKeySize());
      assertEquals(256, SymmetricAlgorithm.CHACHA20.getKeySize());
      assertEquals(256, SymmetricAlgorithm.XCHACHA20.getKeySize());
      assertEquals(256, SymmetricAlgorithm.CHACHA20_POLY1305.getKeySize());
      assertEquals(256, SymmetricAlgorithm.XCHACHA20_POLY1305.getKeySize());
    }

    @Test
    @DisplayName("Should have correct algorithm names")
    void shouldHaveCorrectAlgorithmNames() {
      assertEquals("AES-128", SymmetricAlgorithm.AES_128.getAlgorithmName());
      assertEquals("AES-192", SymmetricAlgorithm.AES_192.getAlgorithmName());
      assertEquals("AES-256", SymmetricAlgorithm.AES_256.getAlgorithmName());
      assertEquals("ChaCha20", SymmetricAlgorithm.CHACHA20.getAlgorithmName());
      assertEquals("XChaCha20", SymmetricAlgorithm.XCHACHA20.getAlgorithmName());
      assertEquals("ChaCha20-Poly1305", SymmetricAlgorithm.CHACHA20_POLY1305.getAlgorithmName());
      assertEquals("XChaCha20-Poly1305", SymmetricAlgorithm.XCHACHA20_POLY1305.getAlgorithmName());
    }
  }

  // ==================== AsymmetricAlgorithm Tests ====================

  @Nested
  @DisplayName("AsymmetricAlgorithm Enum Tests")
  class AsymmetricAlgorithmTests {

    @Test
    @DisplayName("Should have exactly 9 asymmetric algorithms")
    void shouldHaveExactly9AsymmetricAlgorithms() {
      AsymmetricAlgorithm[] values = AsymmetricAlgorithm.values();
      assertEquals(9, values.length, "AsymmetricAlgorithm should have exactly 9 values");
      LOGGER.info(
          "AsymmetricAlgorithm has " + values.length + " values: " + Arrays.toString(values));
    }

    @Test
    @DisplayName("Should contain all expected RSA algorithms")
    void shouldContainAllExpectedRsaAlgorithms() {
      Set<String> expected = new HashSet<>(Arrays.asList("RSA_2048", "RSA_3072", "RSA_4096"));
      for (String name : expected) {
        assertDoesNotThrow(
            () -> AsymmetricAlgorithm.valueOf(name), "AsymmetricAlgorithm should contain " + name);
      }
    }

    @Test
    @DisplayName("Should contain all expected curve-based algorithms")
    void shouldContainAllExpectedCurveAlgorithms() {
      Set<String> expected =
          new HashSet<>(
              Arrays.asList(
                  "X25519", "X448", "ECDH_P256", "ECDH_P384", "ECDH_P521", "ECDH_SECP256K1"));
      for (String name : expected) {
        assertDoesNotThrow(
            () -> AsymmetricAlgorithm.valueOf(name), "AsymmetricAlgorithm should contain " + name);
      }
    }

    @Test
    @DisplayName("Should have correct key sizes")
    void shouldHaveCorrectKeySizes() {
      assertEquals(2048, AsymmetricAlgorithm.RSA_2048.getKeySize());
      assertEquals(3072, AsymmetricAlgorithm.RSA_3072.getKeySize());
      assertEquals(4096, AsymmetricAlgorithm.RSA_4096.getKeySize());
      assertEquals(256, AsymmetricAlgorithm.X25519.getKeySize());
      assertEquals(448, AsymmetricAlgorithm.X448.getKeySize());
      assertEquals(256, AsymmetricAlgorithm.ECDH_P256.getKeySize());
      assertEquals(384, AsymmetricAlgorithm.ECDH_P384.getKeySize());
      assertEquals(521, AsymmetricAlgorithm.ECDH_P521.getKeySize());
      assertEquals(256, AsymmetricAlgorithm.ECDH_SECP256K1.getKeySize());
    }
  }

  // ==================== SignatureAlgorithm Tests ====================

  @Nested
  @DisplayName("SignatureAlgorithm Enum Tests")
  class SignatureAlgorithmTests {

    @Test
    @DisplayName("Should have exactly 12 signature algorithms")
    void shouldHaveExactly12SignatureAlgorithms() {
      SignatureAlgorithm[] values = SignatureAlgorithm.values();
      assertEquals(12, values.length, "SignatureAlgorithm should have exactly 12 values");
      LOGGER.info(
          "SignatureAlgorithm has " + values.length + " values: " + Arrays.toString(values));
    }

    @Test
    @DisplayName("Should contain all expected RSA signature algorithms")
    void shouldContainAllExpectedRsaSignatureAlgorithms() {
      Set<String> expected =
          new HashSet<>(
              Arrays.asList(
                  "RSA_PKCS1_SHA256",
                  "RSA_PKCS1_SHA384",
                  "RSA_PKCS1_SHA512",
                  "RSA_PSS_SHA256",
                  "RSA_PSS_SHA384",
                  "RSA_PSS_SHA512"));
      for (String name : expected) {
        assertDoesNotThrow(
            () -> SignatureAlgorithm.valueOf(name), "SignatureAlgorithm should contain " + name);
      }
    }

    @Test
    @DisplayName("Should contain all expected ECDSA algorithms")
    void shouldContainAllExpectedEcdsaAlgorithms() {
      Set<String> expected =
          new HashSet<>(
              Arrays.asList(
                  "ECDSA_P256_SHA256",
                  "ECDSA_P384_SHA384",
                  "ECDSA_P521_SHA512",
                  "ECDSA_SECP256K1_SHA256"));
      for (String name : expected) {
        assertDoesNotThrow(
            () -> SignatureAlgorithm.valueOf(name), "SignatureAlgorithm should contain " + name);
      }
    }

    @Test
    @DisplayName("Should contain Edwards curve algorithms")
    void shouldContainEdwardsCurveAlgorithms() {
      assertDoesNotThrow(
          () -> SignatureAlgorithm.valueOf("ED25519"), "SignatureAlgorithm should contain ED25519");
      assertDoesNotThrow(
          () -> SignatureAlgorithm.valueOf("ED448"), "SignatureAlgorithm should contain ED448");
    }

    @Test
    @DisplayName("Should have correct key sizes for Ed algorithms")
    void shouldHaveCorrectKeySizesForEdAlgorithms() {
      assertEquals(256, SignatureAlgorithm.ED25519.getKeySize());
      assertEquals(448, SignatureAlgorithm.ED448.getKeySize());
    }
  }

  // ==================== MacAlgorithm Tests ====================

  @Nested
  @DisplayName("MacAlgorithm Enum Tests")
  class MacAlgorithmTests {

    @Test
    @DisplayName("Should have exactly 10 MAC algorithms")
    void shouldHaveExactly10MacAlgorithms() {
      MacAlgorithm[] values = MacAlgorithm.values();
      assertEquals(10, values.length, "MacAlgorithm should have exactly 10 values");
      LOGGER.info("MacAlgorithm has " + values.length + " values: " + Arrays.toString(values));
    }

    @Test
    @DisplayName("Should contain all expected HMAC algorithms")
    void shouldContainAllExpectedHmacAlgorithms() {
      Set<String> expected =
          new HashSet<>(
              Arrays.asList("HMAC_SHA256", "HMAC_SHA384", "HMAC_SHA512", "HMAC_SHA3_256"));
      for (String name : expected) {
        assertDoesNotThrow(() -> MacAlgorithm.valueOf(name), "MacAlgorithm should contain " + name);
      }
    }

    @Test
    @DisplayName("Should contain other MAC algorithms")
    void shouldContainOtherMacAlgorithms() {
      Set<String> expected =
          new HashSet<>(
              Arrays.asList(
                  "POLY1305", "BLAKE2B_MAC", "BLAKE2S_MAC", "AES_CMAC", "KMAC128", "KMAC256"));
      for (String name : expected) {
        assertDoesNotThrow(() -> MacAlgorithm.valueOf(name), "MacAlgorithm should contain " + name);
      }
    }

    @Test
    @DisplayName("Should have correct output sizes")
    void shouldHaveCorrectOutputSizes() {
      assertEquals(256, MacAlgorithm.HMAC_SHA256.getOutputSize());
      assertEquals(384, MacAlgorithm.HMAC_SHA384.getOutputSize());
      assertEquals(512, MacAlgorithm.HMAC_SHA512.getOutputSize());
      assertEquals(256, MacAlgorithm.HMAC_SHA3_256.getOutputSize());
      assertEquals(128, MacAlgorithm.POLY1305.getOutputSize());
      assertEquals(512, MacAlgorithm.BLAKE2B_MAC.getOutputSize());
      assertEquals(256, MacAlgorithm.BLAKE2S_MAC.getOutputSize());
      assertEquals(128, MacAlgorithm.AES_CMAC.getOutputSize());
      assertEquals(256, MacAlgorithm.KMAC128.getOutputSize());
      assertEquals(512, MacAlgorithm.KMAC256.getOutputSize());
    }
  }

  // ==================== KeyDerivationAlgorithm Tests ====================

  @Nested
  @DisplayName("KeyDerivationAlgorithm Enum Tests")
  class KeyDerivationAlgorithmTests {

    @Test
    @DisplayName("Should have exactly 10 KDF algorithms")
    void shouldHaveExactly10KdfAlgorithms() {
      KeyDerivationAlgorithm[] values = KeyDerivationAlgorithm.values();
      assertEquals(10, values.length, "KeyDerivationAlgorithm should have exactly 10 values");
      LOGGER.info(
          "KeyDerivationAlgorithm has " + values.length + " values: " + Arrays.toString(values));
    }

    @Test
    @DisplayName("Should contain all expected HKDF algorithms")
    void shouldContainAllExpectedHkdfAlgorithms() {
      Set<String> expected =
          new HashSet<>(Arrays.asList("HKDF_SHA256", "HKDF_SHA384", "HKDF_SHA512"));
      for (String name : expected) {
        assertDoesNotThrow(
            () -> KeyDerivationAlgorithm.valueOf(name),
            "KeyDerivationAlgorithm should contain " + name);
      }
    }

    @Test
    @DisplayName("Should contain all expected PBKDF2 algorithms")
    void shouldContainAllExpectedPbkdf2Algorithms() {
      Set<String> expected =
          new HashSet<>(
              Arrays.asList("PBKDF2_HMAC_SHA256", "PBKDF2_HMAC_SHA384", "PBKDF2_HMAC_SHA512"));
      for (String name : expected) {
        assertDoesNotThrow(
            () -> KeyDerivationAlgorithm.valueOf(name),
            "KeyDerivationAlgorithm should contain " + name);
      }
    }

    @Test
    @DisplayName("Should contain password hashing algorithms")
    void shouldContainPasswordHashingAlgorithms() {
      Set<String> expected =
          new HashSet<>(Arrays.asList("ARGON2ID", "ARGON2I", "SCRYPT", "BCRYPT"));
      for (String name : expected) {
        assertDoesNotThrow(
            () -> KeyDerivationAlgorithm.valueOf(name),
            "KeyDerivationAlgorithm should contain " + name);
      }
    }

    @Test
    @DisplayName("Should have correct algorithm names")
    void shouldHaveCorrectAlgorithmNames() {
      assertEquals("HKDF-SHA256", KeyDerivationAlgorithm.HKDF_SHA256.getAlgorithmName());
      assertEquals("Argon2id", KeyDerivationAlgorithm.ARGON2ID.getAlgorithmName());
      assertEquals("scrypt", KeyDerivationAlgorithm.SCRYPT.getAlgorithmName());
      assertEquals("bcrypt", KeyDerivationAlgorithm.BCRYPT.getAlgorithmName());
    }
  }

  // ==================== EncryptionMode Tests ====================

  @Nested
  @DisplayName("EncryptionMode Enum Tests")
  class EncryptionModeTests {

    @Test
    @DisplayName("Should have exactly 8 encryption modes")
    void shouldHaveExactly8EncryptionModes() {
      EncryptionMode[] values = EncryptionMode.values();
      assertEquals(8, values.length, "EncryptionMode should have exactly 8 values");
      LOGGER.info("EncryptionMode has " + values.length + " values: " + Arrays.toString(values));
    }

    @Test
    @DisplayName("Should contain all expected encryption modes")
    void shouldContainAllExpectedEncryptionModes() {
      Set<String> expected =
          new HashSet<>(Arrays.asList("ECB", "CBC", "CTR", "GCM", "CCM", "OCB", "SIV", "GCM_SIV"));
      for (String name : expected) {
        assertDoesNotThrow(
            () -> EncryptionMode.valueOf(name), "EncryptionMode should contain " + name);
      }
    }

    @Test
    @DisplayName("Should have correct requiresIv values")
    void shouldHaveCorrectRequiresIvValues() {
      assertFalse(EncryptionMode.ECB.requiresIv(), "ECB should not require IV");
      assertTrue(EncryptionMode.CBC.requiresIv(), "CBC should require IV");
      assertTrue(EncryptionMode.CTR.requiresIv(), "CTR should require IV");
      assertTrue(EncryptionMode.GCM.requiresIv(), "GCM should require IV");
      assertTrue(EncryptionMode.CCM.requiresIv(), "CCM should require IV");
      assertTrue(EncryptionMode.OCB.requiresIv(), "OCB should require IV");
      assertTrue(EncryptionMode.SIV.requiresIv(), "SIV should require IV");
      assertTrue(EncryptionMode.GCM_SIV.requiresIv(), "GCM_SIV should require IV");
    }

    @Test
    @DisplayName("Should have correct providesAuthentication values")
    void shouldHaveCorrectProvidesAuthenticationValues() {
      assertFalse(EncryptionMode.ECB.providesAuthentication(), "ECB should not provide auth");
      assertFalse(EncryptionMode.CBC.providesAuthentication(), "CBC should not provide auth");
      assertFalse(EncryptionMode.CTR.providesAuthentication(), "CTR should not provide auth");
      assertTrue(EncryptionMode.GCM.providesAuthentication(), "GCM should provide auth");
      assertTrue(EncryptionMode.CCM.providesAuthentication(), "CCM should provide auth");
      assertTrue(EncryptionMode.OCB.providesAuthentication(), "OCB should provide auth");
      assertTrue(EncryptionMode.SIV.providesAuthentication(), "SIV should provide auth");
      assertTrue(EncryptionMode.GCM_SIV.providesAuthentication(), "GCM_SIV should provide auth");
    }

    @Test
    @DisplayName("Should have correct mode names")
    void shouldHaveCorrectModeNames() {
      assertEquals("ECB", EncryptionMode.ECB.getModeName());
      assertEquals("CBC", EncryptionMode.CBC.getModeName());
      assertEquals("CTR", EncryptionMode.CTR.getModeName());
      assertEquals("GCM", EncryptionMode.GCM.getModeName());
      assertEquals("CCM", EncryptionMode.CCM.getModeName());
      assertEquals("OCB", EncryptionMode.OCB.getModeName());
      assertEquals("SIV", EncryptionMode.SIV.getModeName());
      assertEquals("GCM-SIV", EncryptionMode.GCM_SIV.getModeName());
    }
  }

  // ==================== PaddingScheme Tests ====================

  @Nested
  @DisplayName("PaddingScheme Enum Tests")
  class PaddingSchemeTests {

    @Test
    @DisplayName("Should have exactly 7 padding schemes")
    void shouldHaveExactly7PaddingSchemes() {
      PaddingScheme[] values = PaddingScheme.values();
      assertEquals(7, values.length, "PaddingScheme should have exactly 7 values");
      LOGGER.info("PaddingScheme has " + values.length + " values: " + Arrays.toString(values));
    }

    @Test
    @DisplayName("Should contain all expected padding schemes")
    void shouldContainAllExpectedPaddingSchemes() {
      Set<String> expected =
          new HashSet<>(
              Arrays.asList(
                  "NONE",
                  "PKCS1_V15",
                  "OAEP_SHA1",
                  "OAEP_SHA256",
                  "OAEP_SHA384",
                  "OAEP_SHA512",
                  "PSS"));
      for (String name : expected) {
        assertDoesNotThrow(
            () -> PaddingScheme.valueOf(name), "PaddingScheme should contain " + name);
      }
    }

    @Test
    @DisplayName("Should have correct scheme names")
    void shouldHaveCorrectSchemeNames() {
      assertEquals("None", PaddingScheme.NONE.getSchemeName());
      assertEquals("PKCS1-v1.5", PaddingScheme.PKCS1_V15.getSchemeName());
      assertEquals("OAEP-SHA1", PaddingScheme.OAEP_SHA1.getSchemeName());
      assertEquals("OAEP-SHA256", PaddingScheme.OAEP_SHA256.getSchemeName());
      assertEquals("OAEP-SHA384", PaddingScheme.OAEP_SHA384.getSchemeName());
      assertEquals("OAEP-SHA512", PaddingScheme.OAEP_SHA512.getSchemeName());
      assertEquals("PSS", PaddingScheme.PSS.getSchemeName());
    }
  }

  // ==================== CryptoKeyType Tests ====================

  @Nested
  @DisplayName("CryptoKeyType Enum Tests")
  class CryptoKeyTypeTests {

    @Test
    @DisplayName("Should have exactly 4 key types")
    void shouldHaveExactly4KeyTypes() {
      CryptoKeyType[] values = CryptoKeyType.values();
      assertEquals(4, values.length, "CryptoKeyType should have exactly 4 values");
      LOGGER.info("CryptoKeyType has " + values.length + " values: " + Arrays.toString(values));
    }

    @Test
    @DisplayName("Should contain all expected key types")
    void shouldContainAllExpectedKeyTypes() {
      Set<String> expected =
          new HashSet<>(Arrays.asList("SYMMETRIC", "PUBLIC", "PRIVATE", "KEY_PAIR"));
      for (String name : expected) {
        assertDoesNotThrow(
            () -> CryptoKeyType.valueOf(name), "CryptoKeyType should contain " + name);
      }
    }
  }

  // ==================== CryptoErrorCode Tests ====================

  @Nested
  @DisplayName("CryptoErrorCode Enum Tests")
  class CryptoErrorCodeTests {

    @Test
    @DisplayName("Should have exactly 20 error codes")
    void shouldHaveExactly20ErrorCodes() {
      CryptoErrorCode[] values = CryptoErrorCode.values();
      assertEquals(20, values.length, "CryptoErrorCode should have exactly 20 values");
      LOGGER.info("CryptoErrorCode has " + values.length + " values: " + Arrays.toString(values));
    }

    @Test
    @DisplayName("Should contain all expected error codes")
    void shouldContainAllExpectedErrorCodes() {
      Set<String> expected =
          new HashSet<>(
              Arrays.asList(
                  "UNKNOWN",
                  "UNSUPPORTED_ALGORITHM",
                  "UNSUPPORTED_OPERATION",
                  "INVALID_KEY",
                  "KEY_NOT_FOUND",
                  "INVALID_SIGNATURE",
                  "VERIFICATION_FAILED",
                  "INVALID_INPUT",
                  "INVALID_OUTPUT_SIZE",
                  "INVALID_NONCE",
                  "AUTHENTICATION_FAILED",
                  "ENCRYPTION_FAILED",
                  "DECRYPTION_FAILED",
                  "KEY_GENERATION_FAILED",
                  "RNG_FAILED",
                  "HARDWARE_UNAVAILABLE",
                  "NOT_PERMITTED",
                  "RESOURCE_EXHAUSTED",
                  "TIMEOUT",
                  "INTERNAL_ERROR"));
      for (String name : expected) {
        assertDoesNotThrow(
            () -> CryptoErrorCode.valueOf(name), "CryptoErrorCode should contain " + name);
      }
    }
  }

  // ==================== CryptoException Tests ====================

  @Nested
  @DisplayName("CryptoException Class Tests")
  class CryptoExceptionTests {

    @Test
    @DisplayName("Should extend WasmException")
    void shouldExtendWasmException() {
      assertTrue(
          WasmException.class.isAssignableFrom(CryptoException.class),
          "CryptoException should extend WasmException");
    }

    @Test
    @DisplayName("Should have four constructors")
    void shouldHaveFourConstructors() {
      Constructor<?>[] constructors = CryptoException.class.getDeclaredConstructors();
      assertEquals(4, constructors.length, "CryptoException should have 4 constructors");
    }

    @Test
    @DisplayName("Should create exception with message only")
    void shouldCreateExceptionWithMessageOnly() {
      CryptoException ex = new CryptoException("test message");
      assertEquals("test message", ex.getMessage());
      assertEquals(CryptoErrorCode.UNKNOWN, ex.getErrorCode());
      assertNull(ex.getCause());
    }

    @Test
    @DisplayName("Should create exception with message and error code")
    void shouldCreateExceptionWithMessageAndErrorCode() {
      CryptoException ex = new CryptoException("test message", CryptoErrorCode.INVALID_KEY);
      assertEquals("test message", ex.getMessage());
      assertEquals(CryptoErrorCode.INVALID_KEY, ex.getErrorCode());
      assertNull(ex.getCause());
    }

    @Test
    @DisplayName("Should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
      RuntimeException cause = new RuntimeException("cause");
      CryptoException ex = new CryptoException("test message", cause);
      assertEquals("test message", ex.getMessage());
      assertEquals(CryptoErrorCode.UNKNOWN, ex.getErrorCode());
      assertEquals(cause, ex.getCause());
    }

    @Test
    @DisplayName("Should create exception with message, error code, and cause")
    void shouldCreateExceptionWithMessageErrorCodeAndCause() {
      RuntimeException cause = new RuntimeException("cause");
      CryptoException ex =
          new CryptoException("test message", CryptoErrorCode.ENCRYPTION_FAILED, cause);
      assertEquals("test message", ex.getMessage());
      assertEquals(CryptoErrorCode.ENCRYPTION_FAILED, ex.getErrorCode());
      assertEquals(cause, ex.getCause());
    }

    @Test
    @DisplayName("Should have getErrorCode method")
    void shouldHaveGetErrorCodeMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = CryptoException.class.getDeclaredMethod("getErrorCode");
            assertEquals(CryptoErrorCode.class, method.getReturnType());
            assertTrue(Modifier.isPublic(method.getModifiers()));
          });
    }

    @Test
    @DisplayName("Should have serialVersionUID")
    void shouldHaveSerialVersionUid() {
      assertDoesNotThrow(
          () -> {
            java.lang.reflect.Field field =
                CryptoException.class.getDeclaredField("serialVersionUID");
            assertTrue(Modifier.isPrivate(field.getModifiers()));
            assertTrue(Modifier.isStatic(field.getModifiers()));
            assertTrue(Modifier.isFinal(field.getModifiers()));
          });
    }
  }

  // ==================== CryptoKey Interface Tests ====================

  @Nested
  @DisplayName("CryptoKey Interface Tests")
  class CryptoKeyInterfaceTests {

    @Test
    @DisplayName("Should be an interface")
    void shouldBeAnInterface() {
      assertTrue(CryptoKey.class.isInterface(), "CryptoKey should be an interface");
    }

    @Test
    @DisplayName("Should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(CryptoKey.class),
          "CryptoKey should extend AutoCloseable");
    }

    @Test
    @DisplayName("Should have getId method")
    void shouldHaveGetIdMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = CryptoKey.class.getDeclaredMethod("getId");
            assertEquals(String.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have getKeyType method")
    void shouldHaveGetKeyTypeMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = CryptoKey.class.getDeclaredMethod("getKeyType");
            assertEquals(CryptoKeyType.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have getAlgorithm method")
    void shouldHaveGetAlgorithmMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = CryptoKey.class.getDeclaredMethod("getAlgorithm");
            assertEquals(String.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have getKeySizeBits method")
    void shouldHaveGetKeySizeBitsMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = CryptoKey.class.getDeclaredMethod("getKeySizeBits");
            assertEquals(int.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have isExportable method")
    void shouldHaveIsExportableMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = CryptoKey.class.getDeclaredMethod("isExportable");
            assertEquals(boolean.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have exportPublicKey method")
    void shouldHaveExportPublicKeyMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = CryptoKey.class.getDeclaredMethod("exportPublicKey");
            assertEquals(Optional.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have extractPublicKey method")
    void shouldHaveExtractPublicKeyMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = CryptoKey.class.getDeclaredMethod("extractPublicKey");
            assertEquals(CryptoKey.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have isValid method")
    void shouldHaveIsValidMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = CryptoKey.class.getDeclaredMethod("isValid");
            assertEquals(boolean.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have close method from AutoCloseable")
    void shouldHaveCloseMethod() {
      assertDoesNotThrow(() -> CryptoKey.class.getDeclaredMethod("close"));
    }
  }

  // ==================== WasiCrypto Interface Tests ====================

  @Nested
  @DisplayName("WasiCrypto Interface Tests")
  class WasiCryptoInterfaceTests {

    @Test
    @DisplayName("Should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiCrypto.class.isInterface(), "WasiCrypto should be an interface");
    }

    @Test
    @DisplayName("Should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(WasiCrypto.class),
          "WasiCrypto should extend AutoCloseable");
    }

    @Test
    @DisplayName("Should have generateSymmetricKey method")
    void shouldHaveGenerateSymmetricKeyMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                WasiCrypto.class.getDeclaredMethod(
                    "generateSymmetricKey", SymmetricAlgorithm.class);
            assertEquals(CryptoKey.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have generateKeyPair method")
    void shouldHaveGenerateKeyPairMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                WasiCrypto.class.getDeclaredMethod("generateKeyPair", AsymmetricAlgorithm.class);
            assertEquals(CryptoKey.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have generateSignatureKeyPair method")
    void shouldHaveGenerateSignatureKeyPairMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                WasiCrypto.class.getDeclaredMethod(
                    "generateSignatureKeyPair", SignatureAlgorithm.class);
            assertEquals(CryptoKey.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have importSymmetricKey method")
    void shouldHaveImportSymmetricKeyMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                WasiCrypto.class.getDeclaredMethod(
                    "importSymmetricKey", SymmetricAlgorithm.class, byte[].class);
            assertEquals(CryptoKey.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have importPublicKey method")
    void shouldHaveImportPublicKeyMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                WasiCrypto.class.getDeclaredMethod(
                    "importPublicKey", AsymmetricAlgorithm.class, byte[].class);
            assertEquals(CryptoKey.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have importPrivateKey method")
    void shouldHaveImportPrivateKeyMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                WasiCrypto.class.getDeclaredMethod(
                    "importPrivateKey", AsymmetricAlgorithm.class, byte[].class);
            assertEquals(CryptoKey.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have symmetricEncrypt method")
    void shouldHaveSymmetricEncryptMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                WasiCrypto.class.getDeclaredMethod(
                    "symmetricEncrypt",
                    byte[].class,
                    SymmetricAlgorithm.class,
                    CryptoKey.class,
                    EncryptionOptions.class);
            assertEquals(byte[].class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have symmetricDecrypt method")
    void shouldHaveSymmetricDecryptMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                WasiCrypto.class.getDeclaredMethod(
                    "symmetricDecrypt",
                    byte[].class,
                    SymmetricAlgorithm.class,
                    CryptoKey.class,
                    EncryptionOptions.class);
            assertEquals(byte[].class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have asymmetricEncrypt method")
    void shouldHaveAsymmetricEncryptMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                WasiCrypto.class.getDeclaredMethod(
                    "asymmetricEncrypt",
                    byte[].class,
                    AsymmetricAlgorithm.class,
                    CryptoKey.class,
                    PaddingScheme.class);
            assertEquals(byte[].class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have asymmetricDecrypt method")
    void shouldHaveAsymmetricDecryptMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                WasiCrypto.class.getDeclaredMethod(
                    "asymmetricDecrypt",
                    byte[].class,
                    AsymmetricAlgorithm.class,
                    CryptoKey.class,
                    PaddingScheme.class);
            assertEquals(byte[].class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have sign method")
    void shouldHaveSignMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                WasiCrypto.class.getDeclaredMethod(
                    "sign",
                    byte[].class,
                    SignatureAlgorithm.class,
                    CryptoKey.class,
                    SignatureOptions.class);
            assertEquals(byte[].class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have verify method")
    void shouldHaveVerifyMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                WasiCrypto.class.getDeclaredMethod(
                    "verify",
                    byte[].class,
                    byte[].class,
                    SignatureAlgorithm.class,
                    CryptoKey.class,
                    SignatureOptions.class);
            assertEquals(boolean.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have hash method")
    void shouldHaveHashMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                WasiCrypto.class.getDeclaredMethod("hash", byte[].class, HashAlgorithm.class);
            assertEquals(byte[].class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have mac method")
    void shouldHaveMacMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                WasiCrypto.class.getDeclaredMethod(
                    "mac", byte[].class, MacAlgorithm.class, CryptoKey.class);
            assertEquals(byte[].class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have verifyMac method")
    void shouldHaveVerifyMacMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                WasiCrypto.class.getDeclaredMethod(
                    "verifyMac", byte[].class, byte[].class, MacAlgorithm.class, CryptoKey.class);
            assertEquals(boolean.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have deriveKey method")
    void shouldHaveDeriveKeyMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                WasiCrypto.class.getDeclaredMethod(
                    "deriveKey",
                    byte[].class,
                    KeyDerivationAlgorithm.class,
                    byte[].class,
                    byte[].class,
                    int.class);
            assertEquals(byte[].class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have keyAgreement method")
    void shouldHaveKeyAgreementMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                WasiCrypto.class.getDeclaredMethod(
                    "keyAgreement", AsymmetricAlgorithm.class, CryptoKey.class, CryptoKey.class);
            assertEquals(byte[].class, method.getReturnType());
          });
    }
  }

  // ==================== EncryptionOptions Tests ====================

  @Nested
  @DisplayName("EncryptionOptions Class Tests")
  class EncryptionOptionsTests {

    @Test
    @DisplayName("Should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(EncryptionOptions.class.getModifiers()),
          "EncryptionOptions should be final");
    }

    @Test
    @DisplayName("Should have builder method")
    void shouldHaveBuilderMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                EncryptionOptions.class.getDeclaredMethod("builder", EncryptionMode.class);
            assertTrue(Modifier.isStatic(method.getModifiers()));
            assertTrue(Modifier.isPublic(method.getModifiers()));
            assertEquals(EncryptionOptions.Builder.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have getMode method")
    void shouldHaveGetModeMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = EncryptionOptions.class.getDeclaredMethod("getMode");
            assertEquals(EncryptionMode.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have getIv method")
    void shouldHaveGetIvMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = EncryptionOptions.class.getDeclaredMethod("getIv");
            assertEquals(Optional.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have getAdditionalData method")
    void shouldHaveGetAdditionalDataMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = EncryptionOptions.class.getDeclaredMethod("getAdditionalData");
            assertEquals(Optional.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have useHardwareAcceleration method")
    void shouldHaveUseHardwareAccelerationMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = EncryptionOptions.class.getDeclaredMethod("useHardwareAcceleration");
            assertEquals(boolean.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should create options using builder")
    void shouldCreateOptionsUsingBuilder() {
      byte[] iv = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
      byte[] aad = new byte[] {1, 2, 3, 4};

      EncryptionOptions options =
          EncryptionOptions.builder(EncryptionMode.GCM)
              .iv(iv)
              .additionalData(aad)
              .useHardwareAcceleration(true)
              .build();

      assertEquals(EncryptionMode.GCM, options.getMode());
      assertTrue(options.getIv().isPresent());
      assertArrayEquals(iv, options.getIv().get());
      assertTrue(options.getAdditionalData().isPresent());
      assertArrayEquals(aad, options.getAdditionalData().get());
      assertTrue(options.useHardwareAcceleration());
    }

    @Test
    @DisplayName("Should create options without optional fields")
    void shouldCreateOptionsWithoutOptionalFields() {
      EncryptionOptions options = EncryptionOptions.builder(EncryptionMode.ECB).build();

      assertEquals(EncryptionMode.ECB, options.getMode());
      assertFalse(options.getIv().isPresent());
      assertFalse(options.getAdditionalData().isPresent());
      assertTrue(options.useHardwareAcceleration()); // Default is true
    }

    @Test
    @DisplayName("Should have inner Builder class")
    void shouldHaveInnerBuilderClass() {
      Class<?>[] innerClasses = EncryptionOptions.class.getDeclaredClasses();
      boolean hasBuilder = false;
      for (Class<?> inner : innerClasses) {
        if (inner.getSimpleName().equals("Builder")) {
          hasBuilder = true;
          assertTrue(Modifier.isFinal(inner.getModifiers()), "Builder should be final");
          assertTrue(Modifier.isStatic(inner.getModifiers()), "Builder should be static");
          break;
        }
      }
      assertTrue(hasBuilder, "EncryptionOptions should have Builder inner class");
    }

    @Test
    @DisplayName("Builder should have iv method")
    void builderShouldHaveIvMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = EncryptionOptions.Builder.class.getDeclaredMethod("iv", byte[].class);
            assertEquals(EncryptionOptions.Builder.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Builder should have additionalData method")
    void builderShouldHaveAdditionalDataMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                EncryptionOptions.Builder.class.getDeclaredMethod("additionalData", byte[].class);
            assertEquals(EncryptionOptions.Builder.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Builder should have useHardwareAcceleration method")
    void builderShouldHaveUseHardwareAccelerationMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                EncryptionOptions.Builder.class.getDeclaredMethod(
                    "useHardwareAcceleration", boolean.class);
            assertEquals(EncryptionOptions.Builder.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Builder should have build method")
    void builderShouldHaveBuildMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = EncryptionOptions.Builder.class.getDeclaredMethod("build");
            assertEquals(EncryptionOptions.class, method.getReturnType());
          });
    }
  }

  // ==================== SignatureOptions Tests ====================

  @Nested
  @DisplayName("SignatureOptions Class Tests")
  class SignatureOptionsTests {

    @Test
    @DisplayName("Should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(SignatureOptions.class.getModifiers()),
          "SignatureOptions should be final");
    }

    @Test
    @DisplayName("Should have builder method")
    void shouldHaveBuilderMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = SignatureOptions.class.getDeclaredMethod("builder");
            assertTrue(Modifier.isStatic(method.getModifiers()));
            assertTrue(Modifier.isPublic(method.getModifiers()));
            assertEquals(SignatureOptions.Builder.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have defaults method")
    void shouldHaveDefaultsMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = SignatureOptions.class.getDeclaredMethod("defaults");
            assertTrue(Modifier.isStatic(method.getModifiers()));
            assertTrue(Modifier.isPublic(method.getModifiers()));
            assertEquals(SignatureOptions.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have isPrehashed method")
    void shouldHaveIsPrehashed() {
      assertDoesNotThrow(
          () -> {
            Method method = SignatureOptions.class.getDeclaredMethod("isPrehashed");
            assertEquals(boolean.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have getPrehashAlgorithm method")
    void shouldHaveGetPrehashAlgorithm() {
      assertDoesNotThrow(
          () -> {
            Method method = SignatureOptions.class.getDeclaredMethod("getPrehashAlgorithm");
            assertEquals(HashAlgorithm.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have useHardwareAcceleration method")
    void shouldHaveUseHardwareAccelerationMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = SignatureOptions.class.getDeclaredMethod("useHardwareAcceleration");
            assertEquals(boolean.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should have useDeterministicSignatures method")
    void shouldHaveUseDeterministicSignaturesMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = SignatureOptions.class.getDeclaredMethod("useDeterministicSignatures");
            assertEquals(boolean.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Should create default options")
    void shouldCreateDefaultOptions() {
      SignatureOptions options = SignatureOptions.defaults();

      assertFalse(options.isPrehashed());
      assertNull(options.getPrehashAlgorithm());
      assertTrue(options.useHardwareAcceleration()); // Default is true
      assertTrue(options.useDeterministicSignatures()); // Default is true
    }

    @Test
    @DisplayName("Should create prehashed options")
    void shouldCreatePrehashedOptions() {
      SignatureOptions options =
          SignatureOptions.builder()
              .prehashed(HashAlgorithm.SHA_256)
              .useHardwareAcceleration(false)
              .deterministicSignatures(false)
              .build();

      assertTrue(options.isPrehashed());
      assertEquals(HashAlgorithm.SHA_256, options.getPrehashAlgorithm());
      assertFalse(options.useHardwareAcceleration());
      assertFalse(options.useDeterministicSignatures());
    }

    @Test
    @DisplayName("Should have inner Builder class")
    void shouldHaveInnerBuilderClass() {
      Class<?>[] innerClasses = SignatureOptions.class.getDeclaredClasses();
      boolean hasBuilder = false;
      for (Class<?> inner : innerClasses) {
        if (inner.getSimpleName().equals("Builder")) {
          hasBuilder = true;
          assertTrue(Modifier.isFinal(inner.getModifiers()), "Builder should be final");
          assertTrue(Modifier.isStatic(inner.getModifiers()), "Builder should be static");
          break;
        }
      }
      assertTrue(hasBuilder, "SignatureOptions should have Builder inner class");
    }

    @Test
    @DisplayName("Builder should have prehashed method")
    void builderShouldHavePrehashedMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                SignatureOptions.Builder.class.getDeclaredMethod("prehashed", HashAlgorithm.class);
            assertEquals(SignatureOptions.Builder.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Builder should have useHardwareAcceleration method")
    void builderShouldHaveUseHardwareAccelerationMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                SignatureOptions.Builder.class.getDeclaredMethod(
                    "useHardwareAcceleration", boolean.class);
            assertEquals(SignatureOptions.Builder.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Builder should have deterministicSignatures method")
    void builderShouldHaveDeterministicSignaturesMethod() {
      assertDoesNotThrow(
          () -> {
            Method method =
                SignatureOptions.Builder.class.getDeclaredMethod(
                    "deterministicSignatures", boolean.class);
            assertEquals(SignatureOptions.Builder.class, method.getReturnType());
          });
    }

    @Test
    @DisplayName("Builder should have build method")
    void builderShouldHaveBuildMethod() {
      assertDoesNotThrow(
          () -> {
            Method method = SignatureOptions.Builder.class.getDeclaredMethod("build");
            assertEquals(SignatureOptions.class, method.getReturnType());
          });
    }
  }

  // ==================== Method Counting Tests ====================

  @Nested
  @DisplayName("API Method Counting Tests")
  class MethodCountingTests {

    @Test
    @DisplayName("WasiCrypto interface should have expected number of methods")
    void wasiCryptoShouldHaveExpectedNumberOfMethods() {
      Method[] methods = WasiCrypto.class.getDeclaredMethods();
      // Expected: 18 crypto methods + close from AutoCloseable
      int expectedMethods = 18;
      assertTrue(
          methods.length >= expectedMethods,
          "WasiCrypto should have at least "
              + expectedMethods
              + " methods, found: "
              + methods.length);
      LOGGER.info("WasiCrypto has " + methods.length + " declared methods");
    }

    @Test
    @DisplayName("CryptoKey interface should have expected number of methods")
    void cryptoKeyShouldHaveExpectedNumberOfMethods() {
      Method[] methods = CryptoKey.class.getDeclaredMethods();
      // Expected: getId, getKeyType, getAlgorithm, getKeySizeBits, isExportable,
      // exportPublicKey, extractPublicKey, isValid, close
      int expectedMethods = 9;
      assertTrue(
          methods.length >= expectedMethods,
          "CryptoKey should have at least "
              + expectedMethods
              + " methods, found: "
              + methods.length);
      LOGGER.info("CryptoKey has " + methods.length + " declared methods");
    }
  }

  // ==================== Package Consistency Tests ====================

  @Nested
  @DisplayName("Package Consistency Tests")
  class PackageConsistencyTests {

    @Test
    @DisplayName("All crypto classes should be in the same package")
    void allCryptoClassesShouldBeInSamePackage() {
      String expectedPackage = "ai.tegmentum.wasmtime4j.wasi.crypto";
      assertEquals(expectedPackage, WasiCrypto.class.getPackage().getName());
      assertEquals(expectedPackage, CryptoKey.class.getPackage().getName());
      assertEquals(expectedPackage, CryptoException.class.getPackage().getName());
      assertEquals(expectedPackage, HashAlgorithm.class.getPackage().getName());
      assertEquals(expectedPackage, SymmetricAlgorithm.class.getPackage().getName());
      assertEquals(expectedPackage, AsymmetricAlgorithm.class.getPackage().getName());
      assertEquals(expectedPackage, SignatureAlgorithm.class.getPackage().getName());
      assertEquals(expectedPackage, MacAlgorithm.class.getPackage().getName());
      assertEquals(expectedPackage, KeyDerivationAlgorithm.class.getPackage().getName());
      assertEquals(expectedPackage, EncryptionMode.class.getPackage().getName());
      assertEquals(expectedPackage, PaddingScheme.class.getPackage().getName());
      assertEquals(expectedPackage, CryptoKeyType.class.getPackage().getName());
      assertEquals(expectedPackage, CryptoErrorCode.class.getPackage().getName());
      assertEquals(expectedPackage, EncryptionOptions.class.getPackage().getName());
      assertEquals(expectedPackage, SignatureOptions.class.getPackage().getName());
    }

    @Test
    @DisplayName("All enums should have consistent getAlgorithmName pattern")
    void allEnumsShouldHaveConsistentNamingPattern() {
      // Hash, Symmetric, Asymmetric, Signature, Mac all have getAlgorithmName
      // KeyDerivation also has getAlgorithmName
      assertDoesNotThrow(() -> HashAlgorithm.class.getDeclaredMethod("getAlgorithmName"));
      assertDoesNotThrow(() -> SymmetricAlgorithm.class.getDeclaredMethod("getAlgorithmName"));
      assertDoesNotThrow(() -> AsymmetricAlgorithm.class.getDeclaredMethod("getAlgorithmName"));
      assertDoesNotThrow(() -> SignatureAlgorithm.class.getDeclaredMethod("getAlgorithmName"));
      assertDoesNotThrow(() -> MacAlgorithm.class.getDeclaredMethod("getAlgorithmName"));
      assertDoesNotThrow(() -> KeyDerivationAlgorithm.class.getDeclaredMethod("getAlgorithmName"));
    }

    @Test
    @DisplayName("EncryptionMode and PaddingScheme should have different naming patterns")
    void encryptionModeAndPaddingSchemeShouldHaveDifferentNamingPatterns() {
      assertDoesNotThrow(() -> EncryptionMode.class.getDeclaredMethod("getModeName"));
      assertDoesNotThrow(() -> PaddingScheme.class.getDeclaredMethod("getSchemeName"));
    }
  }

  // ==================== Exception Throwing Tests ====================

  @Nested
  @DisplayName("Exception Throwing Tests")
  class ExceptionThrowingTests {

    @Test
    @DisplayName("WasiCrypto methods should declare CryptoException")
    void wasiCryptoMethodsShouldDeclareCryptoException() {
      Method[] methods = WasiCrypto.class.getDeclaredMethods();
      for (Method method : methods) {
        if (!method.getName().equals("close")) {
          Class<?>[] exceptionTypes = method.getExceptionTypes();
          boolean declaresCryptoException = false;
          for (Class<?> exType : exceptionTypes) {
            if (CryptoException.class.isAssignableFrom(exType)) {
              declaresCryptoException = true;
              break;
            }
          }
          assertTrue(
              declaresCryptoException,
              "Method " + method.getName() + " should declare CryptoException");
        }
      }
    }

    @Test
    @DisplayName("CryptoKey exportPublicKey should declare CryptoException")
    void cryptoKeyExportPublicKeyShouldDeclareCryptoException() {
      assertDoesNotThrow(
          () -> {
            Method method = CryptoKey.class.getDeclaredMethod("exportPublicKey");
            Class<?>[] exceptionTypes = method.getExceptionTypes();
            boolean declaresCryptoException = false;
            for (Class<?> exType : exceptionTypes) {
              if (CryptoException.class.isAssignableFrom(exType)) {
                declaresCryptoException = true;
                break;
              }
            }
            assertTrue(declaresCryptoException, "exportPublicKey should declare CryptoException");
          });
    }

    @Test
    @DisplayName("CryptoKey extractPublicKey should declare CryptoException")
    void cryptoKeyExtractPublicKeyShouldDeclareCryptoException() {
      assertDoesNotThrow(
          () -> {
            Method method = CryptoKey.class.getDeclaredMethod("extractPublicKey");
            Class<?>[] exceptionTypes = method.getExceptionTypes();
            boolean declaresCryptoException = false;
            for (Class<?> exType : exceptionTypes) {
              if (CryptoException.class.isAssignableFrom(exType)) {
                declaresCryptoException = true;
                break;
              }
            }
            assertTrue(declaresCryptoException, "extractPublicKey should declare CryptoException");
          });
    }
  }
}
