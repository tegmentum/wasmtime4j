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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link EncryptionOptions} class.
 *
 * <p>Verifies builder construction, field accessors, defaults, defensive copies on byte arrays,
 * and mode configuration.
 */
@DisplayName("EncryptionOptions Tests")
class EncryptionOptionsTest {

  @Nested
  @DisplayName("Builder Construction Tests")
  class BuilderConstructionTests {

    @Test
    @DisplayName("should build with all fields set")
    void shouldBuildWithAllFieldsSet() {
      final byte[] iv = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
      final byte[] aad = {0x41, 0x42, 0x43};
      final EncryptionOptions options =
          EncryptionOptions.builder(EncryptionMode.GCM)
              .iv(iv)
              .additionalData(aad)
              .useHardwareAcceleration(false)
              .build();

      assertEquals(EncryptionMode.GCM, options.getMode(), "Mode should be GCM");
      assertTrue(options.getIv().isPresent(), "IV should be present");
      assertArrayEquals(iv, options.getIv().get(), "IV should match original");
      assertTrue(options.getAdditionalData().isPresent(), "Additional data should be present");
      assertArrayEquals(aad, options.getAdditionalData().get(), "Additional data should match");
      assertFalse(
          options.useHardwareAcceleration(),
          "Hardware acceleration should be disabled");
    }

    @Test
    @DisplayName("should build with only mode (defaults)")
    void shouldBuildWithOnlyMode() {
      final EncryptionOptions options =
          EncryptionOptions.builder(EncryptionMode.CBC).build();

      assertEquals(EncryptionMode.CBC, options.getMode(), "Mode should be CBC");
      assertFalse(options.getIv().isPresent(), "IV should not be present by default");
      assertFalse(
          options.getAdditionalData().isPresent(),
          "Additional data should not be present by default");
      assertTrue(
          options.useHardwareAcceleration(),
          "Hardware acceleration should default to true");
    }

    @Test
    @DisplayName("should support ECB mode (no IV)")
    void shouldSupportEcbMode() {
      final EncryptionOptions options =
          EncryptionOptions.builder(EncryptionMode.ECB).build();
      assertEquals(EncryptionMode.ECB, options.getMode(), "Mode should be ECB");
      assertFalse(options.getIv().isPresent(), "ECB should not require IV");
    }

    @Test
    @DisplayName("should support all AEAD modes")
    void shouldSupportAllAeadModes() {
      for (final EncryptionMode mode : EncryptionMode.values()) {
        final EncryptionOptions options =
            EncryptionOptions.builder(mode).build();
        assertEquals(mode, options.getMode(), "Mode should match: " + mode);
      }
    }
  }

  @Nested
  @DisplayName("Defensive Copy Tests")
  class DefensiveCopyTests {

    @Test
    @DisplayName("should defensively copy IV on construction")
    void shouldDefensivelyCopyIvOnConstruction() {
      final byte[] iv = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
      final EncryptionOptions options =
          EncryptionOptions.builder(EncryptionMode.GCM)
              .iv(iv)
              .build();

      iv[0] = 99;
      final Optional<byte[]> storedIv = options.getIv();
      assertTrue(storedIv.isPresent(), "IV should be present");
      assertEquals(
          1, storedIv.get()[0],
          "Modifying original IV should not affect stored IV");
    }

    @Test
    @DisplayName("should defensively copy IV on retrieval")
    void shouldDefensivelyCopyIvOnRetrieval() {
      final byte[] iv = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
      final EncryptionOptions options =
          EncryptionOptions.builder(EncryptionMode.GCM)
              .iv(iv)
              .build();

      final byte[] retrieved1 = options.getIv().get();
      retrieved1[0] = 99;
      final byte[] retrieved2 = options.getIv().get();
      assertEquals(
          1, retrieved2[0],
          "Modifying retrieved IV should not affect stored IV");
    }

    @Test
    @DisplayName("should defensively copy additional data on construction")
    void shouldDefensivelyCopyAdditionalDataOnConstruction() {
      final byte[] aad = {0x41, 0x42, 0x43};
      final EncryptionOptions options =
          EncryptionOptions.builder(EncryptionMode.GCM)
              .additionalData(aad)
              .build();

      aad[0] = 99;
      final Optional<byte[]> storedAad = options.getAdditionalData();
      assertTrue(storedAad.isPresent(), "Additional data should be present");
      assertEquals(
          0x41, storedAad.get()[0],
          "Modifying original AAD should not affect stored AAD");
    }

    @Test
    @DisplayName("should defensively copy additional data on retrieval")
    void shouldDefensivelyCopyAdditionalDataOnRetrieval() {
      final byte[] aad = {0x41, 0x42, 0x43};
      final EncryptionOptions options =
          EncryptionOptions.builder(EncryptionMode.GCM)
              .additionalData(aad)
              .build();

      final byte[] retrieved1 = options.getAdditionalData().get();
      retrieved1[0] = 99;
      final byte[] retrieved2 = options.getAdditionalData().get();
      assertEquals(
          0x41, retrieved2[0],
          "Modifying retrieved AAD should not affect stored AAD");
    }

    @Test
    @DisplayName("builder IV should defensively copy input")
    void builderIvShouldDefensivelyCopyInput() {
      final byte[] iv = {1, 2, 3, 4};
      final EncryptionOptions.Builder builder =
          EncryptionOptions.builder(EncryptionMode.CTR).iv(iv);
      iv[0] = 99;
      final EncryptionOptions options = builder.build();
      assertEquals(
          1, options.getIv().get()[0],
          "Builder should defensively copy IV input");
    }
  }

  @Nested
  @DisplayName("Null Handling Tests")
  class NullHandlingTests {

    @Test
    @DisplayName("should accept null IV in builder")
    void shouldAcceptNullIvInBuilder() {
      final EncryptionOptions options =
          EncryptionOptions.builder(EncryptionMode.ECB)
              .iv(null)
              .build();
      assertFalse(options.getIv().isPresent(), "Null IV should result in empty Optional");
    }

    @Test
    @DisplayName("should accept null additional data in builder")
    void shouldAcceptNullAdditionalDataInBuilder() {
      final EncryptionOptions options =
          EncryptionOptions.builder(EncryptionMode.GCM)
              .additionalData(null)
              .build();
      assertFalse(
          options.getAdditionalData().isPresent(),
          "Null additional data should result in empty Optional");
    }
  }

  @Nested
  @DisplayName("Hardware Acceleration Tests")
  class HardwareAccelerationTests {

    @Test
    @DisplayName("hardware acceleration should default to true")
    void hardwareAccelerationShouldDefaultToTrue() {
      final EncryptionOptions options =
          EncryptionOptions.builder(EncryptionMode.GCM).build();
      assertTrue(
          options.useHardwareAcceleration(),
          "Hardware acceleration should default to true");
    }

    @Test
    @DisplayName("should disable hardware acceleration")
    void shouldDisableHardwareAcceleration() {
      final EncryptionOptions options =
          EncryptionOptions.builder(EncryptionMode.GCM)
              .useHardwareAcceleration(false)
              .build();
      assertFalse(
          options.useHardwareAcceleration(),
          "Hardware acceleration should be disabled");
    }
  }
}
