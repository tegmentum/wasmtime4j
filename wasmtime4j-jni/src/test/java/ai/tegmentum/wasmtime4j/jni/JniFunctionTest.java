package ai.tegmentum.wasmtime4j.jni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import ai.tegmentum.wasmtime4j.jni.exception.JniValidationException;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JniFunction}.
 *
 * <p>These tests focus on the Java wrapper logic, parameter validation, and defensive programming.
 * The tests verify constructor behavior, resource management, and basic API functionality without
 * relying on actual native calls.
 *
 * <p>Note: Functional behavior with actual WebAssembly execution is tested in integration tests.
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
  void testGetCallCount() {
    final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
    assertThat(function.getCallCount()).isEqualTo(0);
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
  void testCallWithObjectNullParameters() {
    final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> function.call((Object[]) null));

    assertThat(exception.getMessage()).contains("parameters");
    assertThat(exception.getMessage()).contains("must not be null");
  }

  @Test
  void testClearCache() {
    final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
    
    // Should not throw any exception
    assertDoesNotThrow(function::clearCache);
  }

  @Test
  void testGetCacheHitRatio() {
    final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
    
    // Should return initial cache hit ratio (0.0)
    assertThat(function.getCacheHitRatio()).isEqualTo(0.0);
  }

  @Test
  void testResourceManagement() {
    final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
    assertFalse(function.isClosed());
    
    function.close();
    assertTrue(function.isClosed());
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
