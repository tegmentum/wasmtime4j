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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.nativeloader.NativeLoaderBuilder.SecurityLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NativeLoaderBuilder} class.
 *
 * <p>This test suite verifies the fluent API, configuration validation, and building functionality
 * of the NativeLoaderBuilder class.
 */
final class NativeLoaderBuilderTest {

  @Test
  @DisplayName("Should start with default configuration values")
  void testDefaultConfiguration() {
    final NativeLoaderBuilder builder = new NativeLoaderBuilder();

    assertEquals("wasmtime4j", builder.getLibraryName(), "Should start with default library name");
    assertEquals(
        "wasmtime4j-native-",
        builder.getTempFilePrefix(),
        "Should start with default temp file prefix");
    assertEquals(
        "-wasmtime4j", builder.getTempDirSuffix(), "Should start with default temp dir suffix");
    assertEquals(
        SecurityLevel.MODERATE,
        builder.getSecurityLevel(),
        "Should start with default security level");
    assertEquals(
        PathConvention.MAVEN_NATIVE,
        builder.getPathConvention(),
        "Should start with default resource path convention");
  }

  @Test
  @DisplayName("Should support fluent API for library name")
  void testLibraryNameFluentAPI() {
    final NativeLoaderBuilder builder = new NativeLoaderBuilder();
    final NativeLoaderBuilder result = builder.libraryName("testlib");

    assertSame(builder, result, "Should return same builder instance for method chaining");
    assertEquals("testlib", builder.getLibraryName(), "Should update library name");
  }

  @Test
  @DisplayName("Should support fluent API for temp file prefix")
  void testTempFilePrefixFluentAPI() {
    final NativeLoaderBuilder builder = new NativeLoaderBuilder();
    final NativeLoaderBuilder result = builder.tempFilePrefix("test-prefix-");

    assertSame(builder, result, "Should return same builder instance for method chaining");
    assertEquals("test-prefix-", builder.getTempFilePrefix(), "Should update temp file prefix");
  }

  @Test
  @DisplayName("Should support fluent API for temp dir suffix")
  void testTempDirSuffixFluentAPI() {
    final NativeLoaderBuilder builder = new NativeLoaderBuilder();
    final NativeLoaderBuilder result = builder.tempDirSuffix("-test-suffix");

    assertSame(builder, result, "Should return same builder instance for method chaining");
    assertEquals("-test-suffix", builder.getTempDirSuffix(), "Should update temp dir suffix");
  }

  @Test
  @DisplayName("Should support fluent API for security level")
  void testSecurityLevelFluentAPI() {
    final NativeLoaderBuilder builder = new NativeLoaderBuilder();
    final NativeLoaderBuilder result = builder.securityLevel(SecurityLevel.STRICT);

    assertSame(builder, result, "Should return same builder instance for method chaining");
    assertEquals(SecurityLevel.STRICT, builder.getSecurityLevel(), "Should update security level");
  }

  @Test
  @DisplayName("Should support fluent API for resource path convention")
  void testPathConventionFluentAPI() {
    final NativeLoaderBuilder builder = new NativeLoaderBuilder();
    final NativeLoaderBuilder result = builder.pathConvention(PathConvention.JNA);

    assertSame(builder, result, "Should return same builder instance for method chaining");
    assertEquals(
        PathConvention.JNA, builder.getPathConvention(), "Should update resource path convention");
  }

  @Test
  @DisplayName("Should reject null library name")
  void testNullLibraryName() {
    final NativeLoaderBuilder builder = new NativeLoaderBuilder();

    assertThrows(
        IllegalArgumentException.class,
        () -> builder.libraryName(null),
        "Should throw IllegalArgumentException for null library name");
  }

  @Test
  @DisplayName("Should reject null temp file prefix")
  void testNullTempFilePrefix() {
    final NativeLoaderBuilder builder = new NativeLoaderBuilder();

    assertThrows(
        IllegalArgumentException.class,
        () -> builder.tempFilePrefix(null),
        "Should throw IllegalArgumentException for null temp file prefix");
  }

  @Test
  @DisplayName("Should reject null temp dir suffix")
  void testNullTempDirSuffix() {
    final NativeLoaderBuilder builder = new NativeLoaderBuilder();

    assertThrows(
        IllegalArgumentException.class,
        () -> builder.tempDirSuffix(null),
        "Should throw IllegalArgumentException for null temp dir suffix");
  }

  @Test
  @DisplayName("Should reject null security level")
  void testNullSecurityLevel() {
    final NativeLoaderBuilder builder = new NativeLoaderBuilder();

    assertThrows(
        IllegalArgumentException.class,
        () -> builder.securityLevel(null),
        "Should throw IllegalArgumentException for null security level");
  }

  @Test
  @DisplayName("Should reject null resource path convention")
  void testNullPathConvention() {
    final NativeLoaderBuilder builder = new NativeLoaderBuilder();

    assertThrows(
        IllegalArgumentException.class,
        () -> builder.pathConvention(null),
        "Should throw IllegalArgumentException for null resource path convention");
  }

  @Test
  @DisplayName("Should support method chaining")
  void testMethodChaining() {
    final NativeLoaderBuilder builder = new NativeLoaderBuilder();

    final NativeLoaderBuilder result =
        builder
            .libraryName("chaintest")
            .tempFilePrefix("chain-")
            .tempDirSuffix("-chain")
            .securityLevel(SecurityLevel.PERMISSIVE)
            .pathConvention(PathConvention.CUSTOM);

    assertSame(builder, result, "Should return same builder instance for method chaining");
    assertEquals("chaintest", builder.getLibraryName());
    assertEquals("chain-", builder.getTempFilePrefix());
    assertEquals("-chain", builder.getTempDirSuffix());
    assertEquals(SecurityLevel.PERMISSIVE, builder.getSecurityLevel());
    assertEquals(PathConvention.CUSTOM, builder.getPathConvention());
  }

  @Test
  @DisplayName("Should build configuration and load library")
  void testBuildAndLoad() {
    final NativeLoaderBuilder builder = new NativeLoaderBuilder().libraryName("test-build-load");

    // This should not throw an exception even if the library doesn't exist
    final NativeLibraryUtils.LibraryLoadInfo info = assertDoesNotThrow(builder::load);

    assertNotNull(info, "Load info should not be null");
    assertEquals(
        "test-build-load", info.getLibraryName(), "Library name should match configured value");
  }

  @Test
  @DisplayName("Should validate configuration during build")
  void testConfigurationValidationDuringBuild() {
    // Test with empty library name (should fail validation in NativeLibraryConfig)
    final NativeLoaderBuilder builder = new NativeLoaderBuilder().libraryName("");

    assertThrows(
        IllegalArgumentException.class,
        builder::load,
        "Should throw IllegalArgumentException for invalid configuration");
  }

  @Test
  @DisplayName("Should be reusable for multiple builds")
  void testBuilderReusability() {
    final NativeLoaderBuilder builder = new NativeLoaderBuilder().libraryName("reusable-test");

    // Build multiple times
    final NativeLibraryUtils.LibraryLoadInfo info1 = assertDoesNotThrow(builder::load);
    final NativeLibraryUtils.LibraryLoadInfo info2 = assertDoesNotThrow(builder::load);

    assertNotNull(info1, "First load info should not be null");
    assertNotNull(info2, "Second load info should not be null");
    assertEquals(
        info1.getLibraryName(), info2.getLibraryName(), "Both loads should use same library name");
  }

  @Test
  @DisplayName("Should maintain independent state between builder instances")
  void testBuilderIndependence() {
    final NativeLoaderBuilder builder1 = new NativeLoaderBuilder().libraryName("lib1");
    final NativeLoaderBuilder builder2 = new NativeLoaderBuilder().libraryName("lib2");

    assertEquals("lib1", builder1.getLibraryName(), "First builder should have lib1");
    assertEquals("lib2", builder2.getLibraryName(), "Second builder should have lib2");

    // Modify one and verify the other is unaffected
    builder1.libraryName("modified");
    assertEquals("modified", builder1.getLibraryName(), "First builder should be modified");
    assertEquals("lib2", builder2.getLibraryName(), "Second builder should be unaffected");
  }

  @Test
  @DisplayName("Should handle all security levels")
  void testAllSecurityLevels() {
    final NativeLoaderBuilder builder = new NativeLoaderBuilder().libraryName("security-test");

    // Test all security levels
    for (final SecurityLevel level : SecurityLevel.values()) {
      builder.securityLevel(level);
      assertEquals(level, builder.getSecurityLevel(), "Should set security level: " + level);

      // Should be able to build with any security level
      assertDoesNotThrow(builder::load, "Should build successfully with security level: " + level);
    }
  }

  @Test
  @DisplayName("Should handle all resource path conventions")
  void testAllPathConventions() {
    final NativeLoaderBuilder builder = new NativeLoaderBuilder().libraryName("convention-test");

    // Test all resource path conventions
    for (final PathConvention convention : PathConvention.values()) {
      builder.pathConvention(convention);
      assertEquals(
          convention,
          builder.getPathConvention(),
          "Should set resource path convention: " + convention);

      // Should be able to build with any convention
      assertDoesNotThrow(builder::load, "Should build successfully with convention: " + convention);
    }
  }

  @Test
  @DisplayName("Should handle whitespace in string parameters appropriately")
  void testWhitespaceHandling() {
    final NativeLoaderBuilder builder = new NativeLoaderBuilder();

    // These should be passed through to NativeLibraryConfig for validation
    assertThrows(
        IllegalArgumentException.class,
        () -> builder.libraryName("   ").load(),
        "Should reject whitespace-only library name");

    assertThrows(
        IllegalArgumentException.class,
        () -> builder.tempFilePrefix("   ").load(),
        "Should reject whitespace-only temp file prefix");

    assertThrows(
        IllegalArgumentException.class,
        () -> builder.tempDirSuffix("   ").load(),
        "Should reject whitespace-only temp dir suffix");
  }
}
