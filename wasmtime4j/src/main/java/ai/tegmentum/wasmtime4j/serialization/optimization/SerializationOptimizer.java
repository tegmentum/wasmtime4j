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

package ai.tegmentum.wasmtime4j.serialization.optimization;

import ai.tegmentum.wasmtime4j.serialization.ModuleSerializationFormat;
import ai.tegmentum.wasmtime4j.serialization.SerializedModuleMetadata;
import ai.tegmentum.wasmtime4j.serialization.SerializationOptions;
import ai.tegmentum.wasmtime4j.serialization.SerializationPerformanceMetrics;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.logging.Logger;

/**
 * Advanced performance optimization engine for WebAssembly module serialization.
 *
 * <p>This optimizer provides intelligent performance tuning based on system resources,
 * module characteristics, and historical performance data. Features include:
 * <ul>
 *   <li>Adaptive buffer sizing based on available memory</li>
 *   <li>Parallel processing optimization</li>
 *   <li>Memory-mapped I/O for large modules</li>
 *   <li>Zero-copy operations where possible</li>
 *   <li>CPU and memory usage monitoring</li>
 *   <li>Performance prediction and optimization suggestions</li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class SerializationOptimizer {

  private static final Logger LOGGER = Logger.getLogger(SerializationOptimizer.class.getName());

  // Optimization thresholds
  private static final long LARGE_MODULE_THRESHOLD = 50 * 1024 * 1024; // 50MB
  private static final long MEMORY_MAPPED_THRESHOLD = 100 * 1024 * 1024; // 100MB
  private static final int MIN_PARALLEL_SIZE = 1024 * 1024; // 1MB
  private static final double HIGH_MEMORY_PRESSURE = 0.8; // 80% memory usage
  private static final double HIGH_CPU_USAGE = 0.7; // 70% CPU usage

  // System monitoring
  private final MemoryMXBean memoryBean;
  private final OperatingSystemMXBean osBean;

  // Performance history
  private final Map<ModuleSerializationFormat, PerformanceHistory> performanceHistory;

  // Optimization settings
  private final OptimizationSettings settings;

  /**
   * Creates a new serialization optimizer with default settings.
   */
  public SerializationOptimizer() {
    this(OptimizationSettings.createDefault());
  }

  /**
   * Creates a new serialization optimizer with custom settings.
   *
   * @param settings the optimization settings
   */
  public SerializationOptimizer(final OptimizationSettings settings) {
    this.settings = Objects.requireNonNull(settings, "Settings cannot be null");
    this.memoryBean = ManagementFactory.getMemoryMXBean();
    this.osBean = ManagementFactory.getOperatingSystemMXBean();
    this.performanceHistory = new EnumMap<>(ModuleSerializationFormat.class);

    // Initialize performance history for all formats
    for (final ModuleSerializationFormat format : ModuleSerializationFormat.values()) {
      performanceHistory.put(format, new PerformanceHistory());
    }

    LOGGER.info("Serialization optimizer initialized with settings: " + settings);
  }

  /**
   * Optimizes serialization options based on module size and system conditions.
   *
   * @param moduleSize the size of the module to serialize in bytes
   * @param requestedFormat the requested serialization format
   * @return optimized serialization options
   */
  public SerializationOptions optimize(final long moduleSize, final ModuleSerializationFormat requestedFormat) {
    Objects.requireNonNull(requestedFormat, "Requested format cannot be null");

    final SystemMetrics systemMetrics = collectSystemMetrics();
    final PerformanceHistory history = performanceHistory.get(requestedFormat);

    LOGGER.fine("Optimizing serialization for " + moduleSize + " bytes using " + requestedFormat +
                " (memory pressure: " + String.format("%.1f%%", systemMetrics.memoryPressure * 100) +
                ", CPU usage: " + String.format("%.1f%%", systemMetrics.cpuUsage * 100) + ")");

    final SerializationOptions.Builder builder = new SerializationOptions.Builder();

    // Optimize buffer size based on module size and available memory
    final int optimalBufferSize = calculateOptimalBufferSize(moduleSize, systemMetrics);
    builder.setBufferSize(optimalBufferSize);

    // Enable streaming for large modules or under memory pressure
    final boolean enableStreaming = shouldEnableStreaming(moduleSize, systemMetrics, requestedFormat);
    builder.enableStreaming(enableStreaming);

    if (enableStreaming) {
      final long streamingThreshold = calculateStreamingThreshold(systemMetrics);
      builder.setStreamingThreshold(streamingThreshold);
    }

    // Optimize compression level based on system resources
    final int compressionLevel = calculateOptimalCompressionLevel(systemMetrics, history);
    builder.setCompressionLevel(compressionLevel);

    // Enable parallel processing for suitable scenarios
    final boolean enableParallel = shouldEnableParallelProcessing(moduleSize, systemMetrics);
    builder.useParallelCompression(enableParallel);

    // Enable memory-mapped files for very large modules
    final boolean enableMmap = shouldEnableMemoryMapping(moduleSize, systemMetrics);
    builder.useMemoryMappedFiles(enableMmap);

    // Optimize integrity verification based on performance requirements
    final boolean verifyIntegrity = shouldEnableIntegrityVerification(systemMetrics, settings);
    builder.verifyIntegrity(verifyIntegrity);

    // Include performance metrics for learning
    builder.includePerformanceMetrics(true);

    final SerializationOptions optimized = builder.build();
    LOGGER.fine("Optimized serialization options: " + optimized);

    return optimized;
  }

  /**
   * Performs parallel serialization optimization for multiple data chunks.
   *
   * @param data the data to serialize
   * @param chunkSize the size of each chunk
   * @return optimized parallel processing future
   */
  public CompletableFuture<byte[]> optimizeParallelSerialization(final byte[] data, final int chunkSize) {
    Objects.requireNonNull(data, "Data cannot be null");

    if (data.length < MIN_PARALLEL_SIZE) {
      // Too small for parallel processing
      return CompletableFuture.completedFuture(data);
    }

    final SystemMetrics metrics = collectSystemMetrics();
    if (metrics.memoryPressure > HIGH_MEMORY_PRESSURE) {
      // System under memory pressure, avoid parallel processing
      return CompletableFuture.completedFuture(data);
    }

    // Create parallel processing task
    final ParallelSerializationTask task = new ParallelSerializationTask(data, chunkSize);
    return ForkJoinPool.commonPool().submit(task);
  }

  /**
   * Optimizes memory-mapped file operations for large modules.
   *
   * @param filePath the file path to optimize
   * @param moduleSize the size of the module
   * @return optimized file channel configuration
   */
  public FileChannelConfiguration optimizeFileChannel(final Path filePath, final long moduleSize) {
    Objects.requireNonNull(filePath, "File path cannot be null");

    final SystemMetrics metrics = collectSystemMetrics();
    final boolean useMemoryMapping = shouldEnableMemoryMapping(moduleSize, metrics);
    final int bufferSize = calculateOptimalBufferSize(moduleSize, metrics);

    return new FileChannelConfiguration(useMemoryMapping, bufferSize, moduleSize);
  }

  /**
   * Records performance metrics for learning and optimization.
   *
   * @param format the serialization format used
   * @param metrics the performance metrics
   * @param moduleSize the size of the module that was serialized
   */
  public void recordPerformance(final ModuleSerializationFormat format,
                               final SerializationPerformanceMetrics metrics,
                               final long moduleSize) {
    Objects.requireNonNull(format, "Format cannot be null");
    Objects.requireNonNull(metrics, "Metrics cannot be null");

    final PerformanceHistory history = performanceHistory.get(format);
    history.addDataPoint(moduleSize, metrics);

    LOGGER.fine("Recorded performance for " + format + ": " + metrics.getPerformanceSummary());
  }

  /**
   * Predicts serialization performance based on historical data.
   *
   * @param format the serialization format
   * @param moduleSize the size of the module to serialize
   * @return predicted performance metrics
   */
  public PredictedPerformance predictPerformance(final ModuleSerializationFormat format, final long moduleSize) {
    Objects.requireNonNull(format, "Format cannot be null");

    final PerformanceHistory history = performanceHistory.get(format);
    return history.predict(moduleSize);
  }

  /**
   * Gets optimization recommendations based on current system state and performance history.
   *
   * @param moduleSize the size of the module to serialize
   * @return optimization recommendations
   */
  public OptimizationRecommendations getRecommendations(final long moduleSize) {
    final SystemMetrics metrics = collectSystemMetrics();
    final OptimizationRecommendations.Builder builder = new OptimizationRecommendations.Builder();

    // Analyze system state
    if (metrics.memoryPressure > HIGH_MEMORY_PRESSURE) {
      builder.addRecommendation("System under memory pressure - consider streaming serialization");
      builder.addRecommendation("Reduce buffer sizes to minimize memory usage");
    }

    if (metrics.cpuUsage > HIGH_CPU_USAGE) {
      builder.addRecommendation("High CPU usage detected - reduce compression level");
      builder.addRecommendation("Disable parallel processing to reduce CPU load");
    }

    // Analyze module size
    if (moduleSize > LARGE_MODULE_THRESHOLD) {
      builder.addRecommendation("Large module detected - enable streaming serialization");
      builder.addRecommendation("Consider using memory-mapped files for better performance");
    }

    if (moduleSize > MEMORY_MAPPED_THRESHOLD) {
      builder.addRecommendation("Very large module - strongly recommend memory-mapped serialization");
    }

    // Analyze format performance
    ModuleSerializationFormat bestFormat = findBestFormat(moduleSize);
    if (bestFormat != null) {
      builder.addRecommendation("Best format for this module size: " + bestFormat.getIdentifier());
    }

    return builder.build();
  }

  /**
   * Clears performance history for a specific format.
   *
   * @param format the format to clear history for
   */
  public void clearPerformanceHistory(final ModuleSerializationFormat format) {
    Objects.requireNonNull(format, "Format cannot be null");
    performanceHistory.get(format).clear();
    LOGGER.info("Cleared performance history for format: " + format);
  }

  /**
   * Clears all performance history.
   */
  public void clearAllPerformanceHistory() {
    performanceHistory.values().forEach(PerformanceHistory::clear);
    LOGGER.info("Cleared all performance history");
  }

  // Private optimization methods

  private SystemMetrics collectSystemMetrics() {
    final long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
    final long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
    final double memoryPressure = maxMemory > 0 ? (double) usedMemory / maxMemory : 0.0;

    final double cpuUsage = osBean.getProcessCpuLoad();
    final int availableProcessors = Runtime.getRuntime().availableProcessors();

    return new SystemMetrics(memoryPressure, cpuUsage, usedMemory, maxMemory, availableProcessors);
  }

  private int calculateOptimalBufferSize(final long moduleSize, final SystemMetrics metrics) {
    // Base buffer size
    int bufferSize = settings.getBaseBufferSize();

    // Adjust based on module size
    if (moduleSize > LARGE_MODULE_THRESHOLD) {
      bufferSize *= 4; // Larger buffer for large modules
    } else if (moduleSize > 10 * 1024 * 1024) { // 10MB
      bufferSize *= 2;
    }

    // Adjust based on memory pressure
    if (metrics.memoryPressure > HIGH_MEMORY_PRESSURE) {
      bufferSize /= 2; // Smaller buffer under memory pressure
    }

    // Ensure buffer size is within reasonable bounds
    bufferSize = Math.max(4096, Math.min(bufferSize, 4 * 1024 * 1024)); // 4KB to 4MB

    return bufferSize;
  }

  private boolean shouldEnableStreaming(final long moduleSize, final SystemMetrics metrics,
                                      final ModuleSerializationFormat format) {
    // Always stream for very large modules
    if (moduleSize > MEMORY_MAPPED_THRESHOLD) {
      return true;
    }

    // Stream under memory pressure
    if (metrics.memoryPressure > HIGH_MEMORY_PRESSURE && moduleSize > 10 * 1024 * 1024) {
      return true;
    }

    // Stream if format supports it and module is reasonably large
    return format.supportsStreaming() && moduleSize > LARGE_MODULE_THRESHOLD;
  }

  private long calculateStreamingThreshold(final SystemMetrics metrics) {
    long threshold = 5 * 1024 * 1024; // 5MB base

    // Reduce threshold under memory pressure
    if (metrics.memoryPressure > HIGH_MEMORY_PRESSURE) {
      threshold /= 2;
    }

    // Increase threshold with more available memory
    if (metrics.memoryPressure < 0.5) {
      threshold *= 2;
    }

    return threshold;
  }

  private int calculateOptimalCompressionLevel(final SystemMetrics metrics, final PerformanceHistory history) {
    int compressionLevel = 6; // Default balanced compression

    // Reduce compression level under high CPU usage
    if (metrics.cpuUsage > HIGH_CPU_USAGE) {
      compressionLevel = 1; // Fast compression
    } else if (metrics.cpuUsage < 0.3) {
      compressionLevel = 9; // Maximum compression when CPU is available
    }

    // Adjust based on historical performance
    if (history.getAverageCompressionRatio() > 3.0) {
      // Good compression ratios, worth using higher compression
      compressionLevel = Math.min(9, compressionLevel + 2);
    }

    return compressionLevel;
  }

  private boolean shouldEnableParallelProcessing(final long moduleSize, final SystemMetrics metrics) {
    // Only enable for sufficiently large modules
    if (moduleSize < MIN_PARALLEL_SIZE) {
      return false;
    }

    // Avoid under memory pressure
    if (metrics.memoryPressure > HIGH_MEMORY_PRESSURE) {
      return false;
    }

    // Avoid under high CPU usage
    if (metrics.cpuUsage > HIGH_CPU_USAGE) {
      return false;
    }

    // Only enable with multiple processors
    return metrics.availableProcessors > 1;
  }

  private boolean shouldEnableMemoryMapping(final long moduleSize, final SystemMetrics metrics) {
    // Only for very large modules
    if (moduleSize < MEMORY_MAPPED_THRESHOLD) {
      return false;
    }

    // Especially beneficial under memory pressure
    if (metrics.memoryPressure > HIGH_MEMORY_PRESSURE) {
      return true;
    }

    // Generally good for very large modules
    return moduleSize > 500 * 1024 * 1024; // 500MB
  }

  private boolean shouldEnableIntegrityVerification(final SystemMetrics metrics, final OptimizationSettings settings) {
    // Always enable if explicitly requested
    if (settings.isAlwaysVerifyIntegrity()) {
      return true;
    }

    // Disable under high CPU pressure for performance
    if (metrics.cpuUsage > HIGH_CPU_USAGE && settings.isPerformanceOptimized()) {
      return false;
    }

    // Default to enabled
    return true;
  }

  private ModuleSerializationFormat findBestFormat(final long moduleSize) {
    ModuleSerializationFormat bestFormat = null;
    double bestScore = Double.NEGATIVE_INFINITY;

    for (final Map.Entry<ModuleSerializationFormat, PerformanceHistory> entry : performanceHistory.entrySet()) {
      final ModuleSerializationFormat format = entry.getKey();
      final PerformanceHistory history = entry.getValue();

      final PredictedPerformance prediction = history.predict(moduleSize);
      if (prediction.hasData()) {
        final double score = calculateFormatScore(prediction, moduleSize);
        if (score > bestScore) {
          bestScore = score;
          bestFormat = format;
        }
      }
    }

    return bestFormat;
  }

  private double calculateFormatScore(final PredictedPerformance prediction, final long moduleSize) {
    // Higher score is better
    double score = 0.0;

    // Favor faster serialization (negative because lower time is better)
    score -= prediction.getPredictedSerializationTimeMs() / 1000.0;

    // Favor better compression ratios
    score += prediction.getPredictedCompressionRatio() * 10.0;

    // Favor lower memory usage
    score -= prediction.getPredictedMemoryUsageMB() / 100.0;

    return score;
  }

  /**
   * Parallel serialization task using Fork/Join framework.
   */
  private static final class ParallelSerializationTask extends RecursiveTask<byte[]> {
    private final byte[] data;
    private final int chunkSize;

    ParallelSerializationTask(final byte[] data, final int chunkSize) {
      this.data = data;
      this.chunkSize = chunkSize;
    }

    @Override
    protected byte[] compute() {
      if (data.length <= chunkSize) {
        // Process chunk directly
        return processChunk(data);
      }

      // Split into smaller tasks
      final int mid = data.length / 2;
      final byte[] leftData = new byte[mid];
      final byte[] rightData = new byte[data.length - mid];
      System.arraycopy(data, 0, leftData, 0, mid);
      System.arraycopy(data, mid, rightData, 0, data.length - mid);

      final ParallelSerializationTask leftTask = new ParallelSerializationTask(leftData, chunkSize);
      final ParallelSerializationTask rightTask = new ParallelSerializationTask(rightData, chunkSize);

      // Execute in parallel
      leftTask.fork();
      final byte[] rightResult = rightTask.compute();
      final byte[] leftResult = leftTask.join();

      // Combine results
      final byte[] result = new byte[leftResult.length + rightResult.length];
      System.arraycopy(leftResult, 0, result, 0, leftResult.length);
      System.arraycopy(rightResult, 0, result, leftResult.length, rightResult.length);

      return result;
    }

    private byte[] processChunk(final byte[] chunk) {
      // Placeholder for actual chunk processing
      // In a real implementation, this would apply compression or other transformations
      return chunk;
    }
  }

  /**
   * System metrics for optimization decisions.
   */
  private static final class SystemMetrics {
    final double memoryPressure;
    final double cpuUsage;
    final long usedMemory;
    final long maxMemory;
    final int availableProcessors;

    SystemMetrics(final double memoryPressure, final double cpuUsage, final long usedMemory,
                  final long maxMemory, final int availableProcessors) {
      this.memoryPressure = memoryPressure;
      this.cpuUsage = cpuUsage;
      this.usedMemory = usedMemory;
      this.maxMemory = maxMemory;
      this.availableProcessors = availableProcessors;
    }
  }
}