package ai.tegmentum.wasmtime4j;

/**
 * CDN (Content Delivery Network) strategy for network streaming compilation.
 *
 * <p>CdnStrategy defines different approaches for selecting and using CDN endpoints during network
 * streaming operations.
 *
 * @since 1.0.0
 */
public enum CdnStrategy {
  /**
   * Try the fastest responding CDN endpoint first.
   *
   * <p>Measures response times and selects the CDN with the lowest latency. Falls back to other
   * CDNs if the fastest one fails.
   */
  FASTEST_FIRST,

  /**
   * Try CDN endpoints in the order provided.
   *
   * <p>Uses CDN endpoints in the sequence they were specified, without performance testing.
   * Fails over to the next CDN only if the current one fails.
   */
  SEQUENTIAL,

  /**
   * Use geographic proximity to select the best CDN.
   *
   * <p>Selects CDN endpoints based on geographic distance from the client. Requires CDN region
   * information to be available.
   */
  GEOGRAPHIC,

  /**
   * Load balance requests across multiple CDN endpoints.
   *
   * <p>Distributes requests across available CDN endpoints to balance load and improve overall
   * throughput.
   */
  LOAD_BALANCED,

  /**
   * Use the CDN with the highest available bandwidth.
   *
   * <p>Measures bandwidth from each CDN endpoint and selects the one with the best throughput
   * for bulk data transfer.
   */
  HIGHEST_BANDWIDTH,

  /**
   * Adaptive strategy that learns from usage patterns.
   *
   * <p>Dynamically adjusts CDN selection based on historical performance data and current
   * network conditions.
   */
  ADAPTIVE
}