package ai.tegmentum.wasmtime4j.wasi.http;

import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
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
 * <p>Example usage:
 *
 * <pre>{@code
 * WasiHttpConfig config = WasiHttpConfig.builder()
 *     .allowHost("api.example.com")
 *     .withConnectTimeout(Duration.ofSeconds(30))
 *     .build();
 *
 * try (WasiHttpContext httpContext = WasiHttpFactory.createContext(config)) {
 *     // Add HTTP capabilities to the linker
 *     httpContext.addToLinker(linker, store);
 *
 *     // Instantiate the WASM module with HTTP support
 *     Instance instance = linker.instantiate(store, module);
 *     // ...
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiHttpContext extends Closeable {

  /**
   * Adds WASI HTTP functions to the linker.
   *
   * <p>This method registers all required WASI HTTP interfaces with the linker, enabling WASM
   * modules to make outbound HTTP requests. The interfaces added include:
   *
   * <ul>
   *   <li>wasi:http/outgoing-handler - for making HTTP requests
   *   <li>wasi:http/types - HTTP types and error handling
   * </ul>
   *
   * @param linker the linker to add HTTP functions to
   * @param store the store associated with the linker
   * @throws WasmException if the HTTP functions cannot be added
   * @throws IllegalArgumentException if linker or store is null
   */
  void addToLinker(Linker<?> linker, Store store) throws WasmException;

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
   * Closes this HTTP context and releases associated resources.
   *
   * <p>After calling this method, the context becomes invalid and should not be used. Any pending
   * HTTP requests will be cancelled.
   */
  @Override
  void close();
}
