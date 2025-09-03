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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Integration tests for {@link PlatformDetector} with other wasmtime4j modules.
 *
 * <p>This test class focuses on:
 *
 * <ul>
 *   <li>Cross-module compatibility
 *   <li>Resource path consistency
 *   <li>Platform detection integration
 *   <li>Service discovery patterns
 *   <li>Module boundary testing
 * </ul>
 *
 * <p>Note: These tests simulate integration scenarios without requiring actual JNI or Panama
 * modules to be present, making them suitable for testing in isolation.
 */
@DisplayName("Integration Tests")
final class IntegrationTest {

  @BeforeEach
  void setUp() {
    PlatformDetectorTestUtils.clearCache();
  }

  @AfterEach
  void tearDown() {
    PlatformDetectorTestUtils.clearCache();
  }

  /**
   * Provides expected resource paths for different runtime modules.
   *
   * @return stream of test arguments with runtime name, library name, and expected resource pattern
   */
  private static Stream<Arguments> provideRuntimeModuleResourcePaths() {
    return Stream.of(
        Arguments.of("jni", "wasmtime4j", "wasmtime4j"),
        Arguments.of("panama", "wasmtime4j", "wasmtime4j"),
        Arguments.of("jni", "wasmtime4j-jni", "wasmtime4j-jni"),
        Arguments.of("panama", "wasmtime4j-panama", "wasmtime4j-panama"),
        Arguments.of("shared", "wasmtime4j-native", "wasmtime4j-native"));
  }

  /**
   * Provides different module scenarios for testing.
   *
   * @return stream of module scenario names
   */
  private static Stream<String> provideModuleScenarios() {
    return Stream.of("wasmtime4j-core", "wasmtime4j-jni", "wasmtime4j-panama", "wasmtime4j-native");
  }

  @Test
  @DisplayName("Should provide consistent platform detection across module boundaries")
  void testCrossModulePlatformDetection() {
    // Simulate multiple modules requesting platform information
    final PlatformDetector.PlatformInfo coreModuleInfo = PlatformDetector.detect();
    final PlatformDetector.PlatformInfo jniModuleInfo = PlatformDetector.detect();
    final PlatformDetector.PlatformInfo panamaModuleInfo = PlatformDetector.detect();

    // All modules should get the same platform information due to caching
    assertEquals(
        coreModuleInfo,
        jniModuleInfo,
        "Core and JNI modules should see consistent platform information");
    assertEquals(
        coreModuleInfo,
        panamaModuleInfo,
        "Core and Panama modules should see consistent platform information");
    assertEquals(
        jniModuleInfo,
        panamaModuleInfo,
        "JNI and Panama modules should see consistent platform information");

    // Verify that all key properties are consistent
    assertEquals(
        coreModuleInfo.getPlatformId(),
        jniModuleInfo.getPlatformId(),
        "Platform IDs should be consistent across modules");
    assertEquals(
        coreModuleInfo.getOperatingSystem(),
        jniModuleInfo.getOperatingSystem(),
        "Operating systems should be consistent across modules");
    assertEquals(
        coreModuleInfo.getArchitecture(),
        jniModuleInfo.getArchitecture(),
        "Architectures should be consistent across modules");
  }

  @ParameterizedTest(name = "Runtime: {0}, Library: {1}")
  @MethodSource("provideRuntimeModuleResourcePaths")
  @DisplayName("Should generate correct resource paths for different runtime modules")
  void testRuntimeModuleResourcePathGeneration(
      final String runtimeName, final String libraryName, final String expectedLibraryInPath) {
    final PlatformDetector.PlatformInfo info = PlatformDetector.detect();
    final String resourcePath = info.getLibraryResourcePath(libraryName);

    assertNotNull(resourcePath, "Resource path should not be null");
    assertTrue(
        resourcePath.startsWith("/natives/"),
        "Resource path should start with /natives/ for runtime: " + runtimeName);
    assertTrue(
        resourcePath.contains(info.getPlatformId()),
        "Resource path should contain platform ID for runtime: " + runtimeName);
    assertTrue(
        resourcePath.contains(expectedLibraryInPath),
        "Resource path should contain expected library name for runtime: " + runtimeName);

    // Verify path structure consistency
    final String expectedPattern =
        "/natives/" + info.getPlatformId() + "/" + info.getLibraryFileName(libraryName);
    assertEquals(
        expectedPattern,
        resourcePath,
        "Resource path should match expected pattern for runtime: " + runtimeName);
  }

  @ParameterizedTest
  @MethodSource("provideModuleScenarios")
  @DisplayName("Should handle module-specific library naming conventions")
  void testModuleSpecificLibraryNaming(final String moduleName) {
    final PlatformDetector.PlatformInfo info = PlatformDetector.detect();
    final String fileName = info.getLibraryFileName(moduleName);
    final String resourcePath = info.getLibraryResourcePath(moduleName);

    // Verify file name follows platform conventions
    assertTrue(
        fileName.contains(moduleName), "File name should contain module name: " + moduleName);
    assertTrue(
        fileName.endsWith(info.getOperatingSystem().getLibraryExtension()),
        "File name should have correct extension for module: " + moduleName);

    if (!info.getOperatingSystem().getLibraryPrefix().isEmpty()) {
      assertTrue(
          fileName.startsWith(info.getOperatingSystem().getLibraryPrefix()),
          "File name should have correct prefix for module: " + moduleName);
    }

    // Verify resource path incorporates module name correctly
    assertTrue(
        resourcePath.contains(fileName),
        "Resource path should contain file name for module: " + moduleName);
    assertTrue(
        resourcePath.contains(info.getPlatformId()),
        "Resource path should contain platform ID for module: " + moduleName);
  }

  @Test
  @DisplayName("Should support runtime selection scenarios")
  void testRuntimeSelectionScenarios() {
    final PlatformDetector.PlatformInfo info = PlatformDetector.detect();

    // Simulate different runtime selection scenarios
    final List<String> runtimeOptions = Arrays.asList("jni", "panama", "native");

    for (final String runtime : runtimeOptions) {
      final String runtimeLibraryName = "wasmtime4j-" + runtime;
      final String fileName = info.getLibraryFileName(runtimeLibraryName);
      final String resourcePath = info.getLibraryResourcePath(runtimeLibraryName);

      // Each runtime should have consistent naming
      assertNotNull(fileName, "File name should not be null for runtime: " + runtime);
      assertNotNull(resourcePath, "Resource path should not be null for runtime: " + runtime);
      assertTrue(
          fileName.contains(runtime), "File name should contain runtime identifier: " + runtime);
      assertTrue(
          resourcePath.contains(fileName),
          "Resource path should contain file name for runtime: " + runtime);
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "wasmtime4j",
        "wasmtime4j-jni",
        "wasmtime4j-panama",
        "wasmtime4j-native",
        "custom-plugin"
      })
  @DisplayName("Should handle various library names in integration scenarios")
  void testIntegrationWithVariousLibraryNames(final String libraryName) {
    final PlatformDetector.PlatformInfo info = PlatformDetector.detect();

    // Test that platform detection works consistently for different library names
    final String fileName1 = info.getLibraryFileName(libraryName);
    final String resourcePath1 = info.getLibraryResourcePath(libraryName);

    // Call again to test caching
    final String fileName2 = info.getLibraryFileName(libraryName);
    final String resourcePath2 = info.getLibraryResourcePath(libraryName);

    // Results should be consistent
    assertEquals(fileName1, fileName2, "File name should be consistent for: " + libraryName);
    assertEquals(
        resourcePath1, resourcePath2, "Resource path should be consistent for: " + libraryName);

    // Results should follow expected patterns
    assertTrue(fileName1.contains(libraryName), "File name should contain library name");
    assertTrue(resourcePath1.contains(fileName1), "Resource path should contain file name");
    assertTrue(resourcePath1.startsWith("/natives/"), "Resource path should have correct prefix");
  }

  @Test
  @DisplayName("Should support platform-specific integration patterns")
  void testPlatformSpecificIntegration() {
    final PlatformDetector.PlatformInfo info = PlatformDetector.detect();
    final String platformId = info.getPlatformId();

    // Test different integration patterns based on platform
    switch (info.getOperatingSystem()) {
      case LINUX:
        assertTrue(platformId.startsWith("linux-"), "Linux platform ID should start with 'linux-'");
        assertTrue(
            info.getLibraryFileName("test").endsWith(".so"), "Linux libraries should end with .so");
        assertTrue(
            info.getLibraryFileName("test").startsWith("lib"),
            "Linux libraries should start with lib prefix");
        break;

      case WINDOWS:
        assertTrue(
            platformId.startsWith("windows-"), "Windows platform ID should start with 'windows-'");
        assertTrue(
            info.getLibraryFileName("test").endsWith(".dll"),
            "Windows libraries should end with .dll");
        assertFalse(
            info.getLibraryFileName("test").startsWith("lib"),
            "Windows libraries should not have lib prefix");
        break;

      case MACOS:
        assertTrue(platformId.startsWith("macos-"), "macOS platform ID should start with 'macos-'");
        assertTrue(
            info.getLibraryFileName("test").endsWith(".dylib"),
            "macOS libraries should end with .dylib");
        assertTrue(
            info.getLibraryFileName("test").startsWith("lib"),
            "macOS libraries should start with lib prefix");
        break;
    }

    // Architecture-specific checks
    switch (info.getArchitecture()) {
      case X86_64:
        assertTrue(platformId.endsWith("-x86_64"), "x86_64 platform ID should end with '-x86_64'");
        break;
      case AARCH64:
        assertTrue(
            platformId.endsWith("-aarch64"), "aarch64 platform ID should end with '-aarch64'");
        break;
    }
  }

  @Test
  @DisplayName("Should handle service loader integration patterns")
  void testServiceLoaderIntegrationPattern() {
    // Simulate how service loaders might use platform detection
    final PlatformDetector.PlatformInfo info = PlatformDetector.detect();

    // Test pattern: service discovery based on platform
    final String servicePattern = "wasmtime4j.runtime.%s.%s";
    final String platformSpecificService =
        String.format(
            servicePattern, info.getOperatingSystem().getName(), info.getArchitecture().getName());

    assertNotNull(platformSpecificService, "Platform-specific service name should not be null");
    assertTrue(
        platformSpecificService.contains(info.getOperatingSystem().getName()),
        "Service name should contain OS name");
    assertTrue(
        platformSpecificService.contains(info.getArchitecture().getName()),
        "Service name should contain architecture name");

    // Test that the pattern is consistent
    final String secondService =
        String.format(
            servicePattern, info.getOperatingSystem().getName(), info.getArchitecture().getName());
    assertEquals(
        platformSpecificService, secondService, "Service discovery should be deterministic");
  }

  @Test
  @DisplayName("Should support module boundary error handling")
  void testModuleBoundaryErrorHandling() {
    // Test error handling across module boundaries
    final PlatformDetector.PlatformInfo info = PlatformDetector.detect();

    // Simulate different error scenarios that might occur in integration
    try {
      // Test with various problematic library names
      final String[] problematicNames = {
        "", "   ", "very-long-name-".repeat(100), "name\nwith\nnewlines"
      };

      for (final String name : problematicNames) {
        try {
          final String fileName = info.getLibraryFileName(name);
          final String resourcePath = info.getLibraryResourcePath(name);

          // If these succeed, verify they're safe
          if (fileName != null) {
            assertFalse(fileName.contains("\n"), "File name should not contain newlines");
            assertFalse(fileName.contains(".."), "File name should not contain path traversal");
          }
          if (resourcePath != null) {
            assertTrue(resourcePath.startsWith("/natives/"), "Resource path should be safe");
          }
        } catch (final RuntimeException e) {
          // Expected for some problematic inputs - verify error is reasonable
          assertNotNull(e.getMessage(), "Exception should have a message");
        }
      }
    } catch (final Exception e) {
      // Should not throw unexpected exceptions
      assertTrue(
          e instanceof RuntimeException || e instanceof IllegalArgumentException,
          "Should only throw expected exception types");
    }
  }

  @Test
  @DisplayName("Should provide consistent interface for module consumers")
  void testConsistentModuleConsumerInterface() {
    // Test that the interface is consistent for different consumers
    final String[] consumerTypes = {"core", "jni", "panama", "test", "benchmark"};

    for (final String consumerType : consumerTypes) {
      // Each consumer type should get consistent results
      final PlatformDetector.PlatformInfo info = PlatformDetector.detect();

      assertNotNull(info, "Platform info should not be null for consumer: " + consumerType);
      assertNotNull(
          info.getPlatformId(), "Platform ID should not be null for consumer: " + consumerType);
      assertNotNull(
          info.getOperatingSystem(),
          "Operating system should not be null for consumer: " + consumerType);
      assertNotNull(
          info.getArchitecture(), "Architecture should not be null for consumer: " + consumerType);

      // Test library operations
      final String testLibName = "wasmtime4j-" + consumerType;
      final String fileName = info.getLibraryFileName(testLibName);
      final String resourcePath = info.getLibraryResourcePath(testLibName);

      assertNotNull(fileName, "File name should not be null for consumer: " + consumerType);
      assertNotNull(resourcePath, "Resource path should not be null for consumer: " + consumerType);
      assertTrue(
          fileName.contains(testLibName),
          "File name should contain library name for consumer: " + consumerType);
      assertTrue(
          resourcePath.contains(fileName),
          "Resource path should contain file name for consumer: " + consumerType);
    }
  }
}
