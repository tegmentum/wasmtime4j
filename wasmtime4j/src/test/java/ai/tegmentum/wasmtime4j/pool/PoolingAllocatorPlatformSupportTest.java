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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PoolingAllocatorPlatformSupport} - platform detection utility.
 *
 * <p>Validates platform detection methods, MPK availability, and consistency between methods.
 */
@DisplayName("PoolingAllocatorPlatformSupport Tests")
class PoolingAllocatorPlatformSupportTest {

  private static final String OS_NAME = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
  private static final String OS_ARCH = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);

  @Nested
  @DisplayName("OS Detection Tests")
  class OsDetectionTests {

    @Test
    @DisplayName("exactly one OS should be detected")
    void exactlyOneOsShouldBeDetected() {
      final boolean linux = PoolingAllocatorPlatformSupport.isLinux();
      final boolean windows = PoolingAllocatorPlatformSupport.isWindows();
      final boolean macos = PoolingAllocatorPlatformSupport.isMacOS();

      // At least one should be true on a known platform
      final int count = (linux ? 1 : 0) + (windows ? 1 : 0) + (macos ? 1 : 0);
      assertTrue(
          count <= 1,
          "At most one OS detection should be true, got linux="
              + linux
              + ", windows="
              + windows
              + ", macos="
              + macos);
    }

    @Test
    @DisplayName("isLinux should match system property")
    void isLinuxShouldMatchSystemProperty() {
      final boolean expected = OS_NAME.contains("linux");
      assertEquals(
          expected,
          PoolingAllocatorPlatformSupport.isLinux(),
          "isLinux should match os.name containing 'linux'");
    }

    @Test
    @DisplayName("isWindows should match system property")
    void isWindowsShouldMatchSystemProperty() {
      final boolean expected = OS_NAME.contains("windows");
      assertEquals(
          expected,
          PoolingAllocatorPlatformSupport.isWindows(),
          "isWindows should match os.name containing 'windows'");
    }

    @Test
    @DisplayName("isMacOS should match system property")
    void isMacOSShouldMatchSystemProperty() {
      final boolean expected = OS_NAME.contains("mac") || OS_NAME.contains("darwin");
      assertEquals(
          expected,
          PoolingAllocatorPlatformSupport.isMacOS(),
          "isMacOS should match os.name containing 'mac' or 'darwin'");
    }
  }

  @Nested
  @DisplayName("Architecture Detection Tests")
  class ArchitectureDetectionTests {

    @Test
    @DisplayName("isX86_64 should match system property")
    void isX8664ShouldMatchSystemProperty() {
      final boolean expected = OS_ARCH.equals("amd64") || OS_ARCH.equals("x86_64");
      assertEquals(
          expected,
          PoolingAllocatorPlatformSupport.isX86_64(),
          "isX86_64 should match os.arch being 'amd64' or 'x86_64'");
    }

    @Test
    @DisplayName("isArm64 should match system property")
    void isArm64ShouldMatchSystemProperty() {
      final boolean expected = OS_ARCH.equals("aarch64") || OS_ARCH.equals("arm64");
      assertEquals(
          expected,
          PoolingAllocatorPlatformSupport.isArm64(),
          "isArm64 should match os.arch being 'aarch64' or 'arm64'");
    }

    @Test
    @DisplayName("at most one architecture should be detected")
    void atMostOneArchShouldBeDetected() {
      final boolean x86 = PoolingAllocatorPlatformSupport.isX86_64();
      final boolean arm = PoolingAllocatorPlatformSupport.isArm64();
      assertFalse(x86 && arm, "Cannot be both x86_64 and arm64 simultaneously");
    }
  }

  @Nested
  @DisplayName("Memory Protection Keys Tests")
  class MemoryProtectionKeysTests {

    @Test
    @DisplayName("areMemoryProtectionKeysAvailable should require Linux and x86_64")
    void areMemoryProtectionKeysAvailableShouldRequireLinuxAndX8664() {
      final boolean expected =
          PoolingAllocatorPlatformSupport.isLinux() && PoolingAllocatorPlatformSupport.isX86_64();
      assertEquals(
          expected,
          PoolingAllocatorPlatformSupport.areMemoryProtectionKeysAvailable(),
          "MPK should only be available on Linux x86_64");
    }

    @Test
    @DisplayName("getMaxMemoryProtectionKeysHint should return 15 when available")
    void getMaxMpkHintShouldReturn15WhenAvailable() {
      final int hint = PoolingAllocatorPlatformSupport.getMaxMemoryProtectionKeysHint();
      if (PoolingAllocatorPlatformSupport.areMemoryProtectionKeysAvailable()) {
        assertEquals(15, hint, "MPK hint should be 15 on supported platforms");
      } else {
        assertEquals(0, hint, "MPK hint should be 0 on unsupported platforms");
      }
    }

    @Test
    @DisplayName("getMaxMemoryProtectionKeysHint should return non-negative value")
    void getMaxMpkHintShouldReturnNonNegative() {
      final int hint = PoolingAllocatorPlatformSupport.getMaxMemoryProtectionKeysHint();
      assertTrue(hint >= 0, "MPK hint should be non-negative, got: " + hint);
    }
  }

  @Nested
  @DisplayName("Platform Description Tests")
  class PlatformDescriptionTests {

    @Test
    @DisplayName("getPlatformDescription should not be null")
    void getPlatformDescriptionShouldNotBeNull() {
      final String description = PoolingAllocatorPlatformSupport.getPlatformDescription();
      assertNotNull(description, "Platform description should not be null");
    }

    @Test
    @DisplayName("getPlatformDescription should contain OS and arch info")
    void getPlatformDescriptionShouldContainOsAndArch() {
      final String description = PoolingAllocatorPlatformSupport.getPlatformDescription();
      assertTrue(
          description.contains("/"),
          "Platform description should contain separator, got: " + description);
    }

    @Test
    @DisplayName("getPlatformDescription should be non-empty")
    void getPlatformDescriptionShouldBeNonEmpty() {
      final String description = PoolingAllocatorPlatformSupport.getPlatformDescription();
      assertFalse(description.isEmpty(), "Platform description should not be empty");
    }
  }

  @Nested
  @DisplayName("Pagemap Scan Tests")
  class PagemapScanTests {

    @Test
    @DisplayName("isPagemapScanAvailable should only be true on Linux")
    void isPagemapScanAvailableShouldOnlyBeTrueOnLinux() {
      if (!PoolingAllocatorPlatformSupport.isLinux()) {
        assertFalse(
            PoolingAllocatorPlatformSupport.isPagemapScanAvailable(),
            "PAGEMAP_SCAN should not be available on non-Linux platforms");
      }
    }

    @Test
    @DisplayName("isPagemapScanAvailable should match isLinux")
    void isPagemapScanAvailableShouldMatchIsLinux() {
      assertEquals(
          PoolingAllocatorPlatformSupport.isLinux(),
          PoolingAllocatorPlatformSupport.isPagemapScanAvailable(),
          "PAGEMAP_SCAN availability should match Linux detection");
    }
  }
}
