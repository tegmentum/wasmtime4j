package ai.tegmentum.wasmtime4j.jni.performance;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Optimized parameter marshalling to reduce copying overhead between Java and native code.
 *
 * <p>This class provides high-performance parameter marshalling techniques that minimize memory
 * allocation and copying when transferring data between Java and WebAssembly.
 *
 * <p>Optimizations include:
 *
 * <ul>
 *   <li>Direct ByteBuffer usage to avoid array copying
 *   <li>Pooled buffer reuse for common parameter patterns
 *   <li>Type-specific optimized marshalling paths
 *   <li>Bulk parameter marshalling for multiple values
 *   <li>Zero-copy techniques for large data transfers
 *   <li>Native-endian format alignment for optimal performance
 * </ul>
 *
 * <p>The marshalling system automatically selects the most efficient approach based on parameter
 * types, sizes, and usage patterns.
 *
 * @since 1.0.0
 */
public final class OptimizedMarshalling {

  private static final Logger LOGGER = Logger.getLogger(OptimizedMarshalling.class.getName());

  /** Buffer pool for marshalling operations. */
  private static final NativeObjectPool<ByteBuffer> BUFFER_POOL =
      NativeObjectPool.getPool(
          ByteBuffer.class,
          () -> ByteBuffer.allocateDirect(4096).order(ByteOrder.nativeOrder()),
          32, // Increased pool size for better performance
          8);

  /** Cache for frequently used parameter patterns. */
  private static final ConcurrentHashMap<String, MarshallingPlan> MARSHALLING_CACHE =
      new ConcurrentHashMap<>();

  /** Maximum cache size for marshalling plans. */
  private static final int MAX_MARSHALLING_CACHE_SIZE = 512; // Increased cache size

  /** Threshold for using direct marshalling vs buffered marshalling. */
  private static final int DIRECT_MARSHALLING_THRESHOLD = 32; // Lowered threshold for better performance

  /** Thread-local cache for small parameter arrays to avoid allocation. */
  private static final ThreadLocal<Object[]> THREAD_LOCAL_PARAM_CACHE =
      ThreadLocal.withInitial(() -> new Object[16]);

  /** Represents an optimized marshalling plan for a specific parameter pattern. */
  private static final class MarshallingPlan {
    final WasmValueType[] types;
    final int totalBytes;
    final boolean useDirect;
    final MarshallingStrategy strategy;
    final long createdTime;
    volatile long lastUsedTime;
    volatile int useCount;

    MarshallingPlan(
        final WasmValueType[] types,
        final int totalBytes,
        final boolean useDirect,
        final MarshallingStrategy strategy) {
      this.types = types.clone();
      this.totalBytes = totalBytes;
      this.useDirect = useDirect;
      this.strategy = strategy;
      this.createdTime = System.currentTimeMillis();
      this.lastUsedTime = createdTime;
      this.useCount = 0;
    }

    void markUsed() {
      lastUsedTime = System.currentTimeMillis();
      useCount++;
    }
  }

  /** Marshalling strategies for different parameter patterns. */
  public enum MarshallingStrategy {
    /** Simple direct marshalling for basic types. */
    DIRECT,
    /** Buffered marshalling using pooled ByteBuffers. */
    BUFFERED,
    /** Bulk marshalling for arrays of similar types. */
    BULK,
    /** Zero-copy marshalling for large data. */
    ZERO_COPY
  }

  // Private constructor - utility class
  private OptimizedMarshalling() {}

  /**
   * Marshals WebAssembly parameters to native format with optimal performance.
   *
   * @param parameters the WebAssembly parameters to marshal
   * @return native parameter representation
   * @throws IllegalArgumentException if parameters are invalid
   */
  public static Object[] marshalParameters(final WasmValue[] parameters) {
    if (parameters == null) {
      throw new IllegalArgumentException("parameters cannot be null");
    }

    final long startTime = PerformanceMonitor.startOperation("parameter_marshalling");
    try {
      // Handle empty parameters quickly
      if (parameters.length == 0) {
        return new Object[0];
      }

      // Extract types for plan lookup
      final WasmValueType[] types = new WasmValueType[parameters.length];
      for (int i = 0; i < parameters.length; i++) {
        types[i] = parameters[i].getType();
      }

      // Get or create marshalling plan
      final MarshallingPlan plan = getOrCreateMarshallingPlan(types);
      plan.markUsed();

      // Execute marshalling based on strategy
      switch (plan.strategy) {
        case DIRECT:
          return marshalDirect(parameters);
        case BUFFERED:
          return marshalBuffered(parameters, plan);
        case BULK:
          return marshalBulk(parameters, plan);
        case ZERO_COPY:
          return marshalZeroCopy(parameters);
        default:
          throw new IllegalStateException("Unknown marshalling strategy: " + plan.strategy);
      }

    } finally {
      PerformanceMonitor.endOperation("parameter_marshalling", startTime);
    }
  }

  /**
   * Unmarshals native results back to WebAssembly values.
   *
   * @param nativeResults the native results to unmarshal
   * @param expectedTypes the expected WebAssembly types
   * @return WebAssembly values
   * @throws IllegalArgumentException if parameters are invalid
   */
  public static WasmValue[] unmarshalResults(
      final Object[] nativeResults, final WasmValueType[] expectedTypes) {
    if (nativeResults == null) {
      throw new IllegalArgumentException("nativeResults cannot be null");
    }
    if (expectedTypes == null) {
      throw new IllegalArgumentException("expectedTypes cannot be null");
    }

    final long startTime = PerformanceMonitor.startOperation("result_unmarshalling");
    try {
      if (nativeResults.length != expectedTypes.length) {
        throw new IllegalArgumentException(
            "Result count mismatch: got "
                + nativeResults.length
                + ", expected "
                + expectedTypes.length);
      }

      final WasmValue[] results = new WasmValue[nativeResults.length];
      for (int i = 0; i < nativeResults.length; i++) {
        results[i] = unmarshalSingleResult(nativeResults[i], expectedTypes[i]);
      }

      return results;

    } finally {
      PerformanceMonitor.endOperation("result_unmarshalling", startTime);
    }
  }

  /**
   * Gets or creates a marshalling plan for the given parameter types.
   *
   * @param types parameter types
   * @return marshalling plan
   */
  private static MarshallingPlan getOrCreateMarshallingPlan(final WasmValueType[] types) {
    // Create cache key based on types
    final String cacheKey = createTypeSignature(types);

    MarshallingPlan plan = MARSHALLING_CACHE.get(cacheKey);
    if (plan != null) {
      return plan;
    }

    // Calculate total bytes needed
    int totalBytes = 0;
    boolean hasComplexTypes = false;
    boolean allSameType = types.length > 1;
    final WasmValueType firstType = types.length > 0 ? types[0] : null;

    for (final WasmValueType type : types) {
      totalBytes += getTypeSize(type);
      if (type == WasmValueType.V128 || type == WasmValueType.EXTERNREF) {
        hasComplexTypes = true;
      }
      if (allSameType && type != firstType) {
        allSameType = false;
      }
    }

    // Determine optimal strategy
    final MarshallingStrategy strategy;
    final boolean useDirect;

    if (types.length == 1 && !hasComplexTypes) {
      // Single simple parameter - use direct marshalling
      strategy = MarshallingStrategy.DIRECT;
      useDirect = true;
    } else if (allSameType && types.length > 4) {
      // Many parameters of same type - use bulk marshalling
      strategy = MarshallingStrategy.BULK;
      useDirect = false;
    } else if (totalBytes > 1024) {
      // Large parameter set - consider zero-copy
      strategy = MarshallingStrategy.ZERO_COPY;
      useDirect = false;
    } else {
      // General case - use buffered marshalling
      strategy = MarshallingStrategy.BUFFERED;
      useDirect = totalBytes < DIRECT_MARSHALLING_THRESHOLD;
    }

    plan = new MarshallingPlan(types, totalBytes, useDirect, strategy);

    // Cache the plan if we have room
    if (MARSHALLING_CACHE.size() < MAX_MARSHALLING_CACHE_SIZE) {
      MARSHALLING_CACHE.put(cacheKey, plan);
    } else if (MARSHALLING_CACHE.size() >= MAX_MARSHALLING_CACHE_SIZE * 2) {
      // Periodically clean old entries
      evictOldMarshallingPlans();
      MARSHALLING_CACHE.put(cacheKey, plan);
    }

    return plan;
  }

  /** Direct marshalling for simple parameter cases. */
  private static Object[] marshalDirect(final WasmValue[] parameters) {
    final int length = parameters.length;

    // Use thread-local cache for small arrays to avoid allocation
    if (length <= 16) {
      final Object[] cached = THREAD_LOCAL_PARAM_CACHE.get();
      for (int i = 0; i < length; i++) {
        cached[i] = marshalSingleValue(parameters[i]);
      }
      // Return a copy of the appropriate size
      final Object[] result = new Object[length];
      System.arraycopy(cached, 0, result, 0, length);
      return result;
    }

    // For larger arrays, allocate normally
    final Object[] result = new Object[length];
    for (int i = 0; i < parameters.length; i++) {
      result[i] = marshalSingleValue(parameters[i]);
    }
    return result;
  }

  /** Buffered marshalling using pooled ByteBuffers. */
  private static Object[] marshalBuffered(
      final WasmValue[] parameters, final MarshallingPlan plan) {
    ByteBuffer buffer = null;
    try {
      // Get buffer from pool
      buffer = BUFFER_POOL.borrow();
      if (buffer == null) {
        // Fallback to direct marshalling
        return marshalDirect(parameters);
      }

      buffer.clear();
      if (buffer.capacity() < plan.totalBytes) {
        // Buffer too small, fallback to direct
        return marshalDirect(parameters);
      }

      // Marshal all parameters into buffer
      for (final WasmValue param : parameters) {
        marshalIntoBuffer(param, buffer);
      }

      // Create result array with buffer
      buffer.flip();
      final byte[] data = new byte[buffer.remaining()];
      buffer.get(data);

      return new Object[] {data};

    } finally {
      if (buffer != null) {
        BUFFER_POOL.returnObject(buffer);
      }
    }
  }

  /** Bulk marshalling for arrays of similar types. */
  private static Object[] marshalBulk(final WasmValue[] parameters, final MarshallingPlan plan) {
    // For bulk operations, group parameters by type
    final WasmValueType type = parameters[0].getType();

    switch (type) {
      case I32:
        return marshalBulkI32(parameters);
      case I64:
        return marshalBulkI64(parameters);
      case F32:
        return marshalBulkF32(parameters);
      case F64:
        return marshalBulkF64(parameters);
      default:
        // Fallback to buffered marshalling
        return marshalBuffered(parameters, plan);
    }
  }

  /** Zero-copy marshalling for large data transfers. */
  private static Object[] marshalZeroCopy(final WasmValue[] parameters) {
    // For zero-copy, try to avoid intermediate copying
    // This is a simplified implementation
    return marshalDirect(parameters);
  }

  /** Marshals a single WebAssembly value to native format. */
  private static Object marshalSingleValue(final WasmValue value) {
    switch (value.getType()) {
      case I32:
        return value.asI32();
      case I64:
        return value.asI64();
      case F32:
        return value.asF32();
      case F64:
        return value.asF64();
      case V128:
        // V128 values need special handling
        return value.asV128();
      case EXTERNREF:
        // Note: asExternRef() method may not be available yet
        return null; // Placeholder for externref handling
      case FUNCREF:
        // Note: asFuncRef() method may not be available yet
        return null; // Placeholder for funcref handling
      default:
        throw new IllegalArgumentException("Unsupported value type: " + value.getType());
    }
  }

  /** Marshals a value into a ByteBuffer. */
  private static void marshalIntoBuffer(final WasmValue value, final ByteBuffer buffer) {
    switch (value.getType()) {
      case I32:
        buffer.putInt(value.asI32());
        break;
      case I64:
        buffer.putLong(value.asI64());
        break;
      case F32:
        buffer.putFloat(value.asF32());
        break;
      case F64:
        buffer.putDouble(value.asF64());
        break;
      case V128:
        // V128 is 16 bytes
        final byte[] v128Data = value.asV128();
        buffer.put(v128Data);
        break;
      default:
        throw new IllegalArgumentException("Cannot marshal type to buffer: " + value.getType());
    }
  }

  /** Bulk marshalling for I32 arrays. */
  private static Object[] marshalBulkI32(final WasmValue[] parameters) {
    final int[] values = new int[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
      values[i] = parameters[i].asI32();
    }
    return new Object[] {values};
  }

  /** Bulk marshalling for I64 arrays. */
  private static Object[] marshalBulkI64(final WasmValue[] parameters) {
    final long[] values = new long[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
      values[i] = parameters[i].asI64();
    }
    return new Object[] {values};
  }

  /** Bulk marshalling for F32 arrays. */
  private static Object[] marshalBulkF32(final WasmValue[] parameters) {
    final float[] values = new float[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
      values[i] = parameters[i].asF32();
    }
    return new Object[] {values};
  }

  /** Bulk marshalling for F64 arrays. */
  private static Object[] marshalBulkF64(final WasmValue[] parameters) {
    final double[] values = new double[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
      values[i] = parameters[i].asF64();
    }
    return new Object[] {values};
  }

  /** Unmarshals a single native result back to WebAssembly value. */
  private static WasmValue unmarshalSingleResult(
      final Object nativeResult, final WasmValueType expectedType) {
    if (nativeResult == null) {
      throw new IllegalArgumentException("nativeResult cannot be null");
    }

    switch (expectedType) {
      case I32:
        if (nativeResult instanceof Integer) {
          return WasmValue.i32((Integer) nativeResult);
        }
        break;
      case I64:
        if (nativeResult instanceof Long) {
          return WasmValue.i64((Long) nativeResult);
        }
        break;
      case F32:
        if (nativeResult instanceof Float) {
          return WasmValue.f32((Float) nativeResult);
        }
        break;
      case F64:
        if (nativeResult instanceof Double) {
          return WasmValue.f64((Double) nativeResult);
        }
        break;
      case V128:
        if (nativeResult instanceof byte[]) {
          return WasmValue.v128((byte[]) nativeResult);
        }
        break;
      case EXTERNREF:
        // Note: externRef() method may not be available yet
        throw new UnsupportedOperationException("EXTERNREF unmarshalling not yet implemented");
      case FUNCREF:
        // Note: funcRef() method may not be available yet
        throw new UnsupportedOperationException("FUNCREF unmarshalling not yet implemented");
      default:
        throw new IllegalArgumentException("Unsupported result type: " + expectedType);
    }

    throw new IllegalArgumentException(
        "Cannot unmarshal " + nativeResult.getClass().getSimpleName() + " as " + expectedType);
  }

  /** Creates a type signature string for caching. */
  private static String createTypeSignature(final WasmValueType[] types) {
    final StringBuilder sb = new StringBuilder();
    for (final WasmValueType type : types) {
      sb.append(type.name().charAt(0));
    }
    return sb.toString();
  }

  /** Gets the size in bytes for a WebAssembly type. */
  private static int getTypeSize(final WasmValueType type) {
    switch (type) {
      case I32:
      case F32:
        return 4;
      case I64:
      case F64:
        return 8;
      case V128:
        return 16;
      case EXTERNREF:
      case FUNCREF:
        return 8; // Pointer size on 64-bit systems
      default:
        return 8; // Default to pointer size
    }
  }

  /** Evicts old marshalling plans to prevent memory leaks. */
  private static void evictOldMarshallingPlans() {
    final long currentTime = System.currentTimeMillis();
    final long maxAge = 60_000; // 1 minute

    MARSHALLING_CACHE
        .entrySet()
        .removeIf(
            entry -> {
              final MarshallingPlan plan = entry.getValue();
              return currentTime - plan.lastUsedTime > maxAge && plan.useCount < 10;
            });

    LOGGER.fine("Evicted old marshalling plans, cache size: " + MARSHALLING_CACHE.size());
  }

  /**
   * Gets marshalling statistics.
   *
   * @return statistics string
   */
  public static String getStatistics() {
    final StringBuilder sb = new StringBuilder();
    sb.append("=== Optimized Marshalling Statistics ===\n");
    sb.append(String.format("Cached marshalling plans: %d\n", MARSHALLING_CACHE.size()));
    sb.append(String.format("Buffer pool: %s\n", BUFFER_POOL.getStats()));

    // Strategy usage breakdown
    final ConcurrentHashMap<MarshallingStrategy, Integer> strategyCounts =
        new ConcurrentHashMap<>();
    for (final MarshallingPlan plan : MARSHALLING_CACHE.values()) {
      strategyCounts.merge(plan.strategy, plan.useCount, Integer::sum);
    }

    sb.append("Strategy usage:\n");
    for (final MarshallingStrategy strategy : MarshallingStrategy.values()) {
      final int count = strategyCounts.getOrDefault(strategy, 0);
      sb.append(String.format("  %s: %d uses\n", strategy, count));
    }

    return sb.toString();
  }

  /** Clears marshalling caches and resets statistics. */
  public static void reset() {
    MARSHALLING_CACHE.clear();
    BUFFER_POOL.clear();
    LOGGER.info("Optimized marshalling caches cleared");
  }
}
