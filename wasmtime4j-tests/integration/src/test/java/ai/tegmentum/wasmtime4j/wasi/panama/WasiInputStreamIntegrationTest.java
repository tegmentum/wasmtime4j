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
import ai.tegmentum.wasmtime4j.panama.wasi.io.PanamaWasiInputStream;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceState;
import ai.tegmentum.wasmtime4j.wasi.io.WasiInputStream;
import ai.tegmentum.wasmtime4j.wasi.security.WasiSecurityValidator;
import java.lang.foreign.MemorySegment;
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
 * Integration tests for Panama WasiInputStream.
 *
 * <p>These tests verify the Panama FFI implementation of WasiInputStream, including read
 * operations, skip functionality, and resource lifecycle management.
 *
 * <p><b>NOTE:</b> This entire test class is disabled because Panama native WASI context creation is
 * not fully implemented. Once the native implementation is complete, these tests can be enabled.
 *
 * @since 1.0.0
 */
@DisplayName("Panama WasiInputStream Integration Tests")
// @Disabled temporarily removed to investigate JVM crash
// @Disabled(
//     "JVM crash in wasmtime4j_panama_wasi_stdio_get_stdin - Panama WASI CLI FFI needs
// investigation")
class WasiInputStreamIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(WasiInputStreamIntegrationTest.class.getName());

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
    LOGGER.info("Loading native library for Panama WASI input stream tests");
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
          () -> new PanamaWasiInputStream(null, MemorySegment.NULL),
          "Should reject null context handle");

      LOGGER.info("Constructor correctly rejected null context handle");
    }

    @Test
    @DisplayName("should reject null stream handle")
    void shouldRejectNullStreamHandle() {
      LOGGER.info("Testing constructor rejection of null stream handle");

      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaWasiInputStream(MemorySegment.ofAddress(12345L), null),
          "Should reject null stream handle");

      LOGGER.info("Constructor correctly rejected null stream handle");
    }
  }

  @Nested
  @DisplayName("Stream Metadata Tests")
  class StreamMetadataTests {

    @Test
    @DisplayName("should report correct stream type")
    void shouldReportCorrectStreamType() throws Exception {
      LOGGER.info("Testing stream type reporting");

      final PanamaWasiStdio stdio = new PanamaWasiStdio(wasiContext.getNativeHandle());
      final WasiInputStream stdin = stdio.getStdin();
      resources.add(stdin);

      assertEquals("wasi:io/input-stream", stdin.getType(), "Should report correct type");

      LOGGER.info("Stream type: " + stdin.getType());
    }

    @Test
    @DisplayName("should report available operations")
    void shouldReportAvailableOperations() throws Exception {
      LOGGER.info("Testing available operations reporting");

      final PanamaWasiStdio stdio = new PanamaWasiStdio(wasiContext.getNativeHandle());
      final WasiInputStream stdin = stdio.getStdin();
      resources.add(stdin);

      final List<String> operations = stdin.getAvailableOperations();
      assertNotNull(operations, "Operations list should not be null");
      assertFalse(operations.isEmpty(), "Operations list should not be empty");

      assertTrue(operations.contains("read"), "Should support read operation");
      assertTrue(operations.contains("blocking-read"), "Should support blocking-read operation");
      assertTrue(operations.contains("skip"), "Should support skip operation");

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
      final WasiInputStream stdin = stdio.getStdin();
      resources.add(stdin);

      assertTrue(stdin.isValid(), "Stream should be valid when open");
      assertTrue(stdin.isOwned(), "Stream should be owned");
      assertEquals(WasiResourceState.ACTIVE, stdin.getState(), "State should be ACTIVE when open");

      LOGGER.info("Stream state verified as open and valid");
    }

    @Test
    @DisplayName("should report closed state after close")
    void shouldReportClosedStateAfterClose() throws Exception {
      LOGGER.info("Testing stream state after close");

      final PanamaWasiStdio stdio = new PanamaWasiStdio(wasiContext.getNativeHandle());
      final WasiInputStream stdin = stdio.getStdin();
      // Don't add to resources - we're closing manually

      stdin.close();

      assertFalse(stdin.isValid(), "Stream should be invalid after close");
      assertEquals(
          WasiResourceState.CLOSED, stdin.getState(), "State should be CLOSED after close");

      LOGGER.info("Stream state correctly updated after close");
    }
  }

  @Nested
  @DisplayName("Read Parameter Validation Tests")
  class ReadParameterValidationTests {

    @Test
    @DisplayName("should reject zero length for read")
    void shouldRejectZeroLengthForRead() throws Exception {
      LOGGER.info("Testing read rejection of zero length");

      final PanamaWasiStdio stdio = new PanamaWasiStdio(wasiContext.getNativeHandle());
      final WasiInputStream stdin = stdio.getStdin();
      resources.add(stdin);

      assertThrows(
          IllegalArgumentException.class,
          () -> stdin.read(0),
          "Should reject zero length for read");

      LOGGER.info("Read correctly rejected zero length");
    }

    @Test
    @DisplayName("should reject negative length for read")
    void shouldRejectNegativeLengthForRead() throws Exception {
      LOGGER.info("Testing read rejection of negative length");

      final PanamaWasiStdio stdio = new PanamaWasiStdio(wasiContext.getNativeHandle());
      final WasiInputStream stdin = stdio.getStdin();
      resources.add(stdin);

      assertThrows(
          IllegalArgumentException.class,
          () -> stdin.read(-1),
          "Should reject negative length for read");

      LOGGER.info("Read correctly rejected negative length");
    }

    @Test
    @DisplayName("should reject zero length for skip")
    void shouldRejectZeroLengthForSkip() throws Exception {
      LOGGER.info("Testing skip rejection of zero length");

      final PanamaWasiStdio stdio = new PanamaWasiStdio(wasiContext.getNativeHandle());
      final WasiInputStream stdin = stdio.getStdin();
      resources.add(stdin);

      assertThrows(
          IllegalArgumentException.class,
          () -> stdin.skip(0),
          "Should reject zero length for skip");

      LOGGER.info("Skip correctly rejected zero length");
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
      final WasiInputStream stdin = stdio.getStdin();
      resources.add(stdin);

      assertThrows(
          IllegalArgumentException.class,
          () -> stdin.invoke(null),
          "Should reject null operation name");

      LOGGER.info("Invoke correctly rejected null operation");
    }

    @Test
    @DisplayName("should reject empty operation name")
    void shouldRejectEmptyOperationName() throws Exception {
      LOGGER.info("Testing invoke rejection of empty operation");

      final PanamaWasiStdio stdio = new PanamaWasiStdio(wasiContext.getNativeHandle());
      final WasiInputStream stdin = stdio.getStdin();
      resources.add(stdin);

      assertThrows(
          IllegalArgumentException.class,
          () -> stdin.invoke(""),
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
      final WasiInputStream stdin = stdio.getStdin();
      resources.add(stdin);

      final var handle = stdin.createHandle();

      assertNotNull(handle, "Resource handle should not be null");
      assertTrue(handle.isValid(), "Handle should be valid");
      assertEquals(stdin.getId(), handle.getResourceId(), "Handle ID should match stream ID");
      assertEquals(
          stdin.getType(), handle.getResourceType(), "Handle type should match stream type");

      LOGGER.info("Resource handle created successfully");
    }
  }

  @Nested
  @DisplayName("PanamaWasiStdio Constructor Tests")
  class PanamaWasiStdioConstructorTests {

    @Test
    @DisplayName("should reject null context handle for stdio")
    void shouldRejectNullContextHandleForStdio() {
      LOGGER.info("Testing stdio constructor rejection of null context handle");

      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaWasiStdio(null),
          "Should reject null context handle");

      LOGGER.info("Stdio constructor correctly rejected null context handle");
    }
  }
}
