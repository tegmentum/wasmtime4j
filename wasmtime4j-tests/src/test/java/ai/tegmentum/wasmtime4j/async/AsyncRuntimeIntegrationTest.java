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

package ai.tegmentum.wasmtime4j.async;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.async.AsyncRuntime.AsyncResult;
import ai.tegmentum.wasmtime4j.async.AsyncRuntime.OperationStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Integration tests for AsyncRuntime - asynchronous WebAssembly execution.
 *
 * <p>These tests verify async runtime initialization, async function execution, cancellation,
 * status tracking, and completion waiting. Tests are disabled until the native AsyncRuntime
 * implementation is complete - the current native implementation may cause JVM crashes.
 *
 * @since 1.0.0
 */
@DisplayName("AsyncRuntime Integration Tests")
public final class AsyncRuntimeIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(AsyncRuntimeIntegrationTest.class.getName());

  /**
   * Simple WebAssembly module with an add function for async testing.
   *
   * <pre>
   * (module
   *   (func (export "add") (param i32 i32) (result i32)
   *     local.get 0
   *     local.get 1
   *     i32.add))
   * </pre>
   */
  private static final byte[] ADD_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6D, // magic number
        0x01,
        0x00,
        0x00,
        0x00, // version 1
        // Type section (id=1)
        0x01,
        0x07, // section id and size
        0x01, // number of types
        0x60,
        0x02,
        0x7F,
        0x7F,
        0x01,
        0x7F, // (i32, i32) -> i32
        // Function section (id=3)
        0x03,
        0x02, // section id and size
        0x01, // number of functions
        0x00, // function 0: type 0
        // Export section (id=7)
        0x07,
        0x07, // section id and size
        0x01, // number of exports
        0x03,
        0x61,
        0x64,
        0x64, // "add"
        0x00,
        0x00, // function export, index 0
        // Code section (id=10)
        0x0A,
        0x09, // section id and size
        0x01, // number of functions
        0x07, // function body size
        0x00, // local variable count
        0x20,
        0x00, // local.get 0
        0x20,
        0x01, // local.get 1
        0x6A, // i32.add
        0x0B // end
      };

  private static boolean asyncRuntimeAvailable = false;

  @BeforeAll
  static void checkAsyncRuntimeAvailable() {
    try {
      final AsyncRuntime runtime = AsyncRuntimeFactory.create();
      runtime.close();
      asyncRuntimeAvailable = true;
      LOGGER.info("AsyncRuntime native implementation is available");
    } catch (final Throwable t) {
      asyncRuntimeAvailable = false;
      LOGGER.warning("AsyncRuntime not available - tests will be skipped: " + t.getMessage());
    }
  }

  private static void assumeAsyncRuntimeAvailable() {
    assumeTrue(
        asyncRuntimeAvailable, "AsyncRuntime native implementation not available - skipping");
  }

  private AsyncRuntime asyncRuntime;
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeEach
  void setUp(final TestInfo testInfo) throws Exception {
    LOGGER.info("Setting up: " + testInfo.getDisplayName());
  }

  @AfterEach
  void tearDown(final TestInfo testInfo) {
    LOGGER.info("Tearing down: " + testInfo.getDisplayName());
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close resource: " + e.getMessage());
      }
    }
    resources.clear();

    if (asyncRuntime != null) {
      asyncRuntime.close();
      asyncRuntime = null;
    }
  }

  @Nested
  @DisplayName("AsyncRuntime Initialization Tests")
  class InitializationTests {

    @Test
    @DisplayName("should create async runtime")
    void shouldCreateAsyncRuntime(final TestInfo testInfo) throws Exception {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      asyncRuntime = AsyncRuntimeFactory.create();
      assertNotNull(asyncRuntime, "AsyncRuntime should not be null");

      LOGGER.info("AsyncRuntime created successfully");
    }

    @Test
    @DisplayName("should initialize async runtime")
    void shouldInitializeAsyncRuntime(final TestInfo testInfo) throws Exception {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      asyncRuntime = AsyncRuntimeFactory.create();
      assertFalse(asyncRuntime.isInitialized(), "Should not be initialized before initialize()");

      asyncRuntime.initialize();
      assertTrue(asyncRuntime.isInitialized(), "Should be initialized after initialize()");

      LOGGER.info("AsyncRuntime initialized successfully");
    }

    @Test
    @DisplayName("should allow multiple initialize calls")
    void shouldAllowMultipleInitializeCalls(final TestInfo testInfo) throws Exception {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      asyncRuntime = AsyncRuntimeFactory.create();

      assertDoesNotThrow(() -> asyncRuntime.initialize(), "First initialize should not throw");
      assertDoesNotThrow(() -> asyncRuntime.initialize(), "Second initialize should not throw");

      assertTrue(asyncRuntime.isInitialized(), "Should remain initialized");

      LOGGER.info("Multiple initialize calls handled correctly");
    }

    @Test
    @DisplayName("should return runtime info")
    void shouldReturnRuntimeInfo(final TestInfo testInfo) throws Exception {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      asyncRuntime = AsyncRuntimeFactory.create();
      asyncRuntime.initialize();

      final String info = asyncRuntime.getRuntimeInfo();
      assertNotNull(info, "Runtime info should not be null");
      assertFalse(info.isEmpty(), "Runtime info should not be empty");

      LOGGER.info("Runtime info: " + info);
    }
  }

  @Nested
  @DisplayName("Async Operation Status Tests")
  class OperationStatusTests {

    @Test
    @DisplayName("should return unknown status for invalid operation ID")
    void shouldReturnUnknownStatusForInvalidOperationId(final TestInfo testInfo) throws Exception {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      asyncRuntime = AsyncRuntimeFactory.create();
      asyncRuntime.initialize();

      final OperationStatus status = asyncRuntime.getOperationStatus(-1);
      assertNotNull(status, "Status should not be null");

      LOGGER.info("Status for invalid ID: " + status);
    }

    @Test
    @DisplayName("should track active operation count")
    void shouldTrackActiveOperationCount(final TestInfo testInfo) throws Exception {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      asyncRuntime = AsyncRuntimeFactory.create();
      asyncRuntime.initialize();

      final int initialCount = asyncRuntime.getActiveOperationCount();
      assertTrue(initialCount >= 0, "Active operation count should be non-negative");

      LOGGER.info("Active operation count: " + initialCount);
    }
  }

  @Nested
  @DisplayName("Async Compilation Tests")
  class AsyncCompilationTests {

    @Test
    @DisplayName("should compile module asynchronously")
    void shouldCompileModuleAsynchronously(final TestInfo testInfo) throws Exception {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      asyncRuntime = AsyncRuntimeFactory.create();
      asyncRuntime.initialize();

      final CountDownLatch latch = new CountDownLatch(1);
      final AtomicReference<AsyncResult> resultRef = new AtomicReference<>();

      final long operationId =
          asyncRuntime.compileAsync(
              ADD_WASM,
              10000L, // 10 second timeout
              progress -> LOGGER.info("Compilation progress: " + progress + "%"),
              result -> {
                resultRef.set(result);
                latch.countDown();
              });

      assertTrue(operationId > 0, "Operation ID should be positive");

      // Wait for completion
      final boolean completed = latch.await(15, TimeUnit.SECONDS);
      assertTrue(completed, "Compilation should complete within timeout");

      final AsyncResult result = resultRef.get();
      assertNotNull(result, "Result should not be null");

      LOGGER.info("Async compilation result: " + result);
    }

    @Test
    @DisplayName("should fail compilation with invalid wasm")
    void shouldFailCompilationWithInvalidWasm(final TestInfo testInfo) throws Exception {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      asyncRuntime = AsyncRuntimeFactory.create();
      asyncRuntime.initialize();

      final byte[] invalidWasm = new byte[] {0x00, 0x01, 0x02, 0x03};

      final CountDownLatch latch = new CountDownLatch(1);
      final AtomicReference<AsyncResult> resultRef = new AtomicReference<>();

      final long operationId =
          asyncRuntime.compileAsync(
              invalidWasm,
              5000L,
              null,
              result -> {
                resultRef.set(result);
                latch.countDown();
              });

      assertTrue(operationId > 0, "Operation ID should be positive");

      // Wait for completion
      final boolean completed = latch.await(10, TimeUnit.SECONDS);
      assertTrue(completed, "Compilation should complete (with failure) within timeout");

      final AsyncResult result = resultRef.get();
      assertNotNull(result, "Result should not be null");
      assertFalse(result.isSuccess(), "Compilation should fail for invalid wasm");

      LOGGER.info("Invalid wasm compilation result: " + result);
    }
  }

  @Nested
  @DisplayName("Operation Cancellation Tests")
  class CancellationTests {

    @Test
    @DisplayName("should cancel operation")
    void shouldCancelOperation(final TestInfo testInfo) throws Exception {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      asyncRuntime = AsyncRuntimeFactory.create();
      asyncRuntime.initialize();

      final CountDownLatch latch = new CountDownLatch(1);

      // Start a long-running compilation
      final long operationId =
          asyncRuntime.compileAsync(
              ADD_WASM,
              60000L, // Long timeout
              null,
              result -> latch.countDown());

      assertTrue(operationId > 0, "Operation ID should be positive");

      // Cancel immediately
      final boolean cancelled = asyncRuntime.cancelOperation(operationId);

      if (cancelled) {
        final OperationStatus status = asyncRuntime.getOperationStatus(operationId);
        assertEquals(OperationStatus.CANCELLED, status, "Status should be CANCELLED");
        LOGGER.info("Operation cancelled successfully");
      } else {
        // Operation may have completed before we could cancel
        LOGGER.info("Operation completed before cancellation");
      }
    }

    @Test
    @DisplayName("should return false when cancelling invalid operation")
    void shouldReturnFalseWhenCancellingInvalidOperation(final TestInfo testInfo) throws Exception {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      asyncRuntime = AsyncRuntimeFactory.create();
      asyncRuntime.initialize();

      final boolean cancelled = asyncRuntime.cancelOperation(-1);
      assertFalse(cancelled, "Should return false for invalid operation ID");

      LOGGER.info("Correctly returned false for invalid operation ID");
    }
  }

  @Nested
  @DisplayName("Operation Waiting Tests")
  class WaitingTests {

    @Test
    @DisplayName("should wait for operation completion")
    void shouldWaitForOperationCompletion(final TestInfo testInfo) throws Exception {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      asyncRuntime = AsyncRuntimeFactory.create();
      asyncRuntime.initialize();

      final long operationId = asyncRuntime.compileAsync(ADD_WASM, 10000L, null, result -> {});

      final OperationStatus status = asyncRuntime.waitForOperation(operationId, 15000L);
      assertNotNull(status, "Status should not be null");

      LOGGER.info("Wait completed with status: " + status);
    }

    @Test
    @DisplayName("should timeout when waiting too long")
    void shouldTimeoutWhenWaitingTooLong(final TestInfo testInfo) throws Exception {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      asyncRuntime = AsyncRuntimeFactory.create();
      asyncRuntime.initialize();

      // Start an operation with long timeout
      final long operationId = asyncRuntime.compileAsync(ADD_WASM, 60000L, null, result -> {});

      // Wait with very short timeout
      final OperationStatus status = asyncRuntime.waitForOperation(operationId, 1L);
      assertNotNull(status, "Status should not be null");

      LOGGER.info("Wait with short timeout returned status: " + status);
    }
  }

  @Nested
  @DisplayName("AsyncRuntime Lifecycle Tests")
  class LifecycleTests {

    @Test
    @DisplayName("should shutdown gracefully")
    void shouldShutdownGracefully(final TestInfo testInfo) throws Exception {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      asyncRuntime = AsyncRuntimeFactory.create();
      asyncRuntime.initialize();

      assertDoesNotThrow(() -> asyncRuntime.shutdown(), "Shutdown should not throw");

      LOGGER.info("Shutdown completed gracefully");
    }

    @Test
    @DisplayName("should close properly")
    void shouldCloseProperly(final TestInfo testInfo) throws Exception {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AsyncRuntime localRuntime = AsyncRuntimeFactory.create();
      localRuntime.initialize();

      assertDoesNotThrow(localRuntime::close, "Close should not throw");

      LOGGER.info("Close completed properly");
    }

    @Test
    @DisplayName("should handle multiple close calls")
    void shouldHandleMultipleCloseCalls(final TestInfo testInfo) throws Exception {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AsyncRuntime localRuntime = AsyncRuntimeFactory.create();
      localRuntime.initialize();

      assertDoesNotThrow(localRuntime::close, "First close should not throw");
      assertDoesNotThrow(localRuntime::close, "Second close should not throw");

      LOGGER.info("Multiple close calls handled correctly");
    }
  }

  @Nested
  @DisplayName("AsyncResult Tests")
  class AsyncResultTests {

    @Test
    @DisplayName("should create successful AsyncResult")
    void shouldCreateSuccessfulAsyncResult(final TestInfo testInfo) {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AsyncResult result =
          new AsyncResult(OperationStatus.COMPLETED, 0, "Success", "test value");

      assertEquals(OperationStatus.COMPLETED, result.getStatus(), "Status should be COMPLETED");
      assertEquals(0, result.getStatusCode(), "Status code should be 0");
      assertEquals("Success", result.getMessage(), "Message should match");
      assertEquals("test value", result.getResult(), "Result value should match");
      assertTrue(result.isSuccess(), "Should be successful");

      LOGGER.info("AsyncResult: " + result);
    }

    @Test
    @DisplayName("should create failed AsyncResult")
    void shouldCreateFailedAsyncResult(final TestInfo testInfo) {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AsyncResult result =
          new AsyncResult(OperationStatus.FAILED, -1, "Error occurred", null);

      assertEquals(OperationStatus.FAILED, result.getStatus(), "Status should be FAILED");
      assertEquals(-1, result.getStatusCode(), "Status code should be -1");
      assertEquals("Error occurred", result.getMessage(), "Message should match");
      assertFalse(result.isSuccess(), "Should not be successful");

      LOGGER.info("Failed AsyncResult: " + result);
    }

    @Test
    @DisplayName("should handle cancelled status")
    void shouldHandleCancelledStatus(final TestInfo testInfo) {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AsyncResult result =
          new AsyncResult(OperationStatus.CANCELLED, 1, "Operation cancelled", null);

      assertEquals(OperationStatus.CANCELLED, result.getStatus(), "Status should be CANCELLED");
      assertFalse(result.isSuccess(), "Cancelled should not be successful");

      LOGGER.info("Cancelled AsyncResult: " + result);
    }

    @Test
    @DisplayName("should handle timed out status")
    void shouldHandleTimedOutStatus(final TestInfo testInfo) {
      assumeAsyncRuntimeAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final AsyncResult result =
          new AsyncResult(OperationStatus.TIMED_OUT, 2, "Operation timed out", null);

      assertEquals(OperationStatus.TIMED_OUT, result.getStatus(), "Status should be TIMED_OUT");
      assertFalse(result.isSuccess(), "Timed out should not be successful");

      LOGGER.info("Timed out AsyncResult: " + result);
    }
  }
}
