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
package ai.tegmentum.wasmtime4j.jni.nativelib;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link NativeLibraryLoader}. */
class NativeLibraryLoaderTest {

  @Test
  void testGetLibraryResourcePath() {
    final String resourcePath = NativeLibraryLoader.getLibraryResourcePath();

    assertNotNull(resourcePath);
    assertTrue(
        resourcePath.startsWith("/natives/"), "Expected resource path to start with: /natives/");
    assertTrue(
        resourcePath.contains("wasmtime4j"), "Expected resource path to contain: wasmtime4j");

    // Should contain platform and architecture
    if (System.getProperty("os.name").toLowerCase().contains("mac")) {
      assertTrue(resourcePath.contains("darwin"), "Expected resource path to contain: darwin");
      assertTrue(resourcePath.endsWith(".dylib"), "Expected resource path to end with: .dylib");
    } else if (System.getProperty("os.name").toLowerCase().contains("linux")) {
      assertTrue(resourcePath.contains("linux"), "Expected resource path to contain: linux");
      assertTrue(resourcePath.endsWith(".so"), "Expected resource path to end with: .so");
    } else if (System.getProperty("os.name").toLowerCase().contains("windows")) {
      assertTrue(resourcePath.contains("windows"), "Expected resource path to contain: windows");
      assertTrue(resourcePath.endsWith(".dll"), "Expected resource path to end with: .dll");
    }

    // Should contain architecture
    final String arch = System.getProperty("os.arch").toLowerCase();
    if (arch.equals("amd64") || arch.equals("x86_64")) {
      assertTrue(resourcePath.contains("x86_64"), "Expected resource path to contain: x86_64");
    } else if (arch.equals("aarch64") || arch.equals("arm64")) {
      assertTrue(resourcePath.contains("aarch64"), "Expected resource path to contain: aarch64");
    }
  }

  @Test
  void testGetPlatformInfo() {
    final String platformInfo = NativeLibraryLoader.getPlatformInfo();

    assertNotNull(platformInfo);
    assertTrue(platformInfo.contains("Platform:"), "Expected platform info to contain: Platform:");
    assertTrue(
        platformInfo.contains("Library path:"), "Expected platform info to contain: Library path:");
    assertTrue(platformInfo.contains("Loaded:"), "Expected platform info to contain: Loaded:");

    // Should contain current platform info
    if (System.getProperty("os.name").toLowerCase().contains("mac")) {
      assertTrue(platformInfo.contains("darwin"), "Expected platform info to contain: darwin");
    } else if (System.getProperty("os.name").toLowerCase().contains("linux")) {
      assertTrue(platformInfo.contains("linux"), "Expected platform info to contain: linux");
    } else if (System.getProperty("os.name").toLowerCase().contains("windows")) {
      assertTrue(platformInfo.contains("windows"), "Expected platform info to contain: windows");
    }
  }

  @Test
  void testLoadLibraryIdempotent() {
    // Should not throw exception even if called multiple times
    // Note: This will likely fail because the actual native library doesn't exist yet,
    // but it tests the method structure and exception handling

    // First call - may fail but shouldn't crash
    try {
      NativeLibraryLoader.loadLibrary();
      // If it succeeds, subsequent calls should also succeed
      assertDoesNotThrow(() -> NativeLibraryLoader.loadLibrary());
      assertTrue(NativeLibraryLoader.isLibraryLoaded());
    } catch (RuntimeException e) {
      // Expected if native library doesn't exist
      assertTrue(
          e.getMessage().contains("Failed to load native library"),
          "Expected message to contain: Failed to load native library");
      assertFalse(NativeLibraryLoader.isLibraryLoaded());
    }
  }

  @Test
  void testUtilityClassCannotBeInstantiated() {
    // Ensure utility class cannot be instantiated
    assertThrows(
        AssertionError.class,
        () -> {
          try {
            final java.lang.reflect.Constructor<?> constructor =
                NativeLibraryLoader.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
          } catch (Exception e) {
            if (e.getCause() instanceof AssertionError) {
              throw (AssertionError) e.getCause();
            }
            throw new RuntimeException(e);
          }
        });
  }

  @Test
  void testResourcePathFormat() {
    final String resourcePath = NativeLibraryLoader.getLibraryResourcePath();

    // Should follow expected format: /natives/{os}-{arch}/wasmtime4j{extension}
    final String[] parts = resourcePath.split("/");
    assertTrue(
        parts.length >= 3, "Expected resource path to have at least 3 parts, got: " + parts.length);
    assertEquals("natives", parts[1]);

    // Last part should be the library file
    final String libraryFile = parts[parts.length - 1];
    assertTrue(libraryFile.contains("wasmtime4j"), "Expected library file to contain: wasmtime4j");
    assertTrue(
        libraryFile.contains(".so")
            || libraryFile.contains(".dll")
            || libraryFile.contains(".dylib"),
        "Expected library file to contain one of: .so, .dll, .dylib");

    // Check platform-specific naming conventions
    if (libraryFile.endsWith(".dll")) {
      // Windows: wasmtime4j.dll (no prefix)
      assertTrue(
          libraryFile.startsWith("wasmtime4j"),
          "Expected Windows library to start with: wasmtime4j");
    } else {
      // Unix-like: libwasmtime4j.so/.dylib (with lib prefix)
      assertTrue(libraryFile.startsWith("lib"), "Expected Unix library to start with: lib");
    }
  }

  @Test
  void testPlatformDetection() {
    // Test that platform detection doesn't throw exceptions
    assertDoesNotThrow(() -> NativeLibraryLoader.getLibraryResourcePath());
    assertDoesNotThrow(() -> NativeLibraryLoader.getPlatformInfo());
  }
}
