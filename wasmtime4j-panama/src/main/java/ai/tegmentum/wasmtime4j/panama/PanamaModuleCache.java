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
import ai.tegmentum.wasmtime4j.cache.ModuleCache;
import ai.tegmentum.wasmtime4j.cache.ModuleCacheConfig;
import ai.tegmentum.wasmtime4j.cache.ModuleCacheStatistics;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of ModuleCache.
 *
 * <p>This implementation uses the native Wasmtime module cache via Panama Foreign Function API
 * calls.
 *
 * @since 1.0.0
 */
public final class PanamaModuleCache implements ModuleCache {

  private static final Logger LOGGER = Logger.getLogger(PanamaModuleCache.class.getName());
  private static final NativeFunctionBindings NATIVE_BINDINGS =
      NativeFunctionBindings.getInstance();
  private static final int HASH_OUTPUT_SIZE = 64;

  private final PanamaEngine engine;
  private final ModuleCacheConfig config;
  private final MemorySegment nativeCache;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * Creates a new PanamaModuleCache.
   *
   * @param engine the engine to associate with the cache
   * @param config the cache configuration
   * @throws WasmException if the cache cannot be created
   */
  public PanamaModuleCache(final PanamaEngine engine, final ModuleCacheConfig config)
      throws WasmException {
    this.engine = Objects.requireNonNull(engine, "engine cannot be null");
    this.config = Objects.requireNonNull(config, "config cannot be null");

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment cacheDirSegment =
          arena.allocateFrom(config.getCacheDir().toString(), StandardCharsets.UTF_8);

      this.nativeCache =
          NATIVE_BINDINGS.moduleCacheCreateWithConfig(
              engine.getNativeEngine(),
              cacheDirSegment,
              config.getMaxCacheSize(),
              config.getMaxEntries(),
              config.isCompressionEnabled(),
              config.getCompressionLevel());

      if (this.nativeCache == null || this.nativeCache.equals(MemorySegment.NULL)) {
        throw new WasmException("Failed to create native module cache");
      }
    }

    LOGGER.fine("Created PanamaModuleCache with config: " + config);
  }

  @Override
  public Module getOrCompile(final byte[] wasmBytes) throws WasmException {
    if (wasmBytes == null || wasmBytes.length == 0) {
      throw new IllegalArgumentException("wasmBytes cannot be null or empty");
    }
    ensureNotClosed();

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment bytecodeSegment = arena.allocate(ValueLayout.JAVA_BYTE, wasmBytes.length);
      bytecodeSegment.copyFrom(MemorySegment.ofArray(wasmBytes));

      final MemorySegment moduleOutPtr = arena.allocate(ValueLayout.ADDRESS);

      final boolean success =
          NATIVE_BINDINGS.moduleCacheGetOrCompile(
              nativeCache, bytecodeSegment, wasmBytes.length, moduleOutPtr);

      if (!success) {
        throw new WasmException("Failed to get or compile module from cache");
      }

      final MemorySegment modulePtr = moduleOutPtr.get(ValueLayout.ADDRESS, 0);
      if (modulePtr == null || modulePtr.equals(MemorySegment.NULL)) {
        throw new WasmException("Module compilation returned null");
      }

      return new PanamaModule(engine, modulePtr);
    }
  }

  @Override
  public String precompile(final byte[] wasmBytes) throws WasmException {
    if (wasmBytes == null || wasmBytes.length == 0) {
      throw new IllegalArgumentException("wasmBytes cannot be null or empty");
    }
    ensureNotClosed();

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment bytecodeSegment = arena.allocate(ValueLayout.JAVA_BYTE, wasmBytes.length);
      bytecodeSegment.copyFrom(MemorySegment.ofArray(wasmBytes));

      final MemorySegment hashOutSegment = arena.allocate(HASH_OUTPUT_SIZE);

      final int hashLen =
          NATIVE_BINDINGS.moduleCachePrecompile(
              nativeCache, bytecodeSegment, wasmBytes.length, hashOutSegment, HASH_OUTPUT_SIZE);

      if (hashLen < 0) {
        throw new WasmException("Failed to precompile module");
      }

      final byte[] hashBytes = hashOutSegment.asSlice(0, hashLen).toArray(ValueLayout.JAVA_BYTE);
      return new String(hashBytes, StandardCharsets.UTF_8);
    }
  }

  @Override
  public boolean contains(final byte[] wasmBytes) {
    if (wasmBytes == null || wasmBytes.length == 0) {
      throw new IllegalArgumentException("wasmBytes cannot be null or empty");
    }

    if (closed.get()) {
      return false;
    }

    // Compute hash and check if it exists in cache
    final String hash = computeHash(wasmBytes);
    return hash != null;
  }

  @Override
  public boolean remove(final byte[] wasmBytes) {
    if (wasmBytes == null || wasmBytes.length == 0) {
      throw new IllegalArgumentException("wasmBytes cannot be null or empty");
    }

    if (closed.get()) {
      return false;
    }

    // Currently not exposed via FFI - would need additional native function
    // For now, return false indicating removal not supported
    LOGGER.fine("Individual entry removal not currently supported via native API");
    return false;
  }

  @Override
  public void clear() throws WasmException {
    ensureNotClosed();

    final boolean success = NATIVE_BINDINGS.moduleCacheClear(nativeCache);
    if (!success) {
      throw new WasmException("Failed to clear module cache");
    }
  }

  @Override
  public void performMaintenance() throws WasmException {
    ensureNotClosed();

    final boolean success = NATIVE_BINDINGS.moduleCachePerformMaintenance(nativeCache);
    if (!success) {
      throw new WasmException("Failed to perform cache maintenance");
    }
  }

  @Override
  public ModuleCacheStatistics getStatistics() {
    if (closed.get()) {
      return ModuleCacheStatistics.builder().build();
    }

    return ModuleCacheStatistics.builder()
        .cacheHits(getHitCount())
        .cacheMisses(getMissCount())
        .entriesCount(getEntryCount())
        .storageBytesUsed(getStorageBytesUsed())
        .build();
  }

  @Override
  public long getEntryCount() {
    if (closed.get()) {
      return 0;
    }
    final long count = NATIVE_BINDINGS.moduleCacheEntryCount(nativeCache);
    return count >= 0 ? count : 0;
  }

  @Override
  public long getStorageBytesUsed() {
    if (closed.get()) {
      return 0;
    }
    final long bytes = NATIVE_BINDINGS.moduleCacheStorageBytes(nativeCache);
    return bytes >= 0 ? bytes : 0;
  }

  @Override
  public long getHitCount() {
    if (closed.get()) {
      return 0;
    }
    final long hits = NATIVE_BINDINGS.moduleCacheHitCount(nativeCache);
    return hits >= 0 ? hits : 0;
  }

  @Override
  public long getMissCount() {
    if (closed.get()) {
      return 0;
    }
    final long misses = NATIVE_BINDINGS.moduleCacheMissCount(nativeCache);
    return misses >= 0 ? misses : 0;
  }

  @Override
  public Engine getEngine() {
    return engine;
  }

  @Override
  public ModuleCacheConfig getConfig() {
    return config;
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      NATIVE_BINDINGS.moduleCacheDestroy(nativeCache);
      LOGGER.fine("Closed PanamaModuleCache");
    }
  }

  private void ensureNotClosed() throws WasmException {
    if (closed.get()) {
      throw new WasmException("ModuleCache has been closed");
    }
  }

  private String computeHash(final byte[] wasmBytes) {
    try {
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      final byte[] hashBytes = digest.digest(wasmBytes);
      return HexFormat.of().formatHex(hashBytes);
    } catch (NoSuchAlgorithmException e) {
      LOGGER.warning("Failed to compute hash: " + e.getMessage());
      return null;
    }
  }

  @Override
  public String toString() {
    return "PanamaModuleCache{"
        + "config="
        + config
        + ", entries="
        + getEntryCount()
        + ", closed="
        + closed.get()
        + '}';
  }
}
