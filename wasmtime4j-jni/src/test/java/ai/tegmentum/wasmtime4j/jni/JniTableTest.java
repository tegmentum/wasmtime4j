package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockStatic;

import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive unit tests for {@link JniTable}.
 *
 * <p>These tests focus on the Java wrapper logic, parameter validation, error handling, and
 * resource management using mocked native calls. They ensure defensive programming practices and
 * proper integration with the JniResource infrastructure.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JniTable Tests")
class JniTableTest {

  private static final long VALID_HANDLE = 0x12345678L;
  private static final long INVALID_HANDLE = 0L;

  private JniTable table;
  private MockedStatic<JniTable> mockStatic;

  @BeforeEach
  void setUp() {
    // Mock static native methods
    mockStatic = mockStatic(JniTable.class);
    setupMockNativeMethods();

    // Create test instance
    table = new JniTable(VALID_HANDLE);
  }

  @AfterEach
  void tearDown() {
    if (table != null && !table.isClosed()) {
      table.close();
    }
    if (mockStatic != null) {
      mockStatic.close();
    }
  }

  private void setupMockNativeMethods() {
    // Mock native method implementations
    mockStatic.when(() -> JniTable.nativeGetSize(VALID_HANDLE)).thenReturn(10);
    mockStatic.when(() -> JniTable.nativeGetMaxSize(VALID_HANDLE)).thenReturn(100);
    mockStatic.when(() -> JniTable.nativeGetElementType(VALID_HANDLE)).thenReturn("funcref");
    mockStatic.when(() -> JniTable.nativeGet(VALID_HANDLE, 0)).thenReturn("element0");
    mockStatic.when(() -> JniTable.nativeSet(VALID_HANDLE, 0, "newElement")).thenReturn(true);
    mockStatic.when(() -> JniTable.nativeGrow(VALID_HANDLE, 5, null)).thenReturn(10);
    mockStatic.when(() -> JniTable.nativeFill(VALID_HANDLE, 0, 3, "fillValue")).thenReturn(true);
    mockStatic.when(() -> JniTable.nativeDestroyTable(anyLong())).thenAnswer(invocation -> null);
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create table with valid handle")
    void shouldCreateTableWithValidHandle() {
      final JniTable newTable = new JniTable(VALID_HANDLE);
      assertNotNull(newTable);
      assertEquals(VALID_HANDLE, newTable.getNativeHandle());
      assertFalse(newTable.isClosed());
      newTable.close();
    }

    @Test
    @DisplayName("Should reject invalid handle")
    void shouldRejectInvalidHandle() {
      assertThrows(
          JniResourceException.class,
          () -> new JniTable(INVALID_HANDLE),
          "Should throw JniResourceException for invalid handle");
    }

    @Test
    @DisplayName("Should reject negative handle")
    void shouldRejectNegativeHandle() {
      assertThrows(
          JniResourceException.class,
          () -> new JniTable(-1L),
          "Should throw JniResourceException for negative handle");
    }
  }

  @Nested
  @DisplayName("Size Operations Tests")
  class SizeOperationsTests {

    @Test
    @DisplayName("Should return current size")
    void shouldReturnCurrentSize() {
      assertEquals(10, table.getSize(), "Should return mocked size");
    }

    @Test
    @DisplayName("Should return maximum size")
    void shouldReturnMaximumSize() {
      assertEquals(100, table.getMaxSize(), "Should return mocked max size");
    }

    @Test
    @DisplayName("Should handle unlimited max size")
    void shouldHandleUnlimitedMaxSize() {
      mockStatic.when(() -> JniTable.nativeGetMaxSize(VALID_HANDLE)).thenReturn(-1);
      assertEquals(-1, table.getMaxSize(), "Should return -1 for unlimited size");
    }

    @Test
    @DisplayName("Should throw exception when closed")
    void shouldThrowExceptionWhenClosed() {
      table.close();

      assertThrows(
          JniResourceException.class,
          () -> table.getSize(),
          "Should throw JniResourceException when accessing size of closed table");

      assertThrows(
          JniResourceException.class,
          () -> table.getMaxSize(),
          "Should throw JniResourceException when accessing max size of closed table");
    }
  }

  @Nested
  @DisplayName("Element Type Tests")
  class ElementTypeTests {

    @Test
    @DisplayName("Should return element type")
    void shouldReturnElementType() {
      assertEquals("funcref", table.getElementType(), "Should return mocked element type");
    }

    @Test
    @DisplayName("Should handle null element type")
    void shouldHandleNullElementType() {
      mockStatic.when(() -> JniTable.nativeGetElementType(VALID_HANDLE)).thenReturn(null);
      assertEquals(
          "unknown", table.getElementType(), "Should return 'unknown' for null element type");
    }

    @Test
    @DisplayName("Should handle different element types")
    void shouldHandleDifferentElementTypes() {
      mockStatic.when(() -> JniTable.nativeGetElementType(VALID_HANDLE)).thenReturn("externref");
      assertEquals("externref", table.getElementType(), "Should return externref element type");
    }

    @Test
    @DisplayName("Should throw exception when closed")
    void shouldThrowExceptionWhenClosed() {
      table.close();

      assertThrows(
          JniResourceException.class,
          () -> table.getElementType(),
          "Should throw JniResourceException when accessing element type of closed table");
    }
  }

  @Nested
  @DisplayName("Element Access Tests")
  class ElementAccessTests {

    @Test
    @DisplayName("Should get element at valid index")
    void shouldGetElementAtValidIndex() {
      assertEquals("element0", table.get(0), "Should return element at index 0");
    }

    @Test
    @DisplayName("Should handle null element")
    void shouldHandleNullElement() {
      mockStatic.when(() -> JniTable.nativeGet(VALID_HANDLE, 5)).thenReturn(null);
      assertNull(table.get(5), "Should return null for uninitialized element");
    }

    @Test
    @DisplayName("Should reject negative index")
    void shouldRejectNegativeIndex() {
      assertThrows(
          IllegalArgumentException.class,
          () -> table.get(-1),
          "Should throw IllegalArgumentException for negative index");
    }

    @Test
    @DisplayName("Should reject index beyond bounds")
    void shouldRejectIndexBeyondBounds() {
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> table.get(10),
          "Should throw IndexOutOfBoundsException for index >= size");
    }

    @Test
    @DisplayName("Should throw exception when closed")
    void shouldThrowExceptionWhenClosed() {
      table.close();

      assertThrows(
          JniResourceException.class,
          () -> table.get(0),
          "Should throw JniResourceException when accessing closed table");
    }
  }

  @Nested
  @DisplayName("Element Modification Tests")
  class ElementModificationTests {

    @Test
    @DisplayName("Should set element at valid index")
    void shouldSetElementAtValidIndex() {
      // This should not throw any exception
      table.set(0, "newElement");
    }

    @Test
    @DisplayName("Should handle null value")
    void shouldHandleNullValue() {
      mockStatic.when(() -> JniTable.nativeSet(VALID_HANDLE, 1, null)).thenReturn(true);
      // Should not throw exception
      table.set(1, null);
    }

    @Test
    @DisplayName("Should reject negative index")
    void shouldRejectNegativeIndex() {
      assertThrows(
          IllegalArgumentException.class,
          () -> table.set(-1, "value"),
          "Should throw IllegalArgumentException for negative index");
    }

    @Test
    @DisplayName("Should reject index beyond bounds")
    void shouldRejectIndexBeyondBounds() {
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> table.set(10, "value"),
          "Should throw IndexOutOfBoundsException for index >= size");
    }

    @Test
    @DisplayName("Should handle native set failure")
    void shouldHandleNativeSetFailure() {
      mockStatic.when(() -> JniTable.nativeSet(VALID_HANDLE, 0, "badValue")).thenReturn(false);

      assertThrows(
          RuntimeException.class,
          () -> table.set(0, "badValue"),
          "Should throw RuntimeException when native set fails");
    }

    @Test
    @DisplayName("Should throw exception when closed")
    void shouldThrowExceptionWhenClosed() {
      table.close();

      assertThrows(
          JniResourceException.class,
          () -> table.set(0, "value"),
          "Should throw JniResourceException when setting element in closed table");
    }
  }

  @Nested
  @DisplayName("Growth Operations Tests")
  class GrowthOperationsTests {

    @Test
    @DisplayName("Should grow table with valid delta")
    void shouldGrowTableWithValidDelta() {
      final int oldSize = table.grow(5, null);
      assertEquals(10, oldSize, "Should return previous size");
    }

    @Test
    @DisplayName("Should grow table with init value")
    void shouldGrowTableWithInitValue() {
      mockStatic.when(() -> JniTable.nativeGrow(VALID_HANDLE, 3, "initValue")).thenReturn(10);
      final int oldSize = table.grow(3, "initValue");
      assertEquals(10, oldSize, "Should return previous size with init value");
    }

    @Test
    @DisplayName("Should handle growth failure")
    void shouldHandleGrowthFailure() {
      mockStatic.when(() -> JniTable.nativeGrow(VALID_HANDLE, 1000, null)).thenReturn(-1);
      final int result = table.grow(1000, null);
      assertEquals(-1, result, "Should return -1 for failed growth");
    }

    @Test
    @DisplayName("Should reject negative delta")
    void shouldRejectNegativeDelta() {
      assertThrows(
          IllegalArgumentException.class,
          () -> table.grow(-1, null),
          "Should throw IllegalArgumentException for negative delta");
    }

    @Test
    @DisplayName("Should allow zero delta")
    void shouldAllowZeroDelta() {
      mockStatic.when(() -> JniTable.nativeGrow(VALID_HANDLE, 0, null)).thenReturn(10);
      final int oldSize = table.grow(0, null);
      assertEquals(10, oldSize, "Should handle zero delta gracefully");
    }

    @Test
    @DisplayName("Should throw exception when closed")
    void shouldThrowExceptionWhenClosed() {
      table.close();

      assertThrows(
          JniResourceException.class,
          () -> table.grow(1, null),
          "Should throw JniResourceException when growing closed table");
    }
  }

  @Nested
  @DisplayName("Fill Operations Tests")
  class FillOperationsTests {

    @Test
    @DisplayName("Should fill table range")
    void shouldFillTableRange() {
      // Should not throw any exception
      table.fill(0, 3, "fillValue");
    }

    @Test
    @DisplayName("Should handle null fill value")
    void shouldHandleNullFillValue() {
      mockStatic.when(() -> JniTable.nativeFill(VALID_HANDLE, 0, 2, null)).thenReturn(true);
      // Should not throw exception
      table.fill(0, 2, null);
    }

    @Test
    @DisplayName("Should reject negative start")
    void shouldRejectNegativeStart() {
      assertThrows(
          IllegalArgumentException.class,
          () -> table.fill(-1, 1, "value"),
          "Should throw IllegalArgumentException for negative start");
    }

    @Test
    @DisplayName("Should reject negative count")
    void shouldRejectNegativeCount() {
      assertThrows(
          IllegalArgumentException.class,
          () -> table.fill(0, -1, "value"),
          "Should throw IllegalArgumentException for negative count");
    }

    @Test
    @DisplayName("Should reject range beyond bounds")
    void shouldRejectRangeBeyondBounds() {
      assertThrows(
          IndexOutOfBoundsException.class,
          () -> table.fill(8, 5, "value"),
          "Should throw IndexOutOfBoundsException for range beyond table size");
    }

    @Test
    @DisplayName("Should allow zero count")
    void shouldAllowZeroCount() {
      mockStatic.when(() -> JniTable.nativeFill(VALID_HANDLE, 0, 0, "value")).thenReturn(true);
      // Should not throw exception
      table.fill(0, 0, "value");
    }

    @Test
    @DisplayName("Should handle native fill failure")
    void shouldHandleNativeFillFailure() {
      mockStatic.when(() -> JniTable.nativeFill(VALID_HANDLE, 0, 1, "badValue")).thenReturn(false);

      assertThrows(
          RuntimeException.class,
          () -> table.fill(0, 1, "badValue"),
          "Should throw RuntimeException when native fill fails");
    }

    @Test
    @DisplayName("Should throw exception when closed")
    void shouldThrowExceptionWhenClosed() {
      table.close();

      assertThrows(
          JniResourceException.class,
          () -> table.fill(0, 1, "value"),
          "Should throw JniResourceException when filling closed table");
    }
  }

  @Nested
  @DisplayName("Resource Management Tests")
  class ResourceManagementTests {

    @Test
    @DisplayName("Should provide resource type")
    void shouldProvideResourceType() {
      assertEquals("Table", table.getResourceType(), "Should return 'Table' as resource type");
    }

    @Test
    @DisplayName("Should close gracefully")
    void shouldCloseGracefully() {
      assertFalse(table.isClosed(), "Should not be closed initially");
      table.close();
      assertTrue(table.isClosed(), "Should be closed after calling close()");
    }

    @Test
    @DisplayName("Should be idempotent on close")
    void shouldBeIdempotentOnClose() {
      table.close();
      assertTrue(table.isClosed(), "Should be closed after first call");

      // Second close should not throw
      table.close();
      assertTrue(table.isClosed(), "Should remain closed after second call");
    }

    @Test
    @DisplayName("Should handle native cleanup failure gracefully")
    void shouldHandleNativeCleanupFailureGracefully() {
      // Make native destroy throw an exception
      mockStatic
          .when(() -> JniTable.nativeDestroyTable(VALID_HANDLE))
          .thenThrow(new RuntimeException("Native cleanup failed"));

      // Close should still complete successfully
      table.close();
      assertTrue(table.isClosed(), "Should be marked as closed even if native cleanup fails");
    }

    @Test
    @DisplayName("Should work with try-with-resources")
    void shouldWorkWithTryWithResources() {
      final long testHandle = 0xABCDEF00L;
      mockStatic.when(() -> JniTable.nativeDestroyTable(testHandle)).thenAnswer(invocation -> null);

      try (final JniTable autoClosedTable = new JniTable(testHandle)) {
        assertFalse(autoClosedTable.isClosed(), "Should not be closed inside try block");
      }
      // Table should be automatically closed after try block
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Should handle native exceptions gracefully")
    void shouldHandleNativeExceptionsGracefully() {
      mockStatic
          .when(() -> JniTable.nativeGetSize(VALID_HANDLE))
          .thenThrow(new RuntimeException("Native error"));

      final RuntimeException exception =
          assertThrows(
              RuntimeException.class,
              () -> table.getSize(),
              "Should propagate native RuntimeException");

      assertEquals("Unexpected error getting table size", exception.getMessage());
    }

    @Test
    @DisplayName("Should preserve RuntimeExceptions in get operations")
    void shouldPreserveRuntimeExceptionsInGetOperations() {
      final RuntimeException nativeException = new RuntimeException("Native get error");
      mockStatic.when(() -> JniTable.nativeGet(VALID_HANDLE, 0)).thenThrow(nativeException);

      final RuntimeException thrown =
          assertThrows(
              RuntimeException.class,
              () -> table.get(0),
              "Should propagate RuntimeException from native get");

      assertEquals(nativeException, thrown, "Should preserve the original RuntimeException");
    }

    @Test
    @DisplayName("Should wrap checked exceptions")
    void shouldWrapCheckedExceptions() {
      mockStatic
          .when(() -> JniTable.nativeGetElementType(VALID_HANDLE))
          .thenThrow(new Exception("Checked exception"));

      final RuntimeException exception =
          assertThrows(
              RuntimeException.class,
              () -> table.getElementType(),
              "Should wrap checked exceptions in RuntimeException");

      assertEquals("Unexpected error getting table element type", exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Concurrent Access Tests")
  class ConcurrentAccessTests {

    @Test
    @DisplayName("Should handle concurrent size queries")
    void shouldHandleConcurrentSizeQueries() throws InterruptedException {
      final int numThreads = 10;
      final Thread[] threads = new Thread[numThreads];
      final boolean[] results = new boolean[numThreads];

      for (int i = 0; i < numThreads; i++) {
        final int threadIndex = i;
        threads[i] =
            new Thread(
                () -> {
                  try {
                    final int size = table.getSize();
                    results[threadIndex] = (size == 10);
                  } catch (final Exception e) {
                    results[threadIndex] = false;
                  }
                });
        threads[i].start();
      }

      for (final Thread thread : threads) {
        thread.join();
      }

      for (int i = 0; i < numThreads; i++) {
        assertTrue(results[i], "Thread " + i + " should have succeeded");
      }
    }

    @Test
    @DisplayName("Should handle concurrent close operations")
    void shouldHandleConcurrentCloseOperations() throws InterruptedException {
      final int numThreads = 5;
      final Thread[] threads = new Thread[numThreads];

      for (int i = 0; i < numThreads; i++) {
        threads[i] =
            new Thread(
                () -> {
                  try {
                    table.close();
                  } catch (final Exception e) {
                    // Should not throw
                  }
                });
        threads[i].start();
      }

      for (final Thread thread : threads) {
        thread.join();
      }

      assertTrue(table.isClosed(), "Table should be closed after concurrent close operations");
    }
  }

  @Nested
  @DisplayName("toString and Object Methods Tests")
  class ToStringAndObjectMethodsTests {

    @Test
    @DisplayName("Should provide meaningful toString")
    void shouldProvideMeaningfulToString() {
      final String toString = table.toString();
      assertNotNull(toString, "toString should not be null");
      assertTrue(toString.contains("Table"), "toString should contain resource type");
      assertTrue(
          toString.contains(Long.toHexString(VALID_HANDLE)), "toString should contain handle");
      assertTrue(toString.contains("false"), "toString should show not closed");
    }

    @Test
    @DisplayName("Should show closed state in toString")
    void shouldShowClosedStateInToString() {
      table.close();
      final String toString = table.toString();
      assertTrue(toString.contains("true"), "toString should show closed state");
    }
  }
}
