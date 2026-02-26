package ai.tegmentum.wasmtime4j.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PoolConcurrencyLimitException} class.
 *
 * <p>PoolConcurrencyLimitException indicates the pooling allocator's concurrency limit was
 * exceeded.
 */
@DisplayName("PoolConcurrencyLimitException Tests")
class PoolConcurrencyLimitExceptionTest {

  @Test
  @DisplayName("should create with message")
  void shouldCreateWithMessage() {
    final PoolConcurrencyLimitException exception =
        new PoolConcurrencyLimitException("pool limit exceeded: 100/100 instances");
    assertEquals("pool limit exceeded: 100/100 instances", exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  @DisplayName("should create with message and cause")
  void shouldCreateWithMessageAndCause() {
    final RuntimeException cause = new RuntimeException("allocation failed");
    final PoolConcurrencyLimitException exception =
        new PoolConcurrencyLimitException("concurrency limit hit", cause);
    assertEquals("concurrency limit hit", exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

  @Test
  @DisplayName("should extend ResourceException")
  void shouldExtendResourceException() {
    final PoolConcurrencyLimitException exception = new PoolConcurrencyLimitException("test");
    assertTrue(exception instanceof ResourceException, "Should be a ResourceException");
    assertTrue(exception instanceof WasmException, "Should also be a WasmException");
  }

  @Test
  @DisplayName("should be serializable")
  void shouldBeSerializable() {
    final PoolConcurrencyLimitException exception = new PoolConcurrencyLimitException("test");
    assertNotNull(exception);
  }
}
