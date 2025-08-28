package ai.tegmentum.wasmtime4j.jni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import ai.tegmentum.wasmtime4j.jni.exception.JniValidationException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@link JniInstance}.
 *
 * <p>Note: These tests focus on the Java wrapper logic and defensive programming. Native method
 * behavior is tested separately in integration tests.
 */
class JniInstanceTest {

  private static final long VALID_HANDLE = 0x12345678L;
  private static final long FUNCTION_HANDLE = 0xAABBCCDDL;
  private static final long MEMORY_HANDLE = 0x11223344L;
  private static final long TABLE_HANDLE = 0x55667788L;
  private static final long GLOBAL_HANDLE = 0x99AABBCCL;

  @Test
  void testConstructorWithValidHandle() {
    final JniInstance instance = new JniInstance(VALID_HANDLE);

    assertThat(instance.getNativeHandle()).isEqualTo(VALID_HANDLE);
    assertThat(instance.getResourceType()).isEqualTo("Instance");
    assertFalse(instance.isClosed());
  }

  @Test
  void testConstructorWithInvalidHandle() {
    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> new JniInstance(0L));

    assertThat(exception.getMessage()).contains("nativeHandle");
    assertThat(exception.getMessage()).contains("invalid native handle");
  }

  @Test
  void testGetFunctionWithValidName() {
    try (MockedStatic<JniInstance> mockedStatic = mockStatic(JniInstance.class)) {
      mockedStatic
          .when(() -> JniInstance.nativeGetFunction(VALID_HANDLE, "test_func"))
          .thenReturn(FUNCTION_HANDLE);

      final JniInstance instance = new JniInstance(VALID_HANDLE);
      final JniFunction function = instance.getFunction("test_func");

      assertThat(function).isNotNull();
      assertThat(function.getName()).isEqualTo("test_func");
      assertThat(function.getNativeHandle()).isEqualTo(FUNCTION_HANDLE);
    }
  }

  @Test
  void testGetFunctionWithNullName() {
    final JniInstance instance = new JniInstance(VALID_HANDLE);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> instance.getFunction(null));

    assertThat(exception.getMessage()).contains("name");
    assertThat(exception.getMessage()).contains("must not be null");
  }

  @Test
  void testGetFunctionWithEmptyName() {
    final JniInstance instance = new JniInstance(VALID_HANDLE);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> instance.getFunction(""));

    assertThat(exception.getMessage()).contains("name");
    assertThat(exception.getMessage()).contains("must not be empty");
  }

  @Test
  void testGetFunctionWithWhitespaceOnlyName() {
    final JniInstance instance = new JniInstance(VALID_HANDLE);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> instance.getFunction("   "));

    assertThat(exception.getMessage()).contains("name");
    assertThat(exception.getMessage()).contains("must not be empty");
  }

  @Test
  void testGetFunctionNotFound() {
    try (MockedStatic<JniInstance> mockedStatic = mockStatic(JniInstance.class)) {
      mockedStatic
          .when(() -> JniInstance.nativeGetFunction(VALID_HANDLE, "missing_func"))
          .thenReturn(0L);

      final JniInstance instance = new JniInstance(VALID_HANDLE);

      final RuntimeException exception =
          assertThrows(RuntimeException.class, () -> instance.getFunction("missing_func"));

      assertThat(exception.getMessage()).contains("Function not found: missing_func");
    }
  }

  @Test
  void testGetMemoryWithValidName() {
    try (MockedStatic<JniInstance> mockedStatic = mockStatic(JniInstance.class)) {
      mockedStatic
          .when(() -> JniInstance.nativeGetMemory(VALID_HANDLE, "memory"))
          .thenReturn(MEMORY_HANDLE);

      final JniInstance instance = new JniInstance(VALID_HANDLE);
      final JniMemory memory = instance.getMemory("memory");

      assertThat(memory).isNotNull();
      assertThat(memory.getNativeHandle()).isEqualTo(MEMORY_HANDLE);
    }
  }

  @Test
  void testGetDefaultMemory() {
    try (MockedStatic<JniInstance> mockedStatic = mockStatic(JniInstance.class)) {
      mockedStatic
          .when(() -> JniInstance.nativeGetMemory(VALID_HANDLE, "memory"))
          .thenReturn(MEMORY_HANDLE);

      final JniInstance instance = new JniInstance(VALID_HANDLE);
      final JniMemory memory = instance.getDefaultMemory();

      assertThat(memory).isNotNull();
      assertThat(memory.getNativeHandle()).isEqualTo(MEMORY_HANDLE);
    }
  }

  @Test
  void testGetTableWithValidName() {
    try (MockedStatic<JniInstance> mockedStatic = mockStatic(JniInstance.class)) {
      mockedStatic
          .when(() -> JniInstance.nativeGetTable(VALID_HANDLE, "table"))
          .thenReturn(TABLE_HANDLE);

      final JniInstance instance = new JniInstance(VALID_HANDLE);
      final JniTable table = instance.getTable("table");

      assertThat(table).isNotNull();
      assertThat(table.getNativeHandle()).isEqualTo(TABLE_HANDLE);
    }
  }

  @Test
  void testGetGlobalWithValidName() {
    try (MockedStatic<JniInstance> mockedStatic = mockStatic(JniInstance.class)) {
      mockedStatic
          .when(() -> JniInstance.nativeGetGlobal(VALID_HANDLE, "global"))
          .thenReturn(GLOBAL_HANDLE);

      final JniInstance instance = new JniInstance(VALID_HANDLE);
      final JniGlobal global = instance.getGlobal("global");

      assertThat(global).isNotNull();
      assertThat(global.getNativeHandle()).isEqualTo(GLOBAL_HANDLE);
    }
  }

  @Test
  void testHasExportWithValidName() {
    try (MockedStatic<JniInstance> mockedStatic = mockStatic(JniInstance.class)) {
      mockedStatic
          .when(() -> JniInstance.nativeHasExport(VALID_HANDLE, "existing"))
          .thenReturn(true);
      mockedStatic
          .when(() -> JniInstance.nativeHasExport(VALID_HANDLE, "missing"))
          .thenReturn(false);

      final JniInstance instance = new JniInstance(VALID_HANDLE);

      assertTrue(instance.hasExport("existing"));
      assertFalse(instance.hasExport("missing"));
    }
  }

  @Test
  void testHasExportWithInvalidName() {
    final JniInstance instance = new JniInstance(VALID_HANDLE);

    assertThrows(JniValidationException.class, () -> instance.hasExport(null));
    assertThrows(JniValidationException.class, () -> instance.hasExport(""));
    assertThrows(JniValidationException.class, () -> instance.hasExport("   "));
  }

  @Test
  void testHasExportWithException() {
    try (MockedStatic<JniInstance> mockedStatic = mockStatic(JniInstance.class)) {
      mockedStatic
          .when(() -> JniInstance.nativeHasExport(anyLong(), anyString()))
          .thenThrow(new RuntimeException("Native error"));

      final JniInstance instance = new JniInstance(VALID_HANDLE);

      // Should return false when native call fails
      assertFalse(instance.hasExport("test"));
    }
  }

  @Test
  void testOperationsOnClosedInstance() {
    final JniInstance instance = new JniInstance(VALID_HANDLE);
    instance.close();

    assertThrows(JniResourceException.class, () -> instance.getFunction("test"));
    assertThrows(JniResourceException.class, () -> instance.getMemory("test"));
    assertThrows(JniResourceException.class, () -> instance.getTable("test"));
    assertThrows(JniResourceException.class, () -> instance.getGlobal("test"));
    assertThrows(JniResourceException.class, () -> instance.hasExport("test"));
    assertThrows(JniResourceException.class, instance::getDefaultMemory);
    assertThrows(JniResourceException.class, instance::getNativeHandle);
  }

  @Test
  void testClose() {
    try (MockedStatic<JniInstance> mockedStatic = mockStatic(JniInstance.class)) {
      mockedStatic
          .when(() -> JniInstance.nativeDestroyInstance(VALID_HANDLE))
          .then(invocation -> null);

      final JniInstance instance = new JniInstance(VALID_HANDLE);
      assertFalse(instance.isClosed());

      instance.close();

      assertTrue(instance.isClosed());
      mockedStatic.verify(() -> JniInstance.nativeDestroyInstance(VALID_HANDLE));
    }
  }

  @Test
  void testCloseIsIdempotent() {
    try (MockedStatic<JniInstance> mockedStatic = mockStatic(JniInstance.class)) {
      mockedStatic
          .when(() -> JniInstance.nativeDestroyInstance(VALID_HANDLE))
          .then(invocation -> null);

      final JniInstance instance = new JniInstance(VALID_HANDLE);

      instance.close();
      instance.close(); // Second close should be safe

      assertTrue(instance.isClosed());
      // Should only call native destroy once
      mockedStatic.verify(() -> JniInstance.nativeDestroyInstance(VALID_HANDLE));
    }
  }

  @Test
  void testTryWithResources() {
    try (MockedStatic<JniInstance> mockedStatic = mockStatic(JniInstance.class)) {
      mockedStatic
          .when(() -> JniInstance.nativeDestroyInstance(VALID_HANDLE))
          .then(invocation -> null);

      assertDoesNotThrow(
          () -> {
            try (JniInstance instance = new JniInstance(VALID_HANDLE)) {
              assertFalse(instance.isClosed());
              assertThat(instance.getNativeHandle()).isEqualTo(VALID_HANDLE);
            }
          });

      mockedStatic.verify(() -> JniInstance.nativeDestroyInstance(VALID_HANDLE));
    }
  }

  @Test
  void testToString() {
    final JniInstance instance = new JniInstance(VALID_HANDLE);
    final String toString = instance.toString();

    assertThat(toString).contains("Instance");
    assertThat(toString).contains("handle=0x" + Long.toHexString(VALID_HANDLE));
    assertThat(toString).contains("closed=false");

    instance.close();
    final String toStringAfterClose = instance.toString();
    assertThat(toStringAfterClose).contains("closed=true");
  }

  @Test
  void testGetResourceType() {
    final JniInstance instance = new JniInstance(VALID_HANDLE);
    assertThat(instance.getResourceType()).isEqualTo("Instance");
  }

  @Test
  void testConcurrentAccess() {
    try (MockedStatic<JniInstance> mockedStatic = mockStatic(JniInstance.class)) {
      mockedStatic.when(() -> JniInstance.nativeHasExport(VALID_HANDLE, "test")).thenReturn(true);

      final JniInstance instance = new JniInstance(VALID_HANDLE);

      // Test concurrent access doesn't cause issues
      final Thread[] threads = new Thread[5];
      for (int i = 0; i < threads.length; i++) {
        threads[i] = new Thread(() -> assertTrue(instance.hasExport("test")));
      }

      for (Thread thread : threads) {
        thread.start();
      }

      for (Thread thread : threads) {
        assertDoesNotThrow(() -> thread.join());
      }
    }
  }

  @Test
  void testExceptionHandling() {
    try (MockedStatic<JniInstance> mockedStatic = mockStatic(JniInstance.class)) {
      mockedStatic
          .when(() -> JniInstance.nativeGetFunction(anyLong(), anyString()))
          .thenThrow(new RuntimeException("Native error"));

      final JniInstance instance = new JniInstance(VALID_HANDLE);

      final RuntimeException exception =
          assertThrows(RuntimeException.class, () -> instance.getFunction("test"));

      assertThat(exception.getMessage()).contains("Unexpected error getting function: test");
      assertThat(exception.getCause()).isNotNull();
      assertThat(exception.getCause().getMessage()).isEqualTo("Native error");
    }
  }
}
