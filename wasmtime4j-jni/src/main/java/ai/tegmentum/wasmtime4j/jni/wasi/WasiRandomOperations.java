package ai.tegmentum.wasmtime4j.jni.wasi;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiErrorCode;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation of WASI random number generation operations.
 *
 * <p>This class provides access to WASI secure random number generation operations as specified in
 * WASI preview1. It integrates with the system's cryptographically secure random sources to provide
 * high-quality random data for WebAssembly applications.
 *
 * <p>Supported operations:
 *
 * <ul>
 *   <li>Secure random bytes generation using system entropy sources
 *   <li>Buffer validation and bounds checking for all operations
 *   <li>Integration with platform-specific secure random generators
 *   <li>Thread-safe access to random number generation
 * </ul>
 *
 * <p>Security considerations:
 *
 * <ul>
 *   <li>All random data is generated using cryptographically secure sources
 *   <li>Buffer validation prevents buffer overflow and memory access violations
 *   <li>Random generation respects WASI sandbox security boundaries
 *   <li>No predictable patterns or weak random sources are used
 * </ul>
 *
 * @since 1.0.0
 */
public final class WasiRandomOperations {

  private static final Logger LOGGER = Logger.getLogger(WasiRandomOperations.class.getName());

  /** Maximum buffer size for a single random generation request (1MB). */
  private static final int MAX_BUFFER_SIZE = 1024 * 1024;

  /** The WASI context this random operations instance belongs to. */
  private final WasiContext wasiContext;

  /** Fallback secure random generator for Java-side operations. */
  private final SecureRandom fallbackRandom;

  /**
   * Creates a new WASI random operations instance.
   *
   * @param wasiContext the WASI context to operate within
   * @throws JniException if the wasiContext is null
   */
  public WasiRandomOperations(final WasiContext wasiContext) {
    JniValidation.requireNonNull(wasiContext, "wasiContext");
    this.wasiContext = wasiContext;
    this.fallbackRandom = new SecureRandom();
  }

  /**
   * Fills the specified buffer with cryptographically secure random bytes.
   *
   * <p>This operation fills the provided buffer with high-quality random data from the system's
   * secure random sources. The buffer must have sufficient remaining space to hold the random data.
   *
   * @param buffer the buffer to fill with random bytes (from current position to limit)
   * @throws WasiException if the random generation fails
   * @throws JniException if the buffer is null, read-only, or a JNI error occurs
   */
  public void getRandomBytes(final ByteBuffer buffer) {
    JniValidation.requireNonNull(buffer, "buffer");

    if (buffer.isReadOnly()) {
      throw new JniException("Buffer is read-only and cannot be filled with random data");
    }

    final int remaining = buffer.remaining();
    if (remaining == 0) {
      LOGGER.fine("Buffer has no remaining space, no random data generated");
      return;
    }

    validateBufferSize(remaining);

    try {
      LOGGER.fine(() -> String.format("Generating %d random bytes", remaining));

      final int result;
      if (buffer.isDirect()) {
        // Use direct buffer for better performance with native code
        result =
            nativeGetRandomBytesDirect(
                wasiContext.getNativeHandle(), buffer, buffer.position(), remaining);
      } else {
        // Handle heap buffer by copying data
        final byte[] randomBytes = new byte[remaining];
        result = nativeGetRandomBytesArray(wasiContext.getNativeHandle(), randomBytes);
        if (result == 0) {
          buffer.put(randomBytes);
        }
      }

      if (result != 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result);
        if (errorCode != null) {
          throw new WasiException(
              "Failed to generate random bytes: " + errorCode.getDescription(),
              errorCode,
              "random_get",
              "buffer");
        } else {
          throw new WasiException(
              "Failed to generate random bytes with unknown error code: " + result);
        }
      }

      LOGGER.fine(() -> String.format("Successfully generated %d random bytes", remaining));

    } catch (final RuntimeException e) {
      LOGGER.log(Level.WARNING, "Error generating " + remaining + " random bytes", e);
      throw e;
    }
  }

  /**
   * Generates the specified number of random bytes into a new byte array.
   *
   * <p>This is a convenience method that creates a new byte array and fills it with
   * cryptographically secure random bytes.
   *
   * @param length the number of random bytes to generate
   * @return a new byte array filled with random bytes
   * @throws WasiException if the random generation fails
   * @throws JniException if the length is invalid or a JNI error occurs
   */
  public byte[] generateRandomBytes(final int length) {
    JniValidation.requireNonNegative(length, "length");
    validateBufferSize(length);

    if (length == 0) {
      return new byte[0];
    }

    try {
      LOGGER.fine(() -> String.format("Generating %d random bytes into new array", length));

      final byte[] randomBytes = new byte[length];
      final int result = nativeGetRandomBytesArray(wasiContext.getNativeHandle(), randomBytes);

      if (result != 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result);
        if (errorCode != null) {
          throw new WasiException(
              "Failed to generate random bytes: " + errorCode.getDescription(),
              errorCode,
              "random_get",
              "buffer");
        } else {
          throw new WasiException(
              "Failed to generate random bytes with unknown error code: " + result);
        }
      }

      LOGGER.fine(() -> String.format("Successfully generated %d random bytes", length));
      return randomBytes;

    } catch (final RuntimeException e) {
      LOGGER.log(Level.WARNING, "Error generating " + length + " random bytes", e);
      throw e;
    }
  }

  /**
   * Generates a random integer using secure random generation.
   *
   * <p>This is a convenience method that generates 4 random bytes and converts them to an integer.
   * The resulting integer is uniformly distributed across the full integer range.
   *
   * @return a random integer
   * @throws WasiException if the random generation fails
   * @throws JniException if a JNI error occurs
   */
  public int generateRandomInt() {
    final byte[] randomBytes = generateRandomBytes(4);
    return ByteBuffer.wrap(randomBytes).getInt();
  }

  /**
   * Generates a random integer within the specified range [0, bound).
   *
   * <p>This method generates a uniformly distributed random integer in the range from 0 (inclusive)
   * to the specified bound (exclusive). The bound must be positive.
   *
   * @param bound the upper bound (exclusive) for the random integer
   * @return a random integer in the range [0, bound)
   * @throws WasiException if the random generation fails
   * @throws JniException if the bound is not positive or a JNI error occurs
   */
  public int generateRandomInt(final int bound) {
    if (bound <= 0) {
      throw new JniException("Bound must be positive: " + bound);
    }

    // Use the same algorithm as SecureRandom to ensure uniform distribution
    int bits;
    int value;
    do {
      bits = generateRandomInt() >>> 1; // Remove sign bit
      value = bits % bound;
    } while (bits - value + (bound - 1) < 0);

    return value;
  }

  /**
   * Generates a random long using secure random generation.
   *
   * <p>This is a convenience method that generates 8 random bytes and converts them to a long. The
   * resulting long is uniformly distributed across the full long range.
   *
   * @return a random long
   * @throws WasiException if the random generation fails
   * @throws JniException if a JNI error occurs
   */
  public long generateRandomLong() {
    final byte[] randomBytes = generateRandomBytes(8);
    return ByteBuffer.wrap(randomBytes).getLong();
  }

  /**
   * Generates a random double in the range [0.0, 1.0).
   *
   * <p>This method generates a uniformly distributed random double in the range from 0.0
   * (inclusive) to 1.0 (exclusive).
   *
   * @return a random double in the range [0.0, 1.0)
   * @throws WasiException if the random generation fails
   * @throws JniException if a JNI error occurs
   */
  public double generateRandomDouble() {
    final long randomLong = generateRandomLong() >>> 11; // Use 53 bits for IEEE 754 double
    return (randomLong * 0x1.0p-53);
  }

  /**
   * Fills the specified buffer with secure random bytes using fallback Java generation.
   *
   * <p>This method uses Java's SecureRandom as a fallback when native random generation is not
   * available or fails. It should only be used as a last resort.
   *
   * @param buffer the buffer to fill with random bytes
   * @throws JniException if the buffer is null or read-only
   */
  public void getRandomBytesFallback(final ByteBuffer buffer) {
    JniValidation.requireNonNull(buffer, "buffer");

    if (buffer.isReadOnly()) {
      throw new JniException("Buffer is read-only and cannot be filled with random data");
    }

    final int remaining = buffer.remaining();
    if (remaining == 0) {
      return;
    }

    LOGGER.warning("Using fallback Java SecureRandom for random generation");

    final byte[] randomBytes = new byte[remaining];
    fallbackRandom.nextBytes(randomBytes);
    buffer.put(randomBytes);
  }

  /**
   * Validates that the specified buffer size is within acceptable limits.
   *
   * @param size the buffer size to validate
   * @throws JniException if the buffer size is too large
   */
  private void validateBufferSize(final int size) {
    if (size > MAX_BUFFER_SIZE) {
      throw new JniException(
          "Buffer size too large: " + size + " bytes (maximum: " + MAX_BUFFER_SIZE + " bytes)");
    }
  }

  /**
   * Native method to fill a direct ByteBuffer with random bytes.
   *
   * @param contextHandle the native WASI context handle
   * @param buffer the direct ByteBuffer to fill
   * @param position the position in the buffer to start filling
   * @param length the number of bytes to generate
   * @return 0 on success, or positive error code on failure
   */
  private static native int nativeGetRandomBytesDirect(
      long contextHandle, ByteBuffer buffer, int position, int length);

  /**
   * Native method to fill a byte array with random bytes.
   *
   * @param contextHandle the native WASI context handle
   * @param buffer the byte array to fill
   * @return 0 on success, or positive error code on failure
   */
  private static native int nativeGetRandomBytesArray(long contextHandle, byte[] buffer);
}
