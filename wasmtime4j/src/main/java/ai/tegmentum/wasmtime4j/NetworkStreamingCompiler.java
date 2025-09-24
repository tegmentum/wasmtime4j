package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for network-aware streaming WebAssembly compilation.
 *
 * <p>NetworkStreamingCompiler extends streaming compilation with network-specific optimizations,
 * including HTTP range requests, adaptive streaming based on network conditions, and CDN
 * integration.
 *
 * @since 1.0.0
 */
public interface NetworkStreamingCompiler extends StreamingCompiler {

  /**
   * Compiles a WebAssembly module from a network URL with streaming.
   *
   * <p>This method fetches and compiles a WebAssembly module from the specified URL using
   * HTTP range requests for progressive download and compilation.
   *
   * @param url the URL to fetch the WebAssembly module from
   * @param config network streaming configuration
   * @return a CompletableFuture that completes when compilation is finished
   * @throws IllegalArgumentException if url or config is null
   */
  CompletableFuture<Module> compileFromUrl(URI url, NetworkStreamingConfig config);

  /**
   * Compiles a WebAssembly module from a network URL with instantiation preparation.
   *
   * <p>This method combines network streaming compilation with preparation for fast instantiation.
   *
   * @param url the URL to fetch the WebAssembly module from
   * @param streamingConfig network streaming configuration
   * @param instantiationConfig configuration for instantiation preparation
   * @return a CompletableFuture that completes with a StreamingInstantiator
   * @throws IllegalArgumentException if any parameter is null
   */
  CompletableFuture<StreamingInstantiator> compileFromUrlWithInstantiation(
      URI url, NetworkStreamingConfig streamingConfig, InstantiationConfig instantiationConfig);

  /**
   * Starts streaming compilation with manual URL segment handling.
   *
   * <p>This method returns a handle that allows the caller to manually control which URL segments
   * to fetch and in what order, enabling custom prioritization strategies.
   *
   * @param baseUrl the base URL for the WebAssembly module segments
   * @param config network streaming configuration
   * @return a NetworkStreamingHandle for manual segment control
   * @throws IllegalArgumentException if baseUrl or config is null
   */
  NetworkStreamingHandle startNetworkStreaming(URI baseUrl, NetworkStreamingConfig config);

  /**
   * Creates a CDN-optimized streaming compilation session.
   *
   * <p>This method sets up streaming compilation optimized for content delivery networks,
   * with intelligent edge server selection and caching strategies.
   *
   * @param cdnUrls list of CDN URLs to try (in order of preference)
   * @param config network streaming configuration
   * @return a CompletableFuture that completes when compilation is finished
   * @throws IllegalArgumentException if cdnUrls or config is null or if cdnUrls is empty
   */
  CompletableFuture<Module> compileFromCdn(URI[] cdnUrls, NetworkStreamingConfig config);

  /**
   * Estimates the optimal segment size for the current network conditions.
   *
   * <p>This method performs network probing to determine the best segment size for HTTP range
   * requests based on current bandwidth and latency.
   *
   * @param testUrl a URL to use for network testing (should be from the same CDN/server)
   * @param timeout maximum time to spend on network testing
   * @return a CompletableFuture that completes with the recommended segment size in bytes
   * @throws IllegalArgumentException if testUrl or timeout is null
   */
  CompletableFuture<Integer> estimateOptimalSegmentSize(URI testUrl, Duration timeout);

  /**
   * Gets the current network statistics.
   *
   * <p>This method provides real-time information about network performance, including bandwidth
   * utilization, latency measurements, and error rates.
   *
   * @return current network streaming statistics
   */
  NetworkStatistics getNetworkStatistics();

  /**
   * Registers a network event listener for streaming compilation events.
   *
   * @param listener the network event listener
   * @throws IllegalArgumentException if listener is null
   */
  void addNetworkEventListener(NetworkEventListener listener);

  /**
   * Removes a previously registered network event listener.
   *
   * @param listener the network event listener to remove
   * @throws IllegalArgumentException if listener is null
   */
  void removeNetworkEventListener(NetworkEventListener listener);
}