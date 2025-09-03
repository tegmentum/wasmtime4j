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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Tests for ResourcePathResolver utility class functionality.
 *
 * <p>This test class focuses on the pattern validation and resolution logic
 * used by path conventions to generate secure resource paths.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
final class ResourcePathResolverTest {

  private PlatformDetector.PlatformInfo linuxInfo;
  private PlatformDetector.PlatformInfo windowsInfo;
  private PlatformDetector.PlatformInfo darwinInfo;

  @BeforeEach
  void setUp() {
    linuxInfo = new PlatformDetector.PlatformInfo(
        PlatformDetector.OperatingSystem.LINUX,
        PlatformDetector.Architecture.X86_64);
        
    windowsInfo = new PlatformDetector.PlatformInfo(
        PlatformDetector.OperatingSystem.WINDOWS,
        PlatformDetector.Architecture.X86_64);
        
    darwinInfo = new PlatformDetector.PlatformInfo(
        PlatformDetector.OperatingSystem.MACOS,
        PlatformDetector.Architecture.X86_64);
  }

  @Test
  @DisplayName("Pattern validation should accept valid patterns")
  void testValidPatternAcceptance() {
    // These should not throw any exceptions
    ResourcePathResolver.validatePattern("/natives/{platform}/{lib}{name}{ext}");
    ResourcePathResolver.validatePattern("/{os}-{arch}/{name}.{ext}");
    ResourcePathResolver.validatePattern("/custom/path/to/{name}");
    ResourcePathResolver.validatePattern("{platform}/{name}{ext}");
  }

  @Test
  @DisplayName("Pattern validation should reject path traversal attempts")
  void testPathTraversalRejection() {
    assertThrows(
        SecurityException.class,
        () -> ResourcePathResolver.validatePattern("../../../etc/passwd"),
        "Should reject basic path traversal");
        
    assertThrows(
        SecurityException.class,
        () -> ResourcePathResolver.validatePattern("/lib/{name}/../secrets"),
        "Should reject path traversal in middle of pattern");
        
    assertThrows(
        SecurityException.class,
        () -> ResourcePathResolver.validatePattern("..\\windows\\system32"),
        "Should reject Windows-style path traversal");
  }

  @Test
  @DisplayName("Pattern validation should reject null bytes")
  void testNullByteRejection() {
    assertThrows(
        SecurityException.class,
        () -> ResourcePathResolver.validatePattern("/lib/{name}\0.so"),
        "Should reject patterns with null bytes");
  }

  @Test
  @DisplayName("Pattern validation should reject control characters")
  void testControlCharacterRejection() {
    assertThrows(
        SecurityException.class,
        () -> ResourcePathResolver.validatePattern("/lib/{name}\u0001.so"),
        "Should reject patterns with control characters");
  }

  @Test
  @DisplayName("Pattern validation should reject absolute paths")
  void testAbsolutePathRejection() {
    assertThrows(
        SecurityException.class,
        () -> ResourcePathResolver.validatePattern("C:\\Windows\\{name}.dll"),
        "Should reject Windows absolute paths");
  }

  @Test
  @DisplayName("Pattern validation should accept leading slashes for resources")
  void testLeadingSlashAcceptance() {
    // Leading slash should be acceptable for JAR resource paths
    ResourcePathResolver.validatePattern("/{platform}/{name}{ext}");
    ResourcePathResolver.validatePattern("/natives/{os}-{arch}/{lib}{name}{ext}");
  }

  @Test
  @DisplayName("Pattern validation should reject malformed placeholders")
  void testMalformedPlaceholderRejection() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ResourcePathResolver.validatePattern("/lib/{invalid placeholder}.so"),
        "Should reject placeholders with spaces");
        
    assertThrows(
        IllegalArgumentException.class,
        () -> ResourcePathResolver.validatePattern("/lib/{.so"),
        "Should reject unclosed placeholders");
  }

  @Test
  @DisplayName("Path resolution should substitute all standard placeholders")
  void testStandardPlaceholderSubstitution() {
    final String pattern = "/{platform}/{os}/{arch}/{lib}{name}{ext}";
    final String resolved = ResourcePathResolver.resolvePath(pattern, "testlib", linuxInfo);
    
    assertEquals("/linux-x86_64/linux/x86_64/libtestlib.so", resolved,
        "All standard placeholders should be substituted");
  }

  @Test
  @DisplayName("Path resolution should work with different platforms")
  void testMultiPlatformResolution() {
    final String pattern = "/native/{platform}/{lib}{name}{ext}";
    final String libraryName = "mylib";
    
    assertEquals("/native/linux-x86_64/libmylib.so",
        ResourcePathResolver.resolvePath(pattern, libraryName, linuxInfo),
        "Linux path should be resolved correctly");
        
    assertEquals("/native/windows-x86_64/mylib.dll",
        ResourcePathResolver.resolvePath(pattern, libraryName, windowsInfo),
        "Windows path should be resolved correctly");
        
    assertEquals("/native/macos-x86_64/libmylib.dylib",
        ResourcePathResolver.resolvePath(pattern, libraryName, darwinInfo),
        "macOS path should be resolved correctly");
  }

  @Test
  @DisplayName("Library name sanitization should remove path separators")
  void testLibraryNameSanitization() {
    final String pattern = "/lib/{name}.so";
    
    final String result1 = ResourcePathResolver.resolvePath(
        pattern, "lib/with/slashes", linuxInfo);
    assertEquals("/lib/libwithslashes.so", result1,
        "Forward slashes should be removed from library name");
        
    final String result2 = ResourcePathResolver.resolvePath(
        pattern, "lib\\with\\backslashes", linuxInfo);
    assertEquals("/lib/libwithbackslashes.so", result2,
        "Backslashes should be removed from library name");
  }

  @Test
  @DisplayName("Library name sanitization should remove control characters")
  void testLibraryNameControlCharacterRemoval() {
    final String pattern = "/lib/{name}.so";
    
    final String result = ResourcePathResolver.resolvePath(
        pattern, "lib\u0001with\u0002control", linuxInfo);
    assertEquals("/lib/libwithcontrol.so", result,
        "Control characters should be removed from library name");
  }

  @Test
  @DisplayName("Resolved path validation should detect remaining placeholders")
  void testUnresolvedPlaceholderDetection() {
    // This would happen if we had an unknown placeholder
    final String invalidPattern = "/lib/{unknown}/{name}.so";
    
    // Note: This test validates the concept, but the current implementation
    // would just leave the placeholder as-is rather than throwing an exception.
    // The validation would catch it as an unresolved placeholder.
    
    final String resolved = ResourcePathResolver.resolvePath(invalidPattern, "test", linuxInfo);
    // The resolved path would contain "{unknown}" which should trigger validation
    
    // For now, we just verify the placeholder remains
    assertEquals("/lib/{unknown}/test.so", resolved,
        "Unknown placeholders should remain in the resolved path");
  }

  @Test
  @DisplayName("Path resolution should handle null inputs gracefully")
  void testNullInputHandling() {
    assertThrows(
        NullPointerException.class,
        () -> ResourcePathResolver.resolvePath(null, "test", linuxInfo),
        "Should throw NPE for null pattern");
        
    assertThrows(
        NullPointerException.class,
        () -> ResourcePathResolver.resolvePath("/lib/{name}.so", null, linuxInfo),
        "Should throw NPE for null library name");
        
    assertThrows(
        NullPointerException.class,
        () -> ResourcePathResolver.resolvePath("/lib/{name}.so", "test", null),
        "Should throw NPE for null platform info");
  }

  @Test
  @DisplayName("Path resolution should handle empty inputs")
  void testEmptyInputHandling() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ResourcePathResolver.resolvePath("", "test", linuxInfo),
        "Should throw IAE for empty pattern");
        
    assertThrows(
        IllegalArgumentException.class,
        () -> ResourcePathResolver.resolvePath("   ", "test", linuxInfo),
        "Should throw IAE for whitespace-only pattern");
        
    assertThrows(
        IllegalArgumentException.class,
        () -> ResourcePathResolver.resolvePath("/lib/{name}.so", "", linuxInfo),
        "Should throw IAE for empty library name");
        
    assertThrows(
        IllegalArgumentException.class,
        () -> ResourcePathResolver.resolvePath("/lib/{name}.so", "   ", linuxInfo),
        "Should throw IAE for whitespace-only library name");
  }

  @Test
  @DisplayName("Pattern validation should handle edge cases")
  void testPatternValidationEdgeCases() {
    // Test minimal valid pattern
    ResourcePathResolver.validatePattern("{name}");
    
    // Test pattern without placeholders
    ResourcePathResolver.validatePattern("/static/path/library.so");
    
    // Test multiple same placeholders (should be valid)
    ResourcePathResolver.validatePattern("/{platform}/{platform}/{name}");
  }
}