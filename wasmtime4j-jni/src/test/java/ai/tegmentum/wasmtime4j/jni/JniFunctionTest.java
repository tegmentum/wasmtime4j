package ai.tegmentum.wasmtime4j.jni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockStatic;

import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import ai.tegmentum.wasmtime4j.jni.exception.JniValidationException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@link JniFunction}.
 *
 * <p>Note: These tests focus on the Java wrapper logic and defensive programming. Native method
 * behavior is tested separately in integration tests.
 */
class JniFunctionTest {

  private static final long VALID_HANDLE = 0x87654321L;
  private static final String FUNCTION_NAME = "test_function";

  @Test
  void testConstructorWithValidParameters() {
    final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);

    assertThat(function.getNativeHandle()).isEqualTo(VALID_HANDLE);
    assertThat(function.getName()).isEqualTo(FUNCTION_NAME);
    assertThat(function.getResourceType()).isEqualTo("Function[" + FUNCTION_NAME + "]");
    assertFalse(function.isClosed());
  }

  @Test
  void testConstructorWithInvalidHandle() {
    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> new JniFunction(0L, FUNCTION_NAME));

    assertThat(exception.getMessage()).contains("nativeHandle");
    assertThat(exception.getMessage()).contains("invalid native handle");
  }

  @Test
  void testConstructorWithNullName() {
    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> new JniFunction(VALID_HANDLE, null));

    assertThat(exception.getMessage()).contains("name");
    assertThat(exception.getMessage()).contains("must not be null");
  }

  @Test
  void testGetName() {
    final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
    assertThat(function.getName()).isEqualTo(FUNCTION_NAME);
  }

  @Test
  void testGetParameterTypes() {
    try (MockedStatic<JniFunction> mockedStatic = mockStatic(JniFunction.class)) {
      final String[] expectedTypes = {"i32", "f64"};
      mockedStatic
          .when(() -> JniFunction.nativeGetParameterTypes(VALID_HANDLE))
          .thenReturn(expectedTypes);

      final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
      final String[] parameterTypes = function.getParameterTypes();

      assertThat(parameterTypes).containsExactly("i32", "f64");
    }
  }

  @Test
  void testGetParameterTypesReturnsEmptyWhenNull() {
    try (MockedStatic<JniFunction> mockedStatic = mockStatic(JniFunction.class)) {
      mockedStatic.when(() -> JniFunction.nativeGetParameterTypes(VALID_HANDLE)).thenReturn(null);

      final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
      final String[] parameterTypes = function.getParameterTypes();

      assertThat(parameterTypes).isEmpty();
    }
  }

  @Test
  void testGetReturnTypes() {
    try (MockedStatic<JniFunction> mockedStatic = mockStatic(JniFunction.class)) {
      final String[] expectedTypes = {"i32"};
      mockedStatic
          .when(() -> JniFunction.nativeGetReturnTypes(VALID_HANDLE))
          .thenReturn(expectedTypes);

      final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
      final String[] returnTypes = function.getReturnTypes();

      assertThat(returnTypes).containsExactly("i32");
    }
  }

  @Test
  void testGetReturnTypesReturnsEmptyWhenNull() {
    try (MockedStatic<JniFunction> mockedStatic = mockStatic(JniFunction.class)) {
      mockedStatic.when(() -> JniFunction.nativeGetReturnTypes(VALID_HANDLE)).thenReturn(null);

      final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
      final String[] returnTypes = function.getReturnTypes();

      assertThat(returnTypes).isEmpty();
    }
  }

  @Test
  void testCallWithNoParameters() {
    try (MockedStatic<JniFunction> mockedStatic = mockStatic(JniFunction.class)) {
      mockedStatic.when(() -> JniFunction.nativeCall(VALID_HANDLE, new Object[0])).thenReturn(42);

      final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
      final Object result = function.call();

      assertThat(result).isEqualTo(42);
    }
  }

  @Test
  void testCallWithParameters() {
    try (MockedStatic<JniFunction> mockedStatic = mockStatic(JniFunction.class)) {
      final Object[] params = {10, "test"};
      mockedStatic.when(() -> JniFunction.nativeCall(VALID_HANDLE, params)).thenReturn("result");

      final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
      final Object result = function.call(params);

      assertThat(result).isEqualTo("result");
    }
  }

  @Test
  void testCallWithNullParameters() {
    final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> function.call((Object[]) null));

    assertThat(exception.getMessage()).contains("parameters");
    assertThat(exception.getMessage()).contains("must not be null");
  }

  @Test
  void testCallInt() {
    try (MockedStatic<JniFunction> mockedStatic = mockStatic(JniFunction.class)) {
      final int[] params = {10, 20};
      mockedStatic.when(() -> JniFunction.nativeCallInt(VALID_HANDLE, params)).thenReturn(30);

      final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
      final int result = function.callInt(params);

      assertThat(result).isEqualTo(30);
    }
  }

  @Test
  void testCallIntWithNullParameters() {
    final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> function.callInt((int[]) null));

    assertThat(exception.getMessage()).contains("parameters");
    assertThat(exception.getMessage()).contains("must not be null");
  }

  @Test
  void testCallLong() {
    try (MockedStatic<JniFunction> mockedStatic = mockStatic(JniFunction.class)) {
      final long[] params = {100L, 200L};
      mockedStatic.when(() -> JniFunction.nativeCallLong(VALID_HANDLE, params)).thenReturn(300L);

      final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
      final long result = function.callLong(params);

      assertThat(result).isEqualTo(300L);
    }
  }

  @Test
  void testCallLongWithNullParameters() {
    final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> function.callLong((long[]) null));

    assertThat(exception.getMessage()).contains("parameters");
    assertThat(exception.getMessage()).contains("must not be null");
  }

  @Test
  void testCallFloat() {
    try (MockedStatic<JniFunction> mockedStatic = mockStatic(JniFunction.class)) {
      final float[] params = {1.5f, 2.5f};
      mockedStatic.when(() -> JniFunction.nativeCallFloat(VALID_HANDLE, params)).thenReturn(4.0f);

      final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
      final float result = function.callFloat(params);

      assertThat(result).isEqualTo(4.0f);
    }
  }

  @Test
  void testCallFloatWithNullParameters() {
    final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> function.callFloat((float[]) null));

    assertThat(exception.getMessage()).contains("parameters");
    assertThat(exception.getMessage()).contains("must not be null");
  }

  @Test
  void testCallDouble() {
    try (MockedStatic<JniFunction> mockedStatic = mockStatic(JniFunction.class)) {
      final double[] params = {1.5, 2.5};
      mockedStatic.when(() -> JniFunction.nativeCallDouble(VALID_HANDLE, params)).thenReturn(4.0);

      final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
      final double result = function.callDouble(params);

      assertThat(result).isEqualTo(4.0);
    }
  }

  @Test
  void testCallDoubleWithNullParameters() {
    final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> function.callDouble((double[]) null));

    assertThat(exception.getMessage()).contains("parameters");
    assertThat(exception.getMessage()).contains("must not be null");
  }

  @Test
  void testOperationsOnClosedFunction() {
    final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
    function.close();

    assertThrows(JniResourceException.class, function::getParameterTypes);
    assertThrows(JniResourceException.class, function::getReturnTypes);
    assertThrows(JniResourceException.class, function::call);
    assertThrows(JniResourceException.class, () -> function.call(new Object[0]));
    assertThrows(JniResourceException.class, () -> function.callInt(new int[0]));
    assertThrows(JniResourceException.class, () -> function.callLong(new long[0]));
    assertThrows(JniResourceException.class, () -> function.callFloat(new float[0]));
    assertThrows(JniResourceException.class, () -> function.callDouble(new double[0]));
    assertThrows(JniResourceException.class, function::getNativeHandle);
  }

  @Test
  void testClose() {
    try (MockedStatic<JniFunction> mockedStatic = mockStatic(JniFunction.class)) {
      mockedStatic
          .when(() -> JniFunction.nativeDestroyFunction(VALID_HANDLE))
          .then(invocation -> null);

      final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
      assertFalse(function.isClosed());

      function.close();

      assertTrue(function.isClosed());
      mockedStatic.verify(() -> JniFunction.nativeDestroyFunction(VALID_HANDLE));
    }
  }

  @Test
  void testCloseIsIdempotent() {
    try (MockedStatic<JniFunction> mockedStatic = mockStatic(JniFunction.class)) {
      mockedStatic
          .when(() -> JniFunction.nativeDestroyFunction(VALID_HANDLE))
          .then(invocation -> null);

      final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);

      function.close();
      function.close(); // Second close should be safe

      assertTrue(function.isClosed());
      // Should only call native destroy once
      mockedStatic.verify(() -> JniFunction.nativeDestroyFunction(VALID_HANDLE));
    }
  }

  @Test
  void testTryWithResources() {
    try (MockedStatic<JniFunction> mockedStatic = mockStatic(JniFunction.class)) {
      mockedStatic
          .when(() -> JniFunction.nativeDestroyFunction(VALID_HANDLE))
          .then(invocation -> null);

      assertDoesNotThrow(
          () -> {
            try (JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME)) {
              assertFalse(function.isClosed());
              assertThat(function.getNativeHandle()).isEqualTo(VALID_HANDLE);
              assertThat(function.getName()).isEqualTo(FUNCTION_NAME);
            }
          });

      mockedStatic.verify(() -> JniFunction.nativeDestroyFunction(VALID_HANDLE));
    }
  }

  @Test
  void testToString() {
    final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
    final String toString = function.toString();

    assertThat(toString).contains("Function[" + FUNCTION_NAME + "]");
    assertThat(toString).contains("handle=0x" + Long.toHexString(VALID_HANDLE));
    assertThat(toString).contains("closed=false");

    function.close();
    final String toStringAfterClose = function.toString();
    assertThat(toStringAfterClose).contains("closed=true");
  }

  @Test
  void testGetResourceType() {
    final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
    assertThat(function.getResourceType()).isEqualTo("Function[" + FUNCTION_NAME + "]");
  }

  @Test
  void testResourceTypeWithDifferentNames() {
    final JniFunction function1 = new JniFunction(VALID_HANDLE, "add");
    final JniFunction function2 = new JniFunction(VALID_HANDLE, "multiply");

    assertThat(function1.getResourceType()).isEqualTo("Function[add]");
    assertThat(function2.getResourceType()).isEqualTo("Function[multiply]");
  }

  @Test
  void testExceptionHandling() {
    try (MockedStatic<JniFunction> mockedStatic = mockStatic(JniFunction.class)) {
      mockedStatic
          .when(() -> JniFunction.nativeCall(anyLong(), any(Object[].class)))
          .thenThrow(new RuntimeException("Native error"));

      final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);

      final RuntimeException exception = assertThrows(RuntimeException.class, function::call);

      assertThat(exception.getMessage())
          .contains("Unexpected error calling function '" + FUNCTION_NAME + "'");
      assertThat(exception.getCause()).isNotNull();
      assertThat(exception.getCause().getMessage()).isEqualTo("Native error");
    }
  }

  @Test
  void testConcurrentAccess() {
    try (MockedStatic<JniFunction> mockedStatic = mockStatic(JniFunction.class)) {
      final String[] paramTypes = {"i32", "i32"};
      mockedStatic
          .when(() -> JniFunction.nativeGetParameterTypes(VALID_HANDLE))
          .thenReturn(paramTypes);

      final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);

      // Test concurrent access doesn't cause issues
      final Thread[] threads = new Thread[5];
      for (int i = 0; i < threads.length; i++) {
        threads[i] =
            new Thread(
                () -> assertThat(function.getParameterTypes()).containsExactly("i32", "i32"));
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
  void testOptimizedCallMethods() {
    try (MockedStatic<JniFunction> mockedStatic = mockStatic(JniFunction.class)) {
      // Test all optimized call variants with empty arrays
      mockedStatic.when(() -> JniFunction.nativeCallInt(VALID_HANDLE, new int[0])).thenReturn(42);
      mockedStatic
          .when(() -> JniFunction.nativeCallLong(VALID_HANDLE, new long[0]))
          .thenReturn(42L);
      mockedStatic
          .when(() -> JniFunction.nativeCallFloat(VALID_HANDLE, new float[0]))
          .thenReturn(42.0f);
      mockedStatic
          .when(() -> JniFunction.nativeCallDouble(VALID_HANDLE, new double[0]))
          .thenReturn(42.0);

      final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);

      assertThat(function.callInt()).isEqualTo(42);
      assertThat(function.callLong()).isEqualTo(42L);
      assertThat(function.callFloat()).isEqualTo(42.0f);
      assertThat(function.callDouble()).isEqualTo(42.0);
    }
  }

  @Test
  void testExceptionPropagationInOptimizedCalls() {
    try (MockedStatic<JniFunction> mockedStatic = mockStatic(JniFunction.class)) {
      mockedStatic
          .when(() -> JniFunction.nativeCallInt(anyLong(), any(int[].class)))
          .thenThrow(new RuntimeException("Native int call error"));

      final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);

      final RuntimeException exception =
          assertThrows(RuntimeException.class, () -> function.callInt(new int[] {1, 2}));

      assertThat(exception.getMessage())
          .contains("Unexpected error calling function '" + FUNCTION_NAME + "'");
      assertThat(exception.getCause().getMessage()).isEqualTo("Native int call error");
    }
  }
}
