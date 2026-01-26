/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.wasi.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for WASI crypto package classes.
 *
 * <p>This test class validates the crypto-related enums, builders, and value classes.
 */
@DisplayName("WASI Crypto Integration Tests")
public class WasiCryptoIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(WasiCryptoIntegrationTest.class.getName());

  @BeforeAll
  static void setUpClass() {
    LOGGER.info("Starting WASI Crypto Integration Tests");
  }

  @Nested
  @DisplayName("SymmetricAlgorithm Tests")
  class SymmetricAlgorithmTests {

    @Test
    @DisplayName("Should have all expected symmetric algorithms")
    void shouldHaveAllExpectedSymmetricAlgorithms() {
      LOGGER.info("Testing SymmetricAlgorithm enum values");

      SymmetricAlgorithm[] algorithms = SymmetricAlgorithm.values();

      assertEquals(7, algorithms.length, "Should have 7 symmetric algorithms");

      assertNotNull(SymmetricAlgorithm.AES_128, "AES_128 should exist");
      assertNotNull(SymmetricAlgorithm.AES_192, "AES_192 should exist");
      assertNotNull(SymmetricAlgorithm.AES_256, "AES_256 should exist");
      assertNotNull(SymmetricAlgorithm.CHACHA20, "CHACHA20 should exist");
      assertNotNull(SymmetricAlgorithm.XCHACHA20, "XCHACHA20 should exist");
      assertNotNull(SymmetricAlgorithm.CHACHA20_POLY1305, "CHACHA20_POLY1305 should exist");
      assertNotNull(SymmetricAlgorithm.XCHACHA20_POLY1305, "XCHACHA20_POLY1305 should exist");

      LOGGER.info("All symmetric algorithms verified");
    }

    @Test
    @DisplayName("Should have correct key sizes")
    void shouldHaveCorrectKeySizes() {
      LOGGER.info("Testing symmetric algorithm key sizes");

      assertEquals(128, SymmetricAlgorithm.AES_128.getKeySize(), "AES-128 should have 128-bit key");
      assertEquals(192, SymmetricAlgorithm.AES_192.getKeySize(), "AES-192 should have 192-bit key");
      assertEquals(256, SymmetricAlgorithm.AES_256.getKeySize(), "AES-256 should have 256-bit key");
      assertEquals(
          256, SymmetricAlgorithm.CHACHA20.getKeySize(), "ChaCha20 should have 256-bit key");
      assertEquals(
          256, SymmetricAlgorithm.XCHACHA20.getKeySize(), "XChaCha20 should have 256-bit key");

      LOGGER.info("Key sizes verified");
    }

    @Test
    @DisplayName("Should have correct algorithm names")
    void shouldHaveCorrectAlgorithmNames() {
      LOGGER.info("Testing symmetric algorithm names");

      assertEquals("AES-128", SymmetricAlgorithm.AES_128.getAlgorithmName());
      assertEquals("AES-192", SymmetricAlgorithm.AES_192.getAlgorithmName());
      assertEquals("AES-256", SymmetricAlgorithm.AES_256.getAlgorithmName());
      assertEquals("ChaCha20", SymmetricAlgorithm.CHACHA20.getAlgorithmName());
      assertEquals("XChaCha20", SymmetricAlgorithm.XCHACHA20.getAlgorithmName());
      assertEquals("ChaCha20-Poly1305", SymmetricAlgorithm.CHACHA20_POLY1305.getAlgorithmName());
      assertEquals("XChaCha20-Poly1305", SymmetricAlgorithm.XCHACHA20_POLY1305.getAlgorithmName());

      LOGGER.info("Algorithm names verified");
    }

    @Test
    @DisplayName("Should parse from string using valueOf")
    void shouldParseFromStringUsingValueOf() {
      LOGGER.info("Testing valueOf parsing");

      assertEquals(SymmetricAlgorithm.AES_128, SymmetricAlgorithm.valueOf("AES_128"));
      assertEquals(
          SymmetricAlgorithm.CHACHA20_POLY1305, SymmetricAlgorithm.valueOf("CHACHA20_POLY1305"));

      LOGGER.info("valueOf parsing verified");
    }
  }

  @Nested
  @DisplayName("HashAlgorithm Tests")
  class HashAlgorithmTests {

    @Test
    @DisplayName("Should have all expected hash algorithms")
    void shouldHaveAllExpectedHashAlgorithms() {
      LOGGER.info("Testing HashAlgorithm enum values");

      HashAlgorithm[] algorithms = HashAlgorithm.values();

      assertTrue(algorithms.length >= 4, "Should have at least 4 hash algorithms");

      assertNotNull(HashAlgorithm.SHA_256, "SHA_256 should exist");
      assertNotNull(HashAlgorithm.SHA_384, "SHA_384 should exist");
      assertNotNull(HashAlgorithm.SHA_512, "SHA_512 should exist");

      LOGGER.info("Hash algorithms verified: " + algorithms.length + " algorithms");
    }

    @Test
    @DisplayName("Should have correct output sizes")
    void shouldHaveCorrectOutputSizes() {
      LOGGER.info("Testing hash algorithm output sizes");

      assertEquals(256, HashAlgorithm.SHA_256.getOutputSize(), "SHA-256 should output 256 bits");
      assertEquals(384, HashAlgorithm.SHA_384.getOutputSize(), "SHA-384 should output 384 bits");
      assertEquals(512, HashAlgorithm.SHA_512.getOutputSize(), "SHA-512 should output 512 bits");

      LOGGER.info("Output sizes verified");
    }
  }

  @Nested
  @DisplayName("SignatureAlgorithm Tests")
  class SignatureAlgorithmTests {

    @Test
    @DisplayName("Should have all expected signature algorithms")
    void shouldHaveAllExpectedSignatureAlgorithms() {
      LOGGER.info("Testing SignatureAlgorithm enum values");

      SignatureAlgorithm[] algorithms = SignatureAlgorithm.values();

      assertTrue(algorithms.length >= 3, "Should have at least 3 signature algorithms");

      LOGGER.info("Signature algorithms verified: " + algorithms.length + " algorithms");
    }
  }

  @Nested
  @DisplayName("AsymmetricAlgorithm Tests")
  class AsymmetricAlgorithmTests {

    @Test
    @DisplayName("Should have all expected asymmetric algorithms")
    void shouldHaveAllExpectedAsymmetricAlgorithms() {
      LOGGER.info("Testing AsymmetricAlgorithm enum values");

      AsymmetricAlgorithm[] algorithms = AsymmetricAlgorithm.values();

      assertTrue(algorithms.length >= 1, "Should have at least 1 asymmetric algorithm");

      LOGGER.info("Asymmetric algorithms verified: " + algorithms.length + " algorithms");
    }
  }

  @Nested
  @DisplayName("EncryptionMode Tests")
  class EncryptionModeTests {

    @Test
    @DisplayName("Should have all expected encryption modes")
    void shouldHaveAllExpectedEncryptionModes() {
      LOGGER.info("Testing EncryptionMode enum values");

      EncryptionMode[] modes = EncryptionMode.values();

      assertTrue(modes.length >= 1, "Should have at least 1 encryption mode");

      LOGGER.info("Encryption modes verified: " + modes.length + " modes");
    }
  }

  @Nested
  @DisplayName("CryptoKeyType Tests")
  class CryptoKeyTypeTests {

    @Test
    @DisplayName("Should have all expected key types")
    void shouldHaveAllExpectedKeyTypes() {
      LOGGER.info("Testing CryptoKeyType enum values");

      CryptoKeyType[] types = CryptoKeyType.values();

      assertTrue(types.length >= 2, "Should have at least 2 key types");

      LOGGER.info("Key types verified: " + types.length + " types");
    }
  }

  @Nested
  @DisplayName("MacAlgorithm Tests")
  class MacAlgorithmTests {

    @Test
    @DisplayName("Should have all expected MAC algorithms")
    void shouldHaveAllExpectedMacAlgorithms() {
      LOGGER.info("Testing MacAlgorithm enum values");

      MacAlgorithm[] algorithms = MacAlgorithm.values();

      assertTrue(algorithms.length >= 1, "Should have at least 1 MAC algorithm");

      LOGGER.info("MAC algorithms verified: " + algorithms.length + " algorithms");
    }
  }

  @Nested
  @DisplayName("PaddingScheme Tests")
  class PaddingSchemeTests {

    @Test
    @DisplayName("Should have all expected padding schemes")
    void shouldHaveAllExpectedPaddingSchemes() {
      LOGGER.info("Testing PaddingScheme enum values");

      PaddingScheme[] schemes = PaddingScheme.values();

      assertTrue(schemes.length >= 1, "Should have at least 1 padding scheme");

      LOGGER.info("Padding schemes verified: " + schemes.length + " schemes");
    }
  }

  @Nested
  @DisplayName("KeyDerivationAlgorithm Tests")
  class KeyDerivationAlgorithmTests {

    @Test
    @DisplayName("Should have all expected key derivation algorithms")
    void shouldHaveAllExpectedKeyDerivationAlgorithms() {
      LOGGER.info("Testing KeyDerivationAlgorithm enum values");

      KeyDerivationAlgorithm[] algorithms = KeyDerivationAlgorithm.values();

      assertTrue(algorithms.length >= 1, "Should have at least 1 key derivation algorithm");

      LOGGER.info("Key derivation algorithms verified: " + algorithms.length + " algorithms");
    }
  }

  @Nested
  @DisplayName("CryptoErrorCode Tests")
  class CryptoErrorCodeTests {

    @Test
    @DisplayName("Should have all expected error codes")
    void shouldHaveAllExpectedErrorCodes() {
      LOGGER.info("Testing CryptoErrorCode enum values");

      CryptoErrorCode[] codes = CryptoErrorCode.values();

      assertTrue(codes.length >= 1, "Should have at least 1 error code");

      LOGGER.info("Error codes verified: " + codes.length + " codes");
    }
  }

  @Nested
  @DisplayName("EncryptionOptions Tests")
  class EncryptionOptionsTests {

    @Test
    @DisplayName("Should build encryption options with mode")
    void shouldBuildEncryptionOptionsWithMode() {
      LOGGER.info("Testing EncryptionOptions builder");

      EncryptionMode mode = EncryptionMode.values()[0];
      EncryptionOptions options = EncryptionOptions.builder(mode).build();

      assertNotNull(options, "Options should not be null");
      assertEquals(mode, options.getMode(), "Mode should match");
      assertTrue(
          options.useHardwareAcceleration(), "Hardware acceleration should be true by default");

      LOGGER.info("EncryptionOptions built successfully");
    }

    @Test
    @DisplayName("Should build encryption options with IV")
    void shouldBuildEncryptionOptionsWithIv() {
      LOGGER.info("Testing EncryptionOptions with IV");

      EncryptionMode mode = EncryptionMode.values()[0];
      byte[] iv = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};

      EncryptionOptions options = EncryptionOptions.builder(mode).iv(iv).build();

      assertTrue(options.getIv().isPresent(), "IV should be present");
      assertArrayEquals(iv, options.getIv().get(), "IV should match");

      LOGGER.info("EncryptionOptions with IV verified");
    }

    @Test
    @DisplayName("Should build encryption options with additional data")
    void shouldBuildEncryptionOptionsWithAdditionalData() {
      LOGGER.info("Testing EncryptionOptions with additional data");

      EncryptionMode mode = EncryptionMode.values()[0];
      byte[] aad = new byte[] {0x41, 0x41, 0x44}; // "AAD"

      EncryptionOptions options = EncryptionOptions.builder(mode).additionalData(aad).build();

      assertTrue(options.getAdditionalData().isPresent(), "AAD should be present");
      assertArrayEquals(aad, options.getAdditionalData().get(), "AAD should match");

      LOGGER.info("EncryptionOptions with additional data verified");
    }

    @Test
    @DisplayName("Should build encryption options without hardware acceleration")
    void shouldBuildEncryptionOptionsWithoutHardwareAcceleration() {
      LOGGER.info("Testing EncryptionOptions without hardware acceleration");

      EncryptionMode mode = EncryptionMode.values()[0];

      EncryptionOptions options =
          EncryptionOptions.builder(mode).useHardwareAcceleration(false).build();

      assertFalse(options.useHardwareAcceleration(), "Hardware acceleration should be disabled");

      LOGGER.info("EncryptionOptions without hardware acceleration verified");
    }

    @Test
    @DisplayName("Should handle null IV")
    void shouldHandleNullIv() {
      LOGGER.info("Testing EncryptionOptions with null IV");

      EncryptionMode mode = EncryptionMode.values()[0];

      EncryptionOptions options = EncryptionOptions.builder(mode).iv(null).build();

      assertFalse(options.getIv().isPresent(), "IV should not be present");

      LOGGER.info("Null IV handling verified");
    }

    @Test
    @DisplayName("Should defensively copy IV")
    void shouldDefensivelyCopyIv() {
      LOGGER.info("Testing EncryptionOptions defensive copying");

      EncryptionMode mode = EncryptionMode.values()[0];
      byte[] iv = new byte[] {1, 2, 3, 4};

      EncryptionOptions options = EncryptionOptions.builder(mode).iv(iv).build();

      // Modify original array
      iv[0] = 99;

      // Original in options should be unchanged
      byte[] optionsIv = options.getIv().get();
      assertNotEquals(99, optionsIv[0], "Options IV should be unchanged");

      LOGGER.info("Defensive copying verified");
    }
  }

  @Nested
  @DisplayName("SignatureOptions Tests")
  class SignatureOptionsTests {

    @Test
    @DisplayName("Should build default signature options")
    void shouldBuildDefaultSignatureOptions() {
      LOGGER.info("Testing SignatureOptions builder");

      SignatureOptions options = SignatureOptions.builder().build();

      assertNotNull(options, "Options should not be null");
      assertTrue(
          options.useHardwareAcceleration(), "Hardware acceleration should be true by default");
      assertTrue(
          options.useDeterministicSignatures(),
          "Deterministic signatures should be true by default");
      assertFalse(options.isPrehashed(), "Prehashed should be false by default");

      LOGGER.info("SignatureOptions built successfully");
    }

    @Test
    @DisplayName("Should build signature options with prehash")
    void shouldBuildSignatureOptionsWithPrehash() {
      LOGGER.info("Testing SignatureOptions with prehash");

      SignatureOptions options =
          SignatureOptions.builder().prehashed(HashAlgorithm.SHA_256).build();

      assertTrue(options.isPrehashed(), "Prehashed should be true");
      assertEquals(
          HashAlgorithm.SHA_256, options.getPrehashAlgorithm(), "Prehash algorithm should match");

      LOGGER.info("SignatureOptions with prehash verified");
    }

    @Test
    @DisplayName("Should create default signature options via static method")
    void shouldCreateDefaultSignatureOptionsViaStaticMethod() {
      LOGGER.info("Testing SignatureOptions.defaults()");

      SignatureOptions options = SignatureOptions.defaults();

      assertNotNull(options, "Options should not be null");
      assertTrue(options.useHardwareAcceleration(), "Default should use hardware acceleration");

      LOGGER.info("SignatureOptions.defaults() verified");
    }

    @Test
    @DisplayName("Should build signature options without hardware acceleration")
    void shouldBuildSignatureOptionsWithoutHardwareAcceleration() {
      LOGGER.info("Testing SignatureOptions without hardware acceleration");

      SignatureOptions options = SignatureOptions.builder().useHardwareAcceleration(false).build();

      assertFalse(options.useHardwareAcceleration(), "Hardware acceleration should be disabled");

      LOGGER.info("SignatureOptions without hardware acceleration verified");
    }

    @Test
    @DisplayName("Should build signature options without deterministic signatures")
    void shouldBuildSignatureOptionsWithoutDeterministicSignatures() {
      LOGGER.info("Testing SignatureOptions without deterministic signatures");

      SignatureOptions options = SignatureOptions.builder().deterministicSignatures(false).build();

      assertFalse(
          options.useDeterministicSignatures(), "Deterministic signatures should be disabled");

      LOGGER.info("SignatureOptions without deterministic signatures verified");
    }
  }

  @Nested
  @DisplayName("CryptoException Tests")
  class CryptoExceptionTests {

    @Test
    @DisplayName("Should create crypto exception with message")
    void shouldCreateCryptoExceptionWithMessage() {
      LOGGER.info("Testing CryptoException creation");

      CryptoException exception = new CryptoException("Test error");

      assertNotNull(exception, "Exception should not be null");
      assertEquals("Test error", exception.getMessage(), "Message should match");

      LOGGER.info("CryptoException creation verified");
    }

    @Test
    @DisplayName("Should create crypto exception with cause")
    void shouldCreateCryptoExceptionWithCause() {
      LOGGER.info("Testing CryptoException with cause");

      RuntimeException cause = new RuntimeException("Cause");
      CryptoException exception = new CryptoException("Test error", cause);

      assertNotNull(exception.getCause(), "Cause should not be null");
      assertEquals(cause, exception.getCause(), "Cause should match");

      LOGGER.info("CryptoException with cause verified");
    }
  }
}
