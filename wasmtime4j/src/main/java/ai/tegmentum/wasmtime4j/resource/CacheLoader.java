package ai.tegmentum.wasmtime4j.resource;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for loading and refreshing cache entries.
 *
 * <p>CacheLoader provides a way to automatically populate cache entries when they are not present
 * or need to be refreshed. This enables transparent caching where the cache can automatically load
 * missing entries on demand.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * CacheLoader moduleLoader = new CacheLoader() {
 *     @Override
 *     public Object load(String key, ResourceType type) throws WasmException {
 *         if (type == ResourceType.MODULE) {
 *             // Load WebAssembly module from file system
 *             byte[] wasmBytes = Files.readAllBytes(Paths.get(key));
 *             return engine.compileModule(wasmBytes);
 *         }
 *         throw new WasmException("Unsupported resource type: " + type);
 *     }
 * };
 *
 * cache.preload(moduleLoader);
 * }</pre>
 *
 * @since 1.0.0
 */
public interface CacheLoader {

  /**
   * Loads a resource for the given key and type.
   *
   * <p>This method is called when a cache entry is not present or needs to be refreshed. The
   * implementation should create and return the appropriate resource for the given key and type.
   *
   * @param key the cache key to load
   * @param type the resource type to load
   * @return the loaded resource
   * @throws WasmException if loading fails
   * @throws IllegalArgumentException if key is null/empty or type is null
   */
  Object load(final String key, final ResourceType type) throws WasmException;

  /**
   * Asynchronously loads a resource for the given key and type.
   *
   * <p>The default implementation calls {@link #load(String, ResourceType)} on a background thread.
   * Implementations can override this to provide truly asynchronous loading.
   *
   * @param key the cache key to load
   * @param type the resource type to load
   * @return future containing the loaded resource
   * @throws IllegalArgumentException if key is null/empty or type is null
   */
  default CompletableFuture<Object> loadAsync(final String key, final ResourceType type) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return load(key, type);
          } catch (WasmException e) {
            throw new RuntimeException("Failed to load resource: " + key, e);
          }
        });
  }

  /**
   * Checks if this loader can handle the given resource type.
   *
   * <p>The default implementation returns true for all types. Implementations can override this to
   * specify which resource types they support.
   *
   * @param type the resource type to check
   * @return true if this loader can handle the type
   * @throws IllegalArgumentException if type is null
   */
  default boolean canLoad(final ResourceType type) {
    if (type == null) {
      throw new IllegalArgumentException("Resource type cannot be null");
    }
    return true;
  }

  /**
   * Gets the cache policy to use for loaded resources.
   *
   * <p>The default implementation returns the default cache policy. Implementations can override
   * this to specify custom policies for loaded resources.
   *
   * @param key the cache key
   * @param type the resource type
   * @return the cache policy to use
   * @throws IllegalArgumentException if key is null/empty or type is null
   */
  default CachePolicy getLoadPolicy(final String key, final ResourceType type) {
    if (key == null || key.trim().isEmpty()) {
      throw new IllegalArgumentException("Key cannot be null or empty");
    }
    if (type == null) {
      throw new IllegalArgumentException("Resource type cannot be null");
    }
    return CachePolicy.defaultPolicy();
  }

  /**
   * Estimates the size of a resource before loading it.
   *
   * <p>This can be used by the cache to make eviction decisions before actually loading the
   * resource. The default implementation returns -1 to indicate that the size is unknown.
   *
   * @param key the cache key
   * @param type the resource type
   * @return the estimated size in bytes, or -1 if unknown
   * @throws IllegalArgumentException if key is null/empty or type is null
   */
  default long estimateSize(final String key, final ResourceType type) {
    if (key == null || key.trim().isEmpty()) {
      throw new IllegalArgumentException("Key cannot be null or empty");
    }
    if (type == null) {
      throw new IllegalArgumentException("Resource type cannot be null");
    }
    return -1;
  }

  /**
   * Called when a loaded resource is evicted from the cache.
   *
   * <p>This allows the loader to perform cleanup operations or update external state when resources
   * are removed from the cache. The default implementation does nothing.
   *
   * @param key the cache key that was evicted
   * @param resource the resource that was evicted
   * @param type the resource type
   */
  default void onEviction(final String key, final Object resource, final ResourceType type) {
    // Default implementation does nothing
  }

  /**
   * Called when loading a resource fails.
   *
   * <p>This allows the loader to perform error handling, logging, or fallback operations when
   * resource loading fails. The default implementation does nothing.
   *
   * @param key the cache key that failed to load
   * @param type the resource type
   * @param cause the cause of the loading failure
   */
  default void onLoadFailure(final String key, final ResourceType type, final Throwable cause) {
    // Default implementation does nothing
  }

  /**
   * Creates a simple cache loader that uses a supplier function.
   *
   * @param <T> the resource type
   * @param resourceType the resource type this loader handles
   * @param supplier the supplier function
   * @return a cache loader that uses the supplier
   * @throws IllegalArgumentException if resourceType or supplier is null
   */
  static <T> CacheLoader of(
      final ResourceType resourceType, final java.util.function.Supplier<T> supplier) {
    if (resourceType == null) {
      throw new IllegalArgumentException("Resource type cannot be null");
    }
    if (supplier == null) {
      throw new IllegalArgumentException("Supplier cannot be null");
    }

    return new CacheLoader() {
      @Override
      public Object load(final String key, final ResourceType type) throws WasmException {
        if (type != resourceType) {
          throw new WasmException(
              "Unsupported resource type: " + type + ", expected: " + resourceType);
        }
        try {
          return supplier.get();
        } catch (Exception e) {
          throw new WasmException("Failed to load resource: " + key, e);
        }
      }

      @Override
      public boolean canLoad(final ResourceType type) {
        return type == resourceType;
      }
    };
  }

  /**
   * Creates a cache loader that uses a function to load resources by key.
   *
   * @param <T> the resource type
   * @param resourceType the resource type this loader handles
   * @param loadFunction the function that loads resources by key
   * @return a cache loader that uses the function
   * @throws IllegalArgumentException if resourceType or loadFunction is null
   */
  static <T> CacheLoader of(
      final ResourceType resourceType, final java.util.function.Function<String, T> loadFunction) {
    if (resourceType == null) {
      throw new IllegalArgumentException("Resource type cannot be null");
    }
    if (loadFunction == null) {
      throw new IllegalArgumentException("Load function cannot be null");
    }

    return new CacheLoader() {
      @Override
      public Object load(final String key, final ResourceType type) throws WasmException {
        if (type != resourceType) {
          throw new WasmException(
              "Unsupported resource type: " + type + ", expected: " + resourceType);
        }
        try {
          return loadFunction.apply(key);
        } catch (Exception e) {
          throw new WasmException("Failed to load resource: " + key, e);
        }
      }

      @Override
      public boolean canLoad(final ResourceType type) {
        return type == resourceType;
      }
    };
  }

  /**
   * Creates a cache loader that always throws an exception.
   *
   * <p>This can be useful for testing or when automatic loading should not be allowed.
   *
   * @param message the exception message
   * @return a cache loader that always fails
   */
  static CacheLoader failing(final String message) {
    return new CacheLoader() {
      @Override
      public Object load(final String key, final ResourceType type) throws WasmException {
        throw new WasmException(message != null ? message : "Loading not supported");
      }

      @Override
      public boolean canLoad(final ResourceType type) {
        return false;
      }
    };
  }
}
