/*
 * Copyright 2024 Tegmentum AI
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

import ai.tegmentum.wasmtime4j.nativeloader.NativeLibraryUtils;
import ai.tegmentum.wasmtime4j.nativeloader.NativeLoader;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Native library loader for Panama FFI implementation.
 *
 * <p>This class handles native library loading and function discovery for the wasmtime4j-panama
 * module. It uses the shared {@link NativeLibraryUtils} for platform detection and library loading,
 * while providing Panama-specific functionality for function symbol lookup and resource management
 * using Arena-based cleanup.
 */
public final class NativeLibraryLoader {

  private static final Logger LOGGER = Logger.getLogger(NativeLibraryLoader.class.getName());

  // Singleton instance
  private static volatile NativeLibraryLoader instance;
  private static final Object INSTANCE_LOCK = new Object();

  // Internal state
  private final NativeLibraryUtils.LibraryLoadInfo loadInfo;
  private final SymbolLookup symbolLookup;
  private final Arena libraryArena;
  private final ConcurrentHashMap<String, MethodHandle> methodHandleCache;

  /**
   * Private constructor for singleton pattern.
   *
   * @throws IllegalStateException if library loading fails
   */
  private NativeLibraryLoader() {
    this.libraryArena = Arena.ofShared();
    this.methodHandleCache = new ConcurrentHashMap<>();

    // Load the native library using shared utilities
    this.loadInfo = NativeLoader.loadLibrary("wasmtime4j");

    if (!loadInfo.isSuccessful()) {
      final String errorMessage = loadInfo.getErrorMessage();
      final RuntimeException cause =
          errorMessage != null
              ? new RuntimeException(errorMessage)
              : new RuntimeException("Unknown error");
      LOGGER.log(Level.SEVERE, "Failed to load native library for Panama: " + loadInfo, cause);
      throw new IllegalStateException("Failed to load native library for Panama FFI", cause);
    }

    // Initialize symbol lookup using the loaded library path
    // Use libraryLookup() with the path - this sees all symbols unlike loaderLookup()
    try {
      final java.nio.file.Path libraryPath = loadInfo.getExtractedPath();
      System.err.println("[LOADER] Loading library from path: " + libraryPath);
      System.err.println("[LOADER] Library exists: " + java.nio.file.Files.exists(libraryPath));
      System.err.flush();
      this.symbolLookup = SymbolLookup.libraryLookup(libraryPath, libraryArena);
      System.err.println("[LOADER] SymbolLookup created successfully");
      System.err.flush();
      LOGGER.info("Successfully loaded native library for Panama: " + loadInfo);
    } catch (Exception e) {
      LOGGER.log(
          Level.SEVERE,
          "Failed to create symbol lookup for library: " + loadInfo.getExtractedPath(),
          e);
      throw new IllegalStateException("Failed to create symbol lookup for Panama FFI", e);
    }
  }

  /**
   * Gets the singleton instance of the native library loader.
   *
   * @return the singleton instance
   * @throws IllegalStateException if library loading failed
   */
  public static NativeLibraryLoader getInstance() {
    NativeLibraryLoader result = instance;
    if (result == null) {
      synchronized (INSTANCE_LOCK) {
        result = instance;
        if (result == null) {
          try {
            instance = result = new NativeLibraryLoader();
          } catch (IllegalStateException e) {
            LOGGER.severe("Failed to create NativeLibraryLoader singleton: " + e.getMessage());
            // Don't store failed instance, allow retry
            throw e;
          }
        }
      }
    }
    return result;
  }

  /**
   * Checks if the native library is loaded and available.
   *
   * @return true if loaded successfully, false otherwise
   */
  public boolean isLoaded() {
    return loadInfo != null && loadInfo.isSuccessful();
  }

  /**
   * Gets the loading error if library loading failed.
   *
   * @return the loading error, or null if no error occurred
   */
  public Optional<String> getLoadingError() {
    return loadInfo != null ? Optional.ofNullable(loadInfo.getErrorMessage()) : Optional.empty();
  }

  /**
   * Gets information about the library loading attempt.
   *
   * @return the load info
   */
  public NativeLibraryUtils.LibraryLoadInfo getLoadInfo() {
    return loadInfo;
  }

  /**
   * Looks up a function symbol and returns a MethodHandle.
   *
   * @param functionName the name of the function to look up
   * @param descriptor the function descriptor defining signature
   * @return optional containing the MethodHandle, or empty if not found
   */
  public Optional<MethodHandle> lookupFunction(
      final String functionName, final FunctionDescriptor descriptor) {
    if (!isLoaded() || symbolLookup == null) {
      LOGGER.warning("Attempted to lookup function before library was loaded: " + functionName);
      return Optional.empty();
    }

    // Check cache first
    String cacheKey = functionName + "_" + descriptor.hashCode();
    MethodHandle cached = methodHandleCache.get(cacheKey);
    if (cached != null) {
      return Optional.of(cached);
    }

    try {
      // libraryLookup() handles platform-specific symbol naming internally
      Optional<MemorySegment> symbol = symbolLookup.find(functionName);

      if (symbol.isEmpty()) {
        LOGGER.warning("Function symbol not found: " + functionName);
        System.err.println("[LOADER] Symbol NOT found: " + functionName);
        System.err.flush();
        return Optional.empty();
      }

      System.err.println(
          "[LOADER] Symbol found: "
              + functionName
              + " at address 0x"
              + Long.toHexString(symbol.get().address()));
      System.err.println("[LOADER] Descriptor: " + descriptor);
      System.err.flush();

      System.err.println("[LOADER] Creating downcall handle...");
      System.err.flush();
      Linker linker = Linker.nativeLinker();
      MethodHandle handle = linker.downcallHandle(symbol.get(), descriptor);

      System.err.println("[LOADER] Created downcall handle for: " + functionName);
      System.err.flush();

      // Cache the handle for future use
      methodHandleCache.put(cacheKey, handle);

      LOGGER.fine("Successfully looked up function: " + functionName);
      return Optional.of(handle);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to lookup function: " + functionName, e);
      return Optional.empty();
    }
  }

  /**
   * Gets the SymbolLookup for the loaded native library.
   *
   * @return the symbol lookup
   * @throws IllegalStateException if the library is not loaded
   */
  public SymbolLookup getSymbolLookup() {
    if (!isLoaded() || symbolLookup == null) {
      throw new IllegalStateException("Native library not loaded");
    }
    return symbolLookup;
  }

  /**
   * Gets the Arena used for library resource management.
   *
   * @return the library arena
   */
  public Arena getLibraryArena() {
    return libraryArena;
  }

  /** Clears the method handle cache. */
  public void clearMethodHandleCache() {
    methodHandleCache.clear();
    LOGGER.fine("Cleared method handle cache");
  }

  /** Closes the library and cleans up resources. This should only be called during JVM shutdown. */
  public void close() {
    try {
      clearMethodHandleCache();
      if (libraryArena != null && libraryArena.scope().isAlive()) {
        libraryArena.close();
      }
      LOGGER.info("Closed Panama native library loader");
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Error during Panama library loader shutdown", e);
    }
  }

  /**
   * Gets diagnostic information about the Panama library loader.
   *
   * @return diagnostic information string
   */
  public String getDiagnosticInfo() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Panama Native Library Status:\n");
    sb.append("  Loaded: ").append(isLoaded()).append("\n");
    sb.append("  Load info: ").append(loadInfo).append("\n");
    sb.append("  Cached method handles: ").append(methodHandleCache.size()).append("\n");
    sb.append("  Arena alive: ")
        .append(libraryArena != null && libraryArena.scope().isAlive())
        .append("\n");
    sb.append(NativeLibraryUtils.getDiagnosticInfo());
    return sb.toString();
  }
}
