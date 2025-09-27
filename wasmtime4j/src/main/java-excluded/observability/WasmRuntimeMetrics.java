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

package ai.tegmentum.wasmtime4j.observability;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongGauge;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Comprehensive metrics collection for WebAssembly runtime operations.
 *
 * <p>This class provides detailed metrics collection for all aspects of WebAssembly runtime
 * operations including:
 *
 * <ul>
 *   <li>Engine and store lifecycle metrics
 *   <li>Module compilation and validation metrics
 *   <li>Instance creation and execution metrics
 *   <li>Function call performance and error rates
 *   <li>Memory usage and allocation patterns
 *   <li>WASI operation metrics
 *   <li>Host function integration metrics
 * </ul>
 *
 * @since 1.0.0
 */
public final class WasmRuntimeMetrics {

  private static final Logger LOGGER = Logger.getLogger(WasmRuntimeMetrics.class.getName());

  /** OpenTelemetry meter instance. */
  private final Meter meter;

  /** Metric tracking for timing operations. */
  private final ConcurrentHashMap<String, Instant> operationStartTimes = new ConcurrentHashMap<>();

  // Engine and Store Metrics
  private final LongCounter enginesCreatedCounter;
  private final LongCounter storesCreatedCounter;
  private final LongGauge activeEnginesGauge;
  private final LongGauge activeStoresGauge;
  private final AtomicLong activeEngines = new AtomicLong(0);
  private final AtomicLong activeStores = new AtomicLong(0);

  // Module Metrics
  private final LongCounter modulesCompiledCounter;
  private final LongCounter moduleValidationCounter;
  private final LongCounter moduleCompilationErrorsCounter;
  private final DoubleHistogram moduleCompilationTimeHistogram;
  private final LongHistogram moduleSizeHistogram;
  private final LongGauge activeCachedModulesGauge;
  private final AtomicLong activeCachedModules = new AtomicLong(0);

  // Instance Metrics
  private final LongCounter instancesCreatedCounter;
  private final LongCounter instanceCreationErrorsCounter;
  private final DoubleHistogram instanceCreationTimeHistogram;
  private final LongGauge activeInstancesGauge;
  private final AtomicLong activeInstances = new AtomicLong(0);

  // Function Call Metrics
  private final LongCounter functionCallsCounter;
  private final LongCounter functionCallErrorsCounter;
  private final DoubleHistogram functionCallDurationHistogram;
  private final LongCounter hostFunctionCallsCounter;
  private final DoubleHistogram hostFunctionCallDurationHistogram;

  // Memory Metrics
  private final LongCounter memoryAllocationsCounter;
  private final LongCounter memoryDeallocationsCounter;
  private final LongGauge totalMemoryUsageGauge;
  private final LongGauge wasmMemoryPagesGauge;
  private final DoubleHistogram memoryGrowTimeHistogram;
  private final AtomicLong totalMemoryUsage = new AtomicLong(0);
  private final AtomicLong wasmMemoryPages = new AtomicLong(0);

  // WASI Metrics
  private final LongCounter wasiOperationsCounter;
  private final LongCounter wasiErrorsCounter;
  private final DoubleHistogram wasiOperationDurationHistogram;
  private final LongCounter wasiFileOperationsCounter;
  private final LongCounter wasiNetworkOperationsCounter;

  // Performance Metrics
  private final DoubleHistogram compilationCacheHitRateGauge;
  private final LongCounter gcTriggeredCounter;
  private final DoubleHistogram gcDurationHistogram;

  // Error Metrics
  private final LongCounter trapOccurredCounter;
  private final LongCounter linkingErrorsCounter;
  private final LongCounter validationErrorsCounter;

  /**
   * Creates a new WebAssembly runtime metrics collector.
   *
   * @param meter OpenTelemetry meter instance
   */
  public WasmRuntimeMetrics(final Meter meter) {
    this.meter = meter;

    // Initialize Engine and Store metrics
    this.enginesCreatedCounter = meter
        .counterBuilder("wasm_engines_created_total")
        .setDescription("Total number of WebAssembly engines created")
        .setUnit("1")
        .build();

    this.storesCreatedCounter = meter
        .counterBuilder("wasm_stores_created_total")
        .setDescription("Total number of WebAssembly stores created")
        .setUnit("1")
        .build();

    this.activeEnginesGauge = meter
        .gaugeBuilder("wasm_engines_active")
        .setDescription("Number of currently active WebAssembly engines")
        .setUnit("1")
        .ofLongs()
        .buildWithCallback(measurement -> measurement.record(activeEngines.get()));

    this.activeStoresGauge = meter
        .gaugeBuilder("wasm_stores_active")
        .setDescription("Number of currently active WebAssembly stores")
        .setUnit("1")
        .ofLongs()
        .buildWithCallback(measurement -> measurement.record(activeStores.get()));

    // Initialize Module metrics
    this.modulesCompiledCounter = meter
        .counterBuilder("wasm_modules_compiled_total")
        .setDescription("Total number of WebAssembly modules compiled")
        .setUnit("1")
        .build();

    this.moduleValidationCounter = meter
        .counterBuilder("wasm_modules_validated_total")
        .setDescription("Total number of WebAssembly modules validated")
        .setUnit("1")
        .build();

    this.moduleCompilationErrorsCounter = meter
        .counterBuilder("wasm_module_compilation_errors_total")
        .setDescription("Total number of WebAssembly module compilation errors")
        .setUnit("1")
        .build();

    this.moduleCompilationTimeHistogram = meter
        .histogramBuilder("wasm_module_compilation_duration_seconds")
        .setDescription("Time spent compiling WebAssembly modules")
        .setUnit("s")
        .build();

    this.moduleSizeHistogram = meter
        .histogramBuilder("wasm_module_size_bytes")
        .setDescription("Size of WebAssembly modules in bytes")
        .setUnit("By")
        .ofLongs()
        .build();

    this.activeCachedModulesGauge = meter
        .gaugeBuilder("wasm_cached_modules_active")
        .setDescription("Number of currently cached WebAssembly modules")
        .setUnit("1")
        .ofLongs()
        .buildWithCallback(measurement -> measurement.record(activeCachedModules.get()));

    // Initialize Instance metrics
    this.instancesCreatedCounter = meter
        .counterBuilder("wasm_instances_created_total")
        .setDescription("Total number of WebAssembly instances created")
        .setUnit("1")
        .build();

    this.instanceCreationErrorsCounter = meter
        .counterBuilder("wasm_instance_creation_errors_total")
        .setDescription("Total number of WebAssembly instance creation errors")
        .setUnit("1")
        .build();

    this.instanceCreationTimeHistogram = meter
        .histogramBuilder("wasm_instance_creation_duration_seconds")
        .setDescription("Time spent creating WebAssembly instances")
        .setUnit("s")
        .build();

    this.activeInstancesGauge = meter
        .gaugeBuilder("wasm_instances_active")
        .setDescription("Number of currently active WebAssembly instances")
        .setUnit("1")
        .ofLongs()
        .buildWithCallback(measurement -> measurement.record(activeInstances.get()));

    // Initialize Function Call metrics
    this.functionCallsCounter = meter
        .counterBuilder("wasm_function_calls_total")
        .setDescription("Total number of WebAssembly function calls")
        .setUnit("1")
        .build();

    this.functionCallErrorsCounter = meter
        .counterBuilder("wasm_function_call_errors_total")
        .setDescription("Total number of WebAssembly function call errors")
        .setUnit("1")
        .build();

    this.functionCallDurationHistogram = meter
        .histogramBuilder("wasm_function_call_duration_seconds")
        .setDescription("Duration of WebAssembly function calls")
        .setUnit("s")
        .build();

    this.hostFunctionCallsCounter = meter
        .counterBuilder("wasm_host_function_calls_total")
        .setDescription("Total number of host function calls from WebAssembly")
        .setUnit("1")
        .build();

    this.hostFunctionCallDurationHistogram = meter
        .histogramBuilder("wasm_host_function_call_duration_seconds")
        .setDescription("Duration of host function calls from WebAssembly")
        .setUnit("s")
        .build();

    // Initialize Memory metrics
    this.memoryAllocationsCounter = meter
        .counterBuilder("wasm_memory_allocations_total")
        .setDescription("Total number of WebAssembly memory allocations")
        .setUnit("1")
        .build();

    this.memoryDeallocationsCounter = meter
        .counterBuilder("wasm_memory_deallocations_total")
        .setDescription("Total number of WebAssembly memory deallocations")
        .setUnit("1")
        .build();

    this.totalMemoryUsageGauge = meter
        .gaugeBuilder("wasm_memory_usage_bytes")
        .setDescription("Current WebAssembly memory usage in bytes")
        .setUnit("By")
        .ofLongs()
        .buildWithCallback(measurement -> measurement.record(totalMemoryUsage.get()));

    this.wasmMemoryPagesGauge = meter
        .gaugeBuilder("wasm_memory_pages")
        .setDescription("Current number of WebAssembly memory pages")
        .setUnit("1")
        .ofLongs()
        .buildWithCallback(measurement -> measurement.record(wasmMemoryPages.get()));

    this.memoryGrowTimeHistogram = meter
        .histogramBuilder("wasm_memory_grow_duration_seconds")
        .setDescription("Time spent growing WebAssembly memory")
        .setUnit("s")
        .build();

    // Initialize WASI metrics
    this.wasiOperationsCounter = meter
        .counterBuilder("wasi_operations_total")
        .setDescription("Total number of WASI operations")
        .setUnit("1")
        .build();

    this.wasiErrorsCounter = meter
        .counterBuilder("wasi_errors_total")
        .setDescription("Total number of WASI errors")
        .setUnit("1")
        .build();

    this.wasiOperationDurationHistogram = meter
        .histogramBuilder("wasi_operation_duration_seconds")
        .setDescription("Duration of WASI operations")
        .setUnit("s")
        .build();

    this.wasiFileOperationsCounter = meter
        .counterBuilder("wasi_file_operations_total")
        .setDescription("Total number of WASI file operations")
        .setUnit("1")
        .build();

    this.wasiNetworkOperationsCounter = meter
        .counterBuilder("wasi_network_operations_total")
        .setDescription("Total number of WASI network operations")
        .setUnit("1")
        .build();

    // Initialize Performance metrics
    this.compilationCacheHitRateGauge = meter
        .histogramBuilder("wasm_compilation_cache_hit_rate")
        .setDescription("WebAssembly compilation cache hit rate")
        .setUnit("1")
        .build();

    this.gcTriggeredCounter = meter
        .counterBuilder("wasm_gc_triggered_total")
        .setDescription("Total number of WebAssembly GC collections triggered")
        .setUnit("1")
        .build();

    this.gcDurationHistogram = meter
        .histogramBuilder("wasm_gc_duration_seconds")
        .setDescription("Duration of WebAssembly GC collections")
        .setUnit("s")
        .build();

    // Initialize Error metrics
    this.trapOccurredCounter = meter
        .counterBuilder("wasm_traps_total")
        .setDescription("Total number of WebAssembly traps occurred")
        .setUnit("1")
        .build();

    this.linkingErrorsCounter = meter
        .counterBuilder("wasm_linking_errors_total")
        .setDescription("Total number of WebAssembly linking errors")
        .setUnit("1")
        .build();

    this.validationErrorsCounter = meter
        .counterBuilder("wasm_validation_errors_total")
        .setDescription("Total number of WebAssembly validation errors")
        .setUnit("1")
        .build();

    LOGGER.info("WebAssembly runtime metrics initialized");
  }

  // Engine and Store Operations
  public void recordEngineCreated(final String engineType) {
    enginesCreatedCounter.add(1, createEngineAttributes(engineType));
    activeEngines.incrementAndGet();
  }

  public void recordEngineDestroyed(final String engineType) {
    activeEngines.decrementAndGet();
  }

  public void recordStoreCreated(final String storeId) {
    storesCreatedCounter.add(1, createStoreAttributes(storeId));
    activeStores.incrementAndGet();
  }

  public void recordStoreDestroyed(final String storeId) {
    activeStores.decrementAndGet();
  }

  // Module Operations
  public void recordModuleCompiled(final String moduleName, final long moduleSize, final Duration compilationTime) {
    final Attributes attributes = createModuleAttributes(moduleName);
    modulesCompiledCounter.add(1, attributes);
    moduleSizeHistogram.record(moduleSize, attributes);
    moduleCompilationTimeHistogram.record(compilationTime.toNanos() / 1_000_000_000.0, attributes);
  }

  public void recordModuleValidated(final String moduleName) {
    moduleValidationCounter.add(1, createModuleAttributes(moduleName));
  }

  public void recordModuleCompilationError(final String moduleName, final String errorType) {
    moduleCompilationErrorsCounter.add(1, createErrorAttributes(moduleName, errorType));
  }

  public void recordModuleCached(final String moduleName) {
    activeCachedModules.incrementAndGet();
  }

  public void recordModuleEvicted(final String moduleName) {
    activeCachedModules.decrementAndGet();
  }

  // Instance Operations
  public void recordInstanceCreated(final String instanceId, final String moduleName, final Duration creationTime) {
    final Attributes attributes = createInstanceAttributes(instanceId, moduleName);
    instancesCreatedCounter.add(1, attributes);
    instanceCreationTimeHistogram.record(creationTime.toNanos() / 1_000_000_000.0, attributes);
    activeInstances.incrementAndGet();
  }

  public void recordInstanceDestroyed(final String instanceId, final String moduleName) {
    activeInstances.decrementAndGet();
  }

  public void recordInstanceCreationError(final String instanceId, final String moduleName, final String errorType) {
    instanceCreationErrorsCounter.add(1, createErrorAttributes(moduleName, errorType));
  }

  // Function Call Operations
  public void recordFunctionCall(final String functionName, final String functionType, final Duration duration) {
    final Attributes attributes = createFunctionAttributes(functionName, functionType, false);
    functionCallsCounter.add(1, attributes);
    functionCallDurationHistogram.record(duration.toNanos() / 1_000_000_000.0, attributes);
  }

  public void recordFunctionCallError(final String functionName, final String functionType, final String errorType) {
    final Attributes attributes = createFunctionErrorAttributes(functionName, functionType, errorType, false);
    functionCallErrorsCounter.add(1, attributes);
  }

  public void recordHostFunctionCall(final String functionName, final String functionType, final Duration duration) {
    final Attributes attributes = createFunctionAttributes(functionName, functionType, true);
    hostFunctionCallsCounter.add(1, attributes);
    hostFunctionCallDurationHistogram.record(duration.toNanos() / 1_000_000_000.0, attributes);
  }

  // Memory Operations
  public void recordMemoryAllocation(final long bytes) {
    memoryAllocationsCounter.add(1);
    totalMemoryUsage.addAndGet(bytes);
  }

  public void recordMemoryDeallocation(final long bytes) {
    memoryDeallocationsCounter.add(1);
    totalMemoryUsage.addAndGet(-bytes);
  }

  public void recordMemoryPages(final long pages) {
    wasmMemoryPages.set(pages);
  }

  public void recordMemoryGrow(final long oldPages, final long newPages, final Duration duration) {
    final Attributes attributes = Attributes.builder()
        .put("old_pages", oldPages)
        .put("new_pages", newPages)
        .build();
    memoryGrowTimeHistogram.record(duration.toNanos() / 1_000_000_000.0, attributes);
    wasmMemoryPages.set(newPages);
  }

  // WASI Operations
  public void recordWasiOperation(final String operation, final Duration duration) {
    final Attributes attributes = createWasiAttributes(operation);
    wasiOperationsCounter.add(1, attributes);
    wasiOperationDurationHistogram.record(duration.toNanos() / 1_000_000_000.0, attributes);
  }

  public void recordWasiError(final String operation, final String errorType) {
    wasiErrorsCounter.add(1, createWasiErrorAttributes(operation, errorType));
  }

  public void recordWasiFileOperation(final String operation, final String filePath) {
    final Attributes attributes = Attributes.builder()
        .put("wasi.operation", operation)
        .put("wasi.file.path", filePath)
        .build();
    wasiFileOperationsCounter.add(1, attributes);
  }

  public void recordWasiNetworkOperation(final String operation, final String address) {
    final Attributes attributes = Attributes.builder()
        .put("wasi.operation", operation)
        .put("wasi.network.address", address)
        .build();
    wasiNetworkOperationsCounter.add(1, attributes);
  }

  // Performance Operations
  public void recordCompilationCacheHitRate(final double hitRate) {
    compilationCacheHitRateGauge.record(hitRate);
  }

  public void recordGcTriggered(final String gcType, final Duration duration) {
    final Attributes attributes = Attributes.builder()
        .put("gc.type", gcType)
        .build();
    gcTriggeredCounter.add(1, attributes);
    gcDurationHistogram.record(duration.toNanos() / 1_000_000_000.0, attributes);
  }

  // Error Operations
  public void recordTrapOccurred(final String trapType, final String functionName) {
    final Attributes attributes = Attributes.builder()
        .put("trap.type", trapType)
        .put("wasm.function.name", functionName != null ? functionName : "unknown")
        .build();
    trapOccurredCounter.add(1, attributes);
  }

  public void recordLinkingError(final String errorType, final String moduleName) {
    linkingErrorsCounter.add(1, createErrorAttributes(moduleName, errorType));
  }

  public void recordValidationError(final String errorType, final String moduleName) {
    validationErrorsCounter.add(1, createErrorAttributes(moduleName, errorType));
  }

  // Timing Operations
  public void startOperation(final String operationId) {
    operationStartTimes.put(operationId, Instant.now());
  }

  public Duration endOperation(final String operationId) {
    final Instant startTime = operationStartTimes.remove(operationId);
    if (startTime != null) {
      return Duration.between(startTime, Instant.now());
    }
    return Duration.ZERO;
  }

  // Attribute Creation Helpers
  private Attributes createEngineAttributes(final String engineType) {
    return Attributes.builder()
        .put("wasm.engine.type", engineType)
        .put("wasm.runtime", "wasmtime")
        .build();
  }

  private Attributes createStoreAttributes(final String storeId) {
    return Attributes.builder()
        .put("wasm.store.id", storeId)
        .put("wasm.runtime", "wasmtime")
        .build();
  }

  private Attributes createModuleAttributes(final String moduleName) {
    final AttributesBuilder builder = Attributes.builder()
        .put("wasm.runtime", "wasmtime");
    if (moduleName != null) {
      builder.put("wasm.module.name", moduleName);
    }
    return builder.build();
  }

  private Attributes createInstanceAttributes(final String instanceId, final String moduleName) {
    final AttributesBuilder builder = Attributes.builder()
        .put("wasm.runtime", "wasmtime")
        .put("wasm.instance.id", instanceId);
    if (moduleName != null) {
      builder.put("wasm.module.name", moduleName);
    }
    return builder.build();
  }

  private Attributes createFunctionAttributes(final String functionName, final String functionType, final boolean isHost) {
    final AttributesBuilder builder = Attributes.builder()
        .put("wasm.runtime", "wasmtime")
        .put("wasm.function.name", functionName)
        .put("wasm.is_host_function", isHost);
    if (functionType != null) {
      builder.put("wasm.function.type", functionType);
    }
    return builder.build();
  }

  private Attributes createFunctionErrorAttributes(final String functionName, final String functionType, final String errorType, final boolean isHost) {
    final AttributesBuilder builder = Attributes.builder()
        .put("wasm.runtime", "wasmtime")
        .put("wasm.function.name", functionName)
        .put("wasm.error.type", errorType)
        .put("wasm.is_host_function", isHost);
    if (functionType != null) {
      builder.put("wasm.function.type", functionType);
    }
    return builder.build();
  }

  private Attributes createWasiAttributes(final String operation) {
    return Attributes.builder()
        .put("wasm.runtime", "wasmtime")
        .put("wasi.operation", operation)
        .build();
  }

  private Attributes createWasiErrorAttributes(final String operation, final String errorType) {
    return Attributes.builder()
        .put("wasm.runtime", "wasmtime")
        .put("wasi.operation", operation)
        .put("wasm.error.type", errorType)
        .build();
  }

  private Attributes createErrorAttributes(final String moduleName, final String errorType) {
    final AttributesBuilder builder = Attributes.builder()
        .put("wasm.runtime", "wasmtime")
        .put("wasm.error.type", errorType);
    if (moduleName != null) {
      builder.put("wasm.module.name", moduleName);
    }
    return builder.build();
  }
}