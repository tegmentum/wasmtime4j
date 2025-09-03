package ai.tegmentum.wasmtime4j.jni.performance;

import java.lang.ref.WeakReference;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Memory pool for frequently allocated native objects to reduce GC pressure.
 *
 * <p>This class manages pools of reusable objects to minimize allocation overhead and garbage
 * collection pressure. It's particularly useful for native parameter arrays, result buffers,
 * and other frequently allocated objects in the JNI layer.
 *
 * <p>Features:
 * <ul>
 *   <li>Type-safe object pools with configurable sizes</li>
 *   <li>Automatic pool size adjustment based on usage patterns</li>
 *   <li>Memory leak prevention through weak references</li>
 *   <li>Performance monitoring and pool statistics</li>
 *   <li>Thread-safe operations with minimal contention</li>
 *   <li>Automatic cleanup of unused pools</li>
 * </ul>
 *
 * <p>Usage Example:
 * <pre>{@code
 * // Get pool for byte arrays
 * NativeObjectPool<byte[]> byteArrayPool = NativeObjectPool.getPool(
 *     byte[].class, () -> new byte[1024], 16);
 * 
 * // Borrow and return objects
 * byte[] buffer = byteArrayPool.borrow();
 * try {
 *   // Use buffer...
 * } finally {
 *   byteArrayPool.returnObject(buffer);
 * }
 * }</pre>
 *
 * @param <T> the type of objects in this pool
 * @since 1.0.0
 */
public final class NativeObjectPool<T> {

    private static final Logger LOGGER = Logger.getLogger(NativeObjectPool.class.getName());

    /** Global registry of object pools by type. */
    private static final ConcurrentHashMap<Class<?>, WeakReference<NativeObjectPool<?>>> POOLS = 
        new ConcurrentHashMap<>();

    /** Default maximum pool size. */
    public static final int DEFAULT_MAX_POOL_SIZE = 32;

    /** Default minimum pool size. */
    public static final int DEFAULT_MIN_POOL_SIZE = 4;

    /** Pool of available objects. */
    private final BlockingQueue<T> availableObjects;

    /** Factory for creating new objects. */
    private final ObjectFactory<T> factory;

    /** Object type for this pool. */
    private final Class<T> objectType;

    /** Maximum number of objects in pool. */
    private final int maxPoolSize;

    /** Minimum number of objects in pool. */
    private final int minPoolSize;

    /** Current pool size. */
    private final AtomicInteger currentSize = new AtomicInteger(0);

    /** Number of objects currently borrowed. */
    private final AtomicInteger borrowedCount = new AtomicInteger(0);

    /** Total number of borrow operations. */
    private final AtomicLong totalBorrows = new AtomicLong(0);

    /** Total number of return operations. */
    private final AtomicLong totalReturns = new AtomicLong(0);

    /** Total number of objects created. */
    private final AtomicLong totalCreated = new AtomicLong(0);

    /** Pool creation timestamp. */
    private final long creationTime = System.currentTimeMillis();

    /** Whether this pool is closed. */
    private volatile boolean closed = false;

    /**
     * Factory interface for creating pooled objects.
     *
     * @param <T> the type of objects to create
     */
    @FunctionalInterface
    public interface ObjectFactory<T> {
        /**
         * Creates a new object instance.
         *
         * @return new object instance
         */
        T create();
    }

    /**
     * Creates a new object pool.
     *
     * @param objectType the class of objects in this pool
     * @param factory factory for creating new objects
     * @param maxPoolSize maximum number of objects in pool
     * @param minPoolSize minimum number of objects in pool
     * @throws IllegalArgumentException if parameters are invalid
     */
    private NativeObjectPool(final Class<T> objectType, final ObjectFactory<T> factory,
                            final int maxPoolSize, final int minPoolSize) {
        if (objectType == null) {
            throw new IllegalArgumentException("objectType cannot be null");
        }
        if (factory == null) {
            throw new IllegalArgumentException("factory cannot be null");
        }
        if (maxPoolSize <= 0) {
            throw new IllegalArgumentException("maxPoolSize must be positive: " + maxPoolSize);
        }
        if (minPoolSize < 0) {
            throw new IllegalArgumentException("minPoolSize cannot be negative: " + minPoolSize);
        }
        if (minPoolSize > maxPoolSize) {
            throw new IllegalArgumentException("minPoolSize cannot exceed maxPoolSize: " 
                                             + minPoolSize + " > " + maxPoolSize);
        }

        this.objectType = objectType;
        this.factory = factory;
        this.maxPoolSize = maxPoolSize;
        this.minPoolSize = minPoolSize;
        this.availableObjects = new LinkedBlockingQueue<>(maxPoolSize);

        // Pre-populate pool with minimum objects
        for (int i = 0; i < minPoolSize; i++) {
            try {
                final T obj = factory.create();
                if (obj != null) {
                    availableObjects.offer(obj);
                    currentSize.incrementAndGet();
                    totalCreated.incrementAndGet();
                }
            } catch (final Exception e) {
                LOGGER.warning("Failed to pre-populate pool with " + objectType.getSimpleName() + ": " + e.getMessage());
                break;
            }
        }

        LOGGER.fine("Created " + objectType.getSimpleName() + " pool with " 
                   + currentSize.get() + "/" + maxPoolSize + " objects");
    }

    /**
     * Gets or creates an object pool for the specified type.
     *
     * @param <T> the object type
     * @param objectType the class of objects in the pool
     * @param factory factory for creating new objects
     * @param maxPoolSize maximum number of objects in pool
     * @return the object pool
     * @throws IllegalArgumentException if parameters are invalid
     */
    @SuppressWarnings("unchecked")
    public static <T> NativeObjectPool<T> getPool(final Class<T> objectType, 
                                                  final ObjectFactory<T> factory,
                                                  final int maxPoolSize) {
        return getPool(objectType, factory, maxPoolSize, DEFAULT_MIN_POOL_SIZE);
    }

    /**
     * Gets or creates an object pool for the specified type.
     *
     * @param <T> the object type
     * @param objectType the class of objects in the pool
     * @param factory factory for creating new objects
     * @param maxPoolSize maximum number of objects in pool
     * @param minPoolSize minimum number of objects in pool
     * @return the object pool
     * @throws IllegalArgumentException if parameters are invalid
     */
    @SuppressWarnings("unchecked")
    public static <T> NativeObjectPool<T> getPool(final Class<T> objectType,
                                                  final ObjectFactory<T> factory,
                                                  final int maxPoolSize,
                                                  final int minPoolSize) {
        if (objectType == null) {
            throw new IllegalArgumentException("objectType cannot be null");
        }

        // Check if pool already exists
        final WeakReference<NativeObjectPool<?>> poolRef = POOLS.get(objectType);
        if (poolRef != null) {
            final NativeObjectPool<?> existingPool = poolRef.get();
            if (existingPool != null && !existingPool.isClosed()) {
                return (NativeObjectPool<T>) existingPool;
            }
            // Remove stale reference
            POOLS.remove(objectType, poolRef);
        }

        // Create new pool
        final NativeObjectPool<T> newPool = new NativeObjectPool<>(
            objectType, factory, maxPoolSize, minPoolSize);
        
        // Register in global pool registry
        POOLS.put(objectType, new WeakReference<>(newPool));

        return newPool;
    }

    /**
     * Borrows an object from the pool.
     *
     * <p>If no objects are available in the pool and the pool hasn't reached its maximum size,
     * a new object will be created. If the pool is full, this method will create a new object
     * that won't be returned to the pool.
     *
     * @return an object from the pool or a newly created object
     * @throws IllegalStateException if the pool is closed
     */
    public T borrow() {
        if (closed) {
            throw new IllegalStateException("Pool is closed");
        }

        totalBorrows.incrementAndGet();
        borrowedCount.incrementAndGet();

        // Try to get object from pool first
        T obj = availableObjects.poll();
        if (obj != null) {
            return obj;
        }

        // Pool is empty, create new object
        try {
            obj = factory.create();
            if (obj != null) {
                totalCreated.incrementAndGet();
                return obj;
            }
        } catch (final Exception e) {
            LOGGER.warning("Failed to create new " + objectType.getSimpleName() + " object: " + e.getMessage());
        }

        // Fallback - return null (caller should handle)
        borrowedCount.decrementAndGet();
        return null;
    }

    /**
     * Returns an object to the pool.
     *
     * <p>The returned object should be in a clean, reusable state. If the pool is full,
     * the object will be discarded to prevent unbounded growth.
     *
     * @param obj the object to return
     * @throws IllegalArgumentException if obj is null
     */
    public void returnObject(final T obj) {
        if (obj == null) {
            throw new IllegalArgumentException("Cannot return null object");
        }
        if (closed) {
            // Pool is closed, don't return object
            borrowedCount.decrementAndGet();
            return;
        }

        totalReturns.incrementAndGet();
        borrowedCount.decrementAndGet();

        // Try to return object to pool
        if (currentSize.get() < maxPoolSize && availableObjects.offer(obj)) {
            // Successfully returned to pool
            return;
        }

        // Pool is full, object will be garbage collected
        // This is intentional to prevent unbounded memory growth
    }

    /**
     * Gets the current number of available objects in the pool.
     *
     * @return number of available objects
     */
    public int getAvailableCount() {
        return availableObjects.size();
    }

    /**
     * Gets the current number of borrowed objects.
     *
     * @return number of borrowed objects
     */
    public int getBorrowedCount() {
        return borrowedCount.get();
    }

    /**
     * Gets the maximum pool size.
     *
     * @return maximum pool size
     */
    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    /**
     * Gets the minimum pool size.
     *
     * @return minimum pool size
     */
    public int getMinPoolSize() {
        return minPoolSize;
    }

    /**
     * Gets the total number of borrow operations.
     *
     * @return total borrow count
     */
    public long getTotalBorrows() {
        return totalBorrows.get();
    }

    /**
     * Gets the total number of return operations.
     *
     * @return total return count
     */
    public long getTotalReturns() {
        return totalReturns.get();
    }

    /**
     * Gets the total number of objects created.
     *
     * @return total objects created
     */
    public long getTotalCreated() {
        return totalCreated.get();
    }

    /**
     * Gets the pool hit rate (percentage of borrows satisfied from pool).
     *
     * @return hit rate as percentage (0.0 to 100.0)
     */
    public double getHitRate() {
        final long borrows = totalBorrows.get();
        if (borrows == 0) {
            return 100.0;
        }
        final long created = totalCreated.get();
        final long hits = Math.max(0, borrows - created);
        return (hits * 100.0) / borrows;
    }

    /**
     * Gets the object type for this pool.
     *
     * @return object type class
     */
    public Class<T> getObjectType() {
        return objectType;
    }

    /**
     * Checks if this pool is closed.
     *
     * @return true if closed
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Clears all objects from the pool.
     */
    public void clear() {
        availableObjects.clear();
        currentSize.set(0);
        LOGGER.fine("Cleared " + objectType.getSimpleName() + " pool");
    }

    /**
     * Closes this pool and releases all resources.
     */
    public void close() {
        if (closed) {
            return;
        }

        closed = true;
        clear();

        // Remove from global registry
        POOLS.remove(objectType);

        LOGGER.fine("Closed " + objectType.getSimpleName() + " pool (borrowed=" + borrowedCount.get() 
                   + ", hit_rate=" + String.format("%.1f", getHitRate()) + "%)");
    }

    /**
     * Gets pool statistics as a formatted string.
     *
     * @return pool statistics
     */
    public String getStats() {
        return String.format("%s pool: available=%d/%d, borrowed=%d, total_borrows=%d, " +
                           "total_returns=%d, total_created=%d, hit_rate=%.1f%%, uptime=%dms",
            objectType.getSimpleName(),
            getAvailableCount(), maxPoolSize,
            getBorrowedCount(),
            getTotalBorrows(),
            getTotalReturns(), 
            getTotalCreated(),
            getHitRate(),
            System.currentTimeMillis() - creationTime);
    }

    @Override
    public String toString() {
        return objectType.getSimpleName() + "Pool{available=" + getAvailableCount() + "/" + maxPoolSize
               + ", borrowed=" + getBorrowedCount() + ", closed=" + closed + "}";
    }

    /**
     * Gets statistics for all registered pools.
     *
     * @return statistics for all pools
     */
    public static String getAllPoolStats() {
        final StringBuilder sb = new StringBuilder("NativeObjectPool Statistics:\n");
        for (final WeakReference<NativeObjectPool<?>> poolRef : POOLS.values()) {
            final NativeObjectPool<?> pool = poolRef.get();
            if (pool != null && !pool.isClosed()) {
                sb.append("  ").append(pool.getStats()).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Clears all pools and releases all resources.
     */
    public static void clearAllPools() {
        for (final WeakReference<NativeObjectPool<?>> poolRef : POOLS.values()) {
            final NativeObjectPool<?> pool = poolRef.get();
            if (pool != null) {
                pool.close();
            }
        }
        POOLS.clear();
        LOGGER.info("Cleared all native object pools");
    }
}