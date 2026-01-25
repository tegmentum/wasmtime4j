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
import ai.tegmentum.wasmtime4j.panama.wasi.WasiContext;
import ai.tegmentum.wasmtime4j.panama.wasi.cli.PanamaWasiStdio;
import ai.tegmentum.wasmtime4j.panama.wasi.io.PanamaWasiOutputStream;
import ai.tegmentum.wasmtime4j.panama.wasi.security.WasiSecurityValidator;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceState;
import ai.tegmentum.wasmtime4j.wasi.io.WasiOutputStream;
import java.lang.foreign.MemorySegment;
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
 * Integration tests for Panama WasiOutputStream.
 *
 * <p>These tests verify the Panama FFI implementation of WasiOutputStream, including write
 * operations, flush functionality, and resource lifecycle management.
 *
 * <p><b>NOTE:</b> This entire test class is disabled because Panama native WASI context creation is
 * not fully implemented. Once the native implementation is complete, these tests can be enabled.
 *
 * @since 1.0.0
 */
@DisplayName("Panama WasiOutputStream Integration Tests")
// @Disabled temporarily removed - crash fixed by correcting type mismatch in panama_wasi_io_ffi.rs
// @Disabled(
//     "JVM crash in wasmtime4j_panama_wasi_stdio_get_stdout - Panama WASI CLI FFI needs"
//         + " investigation")
class WasiOutputStreamIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(WasiOutputStreamIntegrationTest.class.getName());

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
    LOGGER.info("Loading native library for Panama WASI output stream tests");
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

    // Create a preopen directory
    final Path preopenDir = tempDir.resolve("preopen");
    Files.createDirectories(preopenDir);

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
    // Close resources in reverse order to ensure child resources (streams)
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
    @DisplayName("should reject null context handle")
    void shouldRejectNullContextHandle() {
      LOGGER.info("Testing constructor rejection of null context handle");

      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaWasiOutputStream(null, MemorySegment.NULL),
          "Should reject null context handle");

      LOGGER.info("Constructor correctly rejected null context handle");
    }

    @Test
    @DisplayName("should reject null stream handle")
    void shouldRejectNullStreamHandle() {
      LOGGER.info("Testing constructor rejection of null stream handle");

      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaWasiOutputStream(MemorySegment.ofAddress(12345L), null),
          "Should reject null stream handle");

      LOGGER.info("Constructor correctly rejected null stream handle");
    }
  }

  @Nested
  @DisplayName("Stream Metadata Tests")
  class StreamMetadataTests {

    @Test
    @DisplayName("should report correct stream type for stdout")
    void shouldReportCorrectStreamTypeForStdout() throws Exception {
      LOGGER.info("Testing stdout stream type reporting");

      final PanamaWasiStdio stdio = new PanamaWasiStdio(wasiContext.getNativeHandle());
      final WasiOutputStream stdout = stdio.getStdout();
      resources.add(stdout);

      assertEquals("wasi:io/output-stream", stdout.getType(), "Should report correct type");

      LOGGER.info("Stream type: " + stdout.getType());
    }

    @Test
    @DisplayName("should report correct stream type for stderr")
    void shouldReportCorrectStreamTypeForStderr() throws Exception {
      LOGGER.info("Testing stderr stream type reporting");

      final PanamaWasiStdio stdio = new PanamaWasiStdio(wasiContext.getNativeHandle());
      final WasiOutputStream stderr = stdio.getStderr();
      resources.add(stderr);

      assertEquals("wasi:io/output-stream", stderr.getType(), "Should report correct type");

      LOGGER.info("Stream type: " + stderr.getType());
    }

    @Test
    @DisplayName("should report available operations")
    void shouldReportAvailableOperations() throws Exception {
      LOGGER.info("Testing available operations reporting");

      final PanamaWasiStdio stdio = new PanamaWasiStdio(wasiContext.getNativeHandle());
      final WasiOutputStream stdout = stdio.getStdout();
      resources.add(stdout);

      final List<String> operations = stdout.getAvailableOperations();
      assertNotNull(operations, "Operations list should not be null");
      assertFalse(operations.isEmpty(), "Operations list should not be empty");

      assertTrue(operations.contains("write"), "Should support write operation");
      assertTrue(operations.contains("flush"), "Should support flush operation");
      assertTrue(
          operations.contains("blocking-write-and-flush"),
          "Should support blocking-write-and-flush operation");

      LOGGER.info("Available operations: " + operations);
    }
  }

  @Nested
  @DisplayName("Stream State Tests")
  class StreamStateTests {

    @Test
    @DisplayName("should report valid state when open")
    void shouldReportValidStateWhenOpen() throws Exception {
      LOGGER.info("Testing stream state when open");

      final PanamaWasiStdio stdio = new PanamaWasiStdio(wasiContext.getNativeHandle());
      final WasiOutputStream stdout = stdio.getStdout();
      resources.add(stdout);

      assertTrue(stdout.isValid(), "Stream should be valid when open");
      assertTrue(stdout.isOwned(), "Stream should be owned");
      assertEquals(WasiResourceState.ACTIVE, stdout.getState(), "State should be ACTIVE when open");

      LOGGER.info("Stream state verified as open and valid");
    }

    @Test
    @DisplayName("should report closed state after close")
    void shouldReportClosedStateAfterClose() throws Exception {
      LOGGER.info("Testing stream state after close");

      final PanamaWasiStdio stdio = new PanamaWasiStdio(wasiContext.getNativeHandle());
      final WasiOutputStream stdout = stdio.getStdout();
      // Don't add to resources - we're closing manually

      stdout.close();

      assertFalse(stdout.isValid(), "Stream should be invalid after close");
      assertEquals(
          WasiResourceState.CLOSED, stdout.getState(), "State should be CLOSED after close");

      LOGGER.info("Stream state correctly updated after close");
    }
  }

  @Nested
  @DisplayName("Write Parameter Validation Tests")
  class WriteParameterValidationTests {

    @Test
    @DisplayName("should reject null contents for write")
    void shouldRejectNullContentsForWrite() throws Exception {
      LOGGER.info("Testing write rejection of null contents");

      final PanamaWasiStdio stdio = new PanamaWasiStdio(wasiContext.getNativeHandle());
      final WasiOutputStream stdout = stdio.getStdout();
      resources.add(stdout);

      assertThrows(
          IllegalArgumentException.class,
          () -> stdout.write(null),
          "Should reject null contents for write");

      LOGGER.info("Write correctly rejected null contents");
    }

    @Test
    @DisplayName("should reject null contents for blockingWriteAndFlush")
    void shouldRejectNullContentsForBlockingWriteAndFlush() throws Exception {
      LOGGER.info("Testing blockingWriteAndFlush rejection of null contents");

      final PanamaWasiStdio stdio = new PanamaWasiStdio(wasiContext.getNativeHandle());
      final WasiOutputStream stdout = stdio.getStdout();
      resources.add(stdout);

      assertThrows(
          IllegalArgumentException.class,
          () -> stdout.blockingWriteAndFlush(null),
          "Should reject null contents for blockingWriteAndFlush");

      LOGGER.info("BlockingWriteAndFlush correctly rejected null contents");
    }

    @Test
    @DisplayName("should reject negative length for writeZeroes")
    void shouldRejectNegativeLengthForWriteZeroes() throws Exception {
      LOGGER.info("Testing writeZeroes rejection of negative length");

      final PanamaWasiStdio stdio = new PanamaWasiStdio(wasiContext.getNativeHandle());
      final WasiOutputStream stdout = stdio.getStdout();
      resources.add(stdout);

      assertThrows(
          IllegalArgumentException.class,
          () -> stdout.writeZeroes(-1),
          "Should reject negative length for writeZeroes");

      LOGGER.info("WriteZeroes correctly rejected negative length");
    }
  }

  @Nested
  @DisplayName("Splice Parameter Validation Tests")
  class SpliceParameterValidationTests {

    @Test
    @DisplayName("should reject null source for splice")
    void shouldRejectNullSourceForSplice() throws Exception {
      LOGGER.info("Testing splice rejection of null source");

      final PanamaWasiStdio stdio = new PanamaWasiStdio(wasiContext.getNativeHandle());
      final WasiOutputStream stdout = stdio.getStdout();
      resources.add(stdout);

      assertThrows(
          IllegalArgumentException.class,
          () -> stdout.splice(null, 100),
          "Should reject null source for splice");

      LOGGER.info("Splice correctly rejected null source");
    }

    @Test
    @DisplayName("should reject negative length for splice")
    void shouldRejectNegativeLengthForSplice() throws Exception {
      LOGGER.info("Testing splice rejection of negative length");

      final PanamaWasiStdio stdio = new PanamaWasiStdio(wasiContext.getNativeHandle());
      final WasiOutputStream stdout = stdio.getStdout();
      resources.add(stdout);

      // Using stdin as source
      final var stdin = stdio.getStdin();
      resources.add(stdin);

      assertThrows(
          IllegalArgumentException.class,
          () -> stdout.splice(stdin, -1),
          "Should reject negative length for splice");

      LOGGER.info("Splice correctly rejected negative length");
    }
  }

  @Nested
  @DisplayName("Invoke Operation Tests")
  class InvokeOperationTests {

    @Test
    @DisplayName("should reject null operation name")
    void shouldRejectNullOperationName() throws Exception {
      LOGGER.info("Testing invoke rejection of null operation");

      final PanamaWasiStdio stdio = new PanamaWasiStdio(wasiContext.getNativeHandle());
      final WasiOutputStream stdout = stdio.getStdout();
      resources.add(stdout);

      assertThrows(
          IllegalArgumentException.class,
          () -> stdout.invoke(null),
          "Should reject null operation name");

      LOGGER.info("Invoke correctly rejected null operation");
    }

    @Test
    @DisplayName("should reject empty operation name")
    void shouldRejectEmptyOperationName() throws Exception {
      LOGGER.info("Testing invoke rejection of empty operation");

      final PanamaWasiStdio stdio = new PanamaWasiStdio(wasiContext.getNativeHandle());
      final WasiOutputStream stdout = stdio.getStdout();
      resources.add(stdout);

      assertThrows(
          IllegalArgumentException.class,
          () -> stdout.invoke(""),
          "Should reject empty operation name");

      LOGGER.info("Invoke correctly rejected empty operation");
    }
  }

  @Nested
  @DisplayName("Resource Handle Tests")
  class ResourceHandleTests {

    @Test
    @DisplayName("should create resource handle")
    void shouldCreateResourceHandle() throws Exception {
      LOGGER.info("Testing resource handle creation");

      final PanamaWasiStdio stdio = new PanamaWasiStdio(wasiContext.getNativeHandle());
      final WasiOutputStream stdout = stdio.getStdout();
      resources.add(stdout);

      final var handle = stdout.createHandle();

      assertNotNull(handle, "Resource handle should not be null");
      assertTrue(handle.isValid(), "Handle should be valid");
      assertEquals(stdout.getId(), handle.getResourceId(), "Handle ID should match stream ID");
      assertEquals(
          stdout.getType(), handle.getResourceType(), "Handle type should match stream type");

      LOGGER.info("Resource handle created successfully");
    }
  }

  @Nested
  @DisplayName("Ownership Transfer Tests")
  class OwnershipTransferTests {

    @Test
    @DisplayName("should reject null target instance for ownership transfer")
    void shouldRejectNullTargetInstanceForOwnershipTransfer() throws Exception {
      LOGGER.info("Testing ownership transfer rejection of null target");

      final PanamaWasiStdio stdio = new PanamaWasiStdio(wasiContext.getNativeHandle());
      final WasiOutputStream stdout = stdio.getStdout();
      resources.add(stdout);

      assertThrows(
          IllegalArgumentException.class,
          () -> stdout.transferOwnership(null),
          "Should reject null target instance");

      LOGGER.info("Ownership transfer correctly rejected null target");
    }
  }
}
