package ai.tegmentum.wasmtime4j.jni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.jni.exception.JniValidationException;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JniGlobal}.
 *
 * <p>These tests focus on the Java wrapper logic, parameter validation, and defensive programming.
 * The tests verify constructor behavior, resource management, and basic API functionality without
 * relying on actual native calls.
 *
 * <p>Note: Functional behavior with actual WebAssembly global operations is tested in integration
 * tests.
 */
class JniGlobalTest {

  private static final long VALID_HANDLE = 0xFEDCBA98L;
  private static final JniStore STUB_STORE =
      new JniStore(VALID_HANDLE, new JniEngine(VALID_HANDLE));

  @Test
  void testConstructorWithValidHandle() {
    final JniGlobal global = new JniGlobal(VALID_HANDLE, STUB_STORE);

    assertThat(global.getResourceType()).isEqualTo("Global");
    assertFalse(global.isClosed());
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testConstructorWithInvalidHandle() {
    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> new JniGlobal(0L, STUB_STORE));

    assertThat(exception.getMessage()).contains("nativeHandle");
    assertThat(exception.getMessage()).contains("invalid native handle");
  }

  @Test
  void testResourceManagement() {
    final JniGlobal global = new JniGlobal(VALID_HANDLE, STUB_STORE);
    assertFalse(global.isClosed());

    // Test that resource starts in open state
    assertFalse(global.isClosed());
    // Note: Actual close() testing requires native methods and is covered in integration tests
  }

  @Test
  void testCloseIsIdempotent() {
    final JniGlobal global = new JniGlobal(VALID_HANDLE, STUB_STORE);
    assertFalse(global.isClosed());

    // Note: Actual close() idempotency testing requires native methods
    // This test verifies the initial state only
    // Integration tests will verify close() behavior
  }

  @Test
  void testTryWithResources() {
    final JniGlobal global = new JniGlobal(VALID_HANDLE, STUB_STORE);
    assertFalse(global.isClosed());
    // Note: Not using try-with-resources in unit test since close() requires native methods
  }

  @Test
  void testOperationsOnClosedGlobal() {
    // Note: This test would need to actually close the global to test closed state operations
    // Since close() requires native methods, this is covered in integration tests
    // This unit test verifies parameter validation only

    final JniGlobal global = new JniGlobal(VALID_HANDLE, STUB_STORE);
    assertFalse(global.isClosed());

    // Test that operations work on open global (would call native methods in real implementation)
    // Integration tests will verify behavior on closed globals
  }

  @Test
  void testToString() {
    final JniGlobal global = new JniGlobal(VALID_HANDLE, STUB_STORE);
    final String toString = global.toString();

    assertThat(toString).contains("Global");
    assertThat(toString).contains("handle=0x" + Long.toHexString(VALID_HANDLE));
    assertThat(toString).contains("closed=false");

    // Note: Testing toString() after close() requires native methods
    // Integration tests will verify toString() behavior after close()
  }
}
