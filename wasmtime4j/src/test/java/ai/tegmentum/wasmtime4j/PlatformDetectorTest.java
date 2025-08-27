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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PlatformDetector}.
 */
final class PlatformDetectorTest {

  @Test
  void testDetectReturnsNonNull() {
    final PlatformDetector.PlatformInfo platformInfo = PlatformDetector.detect();
    assertNotNull(platformInfo, "Platform detection should never return null");
  }

  @Test
  void testDetectIsCached() {
    final PlatformDetector.PlatformInfo first = PlatformDetector.detect();
    final PlatformDetector.PlatformInfo second = PlatformDetector.detect();
    assertSame(first, second, "Platform detection should return the same instance");
  }

  @Test
  void testPlatformInfoFields() {
    final PlatformDetector.PlatformInfo info = PlatformDetector.detect();
    
    assertNotNull(info.getOperatingSystem(), "Operating system should not be null");
    assertNotNull(info.getArchitecture(), "Architecture should not be null");
    assertNotNull(info.getPlatformId(), "Platform ID should not be null");
    
    // Platform ID should match expected format
    final String expectedId = info.getOperatingSystem().getName() + "-" + info.getArchitecture().getName();
    assertEquals(expectedId, info.getPlatformId(), "Platform ID should match expected format");
  }

  @Test
  void testOperatingSystemProperties() {
    for (final PlatformDetector.OperatingSystem os : PlatformDetector.OperatingSystem.values()) {
      assertNotNull(os.getName(), "OS name should not be null");
      assertFalse(os.getName().isEmpty(), "OS name should not be empty");
      assertNotNull(os.getLibraryExtension(), "Library extension should not be null");
      assertFalse(os.getLibraryExtension().isEmpty(), "Library extension should not be empty");
      assertTrue(os.getLibraryExtension().startsWith("."), "Library extension should start with dot");
      assertNotNull(os.getLibraryPrefix(), "Library prefix should not be null");
    }
  }

  @Test
  void testArchitectureProperties() {
    for (final PlatformDetector.Architecture arch : PlatformDetector.Architecture.values()) {
      assertNotNull(arch.getName(), "Architecture name should not be null");
      assertFalse(arch.getName().isEmpty(), "Architecture name should not be empty");
    }
  }

  @Test
  void testGetLibraryFileName() {
    final PlatformDetector.PlatformInfo info = PlatformDetector.detect();
    final String fileName = info.getLibraryFileName("wasmtime4j");
    
    assertNotNull(fileName, "Library file name should not be null");
    assertFalse(fileName.isEmpty(), "Library file name should not be empty");
    assertTrue(fileName.contains("wasmtime4j"), "Library file name should contain library name");
    assertTrue(fileName.endsWith(info.getOperatingSystem().getLibraryExtension()),
        "Library file name should end with correct extension");
    
    if (!info.getOperatingSystem().getLibraryPrefix().isEmpty()) {
      assertTrue(fileName.startsWith(info.getOperatingSystem().getLibraryPrefix()),
          "Library file name should start with correct prefix");
    }
  }

  @Test
  void testGetLibraryResourcePath() {
    final PlatformDetector.PlatformInfo info = PlatformDetector.detect();
    final String resourcePath = info.getLibraryResourcePath("wasmtime4j");
    
    assertNotNull(resourcePath, "Library resource path should not be null");
    assertFalse(resourcePath.isEmpty(), "Library resource path should not be empty");
    assertTrue(resourcePath.startsWith("/natives/"), "Resource path should start with /natives/");
    assertTrue(resourcePath.contains(info.getPlatformId()), "Resource path should contain platform ID");
    assertTrue(resourcePath.endsWith(info.getLibraryFileName("wasmtime4j")),
        "Resource path should end with library file name");
  }

  @Test
  void testGetLibraryFileNameWithNullThrows() {
    final PlatformDetector.PlatformInfo info = PlatformDetector.detect();
    assertThrows(NullPointerException.class, () -> info.getLibraryFileName(null));
  }

  @Test
  void testGetLibraryResourcePathWithNullThrows() {
    final PlatformDetector.PlatformInfo info = PlatformDetector.detect();
    assertThrows(NullPointerException.class, () -> info.getLibraryResourcePath(null));
  }

  @Test
  void testPlatformInfoEqualsAndHashCode() {
    final PlatformDetector.PlatformInfo info1 = PlatformDetector.detect();
    final PlatformDetector.PlatformInfo info2 = PlatformDetector.detect();
    
    assertEquals(info1, info2, "Same platform info should be equal");
    assertEquals(info1.hashCode(), info2.hashCode(), "Same platform info should have same hash code");
  }

  @Test
  void testPlatformInfoToString() {
    final PlatformDetector.PlatformInfo info = PlatformDetector.detect();
    final String string = info.toString();
    
    assertNotNull(string, "toString should not return null");
    assertFalse(string.isEmpty(), "toString should not return empty string");
    assertEquals(info.getPlatformId(), string, "toString should return platform ID");
  }

  @Test
  void testIsPlatformSupported() {
    assertTrue(PlatformDetector.isPlatformSupported(), "Current platform should be supported");
  }

  @Test
  void testGetPlatformDescription() {
    final String description = PlatformDetector.getPlatformDescription();
    
    assertNotNull(description, "Platform description should not be null");
    assertFalse(description.isEmpty(), "Platform description should not be empty");
    assertTrue(description.contains("Platform:"), "Description should contain platform label");
    assertTrue(description.contains("Java:"), "Description should contain Java version");
  }

  @Test
  void testDetectOperatingSystem() {
    final PlatformDetector.OperatingSystem os = PlatformDetector.detectOperatingSystem();
    assertNotNull(os, "Operating system detection should not return null");
  }

  @Test
  void testDetectArchitecture() {
    final PlatformDetector.Architecture arch = PlatformDetector.detectArchitecture();
    assertNotNull(arch, "Architecture detection should not return null");
  }

  @Test
  void testLibraryExtensions() {
    // Verify expected extensions for each OS
    assertEquals(".so", PlatformDetector.OperatingSystem.LINUX.getLibraryExtension());
    assertEquals(".dll", PlatformDetector.OperatingSystem.WINDOWS.getLibraryExtension());
    assertEquals(".dylib", PlatformDetector.OperatingSystem.MACOS.getLibraryExtension());
  }

  @Test
  void testLibraryPrefixes() {
    // Verify expected prefixes for each OS
    assertEquals("lib", PlatformDetector.OperatingSystem.LINUX.getLibraryPrefix());
    assertEquals("", PlatformDetector.OperatingSystem.WINDOWS.getLibraryPrefix());
    assertEquals("lib", PlatformDetector.OperatingSystem.MACOS.getLibraryPrefix());
  }

  @Test
  void testOperatingSystemNames() {
    // Verify expected names for each OS
    assertEquals("linux", PlatformDetector.OperatingSystem.LINUX.getName());
    assertEquals("windows", PlatformDetector.OperatingSystem.WINDOWS.getName());
    assertEquals("macos", PlatformDetector.OperatingSystem.MACOS.getName());
  }

  @Test
  void testArchitectureNames() {
    // Verify expected names for each architecture
    assertEquals("x86_64", PlatformDetector.Architecture.X86_64.getName());
    assertEquals("aarch64", PlatformDetector.Architecture.AARCH64.getName());
  }
}