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

package ai.tegmentum.wasmtime4j.wasi.jni;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.exception.JniValidationException;
import ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiContext;
import ai.tegmentum.wasmtime4j.jni.wasi.filesystem.JniWasiDescriptor;
import ai.tegmentum.wasmtime4j.jni.wasi.security.WasiSecurityValidator;
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
 * Integration tests for JNI WasiDescriptor.
 *
 * <p>These tests verify the actual native implementation of WasiDescriptor, including file and
 * directory operations, stream creation, and resource lifecycle management.
 *
 * <p><b>NOTE:</b> Most tests in this class require valid descriptor handles obtained from
 * filesystem operations, which are not yet fully implemented in the native library. Tests that
 * require descriptors are marked as disabled.
 *
 * @since 1.0.0
 */
@DisplayName("JNI WasiDescriptor Integration Tests")
class WasiDescriptorIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(WasiDescriptorIntegrationTest.class.getName());

  /**
   * Permissive security validator that allows absolute paths for testing. This is required because
   * the default validator rejects absolute paths.
   */
  private static final WasiSecurityValidator TEST_SECURITY_VALIDATOR =
      WasiSecurityValidator.builder().withAllowAbsolutePaths(true).build();

  /** Tracks resources for cleanup. */
  private final List<AutoCloseable> resources = new ArrayList<>();

  @TempDir Path tempDir;

  private WasiContext wasiContext;

  @BeforeAll
  static void loadNativeLibrary() {
    LOGGER.info("Loading native library for WASI descriptor tests");
    try {
      NativeLibraryLoader.loadLibrary();
      LOGGER.info("Native library loaded successfully");
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library: " + e.getMessage());
      throw new RuntimeException("Native library required for integration tests", e);
    }
  }

  @BeforeEach
  void setUp() throws Exception {
    LOGGER.info("Setting up test resources");

    // Create a preopen directory
    final Path preopenDir = tempDir.resolve("preopen");
    Files.createDirectories(preopenDir);

    // Create test files for file operations
    final Path testFile = preopenDir.resolve("test.txt");
    Files.writeString(testFile, "Hello, WASI!");

    // Create WASI context with test configuration
    wasiContext =
        WasiContext.builder()
            .withSecurityValidator(TEST_SECURITY_VALIDATOR)
            .withPreopenDirectory("/", preopenDir.toString())
            .withWorkingDirectory("/")
            .build();
    resources.add(wasiContext);

    LOGGER.info("Test setup complete with preopen dir: " + preopenDir);
  }

  @AfterEach
  void tearDown() {
    LOGGER.info("Cleaning up test resources");
    // Close resources in reverse order to ensure child resources (descriptors)
    // are closed before parent resources (wasiContext)
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close resource: " + e.getMessage());
      }
    }
    resources.clear();
    wasiContext = null;
  }

  @Nested
  @DisplayName("Constructor Validation Tests")
  class ConstructorValidationTests {

    @Test
    @DisplayName("should reject zero context handle")
    void shouldRejectZeroContextHandle() {
      LOGGER.info("Testing constructor rejection of zero context handle");

      assertThrows(
          IllegalArgumentException.class,
          () -> new JniWasiDescriptor(0, 12345L),
          "Should reject zero context handle");

      LOGGER.info("Constructor correctly rejected zero context handle");
    }

    @Test
    @DisplayName("should reject zero descriptor handle")
    void shouldRejectZeroDescriptorHandle() {
      LOGGER.info("Testing constructor rejection of zero descriptor handle");

      assertThrows(
          JniValidationException.class,
          () -> new JniWasiDescriptor(12345L, 0),
          "Should reject zero descriptor handle");

      LOGGER.info("Constructor correctly rejected zero descriptor handle");
    }
  }

  @Nested
  @DisplayName("Stream Creation Tests")
  class StreamCreationTests {

    @Test
    @DisplayName("should reject negative offset for readViaStream")
    void shouldRejectNegativeOffsetForReadViaStream() throws Exception {
      LOGGER.info("Testing readViaStream rejection of negative offset");

      // This tests Java-side parameter validation only - exception thrown before native call
      final JniWasiDescriptor descriptor = new JniWasiDescriptor(12345L, 67890L);

      assertThrows(
          IllegalArgumentException.class,
          () -> descriptor.readViaStream(-1),
          "Should reject negative offset for readViaStream");

      // Mark as closed to prevent native cleanup with fake handle
      descriptor.markClosedForTesting();

      LOGGER.info("readViaStream correctly rejected negative offset");
    }

    @Test
    @DisplayName("should reject negative offset for writeViaStream")
    void shouldRejectNegativeOffsetForWriteViaStream() throws Exception {
      LOGGER.info("Testing writeViaStream rejection of negative offset");

      // This tests Java-side parameter validation only - exception thrown before native call
      final JniWasiDescriptor descriptor = new JniWasiDescriptor(12345L, 67890L);

      assertThrows(
          IllegalArgumentException.class,
          () -> descriptor.writeViaStream(-1),
          "Should reject negative offset for writeViaStream");

      // Mark as closed to prevent native cleanup with fake handle
      descriptor.markClosedForTesting();

      LOGGER.info("writeViaStream correctly rejected negative offset");
    }
  }

  @Nested
  @DisplayName("Path Operation Tests")
  class PathOperationTests {

    @Test
    @DisplayName("should reject null path for linkAt")
    void shouldRejectNullPathForLinkAt() throws Exception {
      LOGGER.info("Testing linkAt rejection of null path");

      // This tests Java-side parameter validation only - exception thrown before native call
      final JniWasiDescriptor descriptor = new JniWasiDescriptor(12345L, 67890L);

      assertThrows(
          IllegalArgumentException.class,
          () -> descriptor.linkAt(null, null, null, null),
          "Should reject null path for linkAt");

      // Mark as closed to prevent native cleanup with fake handle
      descriptor.markClosedForTesting();

      LOGGER.info("linkAt correctly rejected null path");
    }

    @Test
    @DisplayName("should reject null path for openAt")
    void shouldRejectNullPathForOpenAt() throws Exception {
      LOGGER.info("Testing openAt rejection of null path");

      // This tests Java-side parameter validation only - exception thrown before native call
      final JniWasiDescriptor descriptor = new JniWasiDescriptor(12345L, 67890L);

      assertThrows(
          IllegalArgumentException.class,
          () -> descriptor.openAt(null, null, null, null),
          "Should reject null path for openAt");

      // Mark as closed to prevent native cleanup with fake handle
      descriptor.markClosedForTesting();

      LOGGER.info("openAt correctly rejected null path");
    }

    @Test
    @DisplayName("should reject null path for unlinkFileAt")
    void shouldRejectNullPathForUnlinkFileAt() throws Exception {
      LOGGER.info("Testing unlinkFileAt rejection of null path");

      // This tests Java-side parameter validation only - exception thrown before native call
      final JniWasiDescriptor descriptor = new JniWasiDescriptor(12345L, 67890L);

      assertThrows(
          IllegalArgumentException.class,
          () -> descriptor.unlinkFileAt(null),
          "Should reject null path for unlinkFileAt");

      // Mark as closed to prevent native cleanup with fake handle
      descriptor.markClosedForTesting();

      LOGGER.info("unlinkFileAt correctly rejected null path");
    }

    @Test
    @DisplayName("should reject null path for removeDirectoryAt")
    void shouldRejectNullPathForRemoveDirectoryAt() throws Exception {
      LOGGER.info("Testing removeDirectoryAt rejection of null path");

      // This tests Java-side parameter validation only - exception thrown before native call
      final JniWasiDescriptor descriptor = new JniWasiDescriptor(12345L, 67890L);

      assertThrows(
          IllegalArgumentException.class,
          () -> descriptor.removeDirectoryAt(null),
          "Should reject null path for removeDirectoryAt");

      // Mark as closed to prevent native cleanup with fake handle
      descriptor.markClosedForTesting();

      LOGGER.info("removeDirectoryAt correctly rejected null path");
    }

    @Test
    @DisplayName("should reject null path for createDirectoryAt")
    void shouldRejectNullPathForCreateDirectoryAt() throws Exception {
      LOGGER.info("Testing createDirectoryAt rejection of null path");

      // This tests Java-side parameter validation only - exception thrown before native call
      final JniWasiDescriptor descriptor = new JniWasiDescriptor(12345L, 67890L);

      assertThrows(
          IllegalArgumentException.class,
          () -> descriptor.createDirectoryAt(null),
          "Should reject null path for createDirectoryAt");

      // Mark as closed to prevent native cleanup with fake handle
      descriptor.markClosedForTesting();

      LOGGER.info("createDirectoryAt correctly rejected null path");
    }
  }

  @Nested
  @DisplayName("Stream Creation Parameter Validation Tests")
  class StreamCreationParameterValidationTests {

    @Test
    @DisplayName("should reject negative offset for readViaStream")
    void shouldRejectNegativeOffsetForReadViaStream() throws Exception {
      LOGGER.info("Testing readViaStream rejection of negative offset");

      // This tests Java-side parameter validation only - exception thrown before native call
      final JniWasiDescriptor descriptor = new JniWasiDescriptor(12345L, 67890L);

      assertThrows(
          IllegalArgumentException.class,
          () -> descriptor.readViaStream(-1),
          "Should reject negative offset for readViaStream");

      // Mark as closed to prevent native cleanup with fake handle
      descriptor.markClosedForTesting();

      LOGGER.info("readViaStream correctly rejected negative offset");
    }

    @Test
    @DisplayName("should reject negative offset for writeViaStream")
    void shouldRejectNegativeOffsetForWriteViaStream() throws Exception {
      LOGGER.info("Testing writeViaStream rejection of negative offset");

      // This tests Java-side parameter validation only - exception thrown before native call
      final JniWasiDescriptor descriptor = new JniWasiDescriptor(12345L, 67890L);

      assertThrows(
          IllegalArgumentException.class,
          () -> descriptor.writeViaStream(-1),
          "Should reject negative offset for writeViaStream");

      // Mark as closed to prevent native cleanup with fake handle
      descriptor.markClosedForTesting();

      LOGGER.info("writeViaStream correctly rejected negative offset");
    }
  }

  @Nested
  @DisplayName("Lifecycle Tests")
  class LifecycleTests {

    @Test
    @DisplayName("should report correct resource type")
    void shouldReportCorrectResourceType() throws Exception {
      LOGGER.info("Testing resource type reporting");

      // These tests use Java-side only methods (getType, getAvailableOperations)
      final JniWasiDescriptor descriptor = new JniWasiDescriptor(12345L, 67890L);

      assertNotNull(descriptor.getType(), "Type should not be null");
      assertTrue(
          descriptor.getType().contains("descriptor")
              || descriptor.getType().contains("filesystem"),
          "Type should indicate filesystem/descriptor");

      // Mark as closed to prevent native cleanup with fake handle
      descriptor.markClosedForTesting();

      LOGGER.info("Resource type: " + descriptor.getType());
    }

    @Test
    @DisplayName("should provide available operations")
    void shouldProvideAvailableOperations() throws Exception {
      LOGGER.info("Testing available operations list");

      // These tests use Java-side only methods (getType, getAvailableOperations)
      final JniWasiDescriptor descriptor = new JniWasiDescriptor(12345L, 67890L);

      final var operations = descriptor.getAvailableOperations();
      assertNotNull(operations, "Operations list should not be null");
      assertTrue(operations.size() > 0, "Should have some available operations");

      // Mark as closed to prevent native cleanup with fake handle
      descriptor.markClosedForTesting();

      LOGGER.info("Available operations: " + operations);
    }
  }

  @Nested
  @DisplayName("WASI Context Availability Tests")
  class WasiContextAvailabilityTests {

    @Test
    @DisplayName("should have valid WASI context after setup")
    void shouldHaveValidWasiContextAfterSetup() {
      LOGGER.info("Verifying WASI context is valid");

      assertNotNull(wasiContext, "WASI context should not be null");
      assertTrue(wasiContext.isValid(), "WASI context should be valid");

      LOGGER.info("WASI context is valid and ready for descriptor operations");
    }

    @Test
    @DisplayName("should have valid native handle in WASI context")
    void shouldHaveValidNativeHandleInWasiContext() {
      LOGGER.info("Verifying WASI context has valid native handle");

      assertNotNull(wasiContext, "WASI context should not be null");
      assertTrue(wasiContext.getNativeHandle() != 0, "Native handle should not be zero");

      LOGGER.info("WASI context native handle: " + wasiContext.getNativeHandle());
    }
  }
}
