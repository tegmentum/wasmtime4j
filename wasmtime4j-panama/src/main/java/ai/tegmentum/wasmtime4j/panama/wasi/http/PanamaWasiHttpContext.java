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

package ai.tegmentum.wasmtime4j.panama.wasi.http;

import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.NativeFunctionBindings;
import ai.tegmentum.wasmtime4j.panama.PanamaLinker;
import ai.tegmentum.wasmtime4j.panama.PanamaStore;
import ai.tegmentum.wasmtime4j.wasi.http.WasiHttpConfig;
import ai.tegmentum.wasmtime4j.wasi.http.WasiHttpContext;
import ai.tegmentum.wasmtime4j.wasi.http.WasiHttpStats;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Panama implementation of {@link WasiHttpContext}.
 *
 * <p>This class wraps the native WASI HTTP context and provides HTTP capabilities to WebAssembly
 * modules. It manages the lifecycle of the native context and ensures proper resource cleanup.
 */
public final class PanamaWasiHttpContext implements WasiHttpContext {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiHttpContext.class.getName());

  private final WasiHttpConfig config;
  private final MemorySegment contextPtr;
  private final NativeFunctionBindings bindings;
  private final PanamaWasiHttpStats stats;
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final Arena arena;

  /**
   * Creates a new PanamaWasiHttpContext with the specified configuration.
   *
   * @param config the HTTP configuration
   * @throws WasmException if the context cannot be created
   * @throws IllegalArgumentException if config is null
   */
  public PanamaWasiHttpContext(final WasiHttpConfig config) throws WasmException {
    this.config = Objects.requireNonNull(config, "config cannot be null");
    this.bindings = NativeFunctionBindings.getInstance();
    this.arena = Arena.ofShared();

    try {
      this.contextPtr = createNativeContext(config);
      if (this.contextPtr == null || this.contextPtr.equals(MemorySegment.NULL)) {
        throw new WasmException("Failed to create native WASI HTTP context");
      }
      this.stats = new PanamaWasiHttpStats(contextPtr, bindings);
      LOGGER.fine("Created WASI HTTP context: " + bindings.wasiHttpContextGetId(contextPtr));
    } catch (final WasmException e) {
      arena.close();
      throw e;
    } catch (final Exception e) {
      arena.close();
      throw new WasmException("Failed to create WASI HTTP context: " + e.getMessage(), e);
    }
  }

  private MemorySegment createNativeContext(final WasiHttpConfig config) throws WasmException {
    // Create config builder
    final MemorySegment builderPtr = bindings.wasiHttpConfigBuilderNew();
    if (builderPtr == null || builderPtr.equals(MemorySegment.NULL)) {
      throw new WasmException("Failed to create WASI HTTP config builder");
    }

    // Track whether we've called build() - after build(), the builder is consumed
    // and must NOT be freed. Set this BEFORE calling build().
    boolean builderConsumed = false;
    MemorySegment configPtr = null;

    try {
      // Configure allowed hosts
      final Set<String> allowedHosts = config.getAllowedHosts();
      for (final String host : allowedHosts) {
        final MemorySegment hostStr = arena.allocateFrom(host);
        final int result = bindings.wasiHttpConfigBuilderAllowHost(builderPtr, hostStr);
        if (result != 0) {
          LOGGER.warning("Failed to add allowed host: " + host);
        }
      }

      // Configure blocked hosts
      final Set<String> blockedHosts = config.getBlockedHosts();
      for (final String host : blockedHosts) {
        final MemorySegment hostStr = arena.allocateFrom(host);
        final int result = bindings.wasiHttpConfigBuilderBlockHost(builderPtr, hostStr);
        if (result != 0) {
          LOGGER.warning("Failed to add blocked host: " + host);
        }
      }

      // Configure timeouts
      config
          .getConnectTimeout()
          .ifPresent(
              timeout ->
                  bindings.wasiHttpConfigBuilderSetConnectTimeout(builderPtr, timeout.toMillis()));

      config
          .getReadTimeout()
          .ifPresent(
              timeout ->
                  bindings.wasiHttpConfigBuilderSetReadTimeout(builderPtr, timeout.toMillis()));

      config
          .getWriteTimeout()
          .ifPresent(
              timeout ->
                  bindings.wasiHttpConfigBuilderSetWriteTimeout(builderPtr, timeout.toMillis()));

      // Configure connection limits
      config
          .getMaxConnections()
          .ifPresent(max -> bindings.wasiHttpConfigBuilderSetMaxConnections(builderPtr, max));

      config
          .getMaxConnectionsPerHost()
          .ifPresent(
              max -> bindings.wasiHttpConfigBuilderSetMaxConnectionsPerHost(builderPtr, max));

      // Configure body size limits
      config
          .getMaxRequestBodySize()
          .ifPresent(max -> bindings.wasiHttpConfigBuilderSetMaxRequestBodySize(builderPtr, max));

      config
          .getMaxResponseBodySize()
          .ifPresent(max -> bindings.wasiHttpConfigBuilderSetMaxResponseBodySize(builderPtr, max));

      // Configure security options
      bindings.wasiHttpConfigBuilderSetHttpsRequired(builderPtr, config.isHttpsRequired());
      bindings.wasiHttpConfigBuilderSetCertificateValidation(
          builderPtr, config.isCertificateValidationEnabled());

      // Configure HTTP/2
      bindings.wasiHttpConfigBuilderSetHttp2Enabled(builderPtr, config.isHttp2Enabled());

      // Configure connection pooling
      bindings.wasiHttpConfigBuilderSetConnectionPooling(
          builderPtr, config.isConnectionPoolingEnabled());

      // Configure redirects
      bindings.wasiHttpConfigBuilderSetFollowRedirects(builderPtr, config.isFollowRedirects());
      config
          .getMaxRedirects()
          .ifPresent(max -> bindings.wasiHttpConfigBuilderSetMaxRedirects(builderPtr, max));

      // Configure user agent
      config
          .getUserAgent()
          .ifPresent(
              userAgent -> {
                final MemorySegment userAgentStr = arena.allocateFrom(userAgent);
                bindings.wasiHttpConfigBuilderSetUserAgent(builderPtr, userAgentStr);
              });

      // Build the config - IMPORTANT: build() consumes the builder via Box::from_raw
      // Set builderConsumed BEFORE the call so we don't try to free it if build() throws
      builderConsumed = true;
      configPtr = bindings.wasiHttpConfigBuilderBuild(builderPtr);

      if (configPtr == null || configPtr.equals(MemorySegment.NULL)) {
        throw new WasmException("Failed to build WASI HTTP config");
      }

      // Create the context from the config (config is cloned, not consumed)
      final MemorySegment ctxPtr = bindings.wasiHttpContextNew(configPtr);
      if (ctxPtr == null || ctxPtr.equals(MemorySegment.NULL)) {
        throw new WasmException("Failed to create WASI HTTP context from config");
      }
      return ctxPtr;
    } finally {
      // Free the config if it was created (wasi_http_ctx_new clones it)
      if (configPtr != null && !configPtr.equals(MemorySegment.NULL)) {
        bindings.wasiHttpConfigFree(configPtr);
      }
      // Free the builder ONLY if build() was never called
      // After build() is called, the builder is consumed and must not be freed
      if (!builderConsumed) {
        bindings.wasiHttpConfigBuilderFree(builderPtr);
      }
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

    // Get native pointers from Panama implementations
    if (!(linker instanceof PanamaLinker)) {
      throw new WasmException("linker must be a PanamaLinker instance");
    }
    if (!(store instanceof PanamaStore)) {
      throw new WasmException("store must be a PanamaStore instance");
    }

    final PanamaLinker panamaLinker = (PanamaLinker) linker;
    final PanamaStore panamaStore = (PanamaStore) store;

    final MemorySegment linkerPtr = panamaLinker.getNativeLinker();
    final MemorySegment storePtr = panamaStore.getNativeStore();

    final int result = bindings.wasiHttpAddToLinker(linkerPtr, storePtr, contextPtr);
    if (result != 0) {
      throw new WasmException("Failed to add WASI HTTP to linker, error code: " + result);
    }

    LOGGER.info(
        "WASI HTTP context added to linker (context ID: "
            + bindings.wasiHttpContextGetId(contextPtr)
            + ")");
  }

  @Override
  public WasiHttpConfig getConfig() {
    return config;
  }

  @Override
  public WasiHttpStats getStats() {
    return stats;
  }

  @Override
  public boolean isValid() {
    if (closed.get()) {
      return false;
    }
    return bindings.wasiHttpContextIsValid(contextPtr) != 0;
  }

  @Override
  public boolean isHostAllowed(final String host) {
    if (host == null) {
      throw new IllegalArgumentException("host cannot be null");
    }
    if (closed.get()) {
      return false;
    }

    final MemorySegment hostStr = arena.allocateFrom(host);
    return bindings.wasiHttpContextIsHostAllowed(contextPtr, hostStr) != 0;
  }

  @Override
  public void resetStats() {
    if (closed.get()) {
      throw new IllegalStateException("WASI HTTP context has been closed");
    }
    bindings.wasiHttpContextResetStats(contextPtr);
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      try {
        bindings.wasiHttpContextFree(contextPtr);
        LOGGER.fine("Closed WASI HTTP context");
      } finally {
        arena.close();
      }
    }
  }

  @Override
  public String toString() {
    return "PanamaWasiHttpContext{"
        + "id="
        + (closed.get() ? "closed" : bindings.wasiHttpContextGetId(contextPtr))
        + ", valid="
        + isValid()
        + '}';
  }
}
