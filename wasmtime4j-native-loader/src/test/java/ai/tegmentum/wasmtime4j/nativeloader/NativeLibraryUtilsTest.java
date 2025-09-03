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

package ai.tegmentum.wasmtime4j.nativeloader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Tests for {@link NativeLibraryUtils}. */
final class NativeLibraryUtilsTest {

  @Test
  void testLoadNativeLibraryReturnsNonNull() {
    final NativeLibraryUtils.LibraryLoadInfo info =
        NativeLibraryUtils.loadNativeLibrary("nonexistent");
    assertNotNull(info, "Library load info should never be null");
  }

  @Test
  void testLibraryLoadInfoFields() {
    final NativeLibraryUtils.LibraryLoadInfo info = NativeLibraryUtils.loadNativeLibrary("test");

    assertNotNull(info.getLibraryName(), "Library name should not be null");
    assertEquals("test", info.getLibraryName(), "Library name should match requested");

    if (info.getPlatformInfo() != null) {
      assertNotNull(info.getPlatformInfo().getPlatformId(), "Platform ID should not be null");
      assertNotNull(info.getResourcePath(), "Resource path should not be null");
    }
  }

  @Test
  @SuppressWarnings("NullAway")
  void testLibraryLoadInfoWithNullLibraryNameThrows() {
    assertThrows(
        NullPointerException.class, () -> NativeLibraryUtils.loadNativeLibrary((String) null));
  }

  @Test
  @SuppressWarnings("NullAway")
  void testLibraryLoadInfoWithNullConfigThrows() {
    assertThrows(
        NullPointerException.class,
        () -> NativeLibraryUtils.loadNativeLibrary((NativeLibraryConfig) null));
    assertThrows(
        NullPointerException.class, () -> NativeLibraryUtils.loadNativeLibrary("test", null));
  }

  @Test
  void testLoadNativeLibraryWithConfig() {
    final NativeLibraryConfig config =
        NativeLibraryConfig.builder()
            .libraryName("customlib")
            .tempFilePrefix("custom-prefix-")
            .tempDirSuffix("-custom-suffix")
            .build();

    final NativeLibraryUtils.LibraryLoadInfo info = NativeLibraryUtils.loadNativeLibrary(config);

    assertNotNull(info, "Library load info should not be null");
    assertEquals("customlib", info.getLibraryName(), "Library name should match config");
  }

  @Test
  void testLoadNativeLibraryWithConfigOverrideLibraryName() {
    final NativeLibraryConfig config =
        NativeLibraryConfig.builder()
            .libraryName("configlib")
            .tempFilePrefix("custom-prefix-")
            .tempDirSuffix("-custom-suffix")
            .build();

    final NativeLibraryUtils.LibraryLoadInfo info =
        NativeLibraryUtils.loadNativeLibrary("overridelib", config);

    assertNotNull(info, "Library load info should not be null");
    assertEquals(
        "overridelib", info.getLibraryName(), "Library name should match parameter, not config");
  }

  @Test
  void testCheckResourceExistsWithNull() {
    assertFalse(
        NativeLibraryUtils.checkResourceExists(null), "Null resource path should return false");
  }

  @Test
  void testCheckResourceExistsWithNonexistent() {
    assertFalse(
        NativeLibraryUtils.checkResourceExists("/nonexistent/resource"),
        "Nonexistent resource should return false");
  }

  @Test
  void testGetDiagnosticInfo() {
    final String diagnostics = NativeLibraryUtils.getDiagnosticInfo();

    assertNotNull(diagnostics, "Diagnostics should not be null");
    assertFalse(diagnostics.isEmpty(), "Diagnostics should not be empty");
    assertTrue(diagnostics.contains("Platform:"), "Diagnostics should contain platform info");
    assertTrue(diagnostics.contains("Library path:"), "Diagnostics should contain library path");
    assertTrue(
        diagnostics.contains("Configuration:"), "Diagnostics should contain configuration info");
  }

  @Test
  void testGetDiagnosticInfoWithConfig() {
    final NativeLibraryConfig config =
        NativeLibraryConfig.builder()
            .libraryName("testlib")
            .tempFilePrefix("test-prefix-")
            .tempDirSuffix("-test-suffix")
            .build();

    final String diagnostics = NativeLibraryUtils.getDiagnosticInfo(config);

    assertNotNull(diagnostics, "Diagnostics should not be null");
    assertFalse(diagnostics.isEmpty(), "Diagnostics should not be empty");
    assertTrue(diagnostics.contains("Platform:"), "Diagnostics should contain platform info");
    assertTrue(diagnostics.contains("Library path:"), "Diagnostics should contain library path");
    assertTrue(
        diagnostics.contains("Configuration:"), "Diagnostics should contain configuration info");
    assertTrue(diagnostics.contains("testlib"), "Diagnostics should contain custom library name");
  }

  @Test
  @SuppressWarnings("NullAway")
  void testGetDiagnosticInfoWithNullConfigThrows() {
    assertThrows(NullPointerException.class, () -> NativeLibraryUtils.getDiagnosticInfo(null));
  }

  @Test
  @SuppressWarnings("NullAway")
  void testExtractLibraryFromJarWithNullThrows() {
    final PlatformDetector.PlatformInfo platformInfo = PlatformDetector.detect();

    assertThrows(
        NullPointerException.class,
        () -> NativeLibraryUtils.extractLibraryFromJar(null, platformInfo, "/test"));
    assertThrows(
        NullPointerException.class,
        () -> NativeLibraryUtils.extractLibraryFromJar("test", null, "/test"));
    assertThrows(
        NullPointerException.class,
        () -> NativeLibraryUtils.extractLibraryFromJar("test", platformInfo, null));
  }

  @Test
  @SuppressWarnings("NullAway")
  void testExtractLibraryFromJarWithNullConfigThrows() {
    final PlatformDetector.PlatformInfo platformInfo = PlatformDetector.detect();

    assertThrows(
        NullPointerException.class,
        () -> NativeLibraryUtils.extractLibraryFromJar("test", platformInfo, "/test", null));
  }

  @Test
  void testLibraryLoadInfoLoadingMethodValues() {
    // Verify all enum values are present
    final NativeLibraryUtils.LibraryLoadInfo.LoadingMethod[] methods =
        NativeLibraryUtils.LibraryLoadInfo.LoadingMethod.values();

    assertEquals(2, methods.length, "Should have exactly 2 loading methods");
    assertNotNull(NativeLibraryUtils.LibraryLoadInfo.LoadingMethod.SYSTEM_LIBRARY_PATH);
    assertNotNull(NativeLibraryUtils.LibraryLoadInfo.LoadingMethod.EXTRACTED_FROM_JAR);
  }

  @Test
  void testLibraryLoadInfoSuccessfulCheck() {
    // Test successful load info
    final PlatformDetector.PlatformInfo platformInfo = PlatformDetector.detect();
    final NativeLibraryUtils.LibraryLoadInfo successInfo =
        new NativeLibraryUtils.LibraryLoadInfo(
            "test",
            platformInfo,
            "/test/path",
            true,
            null,
            NativeLibraryUtils.LibraryLoadInfo.LoadingMethod.SYSTEM_LIBRARY_PATH,
            null,
            null,
            new java.util.ArrayList<>());

    assertTrue(
        successInfo.isSuccessful(),
        "Load info should be successful with no error and valid method");

    // Test failed load info
    final NativeLibraryUtils.LibraryLoadInfo failedInfo =
        new NativeLibraryUtils.LibraryLoadInfo(
            "test",
            platformInfo,
            "/test/path",
            false,
            null,
            null,
            new RuntimeException("Test error"),
            null,
            new java.util.ArrayList<>());

    assertFalse(failedInfo.isSuccessful(), "Load info should not be successful with error");
  }

  @Test
  void testLibraryLoadInfoToString() {
    final PlatformDetector.PlatformInfo platformInfo = PlatformDetector.detect();

    // Test successful info toString
    final NativeLibraryUtils.LibraryLoadInfo successInfo =
        new NativeLibraryUtils.LibraryLoadInfo(
            "test",
            platformInfo,
            "/test/path",
            true,
            null,
            NativeLibraryUtils.LibraryLoadInfo.LoadingMethod.SYSTEM_LIBRARY_PATH,
            null,
            null,
            new java.util.ArrayList<>());

    final String successString = successInfo.toString();
    assertNotNull(successString, "toString should not return null");
    assertTrue(
        successString.contains(platformInfo.getPlatformId()),
        "toString should contain platform ID");
    assertTrue(
        successString.contains("SYSTEM_LIBRARY_PATH"), "toString should contain loading method");

    // Test failed info toString
    final RuntimeException testError = new RuntimeException("Test error");
    final NativeLibraryUtils.LibraryLoadInfo failedInfo =
        new NativeLibraryUtils.LibraryLoadInfo(
            "test",
            platformInfo,
            "/test/path",
            false,
            null,
            null,
            testError,
            null,
            new java.util.ArrayList<>());

    final String failedString = failedInfo.toString();
    assertNotNull(failedString, "toString should not return null");
    assertTrue(
        failedString.contains(platformInfo.getPlatformId()), "toString should contain platform ID");
    assertTrue(failedString.contains("Test error"), "toString should contain error message");
  }

  @Test
  void testLoadNativeLibraryDefaultName() {
    final NativeLibraryUtils.LibraryLoadInfo info = NativeLibraryUtils.loadNativeLibrary();

    assertNotNull(info, "Load info should not be null");
    assertEquals("wasmtime4j", info.getLibraryName(), "Default library name should be wasmtime4j");
  }

  @Test
  void testLoadNativeLibraryConsistentResults() {
    // Multiple calls with same library name should behave consistently
    final NativeLibraryUtils.LibraryLoadInfo info1 = NativeLibraryUtils.loadNativeLibrary("test");
    final NativeLibraryUtils.LibraryLoadInfo info2 = NativeLibraryUtils.loadNativeLibrary("test");

    assertEquals(info1.getLibraryName(), info2.getLibraryName(), "Library names should match");
    assertEquals(
        info1.isFoundInResources(),
        info2.isFoundInResources(),
        "Resource found status should match");

    if (info1.getPlatformInfo() != null && info2.getPlatformInfo() != null) {
      assertEquals(
          info1.getPlatformInfo().getPlatformId(),
          info2.getPlatformInfo().getPlatformId(),
          "Platform IDs should match");
    }
  }

  @Test
  void testBackwardCompatibilityWithDefaultConfig() {
    // Test that default static methods still work
    final NativeLibraryUtils.LibraryLoadInfo defaultInfo = NativeLibraryUtils.loadNativeLibrary();
    final NativeLibraryUtils.LibraryLoadInfo configInfo =
        NativeLibraryUtils.loadNativeLibrary(NativeLibraryConfig.defaultConfig());

    assertEquals(
        defaultInfo.getLibraryName(),
        configInfo.getLibraryName(),
        "Default and config methods should produce same library name");
    assertEquals(
        defaultInfo.getResourcePath(),
        configInfo.getResourcePath(),
        "Default and config methods should produce same resource path");
  }

  @Test
  void testConfigurableParametersAffectCaching() {
    final NativeLibraryConfig config1 =
        NativeLibraryConfig.builder()
            .libraryName("testlib")
            .tempFilePrefix("prefix1-")
            .tempDirSuffix("-suffix1")
            .build();

    final NativeLibraryConfig config2 =
        NativeLibraryConfig.builder()
            .libraryName("testlib")
            .tempFilePrefix("prefix2-")
            .tempDirSuffix("-suffix2")
            .build();

    // Should use different cache keys due to different configurations
    final NativeLibraryUtils.LibraryLoadInfo info1 = NativeLibraryUtils.loadNativeLibrary(config1);
    final NativeLibraryUtils.LibraryLoadInfo info2 = NativeLibraryUtils.loadNativeLibrary(config2);

    assertEquals(
        info1.getLibraryName(), info2.getLibraryName(), "Library names should be the same");
    // Note: We can't easily test that different temp directories are created without
    // actually extracting libraries, which may not happen in the test environment
  }
}
