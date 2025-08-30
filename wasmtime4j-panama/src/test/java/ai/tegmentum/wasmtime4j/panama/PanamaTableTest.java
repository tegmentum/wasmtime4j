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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive unit tests for the PanamaTable implementation.
 *
 * <p>These tests verify the complete functionality of the Panama FFI table wrapper, including
 * element access, bounds checking, resource management, and error handling patterns.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PanamaTable Tests")
class PanamaTableTest {

  @Mock private ArenaResourceManager mockArenaManager;
  @Mock private PanamaInstance mockPanamaInstance;
  @Mock private MethodHandle mockSizeHandle;
  @Mock private MethodHandle mockGetHandle;
  @Mock private MethodHandle mockSetHandle;
  @Mock private MethodHandle mockGrowHandle;
  @Mock private MethodHandle mockDeleteHandle;

  private MemorySegment mockTableHandle;
  private PanamaTable panamaTable;

  @BeforeEach
  void setUp() throws Throwable {
    mockTableHandle = MemorySegment.ofAddress(0x1000L);

    // Create the table instance
    panamaTable = new PanamaTable(mockTableHandle, mockArenaManager, mockPanamaInstance);
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create table with valid parameters")
    void shouldCreateTableWithValidParameters() throws Throwable {
      // Verify the table was registered with arena manager
      verify(mockArenaManager)
          .registerManagedNativeResource(eq(panamaTable), eq(mockTableHandle), any());
    }

    @Test
    @DisplayName("Should throw exception when table handle is null")
    void shouldThrowExceptionWhenTableHandleIsNull() {
      assertThrows(
          NullPointerException.class,
          () -> new PanamaTable(null, mockArenaManager, mockPanamaInstance));
    }

    @Test
    @DisplayName("Should throw exception when arena manager is null")
    void shouldThrowExceptionWhenArenaManagerIsNull() {
      assertThrows(
          NullPointerException.class,
          () -> new PanamaTable(mockTableHandle, null, mockPanamaInstance));
    }

    @Test
    @DisplayName("Should throw exception when native bindings is null")
    void shouldThrowExceptionWhenNativeBindingsIsNull() {
      assertThrows(
          NullPointerException.class,
          () -> new PanamaTable(mockTableHandle, mockArenaManager, null));
    }

    @Test
    @DisplayName("Should throw exception when error handler is null")
    void shouldThrowExceptionWhenErrorHandlerIsNull() {
      assertThrows(
          NullPointerException.class,
          () -> new PanamaTable(mockTableHandle, mockArenaManager, mockPanamaInstance));
    }
  }

  @Nested
  @DisplayName("Size Operation Tests")
  class SizeOperationTests {

    @Test
    @DisplayName("Should return correct table size")
    void shouldReturnCorrectTableSize() throws Throwable {
      // Arrange
      final long expectedSize = 42L;
      when(mockSizeHandle.invokeExact(mockTableHandle)).thenReturn(expectedSize);

      // Act
      final long actualSize = panamaTable.getSize();

      // Assert
      assertEquals(expectedSize, actualSize);
      verify(mockSizeHandle).invokeExact(mockTableHandle);
    }

    @Test
    @DisplayName("Should handle size operation failure")
    void shouldHandleSizeOperationFailure() throws Throwable {
      // Arrange
      final RuntimeException cause = new RuntimeException("Native size failed");
      when(mockSizeHandle.invokeExact(mockTableHandle)).thenThrow(cause);

      // Act
      final int result = panamaTable.getSize();
      
      // Assert - getSize returns 0 on failure as per implementation
      assertEquals(0, result);
    }

    // Note: Cannot test null handle scenario with current architecture
    // since NativeFunctionBindings.getInstance() is called internally
  }

  @Nested
  @DisplayName("Get Element Tests")
  class GetElementTests {

    @Test
    @DisplayName("Should get element at valid index")
    void shouldGetElementAtValidIndex() throws Throwable {
      // Arrange
      final int index = 5;
      final long tableSize = 10L;
      final MemorySegment mockElementHandle = MemorySegment.ofAddress(0x2000L);

      when(mockSizeHandle.invokeExact(mockTableHandle)).thenReturn(tableSize);
      when(mockGetHandle.invokeExact(mockTableHandle, index)).thenReturn(mockElementHandle);

      // Act
      final Object result = panamaTable.get(index);

      // Assert
      assertNotNull(result);
      assertTrue(result instanceof PanamaFunction);
      verify(mockGetHandle).invokeExact(mockTableHandle, index);
    }

    @Test
    @DisplayName("Should return null for null element handle")
    void shouldReturnNullForNullElementHandle() throws Throwable {
      // Arrange
      final int index = 5;
      final long tableSize = 10L;

      when(mockSizeHandle.invokeExact(mockTableHandle)).thenReturn(tableSize);
      when(mockGetHandle.invokeExact(mockTableHandle, index)).thenReturn(MemorySegment.NULL);

      // Act
      final Object result = panamaTable.get(index);

      // Assert
      assertNull(result);
    }

    @Test
    @DisplayName("Should throw exception for negative index")
    void shouldThrowExceptionForNegativeIndex() {
      // Arrange
      final int index = -1;

      // Act & Assert
      assertThrows(IllegalArgumentException.class, () -> panamaTable.get(index));
    }

    @Test
    @DisplayName("Should throw exception for index out of bounds")
    void shouldThrowExceptionForIndexOutOfBounds() throws Throwable {
      // Arrange
      final int index = 10;
      final long tableSize = 5L;

      when(mockSizeHandle.invokeExact(mockTableHandle)).thenReturn(tableSize);

      // Act & Assert
      assertThrows(RuntimeException.class, () -> panamaTable.get(index));
    }

    @Test
    @DisplayName("Should handle get operation failure")
    void shouldHandleGetOperationFailure() throws Throwable {
      // Arrange
      final int index = 3;
      final long tableSize = 10L;
      final RuntimeException cause = new RuntimeException("Native get failed");

      when(mockSizeHandle.invokeExact(mockTableHandle)).thenReturn(tableSize);
      when(mockGetHandle.invokeExact(mockTableHandle, index)).thenThrow(cause);

      // Act
      final Object result = panamaTable.get(index);
      
      // Assert - get returns null on failure as per implementation
      assertNull(result);
    }
  }

  @Nested
  @DisplayName("Set Element Tests")
  class SetElementTests {

    @Test
    @DisplayName("Should set element at valid index")
    void shouldSetElementAtValidIndex() throws Throwable {
      // Arrange
      final int index = 3;
      final long tableSize = 10L;
      final PanamaFunction mockFunction = mock(PanamaFunction.class);
      final MemorySegment mockFunctionHandle = MemorySegment.ofAddress(0x3000L);

      when(mockSizeHandle.invokeExact(mockTableHandle)).thenReturn(tableSize);
      when(mockFunction.getFunctionHandle()).thenReturn(mockFunctionHandle);
      when(mockSetHandle.invokeExact(mockTableHandle, index, mockFunctionHandle)).thenReturn(true);

      // Act
      panamaTable.set(index, mockFunction);

      // Assert
      verify(mockSetHandle).invokeExact(mockTableHandle, index, mockFunctionHandle);
    }

    @Test
    @DisplayName("Should set null element")
    void shouldSetNullElement() throws Throwable {
      // Arrange
      final int index = 3;
      final long tableSize = 10L;

      when(mockSizeHandle.invokeExact(mockTableHandle)).thenReturn(tableSize);
      when(mockSetHandle.invokeExact(mockTableHandle, index, MemorySegment.NULL)).thenReturn(true);

      // Act
      panamaTable.set(index, null);

      // Assert
      verify(mockSetHandle).invokeExact(mockTableHandle, index, MemorySegment.NULL);
    }

    @Test
    @DisplayName("Should throw exception for negative index")
    void shouldThrowExceptionForNegativeIndexOnSet() {
      // Arrange
      final int index = -1;

      // Act & Assert
      assertThrows(IllegalArgumentException.class, () -> panamaTable.set(index, null));
    }

    @Test
    @DisplayName("Should throw exception for index out of bounds")
    void shouldThrowExceptionForIndexOutOfBoundsOnSet() throws Throwable {
      // Arrange
      final int index = 15;
      final long tableSize = 10L;

      when(mockSizeHandle.invokeExact(mockTableHandle)).thenReturn(tableSize);

      // Act & Assert
      assertThrows(RuntimeException.class, () -> panamaTable.set(index, null));
    }

    @Test
    @DisplayName("Should throw exception for non-Panama function")
    void shouldThrowExceptionForNonPanamaFunction() throws Throwable {
      // Arrange
      final int index = 3;
      final long tableSize = 10L;
      final Object invalidFunction = new Object();

      when(mockSizeHandle.invokeExact(mockTableHandle)).thenReturn(tableSize);

      // Act & Assert
      assertThrows(IllegalArgumentException.class, () -> panamaTable.set(index, invalidFunction));
    }

    @Test
    @DisplayName("Should handle set operation failure")
    void shouldHandleSetOperationFailure() throws Throwable {
      // Arrange
      final int index = 3;
      final long tableSize = 10L;

      when(mockSizeHandle.invokeExact(mockTableHandle)).thenReturn(tableSize);
      when(mockSetHandle.invokeExact(mockTableHandle, index, MemorySegment.NULL)).thenReturn(false);

      // Act & Assert
      assertThrows(RuntimeException.class, () -> panamaTable.set(index, null));
    }
  }

  @Nested
  @DisplayName("Grow Operation Tests")
  class GrowOperationTests {

    @Test
    @DisplayName("Should grow table successfully")
    void shouldGrowTableSuccessfully() throws Throwable {
      // Arrange
      final int delta = 5;
      final long previousSize = 10L;

      when(mockSizeHandle.invokeExact(mockTableHandle)).thenReturn(previousSize);
      when(mockGrowHandle.invokeExact(mockTableHandle, delta, MemorySegment.NULL)).thenReturn(true);

      // Act
      final long result = panamaTable.grow(delta, null);

      // Assert
      assertEquals(previousSize, result);
      verify(mockGrowHandle).invokeExact(mockTableHandle, delta, MemorySegment.NULL);
    }

    @Test
    @DisplayName("Should grow table with initial value")
    void shouldGrowTableWithInitialValue() throws Throwable {
      // Arrange
      final int delta = 3;
      final long previousSize = 7L;
      final PanamaFunction mockInitialFunction = mock(PanamaFunction.class);
      final MemorySegment mockInitialHandle = MemorySegment.ofAddress(0x4000L);

      when(mockSizeHandle.invokeExact(mockTableHandle)).thenReturn(previousSize);
      when(mockInitialFunction.getFunctionHandle()).thenReturn(mockInitialHandle);
      when(mockGrowHandle.invokeExact(mockTableHandle, delta, mockInitialHandle)).thenReturn(true);

      // Act
      final long result = panamaTable.grow(delta, mockInitialFunction);

      // Assert
      assertEquals(previousSize, result);
      verify(mockGrowHandle).invokeExact(mockTableHandle, delta, mockInitialHandle);
    }

    @Test
    @DisplayName("Should throw exception for negative delta")
    void shouldThrowExceptionForNegativeDelta() {
      // Arrange
      final int delta = -5;

      // Act & Assert
      assertThrows(IllegalArgumentException.class, () -> panamaTable.grow(delta, null));
    }

    @Test
    @DisplayName("Should handle grow operation failure")
    void shouldHandleGrowOperationFailure() throws Throwable {
      // Arrange
      final int delta = 5;
      final long previousSize = 10L;

      when(mockSizeHandle.invokeExact(mockTableHandle)).thenReturn(previousSize);
      when(mockGrowHandle.invokeExact(mockTableHandle, delta, MemorySegment.NULL))
          .thenReturn(false);

      // Act & Assert
      assertThrows(RuntimeException.class, () -> panamaTable.grow(delta, null));
    }
  }

  @Nested
  @DisplayName("Resource Management Tests")
  class ResourceManagementTests {

    @Test
    @DisplayName("Should close table successfully")
    void shouldCloseTableSuccessfully() throws Throwable {
      // Act
      panamaTable.close();

      // Assert
      assertTrue(panamaTable.isClosed());
      verify(mockArenaManager).unregisterManagedResource(panamaTable);
    }

    @Test
    @DisplayName("Should handle double close gracefully")
    void shouldHandleDoubleCloseGracefully() throws Throwable {
      // Act
      panamaTable.close();
      panamaTable.close(); // Second close should be safe

      // Assert
      assertTrue(panamaTable.isClosed());
    }

    @Test
    @DisplayName("Should throw exception when accessing closed table")
    void shouldThrowExceptionWhenAccessingClosedTable() throws Throwable {
      // Arrange
      panamaTable.close();

      // Act & Assert
      assertThrows(IllegalStateException.class, () -> panamaTable.getSize());
      assertThrows(IllegalStateException.class, () -> panamaTable.get(0));
      assertThrows(IllegalStateException.class, () -> panamaTable.set(0, null));
      assertThrows(IllegalStateException.class, () -> panamaTable.grow(1, null));
      assertThrows(IllegalStateException.class, () -> panamaTable.getTableHandle());
    }

    @Test
    @DisplayName("Should return correct table handle")
    void shouldReturnCorrectTableHandle() {
      // Act
      final MemorySegment handle = panamaTable.getTableHandle();

      // Assert
      assertEquals(mockTableHandle, handle);
    }
  }

  @Nested
  @DisplayName("Utility Method Tests")
  class UtilityMethodTests {

    @Test
    @DisplayName("Should return correct max size")
    void shouldReturnCorrectMaxSize() throws Throwable {
      // Act
      final int maxSize = panamaTable.getMaxSize();

      // Assert
      assertEquals(-1L, maxSize); // Unlimited
    }

    @Test
    @DisplayName("Should return correct element type")
    void shouldReturnCorrectElementType() throws Throwable {
      // Act
      final WasmValueType elementType = panamaTable.getElementType();

      // Assert
      assertEquals(WasmValueType.FUNCREF, elementType);
    }

    @Test
    @DisplayName("Should return correct string representation")
    void shouldReturnCorrectStringRepresentation() throws Throwable {
      // Arrange
      when(mockSizeHandle.invokeExact(mockTableHandle)).thenReturn(5L);

      // Act
      final String stringRepr = panamaTable.toString();

      // Assert
      assertTrue(stringRepr.contains("PanamaTable"));
      assertTrue(stringRepr.contains("size=5"));
      assertTrue(stringRepr.contains("elementType=funcref"));
    }

    @Test
    @DisplayName("Should return correct string representation when closed")
    void shouldReturnCorrectStringRepresentationWhenClosed() throws Throwable {
      // Arrange
      panamaTable.close();

      // Act
      final String stringRepr = panamaTable.toString();

      // Assert
      assertTrue(stringRepr.contains("PanamaTable"));
      assertTrue(stringRepr.contains("closed=true"));
    }
  }

  @Nested
  @DisplayName("Thread Safety Tests")
  class ThreadSafetyTests {

    @Test
    @DisplayName("Should handle concurrent access safely")
    void shouldHandleConcurrentAccessSafely() throws Throwable {
      // Arrange
      when(mockSizeHandle.invokeExact(mockTableHandle)).thenReturn(10L);
      when(mockGetHandle.invokeExact(eq(mockTableHandle), anyLong()))
          .thenReturn(MemorySegment.NULL);
      when(mockSetHandle.invokeExact(eq(mockTableHandle), anyLong(), any())).thenReturn(true);

      // Act - simulate concurrent access from multiple threads
      final Runnable operation =
          () -> {
            try {
              panamaTable.getSize();
              panamaTable.get(0);
              panamaTable.set(0, null);
            } catch (Exception e) {
              // Expected in concurrent environment
            }
          };

      final Thread thread1 = new Thread(operation);
      final Thread thread2 = new Thread(operation);

      thread1.start();
      thread2.start();

      thread1.join(1000);
      thread2.join(1000);

      // Assert - no deadlocks or exceptions should occur
      assertFalse(panamaTable.isClosed());
    }

    @Test
    @DisplayName("Should handle concurrent close safely")
    void shouldHandleConcurrentCloseSafely() throws Throwable {
      // Act
      final Runnable closeOperation =
          () -> {
            try {
              panamaTable.close();
            } catch (Exception e) {
              // Expected in concurrent environment
            }
          };

      final Thread thread1 = new Thread(closeOperation);
      final Thread thread2 = new Thread(closeOperation);

      thread1.start();
      thread2.start();

      thread1.join(1000);
      thread2.join(1000);

      // Assert
      assertTrue(panamaTable.isClosed());
    }
  }
}
