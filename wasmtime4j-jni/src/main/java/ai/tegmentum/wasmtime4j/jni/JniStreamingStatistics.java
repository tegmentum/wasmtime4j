package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.CompilationPhase;
import ai.tegmentum.wasmtime4j.StreamingStatistics;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

/**
 * JNI utility for creating StreamingStatistics.
 *
 * <p>This class provides helper methods for creating StreamingStatistics from native data
 * through JNI integration with the native Wasmtime library.
 *
 * @since 1.0.0
 */
public final class JniStreamingStatistics {

  private JniStreamingStatistics() {
    // Utility class
  }

  /**
   * Creates StreamingStatistics from native data.
   *
   * @param bytesProcessed total bytes processed
   * @param bytesReceived total bytes received
   * @param progress compilation progress (0.0 to 1.0)
   * @param memoryUsage current memory usage in bytes
   * @param peakMemoryUsage peak memory usage in bytes
   * @param functionsCompiled number of functions compiled
   * @param totalFunctions total number of functions
   * @return StreamingStatistics instance
   */
  static StreamingStatistics create(
      final long bytesProcessed,
      final long bytesReceived,
      final double progress,
      final long memoryUsage,
      final long peakMemoryUsage,
      final long functionsCompiled,
      final long totalFunctions) {

    final Instant startTime = Instant.now(); // Approximation - real implementation would track this properly
    final CompilationPhase currentPhase = mapProgressToPhase(progress);

    return StreamingStatistics.builder()
        .totalBytesProcessed(bytesProcessed)
        .totalBytesReceived(bytesReceived)
        .currentPhase(currentPhase)
        .completionPercentage(progress)
        .startTime(startTime)
        .elapsedTime(Duration.ofMillis(100)) // Approximation
        .throughputBytesPerSecond(bytesProcessed) // Approximation
        .memoryUsage(memoryUsage)
        .activeThreads(1) // Approximation
        .phaseStatistics(Collections.emptyList())
        .functionsCompiled(functionsCompiled)
        .totalFunctions(totalFunctions)
        .cachingEnabled(false)
        .cacheHits(0)
        .cacheMisses(0)
        .build();
  }

  /**
   * Maps progress value to compilation phase.
   *
   * @param progress the progress value (0.0 to 1.0)
   * @return corresponding compilation phase
   */
  private static CompilationPhase mapProgressToPhase(final double progress) {
    if (progress <= 0.1) {
      return CompilationPhase.PARSING;
    } else if (progress <= 0.2) {
      return CompilationPhase.VALIDATION;
    } else if (progress <= 0.3) {
      return CompilationPhase.IMPORT_RESOLUTION;
    } else if (progress <= 0.4) {
      return CompilationPhase.TYPE_ANALYSIS;
    } else if (progress <= 0.7) {
      return CompilationPhase.CODE_GENERATION;
    } else if (progress <= 0.9) {
      return CompilationPhase.OPTIMIZATION;
    } else {
      return CompilationPhase.FINALIZATION;
    }
  }
}