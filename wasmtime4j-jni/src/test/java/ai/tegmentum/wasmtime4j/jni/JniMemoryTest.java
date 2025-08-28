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
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@link JniMemory}.
 *
 * <p>Note: These tests focus on the Java wrapper logic and defensive programming. Native method
 * behavior is tested separately in integration tests.
 */
class JniMemoryTest {

  private static final long VALID_HANDLE = 0xABCDEF12L;
  private static final long MEMORY_SIZE = 65536L; // 64KB (1 page)

  @Test
  void testConstructorWithValidHandle() {
    final JniMemory memory = new JniMemory(VALID_HANDLE);

    assertThat(memory.getNativeHandle()).isEqualTo(VALID_HANDLE);
    assertThat(memory.getResourceType()).isEqualTo("Memory");
    assertFalse(memory.isClosed());
  }

  @Test
  void testConstructorWithInvalidHandle() {
    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> new JniMemory(0L));

    assertThat(exception.getMessage()).contains("nativeHandle");
    assertThat(exception.getMessage()).contains("invalid native handle");
  }

  @Test
  void testSize() {
    try (MockedStatic<JniMemory> mockedStatic = mockStatic(JniMemory.class)) {
      mockedStatic.when(() -> JniMemory.nativeGetSize(VALID_HANDLE)).thenReturn(MEMORY_SIZE);

      final JniMemory memory = new JniMemory(VALID_HANDLE);
      final long size = memory.size();

      assertThat(size).isEqualTo(MEMORY_SIZE);
    }
  }

  @Test
  void testSizeInPages() {
    try (MockedStatic<JniMemory> mockedStatic = mockStatic(JniMemory.class)) {
      mockedStatic.when(() -> JniMemory.nativeGetSize(VALID_HANDLE)).thenReturn(MEMORY_SIZE);

      final JniMemory memory = new JniMemory(VALID_HANDLE);
      final long pages = memory.sizeInPages();

      assertThat(pages).isEqualTo(1L); // 65536 bytes = 1 page
    }
  }

  @Test
  void testGrowWithValidPages() {
    try (MockedStatic<JniMemory> mockedStatic = mockStatic(JniMemory.class)) {
      mockedStatic.when(() -> JniMemory.nativeGrow(VALID_HANDLE, 2L)).thenReturn(1L);

      final JniMemory memory = new JniMemory(VALID_HANDLE);
      final long previousPages = memory.grow(2L);

      assertThat(previousPages).isEqualTo(1L);
    }
  }

  @Test
  void testGrowWithNegativePages() {
    final JniMemory memory = new JniMemory(VALID_HANDLE);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> memory.grow(-1L));

    assertThat(exception.getMessage()).contains("pages");
    assertThat(exception.getMessage()).contains("must be non-negative");
  }

  @Test
  void testReadByteWithValidOffset() {
    try (MockedStatic<JniMemory> mockedStatic = mockStatic(JniMemory.class)) {
      mockedStatic.when(() -> JniMemory.nativeGetSize(VALID_HANDLE)).thenReturn(MEMORY_SIZE);
      mockedStatic.when(() -> JniMemory.nativeReadByte(VALID_HANDLE, 100L)).thenReturn((byte) 42);

      final JniMemory memory = new JniMemory(VALID_HANDLE);
      final byte value = memory.readByte(100L);

      assertThat(value).isEqualTo((byte) 42);
    }
  }

  @Test
  void testReadByteWithNegativeOffset() {
    final JniMemory memory = new JniMemory(VALID_HANDLE);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> memory.readByte(-1L));

    assertThat(exception.getMessage()).contains("offset");
    assertThat(exception.getMessage()).contains("must be non-negative");
  }

  @Test
  void testReadByteWithOutOfBoundsOffset() {
    try (MockedStatic<JniMemory> mockedStatic = mockStatic(JniMemory.class)) {
      mockedStatic.when(() -> JniMemory.nativeGetSize(VALID_HANDLE)).thenReturn(MEMORY_SIZE);

      final JniMemory memory = new JniMemory(VALID_HANDLE);

      final IndexOutOfBoundsException exception =
          assertThrows(IndexOutOfBoundsException.class, () -> memory.readByte(MEMORY_SIZE));

      assertThat(exception.getMessage()).contains("exceeds memory size");
    }
  }

  @Test
  void testWriteByteWithValidOffset() {
    try (MockedStatic<JniMemory> mockedStatic = mockStatic(JniMemory.class)) {
      mockedStatic.when(() -> JniMemory.nativeGetSize(VALID_HANDLE)).thenReturn(MEMORY_SIZE);
      mockedStatic
          .when(() -> JniMemory.nativeWriteByte(VALID_HANDLE, 100L, (byte) 42))
          .then(invocation -> null);

      final JniMemory memory = new JniMemory(VALID_HANDLE);

      assertDoesNotThrow(() -> memory.writeByte(100L, (byte) 42));
    }
  }

  @Test
  void testWriteByteWithNegativeOffset() {
    final JniMemory memory = new JniMemory(VALID_HANDLE);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> memory.writeByte(-1L, (byte) 0));

    assertThat(exception.getMessage()).contains("offset");
    assertThat(exception.getMessage()).contains("must be non-negative");
  }

  @Test
  void testReadBytesWithValidParameters() {
    try (MockedStatic<JniMemory> mockedStatic = mockStatic(JniMemory.class)) {
      mockedStatic.when(() -> JniMemory.nativeGetSize(VALID_HANDLE)).thenReturn(MEMORY_SIZE);
      mockedStatic
          .when(() -> JniMemory.nativeReadBytes(anyLong(), anyLong(), any(byte[].class)))
          .thenReturn(10);

      final JniMemory memory = new JniMemory(VALID_HANDLE);
      final byte[] buffer = new byte[10];
      final int bytesRead = memory.readBytes(100L, buffer);

      assertThat(bytesRead).isEqualTo(10);
    }
  }

  @Test
  void testReadBytesWithNullBuffer() {
    final JniMemory memory = new JniMemory(VALID_HANDLE);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> memory.readBytes(0L, null));

    assertThat(exception.getMessage()).contains("buffer");
    assertThat(exception.getMessage()).contains("must not be null");
  }

  @Test
  void testReadBytesWithOutOfBounds() {
    try (MockedStatic<JniMemory> mockedStatic = mockStatic(JniMemory.class)) {
      mockedStatic.when(() -> JniMemory.nativeGetSize(VALID_HANDLE)).thenReturn(MEMORY_SIZE);

      final JniMemory memory = new JniMemory(VALID_HANDLE);
      final byte[] buffer = new byte[100];

      final IndexOutOfBoundsException exception =
          assertThrows(
              IndexOutOfBoundsException.class, () -> memory.readBytes(MEMORY_SIZE - 50, buffer));

      assertThat(exception.getMessage()).contains("exceeds memory size");
    }
  }

  @Test
  void testWriteBytesWithValidParameters() {
    try (MockedStatic<JniMemory> mockedStatic = mockStatic(JniMemory.class)) {
      mockedStatic.when(() -> JniMemory.nativeGetSize(VALID_HANDLE)).thenReturn(MEMORY_SIZE);
      mockedStatic
          .when(() -> JniMemory.nativeWriteBytes(anyLong(), anyLong(), any(byte[].class)))
          .thenReturn(10);

      final JniMemory memory = new JniMemory(VALID_HANDLE);
      final byte[] buffer = new byte[10];
      final int bytesWritten = memory.writeBytes(100L, buffer);

      assertThat(bytesWritten).isEqualTo(10);
    }
  }

  @Test
  void testWriteBytesWithNullBuffer() {
    final JniMemory memory = new JniMemory(VALID_HANDLE);

    final JniValidationException exception =
        assertThrows(JniValidationException.class, () -> memory.writeBytes(0L, null));

    assertThat(exception.getMessage()).contains("buffer");
    assertThat(exception.getMessage()).contains("must not be null");
  }

  @Test
  void testGetBuffer() {
    try (MockedStatic<JniMemory> mockedStatic = mockStatic(JniMemory.class)) {
      final ByteBuffer expectedBuffer = ByteBuffer.allocateDirect(1024);
      mockedStatic.when(() -> JniMemory.nativeGetBuffer(VALID_HANDLE)).thenReturn(expectedBuffer);

      final JniMemory memory = new JniMemory(VALID_HANDLE);
      final ByteBuffer buffer = memory.getBuffer();

      assertThat(buffer).isSameAs(expectedBuffer);
    }
  }

  @Test
  void testOperationsOnClosedMemory() {
    final JniMemory memory = new JniMemory(VALID_HANDLE);
    memory.close();

    assertThrows(JniResourceException.class, memory::size);
    assertThrows(JniResourceException.class, memory::sizeInPages);
    assertThrows(JniResourceException.class, () -> memory.grow(1L));
    assertThrows(JniResourceException.class, () -> memory.readByte(0L));
    assertThrows(JniResourceException.class, () -> memory.writeByte(0L, (byte) 0));
    assertThrows(JniResourceException.class, () -> memory.readBytes(0L, new byte[10]));
    assertThrows(JniResourceException.class, () -> memory.writeBytes(0L, new byte[10]));
    assertThrows(JniResourceException.class, memory::getBuffer);
    assertThrows(JniResourceException.class, memory::getNativeHandle);
  }

  @Test
  void testClose() {
    try (MockedStatic<JniMemory> mockedStatic = mockStatic(JniMemory.class)) {
      mockedStatic.when(() -> JniMemory.nativeDestroyMemory(VALID_HANDLE)).then(invocation -> null);

      final JniMemory memory = new JniMemory(VALID_HANDLE);
      assertFalse(memory.isClosed());

      memory.close();

      assertTrue(memory.isClosed());
      mockedStatic.verify(() -> JniMemory.nativeDestroyMemory(VALID_HANDLE));
    }
  }

  @Test
  void testCloseIsIdempotent() {
    try (MockedStatic<JniMemory> mockedStatic = mockStatic(JniMemory.class)) {
      mockedStatic.when(() -> JniMemory.nativeDestroyMemory(VALID_HANDLE)).then(invocation -> null);

      final JniMemory memory = new JniMemory(VALID_HANDLE);

      memory.close();
      memory.close(); // Second close should be safe

      assertTrue(memory.isClosed());
      // Should only call native destroy once
      mockedStatic.verify(() -> JniMemory.nativeDestroyMemory(VALID_HANDLE));
    }
  }

  @Test
  void testTryWithResources() {
    try (MockedStatic<JniMemory> mockedStatic = mockStatic(JniMemory.class)) {
      mockedStatic.when(() -> JniMemory.nativeDestroyMemory(VALID_HANDLE)).then(invocation -> null);

      assertDoesNotThrow(
          () -> {
            try (JniMemory memory = new JniMemory(VALID_HANDLE)) {
              assertFalse(memory.isClosed());
              assertThat(memory.getNativeHandle()).isEqualTo(VALID_HANDLE);
            }
          });

      mockedStatic.verify(() -> JniMemory.nativeDestroyMemory(VALID_HANDLE));
    }
  }

  @Test
  void testToString() {
    final JniMemory memory = new JniMemory(VALID_HANDLE);
    final String toString = memory.toString();

    assertThat(toString).contains("Memory");
    assertThat(toString).contains("handle=0x" + Long.toHexString(VALID_HANDLE));
    assertThat(toString).contains("closed=false");

    memory.close();
    final String toStringAfterClose = memory.toString();
    assertThat(toStringAfterClose).contains("closed=true");
  }

  @Test
  void testGetResourceType() {
    final JniMemory memory = new JniMemory(VALID_HANDLE);
    assertThat(memory.getResourceType()).isEqualTo("Memory");
  }

  @Test
  void testBoundsCheckingEdgeCases() {
    try (MockedStatic<JniMemory> mockedStatic = mockStatic(JniMemory.class)) {
      mockedStatic.when(() -> JniMemory.nativeGetSize(VALID_HANDLE)).thenReturn(MEMORY_SIZE);

      final JniMemory memory = new JniMemory(VALID_HANDLE);

      // Test reading at exact boundary
      assertDoesNotThrow(() -> memory.readByte(MEMORY_SIZE - 1));

      // Test reading beyond boundary
      assertThrows(IndexOutOfBoundsException.class, () -> memory.readByte(MEMORY_SIZE));

      // Test empty buffer operations
      final byte[] emptyBuffer = new byte[0];
      assertDoesNotThrow(() -> memory.readBytes(0L, emptyBuffer));
      assertDoesNotThrow(() -> memory.writeBytes(0L, emptyBuffer));
    }
  }

  @Test
  void testExceptionHandling() {
    try (MockedStatic<JniMemory> mockedStatic = mockStatic(JniMemory.class)) {
      mockedStatic
          .when(() -> JniMemory.nativeGetSize(anyLong()))
          .thenThrow(new RuntimeException("Native error"));

      final JniMemory memory = new JniMemory(VALID_HANDLE);

      final RuntimeException exception = assertThrows(RuntimeException.class, memory::size);

      assertThat(exception.getMessage()).contains("Unexpected error getting memory size");
      assertThat(exception.getCause()).isNotNull();
      assertThat(exception.getCause().getMessage()).isEqualTo("Native error");
    }
  }

  @Test
  void testConcurrentAccess() {
    try (MockedStatic<JniMemory> mockedStatic = mockStatic(JniMemory.class)) {
      mockedStatic.when(() -> JniMemory.nativeGetSize(VALID_HANDLE)).thenReturn(MEMORY_SIZE);

      final JniMemory memory = new JniMemory(VALID_HANDLE);

      // Test concurrent size queries don't cause issues
      final Thread[] threads = new Thread[5];
      for (int i = 0; i < threads.length; i++) {
        threads[i] = new Thread(() -> assertThat(memory.size()).isEqualTo(MEMORY_SIZE));
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
  void testLargeOffsetValidation() {
    try (MockedStatic<JniMemory> mockedStatic = mockStatic(JniMemory.class)) {
      mockedStatic.when(() -> JniMemory.nativeGetSize(VALID_HANDLE)).thenReturn(MEMORY_SIZE);

      final JniMemory memory = new JniMemory(VALID_HANDLE);

      // Test with maximum long value
      assertThrows(IndexOutOfBoundsException.class, () -> memory.readByte(Long.MAX_VALUE));

      // Test with large buffer that would overflow
      final byte[] largeBuffer = new byte[1000];
      assertThrows(
          IndexOutOfBoundsException.class, () -> memory.readBytes(MEMORY_SIZE - 100, largeBuffer));
    }
  }
}
