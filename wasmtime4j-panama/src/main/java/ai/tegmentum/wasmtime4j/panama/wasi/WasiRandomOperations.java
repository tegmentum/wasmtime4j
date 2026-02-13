package ai.tegmentum.wasmtime4j.panama.wasi;

import ai.tegmentum.wasmtime4j.panama.exception.PanamaException;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import ai.tegmentum.wasmtime4j.panama.wasi.exception.WasiException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of WASI random number generation operations.
 *
 * <p>This class provides access to WASI secure random number generation operations as specified in
 * WASI preview1. It integrates with the system's cryptographically secure random sources using
 * Panama Foreign Function Interface to provide high-quality random data for WebAssembly
 * applications.
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

  /** Native symbol lookup for WASI functions. */
  private final SymbolLookup symbolLookup;

  /** Method handle for wasi_random_get function. */
  private final MethodHandle randomGetHandle;

  /** Fallback secure random generator for Java-side operations. */
  private final SecureRandom fallbackRandom;

  /**
   * Creates a new WASI random operations instance.
   *
   * @param wasiContext the WASI context to operate within
   * @param symbolLookup the symbol lookup for native WASI functions
   * @throws PanamaException if the wasiContext or symbolLookup is null, or if native function
   *     lookup fails
   */
  public WasiRandomOperations(final WasiContext wasiContext, final SymbolLookup symbolLookup)
      throws PanamaException {
    PanamaValidation.requireNonNull(wasiContext, "wasiContext");
    PanamaValidation.requireNonNull(symbolLookup, "symbolLookup");

    this.wasiContext = wasiContext;
    this.symbolLookup = symbolLookup;
    this.fallbackRandom = new SecureRandom();

    // Initialize native function handles
    try {
      this.randomGetHandle = initializeRandomGetHandle();
      LOGGER.fine("Initialized WASI random operations with Panama FFI");
    } catch (final Exception e) {
      throw new PanamaException(
          "Failed to initialize WASI random operations: " + e.getMessage(), e);
    }
  }

  /**
   * Fills the specified buffer with cryptographically secure random bytes.
   *
   * <p>This operation fills the provided buffer with high-quality random data from the system's
   * secure random sources. The buffer must have sufficient remaining space to hold the random data.
   *
   * @param buffer the buffer to fill with random bytes (from current position to limit)
   * @throws WasiException if the random generation fails
   * @throws PanamaException if the buffer is null, read-only, or a Panama FFI error occurs
   */
  public void getRandomBytes(final ByteBuffer buffer) throws PanamaException {
    PanamaValidation.requireNonNull(buffer, "buffer");

    if (buffer.isReadOnly()) {
      throw new PanamaException("Buffer is read-only and cannot be filled with random data");
    }

    final int remaining = buffer.remaining();
    if (remaining == 0) {
      LOGGER.fine("Buffer has no remaining space, no random data generated");
      return;
    }

    validateBufferSize(remaining);

    try (final Arena arena = Arena.ofConfined()) {
      LOGGER.fine(() -> String.format("Generating %d random bytes", remaining));

      // Allocate native memory for the random data
      final MemorySegment nativeBuffer = arena.allocate(remaining);

      // Call wasi_random_get to fill the native buffer
      final int result = (int) randomGetHandle.invoke(nativeBuffer, remaining);

      if (result != 0) {
        throw new WasiException("Failed to generate random bytes: error code " + result);
      }

      // Copy the random data to the Java buffer
      final byte[] randomBytes = nativeBuffer.toArray(ValueLayout.JAVA_BYTE);
      buffer.put(randomBytes);

      LOGGER.fine(() -> String.format("Successfully generated %d random bytes", remaining));

    } catch (final Throwable e) {
      LOGGER.log(Level.WARNING, "Error generating " + remaining + " random bytes", e);
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      } else {
        throw new PanamaException("Failed to generate random bytes: " + e.getMessage(), e);
      }
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
   * @throws PanamaException if the length is invalid or a Panama FFI error occurs
   */
  public byte[] generateRandomBytes(final int length) throws PanamaException {
    PanamaValidation.requireNonNegative(length, "length");
    validateBufferSize(length);

    if (length == 0) {
      return new byte[0];
    }

    try (final Arena arena = Arena.ofConfined()) {
      LOGGER.fine(() -> String.format("Generating %d random bytes into new array", length));

      // Allocate native memory for the random data
      final MemorySegment nativeBuffer = arena.allocate(length);

      // Call wasi_random_get to fill the native buffer
      final int result = (int) randomGetHandle.invoke(nativeBuffer, length);

      if (result != 0) {
        throw new WasiException("Failed to generate random bytes: error code " + result);
      }

      // Convert to byte array and return
      final byte[] randomBytes = nativeBuffer.toArray(ValueLayout.JAVA_BYTE);
      LOGGER.fine(() -> String.format("Successfully generated %d random bytes", length));
      return randomBytes;

    } catch (final Throwable e) {
      LOGGER.log(Level.WARNING, "Error generating " + length + " random bytes", e);
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      } else {
        throw new PanamaException("Failed to generate random bytes: " + e.getMessage(), e);
      }
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
   * @throws PanamaException if a Panama FFI error occurs
   */
  public int generateRandomInt() throws PanamaException {
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
   * @throws PanamaException if the bound is not positive or a Panama FFI error occurs
   */
  public int generateRandomInt(final int bound) throws PanamaException {
    if (bound <= 0) {
      throw new PanamaException("Bound must be positive: " + bound);
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
   * @throws PanamaException if a Panama FFI error occurs
   */
  public long generateRandomLong() throws PanamaException {
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
   * @throws PanamaException if a Panama FFI error occurs
   */
  public double generateRandomDouble() throws PanamaException {
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
   * @throws PanamaException if the buffer is null or read-only
   */
  public void getRandomBytesFallback(final ByteBuffer buffer) throws PanamaException {
    PanamaValidation.requireNonNull(buffer, "buffer");

    if (buffer.isReadOnly()) {
      throw new PanamaException("Buffer is read-only and cannot be filled with random data");
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
   * @throws PanamaException if the buffer size is too large
   */
  private void validateBufferSize(final int size) throws PanamaException {
    if (size > MAX_BUFFER_SIZE) {
      throw new PanamaException(
          "Buffer size too large: " + size + " bytes (maximum: " + MAX_BUFFER_SIZE + " bytes)");
    }
  }

  /**
   * Initializes the method handle for wasi_random_get function.
   *
   * @return the method handle for random bytes generation
   * @throws Exception if function lookup fails
   */
  private MethodHandle initializeRandomGetHandle() throws Exception {
    final MemorySegment symbol =
        symbolLookup
            .find("wasi_random_get")
            .orElseThrow(() -> new PanamaException("WASI function wasi_random_get not found"));

    final FunctionDescriptor descriptor =
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return: wasi_errno_t
            ValueLayout.ADDRESS, // buf: *void
            ValueLayout.JAVA_INT // buf_len: size_t
            );

    return Linker.nativeLinker().downcallHandle(symbol, descriptor);
  }
}
