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

package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.config.Serializer;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Panama FFI implementation of the Serializer interface.
 *
 * <p>This serializer provides efficient serialization and deserialization of compiled WebAssembly
 * modules with Java-side caching support. Unlike the JNI implementation, cache management is
 * handled in Java rather than native code.
 *
 * @since 1.0.0
 */
public final class PanamaSerializer implements Serializer {

  private final PanamaWasmRuntime runtime;
  private final long maxCacheSize;
  private final boolean enableCompression;
  private final int compressionLevel;

  private final Map<CacheKey, CacheEntry> cache;
  private final AtomicLong currentCacheSize;
  private final AtomicInteger cacheHits;
  private final AtomicInteger cacheMisses;

  private volatile boolean closed;

  /**
   * Creates a new serializer with default configuration.
   *
   * @param runtime the Panama runtime instance
   */
  public PanamaSerializer(final PanamaWasmRuntime runtime) {
    this(runtime, 0L, false, 6);
  }

  /**
   * Creates a new serializer with custom configuration.
   *
   * @param runtime the Panama runtime instance
   * @param maxCacheSize the maximum cache size in bytes (0 for unlimited)
   * @param enableCompression whether to enable compression (currently unused)
   * @param compressionLevel the compression level 0-9 (currently unused)
   */
  public PanamaSerializer(
      final PanamaWasmRuntime runtime,
      final long maxCacheSize,
      final boolean enableCompression,
      final int compressionLevel) {
    PanamaValidation.requireNonNull(runtime, "runtime");
    if (maxCacheSize < 0) {
      throw new IllegalArgumentException("Max cache size cannot be negative");
    }
    if (compressionLevel < 0 || compressionLevel > 9) {
      throw new IllegalArgumentException("Compression level must be between 0 and 9");
    }

    this.runtime = runtime;
    this.maxCacheSize = maxCacheSize;
    this.enableCompression = enableCompression;
    this.compressionLevel = compressionLevel;
    this.cache = new HashMap<>();
    this.currentCacheSize = new AtomicLong(0);
    this.cacheHits = new AtomicInteger(0);
    this.cacheMisses = new AtomicInteger(0);
    this.closed = false;
  }

  @Override
  public byte[] serialize(final Engine engine, final byte[] moduleBytes) throws WasmException {
    ensureNotClosed();
    PanamaValidation.requireNonNull(engine, "engine");
    PanamaValidation.requireNonNull(moduleBytes, "moduleBytes");

    if (!(engine instanceof PanamaEngine)) {
      throw new IllegalArgumentException("Engine must be a PanamaEngine for Panama serializer");
    }

    // Check cache first
    final CacheKey key = new CacheKey(engine, moduleBytes);
    synchronized (cache) {
      final CacheEntry entry = cache.get(key);
      if (entry != null) {
        cacheHits.incrementAndGet();
        return entry.serializedBytes.clone();
      }
    }

    cacheMisses.incrementAndGet();

    // Compile and serialize the module
    final PanamaEngine panamaEngine = (PanamaEngine) engine;
    final Module module = panamaEngine.compileModule(moduleBytes);

    try {
      if (!(module instanceof PanamaModule)) {
        throw new WasmException("Module must be a PanamaModule");
      }

      final PanamaModule panamaModule = (PanamaModule) module;
      final byte[] serializedBytes = panamaModule.serialize();

      // Add to cache if within size limits
      if (maxCacheSize == 0 || currentCacheSize.get() + serializedBytes.length <= maxCacheSize) {
        synchronized (cache) {
          cache.put(key, new CacheEntry(serializedBytes.clone()));
          currentCacheSize.addAndGet(serializedBytes.length);
        }
      }

      return serializedBytes;
    } finally {
      module.close();
    }
  }

  @Override
  public Module deserialize(final Engine engine, final byte[] serializedBytes)
      throws WasmException {
    ensureNotClosed();
    PanamaValidation.requireNonNull(engine, "engine");
    PanamaValidation.requireNonNull(serializedBytes, "serializedBytes");

    if (!(engine instanceof PanamaEngine)) {
      throw new IllegalArgumentException("Engine must be a PanamaEngine for Panama serializer");
    }

    return runtime.deserializeModule(engine, serializedBytes);
  }

  @Override
  public boolean clearCache() throws WasmException {
    ensureNotClosed();

    synchronized (cache) {
      if (cache.isEmpty()) {
        return false;
      }

      cache.clear();
      currentCacheSize.set(0);
      cacheHits.set(0);
      cacheMisses.set(0);
      return true;
    }
  }

  @Override
  public int getCacheEntryCount() {
    ensureNotClosed();
    synchronized (cache) {
      return cache.size();
    }
  }

  @Override
  public long getCacheTotalSize() {
    ensureNotClosed();
    return currentCacheSize.get();
  }

  @Override
  public double getCacheHitRate() {
    ensureNotClosed();
    final int hits = cacheHits.get();
    final int misses = cacheMisses.get();
    final int total = hits + misses;

    if (total == 0) {
      return 0.0;
    }

    return (double) hits / total;
  }

  @Override
  public void close() {
    if (!closed) {
      closed = true;
      synchronized (cache) {
        cache.clear();
        currentCacheSize.set(0);
      }
    }
  }

  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Serializer has been closed");
    }
  }

  /** Cache key based on engine configuration and module bytes. */
  private static final class CacheKey {
    private final long engineHandle;
    private final int moduleBytesHash;

    CacheKey(final Engine engine, final byte[] moduleBytes) {
      if (engine instanceof PanamaEngine) {
        this.engineHandle = ((PanamaEngine) engine).getNativeEngine().address();
      } else {
        this.engineHandle = 0;
      }
      this.moduleBytesHash = Arrays.hashCode(moduleBytes);
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof CacheKey)) {
        return false;
      }
      final CacheKey other = (CacheKey) obj;
      return engineHandle == other.engineHandle && moduleBytesHash == other.moduleBytesHash;
    }

    @Override
    public int hashCode() {
      return Long.hashCode(engineHandle) * 31 + moduleBytesHash;
    }
  }

  /** Cache entry containing serialized module bytes. */
  private static final class CacheEntry {
    private final byte[] serializedBytes;

    CacheEntry(final byte[] serializedBytes) {
      this.serializedBytes = serializedBytes;
    }
  }
}
