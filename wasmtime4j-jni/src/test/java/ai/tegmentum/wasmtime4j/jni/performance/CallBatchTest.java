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

package ai.tegmentum.wasmtime4j.jni.performance;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.performance.CallBatch.BatchOperationType;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link CallBatch} class.
 *
 * <p>This test class verifies the CallBatch class which batches multiple native calls together
 * to reduce JNI overhead through asynchronous batch execution.
 */
@DisplayName("CallBatch Tests")
class CallBatchTest {

  private CallBatch batch;

  @BeforeEach
  void setUp() {
    batch = new CallBatch();
  }

  @AfterEach
  void tearDown() {
    if (batch != null) {
      batch.close();
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("CallBatch should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      assertTrue(AutoCloseable.class.isAssignableFrom(CallBatch.class),
          "CallBatch should implement AutoCloseable");
    }

    @Test
    @DisplayName("CallBatch should be final class")
    void shouldBeFinalClass() {
      assertTrue(java.lang.reflect.Modifier.isFinal(CallBatch.class.getModifiers()),
          "CallBatch should be final");
    }

    @Test
    @DisplayName("DEFAULT_MAX_BATCH_SIZE should be 32")
    void defaultMaxBatchSizeShouldBe32() {
      assertEquals(32, CallBatch.DEFAULT_MAX_BATCH_SIZE,
          "DEFAULT_MAX_BATCH_SIZE should be 32");
    }

    @Test
    @DisplayName("DEFAULT_BATCH_TIMEOUT_MS should be 100")
    void defaultBatchTimeoutMsShouldBe100() {
      assertEquals(100, CallBatch.DEFAULT_BATCH_TIMEOUT_MS,
          "DEFAULT_BATCH_TIMEOUT_MS should be 100");
    }
  }

  @Nested
  @DisplayName("BatchOperationType Enum Tests")
  class BatchOperationTypeTests {

    @Test
    @DisplayName("Should have FUNCTION_CALL value")
    void shouldHaveFunctionCallValue() {
      assertNotNull(BatchOperationType.valueOf("FUNCTION_CALL"),
          "Should have FUNCTION_CALL value");
    }

    @Test
    @DisplayName("Should have MEMORY_READ value")
    void shouldHaveMemoryReadValue() {
      assertNotNull(BatchOperationType.valueOf("MEMORY_READ"),
          "Should have MEMORY_READ value");
    }

    @Test
    @DisplayName("Should have MEMORY_WRITE value")
    void shouldHaveMemoryWriteValue() {
      assertNotNull(BatchOperationType.valueOf("MEMORY_WRITE"),
          "Should have MEMORY_WRITE value");
    }

    @Test
    @DisplayName("Should have GLOBAL_GET value")
    void shouldHaveGlobalGetValue() {
      assertNotNull(BatchOperationType.valueOf("GLOBAL_GET"),
          "Should have GLOBAL_GET value");
    }

    @Test
    @DisplayName("Should have GLOBAL_SET value")
    void shouldHaveGlobalSetValue() {
      assertNotNull(BatchOperationType.valueOf("GLOBAL_SET"),
          "Should have GLOBAL_SET value");
    }

    @Test
    @DisplayName("Should have exactly 5 operation types")
    void shouldHaveExactly5OperationTypes() {
      assertEquals(5, BatchOperationType.values().length,
          "Should have exactly 5 operation types");
    }

    @Test
    @DisplayName("FUNCTION_CALL should be at ordinal 0")
    void functionCallShouldBeAtOrdinal0() {
      assertEquals(0, BatchOperationType.FUNCTION_CALL.ordinal(),
          "FUNCTION_CALL should be at ordinal 0");
    }

    @Test
    @DisplayName("MEMORY_READ should be at ordinal 1")
    void memoryReadShouldBeAtOrdinal1() {
      assertEquals(1, BatchOperationType.MEMORY_READ.ordinal(),
          "MEMORY_READ should be at ordinal 1");
    }

    @Test
    @DisplayName("MEMORY_WRITE should be at ordinal 2")
    void memoryWriteShouldBeAtOrdinal2() {
      assertEquals(2, BatchOperationType.MEMORY_WRITE.ordinal(),
          "MEMORY_WRITE should be at ordinal 2");
    }

    @Test
    @DisplayName("GLOBAL_GET should be at ordinal 3")
    void globalGetShouldBeAtOrdinal3() {
      assertEquals(3, BatchOperationType.GLOBAL_GET.ordinal(),
          "GLOBAL_GET should be at ordinal 3");
    }

    @Test
    @DisplayName("GLOBAL_SET should be at ordinal 4")
    void globalSetShouldBeAtOrdinal4() {
      assertEquals(4, BatchOperationType.GLOBAL_SET.ordinal(),
          "GLOBAL_SET should be at ordinal 4");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Default constructor should create batch with default configuration")
    void defaultConstructorShouldCreateBatchWithDefaultConfiguration() {
      final CallBatch defaultBatch = new CallBatch();
      try {
        assertEquals(CallBatch.DEFAULT_MAX_BATCH_SIZE, defaultBatch.getMaxBatchSize(),
            "Should use default max batch size");
        assertEquals(CallBatch.DEFAULT_BATCH_TIMEOUT_MS, defaultBatch.getBatchTimeoutMs(),
            "Should use default batch timeout");
        assertTrue(defaultBatch.isEmpty(), "New batch should be empty");
        assertFalse(defaultBatch.isExecuted(), "New batch should not be executed");
        assertFalse(defaultBatch.isClosed(), "New batch should not be closed");
      } finally {
        defaultBatch.close();
      }
    }

    @Test
    @DisplayName("Parameterized constructor should accept custom values")
    void parameterizedConstructorShouldAcceptCustomValues() {
      final CallBatch customBatch = new CallBatch(16, 50);
      try {
        assertEquals(16, customBatch.getMaxBatchSize(), "Should use custom max batch size");
        assertEquals(50, customBatch.getBatchTimeoutMs(), "Should use custom batch timeout");
      } finally {
        customBatch.close();
      }
    }

    @Test
    @DisplayName("Constructor should throw for non-positive maxBatchSize")
    void constructorShouldThrowForNonPositiveMaxBatchSize() {
      assertThrows(IllegalArgumentException.class, () -> new CallBatch(0, 100),
          "Should throw for maxBatchSize = 0");
      assertThrows(IllegalArgumentException.class, () -> new CallBatch(-1, 100),
          "Should throw for negative maxBatchSize");
    }

    @Test
    @DisplayName("Constructor should throw for negative batchTimeoutMs")
    void constructorShouldThrowForNegativeBatchTimeoutMs() {
      assertThrows(IllegalArgumentException.class, () -> new CallBatch(10, -1),
          "Should throw for negative batchTimeoutMs");
    }

    @Test
    @DisplayName("Constructor should accept zero batchTimeoutMs")
    void constructorShouldAcceptZeroBatchTimeoutMs() {
      final CallBatch zeroBatch = new CallBatch(10, 0);
      try {
        assertEquals(0, zeroBatch.getBatchTimeoutMs(), "Should accept zero timeout");
      } finally {
        zeroBatch.close();
      }
    }

    @Test
    @DisplayName("Each batch should have unique ID")
    void eachBatchShouldHaveUniqueId() {
      final CallBatch batch1 = new CallBatch();
      final CallBatch batch2 = new CallBatch();
      try {
        assertTrue(batch1.getBatchId() != batch2.getBatchId(),
            "Each batch should have unique ID");
      } finally {
        batch1.close();
        batch2.close();
      }
    }
  }

  @Nested
  @DisplayName("Add Function Call Tests")
  class AddFunctionCallTests {

    @Test
    @DisplayName("addFunctionCall should return CompletableFuture")
    void addFunctionCallShouldReturnCompletableFuture() {
      final CompletableFuture<?> future = batch.addFunctionCall(1L, new Object[]{}, "test");
      assertNotNull(future, "Should return non-null future");
      assertFalse(future.isDone(), "Future should not be done yet");
    }

    @Test
    @DisplayName("addFunctionCall should increment size")
    void addFunctionCallShouldIncrementSize() {
      assertEquals(0, batch.size(), "Initial size should be 0");
      batch.addFunctionCall(1L, new Object[]{}, "test1");
      assertEquals(1, batch.size(), "Size should be 1 after first add");
      batch.addFunctionCall(2L, new Object[]{}, "test2");
      assertEquals(2, batch.size(), "Size should be 2 after second add");
    }

    @Test
    @DisplayName("addFunctionCall should throw for zero function handle")
    void addFunctionCallShouldThrowForZeroFunctionHandle() {
      assertThrows(IllegalArgumentException.class,
          () -> batch.addFunctionCall(0L, new Object[]{}, "test"),
          "Should throw for zero function handle");
    }

    @Test
    @DisplayName("addFunctionCall should accept null parameters")
    void addFunctionCallShouldAcceptNullParameters() {
      assertDoesNotThrow(() -> batch.addFunctionCall(1L, null, "test"),
          "Should accept null parameters");
      assertEquals(1, batch.size(), "Size should be 1");
    }

    @Test
    @DisplayName("addFunctionCall should accept null debug info")
    void addFunctionCallShouldAcceptNullDebugInfo() {
      assertDoesNotThrow(() -> batch.addFunctionCall(1L, new Object[]{}, null),
          "Should accept null debug info");
      assertEquals(1, batch.size(), "Size should be 1");
    }

    @Test
    @DisplayName("addFunctionCall should throw when batch is full")
    void addFunctionCallShouldThrowWhenBatchIsFull() {
      final CallBatch smallBatch = new CallBatch(2, 100);
      try {
        smallBatch.addFunctionCall(1L, new Object[]{}, "op1");
        smallBatch.addFunctionCall(2L, new Object[]{}, "op2");
        assertThrows(IllegalStateException.class,
            () -> smallBatch.addFunctionCall(3L, new Object[]{}, "op3"),
            "Should throw when batch is full");
      } finally {
        smallBatch.close();
      }
    }

    @Test
    @DisplayName("addFunctionCall should throw when batch is executed")
    void addFunctionCallShouldThrowWhenBatchIsExecuted() {
      batch.execute();
      assertThrows(IllegalStateException.class,
          () -> batch.addFunctionCall(1L, new Object[]{}, "test"),
          "Should throw when batch is executed");
    }

    @Test
    @DisplayName("addFunctionCall should throw when batch is closed")
    void addFunctionCallShouldThrowWhenBatchIsClosed() {
      batch.close();
      assertThrows(IllegalStateException.class,
          () -> batch.addFunctionCall(1L, new Object[]{}, "test"),
          "Should throw when batch is closed");
    }
  }

  @Nested
  @DisplayName("Add Memory Read Tests")
  class AddMemoryReadTests {

    @Test
    @DisplayName("addMemoryRead should return CompletableFuture")
    void addMemoryReadShouldReturnCompletableFuture() {
      final CompletableFuture<?> future = batch.addMemoryRead(1L, 0, 100);
      assertNotNull(future, "Should return non-null future");
    }

    @Test
    @DisplayName("addMemoryRead should increment size")
    void addMemoryReadShouldIncrementSize() {
      batch.addMemoryRead(1L, 0, 100);
      assertEquals(1, batch.size(), "Size should be 1 after add");
    }

    @Test
    @DisplayName("addMemoryRead should throw for zero memory handle")
    void addMemoryReadShouldThrowForZeroMemoryHandle() {
      assertThrows(IllegalArgumentException.class,
          () -> batch.addMemoryRead(0L, 0, 100),
          "Should throw for zero memory handle");
    }

    @Test
    @DisplayName("addMemoryRead should throw for negative offset")
    void addMemoryReadShouldThrowForNegativeOffset() {
      assertThrows(IllegalArgumentException.class,
          () -> batch.addMemoryRead(1L, -1, 100),
          "Should throw for negative offset");
    }

    @Test
    @DisplayName("addMemoryRead should throw for non-positive length")
    void addMemoryReadShouldThrowForNonPositiveLength() {
      assertThrows(IllegalArgumentException.class,
          () -> batch.addMemoryRead(1L, 0, 0),
          "Should throw for length = 0");
      assertThrows(IllegalArgumentException.class,
          () -> batch.addMemoryRead(1L, 0, -1),
          "Should throw for negative length");
    }

    @Test
    @DisplayName("addMemoryRead should throw when batch is executed")
    void addMemoryReadShouldThrowWhenBatchIsExecuted() {
      batch.execute();
      assertThrows(IllegalStateException.class,
          () -> batch.addMemoryRead(1L, 0, 100),
          "Should throw when batch is executed");
    }

    @Test
    @DisplayName("addMemoryRead should throw when batch is closed")
    void addMemoryReadShouldThrowWhenBatchIsClosed() {
      batch.close();
      assertThrows(IllegalStateException.class,
          () -> batch.addMemoryRead(1L, 0, 100),
          "Should throw when batch is closed");
    }
  }

  @Nested
  @DisplayName("Execution Tests")
  class ExecutionTests {

    @Test
    @DisplayName("execute should mark batch as executed")
    void executeShouldMarkBatchAsExecuted() {
      assertFalse(batch.isExecuted(), "Batch should not be executed initially");
      batch.execute();
      assertTrue(batch.isExecuted(), "Batch should be executed after execute()");
    }

    @Test
    @DisplayName("execute should complete all pending futures")
    void executeShouldCompleteAllPendingFutures() throws Exception {
      final CompletableFuture<?> future1 = batch.addFunctionCall(1L, new Object[]{}, "op1");
      final CompletableFuture<?> future2 = batch.addFunctionCall(2L, new Object[]{}, "op2");

      assertFalse(future1.isDone(), "Future1 should not be done before execute");
      assertFalse(future2.isDone(), "Future2 should not be done before execute");

      batch.execute();

      assertTrue(future1.isDone(), "Future1 should be done after execute");
      assertTrue(future2.isDone(), "Future2 should be done after execute");
    }

    @Test
    @DisplayName("execute should succeed on empty batch")
    void executeShouldSucceedOnEmptyBatch() {
      assertTrue(batch.isEmpty(), "Batch should be empty");
      assertDoesNotThrow(() -> batch.execute(),
          "Execute should succeed on empty batch");
      assertTrue(batch.isExecuted(), "Batch should be marked as executed");
    }

    @Test
    @DisplayName("execute should throw when already executed")
    void executeShouldThrowWhenAlreadyExecuted() {
      batch.execute();
      assertThrows(IllegalStateException.class, () -> batch.execute(),
          "Should throw when already executed");
    }

    @Test
    @DisplayName("execute should throw when closed")
    void executeShouldThrowWhenClosed() {
      batch.close();
      assertThrows(IllegalStateException.class, () -> batch.execute(),
          "Should throw when closed");
    }
  }

  @Nested
  @DisplayName("Close Tests")
  class CloseTests {

    @Test
    @DisplayName("close should mark batch as closed")
    void closeShouldMarkBatchAsClosed() {
      assertFalse(batch.isClosed(), "Batch should not be closed initially");
      batch.close();
      assertTrue(batch.isClosed(), "Batch should be closed after close()");
    }

    @Test
    @DisplayName("close should cancel pending operations")
    void closeShouldCancelPendingOperations() {
      final CompletableFuture<?> future = batch.addFunctionCall(1L, new Object[]{}, "test");
      batch.close();

      assertTrue(future.isCompletedExceptionally(),
          "Future should be completed exceptionally after close");
      assertThrows(ExecutionException.class, () -> future.get(),
          "Getting future should throw");
    }

    @Test
    @DisplayName("close should be idempotent")
    void closeShouldBeIdempotent() {
      batch.close();
      assertDoesNotThrow(() -> batch.close(),
          "Calling close twice should not throw");
      assertTrue(batch.isClosed(), "Batch should still be closed");
    }

    @Test
    @DisplayName("close should not cancel already executed operations")
    void closeShouldNotCancelAlreadyExecutedOperations() throws Exception {
      final CompletableFuture<?> future = batch.addFunctionCall(1L, new Object[]{}, "test");
      batch.execute();
      batch.close();

      assertTrue(future.isDone(), "Future should be done");
      assertFalse(future.isCompletedExceptionally(),
          "Executed future should not be exceptionally completed");
    }
  }

  @Nested
  @DisplayName("State Query Tests")
  class StateQueryTests {

    @Test
    @DisplayName("isEmpty should return true for new batch")
    void isEmptyShouldReturnTrueForNewBatch() {
      assertTrue(batch.isEmpty(), "New batch should be empty");
    }

    @Test
    @DisplayName("isEmpty should return false after adding operation")
    void isEmptyShouldReturnFalseAfterAddingOperation() {
      batch.addFunctionCall(1L, new Object[]{}, "test");
      assertFalse(batch.isEmpty(), "Batch should not be empty after adding operation");
    }

    @Test
    @DisplayName("size should return correct count")
    void sizeShouldReturnCorrectCount() {
      assertEquals(0, batch.size(), "Initial size should be 0");
      batch.addFunctionCall(1L, new Object[]{}, "op1");
      assertEquals(1, batch.size(), "Size should be 1");
      batch.addFunctionCall(2L, new Object[]{}, "op2");
      assertEquals(2, batch.size(), "Size should be 2");
      batch.addMemoryRead(3L, 0, 100);
      assertEquals(3, batch.size(), "Size should be 3");
    }

    @Test
    @DisplayName("getBatchId should return positive value")
    void getBatchIdShouldReturnPositiveValue() {
      assertTrue(batch.getBatchId() > 0, "Batch ID should be positive");
    }
  }

  @Nested
  @DisplayName("Auto Execute Tests")
  class AutoExecuteTests {

    @Test
    @DisplayName("shouldAutoExecute should return false for empty batch")
    void shouldAutoExecuteShouldReturnFalseForEmptyBatch() {
      assertFalse(batch.shouldAutoExecute(),
          "Empty batch should not auto execute");
    }

    @Test
    @DisplayName("shouldAutoExecute should return true when batch is full")
    void shouldAutoExecuteShouldReturnTrueWhenBatchIsFull() {
      final CallBatch smallBatch = new CallBatch(2, 10000);
      try {
        smallBatch.addFunctionCall(1L, new Object[]{}, "op1");
        assertFalse(smallBatch.shouldAutoExecute(), "Should not auto execute when not full");
        smallBatch.addFunctionCall(2L, new Object[]{}, "op2");
        assertTrue(smallBatch.shouldAutoExecute(), "Should auto execute when full");
      } finally {
        smallBatch.close();
      }
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain batch ID")
    void toStringShouldContainBatchId() {
      final String str = batch.toString();
      assertTrue(str.contains("id="), "toString should contain batch ID");
    }

    @Test
    @DisplayName("toString should contain size")
    void toStringShouldContainSize() {
      final String str = batch.toString();
      assertTrue(str.contains("size="), "toString should contain size");
    }

    @Test
    @DisplayName("toString should contain executed status")
    void toStringShouldContainExecutedStatus() {
      final String str = batch.toString();
      assertTrue(str.contains("executed="), "toString should contain executed status");
    }

    @Test
    @DisplayName("toString should contain closed status")
    void toStringShouldContainClosedStatus() {
      final String str = batch.toString();
      assertTrue(str.contains("closed="), "toString should contain closed status");
    }

    @Test
    @DisplayName("toString should reflect state changes")
    void toStringShouldReflectStateChanges() {
      String str = batch.toString();
      assertTrue(str.contains("executed=false"), "Should show executed=false initially");
      assertTrue(str.contains("closed=false"), "Should show closed=false initially");

      batch.execute();
      str = batch.toString();
      assertTrue(str.contains("executed=true"), "Should show executed=true after execute");

      batch.close();
      str = batch.toString();
      assertTrue(str.contains("closed=true"), "Should show closed=true after close");
    }
  }

  @Nested
  @DisplayName("Try-With-Resources Tests")
  class TryWithResourcesTests {

    @Test
    @DisplayName("Should work with try-with-resources")
    void shouldWorkWithTryWithResources() {
      CompletableFuture<?> future;
      try (CallBatch trwBatch = new CallBatch()) {
        future = trwBatch.addFunctionCall(1L, new Object[]{}, "test");
        assertFalse(trwBatch.isClosed(), "Should not be closed in try block");
      }
      // After try block, batch should be closed and future should be completed exceptionally
      assertTrue(future.isCompletedExceptionally(),
          "Future should be completed exceptionally after close");
    }

    @Test
    @DisplayName("Executed batch should work with try-with-resources")
    void executedBatchShouldWorkWithTryWithResources() throws Exception {
      CompletableFuture<?> future;
      try (CallBatch trwBatch = new CallBatch()) {
        future = trwBatch.addFunctionCall(1L, new Object[]{}, "test");
        trwBatch.execute();
        assertTrue(future.isDone(), "Future should be done after execute");
      }
      // After try block, batch should be closed but future should not be exceptionally completed
      assertFalse(future.isCompletedExceptionally(),
          "Executed future should not be exceptionally completed");
    }
  }
}
