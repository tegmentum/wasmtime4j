package ai.tegmentum.wasmtime4j.jni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.jni.exception.JniValidationException;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JniMemory}.
 *
 * <p>These tests focus on the Java wrapper logic, parameter validation, and defensive programming.
 * The tests verify constructor behavior, resource management, and basic API functionality without
 * relying on actual native calls.
 *
 * <p>Note: Functional behavior with actual WebAssembly memory operations is tested in integration
 * tests.
 */
class JniMemoryTest {

  private static final long VALID_HANDLE = 0xABCDEF12L;

  @Test
  void testConstructorWithValidHandle() {
    final JniMemory memory = new JniMemory(VALID_HANDLE);

    assertThat(memory.getResourceType()).isEqualTo("Memory");
    assertFalse(memory.isClosed());
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testConstructorWithInvalidHandle() {
    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> new JniMemory(0L));

    assertThat(exception.getMessage()).contains("nativeHandle");
    assertThat(exception.getMessage()).contains("invalid native handle");
  }

  @Test
  void testResourceManagement() {
    final JniMemory memory = new JniMemory(VALID_HANDLE);
    assertFalse(memory.isClosed());

    // Note: This test only verifies the initial state since close() requires native methods
    // The closed state is tested in integration tests with actual native implementations
  }

  @Test
  void testGrowWithNegativePagesLong() {
    final JniMemory memory = new JniMemory(VALID_HANDLE);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> memory.grow(-1L));

    assertThat(exception.getMessage()).contains("pages");
    assertThat(exception.getMessage()).contains("must be non-negative");
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testGrowWithNegativePagesInt() {
    final JniMemory memory = new JniMemory(VALID_HANDLE);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> memory.grow(-1));

    assertThat(exception.getMessage()).contains("pages");
    assertThat(exception.getMessage()).contains("must be non-negative");
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testReadByteWithNegativeOffsetLong() {
    final JniMemory memory = new JniMemory(VALID_HANDLE);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> memory.readByte(-1L));

    assertThat(exception.getMessage()).contains("offset");
    assertThat(exception.getMessage()).contains("must be non-negative");
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testReadByteWithNegativeOffsetInt() {
    final JniMemory memory = new JniMemory(VALID_HANDLE);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> memory.readByte(-1));

    assertThat(exception.getMessage()).contains("offset");
    assertThat(exception.getMessage()).contains("must be non-negative");
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testWriteByteWithNegativeOffsetLong() {
    final JniMemory memory = new JniMemory(VALID_HANDLE);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> memory.writeByte(-1L, (byte) 0));

    assertThat(exception.getMessage()).contains("offset");
    assertThat(exception.getMessage()).contains("must be non-negative");
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testWriteByteWithNegativeOffsetInt() {
    final JniMemory memory = new JniMemory(VALID_HANDLE);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> memory.writeByte(-1, (byte) 0));

    assertThat(exception.getMessage()).contains("offset");
    assertThat(exception.getMessage()).contains("must be non-negative");
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testReadBytesWithNullBuffer() {
    final JniMemory memory = new JniMemory(VALID_HANDLE);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> memory.readBytes(0L, null));

    assertThat(exception.getMessage()).contains("buffer");
    assertThat(exception.getMessage()).contains("must not be null");
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testReadBytesWithNegativeOffset() {
    final JniMemory memory = new JniMemory(VALID_HANDLE);
    final byte[] buffer = new byte[10];

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> memory.readBytes(-1L, buffer));

    assertThat(exception.getMessage()).contains("offset");
    assertThat(exception.getMessage()).contains("must be non-negative");
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testWriteBytesWithNullBuffer() {
    final JniMemory memory = new JniMemory(VALID_HANDLE);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> memory.writeBytes(0L, null));

    assertThat(exception.getMessage()).contains("buffer");
    assertThat(exception.getMessage()).contains("must not be null");
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testWriteBytesWithNegativeOffset() {
    final JniMemory memory = new JniMemory(VALID_HANDLE);
    final byte[] buffer = new byte[10];

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> memory.writeBytes(-1L, buffer));

    assertThat(exception.getMessage()).contains("offset");
    assertThat(exception.getMessage()).contains("must be non-negative");
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testReadBytesWithDestinationNullCheck() {
    final JniMemory memory = new JniMemory(VALID_HANDLE);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> memory.readBytes(0, null, 0, 10));

    assertThat(exception.getMessage()).contains("dest");
    assertThat(exception.getMessage()).contains("must not be null");
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testWriteBytesWithSourceNullCheck() {
    final JniMemory memory = new JniMemory(VALID_HANDLE);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> memory.writeBytes(0, null, 0, 10));

    assertThat(exception.getMessage()).contains("src");
    assertThat(exception.getMessage()).contains("must not be null");
    // Note: Not calling close() in unit test since it requires native methods
  }

  @Test
  void testOperationsOnClosedMemory() {
    // Note: This test would need to actually close the memory to test closed state operations
    // Since close() requires native methods, this is covered in integration tests
    // This unit test verifies parameter validation only

    final JniMemory memory = new JniMemory(VALID_HANDLE);
    assertFalse(memory.isClosed());

    // Test that operations work on open memory (would call native methods in real implementation)
    // Integration tests will verify behavior on closed memory
  }

  @Test
  void testToString() {
    final JniMemory memory = new JniMemory(VALID_HANDLE);
    final String toString = memory.toString();

    assertThat(toString).contains("Memory");
    assertThat(toString).contains("handle=0x" + Long.toHexString(VALID_HANDLE));
    assertThat(toString).contains("closed=false");

    // Note: Testing closed=true state requires native methods and is covered in integration tests
  }
}
