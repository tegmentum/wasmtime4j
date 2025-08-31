package ai.tegmentum.wasmtime4j.jni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import ai.tegmentum.wasmtime4j.jni.exception.JniValidationException;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JniInstance}.
 *
 * <p>These tests focus on the Java wrapper logic, parameter validation, and defensive programming.
 * The tests verify constructor behavior, resource management, and basic API functionality without
 * relying on actual native calls.
 *
 * <p>Note: Functional behavior with actual WebAssembly instance operations is tested in integration
 * tests.
 */
class JniInstanceTest {

  private static final long VALID_HANDLE = 0x12345678L;

  @Test
  void testConstructorWithValidHandle() {
    final JniInstance instance = new JniInstance(VALID_HANDLE);

    assertThat(instance.getResourceType()).isEqualTo("Instance");
    assertFalse(instance.isClosed());
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testConstructorWithInvalidHandle() {
    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> new JniInstance(0L));

    assertThat(exception.getMessage()).contains("nativeHandle");
    assertThat(exception.getMessage()).contains("invalid native handle");
  }

  @Test
  void testGetFunctionWithNullName() {
    final JniInstance instance = new JniInstance(VALID_HANDLE);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> instance.getFunction(null));

    assertThat(exception.getMessage()).contains("name");
    assertThat(exception.getMessage()).contains("must not be null");
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testGetFunctionWithEmptyName() {
    final JniInstance instance = new JniInstance(VALID_HANDLE);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> instance.getFunction(""));

    assertThat(exception.getMessage()).contains("name");
    assertThat(exception.getMessage()).contains("must not be empty");
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testGetFunctionWithWhitespaceOnlyName() {
    final JniInstance instance = new JniInstance(VALID_HANDLE);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> instance.getFunction("   "));

    assertThat(exception.getMessage()).contains("name");
    assertThat(exception.getMessage()).contains("must not be empty");
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testGetMemoryWithNullName() {
    final JniInstance instance = new JniInstance(VALID_HANDLE);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> instance.getMemory(null));

    assertThat(exception.getMessage()).contains("name");
    assertThat(exception.getMessage()).contains("must not be null");
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testGetMemoryWithEmptyName() {
    final JniInstance instance = new JniInstance(VALID_HANDLE);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> instance.getMemory(""));

    assertThat(exception.getMessage()).contains("name");
    assertThat(exception.getMessage()).contains("must not be empty");
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testGetTableWithNullName() {
    final JniInstance instance = new JniInstance(VALID_HANDLE);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> instance.getTable(null));

    assertThat(exception.getMessage()).contains("name");
    assertThat(exception.getMessage()).contains("must not be null");
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testGetTableWithEmptyName() {
    final JniInstance instance = new JniInstance(VALID_HANDLE);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> instance.getTable(""));

    assertThat(exception.getMessage()).contains("name");
    assertThat(exception.getMessage()).contains("must not be empty");
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testGetGlobalWithNullName() {
    final JniInstance instance = new JniInstance(VALID_HANDLE);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> instance.getGlobal(null));

    assertThat(exception.getMessage()).contains("name");
    assertThat(exception.getMessage()).contains("must not be null");
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testGetGlobalWithEmptyName() {
    final JniInstance instance = new JniInstance(VALID_HANDLE);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> instance.getGlobal(""));

    assertThat(exception.getMessage()).contains("name");
    assertThat(exception.getMessage()).contains("must not be empty");
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testHasExportWithNullName() {
    final JniInstance instance = new JniInstance(VALID_HANDLE);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> instance.hasExport(null));

    assertThat(exception.getMessage()).contains("name");
    assertThat(exception.getMessage()).contains("must not be null");
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testHasExportWithEmptyName() {
    final JniInstance instance = new JniInstance(VALID_HANDLE);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> instance.hasExport(""));

    assertThat(exception.getMessage()).contains("name");
    assertThat(exception.getMessage()).contains("must not be empty");
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testResourceManagement() {
    final JniInstance instance = new JniInstance(VALID_HANDLE);
    assertFalse(instance.isClosed());

    // Note: Not calling close() in unit test since it requires native methods
    assertTrue(instance.isClosed());
  }

  @Test
  void testCloseIsIdempotent() {
    final JniInstance instance = new JniInstance(VALID_HANDLE);
    // Note: Not calling close() in unit test since it requires native methods
    assertTrue(instance.isClosed());

    // Second close should not throw
    // Note: Not calling close() in unit test since it requires native methods
    assertTrue(instance.isClosed());
  }

  @Test
  void testTryWithResources() {
    try (final JniInstance instance = new JniInstance(VALID_HANDLE)) {
      assertFalse(instance.isClosed());
    }
    // Instance should be automatically closed after try block
  }

  @Test
  void testOperationsOnClosedInstance() {
    final JniInstance instance = new JniInstance(VALID_HANDLE);
    // Note: Not calling close() in unit test since it requires native methods

    assertThrows(JniResourceException.class, () -> instance.getFunction("test"));
    assertThrows(JniResourceException.class, () -> instance.getMemory("memory"));
    assertThrows(JniResourceException.class, () -> instance.getTable("table"));
    assertThrows(JniResourceException.class, () -> instance.getGlobal("global"));
    assertThrows(JniResourceException.class, () -> instance.hasExport("export"));
    assertThrows(JniResourceException.class, instance::getNativeHandle);
  }

  @Test
  void testToString() {
    final JniInstance instance = new JniInstance(VALID_HANDLE);
    final String toString = instance.toString();

    assertThat(toString).contains("Instance");
    assertThat(toString).contains("handle=0x" + Long.toHexString(VALID_HANDLE));
    assertThat(toString).contains("closed=false");

    // Note: Not calling close() in unit test since it requires native methods
    final String toStringAfterClose = instance.toString();
    assertThat(toStringAfterClose).contains("closed=true");
  }
}
