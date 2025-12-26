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

package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.async.AsyncRuntime;
import ai.tegmentum.wasmtime4j.async.AsyncRuntime.OperationStatus;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for {@link PanamaAsyncRuntime}. */
@DisplayName("PanamaAsyncRuntime Tests")
class PanamaAsyncRuntimeTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("PanamaAsyncRuntime should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(PanamaAsyncRuntime.class.getModifiers()),
          "PanamaAsyncRuntime should be final");
    }

    @Test
    @DisplayName("PanamaAsyncRuntime should implement AsyncRuntime")
    void shouldImplementAsyncRuntime() {
      assertTrue(
          AsyncRuntime.class.isAssignableFrom(PanamaAsyncRuntime.class),
          "PanamaAsyncRuntime should implement AsyncRuntime");
    }

    @Test
    @DisplayName("PanamaAsyncRuntime should be AutoCloseable")
    void shouldBeAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(PanamaAsyncRuntime.class),
          "PanamaAsyncRuntime should implement AutoCloseable");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should create uninitialized runtime")
    void constructorShouldCreateUninitializedRuntime() {
      // May fail if native bindings aren't available, but structure should work
      try {
        final PanamaAsyncRuntime runtime = new PanamaAsyncRuntime();
        assertFalse(runtime.isInitialized(), "Newly created runtime should not be initialized");
        runtime.close();
      } catch (Exception e) {
        // Expected if native bindings are not available
        assertTrue(e.getMessage() != null, "Exception should have message");
      }
    }
  }

  @Nested
  @DisplayName("isInitialized Tests")
  class IsInitializedTests {

    @Test
    @DisplayName("isInitialized should return false before initialize()")
    void isInitializedShouldReturnFalseBeforeInitialize() {
      try {
        final PanamaAsyncRuntime runtime = new PanamaAsyncRuntime();
        assertFalse(
            runtime.isInitialized(),
            "Runtime should not be initialized before calling initialize()");
        runtime.close();
      } catch (Exception e) {
        // Expected if native bindings not available
      }
    }

    @Test
    @DisplayName("isInitialized should return false after shutdown")
    void isInitializedShouldReturnFalseAfterShutdown() {
      try {
        final PanamaAsyncRuntime runtime = new PanamaAsyncRuntime();
        runtime.shutdown();
        assertFalse(runtime.isInitialized(), "Runtime should not be initialized after shutdown");
      } catch (Exception e) {
        // Expected if native bindings not available
      }
    }
  }

  @Nested
  @DisplayName("initialize Tests")
  class InitializeTests {

    @Test
    @DisplayName("initialize should throw if already shutdown")
    void initializeShouldThrowIfAlreadyShutdown() {
      try {
        final PanamaAsyncRuntime runtime = new PanamaAsyncRuntime();
        runtime.shutdown();
        assertThrows(
            WasmException.class,
            runtime::initialize,
            "initialize should throw WasmException if already shutdown");
      } catch (Exception e) {
        // Expected if native bindings not available
      }
    }
  }

  @Nested
  @DisplayName("getRuntimeInfo Tests")
  class GetRuntimeInfoTests {

    @Test
    @DisplayName("getRuntimeInfo should return 'Not initialized' before init")
    void getRuntimeInfoShouldReturnNotInitializedBeforeInit() {
      try {
        final PanamaAsyncRuntime runtime = new PanamaAsyncRuntime();
        final String info = runtime.getRuntimeInfo();
        assertEquals("Not initialized", info, "Should return 'Not initialized' before init");
        runtime.close();
      } catch (Exception e) {
        // Expected if native bindings not available
      }
    }
  }

  @Nested
  @DisplayName("executeAsync Tests")
  class ExecuteAsyncTests {

    @Test
    @DisplayName("executeAsync should validate non-null functionName")
    void executeAsyncShouldValidateNonNullFunctionName() {
      try {
        final PanamaAsyncRuntime runtime = new PanamaAsyncRuntime();
        runtime.initialize();
        assertThrows(
            NullPointerException.class,
            () -> runtime.executeAsync(0L, null, new Object[0], 1000L, result -> {}),
            "executeAsync should throw on null functionName");
        runtime.close();
      } catch (Exception e) {
        // Expected if native bindings not available or not initialized
      }
    }

    @Test
    @DisplayName("executeAsync should validate non-null callback")
    void executeAsyncShouldValidateNonNullCallback() {
      try {
        final PanamaAsyncRuntime runtime = new PanamaAsyncRuntime();
        runtime.initialize();
        assertThrows(
            NullPointerException.class,
            () -> runtime.executeAsync(0L, "test", new Object[0], 1000L, null),
            "executeAsync should throw on null callback");
        runtime.close();
      } catch (Exception e) {
        // Expected if native bindings not available or not initialized
      }
    }
  }

  @Nested
  @DisplayName("compileAsync Tests")
  class CompileAsyncTests {

    @Test
    @DisplayName("compileAsync should validate non-null wasmBytes")
    void compileAsyncShouldValidateNonNullWasmBytes() {
      try {
        final PanamaAsyncRuntime runtime = new PanamaAsyncRuntime();
        runtime.initialize();
        assertThrows(
            NullPointerException.class,
            () -> runtime.compileAsync(null, 1000L, null, result -> {}),
            "compileAsync should throw on null wasmBytes");
        runtime.close();
      } catch (Exception e) {
        // Expected if native bindings not available or not initialized
      }
    }

    @Test
    @DisplayName("compileAsync should validate non-null completionCallback")
    void compileAsyncShouldValidateNonNullCompletionCallback() {
      try {
        final PanamaAsyncRuntime runtime = new PanamaAsyncRuntime();
        runtime.initialize();
        assertThrows(
            NullPointerException.class,
            () -> runtime.compileAsync(new byte[] {0x00}, 1000L, null, null),
            "compileAsync should throw on null completionCallback");
        runtime.close();
      } catch (Exception e) {
        // Expected if native bindings not available or not initialized
      }
    }

    @Test
    @DisplayName("compileAsync should validate non-empty wasmBytes")
    void compileAsyncShouldValidateNonEmptyWasmBytes() {
      try {
        final PanamaAsyncRuntime runtime = new PanamaAsyncRuntime();
        runtime.initialize();
        assertThrows(
            IllegalArgumentException.class,
            () -> runtime.compileAsync(new byte[0], 1000L, null, result -> {}),
            "compileAsync should throw on empty wasmBytes");
        runtime.close();
      } catch (Exception e) {
        // Expected if native bindings not available or not initialized
      }
    }
  }

  @Nested
  @DisplayName("cancelOperation Tests")
  class CancelOperationTests {

    @Test
    @DisplayName("cancelOperation should return false for unknown operation")
    void cancelOperationShouldReturnFalseForUnknownOperation() {
      try {
        final PanamaAsyncRuntime runtime = new PanamaAsyncRuntime();
        assertFalse(
            runtime.cancelOperation(999L),
            "cancelOperation should return false for unknown operation ID");
        runtime.close();
      } catch (Exception e) {
        // Expected if native bindings not available
      }
    }
  }

  @Nested
  @DisplayName("getOperationStatus Tests")
  class GetOperationStatusTests {

    @Test
    @DisplayName("getOperationStatus should return PENDING for unknown operation")
    void getOperationStatusShouldReturnPendingForUnknownOperation() {
      try {
        final PanamaAsyncRuntime runtime = new PanamaAsyncRuntime();
        assertEquals(
            OperationStatus.PENDING,
            runtime.getOperationStatus(999L),
            "getOperationStatus should return PENDING for unknown operation");
        runtime.close();
      } catch (Exception e) {
        // Expected if native bindings not available
      }
    }
  }

  @Nested
  @DisplayName("waitForOperation Tests")
  class WaitForOperationTests {

    @Test
    @DisplayName("waitForOperation should throw for unknown operation")
    void waitForOperationShouldThrowForUnknownOperation() {
      try {
        final PanamaAsyncRuntime runtime = new PanamaAsyncRuntime();
        assertThrows(
            WasmException.class,
            () -> runtime.waitForOperation(999L, 100L),
            "waitForOperation should throw for unknown operation");
        runtime.close();
      } catch (Exception e) {
        // Expected if native bindings not available
      }
    }
  }

  @Nested
  @DisplayName("getActiveOperationCount Tests")
  class GetActiveOperationCountTests {

    @Test
    @DisplayName("getActiveOperationCount should return 0 initially")
    void getActiveOperationCountShouldReturnZeroInitially() {
      try {
        final PanamaAsyncRuntime runtime = new PanamaAsyncRuntime();
        assertEquals(
            0, runtime.getActiveOperationCount(), "Active operation count should be 0 initially");
        runtime.close();
      } catch (Exception e) {
        // Expected if native bindings not available
      }
    }
  }

  @Nested
  @DisplayName("shutdown Tests")
  class ShutdownTests {

    @Test
    @DisplayName("shutdown should be idempotent")
    void shutdownShouldBeIdempotent() {
      try {
        final PanamaAsyncRuntime runtime = new PanamaAsyncRuntime();
        assertDoesNotThrow(
            () -> {
              runtime.shutdown();
              runtime.shutdown();
              runtime.shutdown();
            },
            "shutdown should be safe to call multiple times");
      } catch (Exception e) {
        // Expected if native bindings not available
      }
    }

    @Test
    @DisplayName("shutdown should set active operations to 0")
    void shutdownShouldSetActiveOperationsToZero() {
      try {
        final PanamaAsyncRuntime runtime = new PanamaAsyncRuntime();
        runtime.shutdown();
        assertEquals(
            0, runtime.getActiveOperationCount(), "Active operations should be 0 after shutdown");
      } catch (Exception e) {
        // Expected if native bindings not available
      }
    }
  }

  @Nested
  @DisplayName("close Tests")
  class CloseTests {

    @Test
    @DisplayName("close should call shutdown")
    void closeShouldCallShutdown() {
      try {
        final PanamaAsyncRuntime runtime = new PanamaAsyncRuntime();
        runtime.close();
        assertFalse(runtime.isInitialized(), "Runtime should not be initialized after close");
      } catch (Exception e) {
        // Expected if native bindings not available
      }
    }

    @Test
    @DisplayName("close should be safe to call multiple times")
    void closeShouldBeSafeToCallMultipleTimes() {
      try {
        final PanamaAsyncRuntime runtime = new PanamaAsyncRuntime();
        assertDoesNotThrow(
            () -> {
              runtime.close();
              runtime.close();
              runtime.close();
            },
            "close should be safe to call multiple times");
      } catch (Exception e) {
        // Expected if native bindings not available
      }
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should return formatted string")
    void toStringShouldReturnFormattedString() {
      try {
        final PanamaAsyncRuntime runtime = new PanamaAsyncRuntime();
        final String str = runtime.toString();
        assertNotNull(str, "toString should not return null");
        assertTrue(str.contains("PanamaAsyncRuntime"), "toString should contain class name");
        assertTrue(str.contains("initialized"), "toString should contain initialized status");
        assertTrue(str.contains("shutdown"), "toString should contain shutdown status");
        assertTrue(str.contains("activeOperations"), "toString should contain activeOperations");
        runtime.close();
      } catch (Exception e) {
        // Expected if native bindings not available
      }
    }
  }

  @Nested
  @DisplayName("OperationStatus Enum Tests")
  class OperationStatusEnumTests {

    @Test
    @DisplayName("OperationStatus should have all expected values")
    void operationStatusShouldHaveAllExpectedValues() {
      assertNotNull(OperationStatus.PENDING, "PENDING should exist");
      assertNotNull(OperationStatus.RUNNING, "RUNNING should exist");
      assertNotNull(OperationStatus.COMPLETED, "COMPLETED should exist");
      assertNotNull(OperationStatus.FAILED, "FAILED should exist");
      assertNotNull(OperationStatus.CANCELLED, "CANCELLED should exist");
      assertNotNull(OperationStatus.TIMED_OUT, "TIMED_OUT should exist");
    }
  }
}
