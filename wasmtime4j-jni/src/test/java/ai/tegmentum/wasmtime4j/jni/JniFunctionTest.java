package ai.tegmentum.wasmtime4j.jni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockStatic;

import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import ai.tegmentum.wasmtime4j.jni.exception.JniValidationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@link JniFunction}.
 *
 * <p>These tests cover the complete WebAssembly type system support, multi-value operations, type
 * validation, function caching, and async execution functionality.
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
    assertThat(function.getCallCount()).isEqualTo(0);
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
  void testGetFunctionType() {
    try (MockedStatic<JniFunction> mockedStatic = mockStatic(JniFunction.class)) {
      final String[] paramTypes = {"i32", "f64"};
      final String[] returnTypes = {"i64"};

      mockedStatic
          .when(() -> JniFunction.nativeGetParameterTypes(VALID_HANDLE))
          .thenReturn(paramTypes);
      mockedStatic
          .when(() -> JniFunction.nativeGetReturnTypes(VALID_HANDLE))
          .thenReturn(returnTypes);

      final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
      final FunctionType functionType = function.getFunctionType();

      assertThat(functionType.getParamTypes())
          .containsExactly(WasmValueType.I32, WasmValueType.F64);
      assertThat(functionType.getReturnTypes()).containsExactly(WasmValueType.I64);

      // Test caching - second call should return same instance
      final FunctionType cachedType = function.getFunctionType();
      assertThat(cachedType).isSameAs(functionType);
    }
  }

  @Test
  void testGetFunctionTypeWithAdvancedTypes() {
    try (MockedStatic<JniFunction> mockedStatic = mockStatic(JniFunction.class)) {
      final String[] paramTypes = {"v128", "funcref", "externref"};
      final String[] returnTypes = {"v128", "i32"};

      mockedStatic
          .when(() -> JniFunction.nativeGetParameterTypes(VALID_HANDLE))
          .thenReturn(paramTypes);
      mockedStatic
          .when(() -> JniFunction.nativeGetReturnTypes(VALID_HANDLE))
          .thenReturn(returnTypes);

      final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
      final FunctionType functionType = function.getFunctionType();

      assertThat(functionType.getParamTypes())
          .containsExactly(WasmValueType.V128, WasmValueType.FUNCREF, WasmValueType.EXTERNREF);
      assertThat(functionType.getReturnTypes())
          .containsExactly(WasmValueType.V128, WasmValueType.I32);
    }
  }

  @Test
  void testGetFunctionTypeWithNativeError() {
    try (MockedStatic<JniFunction> mockedStatic = mockStatic(JniFunction.class)) {
      mockedStatic.when(() -> JniFunction.nativeGetParameterTypes(VALID_HANDLE)).thenReturn(null);

      final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);

      final WasmException exception = assertThrows(WasmException.class, function::getFunctionType);

      assertThat(exception.getMessage())
          .contains("Failed to retrieve function signature for '" + FUNCTION_NAME + "'");
    }
  }

  @Test
  void testCallWithBasicTypes() throws WasmException {
    try (MockedStatic<JniFunction> mockedStatic = mockStatic(JniFunction.class)) {
      final String[] paramTypes = {"i32", "f64"};
      final String[] returnTypes = {"i64"};
      final Object[] nativeResults = {42L};

      mockedStatic
          .when(() -> JniFunction.nativeGetParameterTypes(VALID_HANDLE))
          .thenReturn(paramTypes);
      mockedStatic
          .when(() -> JniFunction.nativeGetReturnTypes(VALID_HANDLE))
          .thenReturn(returnTypes);
      mockedStatic
          .when(() -> JniFunction.nativeCallMultiValue(anyLong(), any(Object[].class)))
          .thenReturn(nativeResults);

      final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
      final WasmValue[] params = {WasmValue.i32(10), WasmValue.f64(3.14)};
      final WasmValue[] results = function.call(params);

      assertThat(results).hasSize(1);
      assertThat(results[0].getType()).isEqualTo(WasmValueType.I64);
      assertThat(results[0].asLong()).isEqualTo(42L);
      assertThat(function.getCallCount()).isEqualTo(1);
    }
  }

  @Test
  void testCallWithV128Type() throws WasmException {
    try (MockedStatic<JniFunction> mockedStatic = mockStatic(JniFunction.class)) {
      final String[] paramTypes = {"v128"};
      final String[] returnTypes = {"v128"};
      final byte[] v128Input = new byte[16];
      final byte[] v128Output = new byte[16];
      for (int i = 0; i < 16; i++) {
        v128Input[i] = (byte) i;
        v128Output[i] = (byte) (i * 2);
      }
      final Object[] nativeResults = {v128Output};

      mockedStatic
          .when(() -> JniFunction.nativeGetParameterTypes(VALID_HANDLE))
          .thenReturn(paramTypes);
      mockedStatic
          .when(() -> JniFunction.nativeGetReturnTypes(VALID_HANDLE))
          .thenReturn(returnTypes);
      mockedStatic
          .when(() -> JniFunction.nativeCallMultiValue(anyLong(), any(Object[].class)))
          .thenReturn(nativeResults);

      final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
      final WasmValue[] params = {WasmValue.v128(v128Input)};
      final WasmValue[] results = function.call(params);

      assertThat(results).hasSize(1);
      assertThat(results[0].getType()).isEqualTo(WasmValueType.V128);
      assertThat(results[0].asV128()).isEqualTo(v128Output);
    }
  }

  @Test
  void testCallWithReferenceTypes() throws WasmException {
    try (MockedStatic<JniFunction> mockedStatic = mockStatic(JniFunction.class)) {
      final String[] paramTypes = {"funcref", "externref"};
      final String[] returnTypes = {"externref"};
      final Object funcRef = new Object(); // Mock function reference
      final Object externRef = "external_data";
      final Object returnRef = "returned_data";
      final Object[] nativeResults = {returnRef};

      mockedStatic
          .when(() -> JniFunction.nativeGetParameterTypes(VALID_HANDLE))
          .thenReturn(paramTypes);
      mockedStatic
          .when(() -> JniFunction.nativeGetReturnTypes(VALID_HANDLE))
          .thenReturn(returnTypes);
      mockedStatic
          .when(() -> JniFunction.nativeCallMultiValue(anyLong(), any(Object[].class)))
          .thenReturn(nativeResults);

      final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
      final WasmValue[] params = {WasmValue.funcref(funcRef), WasmValue.externref(externRef)};
      final WasmValue[] results = function.call(params);

      assertThat(results).hasSize(1);
      assertThat(results[0].getType()).isEqualTo(WasmValueType.EXTERNREF);
      assertThat(results[0].asExternref()).isEqualTo(returnRef);
    }
  }

  @Test
  void testCallWithMultipleReturnValues() throws WasmException {
    try (MockedStatic<JniFunction> mockedStatic = mockStatic(JniFunction.class)) {
      final String[] paramTypes = {"i32"};
      final String[] returnTypes = {"i32", "f64", "i64"};
      final Object[] nativeResults = {42, 3.14, 100L};

      mockedStatic
          .when(() -> JniFunction.nativeGetParameterTypes(VALID_HANDLE))
          .thenReturn(paramTypes);
      mockedStatic
          .when(() -> JniFunction.nativeGetReturnTypes(VALID_HANDLE))
          .thenReturn(returnTypes);
      mockedStatic
          .when(() -> JniFunction.nativeCallMultiValue(anyLong(), any(Object[].class)))
          .thenReturn(nativeResults);

      final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
      final WasmValue[] params = {WasmValue.i32(10)};
      final WasmValue[] results = function.call(params);

      assertThat(results).hasSize(3);
      assertThat(results[0].getType()).isEqualTo(WasmValueType.I32);
      assertThat(results[0].asInt()).isEqualTo(42);
      assertThat(results[1].getType()).isEqualTo(WasmValueType.F64);
      assertThat(results[1].asDouble()).isEqualTo(3.14);
      assertThat(results[2].getType()).isEqualTo(WasmValueType.I64);
      assertThat(results[2].asLong()).isEqualTo(100L);
    }
  }

  @Test
  void testCallWithNoParameters() throws WasmException {
    try (MockedStatic<JniFunction> mockedStatic = mockStatic(JniFunction.class)) {
      final String[] paramTypes = {};
      final String[] returnTypes = {"i32"};
      final Object[] nativeResults = {42};

      mockedStatic
          .when(() -> JniFunction.nativeGetParameterTypes(VALID_HANDLE))
          .thenReturn(paramTypes);
      mockedStatic
          .when(() -> JniFunction.nativeGetReturnTypes(VALID_HANDLE))
          .thenReturn(returnTypes);
      mockedStatic
          .when(() -> JniFunction.nativeCallMultiValue(anyLong(), any(Object[].class)))
          .thenReturn(nativeResults);

      final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
      final WasmValue[] results = function.call();

      assertThat(results).hasSize(1);
      assertThat(results[0].asInt()).isEqualTo(42);
    }
  }

  @Test
  void testCallWithParameterCountMismatch() {
    try (MockedStatic<JniFunction> mockedStatic = mockStatic(JniFunction.class)) {
      final String[] paramTypes = {"i32", "f64"};
      final String[] returnTypes = {"i32"};

      mockedStatic
          .when(() -> JniFunction.nativeGetParameterTypes(VALID_HANDLE))
          .thenReturn(paramTypes);
      mockedStatic
          .when(() -> JniFunction.nativeGetReturnTypes(VALID_HANDLE))
          .thenReturn(returnTypes);

      final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
      final WasmValue[] params = {WasmValue.i32(10)}; // Missing one parameter

      final WasmException exception =
          assertThrows(WasmException.class, () -> function.call(params));

      assertThat(exception.getMessage())
          .contains("Parameter validation failed")
          .contains(FUNCTION_NAME);
      assertThat(exception.getCause().getMessage()).contains("Parameter count mismatch");
    }
  }

  @Test
  void testCallWithTypeMismatch() {
    try (MockedStatic<JniFunction> mockedStatic = mockStatic(JniFunction.class)) {
      final String[] paramTypes = {"i32", "f64"};
      final String[] returnTypes = {"i32"};

      mockedStatic
          .when(() -> JniFunction.nativeGetParameterTypes(VALID_HANDLE))
          .thenReturn(paramTypes);
      mockedStatic
          .when(() -> JniFunction.nativeGetReturnTypes(VALID_HANDLE))
          .thenReturn(returnTypes);

      final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
      final WasmValue[] params = {
        WasmValue.i32(10), WasmValue.i32(20)
      }; // Wrong type for second param

      final WasmException exception =
          assertThrows(WasmException.class, () -> function.call(params));

      assertThat(exception.getMessage()).contains("Parameter validation failed");
      assertThat(exception.getCause().getMessage()).contains("Parameter type mismatch");
    }
  }

  @Test
  void testCallWithNullParameters() {
    final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> function.call((WasmValue[]) null));

    assertThat(exception.getMessage()).contains("params");
    assertThat(exception.getMessage()).contains("must not be null");
  }

  @Test
  void testCallWithNullParameterElement() {
    try (MockedStatic<JniFunction> mockedStatic = mockStatic(JniFunction.class)) {
      final String[] paramTypes = {"i32", "i32"};
      final String[] returnTypes = {"i32"};

      mockedStatic
          .when(() -> JniFunction.nativeGetParameterTypes(VALID_HANDLE))
          .thenReturn(paramTypes);
      mockedStatic
          .when(() -> JniFunction.nativeGetReturnTypes(VALID_HANDLE))
          .thenReturn(returnTypes);

      final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
      final WasmValue[] params = {WasmValue.i32(10), null};

      final WasmException exception =
          assertThrows(WasmException.class, () -> function.call(params));

      assertThat(exception.getMessage()).contains("Parameter validation failed");
      assertThat(exception.getCause().getMessage()).contains("Parameter at index 1 is null");
    }
  }

  @Test
  void testCallAsync() throws ExecutionException, InterruptedException {
    try (MockedStatic<JniFunction> mockedStatic = mockStatic(JniFunction.class)) {
      final String[] paramTypes = {"i32"};
      final String[] returnTypes = {"i64"};
      final Object[] nativeResults = {42L};

      mockedStatic
          .when(() -> JniFunction.nativeGetParameterTypes(VALID_HANDLE))
          .thenReturn(paramTypes);
      mockedStatic
          .when(() -> JniFunction.nativeGetReturnTypes(VALID_HANDLE))
          .thenReturn(returnTypes);
      mockedStatic
          .when(() -> JniFunction.nativeCallMultiValue(anyLong(), any(Object[].class)))
          .thenReturn(nativeResults);

      final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
      final WasmValue[] params = {WasmValue.i32(10)};

      final CompletableFuture<WasmValue[]> future = function.callAsync(params);
      final WasmValue[] results = future.get(5, TimeUnit.SECONDS);

      assertThat(results).hasSize(1);
      assertThat(results[0].asLong()).isEqualTo(42L);
    }
  }

  @Test
  void testCallAsyncWithException() {
    try (MockedStatic<JniFunction> mockedStatic = mockStatic(JniFunction.class)) {
      final String[] paramTypes = {"i32"};
      final String[] returnTypes = {"i32"};

      mockedStatic
          .when(() -> JniFunction.nativeGetParameterTypes(VALID_HANDLE))
          .thenReturn(paramTypes);
      mockedStatic
          .when(() -> JniFunction.nativeGetReturnTypes(VALID_HANDLE))
          .thenReturn(returnTypes);
      mockedStatic
          .when(() -> JniFunction.nativeCallMultiValue(anyLong(), any(Object[].class)))
          .thenThrow(new RuntimeException("Native error"));

      final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
      final WasmValue[] params = {WasmValue.i32(10)};

      final CompletableFuture<WasmValue[]> future = function.callAsync(params);

      final ExecutionException exception =
          assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));

      assertThat(exception.getCause()).isInstanceOf(RuntimeException.class);
    }
  }

  @Test
  void testResultCaching() throws WasmException {
    try (MockedStatic<JniFunction> mockedStatic = mockStatic(JniFunction.class)) {
      final String[] paramTypes = {"i32"};
      final String[] returnTypes = {"i32"};
      final Object[] nativeResults = {42};

      mockedStatic
          .when(() -> JniFunction.nativeGetParameterTypes(VALID_HANDLE))
          .thenReturn(paramTypes);
      mockedStatic
          .when(() -> JniFunction.nativeGetReturnTypes(VALID_HANDLE))
          .thenReturn(returnTypes);
      mockedStatic
          .when(() -> JniFunction.nativeCallMultiValue(anyLong(), any(Object[].class)))
          .thenReturn(nativeResults);

      final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
      final WasmValue[] params = {WasmValue.i32(10)};

      // Make multiple calls to trigger caching behavior
      for (int i = 0; i < 20; i++) {
        final WasmValue[] results = function.call(params);
        assertThat(results[0].asInt()).isEqualTo(42);
      }

      assertThat(function.getCallCount()).isEqualTo(20);
      // Cache hit ratio should be calculated (though specific value depends on caching logic)
      assertThat(function.getCacheHitRatio()).isGreaterThanOrEqualTo(0.0);
    }
  }

  @Test
  void testClearCache() throws WasmException {
    try (MockedStatic<JniFunction> mockedStatic = mockStatic(JniFunction.class)) {
      final String[] paramTypes = {"i32"};
      final String[] returnTypes = {"i32"};
      final Object[] nativeResults = {42};

      mockedStatic
          .when(() -> JniFunction.nativeGetParameterTypes(VALID_HANDLE))
          .thenReturn(paramTypes);
      mockedStatic
          .when(() -> JniFunction.nativeGetReturnTypes(VALID_HANDLE))
          .thenReturn(returnTypes);
      mockedStatic
          .when(() -> JniFunction.nativeCallMultiValue(anyLong(), any(Object[].class)))
          .thenReturn(nativeResults);

      final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
      final WasmValue[] params = {WasmValue.i32(10)};

      // Make some calls and then clear cache
      function.call(params);
      function.clearCache();

      // Should still work after clearing cache
      assertDoesNotThrow(() -> function.call(params));
    }
  }

  @Test
  void testOperationsOnClosedFunction() {
    final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
    function.close();

    assertThrows(JniResourceException.class, function::getFunctionType);
    assertThrows(JniResourceException.class, function::getParameterTypes);
    assertThrows(JniResourceException.class, function::getReturnTypes);
    assertThrows(JniResourceException.class, function::call);
    assertThrows(JniResourceException.class, () -> function.call(new WasmValue[0]));
    assertThrows(JniResourceException.class, () -> function.call(new Object[0]));
    assertThrows(JniResourceException.class, function::getNativeHandle);
  }

  @Test
  void testCloseWithCacheCleanup() {
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
  void testLegacyCallMethodsBackwardCompatibility() {
    try (MockedStatic<JniFunction> mockedStatic = mockStatic(JniFunction.class)) {
      mockedStatic.when(() -> JniFunction.nativeCall(VALID_HANDLE, new Object[0])).thenReturn(42);
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

      // Test legacy methods still work
      assertThat(function.call(new Object[0])).isEqualTo(42);
      assertThat(function.callInt()).isEqualTo(42);
      assertThat(function.callLong()).isEqualTo(42L);
      assertThat(function.callFloat()).isEqualTo(42.0f);
      assertThat(function.callDouble()).isEqualTo(42.0);
    }
  }

  @Test
  void testDeprecatedParameterAndReturnTypeMethods() {
    try (MockedStatic<JniFunction> mockedStatic = mockStatic(JniFunction.class)) {
      final String[] paramTypes = {"i32", "f64"};
      final String[] returnTypes = {"i64"};

      mockedStatic
          .when(() -> JniFunction.nativeGetParameterTypes(VALID_HANDLE))
          .thenReturn(paramTypes);
      mockedStatic
          .when(() -> JniFunction.nativeGetReturnTypes(VALID_HANDLE))
          .thenReturn(returnTypes);

      final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);

      // Test deprecated methods still work
      assertThat(function.getParameterTypes()).containsExactly("i32", "f64");
      assertThat(function.getReturnTypes()).containsExactly("i64");
    }
  }

  @Test
  void testGetName() {
    final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
    assertThat(function.getName()).isEqualTo(FUNCTION_NAME);
  }

  @Test
  void testGetResourceType() {
    final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
    assertThat(function.getResourceType()).isEqualTo("Function[" + FUNCTION_NAME + "]");
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
}
