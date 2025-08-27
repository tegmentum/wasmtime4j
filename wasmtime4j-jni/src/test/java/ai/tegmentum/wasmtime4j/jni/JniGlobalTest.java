package ai.tegmentum.wasmtime4j.jni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import ai.tegmentum.wasmtime4j.jni.exception.JniValidationException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@link JniGlobal}.
 *
 * <p>Note: These tests focus on the Java wrapper logic and defensive programming. Native method
 * behavior is tested separately in integration tests.
 */
class JniGlobalTest {

  private static final long VALID_HANDLE = 0xFEDCBA98L;

  @Test
  void testConstructorWithValidHandle() {
    final JniGlobal global = new JniGlobal(VALID_HANDLE);

    assertThat(global.getNativeHandle()).isEqualTo(VALID_HANDLE);
    assertThat(global.getResourceType()).isEqualTo("Global");
    assertFalse(global.isClosed());
  }

  @Test
  void testConstructorWithInvalidHandle() {
    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> new JniGlobal(0L));

    assertThat(exception.getMessage()).contains("nativeHandle");
    assertThat(exception.getMessage()).contains("invalid native handle");
  }

  @Test
  void testGetValueType() {
    try (MockedStatic<JniGlobal> mockedStatic = mockStatic(JniGlobal.class)) {
      mockedStatic.when(() -> JniGlobal.nativeGetValueType(VALID_HANDLE)).thenReturn("i32");

      final JniGlobal global = new JniGlobal(VALID_HANDLE);
      final String valueType = global.getValueType();

      assertThat(valueType).isEqualTo("i32");
    }
  }

  @Test
  void testGetValueTypeReturnsUnknownWhenNull() {
    try (MockedStatic<JniGlobal> mockedStatic = mockStatic(JniGlobal.class)) {
      mockedStatic.when(() -> JniGlobal.nativeGetValueType(VALID_HANDLE)).thenReturn(null);

      final JniGlobal global = new JniGlobal(VALID_HANDLE);
      final String valueType = global.getValueType();

      assertThat(valueType).isEqualTo("unknown");
    }
  }

  @Test
  void testIsMutable() {
    try (MockedStatic<JniGlobal> mockedStatic = mockStatic(JniGlobal.class)) {
      mockedStatic.when(() -> JniGlobal.nativeIsMutable(VALID_HANDLE)).thenReturn(true);

      final JniGlobal global = new JniGlobal(VALID_HANDLE);
      final boolean mutable = global.isMutable();

      assertTrue(mutable);
    }
  }

  @Test
  void testIsImmutable() {
    try (MockedStatic<JniGlobal> mockedStatic = mockStatic(JniGlobal.class)) {
      mockedStatic.when(() -> JniGlobal.nativeIsMutable(VALID_HANDLE)).thenReturn(false);

      final JniGlobal global = new JniGlobal(VALID_HANDLE);
      final boolean mutable = global.isMutable();

      assertFalse(mutable);
    }
  }

  @Test
  void testGetValue() {
    try (MockedStatic<JniGlobal> mockedStatic = mockStatic(JniGlobal.class)) {
      final Integer expectedValue = 42;
      mockedStatic.when(() -> JniGlobal.nativeGetValue(VALID_HANDLE)).thenReturn(expectedValue);

      final JniGlobal global = new JniGlobal(VALID_HANDLE);
      final Object value = global.getValue();

      assertThat(value).isEqualTo(expectedValue);
    }
  }

  @Test
  void testGetIntValue() {
    try (MockedStatic<JniGlobal> mockedStatic = mockStatic(JniGlobal.class)) {
      mockedStatic.when(() -> JniGlobal.nativeGetIntValue(VALID_HANDLE)).thenReturn(123);

      final JniGlobal global = new JniGlobal(VALID_HANDLE);
      final int value = global.getIntValue();

      assertThat(value).isEqualTo(123);
    }
  }

  @Test
  void testGetLongValue() {
    try (MockedStatic<JniGlobal> mockedStatic = mockStatic(JniGlobal.class)) {
      mockedStatic.when(() -> JniGlobal.nativeGetLongValue(VALID_HANDLE)).thenReturn(456L);

      final JniGlobal global = new JniGlobal(VALID_HANDLE);
      final long value = global.getLongValue();

      assertThat(value).isEqualTo(456L);
    }
  }

  @Test
  void testGetFloatValue() {
    try (MockedStatic<JniGlobal> mockedStatic = mockStatic(JniGlobal.class)) {
      mockedStatic.when(() -> JniGlobal.nativeGetFloatValue(VALID_HANDLE)).thenReturn(3.14f);

      final JniGlobal global = new JniGlobal(VALID_HANDLE);
      final float value = global.getFloatValue();

      assertThat(value).isEqualTo(3.14f);
    }
  }

  @Test
  void testGetDoubleValue() {
    try (MockedStatic<JniGlobal> mockedStatic = mockStatic(JniGlobal.class)) {
      mockedStatic.when(() -> JniGlobal.nativeGetDoubleValue(VALID_HANDLE)).thenReturn(2.718);

      final JniGlobal global = new JniGlobal(VALID_HANDLE);
      final double value = global.getDoubleValue();

      assertThat(value).isEqualTo(2.718);
    }
  }

  @Test
  void testSetValueOnMutableGlobal() {
    try (MockedStatic<JniGlobal> mockedStatic = mockStatic(JniGlobal.class)) {
      mockedStatic.when(() -> JniGlobal.nativeIsMutable(VALID_HANDLE)).thenReturn(true);
      mockedStatic.when(() -> JniGlobal.nativeSetValue(VALID_HANDLE, 999)).thenReturn(true);

      final JniGlobal global = new JniGlobal(VALID_HANDLE);

      assertDoesNotThrow(() -> global.setValue(999));
    }
  }

  @Test
  void testSetValueWithNullValue() {
    final JniGlobal global = new JniGlobal(VALID_HANDLE);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> global.setValue(null));

    assertThat(exception.getMessage()).contains("value");
    assertThat(exception.getMessage()).contains("must not be null");
  }

  @Test
  void testSetValueOnImmutableGlobal() {
    try (MockedStatic<JniGlobal> mockedStatic = mockStatic(JniGlobal.class)) {
      mockedStatic.when(() -> JniGlobal.nativeIsMutable(VALID_HANDLE)).thenReturn(false);

      final JniGlobal global = new JniGlobal(VALID_HANDLE);

      final JniResourceException exception =
          assertThrows(JniResourceException.class, () -> global.setValue(123));

      assertThat(exception.getMessage()).contains("Global is immutable");
    }
  }

  @Test
  void testSetValueFailure() {
    try (MockedStatic<JniGlobal> mockedStatic = mockStatic(JniGlobal.class)) {
      mockedStatic.when(() -> JniGlobal.nativeIsMutable(VALID_HANDLE)).thenReturn(true);
      mockedStatic.when(() -> JniGlobal.nativeSetValue(VALID_HANDLE, 123)).thenReturn(false);

      final JniGlobal global = new JniGlobal(VALID_HANDLE);

      final RuntimeException exception =
          assertThrows(RuntimeException.class, () -> global.setValue(123));

      assertThat(exception.getMessage()).contains("Failed to set global value");
    }
  }

  @Test
  void testSetIntValueOnMutableGlobal() {
    try (MockedStatic<JniGlobal> mockedStatic = mockStatic(JniGlobal.class)) {
      mockedStatic.when(() -> JniGlobal.nativeIsMutable(VALID_HANDLE)).thenReturn(true);
      mockedStatic.when(() -> JniGlobal.nativeSetIntValue(VALID_HANDLE, 789)).thenReturn(true);

      final JniGlobal global = new JniGlobal(VALID_HANDLE);

      assertDoesNotThrow(() -> global.setIntValue(789));
    }
  }

  @Test
  void testSetIntValueOnImmutableGlobal() {
    try (MockedStatic<JniGlobal> mockedStatic = mockStatic(JniGlobal.class)) {
      mockedStatic.when(() -> JniGlobal.nativeIsMutable(VALID_HANDLE)).thenReturn(false);

      final JniGlobal global = new JniGlobal(VALID_HANDLE);

      final JniResourceException exception =
          assertThrows(JniResourceException.class, () -> global.setIntValue(789));

      assertThat(exception.getMessage()).contains("Global is immutable");
    }
  }

  @Test
  void testSetLongValueOnMutableGlobal() {
    try (MockedStatic<JniGlobal> mockedStatic = mockStatic(JniGlobal.class)) {
      mockedStatic.when(() -> JniGlobal.nativeIsMutable(VALID_HANDLE)).thenReturn(true);
      mockedStatic.when(() -> JniGlobal.nativeSetLongValue(VALID_HANDLE, 999L)).thenReturn(true);

      final JniGlobal global = new JniGlobal(VALID_HANDLE);

      assertDoesNotThrow(() -> global.setLongValue(999L));
    }
  }

  @Test
  void testSetFloatValueOnMutableGlobal() {
    try (MockedStatic<JniGlobal> mockedStatic = mockStatic(JniGlobal.class)) {
      mockedStatic.when(() -> JniGlobal.nativeIsMutable(VALID_HANDLE)).thenReturn(true);
      mockedStatic.when(() -> JniGlobal.nativeSetFloatValue(VALID_HANDLE, 1.23f)).thenReturn(true);

      final JniGlobal global = new JniGlobal(VALID_HANDLE);

      assertDoesNotThrow(() -> global.setFloatValue(1.23f));
    }
  }

  @Test
  void testSetDoubleValueOnMutableGlobal() {
    try (MockedStatic<JniGlobal> mockedStatic = mockStatic(JniGlobal.class)) {
      mockedStatic.when(() -> JniGlobal.nativeIsMutable(VALID_HANDLE)).thenReturn(true);
      mockedStatic.when(() -> JniGlobal.nativeSetDoubleValue(VALID_HANDLE, 4.56)).thenReturn(true);

      final JniGlobal global = new JniGlobal(VALID_HANDLE);

      assertDoesNotThrow(() -> global.setDoubleValue(4.56));
    }
  }

  @Test
  void testAllSetMethodsFailOnImmutableGlobal() {
    try (MockedStatic<JniGlobal> mockedStatic = mockStatic(JniGlobal.class)) {
      mockedStatic.when(() -> JniGlobal.nativeIsMutable(VALID_HANDLE)).thenReturn(false);

      final JniGlobal global = new JniGlobal(VALID_HANDLE);

      assertThrows(JniResourceException.class, () -> global.setIntValue(1));
      assertThrows(JniResourceException.class, () -> global.setLongValue(1L));
      assertThrows(JniResourceException.class, () -> global.setFloatValue(1.0f));
      assertThrows(JniResourceException.class, () -> global.setDoubleValue(1.0));
    }
  }

  @Test
  void testAllSetMethodsFailureHandling() {
    try (MockedStatic<JniGlobal> mockedStatic = mockStatic(JniGlobal.class)) {
      mockedStatic.when(() -> JniGlobal.nativeIsMutable(VALID_HANDLE)).thenReturn(true);
      mockedStatic.when(() -> JniGlobal.nativeSetIntValue(anyLong(), anyInt())).thenReturn(false);
      mockedStatic.when(() -> JniGlobal.nativeSetLongValue(anyLong(), anyLong())).thenReturn(false);
      mockedStatic.when(() -> JniGlobal.nativeSetFloatValue(anyLong(), anyFloat())).thenReturn(false);
      mockedStatic.when(() -> JniGlobal.nativeSetDoubleValue(anyLong(), anyDouble())).thenReturn(false);

      final JniGlobal global = new JniGlobal(VALID_HANDLE);

      assertThrows(RuntimeException.class, () -> global.setIntValue(1));
      assertThrows(RuntimeException.class, () -> global.setLongValue(1L));
      assertThrows(RuntimeException.class, () -> global.setFloatValue(1.0f));
      assertThrows(RuntimeException.class, () -> global.setDoubleValue(1.0));
    }
  }

  @Test
  void testOperationsOnClosedGlobal() {
    final JniGlobal global = new JniGlobal(VALID_HANDLE);
    global.close();

    assertThrows(JniResourceException.class, global::getValueType);
    assertThrows(JniResourceException.class, global::isMutable);
    assertThrows(JniResourceException.class, global::getValue);
    assertThrows(JniResourceException.class, global::getIntValue);
    assertThrows(JniResourceException.class, global::getLongValue);
    assertThrows(JniResourceException.class, global::getFloatValue);
    assertThrows(JniResourceException.class, global::getDoubleValue);
    assertThrows(JniResourceException.class, () -> global.setValue(123));
    assertThrows(JniResourceException.class, () -> global.setIntValue(123));
    assertThrows(JniResourceException.class, () -> global.setLongValue(123L));
    assertThrows(JniResourceException.class, () -> global.setFloatValue(1.23f));
    assertThrows(JniResourceException.class, () -> global.setDoubleValue(1.23));
    assertThrows(JniResourceException.class, global::getNativeHandle);
  }

  @Test
  void testClose() {
    try (MockedStatic<JniGlobal> mockedStatic = mockStatic(JniGlobal.class)) {
      mockedStatic.when(() -> JniGlobal.nativeDestroyGlobal(VALID_HANDLE)).then(invocation -> null);

      final JniGlobal global = new JniGlobal(VALID_HANDLE);
      assertFalse(global.isClosed());

      global.close();

      assertTrue(global.isClosed());
      mockedStatic.verify(() -> JniGlobal.nativeDestroyGlobal(VALID_HANDLE));
    }
  }

  @Test
  void testCloseIsIdempotent() {
    try (MockedStatic<JniGlobal> mockedStatic = mockStatic(JniGlobal.class)) {
      mockedStatic.when(() -> JniGlobal.nativeDestroyGlobal(VALID_HANDLE)).then(invocation -> null);

      final JniGlobal global = new JniGlobal(VALID_HANDLE);

      global.close();
      global.close(); // Second close should be safe

      assertTrue(global.isClosed());
      // Should only call native destroy once
      mockedStatic.verify(() -> JniGlobal.nativeDestroyGlobal(VALID_HANDLE));
    }
  }

  @Test
  void testTryWithResources() {
    try (MockedStatic<JniGlobal> mockedStatic = mockStatic(JniGlobal.class)) {
      mockedStatic.when(() -> JniGlobal.nativeDestroyGlobal(VALID_HANDLE)).then(invocation -> null);

      assertDoesNotThrow(() -> {
        try (JniGlobal global = new JniGlobal(VALID_HANDLE)) {
          assertFalse(global.isClosed());
          assertThat(global.getNativeHandle()).isEqualTo(VALID_HANDLE);
        }
      });

      mockedStatic.verify(() -> JniGlobal.nativeDestroyGlobal(VALID_HANDLE));
    }
  }

  @Test
  void testToString() {
    final JniGlobal global = new JniGlobal(VALID_HANDLE);
    final String toString = global.toString();

    assertThat(toString).contains("Global");
    assertThat(toString).contains("handle=0x" + Long.toHexString(VALID_HANDLE));
    assertThat(toString).contains("closed=false");

    global.close();
    final String toStringAfterClose = global.toString();
    assertThat(toStringAfterClose).contains("closed=true");
  }

  @Test
  void testGetResourceType() {
    final JniGlobal global = new JniGlobal(VALID_HANDLE);
    assertThat(global.getResourceType()).isEqualTo("Global");
  }

  @Test
  void testExceptionHandling() {
    try (MockedStatic<JniGlobal> mockedStatic = mockStatic(JniGlobal.class)) {
      mockedStatic
          .when(() -> JniGlobal.nativeGetValue(anyLong()))
          .thenThrow(new RuntimeException("Native error"));

      final JniGlobal global = new JniGlobal(VALID_HANDLE);

      final RuntimeException exception =
          assertThrows(RuntimeException.class, global::getValue);

      assertThat(exception.getMessage()).contains("Unexpected error getting global value");
      assertThat(exception.getCause()).isNotNull();
      assertThat(exception.getCause().getMessage()).isEqualTo("Native error");
    }
  }

  @Test
  void testConcurrentAccess() {
    try (MockedStatic<JniGlobal> mockedStatic = mockStatic(JniGlobal.class)) {
      mockedStatic.when(() -> JniGlobal.nativeGetValueType(VALID_HANDLE)).thenReturn("i32");
      mockedStatic.when(() -> JniGlobal.nativeIsMutable(VALID_HANDLE)).thenReturn(true);

      final JniGlobal global = new JniGlobal(VALID_HANDLE);

      // Test concurrent access doesn't cause issues
      final Thread[] threads = new Thread[5];
      for (int i = 0; i < threads.length; i++) {
        threads[i] = new Thread(() -> {
          assertThat(global.getValueType()).isEqualTo("i32");
          assertTrue(global.isMutable());
        });
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
  void testAllValueTypes() {
    try (MockedStatic<JniGlobal> mockedStatic = mockStatic(JniGlobal.class)) {
      final String[] valueTypes = {"i32", "i64", "f32", "f64"};

      for (String valueType : valueTypes) {
        mockedStatic.when(() -> JniGlobal.nativeGetValueType(VALID_HANDLE)).thenReturn(valueType);

        final JniGlobal global = new JniGlobal(VALID_HANDLE);
        assertThat(global.getValueType()).isEqualTo(valueType);
      }
    }
  }

  @Test
  void testTypedGettersWithRuntimeExceptions() {
    try (MockedStatic<JniGlobal> mockedStatic = mockStatic(JniGlobal.class)) {
      mockedStatic
          .when(() -> JniGlobal.nativeGetIntValue(anyLong()))
          .thenThrow(new RuntimeException("Type mismatch"));

      final JniGlobal global = new JniGlobal(VALID_HANDLE);

      final RuntimeException exception =
          assertThrows(RuntimeException.class, global::getIntValue);

      assertThat(exception).isInstanceOf(RuntimeException.class);
      assertThat(exception.getCause().getMessage()).isEqualTo("Type mismatch");
    }
  }

  @Test
  void testTypedSettersWithRuntimeExceptions() {
    try (MockedStatic<JniGlobal> mockedStatic = mockStatic(JniGlobal.class)) {
      mockedStatic.when(() -> JniGlobal.nativeIsMutable(VALID_HANDLE)).thenReturn(true);
      mockedStatic
          .when(() -> JniGlobal.nativeSetIntValue(anyLong(), anyInt()))
          .thenThrow(new RuntimeException("Type mismatch"));

      final JniGlobal global = new JniGlobal(VALID_HANDLE);

      final RuntimeException exception =
          assertThrows(RuntimeException.class, () -> global.setIntValue(123));

      assertThat(exception.getMessage()).contains("Unexpected error setting global int value");
      assertThat(exception.getCause().getMessage()).isEqualTo("Type mismatch");
    }
  }
}