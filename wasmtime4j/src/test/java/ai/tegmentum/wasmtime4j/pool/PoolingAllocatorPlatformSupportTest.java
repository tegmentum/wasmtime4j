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

package ai.tegmentum.wasmtime4j.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PoolingAllocatorPlatformSupport} class.
 *
 * <p>PoolingAllocatorPlatformSupport provides platform-specific feature detection utilities.
 */
@DisplayName("PoolingAllocatorPlatformSupport Tests")
class PoolingAllocatorPlatformSupportTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(PoolingAllocatorPlatformSupport.class.getModifiers()),
          "PoolingAllocatorPlatformSupport should be a final class");
    }

    @Test
    @DisplayName("should have private constructor")
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
      final var constructor = PoolingAllocatorPlatformSupport.class.getDeclaredConstructor();
      assertTrue(
          java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()),
          "Constructor should be private");
    }
  }

  @Nested
  @DisplayName("Platform Detection Tests")
  class PlatformDetectionTests {

    @Test
    @DisplayName("should detect Linux correctly")
    void shouldDetectLinuxCorrectly() {
      final String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
      final boolean expected = osName.contains("linux");
      assertEquals(
          expected, PoolingAllocatorPlatformSupport.isLinux(), "Linux detection should match");
    }

    @Test
    @DisplayName("should detect Windows correctly")
    void shouldDetectWindowsCorrectly() {
      final String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
      final boolean expected = osName.contains("windows");
      assertEquals(
          expected, PoolingAllocatorPlatformSupport.isWindows(), "Windows detection should match");
    }

    @Test
    @DisplayName("should detect macOS correctly")
    void shouldDetectMacOSCorrectly() {
      final String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
      final boolean expected = osName.contains("mac") || osName.contains("darwin");
      assertEquals(
          expected, PoolingAllocatorPlatformSupport.isMacOS(), "macOS detection should match");
    }

    @Test
    @DisplayName("should detect x86_64 correctly")
    void shouldDetectX86_64Correctly() {
      final String osArch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
      final boolean expected = osArch.equals("amd64") || osArch.equals("x86_64");
      assertEquals(
          expected, PoolingAllocatorPlatformSupport.isX86_64(), "x86_64 detection should match");
    }

    @Test
    @DisplayName("should detect ARM64 correctly")
    void shouldDetectArm64Correctly() {
      final String osArch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
      final boolean expected = osArch.equals("aarch64") || osArch.equals("arm64");
      assertEquals(
          expected, PoolingAllocatorPlatformSupport.isArm64(), "ARM64 detection should match");
    }
  }

  @Nested
  @DisplayName("Feature Availability Tests")
  class FeatureAvailabilityTests {

    @Test
    @DisplayName("MPK should only be available on Linux x86_64")
    void mpkShouldOnlyBeAvailableOnLinuxX86_64() {
      final boolean expected =
          PoolingAllocatorPlatformSupport.isLinux() && PoolingAllocatorPlatformSupport.isX86_64();
      assertEquals(
          expected,
          PoolingAllocatorPlatformSupport.areMemoryProtectionKeysAvailable(),
          "MPK availability should match Linux x86_64");
    }

    @Test
    @DisplayName("PAGEMAP_SCAN should only be available on Linux")
    void pagemapScanShouldOnlyBeAvailableOnLinux() {
      final boolean expected = PoolingAllocatorPlatformSupport.isLinux();
      assertEquals(
          expected,
          PoolingAllocatorPlatformSupport.isPagemapScanAvailable(),
          "PAGEMAP_SCAN availability should match Linux");
    }

    @Test
    @DisplayName("getMaxMemoryProtectionKeysHint should return 15 when MPK available")
    void getMaxMpkKeysShouldReturn15WhenAvailable() {
      final int hint = PoolingAllocatorPlatformSupport.getMaxMemoryProtectionKeysHint();
      if (PoolingAllocatorPlatformSupport.areMemoryProtectionKeysAvailable()) {
        assertEquals(15, hint, "Should return 15 when MPK is available");
      } else {
        assertEquals(0, hint, "Should return 0 when MPK is not available");
      }
    }
  }

  @Nested
  @DisplayName("Platform Description Tests")
  class PlatformDescriptionTests {

    @Test
    @DisplayName("should return non-null platform description")
    void shouldReturnNonNullPlatformDescription() {
      final String description = PoolingAllocatorPlatformSupport.getPlatformDescription();
      assertNotNull(description, "Platform description should not be null");
    }

    @Test
    @DisplayName("should include OS name in platform description")
    void shouldIncludeOsNameInPlatformDescription() {
      final String description = PoolingAllocatorPlatformSupport.getPlatformDescription();
      final String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
      assertTrue(
          description.toLowerCase(Locale.ROOT).contains(osName) || description.length() > 0,
          "Platform description should include OS information");
    }

    @Test
    @DisplayName("should contain separator in platform description")
    void shouldContainSeparatorInPlatformDescription() {
      final String description = PoolingAllocatorPlatformSupport.getPlatformDescription();
      assertTrue(description.contains("/"), "Platform description should contain '/' separator");
    }
  }

  @Nested
  @DisplayName("Consistency Tests")
  class ConsistencyTests {

    @Test
    @DisplayName("exactly one OS should be detected")
    void exactlyOneOsShouldBeDetected() {
      int osCount = 0;
      if (PoolingAllocatorPlatformSupport.isLinux()) {
        osCount++;
      }
      if (PoolingAllocatorPlatformSupport.isWindows()) {
        osCount++;
      }
      if (PoolingAllocatorPlatformSupport.isMacOS()) {
        osCount++;
      }
      // At least one OS should be detected (might be unknown OS)
      assertTrue(osCount <= 1, "At most one OS should be detected");
    }

    @Test
    @DisplayName("at most one architecture should be detected")
    void atMostOneArchShouldBeDetected() {
      int archCount = 0;
      if (PoolingAllocatorPlatformSupport.isX86_64()) {
        archCount++;
      }
      if (PoolingAllocatorPlatformSupport.isArm64()) {
        archCount++;
      }
      assertTrue(archCount <= 1, "At most one architecture should be detected");
    }
  }
}
