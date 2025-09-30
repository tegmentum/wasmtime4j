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

package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.observability.ObservabilityConfiguration;
import ai.tegmentum.wasmtime4j.observability.WasmObservability;
import ai.tegmentum.wasmtime4j.observability.WasmRuntimeLogger;
import ai.tegmentum.wasmtime4j.observability.WasmRuntimeMetrics;
import ai.tegmentum.wasmtime4j.observability.WasmRuntimeTracer;
import io.opentelemetry.api.common.Attributes;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Benchmarks for measuring observability overhead in wasmtime4j operations.
 *
 * <p>These benchmarks measure the performance impact of enabling various observability features,
 * helping to ensure that the observability system does not significantly impact runtime
 * performance.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(
    value = 1,
    jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
public class ObservabilityOverheadBenchmark {

  @Param({"disabled", "logging_only", "metrics_only", "tracing_only", "all_enabled"})
  private String observabilityMode;

  private WasmObservability observabilityEnabled;
  private WasmObservability observabilityDisabled;
  private WasmRuntimeTracer tracer;
  private WasmRuntimeMetrics metrics;
  private WasmRuntimeLogger logger;

  // Test workload parameters
  private static final String TEST_MODULE_NAME = "benchmark_module";
  private static final String TEST_FUNCTION_NAME = "benchmark_function";
  private static final String TEST_INSTANCE_ID = "benchmark_instance";
  private static final long TEST_MODULE_SIZE = 8192L;

  @Setup(Level.Trial)
  public void setupObservability() {
    // Setup disabled observability baseline
    observabilityDisabled = null; // No observability

    // Setup enabled observability based on mode
    final ObservabilityConfiguration.Builder configBuilder =
        ObservabilityConfiguration.builder()
            .setGlobalProviderEnabled(false) // Avoid global state pollution
            .setResourceDetectionEnabled(false) // Reduce setup overhead
            .setUseBatchProcessors(false); // Use simple processors for consistent timing

    switch (observabilityMode) {
      case "disabled":
        observabilityEnabled = null;
        break;
      case "logging_only":
        configBuilder.addLoggingExporters();
        observabilityEnabled = WasmObservability.initialize(configBuilder.build());
        break;
      case "metrics_only":
        configBuilder.addLoggingExporters(); // Minimal exporters
        observabilityEnabled = WasmObservability.initialize(configBuilder.build());
        break;
      case "tracing_only":
        configBuilder.addLoggingExporters(); // Minimal exporters
        observabilityEnabled = WasmObservability.initialize(configBuilder.build());
        break;
      case "all_enabled":
        configBuilder.addLoggingExporters();
        observabilityEnabled = WasmObservability.initialize(configBuilder.build());
        break;
    }

    if (observabilityEnabled != null) {
      tracer = observabilityEnabled.getTracer();
      metrics = observabilityEnabled.getMetrics();
      logger = observabilityEnabled.getLogger();
    }
  }

  @TearDown(Level.Trial)
  public void tearDownObservability() {
    if (observabilityEnabled != null) {
      observabilityEnabled.shutdown();
    }
  }

  @Benchmark
  public String benchmarkSimpleOperationWithoutObservability() {
    return simulateSimpleOperation();
  }

  @Benchmark
  public String benchmarkSimpleOperationWithObservability() {
    if (observabilityEnabled == null) {
      return simulateSimpleOperation();
    }

    return observabilityEnabled.observeOperation("simple_operation", this::simulateSimpleOperation);
  }

  @Benchmark
  public String benchmarkModuleCompilationWithoutObservability() {
    return simulateModuleCompilation();
  }

  @Benchmark
  public String benchmarkModuleCompilationWithObservability() {
    if (observabilityEnabled == null) {
      return simulateModuleCompilation();
    }

    return observabilityEnabled.observeModuleOperation(
        "compile", TEST_MODULE_NAME, TEST_MODULE_SIZE, this::simulateModuleCompilation);
  }

  @Benchmark
  public Integer benchmarkFunctionCallWithoutObservability() {
    return simulateFunctionCall();
  }

  @Benchmark
  public Integer benchmarkFunctionCallWithObservability() {
    if (observabilityEnabled == null) {
      return simulateFunctionCall();
    }

    return observabilityEnabled.observeFunctionCall(
        TEST_FUNCTION_NAME, "(i32, i32) -> i32", false, this::simulateFunctionCall);
  }

  @Benchmark
  public void benchmarkMetricsOnlyOverhead() {
    if (metrics == null) {
      return; // No metrics to record
    }

    // Record various metrics
    metrics.recordEngineCreated("benchmark");
    metrics.recordModuleCompiled(TEST_MODULE_NAME, TEST_MODULE_SIZE, Duration.ofMillis(10));
    metrics.recordInstanceCreated(TEST_INSTANCE_ID, TEST_MODULE_NAME, Duration.ofMillis(5));
    metrics.recordFunctionCall(
        TEST_FUNCTION_NAME, "(i32, i32) -> i32", Duration.ofMicroseconds(100));
  }

  @Benchmark
  public void benchmarkTracingOnlyOverhead() {
    if (tracer == null) {
      return; // No tracing
    }

    // Create and close spans
    try (final WasmRuntimeTracer.TracedOperation operation =
        tracer.startEngineOperation("benchmark", "test")) {
      operation
          .setAttribute(WasmRuntimeTracer.WASM_MODULE_NAME, TEST_MODULE_NAME)
          .recordEvent("operation_started")
          .recordEvent("operation_completed");
    }
  }

  @Benchmark
  public void benchmarkLoggingOnlyOverhead() {
    if (logger == null) {
      return; // No logging
    }

    // Log various events
    logger.info(
        WasmRuntimeLogger.LogComponent.ENGINE,
        "benchmark",
        "Benchmark operation",
        Attributes.empty());
    logger.debug(
        WasmRuntimeLogger.LogComponent.MODULE,
        "compile",
        "Module compilation",
        Attributes.builder().put("module.name", TEST_MODULE_NAME).build());
  }

  @Benchmark
  public void benchmarkCompleteObservabilityOverhead() {
    if (observabilityEnabled == null) {
      return; // No observability
    }

    // Simulate a complete operation with all observability features
    observabilityEnabled.observeEngineOperation(
        "benchmark",
        "complete_operation",
        () -> {
          simulateModuleCompilation();
          simulateFunctionCall();
          return "completed";
        });
  }

  @Benchmark
  public long benchmarkHighFrequencyOperationsWithoutObservability() {
    long sum = 0;
    for (int i = 0; i < 1000; i++) {
      sum += simulateFastOperation(i);
    }
    return sum;
  }

  @Benchmark
  public long benchmarkHighFrequencyOperationsWithObservability() {
    if (observabilityEnabled == null) {
      long sum = 0;
      for (int i = 0; i < 1000; i++) {
        sum += simulateFastOperation(i);
      }
      return sum;
    }

    return observabilityEnabled.observeOperation(
        "high_frequency_ops",
        () -> {
          long sum = 0;
          for (int i = 0; i < 1000; i++) {
            sum += simulateFastOperation(i);
            // Record metrics for every 100th operation to simulate realistic usage
            if (i % 100 == 0) {
              metrics.recordFunctionCall("fast_op_" + i, "() -> i64", Duration.ofNanos(1000));
            }
          }
          return sum;
        });
  }

  @Benchmark
  public void benchmarkNestedOperationsOverhead() {
    if (observabilityEnabled == null) {
      simulateNestedOperations();
      return;
    }

    observabilityEnabled.observeOperation(
        "outer_operation",
        () -> {
          observabilityEnabled.observeOperation(
              "inner_operation_1",
              () -> {
                observabilityEnabled.observeOperation(
                    "inner_operation_2", this::simulateSimpleOperation);
                return null;
              });
          return null;
        });
  }

  // Simulation methods that represent typical WebAssembly operations

  private String simulateSimpleOperation() {
    // Simulate a simple operation with minimal CPU work
    return "operation_result_" + System.nanoTime();
  }

  private String simulateModuleCompilation() {
    // Simulate module compilation with some CPU work
    int hash = 0;
    for (int i = 0; i < 1000; i++) {
      hash = hash * 31 + i;
    }
    return "compiled_module_" + hash;
  }

  private Integer simulateFunctionCall() {
    // Simulate function call with some computation
    int result = 42;
    for (int i = 0; i < 100; i++) {
      result = result * 2 % 1000007;
    }
    return result;
  }

  private long simulateFastOperation(final int input) {
    // Simulate a very fast operation
    return input * 13L + 7L;
  }

  private void simulateNestedOperations() {
    simulateSimpleOperation();
    simulateSimpleOperation();
    simulateSimpleOperation();
  }

  /**
   * Main method to run the benchmarks.
   *
   * @param args command line arguments
   * @throws RunnerException if benchmark execution fails
   */
  public static void main(final String[] args) throws RunnerException {
    final Options opt =
        new OptionsBuilder().include(ObservabilityOverheadBenchmark.class.getSimpleName()).build();

    new Runner(opt).run();
  }
}
