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

package ai.tegmentum.wasmtime4j.wasi.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link SignatureOptions} class.
 *
 * <p>Verifies builder construction, defaults factory, prehashing configuration, hardware
 * acceleration, and deterministic signature settings.
 */
@DisplayName("SignatureOptions Tests")
class SignatureOptionsTest {

  @Nested
  @DisplayName("Defaults Factory Tests")
  class DefaultsFactoryTests {

    @Test
    @DisplayName("defaults() should return non-null options")
    void defaultsShouldReturnNonNull() {
      final SignatureOptions options = SignatureOptions.defaults();
      assertNotNull(options, "defaults() should return non-null options");
    }

    @Test
    @DisplayName("defaults() should not be prehashed")
    void defaultsShouldNotBePrehashed() {
      final SignatureOptions options = SignatureOptions.defaults();
      assertFalse(options.isPrehashed(), "Default options should not be prehashed");
    }

    @Test
    @DisplayName("defaults() should have null prehash algorithm")
    void defaultsShouldHaveNullPrehashAlgorithm() {
      final SignatureOptions options = SignatureOptions.defaults();
      assertNull(
          options.getPrehashAlgorithm(),
          "Default options should have null prehash algorithm");
    }

    @Test
    @DisplayName("defaults() should enable hardware acceleration")
    void defaultsShouldEnableHardwareAcceleration() {
      final SignatureOptions options = SignatureOptions.defaults();
      assertTrue(
          options.useHardwareAcceleration(),
          "Default options should enable hardware acceleration");
    }

    @Test
    @DisplayName("defaults() should enable deterministic signatures")
    void defaultsShouldEnableDeterministicSignatures() {
      final SignatureOptions options = SignatureOptions.defaults();
      assertTrue(
          options.useDeterministicSignatures(),
          "Default options should enable deterministic signatures");
    }
  }

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("builder should create options with defaults")
    void builderShouldCreateWithDefaults() {
      final SignatureOptions options = SignatureOptions.builder().build();
      assertFalse(options.isPrehashed(), "Builder default should not be prehashed");
      assertNull(
          options.getPrehashAlgorithm(),
          "Builder default should have null prehash algorithm");
      assertTrue(
          options.useHardwareAcceleration(),
          "Builder default should enable hardware acceleration");
      assertTrue(
          options.useDeterministicSignatures(),
          "Builder default should enable deterministic signatures");
    }

    @Test
    @DisplayName("builder should match defaults() factory output")
    void builderShouldMatchDefaultsFactory() {
      final SignatureOptions fromDefaults = SignatureOptions.defaults();
      final SignatureOptions fromBuilder = SignatureOptions.builder().build();

      assertEquals(
          fromDefaults.isPrehashed(), fromBuilder.isPrehashed(),
          "isPrehashed should match between defaults() and builder().build()");
      assertEquals(
          fromDefaults.useHardwareAcceleration(), fromBuilder.useHardwareAcceleration(),
          "useHardwareAcceleration should match");
      assertEquals(
          fromDefaults.useDeterministicSignatures(), fromBuilder.useDeterministicSignatures(),
          "useDeterministicSignatures should match");
    }
  }

  @Nested
  @DisplayName("Prehashing Tests")
  class PrehashingTests {

    @Test
    @DisplayName("prehashed should set algorithm and flag")
    void prehashedShouldSetAlgorithmAndFlag() {
      final SignatureOptions options =
          SignatureOptions.builder()
              .prehashed(HashAlgorithm.SHA_256)
              .build();

      assertTrue(options.isPrehashed(), "Should be prehashed");
      assertEquals(
          HashAlgorithm.SHA_256, options.getPrehashAlgorithm(),
          "Prehash algorithm should be SHA-256");
    }

    @Test
    @DisplayName("prehashed with SHA-512 should set correctly")
    void prehashedWithSha512() {
      final SignatureOptions options =
          SignatureOptions.builder()
              .prehashed(HashAlgorithm.SHA_512)
              .build();

      assertTrue(options.isPrehashed(), "Should be prehashed with SHA-512");
      assertEquals(
          HashAlgorithm.SHA_512, options.getPrehashAlgorithm(),
          "Prehash algorithm should be SHA-512");
    }

    @Test
    @DisplayName("prehashed with BLAKE3 should set correctly")
    void prehashedWithBlake3() {
      final SignatureOptions options =
          SignatureOptions.builder()
              .prehashed(HashAlgorithm.BLAKE3)
              .build();

      assertTrue(options.isPrehashed(), "Should be prehashed with BLAKE3");
      assertEquals(
          HashAlgorithm.BLAKE3, options.getPrehashAlgorithm(),
          "Prehash algorithm should be BLAKE3");
    }
  }

  @Nested
  @DisplayName("Hardware Acceleration Tests")
  class HardwareAccelerationTests {

    @Test
    @DisplayName("should disable hardware acceleration")
    void shouldDisableHardwareAcceleration() {
      final SignatureOptions options =
          SignatureOptions.builder()
              .useHardwareAcceleration(false)
              .build();
      assertFalse(
          options.useHardwareAcceleration(),
          "Hardware acceleration should be disabled");
    }

    @Test
    @DisplayName("should explicitly enable hardware acceleration")
    void shouldExplicitlyEnableHardwareAcceleration() {
      final SignatureOptions options =
          SignatureOptions.builder()
              .useHardwareAcceleration(true)
              .build();
      assertTrue(
          options.useHardwareAcceleration(),
          "Hardware acceleration should be enabled");
    }
  }

  @Nested
  @DisplayName("Deterministic Signatures Tests")
  class DeterministicSignaturesTests {

    @Test
    @DisplayName("should disable deterministic signatures")
    void shouldDisableDeterministicSignatures() {
      final SignatureOptions options =
          SignatureOptions.builder()
              .deterministicSignatures(false)
              .build();
      assertFalse(
          options.useDeterministicSignatures(),
          "Deterministic signatures should be disabled");
    }

    @Test
    @DisplayName("should explicitly enable deterministic signatures")
    void shouldExplicitlyEnableDeterministicSignatures() {
      final SignatureOptions options =
          SignatureOptions.builder()
              .deterministicSignatures(true)
              .build();
      assertTrue(
          options.useDeterministicSignatures(),
          "Deterministic signatures should be enabled");
    }
  }

  @Nested
  @DisplayName("Combined Configuration Tests")
  class CombinedConfigurationTests {

    @Test
    @DisplayName("should configure all options together")
    void shouldConfigureAllOptionsTogether() {
      final SignatureOptions options =
          SignatureOptions.builder()
              .prehashed(HashAlgorithm.SHA_384)
              .useHardwareAcceleration(false)
              .deterministicSignatures(false)
              .build();

      assertTrue(options.isPrehashed(), "Should be prehashed");
      assertEquals(
          HashAlgorithm.SHA_384, options.getPrehashAlgorithm(),
          "Algorithm should be SHA-384");
      assertFalse(
          options.useHardwareAcceleration(),
          "Hardware acceleration should be disabled");
      assertFalse(
          options.useDeterministicSignatures(),
          "Deterministic signatures should be disabled");
    }
  }
}
