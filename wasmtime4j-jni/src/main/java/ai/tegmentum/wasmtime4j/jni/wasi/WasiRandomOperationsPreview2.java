package ai.tegmentum.wasmtime4j.jni.wasi;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiErrorCode;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation of WASI Preview 2 random number operations.
 *
 * <p>This class implements the WASI Preview 2 random operations as defined in the WIT interface
 * `wasi:random/random`. It provides enhanced random number generation with async support and
 * improved security.
 *
 * <p>Supported WASI Preview 2 random operations:
 *
 * <ul>
 *   <li>Cryptographically secure random byte generation
 *   <li>Async random number generation
 *   <li>Seeded random number generation for reproducible results
 *   <li>High-performance random data streaming
 * </ul>
 *
 * @since 1.0.0
 */
public final class WasiRandomOperationsPreview2 {

  private static final Logger LOGGER =
      Logger.getLogger(WasiRandomOperationsPreview2.class.getName());

  /** Maximum number of random bytes that can be generated in a single call. */
  private static final int MAX_RANDOM_BYTES = 1024 * 1024; // 1MB

  /** The WASI context this random operations instance belongs to. */
  private final WasiContext wasiContext;

  /** Executor service for async operations. */
  private final ExecutorService asyncExecutor;

  /** Fallback secure random instance for Java-side generation. */
  private final SecureRandom fallbackRandom;

  /**
   * Creates a new WASI Preview 2 random operations instance.
   *
   * @param wasiContext the WASI context to operate within
   * @param asyncExecutor the executor service for async operations
   * @throws JniException if parameters are null
   */
  public WasiRandomOperationsPreview2(
      final WasiContext wasiContext, final ExecutorService asyncExecutor) {
    JniValidation.requireNonNull(wasiContext, "wasiContext");
    JniValidation.requireNonNull(asyncExecutor, "asyncExecutor");

    this.wasiContext = wasiContext;
    this.asyncExecutor = asyncExecutor;
    this.fallbackRandom = new SecureRandom();

    LOGGER.info("Created WASI Preview 2 random operations handler");
  }

  /**
   * Generates random bytes using the system's cryptographically secure random number generator.
   *
   * <p>WIT interface: wasi:random/random.get-random-bytes
   *
   * @param buffer the buffer to fill with random bytes
   * @throws WasiException if random generation fails
   */
  public void getRandomBytes(final ByteBuffer buffer) {
    JniValidation.requireNonNull(buffer, "buffer");

    final int bytesRequested = buffer.remaining();
    if (bytesRequested == 0) {
      return;
    }

    if (bytesRequested > MAX_RANDOM_BYTES) {
      throw new WasiException(
          "Too many random bytes requested: " + bytesRequested + " > " + MAX_RANDOM_BYTES,
          WasiErrorCode.EINVAL);
    }

    LOGGER.fine(() -> String.format("Generating %d random bytes", bytesRequested));

    try {
      final byte[] randomBytes = new byte[bytesRequested];
      final int result =
          nativeGetRandomBytes(wasiContext.getNativeHandle(), randomBytes, bytesRequested);

      if (result != 0) {
        // Fallback to Java's SecureRandom if native generation fails
        LOGGER.fine("Native random generation failed, using Java fallback");
        fallbackRandom.nextBytes(randomBytes);
      }

      buffer.put(randomBytes);
      LOGGER.fine(() -> String.format("Generated %d random bytes successfully", bytesRequested));

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Random byte generation failed", e);
      throw new WasiException(
          "Random byte generation failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Generates random bytes asynchronously.
   *
   * @param buffer the buffer to fill with random bytes
   * @return CompletableFuture that completes when the buffer is filled with random bytes
   */
  public CompletableFuture<Void> getRandomBytesAsync(final ByteBuffer buffer) {
    JniValidation.requireNonNull(buffer, "buffer");

    final int bytesRequested = buffer.remaining();
    LOGGER.fine(() -> String.format("Generating %d random bytes async", bytesRequested));

    return CompletableFuture.runAsync(
        () -> {
          try {
            getRandomBytes(buffer);
          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Async random byte generation failed", e);
            throw new RuntimeException("Async random byte generation failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Generates a random 32-bit unsigned integer.
   *
   * <p>WIT interface: wasi:random/random.get-random-u32
   *
   * @return a random 32-bit unsigned integer
   * @throws WasiException if random generation fails
   */
  public int getRandomU32() {
    LOGGER.fine("Generating random u32");

    try {
      final RandomU32Result result = nativeGetRandomU32(wasiContext.getNativeHandle());

      if (result.errorCode != 0) {
        // Fallback to Java's SecureRandom if native generation fails
        LOGGER.fine("Native random u32 generation failed, using Java fallback");
        return fallbackRandom.nextInt();
      }

      LOGGER.fine(() -> String.format("Generated random u32: %d", result.value));
      return result.value;

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Random u32 generation failed", e);
      // Use fallback
      return fallbackRandom.nextInt();
    }
  }

  /**
   * Generates a random 32-bit unsigned integer asynchronously.
   *
   * @return CompletableFuture that resolves to a random 32-bit unsigned integer
   */
  public CompletableFuture<Integer> getRandomU32Async() {
    LOGGER.fine("Generating random u32 async");

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return getRandomU32();
          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Async random u32 generation failed", e);
            throw new RuntimeException("Async random u32 generation failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Generates a random 64-bit unsigned integer.
   *
   * <p>WIT interface: wasi:random/random.get-random-u64
   *
   * @return a random 64-bit unsigned integer
   * @throws WasiException if random generation fails
   */
  public long getRandomU64() {
    LOGGER.fine("Generating random u64");

    try {
      final RandomU64Result result = nativeGetRandomU64(wasiContext.getNativeHandle());

      if (result.errorCode != 0) {
        // Fallback to Java's SecureRandom if native generation fails
        LOGGER.fine("Native random u64 generation failed, using Java fallback");
        return fallbackRandom.nextLong();
      }

      LOGGER.fine(() -> String.format("Generated random u64: %d", result.value));
      return result.value;

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Random u64 generation failed", e);
      // Use fallback
      return fallbackRandom.nextLong();
    }
  }

  /**
   * Generates a random 64-bit unsigned integer asynchronously.
   *
   * @return CompletableFuture that resolves to a random 64-bit unsigned integer
   */
  public CompletableFuture<Long> getRandomU64Async() {
    LOGGER.fine("Generating random u64 async");

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return getRandomU64();
          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Async random u64 generation failed", e);
            throw new RuntimeException("Async random u64 generation failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Seeds the random number generator for reproducible results.
   *
   * <p>This is primarily useful for testing and debugging. In production, the system should use
   * cryptographically secure entropy sources.
   *
   * @param seed the seed value
   * @throws WasiException if seeding fails
   */
  public void seed(final long seed) {
    LOGGER.fine(() -> String.format("Seeding random generator with: %d", seed));

    try {
      final int result = nativeSeedRandom(wasiContext.getNativeHandle(), seed);

      if (result != 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result);
        LOGGER.warning(
            "Native random seeding failed: "
                + (errorCode != null ? errorCode.getDescription() : "Unknown error"));
      }

      // Also seed the fallback random generator
      fallbackRandom.setSeed(seed);
      LOGGER.fine("Random generator seeded successfully");

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Random seeding failed", e);
      // Still seed the fallback
      fallbackRandom.setSeed(seed);
    }
  }

  /**
   * Generates a random value within a specified range.
   *
   * @param min the minimum value (inclusive)
   * @param max the maximum value (exclusive)
   * @return a random value in the range [min, max)
   * @throws WasiException if the range is invalid
   */
  public int getRandomInRange(final int min, final int max) {
    if (min >= max) {
      throw new WasiException("Invalid range: min must be less than max", WasiErrorCode.EINVAL);
    }

    final int range = max - min;
    final int randomValue = Math.abs(getRandomU32()) % range;
    return min + randomValue;
  }

  /**
   * Generates a random value within a specified range asynchronously.
   *
   * @param min the minimum value (inclusive)
   * @param max the maximum value (exclusive)
   * @return CompletableFuture that resolves to a random value in the range [min, max)
   */
  public CompletableFuture<Integer> getRandomInRangeAsync(final int min, final int max) {
    if (min >= max) {
      return CompletableFuture.failedFuture(
          new WasiException("Invalid range: min must be less than max", WasiErrorCode.EINVAL));
    }

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return getRandomInRange(min, max);
          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Async random range generation failed", e);
            throw new RuntimeException(
                "Async random range generation failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Generates a random boolean value.
   *
   * @return a random boolean value
   */
  public boolean getRandomBoolean() {
    return (getRandomU32() & 1) == 1;
  }

  /**
   * Generates a random boolean value asynchronously.
   *
   * @return CompletableFuture that resolves to a random boolean value
   */
  public CompletableFuture<Boolean> getRandomBooleanAsync() {
    return getRandomU32Async().thenApply(value -> (value & 1) == 1);
  }

  /**
   * Generates a random double value between 0.0 (inclusive) and 1.0 (exclusive).
   *
   * @return a random double value in the range [0.0, 1.0)
   */
  public double getRandomDouble() {
    // Use the upper 53 bits of a 64-bit random value to ensure uniform distribution
    final long randomBits = getRandomU64() >>> 11; // Remove lower 11 bits
    return (randomBits & 0x1FFFFFFFFFFFFFL) * 0x1.0p-53;
  }

  /**
   * Generates a random double value asynchronously.
   *
   * @return CompletableFuture that resolves to a random double value in the range [0.0, 1.0)
   */
  public CompletableFuture<Double> getRandomDoubleAsync() {
    return getRandomU64Async()
        .thenApply(
            randomBits -> {
              final long bits = randomBits >>> 11; // Remove lower 11 bits
              return (bits & 0x1FFFFFFFFFFFFFL) * 0x1.0p-53;
            });
  }

  // Native method declarations
  private static native int nativeGetRandomBytes(long contextHandle, byte[] buffer, int length);

  private static native RandomU32Result nativeGetRandomU32(long contextHandle);

  private static native RandomU64Result nativeGetRandomU64(long contextHandle);

  private static native int nativeSeedRandom(long contextHandle, long seed);

  /** Random U32 result from native code. */
  private static final class RandomU32Result {
    public final int errorCode;
    public final int value;

    public RandomU32Result(final int errorCode, final int value) {
      this.errorCode = errorCode;
      this.value = value;
    }
  }

  /** Random U64 result from native code. */
  private static final class RandomU64Result {
    public final int errorCode;
    public final long value;

    public RandomU64Result(final int errorCode, final long value) {
      this.errorCode = errorCode;
      this.value = value;
    }
  }
}
