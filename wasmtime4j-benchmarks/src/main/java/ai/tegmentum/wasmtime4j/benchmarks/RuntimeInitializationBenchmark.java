package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.OptimizationLevel;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Optional;
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
import org.openjdk.jmh.infra.Blackhole;

/**
 * Benchmarks for runtime initialization and engine creation performance.
 *
 * <p>This benchmark class measures the performance characteristics of initializing Wasmtime4j
 * runtime engines, comparing JNI and Panama implementations across different configuration
 * scenarios.
 *
 * <p>Key metrics measured:
 *
 * <ul>
 *   <li>Engine creation time
 *   <li>Runtime initialization overhead
 *   <li>Configuration parsing performance
 *   <li>Memory allocation patterns during startup
 * </ul>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(
    value = 2,
    jvmArgs = {"-Xms1g", "-Xmx1g"})
public class RuntimeInitializationBenchmark extends BenchmarkBase {

  /** Runtime implementation to benchmark. */
  @Param({"JNI", "PANAMA", "AUTO"})
  private String runtimeTypeName;

  /** Engine configuration scenario to test. */
  @Param({"DEFAULT", "OPTIMIZED", "DEBUG"})
  private String configType;

  /** Converts string runtime type name to RuntimeType enum, handling AUTO case. */
  private RuntimeType getRuntimeType() {
    if ("AUTO".equals(runtimeTypeName)) {
      return getRecommendedRuntime();
    }
    return RuntimeType.valueOf(runtimeTypeName);
  }

  /**
   * Creates an {@link EngineConfig} based on the current {@code configType} parameter.
   *
   * @return the engine configuration matching the current config type
   */
  private EngineConfig createConfig() {
    switch (configType) {
      case "OPTIMIZED":
        return new EngineConfig().optimizationLevel(OptimizationLevel.SPEED);
      case "DEBUG":
        return EngineConfig.forDebug();
      case "DEFAULT":
      default:
        return new EngineConfig();
    }
  }

  /** Setup performed before each benchmark iteration. */
  @Setup(Level.Iteration)
  public void setupIteration() {
    // Force garbage collection to ensure clean memory state
    System.gc();
    try {
      Thread.sleep(10);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /** Cleanup performed after each benchmark iteration. */
  @TearDown(Level.Iteration)
  public void teardownIteration() {
    // Each benchmark method manages its own resources.
  }

  /**
   * Benchmarks basic engine creation performance.
   *
   * <p>Creates a WasmRuntime and Engine with the current configuration, then closes both. Measures
   * the overhead of engine creation across runtime types and configurations.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   * @return the created engine instance
   */
  @Benchmark
  public Engine benchmarkEngineCreation(final Blackhole blackhole) throws WasmException {
    WasmRuntime wasmRuntime = null;
    Engine engine = null;
    try {
      wasmRuntime = createRuntime(getRuntimeType());
      engine = createEngine(wasmRuntime);
      blackhole.consume(engine);
      return engine;
    } finally {
      closeQuietly(engine);
      closeQuietly(wasmRuntime);
    }
  }

  /**
   * Benchmarks full runtime initialization performance.
   *
   * <p>Creates a WasmRuntime, Engine with configuration, Store, and compiles the simple WAT module.
   * Measures the total initialization cost including module compilation.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkRuntimeInitialization(final Blackhole blackhole) throws WasmException {
    WasmRuntime wasmRuntime = null;
    Engine engine = null;
    Store store = null;
    Module module = null;
    try {
      wasmRuntime = createRuntime(getRuntimeType());
      engine = wasmRuntime.createEngine(createConfig());
      store = createStore(engine);
      module = compileWatModule(engine, SIMPLE_WAT_MODULE);
      blackhole.consume(store);
      blackhole.consume(module);
    } finally {
      closeQuietly(module);
      closeQuietly(store);
      closeQuietly(engine);
      closeQuietly(wasmRuntime);
    }
  }

  /**
   * Benchmarks the complete initialization cycle including function invocation.
   *
   * <p>Creates a WasmRuntime, Engine with configuration, Store, compiles a module, instantiates it,
   * retrieves the exported "add" function, and calls it. Measures end-to-end initialization through
   * first function call.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkFullInitializationCycle(final Blackhole blackhole) throws WasmException {
    WasmRuntime wasmRuntime = null;
    Engine engine = null;
    Store store = null;
    Module module = null;
    Instance instance = null;
    try {
      wasmRuntime = createRuntime(getRuntimeType());
      engine = wasmRuntime.createEngine(createConfig());
      store = createStore(engine);
      module = compileWatModule(engine, SIMPLE_WAT_MODULE);
      instance = instantiateModule(store, module);

      final Optional<WasmFunction> addFunc = instance.getFunction("add");
      if (addFunc.isPresent()) {
        final WasmValue[] result = addFunc.get().call(WasmValue.i32(3), WasmValue.i32(7));
        blackhole.consume(result);
      }

      final String benchmarkId = formatBenchmarkId("full_init", getRuntimeType());
      blackhole.consume(benchmarkId);
    } finally {
      closeQuietly(instance);
      closeQuietly(module);
      closeQuietly(store);
      closeQuietly(engine);
      closeQuietly(wasmRuntime);
    }
  }

  /**
   * Benchmarks runtime creation and immediate cleanup performance.
   *
   * <p>Creates the full resource chain (runtime, engine, store, module, instance), invokes a
   * function, then immediately closes all resources. Focuses on measuring the resource lifecycle
   * cost.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkCreateAndCleanup(final Blackhole blackhole) throws WasmException {
    WasmRuntime wasmRuntime = null;
    Engine engine = null;
    Store store = null;
    Module module = null;
    Instance instance = null;
    try {
      wasmRuntime = createRuntime(getRuntimeType());
      engine = wasmRuntime.createEngine(createConfig());
      store = createStore(engine);
      module = compileWatModule(engine, SIMPLE_WAT_MODULE);
      instance = instantiateModule(store, module);

      final Optional<WasmFunction> addFunc = instance.getFunction("add");
      if (addFunc.isPresent()) {
        final WasmValue[] result = addFunc.get().call(WasmValue.i32(1), WasmValue.i32(2));
        blackhole.consume(result);
      }
    } finally {
      closeQuietly(instance);
      closeQuietly(module);
      closeQuietly(store);
      closeQuietly(engine);
      closeQuietly(wasmRuntime);
    }
  }

  /**
   * Benchmarks multiple engine creation for pooling scenarios.
   *
   * <p>Creates 5 separate engines, each with its own store, compiled module, and instance. Each
   * instance's "add" function is invoked before all resources are closed. Measures the overhead of
   * managing multiple concurrent engine instances.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkMultipleEngineCreation(final Blackhole blackhole) throws WasmException {
    final int engineCount = 5;
    final WasmRuntime[] runtimes = new WasmRuntime[engineCount];
    final Engine[] engines = new Engine[engineCount];
    final Store[] stores = new Store[engineCount];
    final Module[] modules = new Module[engineCount];
    final Instance[] instances = new Instance[engineCount];

    try {
      for (int i = 0; i < engineCount; i++) {
        runtimes[i] = createRuntime(getRuntimeType());
        engines[i] = createEngine(runtimes[i]);
        stores[i] = createStore(engines[i]);
        modules[i] = compileWatModule(engines[i], SIMPLE_WAT_MODULE);
        instances[i] = instantiateModule(stores[i], modules[i]);

        final Optional<WasmFunction> addFunc = instances[i].getFunction("add");
        if (addFunc.isPresent()) {
          final WasmValue[] result = addFunc.get().call(WasmValue.i32(i), WasmValue.i32(i + 1));
          blackhole.consume(result);
        }
      }
    } finally {
      for (int i = engineCount - 1; i >= 0; i--) {
        closeQuietly(instances[i]);
        closeQuietly(modules[i]);
        closeQuietly(stores[i]);
        closeQuietly(engines[i]);
        closeQuietly(runtimes[i]);
      }
    }
  }

  /**
   * Benchmarks engine creation with different configuration types.
   *
   * <p>Creates three engines with DEFAULT, OPTIMIZED, and DEBUG configurations respectively,
   * comparing the overhead of each configuration path.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkConfigurationOverhead(final Blackhole blackhole) throws WasmException {
    final EngineConfig[] configs = {
      new EngineConfig(), EngineConfig.forSpeed(), EngineConfig.forDebug()
    };

    for (final EngineConfig config : configs) {
      WasmRuntime wasmRuntime = null;
      Engine engine = null;
      Store store = null;
      Module module = null;
      Instance instance = null;
      try {
        wasmRuntime = createRuntime(getRuntimeType());
        engine = wasmRuntime.createEngine(config);
        store = createStore(engine);
        module = compileWatModule(engine, SIMPLE_WAT_MODULE);
        instance = instantiateModule(store, module);

        blackhole.consume(config.getOptimizationLevel());
        blackhole.consume(instance);
      } finally {
        closeQuietly(instance);
        closeQuietly(module);
        closeQuietly(store);
        closeQuietly(engine);
        closeQuietly(wasmRuntime);
      }
    }
  }

  /**
   * Benchmarks runtime initialization under memory pressure.
   *
   * <p>Allocates 100 arrays of 1KB each to simulate memory pressure, then creates the full resource
   * chain (runtime, engine, store, module). Measures initialization performance when the JVM heap
   * is under load.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkInitializationWithMemoryPressure(final Blackhole blackhole)
      throws WasmException {
    // Allocate memory to simulate pressure
    final byte[][] memoryPressure = new byte[100][];
    for (int i = 0; i < memoryPressure.length; i++) {
      memoryPressure[i] = new byte[1024];
      blackhole.consume(memoryPressure[i].length);
    }

    WasmRuntime wasmRuntime = null;
    Engine engine = null;
    Store store = null;
    Module module = null;
    try {
      wasmRuntime = createRuntime(getRuntimeType());
      engine = wasmRuntime.createEngine(createConfig());
      store = createStore(engine);
      module = compileWatModule(engine, SIMPLE_WAT_MODULE);
      blackhole.consume(module);
    } finally {
      closeQuietly(module);
      closeQuietly(store);
      closeQuietly(engine);
      closeQuietly(wasmRuntime);
    }

    // Clear memory pressure
    for (int i = 0; i < memoryPressure.length; i++) {
      memoryPressure[i] = null;
    }
  }
}
