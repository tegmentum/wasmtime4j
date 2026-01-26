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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.panama.NativeLibraryLoader;
import ai.tegmentum.wasmtime4j.panama.PanamaWasiFilesystem;
import ai.tegmentum.wasmtime4j.wasi.WasiDirEntry;
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
  class NativeImplementationTests {

    private PanamaWasiFilesystem filesystem;

    @BeforeEach
    void setUpFilesystem() {
      LOGGER.info("Setting up PanamaWasiFilesystem with root: " + tempDir);
      filesystem = new PanamaWasiFilesystem(tempDir);
      resources.add(() -> filesystem.close());
    }

    @Test
    @DisplayName("should create directory handle from WASI filesystem")
    void shouldCreateDirectoryHandleFromWasiContext() throws Exception {
      LOGGER.info("Testing directory handle creation from WASI filesystem");

      // Open the root directory using the WASI filesystem
      final WasiDirectoryHandle handle = filesystem.openDirectory("/", WasiRights.PATH_OPEN);
      assertNotNull(handle, "Directory handle should not be null");
      resources.add(handle);

      // Verify handle properties
      assertTrue(
          handle.getFileDescriptor() >= 3, "File descriptor should be >= 3 (stdio reserved)");
      assertEquals("/", handle.getPath(), "Path should match");
      assertEquals(WasiRights.PATH_OPEN, handle.getRights(), "Rights should match");
      assertTrue(handle.isValid(), "Handle should be valid");
      assertEquals(0L, handle.getPosition(), "Initial position should be 0");

      LOGGER.info(
          "Successfully created directory handle with fd="
              + handle.getFileDescriptor()
              + ", path="
              + handle.getPath());
    }

    @Test
    @DisplayName("should iterate directory entries")
    void shouldIterateDirectoryEntries() throws Exception {
      LOGGER.info("Testing directory entry iteration");

      // Open directory and read entries
      final WasiDirectoryHandle handle = filesystem.openDirectory("/", WasiRights.PATH_OPEN);
      resources.add(handle);

      final List<WasiDirEntry> entries = filesystem.readDirectory(handle);
      assertNotNull(entries, "Entries list should not be null");

      // We created 2 subdirectories and 2 files in setUp
      assertEquals(
          4, entries.size(), "Should have 4 entries (subdir1, subdir2, file1.txt, file2.txt)");

      // Collect entry names for verification
      final List<String> names = new ArrayList<>();
      for (final WasiDirEntry entry : entries) {
        assertNotNull(entry.getName(), "Entry name should not be null");
        assertNotNull(entry.getType(), "Entry type should not be null");
        names.add(entry.getName());
        LOGGER.info("Found entry: " + entry.getName() + " (type: " + entry.getType() + ")");
      }

      // Verify expected entries exist
      assertTrue(names.contains("subdir1"), "Should contain subdir1");
      assertTrue(names.contains("subdir2"), "Should contain subdir2");
      assertTrue(names.contains("file1.txt"), "Should contain file1.txt");
      assertTrue(names.contains("file2.txt"), "Should contain file2.txt");

      LOGGER.info("Successfully iterated " + entries.size() + " directory entries");
    }

    @Test
    @DisplayName("should track position during iteration")
    void shouldTrackPositionDuringIteration() throws Exception {
      LOGGER.info("Testing position tracking during iteration");

      final WasiDirectoryHandle handle = filesystem.openDirectory("/", WasiRights.PATH_OPEN);
      resources.add(handle);

      // Initial position should be 0
      assertEquals(0L, handle.getPosition(), "Initial position should be 0");

      // Set position to different values and verify
      handle.setPosition(100L);
      assertEquals(100L, handle.getPosition(), "Position should be 100 after setPosition(100)");

      handle.setPosition(500L);
      assertEquals(500L, handle.getPosition(), "Position should be 500 after setPosition(500)");

      // Verify negative position throws exception
      assertThrows(
          IllegalArgumentException.class,
          () -> handle.setPosition(-1L),
          "Negative position should throw IllegalArgumentException");

      LOGGER.info("Position tracking verified successfully");
    }

    @Test
    @DisplayName("should rewind position to start")
    void shouldRewindPositionToStart() throws Exception {
      LOGGER.info("Testing position rewind");

      final WasiDirectoryHandle handle = filesystem.openDirectory("/", WasiRights.PATH_OPEN);
      resources.add(handle);

      // Set position to some non-zero value
      handle.setPosition(12345L);
      assertEquals(12345L, handle.getPosition(), "Position should be 12345 after setPosition");

      // Rewind should reset position to 0
      handle.rewind();
      assertEquals(0L, handle.getPosition(), "Position should be 0 after rewind()");

      // Verify we can set position again after rewind
      handle.setPosition(999L);
      assertEquals(999L, handle.getPosition(), "Position should be 999 after setPosition(999)");

      // Rewind again
      handle.rewind();
      assertEquals(0L, handle.getPosition(), "Position should be 0 after second rewind()");

      LOGGER.info("Position rewind verified successfully");
    }

    @Test
    @DisplayName("should invalidate handle after close")
    void shouldInvalidateHandleAfterClose() throws Exception {
      LOGGER.info("Testing handle invalidation after close");

      final WasiDirectoryHandle handle = filesystem.openDirectory("/", WasiRights.PATH_OPEN);
      assertTrue(handle.isValid(), "Handle should be valid before close");

      handle.close();
      assertFalse(handle.isValid(), "Handle should be invalid after close");

      LOGGER.info("Handle invalidation verified successfully");
    }

    @Test
    @DisplayName("should open subdirectory")
    void shouldOpenSubdirectory() throws Exception {
      LOGGER.info("Testing subdirectory opening");

      // Open a subdirectory
      final WasiDirectoryHandle handle = filesystem.openDirectory("/subdir1", WasiRights.PATH_OPEN);
      assertNotNull(handle, "Subdirectory handle should not be null");
      resources.add(handle);

      assertEquals("/subdir1", handle.getPath(), "Path should match subdirectory");
      assertTrue(handle.isValid(), "Handle should be valid");

      // Read entries (should be empty)
      final List<WasiDirEntry> entries = filesystem.readDirectory(handle);
      assertNotNull(entries, "Entries list should not be null");
      assertEquals(0, entries.size(), "Subdirectory should be empty");

      LOGGER.info("Successfully opened and read subdirectory");
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
