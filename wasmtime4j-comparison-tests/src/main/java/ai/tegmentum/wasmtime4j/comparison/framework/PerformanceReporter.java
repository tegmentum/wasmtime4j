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

package ai.tegmentum.wasmtime4j.comparison.framework;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Collects and reports performance comparison data between JNI and Panama runtimes.
 *
 * <p>This class uses a thread-safe singleton pattern to collect timing data during test execution
 * and can generate comprehensive performance reports comparing the two runtime implementations.
 *
 * <p>Performance metrics include:
 *
 * <ul>
 *   <li>Total execution time per runtime
 *   <li>Per-test execution time comparison
 *   <li>Relative performance (speedup/slowdown ratios)
 *   <li>Statistical summaries (min, max, mean, median)
 * </ul>
 */
public final class PerformanceReporter {

  private static final PerformanceReporter INSTANCE = new PerformanceReporter();

  private final Map<String, Map<RuntimeType, Long>> testTimings = new ConcurrentHashMap<>();
  private final Map<RuntimeType, AtomicLong> totalTimes = new ConcurrentHashMap<>();

  private PerformanceReporter() {
    totalTimes.put(RuntimeType.JNI, new AtomicLong(0));
    totalTimes.put(RuntimeType.PANAMA, new AtomicLong(0));
  }

  /**
   * Gets the singleton instance of the performance reporter.
   *
   * @return the performance reporter instance
   */
  public static PerformanceReporter getInstance() {
    return INSTANCE;
  }

  /**
   * Records the execution time for a test.
   *
   * @param testName the unique name of the test
   * @param runtime the runtime type used for the test
   * @param durationNanos the execution duration in nanoseconds
   */
  public void recordTiming(
      final String testName, final RuntimeType runtime, final long durationNanos) {
    testTimings.computeIfAbsent(testName, k -> new ConcurrentHashMap<>()).put(runtime, durationNanos);
    totalTimes.get(runtime).addAndGet(durationNanos);
  }

  /**
   * Clears all collected timing data.
   *
   * <p>This is useful for starting a fresh performance collection session.
   */
  public void reset() {
    testTimings.clear();
    totalTimes.get(RuntimeType.JNI).set(0);
    totalTimes.get(RuntimeType.PANAMA).set(0);
  }

  /**
   * Generates a comprehensive performance comparison report.
   *
   * @return a formatted string containing the performance report
   */
  public String generateReport() {
    final StringBuilder report = new StringBuilder();

    report.append("═══════════════════════════════════════════════════════════════════\n");
    report.append("             JNI vs Panama Performance Comparison Report\n");
    report.append("═══════════════════════════════════════════════════════════════════\n\n");

    // Overall summary
    appendOverallSummary(report);

    // Per-test comparison
    appendPerTestComparison(report);

    // Statistical summary
    appendStatisticalSummary(report);

    report.append("═══════════════════════════════════════════════════════════════════\n");

    return report.toString();
  }

  private void appendOverallSummary(final StringBuilder report) {
    final long jniTotal = totalTimes.get(RuntimeType.JNI).get();
    final long panamaTotal = totalTimes.get(RuntimeType.PANAMA).get();

    report.append("Overall Summary:\n");
    report.append("───────────────────────────────────────────────────────────────────\n");
    report.append(String.format("  JNI Total Time:    %,15d ns  (%s)%n", jniTotal, formatDuration(jniTotal)));
    report.append(String.format("  Panama Total Time: %,15d ns  (%s)%n", panamaTotal, formatDuration(panamaTotal)));

    if (jniTotal > 0 && panamaTotal > 0) {
      final double speedup = (double) jniTotal / panamaTotal;
      if (speedup > 1.0) {
        report.append(String.format("  Panama is %.2fx faster than JNI%n", speedup));
      } else if (speedup < 1.0) {
        report.append(String.format("  JNI is %.2fx faster than Panama%n", 1.0 / speedup));
      } else {
        report.append("  Both runtimes have equal performance\n");
      }
    }
    report.append("\n");
  }

  private void appendPerTestComparison(final StringBuilder report) {
    report.append("Per-Test Comparison (sorted by absolute time difference):\n");
    report.append("───────────────────────────────────────────────────────────────────\n");

    final List<Map.Entry<String, Map<RuntimeType, Long>>> sortedTests = new ArrayList<>(testTimings.entrySet());

    // Sort by absolute time difference
    sortedTests.sort(
        Comparator.<Map.Entry<String, Map<RuntimeType, Long>>>comparingLong(
            e -> {
              final Long jniTime = e.getValue().get(RuntimeType.JNI);
              final Long panamaTime = e.getValue().get(RuntimeType.PANAMA);
              if (jniTime == null || panamaTime == null) {
                return 0;
              }
              return Math.abs(jniTime - panamaTime);
            })
            .reversed());

    for (final Map.Entry<String, Map<RuntimeType, Long>> entry : sortedTests) {
      final String testName = entry.getKey();
      final Long jniTime = entry.getValue().get(RuntimeType.JNI);
      final Long panamaTime = entry.getValue().get(RuntimeType.PANAMA);

      if (jniTime != null && panamaTime != null) {
        final double ratio = (double) jniTime / panamaTime;
        final String fasterRuntime = ratio > 1.0 ? "Panama" : "JNI";
        final double speedup = ratio > 1.0 ? ratio : 1.0 / ratio;

        report.append(String.format("%n  Test: %s%n", testName));
        report.append(String.format("    JNI:    %,12d ns  (%s)%n", jniTime, formatDuration(jniTime)));
        report.append(String.format("    Panama: %,12d ns  (%s)%n", panamaTime, formatDuration(panamaTime)));
        report.append(String.format("    %s is %.2fx faster%n", fasterRuntime, speedup));
      }
    }
    report.append("\n");
  }

  private void appendStatisticalSummary(final StringBuilder report) {
    final List<Double> speedupRatios = new ArrayList<>();

    for (final Map<RuntimeType, Long> timings : testTimings.values()) {
      final Long jniTime = timings.get(RuntimeType.JNI);
      final Long panamaTime = timings.get(RuntimeType.PANAMA);

      if (jniTime != null && panamaTime != null && panamaTime > 0) {
        speedupRatios.add((double) jniTime / panamaTime);
      }
    }

    if (speedupRatios.isEmpty()) {
      return;
    }

    speedupRatios.sort(Comparator.naturalOrder());

    final double min = speedupRatios.get(0);
    final double max = speedupRatios.get(speedupRatios.size() - 1);
    final double mean = speedupRatios.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    final double median =
        speedupRatios.size() % 2 == 0
            ? (speedupRatios.get(speedupRatios.size() / 2 - 1)
                + speedupRatios.get(speedupRatios.size() / 2))
                / 2.0
            : speedupRatios.get(speedupRatios.size() / 2);

    report.append("Statistical Summary (JNI time / Panama time ratio):\n");
    report.append("───────────────────────────────────────────────────────────────────\n");
    report.append(String.format("  Min ratio:    %.4f%n", min));
    report.append(String.format("  Max ratio:    %.4f%n", max));
    report.append(String.format("  Mean ratio:   %.4f%n", mean));
    report.append(String.format("  Median ratio: %.4f%n", median));
    report.append(String.format("%n  Tests compared: %d%n%n", speedupRatios.size()));
  }

  private String formatDuration(final long nanos) {
    if (nanos < 1_000) {
      return String.format("%d ns", nanos);
    } else if (nanos < 1_000_000) {
      return String.format("%.2f μs", nanos / 1_000.0);
    } else if (nanos < 1_000_000_000) {
      return String.format("%.2f ms", nanos / 1_000_000.0);
    } else {
      return String.format("%.2f s", nanos / 1_000_000_000.0);
    }
  }

  /**
   * Writes the performance report to a file.
   *
   * @param outputPath the path where the report should be written
   * @throws IOException if writing fails
   */
  public void writeReportToFile(final String outputPath) throws IOException {
    final Path path = Paths.get(outputPath);
    Files.createDirectories(path.getParent());

    try (final PrintWriter writer = new PrintWriter(Files.newBufferedWriter(path))) {
      writer.print(generateReport());
    }
  }

  /**
   * Prints the performance report to standard output.
   */
  public void printReport() {
    System.out.println(generateReport());
  }

  /**
   * Gets the number of tests that have timing data recorded.
   *
   * @return the count of tests with timing data
   */
  public int getTestCount() {
    return testTimings.size();
  }

  /**
   * Checks if timing data exists for a specific test and runtime.
   *
   * @param testName the test name
   * @param runtime the runtime type
   * @return true if timing data exists
   */
  public boolean hasTimingData(final String testName, final RuntimeType runtime) {
    final Map<RuntimeType, Long> timings = testTimings.get(testName);
    return timings != null && timings.containsKey(runtime);
  }
}
