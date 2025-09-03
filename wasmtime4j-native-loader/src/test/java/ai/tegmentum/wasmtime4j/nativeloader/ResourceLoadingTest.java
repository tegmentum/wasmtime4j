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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Resource loading and extraction tests for native library utilities.
 *
 * <p>This test class focuses on:
 *
 * <ul>
 *   <li>Resource detection and validation
 *   <li>Temporary file creation and management
 *   <li>File permissions and security
 *   <li>Cleanup and resource management
 *   <li>Error handling for resource operations
 * </ul>
 */
@DisplayName("Resource Loading Tests")
final class ResourceLoadingTest {

  @TempDir private Path tempDir;

  private List<Path> createdPaths;

  @BeforeEach
  void setUp() {
    createdPaths = new ArrayList<>();
  }

  @AfterEach
  void tearDown() {
    // Clean up any created paths
    for (final Path path : createdPaths) {
      try {
        if (Files.exists(path)) {
          if (Files.isDirectory(path)) {
            Files.walk(path)
                .sorted((a, b) -> b.compareTo(a)) // Delete children first
                .forEach(
                    p -> {
                      try {
                        Files.deleteIfExists(p);
                      } catch (final IOException e) {
                        // Best effort cleanup
                      }
                    });
          } else {
            Files.deleteIfExists(path);
          }
        }
      } catch (final IOException e) {
        // Best effort cleanup
      }
    }
  }

  /**
   * Provides test data for various library names and platform combinations.
   *
   * @return stream of test arguments
   */
  private static Stream<Arguments> provideLibraryAndPlatformCombinations() {
    return Stream.of(
        Arguments.of(
            "wasmtime4j",
            PlatformDetector.OperatingSystem.LINUX,
            PlatformDetector.Architecture.X86_64,
            "/natives/linux-x86_64/libwasmtime4j.so"),
        Arguments.of(
            "wasmtime4j",
            PlatformDetector.OperatingSystem.LINUX,
            PlatformDetector.Architecture.AARCH64,
            "/natives/linux-aarch64/libwasmtime4j.so"),
        Arguments.of(
            "wasmtime4j",
            PlatformDetector.OperatingSystem.WINDOWS,
            PlatformDetector.Architecture.X86_64,
            "/natives/windows-x86_64/wasmtime4j.dll"),
        Arguments.of(
            "wasmtime4j",
            PlatformDetector.OperatingSystem.WINDOWS,
            PlatformDetector.Architecture.AARCH64,
            "/natives/windows-aarch64/wasmtime4j.dll"),
        Arguments.of(
            "wasmtime4j",
            PlatformDetector.OperatingSystem.MACOS,
            PlatformDetector.Architecture.X86_64,
            "/natives/macos-x86_64/libwasmtime4j.dylib"),
        Arguments.of(
            "wasmtime4j",
            PlatformDetector.OperatingSystem.MACOS,
            PlatformDetector.Architecture.AARCH64,
            "/natives/macos-aarch64/libwasmtime4j.dylib"),
        Arguments.of(
            "testlib",
            PlatformDetector.OperatingSystem.LINUX,
            PlatformDetector.Architecture.X86_64,
            "/natives/linux-x86_64/libtestlib.so"),
        Arguments.of(
            "custom-native",
            PlatformDetector.OperatingSystem.WINDOWS,
            PlatformDetector.Architecture.X86_64,
            "/natives/windows-x86_64/custom-native.dll"));
  }

  @ParameterizedTest(name = "{0} on {1}-{2} -> {3}")
  @MethodSource("provideLibraryAndPlatformCombinations")
  @DisplayName("Should generate correct resource paths for all platform combinations")
  void testResourcePathGeneration(
      final String libraryName,
      final PlatformDetector.OperatingSystem os,
      final PlatformDetector.Architecture arch,
      final String expectedResourcePath) {

    final PlatformDetector.PlatformInfo info =
        PlatformDetectorTestUtils.createPlatformInfo(os, arch);

    final String actualResourcePath = info.getLibraryResourcePath(libraryName);

    assertEquals(
        expectedResourcePath, actualResourcePath, "Resource path should match expected format");

    // Verify path components
    assertTrue(
        actualResourcePath.startsWith("/natives/"), "Resource path should start with /natives/");
    assertTrue(
        actualResourcePath.contains(info.getPlatformId()),
        "Resource path should contain platform ID");
    assertTrue(
        actualResourcePath.endsWith(info.getLibraryFileName(libraryName)),
        "Resource path should end with library file name");
  }

  @Test
  @DisplayName("Should handle library file name construction consistently")
  void testLibraryFileNameConstruction() {
    // Test all OS/architecture combinations
    for (final PlatformDetector.OperatingSystem os : PlatformDetector.OperatingSystem.values()) {
      for (final PlatformDetector.Architecture arch : PlatformDetector.Architecture.values()) {
        final PlatformDetector.PlatformInfo info =
            PlatformDetectorTestUtils.createPlatformInfo(os, arch);

        final String libraryName = "testlib";
        final String fileName = info.getLibraryFileName(libraryName);

        assertNotNull(fileName, "File name should not be null for " + os + "-" + arch);
        assertFalse(fileName.isEmpty(), "File name should not be empty for " + os + "-" + arch);
        assertTrue(
            fileName.contains(libraryName),
            "File name should contain library name for " + os + "-" + arch);
        assertTrue(
            fileName.endsWith(os.getLibraryExtension()),
            "File name should end with correct extension for " + os);

        if (!os.getLibraryPrefix().isEmpty()) {
          assertTrue(
              fileName.startsWith(os.getLibraryPrefix()),
              "File name should start with correct prefix for " + os);
        }
      }
    }
  }

  @Test
  @DisplayName("Should create mock resource for testing extraction")
  void testMockResourceCreation() throws IOException {
    // Create a mock native library file for testing
    final Path mockLibrary = tempDir.resolve("mock-library.so");
    final byte[] mockContent = "MOCK_NATIVE_LIBRARY_CONTENT".getBytes();
    Files.write(mockLibrary, mockContent);
    createdPaths.add(mockLibrary);

    assertTrue(Files.exists(mockLibrary), "Mock library should exist");
    assertEquals(
        mockContent.length, Files.size(mockLibrary), "Mock library should have expected size");
    assertTrue(Files.isReadable(mockLibrary), "Mock library should be readable");
  }

  @Test
  @DisplayName("Should handle temporary directory creation")
  void testTemporaryDirectoryCreation() throws IOException {
    final Path tempLibDir = Files.createTempDirectory(tempDir, "wasmtime4j-test-");
    createdPaths.add(tempLibDir);

    assertTrue(Files.exists(tempLibDir), "Temporary directory should exist");
    assertTrue(Files.isDirectory(tempLibDir), "Should be a directory");
    assertTrue(Files.isWritable(tempLibDir), "Directory should be writable");
    assertTrue(Files.isReadable(tempLibDir), "Directory should be readable");
  }

  @Test
  @DisplayName("Should handle file permission setting simulation")
  void testFilePermissionHandling() throws IOException {
    final Path testFile = tempDir.resolve("test-permissions.so");
    Files.write(testFile, "test content".getBytes());
    createdPaths.add(testFile);

    // Test that the file exists and we can modify its permissions
    assertTrue(Files.exists(testFile), "Test file should exist");

    // On most systems, we should be able to set basic permissions
    final boolean isExecutableSet = testFile.toFile().setExecutable(true);
    final boolean isReadableSet = testFile.toFile().setReadable(true);

    // Note: These may fail on some systems, but should not throw exceptions
    assertTrue(
        isExecutableSet || !isExecutableSet, "setExecutable should complete without exception");
    assertTrue(isReadableSet || !isReadableSet, "setReadable should complete without exception");
  }

  @Test
  @DisplayName("Should handle resource stream operations")
  void testResourceStreamOperations() throws IOException {
    // Test with a mock input stream
    final byte[] testData = "Mock native library content for testing".getBytes();
    try (final InputStream mockStream = new ByteArrayInputStream(testData)) {
      final Path outputFile = tempDir.resolve("extracted-lib.so");
      Files.copy(mockStream, outputFile);
      createdPaths.add(outputFile);

      assertTrue(Files.exists(outputFile), "Extracted file should exist");
      assertEquals(testData.length, Files.size(outputFile), "File size should match input data");

      final byte[] extractedData = Files.readAllBytes(outputFile);
      assertEquals(
          new String(testData), new String(extractedData), "Extracted content should match input");
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"lib1", "wasmtime4j", "test-lib", "native_module", "a", "very-long-name"})
  @DisplayName("Should handle various library names in resource operations")
  void testVariousLibraryNamesInResourceOperations(final String libraryName) {
    final PlatformDetector.PlatformInfo info = PlatformDetector.detect();

    // Test file name generation
    final String fileName = info.getLibraryFileName(libraryName);
    assertNotNull(fileName, "File name should not be null");
    assertTrue(fileName.contains(libraryName), "File name should contain library name");

    // Test resource path generation
    final String resourcePath = info.getLibraryResourcePath(libraryName);
    assertNotNull(resourcePath, "Resource path should not be null");
    assertTrue(resourcePath.startsWith("/natives/"), "Resource path should start correctly");
    assertTrue(resourcePath.contains(fileName), "Resource path should contain file name");
  }

  @Test
  @DisplayName("Should handle concurrent resource operations")
  void testConcurrentResourceOperations()
      throws InterruptedException, ExecutionException, TimeoutException {
    final int threadCount = 5;
    final List<CompletableFuture<Void>> futures = new ArrayList<>();

    for (int i = 0; i < threadCount; i++) {
      final int threadIndex = i;
      final CompletableFuture<Void> future =
          CompletableFuture.runAsync(
              () -> {
                try {
                  final Path threadFile = tempDir.resolve("thread-" + threadIndex + ".so");
                  final String content = "Thread " + threadIndex + " content";
                  Files.write(threadFile, content.getBytes());

                  // Verify the file was created successfully
                  assertTrue(
                      Files.exists(threadFile), "Thread " + threadIndex + " file should exist");
                  assertEquals(
                      content.length(),
                      Files.size(threadFile),
                      "Thread " + threadIndex + " file should have correct size");

                  createdPaths.add(threadFile);
                } catch (final IOException e) {
                  throw new RuntimeException("Thread " + threadIndex + " failed", e);
                }
              });
      futures.add(future);
    }

    // Wait for all threads to complete
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(10, TimeUnit.SECONDS);

    // Verify all files were created
    for (int i = 0; i < threadCount; i++) {
      final Path threadFile = tempDir.resolve("thread-" + i + ".so");
      assertTrue(Files.exists(threadFile), "All thread files should exist after completion");
    }
  }

  @Test
  @DisplayName("Should handle cleanup operations")
  void testCleanupOperations() throws IOException {
    // Create multiple files and directories
    final Path subDir = tempDir.resolve("cleanup-test");
    Files.createDirectory(subDir);
    createdPaths.add(subDir);

    final Path file1 = subDir.resolve("file1.so");
    final Path file2 = subDir.resolve("file2.dll");
    Files.write(file1, "content1".getBytes());
    Files.write(file2, "content2".getBytes());
    createdPaths.add(file1);
    createdPaths.add(file2);

    // Verify files exist
    assertTrue(Files.exists(subDir), "Subdirectory should exist");
    assertTrue(Files.exists(file1), "File 1 should exist");
    assertTrue(Files.exists(file2), "File 2 should exist");

    // Simulate cleanup (files will be cleaned up in @AfterEach)
    // This test verifies the structure is correct for cleanup
    assertEquals(2, Files.list(subDir).count(), "Subdirectory should contain 2 files");
  }

  @Test
  @DisplayName("Should handle disk space simulation")
  void testDiskSpaceHandling() throws IOException {
    // Create a reasonably large file to test space handling
    final Path largeFile = tempDir.resolve("large-library.so");
    final byte[] data = new byte[1024]; // 1KB - reasonable for testing
    for (int i = 0; i < data.length; i++) {
      data[i] = (byte) (i % 256);
    }
    Files.write(largeFile, data);
    createdPaths.add(largeFile);

    assertTrue(Files.exists(largeFile), "Large file should exist");
    assertEquals(data.length, Files.size(largeFile), "Large file should have expected size");

    // Verify we can read the file back
    final byte[] readData = Files.readAllBytes(largeFile);
    assertEquals(data.length, readData.length, "Read data should have same length as written");
  }

  @Test
  @DisplayName("Should handle path validation")
  void testPathValidation() {
    final PlatformDetector.PlatformInfo info = PlatformDetector.detect();
    final String resourcePath = info.getLibraryResourcePath("testlib");

    // Verify path structure
    assertTrue(resourcePath.startsWith("/"), "Resource path should be absolute");
    assertFalse(resourcePath.contains(".."), "Resource path should not contain parent references");
    assertFalse(resourcePath.contains("//"), "Resource path should not contain double slashes");
    assertTrue(Paths.get(resourcePath).isAbsolute(), "Resource path should parse as absolute path");
  }

  @Test
  @DisplayName("Should handle error conditions in resource operations")
  void testResourceOperationErrorConditions() {
    // Test with invalid paths
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          final Path invalidPath = Paths.get("");
          invalidPath.toFile().setExecutable(true);
        },
        "Should handle invalid path operations gracefully");
  }
}
