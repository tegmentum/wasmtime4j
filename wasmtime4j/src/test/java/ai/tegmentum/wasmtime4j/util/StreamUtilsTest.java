package ai.tegmentum.wasmtime4j.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StreamUtils}.
 *
 * @since 1.0.0
 */
class StreamUtilsTest {

  private static final Logger LOGGER = Logger.getLogger(StreamUtilsTest.class.getName());

  @Test
  void readAllBytesWithEmptyStream() throws IOException {
    LOGGER.info("Testing readAllBytes with empty stream");
    final byte[] result = StreamUtils.readAllBytes(new ByteArrayInputStream(new byte[0]));
    assertEquals(0, result.length, "Empty stream should produce empty byte array");
    LOGGER.info("Empty stream returned " + result.length + " bytes");
  }

  @Test
  void readAllBytesWithSmallPayload() throws IOException {
    LOGGER.info("Testing readAllBytes with small payload");
    final byte[] input = {1, 2, 3, 4, 5};
    final byte[] result = StreamUtils.readAllBytes(new ByteArrayInputStream(input));
    assertArrayEquals(input, result, "Small payload should be read correctly");
    LOGGER.info("Small payload: wrote " + input.length + " bytes, read " + result.length);
  }

  @Test
  void readAllBytesWithLargePayloadExceedingBufferSize() throws IOException {
    LOGGER.info("Testing readAllBytes with payload larger than 8192 byte buffer");
    final byte[] input = new byte[8192 * 3 + 1];
    for (int i = 0; i < input.length; i++) {
      input[i] = (byte) (i % 256);
    }
    final byte[] result = StreamUtils.readAllBytes(new ByteArrayInputStream(input));
    assertArrayEquals(input, result, "Large payload spanning multiple buffer reads should match");
    LOGGER.info(
        "Large payload: wrote " + input.length + " bytes, read " + result.length + " bytes");
  }

  @Test
  void readAllBytesWithNullStreamThrowsIllegalArgumentException() {
    LOGGER.info("Testing readAllBytes with null stream");
    final IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> StreamUtils.readAllBytes(null),
            "Null stream should throw IllegalArgumentException");
    LOGGER.info("Caught expected exception: " + exception.getMessage());
  }

  @Test
  void readAllBytesWithExactBufferSizePayload() throws IOException {
    LOGGER.info("Testing readAllBytes with payload exactly equal to buffer size (8192)");
    final byte[] input = new byte[8192];
    for (int i = 0; i < input.length; i++) {
      input[i] = (byte) (i % 256);
    }
    final byte[] result = StreamUtils.readAllBytes(new ByteArrayInputStream(input));
    assertArrayEquals(input, result, "Buffer-sized payload should be read correctly");
    LOGGER.info("Buffer-sized payload: wrote " + input.length + " bytes, read " + result.length);
  }
}
