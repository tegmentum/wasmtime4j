/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.cache;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.Closeable;

/**
 * WebAssembly module compilation cache interface.
 *
 * <p>A ModuleCache provides persistent caching for compiled WebAssembly modules, dramatically
 * reducing startup time for applications that repeatedly load the same modules. The cache stores
 * compiled module artifacts on disk and reuses them when the same module bytecode is requested.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Content-addressed caching using SHA-256 hashes of module bytecode
 *   <li>Optional compression using zstd for reduced storage footprint
 *   <li>LRU eviction policy for managing cache size
 *   <li>Thread-safe operation for concurrent access
 *   <li>Support for precompilation and warming
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Engine engine = WasmRuntimeFactory.createEngine();
 * ModuleCacheConfig config = ModuleCacheConfig.builder()
 *     .cacheDir(Paths.get("/var/cache/wasm"))
 *     .maxCacheSize(1024 * 1024 * 1024) // 1 GB
 *     .compressionEnabled(true)
 *     .build();
 *
 * try (ModuleCache cache = ModuleCacheFactory.create(engine, config)) {
 *     // First load compiles and caches
 *     Module module1 = cache.getOrCompile(wasmBytes);
 *
 *     // Second load retrieves from cache
 *     Module module2 = cache.getOrCompile(wasmBytes);
 *
 *     System.out.println(cache.getStatistics());
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface ModuleCache extends Closeable {

  /**
   * Gets a module from the cache or compiles it if not present.
   *
   * <p>This is the primary method for using the cache. If the module bytecode has been previously
   * compiled and cached, the cached version is returned. Otherwise, the bytecode is compiled, the
   * result is cached, and the compiled module is returned.
   *
   * @param wasmBytes the WebAssembly bytecode to compile
   * @return the compiled module (possibly from cache)
   * @throws WasmException if compilation fails
   * @throws IllegalArgumentException if wasmBytes is null or empty
   */
  Module getOrCompile(final byte[] wasmBytes) throws WasmException;

  /**
   * Pre-compiles and caches a module without returning it.
   *
   * <p>This method is useful for warming the cache ahead of time. The module is compiled and stored
   * in the cache, but not instantiated.
   *
   * @param wasmBytes the WebAssembly bytecode to precompile
   * @return the cache key (hash) for the precompiled module
   * @throws WasmException if precompilation fails
   * @throws IllegalArgumentException if wasmBytes is null or empty
   */
  String precompile(final byte[] wasmBytes) throws WasmException;

  /**
   * Checks if a module is present in the cache.
   *
   * @param wasmBytes the WebAssembly bytecode to check
   * @return true if the module is cached, false otherwise
   * @throws IllegalArgumentException if wasmBytes is null or empty
   */
  boolean contains(final byte[] wasmBytes);

  /**
   * Removes a module from the cache.
   *
   * @param wasmBytes the WebAssembly bytecode to remove
   * @return true if the module was removed, false if it wasn't in the cache
   * @throws IllegalArgumentException if wasmBytes is null or empty
   */
  boolean remove(final byte[] wasmBytes);

  /**
   * Clears all entries from the cache.
   *
   * <p>This method removes all cached modules from both memory and persistent storage.
   *
   * @throws WasmException if the clear operation fails
   */
  void clear() throws WasmException;

  /**
   * Performs cache maintenance tasks.
   *
   * <p>This method performs eviction of old entries, cleanup of orphaned files, and other
   * maintenance tasks. It is automatically called periodically, but can be invoked manually if
   * needed.
   *
   * @throws WasmException if maintenance fails
   */
  void performMaintenance() throws WasmException;

  /**
   * Gets cache statistics.
   *
   * @return current cache statistics
   */
  ModuleCacheStatistics getStatistics();

  /**
   * Gets the number of entries currently in the cache.
   *
   * @return entry count
   */
  long getEntryCount();

  /**
   * Gets the total storage bytes used by the cache.
   *
   * @return storage bytes used
   */
  long getStorageBytesUsed();

  /**
   * Gets the cache hit count.
   *
   * @return number of cache hits
   */
  long getHitCount();

  /**
   * Gets the cache miss count.
   *
   * @return number of cache misses
   */
  long getMissCount();

  /**
   * Gets the engine associated with this cache.
   *
   * @return the engine
   */
  Engine getEngine();

  /**
   * Gets the configuration for this cache.
   *
   * @return the cache configuration
   */
  ModuleCacheConfig getConfig();

  /**
   * Closes the cache and releases resources.
   *
   * <p>After closing, the cache cannot be used. Cached modules remain on disk and can be used when
   * a new cache is created with the same configuration.
   */
  @Override
  void close();
}
