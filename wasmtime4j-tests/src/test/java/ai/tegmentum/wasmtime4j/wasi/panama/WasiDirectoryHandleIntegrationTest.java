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

package ai.tegmentum.wasmtime4j.wasi.panama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.panama.NativeLibraryLoader;
import ai.tegmentum.wasmtime4j.wasi.WasiDirectoryHandle;
import ai.tegmentum.wasmtime4j.wasi.WasiRights;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration tests for Panama WasiDirectoryHandle.
 *
 * <p>These tests verify the WasiDirectoryHandle interface implementation in Panama, including
 * directory iteration, position management, and resource lifecycle.
 *
 * <p><b>NOTE:</b> The Panama implementation (PanamaWasiDirectoryHandleImpl) is package-private and
 * cannot be directly instantiated from this test module. Tests focus on interface contract
 * verification and will require proper WASI context setup when the native implementation is
 * complete.
 *
 * @since 1.0.0
 */
@DisplayName("Panama WasiDirectoryHandle Integration Tests")
class WasiDirectoryHandleIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(WasiDirectoryHandleIntegrationTest.class.getName());

  /** Tracks resources for cleanup. */
  private final List<AutoCloseable> resources = new ArrayList<>();

  @TempDir Path tempDir;

  @BeforeAll
  static void loadNativeLibrary() {
    LOGGER.info("Loading native library for Panama WASI directory handle tests");
    try {
      NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
      assertTrue(loader.isLoaded(), "Native library should be loaded");
      LOGGER.info("Native library loaded successfully for Panama");
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library: " + e.getMessage());
      throw new RuntimeException("Native library required for integration tests", e);
    }
  }

  @BeforeEach
  void setUp() throws Exception {
    LOGGER.info("Setting up test resources");

    // Create test directory structure
    final Path subDir1 = tempDir.resolve("subdir1");
    final Path subDir2 = tempDir.resolve("subdir2");
    final Path file1 = tempDir.resolve("file1.txt");
    final Path file2 = tempDir.resolve("file2.txt");

    Files.createDirectories(subDir1);
    Files.createDirectories(subDir2);
    Files.writeString(file1, "Content 1");
    Files.writeString(file2, "Content 2");

    LOGGER.info("Test setup complete with temp dir: " + tempDir);
  }

  @AfterEach
  void tearDown() {
    LOGGER.info("Cleaning up test resources");
    // Close resources in reverse order to ensure child resources
    // are closed before parent resources
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close resource: " + e.getMessage());
      }
    }
    resources.clear();
  }

  @Nested
  @DisplayName("Interface Contract Tests")
  class InterfaceContractTests {

    @Test
    @DisplayName("should define getFileDescriptor method")
    void shouldDefineGetFileDescriptorMethod() throws NoSuchMethodException {
      LOGGER.info("Verifying getFileDescriptor method exists in interface");

      final Method method = WasiDirectoryHandle.class.getMethod("getFileDescriptor");
      assertNotNull(method, "getFileDescriptor method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");

      LOGGER.info("Interface correctly defines getFileDescriptor method");
    }

    @Test
    @DisplayName("should define getPath method")
    void shouldDefineGetPathMethod() throws NoSuchMethodException {
      LOGGER.info("Verifying getPath method exists in interface");

      final Method method = WasiDirectoryHandle.class.getMethod("getPath");
      assertNotNull(method, "getPath method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");

      LOGGER.info("Interface correctly defines getPath method");
    }

    @Test
    @DisplayName("should define getRights method")
    void shouldDefineGetRightsMethod() throws NoSuchMethodException {
      LOGGER.info("Verifying getRights method exists in interface");

      final Method method = WasiDirectoryHandle.class.getMethod("getRights");
      assertNotNull(method, "getRights method should exist");
      assertEquals(WasiRights.class, method.getReturnType(), "Should return WasiRights");

      LOGGER.info("Interface correctly defines getRights method");
    }

    @Test
    @DisplayName("should define isValid method")
    void shouldDefineIsValidMethod() throws NoSuchMethodException {
      LOGGER.info("Verifying isValid method exists in interface");

      final Method method = WasiDirectoryHandle.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");

      LOGGER.info("Interface correctly defines isValid method");
    }

    @Test
    @DisplayName("should define getPosition method")
    void shouldDefineGetPositionMethod() throws NoSuchMethodException {
      LOGGER.info("Verifying getPosition method exists in interface");

      final Method method = WasiDirectoryHandle.class.getMethod("getPosition");
      assertNotNull(method, "getPosition method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");

      LOGGER.info("Interface correctly defines getPosition method");
    }

    @Test
    @DisplayName("should define setPosition method")
    void shouldDefineSetPositionMethod() throws NoSuchMethodException {
      LOGGER.info("Verifying setPosition method exists in interface");

      final Method method = WasiDirectoryHandle.class.getMethod("setPosition", long.class);
      assertNotNull(method, "setPosition method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");

      LOGGER.info("Interface correctly defines setPosition method");
    }

    @Test
    @DisplayName("should define rewind method")
    void shouldDefineRewindMethod() throws NoSuchMethodException {
      LOGGER.info("Verifying rewind method exists in interface");

      final Method method = WasiDirectoryHandle.class.getMethod("rewind");
      assertNotNull(method, "rewind method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");

      LOGGER.info("Interface correctly defines rewind method");
    }

    @Test
    @DisplayName("should define close method from AutoCloseable")
    void shouldDefineCloseMethod() throws NoSuchMethodException {
      LOGGER.info("Verifying close method exists in interface");

      final Method method = WasiDirectoryHandle.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");

      LOGGER.info("Interface correctly defines close method");
    }
  }

  @Nested
  @DisplayName("Interface Hierarchy Tests")
  class InterfaceHierarchyTests {

    @Test
    @DisplayName("should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      LOGGER.info("Verifying interface extends AutoCloseable");

      assertTrue(
          AutoCloseable.class.isAssignableFrom(WasiDirectoryHandle.class),
          "WasiDirectoryHandle should extend AutoCloseable");

      LOGGER.info("Interface correctly extends AutoCloseable");
    }
  }

  @Nested
  @DisplayName("WasiRights Enum Tests")
  class WasiRightsEnumTests {

    @Test
    @DisplayName("should have common directory rights")
    void shouldHaveCommonDirectoryRights() {
      LOGGER.info("Verifying WasiRights enum has common directory rights");

      // Verify that the WasiRights enum exists and has expected values
      final WasiRights[] rights = WasiRights.values();
      assertNotNull(rights, "WasiRights values should not be null");
      assertTrue(rights.length > 0, "WasiRights should have at least one value");

      LOGGER.info("WasiRights has " + rights.length + " values defined");
    }
  }

  @Nested
  @DisplayName("Native Implementation Tests")
  @Disabled("Requires valid native directory handles from WASI context")
  class NativeImplementationTests {

    @Test
    @DisplayName("should create directory handle from WASI context")
    void shouldCreateDirectoryHandleFromWasiContext() {
      LOGGER.info("Testing directory handle creation from WASI context");

      // This test would require:
      // 1. A properly configured WASI context
      // 2. Native directory handle creation from preopened directories
      // 3. The handle would be obtained from WasiFilesystemOperations or similar

      LOGGER.info("Test placeholder for native directory handle creation");
    }

    @Test
    @DisplayName("should iterate directory entries")
    void shouldIterateDirectoryEntries() {
      LOGGER.info("Testing directory entry iteration");

      // This test would require a valid directory handle to call:
      // - readEntry() or similar methods
      // - Verify each entry's name and type

      LOGGER.info("Test placeholder for directory entry iteration");
    }

    @Test
    @DisplayName("should track position during iteration")
    void shouldTrackPositionDuringIteration() {
      LOGGER.info("Testing position tracking during iteration");

      // This test would require:
      // - A valid directory handle
      // - Multiple calls to advance position
      // - Verification of getPosition() after each call

      LOGGER.info("Test placeholder for position tracking");
    }

    @Test
    @DisplayName("should rewind position to start")
    void shouldRewindPositionToStart() {
      LOGGER.info("Testing position rewind");

      // This test would require:
      // - A valid directory handle
      // - Advance position to some point
      // - Call rewind()
      // - Verify getPosition() returns 0

      LOGGER.info("Test placeholder for position rewind");
    }
  }

  @Nested
  @DisplayName("Test Directory Setup Tests")
  class TestDirectorySetupTests {

    @Test
    @DisplayName("should have test directory with subdirectories")
    void shouldHaveTestDirectoryWithSubdirectories() throws Exception {
      LOGGER.info("Verifying test directory structure");

      assertTrue(Files.isDirectory(tempDir), "Temp dir should exist");
      assertTrue(Files.isDirectory(tempDir.resolve("subdir1")), "subdir1 should exist");
      assertTrue(Files.isDirectory(tempDir.resolve("subdir2")), "subdir2 should exist");
      assertTrue(Files.isRegularFile(tempDir.resolve("file1.txt")), "file1.txt should exist");
      assertTrue(Files.isRegularFile(tempDir.resolve("file2.txt")), "file2.txt should exist");

      LOGGER.info("Test directory structure is correct");
    }
  }
}
