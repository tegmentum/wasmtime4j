package ai.tegmentum.wasmtime4j.wasi.http;

import java.io.Closeable;

/**
 * Main interface for WASI HTTP context operations.
 *
 * <p>WasiHttpContext provides the capability for WebAssembly components to make outbound HTTP
 * requests according to the WASI HTTP specification. This context integrates with the Wasmtime
 * linker to provide the wasi:http/outgoing-handler interface to WASM modules.
 *
 * <p>The context enforces security policies configured via {@link WasiHttpConfig}, including host
 * allowlists, connection limits, and timeout settings.
 *
 * <p>WASI HTTP is a Component Model feature. Use {@code ComponentLinker.enableWasiHttp()} to add
 * HTTP support to a component linker.
 *
 * @since 1.0.0
 */
public interface WasiHttpContext extends Closeable {

  /**
   * Gets the configuration for this HTTP context.
   *
   * @return the HTTP configuration
   */
  WasiHttpConfig getConfig();

  /**
   * Checks if the context is still valid and usable.
   *
   * @return true if the context is valid, false otherwise
   */
  boolean isValid();

  /**
   * Checks if outbound requests to the specified host are allowed.
   *
   * @param host the host to check
   * @return true if requests to the host are allowed, false otherwise
   */
  boolean isHostAllowed(String host);

  /**
   * Returns a snapshot of the current HTTP statistics for this context.
   *
   * <p>The returned object captures point-in-time values. Counter values are cumulative since
   * context creation or the last call to {@link #resetStats()}.
   *
   * @return a snapshot of the current statistics
   * @throws IllegalStateException if the context has been closed
   */
  WasiHttpStats getStats();

  /**
   * Resets the cumulative HTTP statistics counters for this context.
   *
   * <p>Gauge values (active requests, active connections, idle connections) are not affected by
   * this operation since they reflect current state rather than cumulative counts.
   *
   * @throws IllegalStateException if the context has been closed
   */
  void resetStats();

  /**
   * Closes this HTTP context and releases associated resources.
   *
   * <p>After calling this method, the context becomes invalid and should not be used. Any pending
   * HTTP requests will be cancelled.
   */
  @Override
  void close();
}
