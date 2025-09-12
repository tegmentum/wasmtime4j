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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NativeLoader} class.
 *
 * <p>This test suite verifies the static convenience methods and builder factory methods of the
 * NativeLoader class.
 */
final class NativeLoaderTest {

  @Test

  void testLoadLibraryWithDefaultConfiguration() {
    // Test loading a library that doesn't exist - should return info with error
    final NativeLibraryUtils.LibraryLoadInfo info =
        assertDoesNotThrow(() -> NativeLoader.loadLibrary("nonexistent-test-library"));

    assertNotNull(info, "Load info should not be null");
    assertNotNull(info.getLibraryName(), "Library name should not be null");
    assertEquals(
        "nonexistent-test-library", info.getLibraryName(), "Library name should match input");
  }

  @Test

  void testLoadLibraryWithNullName() {
    assertThrows(
        NullPointerException.class,
        () -> NativeLoader.loadLibrary(null),
        "Should throw NullPointerException for null library name");
  }

  @Test

  void testBuilderCreation() {
    final NativeLoaderBuilder builder = assertDoesNotThrow(NativeLoader::builder);

    assertNotNull(builder, "Builder should not be null");
    assertEquals(
        "wasmtime4j", builder.getLibraryName(), "Builder should start with default library name");
    assertEquals(
        "wasmtime4j-native-",
        builder.getTempFilePrefix(),
        "Builder should start with default temp file prefix");
    assertEquals(
        "-wasmtime4j",
        builder.getTempDirSuffix(),
        "Builder should start with default temp dir suffix");
  }

  @Test

  void testUtilityClassInstantiation() {
    // Use reflection to try to instantiate the utility class
    final var exception =
        assertThrows(
            InvocationTargetException.class,
            () -> {
              final var constructor = NativeLoader.class.getDeclaredConstructor();
              constructor.setAccessible(true);
              constructor.newInstance();
            },
            "Should throw InvocationTargetException when trying to instantiate utility class");

    // Verify the cause is AssertionError
    assertTrue(
        exception.getCause() instanceof AssertionError,
        "The cause should be AssertionError to prevent instantiation");
  }

  @Test

  void testMultipleBuilderInstances() {
    final NativeLoaderBuilder builder1 = NativeLoader.builder();
    final NativeLoaderBuilder builder2 = NativeLoader.builder();

    assertNotNull(builder1, "First builder should not be null");
    assertNotNull(builder2, "Second builder should not be null");

    // Modify one builder and verify it doesn't affect the other
    builder1.libraryName("test1");
    builder2.libraryName("test2");

    assertEquals(
        "test1", builder1.getLibraryName(), "First builder should have test1 library name");
    assertEquals(
        "test2", builder2.getLibraryName(), "Second builder should have test2 library name");
  }

  @Test

  void testEmptyLibraryName() {
    // Empty string should be passed through to the builder and fail validation there
    assertThrows(
        IllegalArgumentException.class,
        () -> NativeLoader.loadLibrary(""),
        "Should throw IllegalArgumentException for empty library name");
  }

  @Test

  void testWhitespaceOnlyLibraryName() {
    // Whitespace-only string should be passed through and fail validation
    assertThrows(
        IllegalArgumentException.class,
        () -> NativeLoader.loadLibrary("   "),
        "Should throw IllegalArgumentException for whitespace-only library name");
  }

  @Test

  void testLibraryNamePreservation() {
    final String testLibName = "test-library-name";
    final NativeLibraryUtils.LibraryLoadInfo info =
        assertDoesNotThrow(() -> NativeLoader.loadLibrary(testLibName));

    assertEquals(
        testLibName,
        info.getLibraryName(),
        "Library name should be preserved through the loading process");
  }

  @Test

  void testBuilderPatternEquivalence() {
    final String testLibName = "equivalent-test";

    // Load using static method
    final NativeLibraryUtils.LibraryLoadInfo staticInfo =
        assertDoesNotThrow(() -> NativeLoader.loadLibrary(testLibName));

    // Load using builder pattern
    final NativeLibraryUtils.LibraryLoadInfo builderInfo =
        assertDoesNotThrow(() -> NativeLoader.builder().libraryName(testLibName).load());

    assertEquals(
        staticInfo.getLibraryName(),
        builderInfo.getLibraryName(),
        "Static and builder methods should produce equivalent results");
  }
}
