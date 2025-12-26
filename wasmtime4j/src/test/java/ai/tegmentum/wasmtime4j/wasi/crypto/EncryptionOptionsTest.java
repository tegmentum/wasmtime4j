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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link EncryptionOptions} class.
 *
 * <p>EncryptionOptions provides configuration for symmetric encryption operations including
 * encryption mode, initialization vector, additional authenticated data, and hardware acceleration.
 */
@DisplayName("EncryptionOptions Tests")
class EncryptionOptionsTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(EncryptionOptions.class.getModifiers()),
          "EncryptionOptions should be final");
    }

    @Test
    @DisplayName("should not be an interface")
    void shouldNotBeAnInterface() {
      assertFalse(
          EncryptionOptions.class.isInterface(),
          "EncryptionOptions should be a class, not an interface");
    }

    @Test
    @DisplayName("should have nested Builder class")
    void shouldHaveNestedBuilderClass() {
      final Class<?>[] declaredClasses = EncryptionOptions.class.getDeclaredClasses();
      boolean hasBuilder = false;
      for (final Class<?> clazz : declaredClasses) {
        if (clazz.getSimpleName().equals("Builder")) {
          hasBuilder = true;
          break;
        }
      }
      assertTrue(hasBuilder, "Should have nested Builder class");
    }
  }

  @Nested
  @DisplayName("Builder Pattern Tests")
  class BuilderPatternTests {

    @Test
    @DisplayName("builder should require encryption mode")
    void builderShouldRequireEncryptionMode() {
      final EncryptionOptions.Builder builder = EncryptionOptions.builder(EncryptionMode.GCM);
      assertNotNull(builder, "Builder should be created with mode");
    }

    @Test
    @DisplayName("builder should create instance with mode only")
    void builderShouldCreateInstanceWithModeOnly() {
      final EncryptionOptions options = EncryptionOptions.builder(EncryptionMode.CBC).build();
      assertNotNull(options, "Should create instance");
      assertEquals(EncryptionMode.CBC, options.getMode(), "Should have correct mode");
    }

    @Test
    @DisplayName("builder should support fluent API")
    void builderShouldSupportFluentApi() {
      final byte[] iv = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
      final byte[] aad = new byte[] {0x41, 0x42, 0x43};

      final EncryptionOptions options =
          EncryptionOptions.builder(EncryptionMode.GCM)
              .iv(iv)
              .additionalData(aad)
              .useHardwareAcceleration(false)
              .build();

      assertNotNull(options, "Should create instance");
      assertEquals(EncryptionMode.GCM, options.getMode(), "Should have correct mode");
    }

    @Test
    @DisplayName("builder should be reusable for multiple builds")
    void builderShouldBeReusableForMultipleBuilds() {
      final EncryptionOptions.Builder builder = EncryptionOptions.builder(EncryptionMode.CTR);

      final EncryptionOptions options1 = builder.build();
      final EncryptionOptions options2 = builder.build();

      assertNotNull(options1, "First build should work");
      assertNotNull(options2, "Second build should work");
      assertNotSame(options1, options2, "Should create different instances");
    }
  }

  @Nested
  @DisplayName("Encryption Mode Tests")
  class EncryptionModeTests {

    @Test
    @DisplayName("getMode should return the configured mode")
    void getModeShouldReturnTheConfiguredMode() {
      for (final EncryptionMode mode : EncryptionMode.values()) {
        final EncryptionOptions options = EncryptionOptions.builder(mode).build();
        assertEquals(mode, options.getMode(), "Should return mode: " + mode);
      }
    }

    @Test
    @DisplayName("should support CBC mode")
    void shouldSupportCbcMode() {
      final EncryptionOptions options = EncryptionOptions.builder(EncryptionMode.CBC).build();
      assertEquals(EncryptionMode.CBC, options.getMode(), "Should support CBC");
    }

    @Test
    @DisplayName("should support GCM mode")
    void shouldSupportGcmMode() {
      final EncryptionOptions options = EncryptionOptions.builder(EncryptionMode.GCM).build();
      assertEquals(EncryptionMode.GCM, options.getMode(), "Should support GCM");
    }

    @Test
    @DisplayName("should support CTR mode")
    void shouldSupportCtrMode() {
      final EncryptionOptions options = EncryptionOptions.builder(EncryptionMode.CTR).build();
      assertEquals(EncryptionMode.CTR, options.getMode(), "Should support CTR");
    }
  }

  @Nested
  @DisplayName("Initialization Vector Tests")
  class InitializationVectorTests {

    @Test
    @DisplayName("getIv should return empty when not set")
    void getIvShouldReturnEmptyWhenNotSet() {
      final EncryptionOptions options = EncryptionOptions.builder(EncryptionMode.CBC).build();
      final Optional<byte[]> iv = options.getIv();
      assertFalse(iv.isPresent(), "IV should be empty when not set");
    }

    @Test
    @DisplayName("getIv should return IV when set")
    void getIvShouldReturnIvWhenSet() {
      final byte[] iv = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
      final EncryptionOptions options =
          EncryptionOptions.builder(EncryptionMode.CBC).iv(iv).build();

      final Optional<byte[]> result = options.getIv();
      assertTrue(result.isPresent(), "IV should be present");
      assertArrayEquals(iv, result.get(), "IV should match");
    }

    @Test
    @DisplayName("IV should be defensively copied on input")
    void ivShouldBeDefensivelyCopiedOnInput() {
      final byte[] iv = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
      final EncryptionOptions options =
          EncryptionOptions.builder(EncryptionMode.GCM).iv(iv).build();

      // Modify original array
      iv[0] = 99;

      // Stored IV should be unaffected
      final Optional<byte[]> result = options.getIv();
      assertTrue(result.isPresent(), "IV should be present");
      assertEquals(1, result.get()[0], "IV should not be affected by original array modification");
    }

    @Test
    @DisplayName("IV should be defensively copied on output")
    void ivShouldBeDefensivelyCopiedOnOutput() {
      final byte[] iv = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
      final EncryptionOptions options =
          EncryptionOptions.builder(EncryptionMode.GCM).iv(iv).build();

      // Get and modify the returned array
      final byte[] result1 = options.getIv().get();
      result1[0] = 99;

      // Second retrieval should be unaffected
      final byte[] result2 = options.getIv().get();
      assertEquals(1, result2[0], "Subsequent getIv should return fresh copy");
    }

    @Test
    @DisplayName("setting null IV should clear it")
    void settingNullIvShouldClearIt() {
      final EncryptionOptions options =
          EncryptionOptions.builder(EncryptionMode.CBC).iv(new byte[] {1, 2, 3}).iv(null).build();

      assertFalse(options.getIv().isPresent(), "IV should be empty after setting null");
    }
  }

  @Nested
  @DisplayName("Additional Authenticated Data Tests")
  class AdditionalAuthenticatedDataTests {

    @Test
    @DisplayName("getAdditionalData should return empty when not set")
    void getAdditionalDataShouldReturnEmptyWhenNotSet() {
      final EncryptionOptions options = EncryptionOptions.builder(EncryptionMode.GCM).build();
      final Optional<byte[]> aad = options.getAdditionalData();
      assertFalse(aad.isPresent(), "AAD should be empty when not set");
    }

    @Test
    @DisplayName("getAdditionalData should return AAD when set")
    void getAdditionalDataShouldReturnAadWhenSet() {
      final byte[] aad = new byte[] {0x48, 0x65, 0x61, 0x64, 0x65, 0x72};
      final EncryptionOptions options =
          EncryptionOptions.builder(EncryptionMode.GCM).additionalData(aad).build();

      final Optional<byte[]> result = options.getAdditionalData();
      assertTrue(result.isPresent(), "AAD should be present");
      assertArrayEquals(aad, result.get(), "AAD should match");
    }

    @Test
    @DisplayName("AAD should be defensively copied on input")
    void aadShouldBeDefensivelyCopiedOnInput() {
      final byte[] aad = new byte[] {1, 2, 3, 4, 5};
      final EncryptionOptions options =
          EncryptionOptions.builder(EncryptionMode.GCM).additionalData(aad).build();

      // Modify original array
      aad[0] = 99;

      // Stored AAD should be unaffected
      final Optional<byte[]> result = options.getAdditionalData();
      assertTrue(result.isPresent(), "AAD should be present");
      assertEquals(1, result.get()[0], "AAD should not be affected by original array modification");
    }

    @Test
    @DisplayName("AAD should be defensively copied on output")
    void aadShouldBeDefensivelyCopiedOnOutput() {
      final byte[] aad = new byte[] {1, 2, 3, 4, 5};
      final EncryptionOptions options =
          EncryptionOptions.builder(EncryptionMode.GCM).additionalData(aad).build();

      // Get and modify the returned array
      final byte[] result1 = options.getAdditionalData().get();
      result1[0] = 99;

      // Second retrieval should be unaffected
      final byte[] result2 = options.getAdditionalData().get();
      assertEquals(1, result2[0], "Subsequent getAdditionalData should return fresh copy");
    }

    @Test
    @DisplayName("setting null AAD should clear it")
    void settingNullAadShouldClearIt() {
      final EncryptionOptions options =
          EncryptionOptions.builder(EncryptionMode.GCM)
              .additionalData(new byte[] {1, 2, 3})
              .additionalData(null)
              .build();

      assertFalse(
          options.getAdditionalData().isPresent(), "AAD should be empty after setting null");
    }
  }

  @Nested
  @DisplayName("Hardware Acceleration Tests")
  class HardwareAccelerationTests {

    @Test
    @DisplayName("useHardwareAcceleration should default to true")
    void useHardwareAccelerationShouldDefaultToTrue() {
      final EncryptionOptions options = EncryptionOptions.builder(EncryptionMode.GCM).build();
      assertTrue(options.useHardwareAcceleration(), "Hardware acceleration should default to true");
    }

    @Test
    @DisplayName("useHardwareAcceleration should return configured value")
    void useHardwareAccelerationShouldReturnConfiguredValue() {
      final EncryptionOptions enabled =
          EncryptionOptions.builder(EncryptionMode.GCM).useHardwareAcceleration(true).build();
      assertTrue(enabled.useHardwareAcceleration(), "Should be enabled when set to true");

      final EncryptionOptions disabled =
          EncryptionOptions.builder(EncryptionMode.GCM).useHardwareAcceleration(false).build();
      assertFalse(disabled.useHardwareAcceleration(), "Should be disabled when set to false");
    }
  }

  @Nested
  @DisplayName("AEAD Mode Usage Tests")
  class AeadModeUsageTests {

    @Test
    @DisplayName("should support GCM with IV and AAD")
    void shouldSupportGcmWithIvAndAad() {
      final byte[] iv = new byte[12]; // GCM typically uses 12-byte IV
      final byte[] aad = "header".getBytes();

      final EncryptionOptions options =
          EncryptionOptions.builder(EncryptionMode.GCM).iv(iv).additionalData(aad).build();

      assertEquals(EncryptionMode.GCM, options.getMode(), "Should be GCM mode");
      assertTrue(options.getIv().isPresent(), "Should have IV");
      assertTrue(options.getAdditionalData().isPresent(), "Should have AAD");
    }

    @Test
    @DisplayName("should support CBC with IV only")
    void shouldSupportCbcWithIvOnly() {
      final byte[] iv = new byte[16]; // CBC uses 16-byte IV

      final EncryptionOptions options =
          EncryptionOptions.builder(EncryptionMode.CBC).iv(iv).build();

      assertEquals(EncryptionMode.CBC, options.getMode(), "Should be CBC mode");
      assertTrue(options.getIv().isPresent(), "Should have IV");
      assertFalse(options.getAdditionalData().isPresent(), "Should not have AAD for CBC");
    }
  }

  @Nested
  @DisplayName("Usage Pattern Documentation Tests")
  class UsagePatternDocumentationTests {

    @Test
    @DisplayName("should support typical GCM encryption setup")
    void shouldSupportTypicalGcmEncryptionSetup() {
      // Typical AES-GCM usage pattern
      final byte[] iv = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
      final byte[] aad = "Associated Data".getBytes();

      final EncryptionOptions options =
          EncryptionOptions.builder(EncryptionMode.GCM)
              .iv(iv)
              .additionalData(aad)
              .useHardwareAcceleration(true)
              .build();

      assertEquals(EncryptionMode.GCM, options.getMode(), "Should use GCM");
      assertArrayEquals(iv, options.getIv().get(), "Should have correct IV");
      assertArrayEquals(aad, options.getAdditionalData().get(), "Should have correct AAD");
      assertTrue(options.useHardwareAcceleration(), "Should use hardware acceleration");
    }

    @Test
    @DisplayName("should support software-only encryption")
    void shouldSupportSoftwareOnlyEncryption() {
      // Pattern for environments without hardware AES support
      final EncryptionOptions options =
          EncryptionOptions.builder(EncryptionMode.CBC)
              .iv(new byte[16])
              .useHardwareAcceleration(false)
              .build();

      assertFalse(options.useHardwareAcceleration(), "Should disable hardware acceleration");
    }

    @Test
    @DisplayName("should support CTR mode streaming encryption")
    void shouldSupportCtrModeStreamingEncryption() {
      // CTR mode for streaming encryption
      final byte[] nonce = new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1};

      final EncryptionOptions options =
          EncryptionOptions.builder(EncryptionMode.CTR).iv(nonce).build();

      assertEquals(EncryptionMode.CTR, options.getMode(), "Should use CTR mode");
      assertTrue(options.getIv().isPresent(), "Should have nonce as IV");
    }
  }

  @Nested
  @DisplayName("Immutability Tests")
  class ImmutabilityTests {

    @Test
    @DisplayName("EncryptionOptions should be immutable")
    void encryptionOptionsShouldBeImmutable() {
      final byte[] iv = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
      final byte[] aad = new byte[] {0x41, 0x42, 0x43};

      final EncryptionOptions options =
          EncryptionOptions.builder(EncryptionMode.GCM).iv(iv).additionalData(aad).build();

      // Modify original arrays
      iv[0] = 99;
      aad[0] = 99;

      // Options should be unaffected
      assertEquals(1, options.getIv().get()[0], "IV should be immutable");
      assertEquals(0x41, options.getAdditionalData().get()[0], "AAD should be immutable");
    }
  }
}
