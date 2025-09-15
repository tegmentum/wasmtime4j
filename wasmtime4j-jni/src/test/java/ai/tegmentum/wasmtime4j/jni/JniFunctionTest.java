package ai.tegmentum.wasmtime4j.jni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.WasmValue;
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

    assertThat(exception.getMessage()).contains("parameters");
    assertThat(exception.getMessage()).contains("must not be null");
  }

  @Test
  void testCallWithObjectNullParameters() {
    final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> function.call((WasmValue[]) null));

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

    // Test that resource starts in open state
    assertFalse(function.isClosed());
    // Note: Actual close() testing requires native methods and is covered in integration tests
  }

  @Test
  void testOperationsOnClosedFunction() {
    // Note: This test would need to actually close the function to test closed state operations
    // Since close() requires native methods, this is covered in integration tests
    // This unit test verifies parameter validation only

    final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
    assertFalse(function.isClosed());

    // Test that operations work on open function (would call native methods in real implementation)
    // Integration tests will verify behavior on closed functions
  }

  @Test
  void testToString() {
    final JniFunction function = new JniFunction(VALID_HANDLE, FUNCTION_NAME);
    final String toString = function.toString();

    assertThat(toString).contains("Function[" + FUNCTION_NAME + "]");
    assertThat(toString).contains("handle=0x" + Long.toHexString(VALID_HANDLE));
    assertThat(toString).contains("closed=false");

    // Note: Testing toString() after close() requires native methods
    // Integration tests will verify toString() behavior after close()
  }
}
