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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SignatureOptions} class.
 *
 * <p>SignatureOptions provides configuration for digital signature operations including prehashing,
 * hardware acceleration, and deterministic signature generation (RFC 6979).
 */
@DisplayName("SignatureOptions Tests")
class SignatureOptionsTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(SignatureOptions.class.getModifiers()),
          "SignatureOptions should be final");
    }

    @Test
    @DisplayName("should not be an interface")
    void shouldNotBeAnInterface() {
      assertFalse(
          SignatureOptions.class.isInterface(),
          "SignatureOptions should be a class, not an interface");
    }

    @Test
    @DisplayName("should have nested Builder class")
    void shouldHaveNestedBuilderClass() {
      final Class<?>[] declaredClasses = SignatureOptions.class.getDeclaredClasses();
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
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("defaults should return non-null instance")
    void defaultsShouldReturnNonNullInstance() {
      final SignatureOptions defaults = SignatureOptions.defaults();
      assertNotNull(defaults, "defaults() should return non-null");
    }

    @Test
    @DisplayName("defaults should have expected default values")
    void defaultsShouldHaveExpectedDefaultValues() {
      final SignatureOptions defaults = SignatureOptions.defaults();

      assertFalse(defaults.isPrehashed(), "Should not be prehashed by default");
      assertNull(defaults.getPrehashAlgorithm(), "Should have no prehash algorithm by default");
      assertTrue(defaults.useHardwareAcceleration(), "Should use hardware acceleration by default");
      assertTrue(
          defaults.useDeterministicSignatures(), "Should use deterministic signatures by default");
    }

    @Test
    @DisplayName("builder should return non-null builder")
    void builderShouldReturnNonNullBuilder() {
      final SignatureOptions.Builder builder = SignatureOptions.builder();
      assertNotNull(builder, "builder() should return non-null");
    }
  }

  @Nested
  @DisplayName("Builder Pattern Tests")
  class BuilderPatternTests {

    @Test
    @DisplayName("builder should create instance with defaults")
    void builderShouldCreateInstanceWithDefaults() {
      final SignatureOptions options = SignatureOptions.builder().build();

      assertNotNull(options, "Should create instance");
      assertFalse(options.isPrehashed(), "Should not be prehashed");
      assertNull(options.getPrehashAlgorithm(), "Should have no prehash algorithm");
      assertTrue(options.useHardwareAcceleration(), "Should use hardware acceleration");
      assertTrue(options.useDeterministicSignatures(), "Should use deterministic signatures");
    }

    @Test
    @DisplayName("builder should support fluent API")
    void builderShouldSupportFluentApi() {
      final SignatureOptions options =
          SignatureOptions.builder()
              .prehashed(HashAlgorithm.SHA_256)
              .useHardwareAcceleration(false)
              .deterministicSignatures(false)
              .build();

      assertNotNull(options, "Should create instance");
      assertTrue(options.isPrehashed(), "Should be prehashed");
    }

    @Test
    @DisplayName("builder should be reusable for multiple builds")
    void builderShouldBeReusableForMultipleBuilds() {
      final SignatureOptions.Builder builder = SignatureOptions.builder();

      final SignatureOptions options1 = builder.build();
      final SignatureOptions options2 = builder.build();

      assertNotNull(options1, "First build should work");
      assertNotNull(options2, "Second build should work");
      assertNotSame(options1, options2, "Should create different instances");
    }
  }

  @Nested
  @DisplayName("Prehash Tests")
  class PrehashTests {

    @Test
    @DisplayName("isPrehashed should return false by default")
    void isPrehashedShouldReturnFalseByDefault() {
      final SignatureOptions options = SignatureOptions.builder().build();
      assertFalse(options.isPrehashed(), "Should not be prehashed by default");
    }

    @Test
    @DisplayName("isPrehashed should return true when prehashed is set")
    void isPrehashedShouldReturnTrueWhenPrehashedIsSet() {
      final SignatureOptions options =
          SignatureOptions.builder().prehashed(HashAlgorithm.SHA_256).build();

      assertTrue(options.isPrehashed(), "Should be prehashed when set");
    }

    @Test
    @DisplayName("getPrehashAlgorithm should return null by default")
    void getPrehashAlgorithmShouldReturnNullByDefault() {
      final SignatureOptions options = SignatureOptions.builder().build();
      assertNull(options.getPrehashAlgorithm(), "Should have no prehash algorithm by default");
    }

    @Test
    @DisplayName("getPrehashAlgorithm should return the configured algorithm")
    void getPrehashAlgorithmShouldReturnTheConfiguredAlgorithm() {
      final SignatureOptions sha256 =
          SignatureOptions.builder().prehashed(HashAlgorithm.SHA_256).build();
      assertEquals(HashAlgorithm.SHA_256, sha256.getPrehashAlgorithm(), "Should have SHA-256");

      final SignatureOptions sha384 =
          SignatureOptions.builder().prehashed(HashAlgorithm.SHA_384).build();
      assertEquals(HashAlgorithm.SHA_384, sha384.getPrehashAlgorithm(), "Should have SHA-384");

      final SignatureOptions sha512 =
          SignatureOptions.builder().prehashed(HashAlgorithm.SHA_512).build();
      assertEquals(HashAlgorithm.SHA_512, sha512.getPrehashAlgorithm(), "Should have SHA-512");
    }

    @Test
    @DisplayName("prehashed should set both flag and algorithm")
    void prehashedShouldSetBothFlagAndAlgorithm() {
      final SignatureOptions options =
          SignatureOptions.builder().prehashed(HashAlgorithm.SHA3_256).build();

      assertTrue(options.isPrehashed(), "Prehashed flag should be set");
      assertEquals(
          HashAlgorithm.SHA3_256, options.getPrehashAlgorithm(), "Prehash algorithm should be set");
    }
  }

  @Nested
  @DisplayName("Hardware Acceleration Tests")
  class HardwareAccelerationTests {

    @Test
    @DisplayName("useHardwareAcceleration should default to true")
    void useHardwareAccelerationShouldDefaultToTrue() {
      final SignatureOptions options = SignatureOptions.builder().build();
      assertTrue(options.useHardwareAcceleration(), "Hardware acceleration should default to true");
    }

    @Test
    @DisplayName("useHardwareAcceleration should return configured value")
    void useHardwareAccelerationShouldReturnConfiguredValue() {
      final SignatureOptions enabled =
          SignatureOptions.builder().useHardwareAcceleration(true).build();
      assertTrue(enabled.useHardwareAcceleration(), "Should be enabled when set to true");

      final SignatureOptions disabled =
          SignatureOptions.builder().useHardwareAcceleration(false).build();
      assertFalse(disabled.useHardwareAcceleration(), "Should be disabled when set to false");
    }
  }

  @Nested
  @DisplayName("Deterministic Signatures Tests")
  class DeterministicSignaturesTests {

    @Test
    @DisplayName("useDeterministicSignatures should default to true")
    void useDeterministicSignaturesShouldDefaultToTrue() {
      final SignatureOptions options = SignatureOptions.builder().build();
      assertTrue(
          options.useDeterministicSignatures(), "Deterministic signatures should default to true");
    }

    @Test
    @DisplayName("useDeterministicSignatures should return configured value")
    void useDeterministicSignaturesShouldReturnConfiguredValue() {
      final SignatureOptions deterministic =
          SignatureOptions.builder().deterministicSignatures(true).build();
      assertTrue(
          deterministic.useDeterministicSignatures(), "Should be deterministic when set to true");

      final SignatureOptions randomized =
          SignatureOptions.builder().deterministicSignatures(false).build();
      assertFalse(
          randomized.useDeterministicSignatures(), "Should be randomized when set to false");
    }
  }

  @Nested
  @DisplayName("RFC 6979 Compliance Tests")
  class Rfc6979ComplianceTests {

    @Test
    @DisplayName("default options should follow RFC 6979")
    void defaultOptionsShouldFollowRfc6979() {
      // RFC 6979 specifies deterministic ECDSA/DSA signatures
      final SignatureOptions defaults = SignatureOptions.defaults();
      assertTrue(
          defaults.useDeterministicSignatures(),
          "Default should use deterministic signatures per RFC 6979");
    }

    @Test
    @DisplayName("should support explicit deterministic mode")
    void shouldSupportExplicitDeterministicMode() {
      final SignatureOptions options =
          SignatureOptions.builder().deterministicSignatures(true).build();

      assertTrue(options.useDeterministicSignatures(), "Should support explicit RFC 6979 mode");
    }

    @Test
    @DisplayName("should support randomized mode for specific use cases")
    void shouldSupportRandomizedModeForSpecificUseCases() {
      // Some protocols require randomized signatures
      final SignatureOptions options =
          SignatureOptions.builder().deterministicSignatures(false).build();

      assertFalse(options.useDeterministicSignatures(), "Should support non-deterministic mode");
    }
  }

  @Nested
  @DisplayName("Usage Pattern Documentation Tests")
  class UsagePatternDocumentationTests {

    @Test
    @DisplayName("should support typical ECDSA signing pattern")
    void shouldSupportTypicalEcdsaSigningPattern() {
      // Typical ECDSA P-256 with SHA-256 prehash
      final SignatureOptions options =
          SignatureOptions.builder()
              .prehashed(HashAlgorithm.SHA_256)
              .deterministicSignatures(true)
              .useHardwareAcceleration(true)
              .build();

      assertTrue(options.isPrehashed(), "Should prehash message");
      assertEquals(HashAlgorithm.SHA_256, options.getPrehashAlgorithm(), "Should use SHA-256");
      assertTrue(options.useDeterministicSignatures(), "Should use deterministic ECDSA");
      assertTrue(options.useHardwareAcceleration(), "Should use hardware acceleration");
    }

    @Test
    @DisplayName("should support Ed25519 pure mode pattern")
    void shouldSupportEd25519PureModePattern() {
      // Ed25519 pure mode - no prehashing, message signed directly
      final SignatureOptions options =
          SignatureOptions.builder().useHardwareAcceleration(true).build();

      assertFalse(options.isPrehashed(), "Ed25519 pure mode should not prehash");
      assertNull(options.getPrehashAlgorithm(), "Should have no prehash algorithm");
    }

    @Test
    @DisplayName("should support Ed25519ph mode pattern")
    void shouldSupportEd25519phModePattern() {
      // Ed25519ph mode - prehash with SHA-512
      final SignatureOptions options =
          SignatureOptions.builder().prehashed(HashAlgorithm.SHA_512).build();

      assertTrue(options.isPrehashed(), "Ed25519ph should prehash");
      assertEquals(HashAlgorithm.SHA_512, options.getPrehashAlgorithm(), "Should use SHA-512");
    }

    @Test
    @DisplayName("should support software-only signing")
    void shouldSupportSoftwareOnlySigning() {
      // Pattern for environments without hardware crypto support
      final SignatureOptions options =
          SignatureOptions.builder()
              .useHardwareAcceleration(false)
              .deterministicSignatures(true)
              .build();

      assertFalse(options.useHardwareAcceleration(), "Should disable hardware acceleration");
      assertTrue(options.useDeterministicSignatures(), "Should still use deterministic signatures");
    }

    @Test
    @DisplayName("should support randomized signatures for compliance")
    void shouldSupportRandomizedSignaturesForCompliance() {
      // Some regulatory requirements mandate randomized signatures
      final SignatureOptions options =
          SignatureOptions.builder()
              .prehashed(HashAlgorithm.SHA_256)
              .deterministicSignatures(false)
              .build();

      assertFalse(
          options.useDeterministicSignatures(), "Should use randomized signatures when required");
    }
  }

  @Nested
  @DisplayName("Hash Algorithm Integration Tests")
  class HashAlgorithmIntegrationTests {

    @Test
    @DisplayName("should support SHA-2 family for prehashing")
    void shouldSupportSha2FamilyForPrehashing() {
      final HashAlgorithm[] sha2Family = {
        HashAlgorithm.SHA_256, HashAlgorithm.SHA_384, HashAlgorithm.SHA_512
      };

      for (final HashAlgorithm algorithm : sha2Family) {
        final SignatureOptions options = SignatureOptions.builder().prehashed(algorithm).build();

        assertTrue(options.isPrehashed(), "Should be prehashed with " + algorithm);
        assertEquals(
            algorithm,
            options.getPrehashAlgorithm(),
            "Should have correct algorithm: " + algorithm);
      }
    }

    @Test
    @DisplayName("should support SHA-3 family for prehashing")
    void shouldSupportSha3FamilyForPrehashing() {
      final HashAlgorithm[] sha3Family = {
        HashAlgorithm.SHA3_256, HashAlgorithm.SHA3_384, HashAlgorithm.SHA3_512
      };

      for (final HashAlgorithm algorithm : sha3Family) {
        final SignatureOptions options = SignatureOptions.builder().prehashed(algorithm).build();

        assertTrue(options.isPrehashed(), "Should be prehashed with " + algorithm);
        assertEquals(
            algorithm,
            options.getPrehashAlgorithm(),
            "Should have correct algorithm: " + algorithm);
      }
    }
  }

  @Nested
  @DisplayName("Immutability Tests")
  class ImmutabilityTests {

    @Test
    @DisplayName("SignatureOptions should be effectively immutable")
    void signatureOptionsShouldBeEffectivelyImmutable() {
      final SignatureOptions options =
          SignatureOptions.builder()
              .prehashed(HashAlgorithm.SHA_256)
              .useHardwareAcceleration(true)
              .deterministicSignatures(true)
              .build();

      // All getters return primitives, immutable objects, or null
      // No way to modify the internal state
      assertTrue(options.isPrehashed(), "Prehashed should be stable");
      assertEquals(
          HashAlgorithm.SHA_256, options.getPrehashAlgorithm(), "Algorithm should be stable");
      assertTrue(options.useHardwareAcceleration(), "Hardware acceleration should be stable");
      assertTrue(options.useDeterministicSignatures(), "Deterministic signatures should be stable");
    }
  }
}
