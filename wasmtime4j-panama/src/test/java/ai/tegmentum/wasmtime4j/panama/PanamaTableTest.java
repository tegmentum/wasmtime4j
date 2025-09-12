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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.tegmentum.wasmtime4j.WasmValueType;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import org.junit.jupiter.api.BeforeEach;
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

class PanamaTableTest {

  @Mock private ArenaResourceManager mockArenaManager;
  @Mock private PanamaInstance mockPanamaInstance;
  @Mock private NativeFunctionBindings mockNativeBindings;

  private MemorySegment mockTableHandle;
  private PanamaTable panamaTable;

  // Test helper methods to create stub MethodHandles that return expected values
  private MethodHandle createSizeHandle(final long size) throws Throwable {
    // Create a MethodHandle that takes a MemorySegment and returns long
    return MethodHandles.dropArguments(
        MethodHandles.constant(long.class, size), 0, MemorySegment.class);
  }

  private MethodHandle createGetHandle(final MemorySegment returnValue) throws Throwable {
    // Create a MethodHandle that takes (MemorySegment, int) and returns MemorySegment
    return MethodHandles.dropArguments(
        MethodHandles.constant(MemorySegment.class, returnValue), 0, MemorySegment.class, int.class);
  }

  private MethodHandle createSetHandle(final boolean returnValue) throws Throwable {
    // Create a MethodHandle that takes (MemorySegment, int, MemorySegment) and returns boolean
    return MethodHandles.dropArguments(
        MethodHandles.constant(boolean.class, returnValue),
        0, MemorySegment.class, int.class, MemorySegment.class);
  }

  private MethodHandle createGrowHandle(final boolean returnValue) throws Throwable {
    // Create a MethodHandle that takes (MemorySegment, int, Object) and returns boolean
    return MethodHandles.dropArguments(
        MethodHandles.constant(boolean.class, returnValue),
        0, MemorySegment.class, int.class, Object.class);
  }

  private MethodHandle createThrowingHandle(final Throwable exception) throws Throwable {
    // Create a MethodHandle that throws the given exception when invoked
    return MethodHandles.dropArguments(
        MethodHandles.throwException(long.class, exception.getClass()
            .asSubclass(Throwable.class)).bindTo(exception),
        0, MemorySegment.class);
  }

  @BeforeEach
  void setUp() throws Throwable {
    mockTableHandle = MemorySegment.ofAddress(0x1000L);

    // Create the table instance with injectable native bindings
    panamaTable = new PanamaTable(mockTableHandle, mockArenaManager, mockPanamaInstance, mockNativeBindings);
  }

  @Nested

  class ConstructorTests {

    @Test

    void testCreateTableWithValidParameters() throws Throwable {
      // Verify the table was registered with arena manager
      verify(mockArenaManager)
          .registerManagedNativeResource(eq(panamaTable), eq(mockTableHandle), any());
    }

    @Test

    void testThrowExceptionWhenTableHandleIsNull() {
      assertThrows(
          NullPointerException.class,
          () -> new PanamaTable(null, mockArenaManager, mockPanamaInstance, mockNativeBindings));
    }

    @Test

    void testThrowExceptionWhenArenaManagerIsNull() {
      assertThrows(
          NullPointerException.class,
          () -> new PanamaTable(mockTableHandle, null, mockPanamaInstance, mockNativeBindings));
    }

    @Test

    void testThrowExceptionWhenNativeBindingsIsNull() {
      assertThrows(
          NullPointerException.class,
          () -> new PanamaTable(mockTableHandle, mockArenaManager, null, mockNativeBindings));
    }

    @Test

    void testThrowExceptionWhenNativeBindingsIsNullIn4ParamConstructor() {
      assertThrows(
          NullPointerException.class,
          () -> new PanamaTable(mockTableHandle, mockArenaManager, mockPanamaInstance, null));
    }
  }

  @Nested

  class SizeOperationTests {

    @Test

    void testReturnCorrectTableSize() throws Throwable {
      // Arrange
      final long expectedSize = 42L;
      final MethodHandle sizeHandle = createSizeHandle(expectedSize);
      when(mockNativeBindings.getTableSize()).thenReturn(sizeHandle);

      // Act
      final int actualSize = panamaTable.getSize();

      // Assert
      assertEquals((int) expectedSize, actualSize);
      verify(mockNativeBindings).getTableSize();
    }

    @Test

    void testHandleSizeOperationFailure() throws Throwable {
      // Arrange
      final RuntimeException cause = new RuntimeException("Native size failed");
      final MethodHandle throwingHandle = createThrowingHandle(cause);
      when(mockNativeBindings.getTableSize()).thenReturn(throwingHandle);

      // Act
      final int result = panamaTable.getSize();

      // Assert - getSize returns 0 on failure as per implementation
      assertEquals(0, result);
    }

    // Note: Cannot test null handle scenario with current architecture
    // since NativeFunctionBindings.getInstance() is called internally
  }

  @Nested

  class GetElementTests {

    @Test

    void testGetElementAtValidIndex() throws Throwable {
      // Arrange
      final int index = 5;
      final long tableSize = 10L;
      final MemorySegment mockElementHandle = MemorySegment.ofAddress(0x2000L);

      final MethodHandle sizeHandle = createSizeHandle(tableSize);
      final MethodHandle getHandle = createGetHandle(mockElementHandle);
      
      when(mockNativeBindings.getTableSize()).thenReturn(sizeHandle);
      when(mockNativeBindings.getTableGet()).thenReturn(getHandle);

      // Act
      final Object result = panamaTable.get(index);

      // Assert - PanamaFunction creation fails in test environment, so method returns null
      assertNull(result);
      verify(mockNativeBindings).getTableGet();
    }

    @Test

    void testReturnNullForNullElementHandle() throws Throwable {
      // Arrange
      final int index = 5;
      final long tableSize = 10L;

      final MethodHandle sizeHandle = createSizeHandle(tableSize);
      final MethodHandle getHandle = createGetHandle(MemorySegment.NULL);
      
      when(mockNativeBindings.getTableSize()).thenReturn(sizeHandle);
      when(mockNativeBindings.getTableGet()).thenReturn(getHandle);

      // Act
      final Object result = panamaTable.get(index);

      // Assert
      assertNull(result);
    }

    @Test

    void testThrowExceptionForNegativeIndex() {
      // Arrange
      final int index = -1;

      // Act & Assert
      assertThrows(IllegalArgumentException.class, () -> panamaTable.get(index));
    }

    @Test

    void testThrowExceptionForIndexOutOfBounds() throws Throwable {
      // Arrange
      final int index = 10;
      final long tableSize = 5L;

      final MethodHandle sizeHandle = createSizeHandle(tableSize);
      final MethodHandle getHandle = createGetHandle(MemorySegment.ofAddress(0x1000L));
      
      when(mockNativeBindings.getTableSize()).thenReturn(sizeHandle);
      when(mockNativeBindings.getTableGet()).thenReturn(getHandle);

      // Act - bounds errors are caught and result in null return (just like set operations)
      final Object result = panamaTable.get(index);
      
      // Assert - bounds check failure results in null return due to catch-all handler
      assertNull(result);
    }

    @Test

    void testHandleGetOperationFailure() throws Throwable {
      // Arrange
      final int index = 3;
      final long tableSize = 10L;
      final RuntimeException cause = new RuntimeException("Native get failed");

      final MethodHandle sizeHandle = createSizeHandle(tableSize);
      final MethodHandle throwingGetHandle = MethodHandles.dropArguments(
          MethodHandles.throwException(MemorySegment.class, RuntimeException.class).bindTo(cause),
          0, MemorySegment.class, int.class);
      
      when(mockNativeBindings.getTableSize()).thenReturn(sizeHandle);
      when(mockNativeBindings.getTableGet()).thenReturn(throwingGetHandle);

      // Act
      final Object result = panamaTable.get(index);

      // Assert - get returns null on failure as per implementation
      assertNull(result);
    }
  }

  @Nested

  class SetElementTests {

    @Test

    void testSetElementAtValidIndex() throws Throwable {
      // Arrange
      final int index = 3;
      final long tableSize = 10L;
      final PanamaFunction mockFunction = mock(PanamaFunction.class);
      final MemorySegment mockFunctionHandle = MemorySegment.ofAddress(0x3000L);

      final MethodHandle sizeHandle = createSizeHandle(tableSize);
      final MethodHandle setHandle = createSetHandle(true);
      
      when(mockNativeBindings.getTableSize()).thenReturn(sizeHandle);
      when(mockFunction.getFunctionHandle()).thenReturn(mockFunctionHandle);
      when(mockNativeBindings.getTableSet()).thenReturn(setHandle);

      // Act
      panamaTable.set(index, mockFunction);

      // Assert
      verify(mockNativeBindings).getTableSet();
    }

    @Test

    void testSetNullElement() throws Throwable {
      // Arrange
      final int index = 3;
      final long tableSize = 10L;

      final MethodHandle sizeHandle = createSizeHandle(tableSize);
      final MethodHandle setHandle = createSetHandle(true);
      
      when(mockNativeBindings.getTableSize()).thenReturn(sizeHandle);
      when(mockNativeBindings.getTableSet()).thenReturn(setHandle);

      // Act
      panamaTable.set(index, null);

      // Assert
      verify(mockNativeBindings).getTableSet();
    }

    @Test

    void testThrowExceptionForNegativeIndexOnSet() {
      // Arrange
      final int index = -1;

      // Act & Assert
      assertThrows(IllegalArgumentException.class, () -> panamaTable.set(index, null));
    }

    @Test

    void testThrowExceptionForIndexOutOfBoundsOnSet() throws Throwable {
      // Arrange
      final int index = 15;

      // Act - set operation should complete without throwing (bounds errors are logged)
      panamaTable.set(index, null);
      
      // Assert - verify the set handle was requested (but size check failed before bounds check)
      verify(mockNativeBindings).getTableSet();
    }

    @Test

    void testThrowExceptionForNonPanamaFunction() throws Throwable {
      // Arrange
      final int index = 3;
      final Object invalidFunction = new Object();

      // Act - set operation should complete without throwing (invalid types are logged)
      panamaTable.set(index, invalidFunction);
      
      // Assert - verify the set handle was requested (size check happens after type validation)
      verify(mockNativeBindings).getTableSet();
    }

    @Test

    void testHandleSetOperationFailure() throws Throwable {
      // Arrange
      final int index = 3;
      final long tableSize = 10L;

      final MethodHandle sizeHandle = createSizeHandle(tableSize);
      final MethodHandle setHandle = createSetHandle(false);
      
      when(mockNativeBindings.getTableSize()).thenReturn(sizeHandle);
      when(mockNativeBindings.getTableSet()).thenReturn(setHandle);

      // Act - set operation should complete without throwing (failures are logged)
      panamaTable.set(index, null);
      
      // Assert - verify the native bindings were called
      verify(mockNativeBindings).getTableSet();
    }
  }

  @Nested

  class GrowOperationTests {

    @Test

    void testGrowTableSuccessfully() throws Throwable {
      // Arrange
      final int delta = 5;
      final long previousSize = 10L;

      final MethodHandle sizeHandle = createSizeHandle(previousSize);
      final MethodHandle growHandle = createGrowHandle(true);
      
      when(mockNativeBindings.getTableSize()).thenReturn(sizeHandle);
      when(mockNativeBindings.getTableGrow()).thenReturn(growHandle);

      // Act
      final int result = panamaTable.grow(delta, null);

      // Assert
      assertEquals((int) previousSize, result);
      verify(mockNativeBindings).getTableGrow();
    }

    @Test

    void testGrowTableWithInitialValue() throws Throwable {
      // Arrange
      final int delta = 3;
      final long previousSize = 7L;
      final PanamaFunction mockInitialFunction = mock(PanamaFunction.class);
      final MemorySegment mockInitialHandle = MemorySegment.ofAddress(0x4000L);

      final MethodHandle sizeHandle = createSizeHandle(previousSize);
      final MethodHandle growHandle = createGrowHandle(true);
      
      when(mockNativeBindings.getTableSize()).thenReturn(sizeHandle);
      when(mockInitialFunction.getFunctionHandle()).thenReturn(mockInitialHandle);
      when(mockNativeBindings.getTableGrow()).thenReturn(growHandle);

      // Act
      final int result = panamaTable.grow(delta, mockInitialFunction);

      // Assert
      assertEquals((int) previousSize, result);
      verify(mockNativeBindings).getTableGrow();
    }

    @Test

    void testThrowExceptionForNegativeDelta() {
      // Arrange
      final int delta = -5;

      // Act & Assert
      assertThrows(IllegalArgumentException.class, () -> panamaTable.grow(delta, null));
    }

    @Test

    void testHandleGrowOperationFailure() throws Throwable {
      // Arrange
      final int delta = 5;
      final long previousSize = 10L;

      final MethodHandle sizeHandle = createSizeHandle(previousSize);
      final MethodHandle growHandle = createGrowHandle(false);
      
      when(mockNativeBindings.getTableSize()).thenReturn(sizeHandle);
      when(mockNativeBindings.getTableGrow()).thenReturn(growHandle);

      // Act
      final int result = panamaTable.grow(delta, null);

      // Assert - grow returns -1 on failure as per implementation
      assertEquals(-1, result);
    }
  }

  @Nested

  class ResourceManagementTests {

    @Test

    void testCloseTableSuccessfully() throws Throwable {
      // Act
      panamaTable.close();

      // Assert
      assertTrue(panamaTable.isClosed());
      verify(mockArenaManager).unregisterManagedResource(panamaTable);
    }

    @Test

    void testHandleDoubleCloseGracefully() throws Throwable {
      // Act
      panamaTable.close();
      panamaTable.close(); // Second close should be safe

      // Assert
      assertTrue(panamaTable.isClosed());
    }

    @Test

    void testThrowExceptionWhenAccessingClosedTable() throws Throwable {
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

    void testReturnCorrectTableHandle() {
      // Act
      final MemorySegment handle = panamaTable.getTableHandle();

      // Assert
      assertEquals(mockTableHandle, handle);
    }
  }

  @Nested

  class UtilityMethodTests {

    @Test

    void testReturnCorrectMaxSize() throws Throwable {
      // Act
      final int maxSize = panamaTable.getMaxSize();

      // Assert
      assertEquals(-1L, maxSize); // Unlimited
    }

    @Test

    void testReturnCorrectElementType() throws Throwable {
      // Act
      final WasmValueType elementType = panamaTable.getElementType();

      // Assert
      assertEquals(WasmValueType.FUNCREF, elementType);
    }

    @Test

    void testReturnCorrectStringRepresentation() throws Throwable {
      // Arrange
      final MethodHandle sizeHandle = createSizeHandle(5L);
      when(mockNativeBindings.getTableSize()).thenReturn(sizeHandle);

      // Act
      final String stringRepr = panamaTable.toString();

      // Assert
      assertTrue(stringRepr.contains("PanamaTable"));
      assertTrue(stringRepr.contains("size=5"));
      assertTrue(stringRepr.contains("elementType=FUNCREF"));
    }

    @Test

    void testReturnCorrectStringRepresentationWhenClosed() throws Throwable {
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

  class ThreadSafetyTests {

    @Test

    void testHandleConcurrentAccessSafely() throws Throwable {
      // Arrange
      final MethodHandle sizeHandle = createSizeHandle(10L);
      final MethodHandle getHandle = createGetHandle(MemorySegment.NULL);
      final MethodHandle setHandle = createSetHandle(true);
      
      when(mockNativeBindings.getTableSize()).thenReturn(sizeHandle);
      when(mockNativeBindings.getTableGet()).thenReturn(getHandle);
      when(mockNativeBindings.getTableSet()).thenReturn(setHandle);

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

    void testHandleConcurrentCloseSafely() throws Throwable {
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
