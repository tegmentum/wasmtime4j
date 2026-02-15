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

package ai.tegmentum.wasmtime4j.jni.wasi.http;

import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.JniLibraryLoader;
import ai.tegmentum.wasmtime4j.wasi.http.WasiHttpConfig;
import ai.tegmentum.wasmtime4j.wasi.http.WasiHttpContext;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * JNI implementation of {@link WasiHttpContext}.
 *
 * <p>This class wraps the native WASI HTTP context and provides HTTP capabilities to WebAssembly
 * modules. It manages the lifecycle of the native context and ensures proper resource cleanup.
 *
 * @since 1.0.0
 */
public final class JniWasiHttpContext implements WasiHttpContext {

  private static final Logger LOGGER = Logger.getLogger(JniWasiHttpContext.class.getName());
  private static final AtomicLong CONTEXT_ID_GENERATOR = new AtomicLong(0);

  private final WasiHttpConfig config;
  private final long contextHandle;
  private final long contextId;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * Creates a new JniWasiHttpContext with the specified configuration.
   *
   * @param config the HTTP configuration
   * @throws WasmException if the context cannot be created
   * @throws IllegalArgumentException if config is null
   */
  public JniWasiHttpContext(final WasiHttpConfig config) throws WasmException {
    this.config = Objects.requireNonNull(config, "config cannot be null");
    this.contextId = CONTEXT_ID_GENERATOR.incrementAndGet();

    // Ensure native library is loaded
    JniLibraryLoader.ensureLoaded();

    try {
      this.contextHandle = nativeCreate(config);
      if (this.contextHandle == 0) {
        throw new WasmException("Failed to create native WASI HTTP context");
      }
      LOGGER.fine("Created WASI HTTP context: " + contextId);
    } catch (final WasmException e) {
      throw e;
    } catch (final Exception e) {
      throw new WasmException("Failed to create WASI HTTP context: " + e.getMessage(), e);
    }
  }

  @Override
  public void addToLinker(final Linker<?> linker, final Store store) throws WasmException {
    if (linker == null) {
      throw new IllegalArgumentException("linker cannot be null");
    }
    if (store == null) {
      throw new IllegalArgumentException("store cannot be null");
    }
    if (closed.get()) {
      throw new WasmException("WASI HTTP context has been closed");
    }

    // The native layer handles adding WASI HTTP interfaces to the linker
    // This is a placeholder - actual implementation would integrate with
    // the wasmtime-wasi-http crate's add_to_linker functionality
    LOGGER.info("WASI HTTP context added to linker (context ID: " + contextId + ")");
  }

  @Override
  public WasiHttpConfig getConfig() {
    return config;
  }

  @Override
  public boolean isValid() {
    if (closed.get()) {
      return false;
    }
    return nativeIsValid(contextHandle);
  }

  @Override
  public boolean isHostAllowed(final String host) {
    if (host == null) {
      throw new IllegalArgumentException("host cannot be null");
    }
    if (closed.get()) {
      return false;
    }

    // Check blocked hosts first (they take precedence)
    final Set<String> blockedHosts = config.getBlockedHosts();
    if (matchesAnyPattern(host, blockedHosts)) {
      return false;
    }

    // Check allowed hosts
    final Set<String> allowedHosts = config.getAllowedHosts();
    if (allowedHosts.isEmpty()) {
      return false;
    }

    return matchesAnyPattern(host, allowedHosts);
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      nativeFree(contextHandle);
      LOGGER.fine("Closed WASI HTTP context: " + contextId);
    }
  }

  /**
   * Returns the native handle for this context.
   *
   * @return the native handle
   */
  public long getNativeHandle() {
    return contextHandle;
  }

  /**
   * Returns the context ID.
   *
   * @return the context ID
   */
  public long getContextId() {
    return contextId;
  }

  @Override
  public String toString() {
    return "JniWasiHttpContext{"
        + "id="
        + contextId
        + ", valid="
        + isValid()
        + ", closed="
        + closed.get()
        + '}';
  }

  private boolean matchesAnyPattern(final String host, final Set<String> patterns) {
    for (final String pattern : patterns) {
      if (matchesPattern(host, pattern)) {
        return true;
      }
    }
    return false;
  }

  private boolean matchesPattern(final String host, final String pattern) {
    if (pattern.equals("*")) {
      return true;
    }
    if (pattern.startsWith("*.")) {
      // Wildcard pattern like *.example.com
      final String suffix = pattern.substring(1);
      return host.endsWith(suffix) || host.equals(pattern.substring(2));
    }
    // Exact match
    return host.equalsIgnoreCase(pattern);
  }

  // Native methods
  private static native long nativeCreate(WasiHttpConfig config);

  private static native boolean nativeIsValid(long contextHandle);

  private static native void nativeFree(long contextHandle);
}
