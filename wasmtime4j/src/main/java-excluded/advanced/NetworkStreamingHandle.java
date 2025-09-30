package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Handle for manual control over network streaming WebAssembly compilation.
 *
 * <p>NetworkStreamingHandle provides fine-grained control over the network streaming process,
 * allowing applications to prioritize specific URL segments, control request patterns, and adapt
 * to changing network conditions.
 *
 * @since 1.0.0
 */
public interface NetworkStreamingHandle extends StreamingFeedHandle {

  /**
   * Requests a specific URL segment with high priority.
   *
   * <p>This method schedules the specified URL segment for immediate download, bypassing normal
   * sequential ordering.
   *
   * @param segmentUrl the URL of the segment to prioritize
   * @return a CompletableFuture that completes when the segment is downloaded and processed
   * @throws IllegalArgumentException if segmentUrl is null
   * @throws IllegalStateException if the handle is closed
   */
  CompletableFuture<Void> prioritizeSegment(URI segmentUrl);

  /**
   * Requests multiple URL segments with high priority.
   *
   * @param segmentUrls the URLs of the segments to prioritize
   * @return a CompletableFuture that completes when all segments are downloaded and processed
   * @throws IllegalArgumentException if segmentUrls is null or contains null elements
   * @throws IllegalStateException if the handle is closed
   */
  CompletableFuture<Void> prioritizeSegments(List<URI> segmentUrls);

  /**
   * Configures the segment size for HTTP range requests dynamically.
   *
   * <p>This method allows runtime adjustment of the segment size based on network conditions.
   *
   * @param segmentSize new segment size in bytes
   * @throws IllegalArgumentException if segmentSize is not positive
   * @throws IllegalStateException if the handle is closed
   */
  void setSegmentSize(int segmentSize);

  /**
   * Configures the maximum number of concurrent connections dynamically.
   *
   * @param maxConnections maximum number of concurrent connections
   * @throws IllegalArgumentException if maxConnections is not positive
   * @throws IllegalStateException if the handle is closed
   */
  void setMaxConcurrentConnections(int maxConnections);

  /**
   * Switches to a different CDN endpoint for subsequent requests.
   *
   * @param newCdnUrl the new CDN URL to use
   * @return a CompletableFuture that completes when the CDN switch is successful
   * @throws IllegalArgumentException if newCdnUrl is null
   * @throws IllegalStateException if the handle is closed
   */
  CompletableFuture<Void> switchCdn(URI newCdnUrl);

  /**
   * Tests network conditions and adjusts streaming parameters automatically.
   *
   * <p>This method performs bandwidth and latency tests and adjusts segment size and connection
   * count for optimal performance.
   *
   * @return a CompletableFuture that completes when network optimization is finished
   * @throws IllegalStateException if the handle is closed
   */
  CompletableFuture<NetworkOptimizationResult> optimizeForNetwork();

  /**
   * Gets the current network statistics.
   *
   * @return current network streaming statistics
   */
  NetworkStatistics getNetworkStatistics();

  /**
   * Gets the list of URL segments that have been successfully downloaded.
   *
   * @return list of completed segment URLs
   */
  List<URI> getCompletedSegments();

  /**
   * Gets the list of URL segments that are currently being downloaded.
   *
   * @return list of active segment URLs
   */
  List<URI> getActiveSegments();

  /**
   * Gets the list of URL segments that are queued for download.
   *
   * @return list of pending segment URLs
   */
  List<URI> getPendingSegments();

  /**
   * Checks if a specific URL segment has been downloaded.
   *
   * @param segmentUrl the URL of the segment to check
   * @return true if the segment has been downloaded
   * @throws IllegalArgumentException if segmentUrl is null
   */
  boolean isSegmentComplete(URI segmentUrl);

  /**
   * Gets the current effective segment size being used.
   *
   * @return current segment size in bytes
   */
  int getCurrentSegmentSize();

  /**
   * Gets the current number of active connections.
   *
   * @return number of active connections
   */
  int getCurrentConnectionCount();

  /**
   * Estimates the remaining download time based on current network conditions.
   *
   * @return estimated time to completion
   */
  java.time.Duration getEstimatedTimeToCompletion();
}