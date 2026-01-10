package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
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
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Production workload benchmark simulating realistic usage scenarios.
 *
 * <p>This benchmark validates wasmtime4j performance under realistic production workloads that
 * closely match real-world application usage patterns and requirements.
 *
 * <p>Simulated workload scenarios include:
 *
 * <ul>
 *   <li>Serverless function execution patterns
 *   <li>Plugin system with dynamic module loading
 *   <li>Data processing pipeline workloads
 *   <li>Web service backend processing
 *   <li>Multi-tenant application scenarios
 *   <li>Long-running microservice patterns
 * </ul>
 *
 * <p>Production characteristics tested:
 *
 * <ul>
 *   <li>Mixed allocation patterns with varying lifetimes
 *   <li>Concurrent execution with realistic thread counts
 *   <li>Resource cleanup under production load
 *   <li>Error handling in production scenarios
 *   <li>Memory usage patterns typical of production systems
 * </ul>
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(2)
@State(Scope.Benchmark)
public class ProductionWorkloadBenchmark extends BenchmarkBase {

  /** Data processing WebAssembly module. */
  private static final byte[] DATA_PROCESSOR_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6d, // WASM magic
        0x01,
        0x00,
        0x00,
        0x00, // Version
        0x01,
        0x10,
        0x04, // Type section
        0x60,
        0x02,
        0x7f,
        0x7f,
        0x01,
        0x7f, // Type 0: (i32, i32) -> i32 (sum)
        0x60,
        0x03,
        0x7f,
        0x7f,
        0x7f,
        0x01,
        0x7f, // Type 1: (i32, i32, i32) -> i32 (average)
        0x60,
        0x01,
        0x7f,
        0x01,
        0x7f, // Type 2: (i32) -> i32 (square)
        0x60,
        0x02,
        0x7f,
        0x7f,
        0x00, // Type 3: (i32, i32) -> () (store_result)
        0x03,
        0x05,
        0x04,
        0x00,
        0x01,
        0x02,
        0x03, // Function section: 4 functions
        0x05,
        0x03,
        0x01,
        0x00,
        0x04, // Memory section: 4 pages initial
        0x07,
        0x2a,
        0x04, // Export section
        0x03,
        0x73,
        0x75,
        0x6d,
        0x00,
        0x00, // Export "sum" as function 0
        0x07,
        0x61,
        0x76,
        0x65,
        0x72,
        0x61,
        0x67,
        0x65,
        0x00,
        0x01, // Export "average" as function 1
        0x06,
        0x73,
        0x71,
        0x75,
        0x61,
        0x72,
        0x65,
        0x00,
        0x02, // Export "square" as function 2
        0x0c,
        0x73,
        0x74,
        0x6f,
        0x72,
        0x65,
        0x5f,
        0x72,
        0x65,
        0x73,
        0x75,
        0x6c,
        0x74,
        0x00,
        0x03, // Export "store_result"
        0x0a,
        0x22,
        0x04, // Code section: 4 function bodies
        0x07,
        0x00,
        0x20,
        0x00,
        0x20,
        0x01,
        0x6a,
        0x0b, // Function 0: sum
        0x0b,
        0x00,
        0x20,
        0x00,
        0x20,
        0x01,
        0x6a,
        0x20,
        0x02,
        0x6a,
        0x41,
        0x03,
        0x6d,
        0x0b, // Function 1: average
        0x07,
        0x00,
        0x20,
        0x00,
        0x20,
        0x00,
        0x6c,
        0x0b, // Function 2: square
        0x07,
        0x00,
        0x20,
        0x00,
        0x20,
        0x01,
        0x36,
        0x02,
        0x00,
        0x0b // Function 3: store_result
      };

  /** Serverless function WebAssembly module. */
  private static final byte[] SERVERLESS_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6d, // WASM magic
        0x01,
        0x00,
        0x00,
        0x00, // Version
        0x01,
        0x0c,
        0x03, // Type section
        0x60,
        0x01,
        0x7f,
        0x01,
        0x7f, // Type 0: (i32) -> i32 (handler)
        0x60,
        0x02,
        0x7f,
        0x7f,
        0x01,
        0x7f, // Type 1: (i32, i32) -> i32 (validate)
        0x60,
        0x00,
        0x01,
        0x7f, // Type 2: () -> i32 (status)
        0x03,
        0x04,
        0x03,
        0x00,
        0x01,
        0x02, // Function section: 3 functions
        0x07,
        0x1c,
        0x03, // Export section
        0x07,
        0x68,
        0x61,
        0x6e,
        0x64,
        0x6c,
        0x65,
        0x72,
        0x00,
        0x00, // Export "handler" as function 0
        0x08,
        0x76,
        0x61,
        0x6c,
        0x69,
        0x64,
        0x61,
        0x74,
        0x65,
        0x00,
        0x01, // Export "validate" as function 1
        0x06,
        0x73,
        0x74,
        0x61,
        0x74,
        0x75,
        0x73,
        0x00,
        0x02, // Export "status" as function 2
        0x0a,
        0x1c,
        0x03, // Code section: 3 function bodies
        0x0c,
        0x00,
        0x20,
        0x00,
        0x41,
        0x0a,
        0x6c,
        0x41,
        0x05,
        0x6a,
        0x41,
        0x64,
        0x70,
        0x0b, // Function 0: handler (multiply by 10, add 5, mod 100)
        0x09,
        0x00,
        0x20,
        0x00,
        0x20,
        0x01,
        0x4a,
        0x04,
        0x7f,
        0x41,
        0x01,
        0x05,
        0x41,
        0x00,
        0x0b,
        0x0b, // Function 1: validate (a > b ? 1 : 0)
        0x04,
        0x00,
        0x41,
        0xc8,
        0x00,
        0x0b // Function 2: status (return 200)
      };

  /** Plugin system WebAssembly module. */
  private static final byte[] PLUGIN_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6d, // WASM magic
        0x01,
        0x00,
        0x00,
        0x00, // Version
        0x01,
        0x10,
        0x04, // Type section
        0x60,
        0x00,
        0x00, // Type 0: () -> () (init)
        0x60,
        0x01,
        0x7f,
        0x01,
        0x7f, // Type 1: (i32) -> i32 (process)
        0x60,
        0x00,
        0x01,
        0x7f, // Type 2: () -> i32 (cleanup)
        0x60,
        0x02,
        0x7f,
        0x7f,
        0x01,
        0x7f, // Type 3: (i32, i32) -> i32 (configure)
        0x03,
        0x05,
        0x04,
        0x00,
        0x01,
        0x02,
        0x03, // Function section: 4 functions
        0x07,
        0x23,
        0x04, // Export section
        0x04,
        0x69,
        0x6e,
        0x69,
        0x74,
        0x00,
        0x00, // Export "init" as function 0
        0x07,
        0x70,
        0x72,
        0x6f,
        0x63,
        0x65,
        0x73,
        0x73,
        0x00,
        0x01, // Export "process" as function 1
        0x07,
        0x63,
        0x6c,
        0x65,
        0x61,
        0x6e,
        0x75,
        0x70,
        0x00,
        0x02, // Export "cleanup" as function 2
        0x09,
        0x63,
        0x6f,
        0x6e,
        0x66,
        0x69,
        0x67,
        0x75,
        0x72,
        0x65,
        0x00,
        0x03, // Export "configure" as function 3
        0x0a,
        0x15,
        0x04, // Code section: 4 function bodies
        0x02,
        0x00,
        0x0b, // Function 0: init (noop)
        0x07,
        0x00,
        0x20,
        0x00,
        0x41,
        0x02,
        0x74,
        0x0b, // Function 1: process (multiply by 4)
        0x04,
        0x00,
        0x41,
        0x01,
        0x0b, // Function 2: cleanup (return 1)
        0x07,
        0x00,
        0x20,
        0x00,
        0x20,
        0x01,
        0x6a,
        0x0b // Function 3: configure (add parameters)
      };

  @Param({"JNI", "PANAMA"})
  private String runtimeTypeName;

  @Param({"SERVERLESS", "PLUGIN_SYSTEM", "DATA_PROCESSING", "WEB_SERVICE"})
  private String workloadType;

  private Engine engine;
  private List<Module> modules;
  private List<Store> stores;
  private List<Instance> instances;

  @Setup(Level.Trial)
  public void setupTrial() throws WasmException {
    super.setupRuntime(runtimeTypeName);

    // Create production-optimized engine with enterprise features
    engine =
        Engine.builder()
            .optimizationLevel("speed")
            .crankLiftOptLevel("speed")
            .poolingAllocator(true)
            .poolSize(500)
            .moduleCaching(true)
            .performanceMonitoring(true)
            .build();

    modules = new ArrayList<>();
    stores = new ArrayList<>();
    instances = new ArrayList<>();

    // Pre-compile modules based on workload type
    switch (workloadType) {
      case "SERVERLESS":
        setupServerlessWorkload();
        break;
      case "PLUGIN_SYSTEM":
        setupPluginSystemWorkload();
        break;
      case "DATA_PROCESSING":
        setupDataProcessingWorkload();
        break;
      case "WEB_SERVICE":
        setupWebServiceWorkload();
        break;
      default:
        throw new IllegalArgumentException("Unknown workload type: " + workloadType);
    }

    logInfo(
        "Production workload benchmark setup completed for runtime: "
            + runtimeTypeName
            + ", workload: "
            + workloadType);
  }

  @TearDown(Level.Trial)
  public void tearDownTrial() {
    for (final Instance instance : instances) {
      closeQuietly(instance);
    }
    instances.clear();

    for (final Store store : stores) {
      closeQuietly(store);
    }
    stores.clear();

    for (final Module module : modules) {
      closeQuietly(module);
    }
    modules.clear();

    closeQuietly(engine);

    super.tearDownRuntime();
    logInfo("Production workload benchmark teardown completed for runtime: " + runtimeTypeName);
  }

  /**
   * Benchmarks serverless function execution patterns. Simulates AWS Lambda, Azure Functions, or
   * Google Cloud Functions workloads.
   */
  @Benchmark
  public void benchmarkServerlessFunctionExecution() throws WasmException {
    if (!"SERVERLESS".equals(workloadType)) {
      return;
    }

    // Simulate serverless cold start
    final Store store = Store.withoutData(engine);
    try {
      final Instance instance = Instance.create(store, modules.get(0));
      try {
        final Function handler = instance.getFunction("handler");
        final Function validate = instance.getFunction("validate");
        final Function status = instance.getFunction("status");

        // Simulate request processing
        final int requestId = ThreadLocalRandom.current().nextInt(1000);
        final int result = handler.call(requestId);

        // Validate result
        final int isValid = validate.call(result, 50);

        // Get status
        final int statusCode = status.call();

        // Simulate response preparation
        if (isValid == 1 && statusCode == 200) {
          // Success path
        } else {
          // Error handling
          logWarn("Request processing failed");
        }

      } finally {
        closeQuietly(instance);
      }
    } finally {
      closeQuietly(store);
    }
  }

  /**
   * Benchmarks plugin system with dynamic loading. Simulates application plugin architecture
   * workloads.
   */
  @Benchmark
  public void benchmarkPluginSystemWorkload() throws WasmException {
    if (!"PLUGIN_SYSTEM".equals(workloadType)) {
      return;
    }

    // Simulate plugin lifecycle management
    final Store store = Store.withoutData(engine);
    try {
      final Instance instance = Instance.create(store, modules.get(0));
      try {
        final Function init = instance.getFunction("init");
        final Function process = instance.getFunction("process");
        final Function configure = instance.getFunction("configure");
        final Function cleanup = instance.getFunction("cleanup");

        // Plugin initialization
        init.call();

        // Configuration
        final int config1 = ThreadLocalRandom.current().nextInt(100);
        final int config2 = ThreadLocalRandom.current().nextInt(100);
        configure.call(config1, config2);

        // Process multiple requests
        for (int i = 0; i < 10; i++) {
          final int data = ThreadLocalRandom.current().nextInt(1000);
          process.call(data);
        }

        // Plugin cleanup
        cleanup.call();

      } finally {
        closeQuietly(instance);
      }
    } finally {
      closeQuietly(store);
    }
  }

  /**
   * Benchmarks data processing pipeline workloads. Simulates ETL, analytics, and data
   * transformation scenarios.
   */
  @Benchmark
  public void benchmarkDataProcessingWorkload() throws WasmException {
    if (!"DATA_PROCESSING".equals(workloadType)) {
      return;
    }

    // Simulate data processing pipeline
    final Store store = Store.withoutData(engine);
    try {
      final Instance instance = Instance.create(store, modules.get(0));
      try {
        final Function sum = instance.getFunction("sum");
        final Function average = instance.getFunction("average");
        final Function square = instance.getFunction("square");
        final Function storeResult = instance.getFunction("store_result");

        // Process batch of data
        final int[] dataset = new int[20];
        for (int i = 0; i < dataset.length; i++) {
          dataset[i] = ThreadLocalRandom.current().nextInt(100);
        }

        // Data transformation pipeline
        int totalSum = 0;
        for (int i = 0; i < dataset.length - 1; i += 2) {
          final int pairSum = sum.call(dataset[i], dataset[i + 1]);
          totalSum += pairSum;
        }

        // Calculate average of first three values
        if (dataset.length >= 3) {
          final int avg = average.call(dataset[0], dataset[1], dataset[2]);
          storeResult.call(0, avg);
        }

        // Square transformation
        for (int i = 0; i < Math.min(dataset.length, 5); i++) {
          final int squared = square.call(dataset[i]);
          storeResult.call((i + 1) * 4, squared);
        }

      } finally {
        closeQuietly(instance);
      }
    } finally {
      closeQuietly(store);
    }
  }

  /**
   * Benchmarks web service backend processing. Simulates REST API, microservice, and web backend
   * workloads.
   */
  @Benchmark
  @Threads(8)
  public void benchmarkWebServiceWorkload() throws WasmException {
    if (!"WEB_SERVICE".equals(workloadType)) {
      return;
    }

    // Simulate concurrent web requests
    final Store store = Store.withoutData(engine);
    try {
      final Instance instance = Instance.create(store, modules.get(0));
      try {
        final Function handler = instance.getFunction("handler");
        final Function validate = instance.getFunction("validate");

        // Simulate request processing with thread-specific data
        final int threadId = Thread.currentThread().hashCode() % 1000;
        final int requestData = ThreadLocalRandom.current().nextInt(1000) + threadId;

        // Process request
        final int result = handler.call(requestData);

        // Validate response
        final int isValid = validate.call(result, 200);

        if (isValid != 1) {
          logWarn("Request validation failed for thread: " + threadId);
        }

      } finally {
        closeQuietly(instance);
      }
    } finally {
      closeQuietly(store);
    }
  }

  /**
   * Benchmarks multi-tenant application scenarios. Simulates SaaS platform with tenant isolation.
   */
  @Benchmark
  @Threads(4)
  public void benchmarkMultiTenantWorkload() throws WasmException {
    // Each thread represents a different tenant
    final int tenantId = Thread.currentThread().hashCode() % 100;
    final Store tenantStore = Store.withoutData(engine);

    try {
      // Each tenant has its own instance for isolation
      final Module tenantModule = modules.get(tenantId % modules.size());
      final Instance tenantInstance = Instance.create(tenantStore, tenantModule);

      try {
        // Tenant-specific processing based on workload type
        switch (workloadType) {
          case "SERVERLESS":
            final Function handler = tenantInstance.getFunction("handler");
            handler.call(tenantId);
            break;
          case "PLUGIN_SYSTEM":
            final Function process = tenantInstance.getFunction("process");
            process.call(tenantId * 10);
            break;
          case "DATA_PROCESSING":
            final Function sum = tenantInstance.getFunction("sum");
            sum.call(tenantId, tenantId + 1);
            break;
          default:
            // Default processing
            break;
        }

      } finally {
        closeQuietly(tenantInstance);
      }
    } finally {
      closeQuietly(tenantStore);
    }
  }

  /**
   * Benchmarks long-running microservice patterns. Simulates persistent service instances with
   * periodic processing.
   */
  @Benchmark
  public void benchmarkLongRunningMicroservice() throws WasmException {
    // Use persistent instances for long-running services
    if (instances.isEmpty()) {
      return;
    }

    final Instance serviceInstance = instances.get(0);
    final ThreadLocalRandom random = ThreadLocalRandom.current();

    // Simulate periodic service tasks
    for (int i = 0; i < 5; i++) {
      switch (workloadType) {
        case "SERVERLESS":
          final Function handler = serviceInstance.getFunction("handler");
          handler.call(random.nextInt(1000));
          break;
        case "PLUGIN_SYSTEM":
          final Function process = serviceInstance.getFunction("process");
          process.call(random.nextInt(100));
          break;
        case "DATA_PROCESSING":
          final Function sum = serviceInstance.getFunction("sum");
          final Function square = serviceInstance.getFunction("square");
          final int value = random.nextInt(50);
          final int sumResult = sum.call(value, value + 1);
          square.call(sumResult);
          break;
        default:
          // Default processing
          break;
      }

      // Simulate service interval
      Thread.yield();
    }
  }

  /**
   * Benchmarks resource cleanup under production load. Tests efficiency of resource management in
   * high-load scenarios.
   */
  @Benchmark
  public void benchmarkResourceCleanupUnderLoad() throws WasmException {
    final List<Store> tempStores = new ArrayList<>();
    final List<Instance> tempInstances = new ArrayList<>();

    try {
      // Create multiple temporary resources
      for (int i = 0; i < 10; i++) {
        final Store store = Store.withoutData(engine);
        final Instance instance = Instance.create(store, modules.get(i % modules.size()));

        tempStores.add(store);
        tempInstances.add(instance);

        // Quick processing to simulate work
        switch (workloadType) {
          case "SERVERLESS":
            final Function handler = instance.getFunction("handler");
            handler.call(i);
            break;
          case "DATA_PROCESSING":
            final Function sum = instance.getFunction("sum");
            sum.call(i, i + 1);
            break;
          default:
            // Minimal processing
            break;
        }
      }

      // Simulate load processing
      for (int i = 0; i < tempInstances.size(); i++) {
        final Instance instance = tempInstances.get(i);
        final int processId = ThreadLocalRandom.current().nextInt(100);

        // Additional processing under load
        switch (workloadType) {
          case "PLUGIN_SYSTEM":
            final Function process = instance.getFunction("process");
            process.call(processId);
            break;
          default:
            // Default load simulation
            break;
        }
      }

    } finally {
      // Cleanup all temporary resources
      for (final Instance instance : tempInstances) {
        closeQuietly(instance);
      }
      for (final Store store : tempStores) {
        closeQuietly(store);
      }
    }
  }

  private void setupServerlessWorkload() throws WasmException {
    modules.add(Module.fromBinary(engine, SERVERLESS_WASM));

    // Pre-warm some instances for better performance
    final Store store = Store.withoutData(engine);
    stores.add(store);
    instances.add(Instance.create(store, modules.get(0)));
  }

  private void setupPluginSystemWorkload() throws WasmException {
    modules.add(Module.fromBinary(engine, PLUGIN_WASM));

    // Create multiple plugin instances
    for (int i = 0; i < 3; i++) {
      final Store store = Store.withoutData(engine);
      stores.add(store);
      instances.add(Instance.create(store, modules.get(0)));
    }
  }

  private void setupDataProcessingWorkload() throws WasmException {
    modules.add(Module.fromBinary(engine, DATA_PROCESSOR_WASM));

    // Create data processing instances
    for (int i = 0; i < 2; i++) {
      final Store store = Store.withoutData(engine);
      stores.add(store);
      instances.add(Instance.create(store, modules.get(0)));
    }
  }

  private void setupWebServiceWorkload() throws WasmException {
    // Use serverless module for web service simulation
    modules.add(Module.fromBinary(engine, SERVERLESS_WASM));

    // Create service instances for concurrent processing
    for (int i = 0; i < 4; i++) {
      final Store store = Store.withoutData(engine);
      stores.add(store);
      instances.add(Instance.create(store, modules.get(0)));
    }
  }

  private void closeQuietly(final AutoCloseable resource) {
    if (resource != null) {
      try {
        resource.close();
      } catch (final Exception e) {
        logWarn("Error closing resource: " + e.getMessage());
      }
    }
  }
}
