package ai.tegmentum.wasmtime4j.parallel;

/**
 * Load balancing strategies for parallel WebAssembly execution.
 *
 * <p>These strategies determine how function calls are distributed
 * across available WebAssembly instances for optimal performance.
 *
 * @since 1.0.0
 */
public enum LoadBalancingStrategy {

  /**
   * Round-robin load balancing.
   * Distributes requests evenly across all available instances in rotation.
   */
  ROUND_ROBIN,

  /**
   * Least connections load balancing.
   * Routes requests to the instance with the fewest active connections.
   */
  LEAST_CONNECTIONS,

  /**
   * Random load balancing.
   * Randomly selects an instance for each request.
   */
  RANDOM,

  /**
   * Weighted round-robin load balancing.
   * Distributes requests based on instance capacity weights.
   */
  WEIGHTED_ROUND_ROBIN,

  /**
   * Least response time load balancing.
   * Routes requests to the instance with the fastest response time.
   */
  LEAST_RESPONSE_TIME,

  /**
   * CPU utilization based load balancing.
   * Routes requests to the instance with the lowest CPU usage.
   */
  CPU_UTILIZATION,

  /**
   * Memory utilization based load balancing.
   * Routes requests to the instance with the lowest memory usage.
   */
  MEMORY_UTILIZATION,

  /**
   * Combined resource utilization load balancing.
   * Routes requests based on overall resource usage (CPU + memory).
   */
  RESOURCE_UTILIZATION,

  /**
   * Consistent hashing load balancing.
   * Uses consistent hashing to ensure requests with similar characteristics
   * are routed to the same instance when possible.
   */
  CONSISTENT_HASH,

  /**
   * Priority-based load balancing.
   * Routes high-priority requests to premium instances first.
   */
  PRIORITY_BASED,

  /**
   * Geographic or network proximity based load balancing.
   * Routes requests to instances with the best network characteristics.
   */
  PROXIMITY_BASED,

  /**
   * Adaptive load balancing.
   * Dynamically switches between strategies based on current conditions.
   */
  ADAPTIVE
}