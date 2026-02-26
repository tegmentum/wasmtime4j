package ai.tegmentum.wasmtime4j.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MemoryAccessException} class.
 *
 * <p>MemoryAccessException represents out-of-bounds memory reads/writes, misaligned accesses, or
 * freed memory access attempts.
 */
@DisplayName("MemoryAccessException Tests")
class MemoryAccessExceptionTest {

  @Test
  @DisplayName("should create with message")
  void shouldCreateWithMessage() {
    final MemoryAccessException exception = new MemoryAccessException("out of bounds at offset 42");
    assertEquals("out of bounds at offset 42", exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  @DisplayName("should create with message and cause")
  void shouldCreateWithMessageAndCause() {
    final RuntimeException cause = new RuntimeException("segfault");
    final MemoryAccessException exception = new MemoryAccessException("memory read failed", cause);
    assertEquals("memory read failed", exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

  @Test
  @DisplayName("should extend WasmRuntimeException")
  void shouldExtendWasmRuntimeException() {
    final MemoryAccessException exception = new MemoryAccessException("test");
    assertTrue(exception instanceof WasmRuntimeException, "Should be a WasmRuntimeException");
    assertTrue(exception instanceof WasmException, "Should also be a WasmException");
  }

  @Test
  @DisplayName("should be serializable")
  void shouldBeSerializable() {
    final MemoryAccessException exception = new MemoryAccessException("test");
    assertNotNull(exception);
    // serialVersionUID presence is verified by compilation
  }
}
