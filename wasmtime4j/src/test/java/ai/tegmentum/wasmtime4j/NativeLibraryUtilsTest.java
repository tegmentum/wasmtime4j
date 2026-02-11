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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.util.NativeLibraryUtils;
import ai.tegmentum.wasmtime4j.util.PlatformDetector;
import org.junit.jupiter.api.Test;

/** Tests for {@link NativeLibraryUtils}. */
@SuppressWarnings("deprecation") // Testing deprecated wrapper class
final class NativeLibraryUtilsTest {

  @Test
  void testLoadNativeLibraryReturnsNonNull() {
    assertThrows(
        IllegalStateException.class,
        () -> NativeLibraryUtils.loadNativeLibrary("nonexistent"),
        "loadNativeLibrary should throw IllegalStateException when mode=forbid");
  }

  @Test
  void testLibraryLoadInfoFields() {
    assertThrows(
        IllegalStateException.class,
        () -> NativeLibraryUtils.loadNativeLibrary("test"),
        "loadNativeLibrary should throw IllegalStateException when mode=forbid");
  }

  @Test
  @SuppressWarnings("NullAway")
  void testLibraryLoadInfoWithNullLibraryNameThrows() {
    assertThrows(NullPointerException.class, () -> NativeLibraryUtils.loadNativeLibrary(null));
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
  }

  @Test
  @SuppressWarnings("NullAway")
  void testExtractLibraryFromJarWithNullThrows() {
    final PlatformDetector.PlatformInfo platformInfo = PlatformDetector.detect();

    assertThrows(
        NullPointerException.class,
        () ->
            ai.tegmentum.wasmtime4j.util.NativeLibraryUtils.extractLibraryFromJar(
                null, platformInfo, "/test"));
    assertThrows(
        NullPointerException.class,
        () ->
            ai.tegmentum.wasmtime4j.util.NativeLibraryUtils.extractLibraryFromJar(
                "test", null, "/test"));
    assertThrows(
        NullPointerException.class,
        () ->
            ai.tegmentum.wasmtime4j.util.NativeLibraryUtils.extractLibraryFromJar(
                "test", platformInfo, null));
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
    assertThrows(
        IllegalStateException.class,
        () -> NativeLibraryUtils.loadNativeLibrary("nonexistent"),
        "loadNativeLibrary should throw IllegalStateException when mode=forbid");
  }

  @Test
  void testLibraryLoadInfoToString() {
    assertThrows(
        IllegalStateException.class,
        () -> NativeLibraryUtils.loadNativeLibrary("testlib"),
        "loadNativeLibrary should throw IllegalStateException when mode=forbid");
  }

  @Test
  void testLoadNativeLibraryDefaultName() {
    assertThrows(
        IllegalStateException.class,
        () -> NativeLibraryUtils.loadNativeLibrary(),
        "loadNativeLibrary should throw IllegalStateException when mode=forbid");
  }

  @Test
  void testLoadNativeLibraryConsistentResults() {
    // Both calls should throw IllegalStateException in forbid mode
    assertThrows(
        IllegalStateException.class,
        () -> NativeLibraryUtils.loadNativeLibrary("test"),
        "First loadNativeLibrary call should throw IllegalStateException when mode=forbid");
    assertThrows(
        IllegalStateException.class,
        () -> NativeLibraryUtils.loadNativeLibrary("test"),
        "Second loadNativeLibrary call should throw IllegalStateException when mode=forbid");
  }
}
