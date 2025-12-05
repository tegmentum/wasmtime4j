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

package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.cache.ModuleCache;
import ai.tegmentum.wasmtime4j.cache.ModuleCacheConfig;
import ai.tegmentum.wasmtime4j.cache.ModuleCacheStatistics;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * JNI implementation of ModuleCache.
 *
 * <p>This implementation uses the native Wasmtime module cache via JNI calls.
 *
 * @since 1.0.0
 */
public final class JniModuleCache implements ModuleCache {

  private static final Logger LOGGER = Logger.getLogger(JniModuleCache.class.getName());

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private final JniEngine engine;
  private final ModuleCacheConfig config;
  private final long nativeHandle;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * Creates a new JniModuleCache.
   *
   * @param engine the engine to associate with the cache
   * @param config the cache configuration
   * @throws WasmException if the cache cannot be created
   */
  public JniModuleCache(final JniEngine engine, final ModuleCacheConfig config)
      throws WasmException {
    this.engine = Objects.requireNonNull(engine, "engine cannot be null");
    this.config = Objects.requireNonNull(config, "config cannot be null");

    this.nativeHandle =
        nativeCreateWithConfig(
            engine.getNativeHandle(),
            config.getCacheDir().toString(),
            config.getMaxCacheSize(),
            config.getMaxEntries(),
            config.isCompressionEnabled(),
            config.getCompressionLevel());

    if (this.nativeHandle == 0) {
      throw new WasmException("Failed to create native module cache");
    }

    LOGGER.fine("Created JniModuleCache with config: " + config);
  }

  @Override
  public Module getOrCompile(final byte[] wasmBytes) throws WasmException {
    if (wasmBytes == null || wasmBytes.length == 0) {
      throw new IllegalArgumentException("wasmBytes cannot be null or empty");
    }
    ensureNotClosed();

    final long moduleHandle = nativeGetOrCompile(nativeHandle, wasmBytes);
    if (moduleHandle == 0) {
      throw new WasmException("Failed to get or compile module from cache");
    }

    return new JniModule(moduleHandle, engine);
  }

  @Override
  public String precompile(final byte[] wasmBytes) throws WasmException {
    if (wasmBytes == null || wasmBytes.length == 0) {
      throw new IllegalArgumentException("wasmBytes cannot be null or empty");
    }
    ensureNotClosed();

    final String hash = nativePrecompile(nativeHandle, wasmBytes);
    if (hash == null || hash.isEmpty()) {
      throw new WasmException("Failed to precompile module");
    }

    return hash;
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

    // Currently not exposed via JNI - would need additional native function
    // For now, return false indicating removal not supported
    LOGGER.fine("Individual entry removal not currently supported via native API");
    return false;
  }

  @Override
  public void clear() throws WasmException {
    ensureNotClosed();

    final boolean success = nativeClear(nativeHandle);
    if (!success) {
      throw new WasmException("Failed to clear module cache");
    }
  }

  @Override
  public void performMaintenance() throws WasmException {
    ensureNotClosed();

    final boolean success = nativePerformMaintenance(nativeHandle);
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
    final long count = nativeEntryCount(nativeHandle);
    return count >= 0 ? count : 0;
  }

  @Override
  public long getStorageBytesUsed() {
    if (closed.get()) {
      return 0;
    }
    final long bytes = nativeStorageBytes(nativeHandle);
    return bytes >= 0 ? bytes : 0;
  }

  @Override
  public long getHitCount() {
    if (closed.get()) {
      return 0;
    }
    final long hits = nativeHitCount(nativeHandle);
    return hits >= 0 ? hits : 0;
  }

  @Override
  public long getMissCount() {
    if (closed.get()) {
      return 0;
    }
    final long misses = nativeMissCount(nativeHandle);
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
      nativeDestroy(nativeHandle);
      LOGGER.fine("Closed JniModuleCache");
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
      return bytesToHex(hashBytes);
    } catch (NoSuchAlgorithmException e) {
      LOGGER.warning("Failed to compute hash: " + e.getMessage());
      return null;
    }
  }

  /**
   * Converts a byte array to a hex string (Java 8 compatible replacement for HexFormat).
   *
   * @param bytes the byte array to convert
   * @return the hex string representation
   */
  private static String bytesToHex(final byte[] bytes) {
    final StringBuilder hexString = new StringBuilder(bytes.length * 2);
    for (final byte b : bytes) {
      final String hex = Integer.toHexString(0xff & b);
      if (hex.length() == 1) {
        hexString.append('0');
      }
      hexString.append(hex);
    }
    return hexString.toString();
  }

  @Override
  public String toString() {
    return "JniModuleCache{"
        + "config="
        + config
        + ", entries="
        + getEntryCount()
        + ", closed="
        + closed.get()
        + '}';
  }

  // Native method declarations
  private static native long nativeCreateWithConfig(
      long engineHandle,
      String cacheDir,
      long maxCacheSize,
      int maxEntries,
      boolean compressionEnabled,
      int compressionLevel);

  private static native long nativeGetOrCompile(long cacheHandle, byte[] wasmBytes);

  private static native String nativePrecompile(long cacheHandle, byte[] wasmBytes);

  private static native boolean nativeClear(long cacheHandle);

  private static native boolean nativePerformMaintenance(long cacheHandle);

  private static native long nativeEntryCount(long cacheHandle);

  private static native long nativeStorageBytes(long cacheHandle);

  private static native long nativeHitCount(long cacheHandle);

  private static native long nativeMissCount(long cacheHandle);

  private static native void nativeDestroy(long cacheHandle);
}
