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

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NativeLibraryUtils}.
 */
final class NativeLibraryUtilsTest {

  @Test
  void testLoadNativeLibraryReturnsNonNull() {
    final NativeLibraryUtils.LibraryLoadInfo info = NativeLibraryUtils.loadNativeLibrary("nonexistent");
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
  void testLibraryLoadInfoWithNullLibraryNameThrows() {
    assertThrows(NullPointerException.class, 
        () -> NativeLibraryUtils.loadNativeLibrary(null));
  }

  @Test
  void testCheckResourceExistsWithNull() {
    assertFalse(NativeLibraryUtils.checkResourceExists(null), 
        "Null resource path should return false");
  }

  @Test
  void testCheckResourceExistsWithNonexistent() {
    assertFalse(NativeLibraryUtils.checkResourceExists("/nonexistent/resource"), 
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
  void testExtractLibraryFromJarWithNullThrows() {
    final PlatformDetector.PlatformInfo platformInfo = PlatformDetector.detect();
    
    assertThrows(NullPointerException.class, 
        () -> NativeLibraryUtils.extractLibraryFromJar(null, platformInfo, "/test"));
    assertThrows(NullPointerException.class, 
        () -> NativeLibraryUtils.extractLibraryFromJar("test", null, "/test"));
    assertThrows(NullPointerException.class, 
        () -> NativeLibraryUtils.extractLibraryFromJar("test", platformInfo, null));
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
            null);
    
    assertTrue(successInfo.isSuccessful(), "Load info should be successful with no error and valid method");
    
    // Test failed load info
    final NativeLibraryUtils.LibraryLoadInfo failedInfo = 
        new NativeLibraryUtils.LibraryLoadInfo(
            "test", 
            platformInfo, 
            "/test/path", 
            false, 
            null, 
            null, 
            new RuntimeException("Test error"));
    
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
            null);
    
    final String successString = successInfo.toString();
    assertNotNull(successString, "toString should not return null");
    assertTrue(successString.contains(platformInfo.getPlatformId()), "toString should contain platform ID");
    assertTrue(successString.contains("SYSTEM_LIBRARY_PATH"), "toString should contain loading method");
    
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
            testError);
    
    final String failedString = failedInfo.toString();
    assertNotNull(failedString, "toString should not return null");
    assertTrue(failedString.contains(platformInfo.getPlatformId()), "toString should contain platform ID");
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
    assertEquals(info1.isFoundInResources(), info2.isFoundInResources(), "Resource found status should match");
    
    if (info1.getPlatformInfo() != null && info2.getPlatformInfo() != null) {
      assertEquals(info1.getPlatformInfo().getPlatformId(), 
                  info2.getPlatformInfo().getPlatformId(), "Platform IDs should match");
    }
  }
}