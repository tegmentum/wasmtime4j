package ai.tegmentum.wasmtime4j.jni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import ai.tegmentum.wasmtime4j.jni.exception.JniValidationException;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JniGlobal}.
 *
 * <p>These tests focus on the Java wrapper logic, parameter validation, and defensive programming.
 * The tests verify constructor behavior, resource management, and basic API functionality without
 * relying on actual native calls.
 *
 * <p>Note: Functional behavior with actual WebAssembly global operations is tested in integration tests.
 */
class JniGlobalTest {

  private static final long VALID_HANDLE = 0xFEDCBA98L;

  @Test
  void testConstructorWithValidHandle() {
    final JniGlobal global = new JniGlobal(VALID_HANDLE);

    assertThat(global.getResourceType()).isEqualTo("Global");
    assertFalse(global.isClosed());
    global.close();
  }

  @Test
  void testConstructorWithInvalidHandle() {
    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> new JniGlobal(0L));

    assertThat(exception.getMessage()).contains("nativeHandle");
    assertThat(exception.getMessage()).contains("invalid native handle");
  }

  @Test
  void testResourceManagement() {
    final JniGlobal global = new JniGlobal(VALID_HANDLE);
    assertFalse(global.isClosed());
    
    global.close();
    assertTrue(global.isClosed());
  }

  @Test
  void testCloseIsIdempotent() {
    final JniGlobal global = new JniGlobal(VALID_HANDLE);
    global.close();
    assertTrue(global.isClosed());

    // Second close should not throw
    global.close();
    assertTrue(global.isClosed());
  }

  @Test
  void testTryWithResources() {
    try (final JniGlobal global = new JniGlobal(VALID_HANDLE)) {
      assertFalse(global.isClosed());
    }
    // Global should be automatically closed after try block
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
    assertThrows(JniResourceException.class, () -> global.setValue(42));
    assertThrows(JniResourceException.class, () -> global.setIntValue(42));
    assertThrows(JniResourceException.class, () -> global.setLongValue(42L));
    assertThrows(JniResourceException.class, () -> global.setFloatValue(3.14f));
    assertThrows(JniResourceException.class, () -> global.setDoubleValue(3.14));
    assertThrows(JniResourceException.class, global::getNativeHandle);
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
}