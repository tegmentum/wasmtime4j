package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

/**
 * JMH benchmarks for WebAssembly execution control system performance.
 *
 * <p>Measures the overhead of fuel management, epoch interruption, resource quotas, and other
 * execution control mechanisms to ensure minimal performance impact on WebAssembly execution.
 *
 * <p>Tests both JNI and Panama implementations via the runtimeType parameter.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(
    value = 1,
    jvmArgs = {"--enable-native-access=ALL-UNNAMED"})
@State(Scope.Benchmark)
public class ExecutionControlBenchmark {

  @Param({"JNI", "PANAMA"})
  private String runtimeType;

  private WasmRuntime runtime;
  private Engine engine;
  private Store store;

  private static final long INITIAL_FUEL = 1_000_000L;
  private static final long FUEL_ALLOCATION = 1000L;
  private static final long FUEL_CONSUMPTION = 10L;

  @Setup(Level.Trial)
  public void setupTrial() throws WasmException {
    // Get the runtime based on parameter
    RuntimeType type = RuntimeType.valueOf(runtimeType);
    if (!WasmRuntimeFactory.isRuntimeAvailable(type)) {
      throw new RuntimeException("Runtime not available: " + type);
    }
    runtime = WasmRuntimeFactory.create(type);

    // Initialize engine with fuel consumption and epoch interruption enabled
    EngineConfig config =
        new EngineConfig()
            .consumeFuel(true)
            .setEpochInterruption(true)
            .craneliftDebugVerifier(false);

    engine = runtime.createEngine(config);
    store = engine.createStore();

    // Pre-allocate initial fuel
    store.setFuel(INITIAL_FUEL);

    System.out.println("Execution control benchmark setup complete");
    System.out.println("Initial fuel: " + store.getFuel());
  }

  @TearDown(Level.Trial)
  public void teardownTrial() throws Exception {
    if (store != null) {
      store.close();
    }
    if (engine != null) {
      engine.close();
    }
    if (runtime != null) {
      runtime.close();
    }
  }

  @Setup(Level.Iteration)
  public void setupIteration() throws WasmException {
    // Reset fuel at the start of each iteration
    store.setFuel(INITIAL_FUEL);
  }

  // Fuel Management Benchmarks

  @Benchmark
  @OperationsPerInvocation(1)
  public void benchmarkFuelAllocation(Blackhole bh) throws WasmException {
    store.setFuel(store.getFuel() + FUEL_ALLOCATION);
    bh.consume(store.getFuel());
  }

  @Benchmark
  @OperationsPerInvocation(1)
  public void benchmarkFuelConsumption(Blackhole bh) throws WasmException {
    long consumed = store.consumeFuel(FUEL_CONSUMPTION);
    bh.consume(consumed);
  }

  @Benchmark
  @OperationsPerInvocation(1)
  public void benchmarkGetFuel(Blackhole bh) throws WasmException {
    long remaining = store.getFuel();
    bh.consume(remaining);
  }

  @Benchmark
  @OperationsPerInvocation(1)
  public void benchmarkSetFuel(Blackhole bh) throws WasmException {
    store.setFuel(INITIAL_FUEL);
    bh.consume(store.getFuel());
  }

  @Benchmark
  @OperationsPerInvocation(10)
  public void benchmarkBurstFuelConsumption(Blackhole bh) throws WasmException {
    // Simulate burst consumption pattern
    long totalConsumed = 0;
    for (int i = 0; i < 10; i++) {
      totalConsumed += store.consumeFuel(FUEL_CONSUMPTION);
    }
    bh.consume(totalConsumed);
  }

  @Benchmark
  @OperationsPerInvocation(1)
  public void benchmarkFuelRefill(Blackhole bh) throws WasmException {
    // Consume some fuel, then refill
    store.consumeFuel(FUEL_CONSUMPTION * 100);
    store.setFuel(INITIAL_FUEL);
    bh.consume(store.getFuel());
  }

  // Epoch Interruption Benchmarks

  @Benchmark
  @OperationsPerInvocation(1)
  public void benchmarkSetEpochDeadline(Blackhole bh) throws WasmException {
    store.setEpochDeadline(100L);
    bh.consume(store);
  }

  @Benchmark
  @OperationsPerInvocation(1)
  public void benchmarkIncrementEpoch(Blackhole bh) throws WasmException {
    engine.incrementEpoch();
    bh.consume(engine);
  }

  @Benchmark
  @OperationsPerInvocation(10)
  public void benchmarkBurstEpochIncrements(Blackhole bh) throws WasmException {
    // Simulate burst epoch increments
    for (int i = 0; i < 10; i++) {
      engine.incrementEpoch();
    }
    bh.consume(engine);
  }

  @Benchmark
  @OperationsPerInvocation(1)
  public void benchmarkEpochDeadlineUpdate(Blackhole bh) throws WasmException {
    // Update deadline multiple times
    store.setEpochDeadline(10L);
    store.setEpochDeadline(20L);
    store.setEpochDeadline(30L);
    bh.consume(store);
  }

  // Combined Operations

  @Benchmark
  @OperationsPerInvocation(1)
  public void benchmarkCombinedFuelAndEpoch(Blackhole bh) throws WasmException {
    // Simulate typical execution control pattern
    store.setFuel(INITIAL_FUEL);
    store.setEpochDeadline(100L);
    engine.incrementEpoch();
    long fuel = store.getFuel();
    bh.consume(fuel);
  }

  @Benchmark
  @OperationsPerInvocation(1)
  public void benchmarkExecutionControlOverhead(Blackhole bh) throws WasmException {
    // Measure overall execution control overhead
    long startFuel = store.getFuel();
    store.setEpochDeadline(50L);

    // Simulate some fuel consumption
    store.consumeFuel(100L);

    engine.incrementEpoch();

    long endFuel = store.getFuel();
    bh.consume(startFuel - endFuel);
  }

  // Store Creation/Destruction Benchmarks (measures execution context creation)

  @Benchmark
  @OperationsPerInvocation(1)
  public void benchmarkStoreCreation(Blackhole bh) throws WasmException {
    Store newStore = engine.createStore();
    bh.consume(newStore);
    newStore.close();
  }

  @Benchmark
  @OperationsPerInvocation(1)
  public void benchmarkStoreWithFuelSetup(Blackhole bh) throws WasmException {
    Store newStore = engine.createStore();
    newStore.setFuel(INITIAL_FUEL);
    newStore.setEpochDeadline(100L);
    bh.consume(newStore.getFuel());
    newStore.close();
  }

  // Fuel Limit Boundary Testing

  @Benchmark
  @OperationsPerInvocation(1)
  public void benchmarkLargeFuelAllocation(Blackhole bh) throws WasmException {
    store.setFuel(Long.MAX_VALUE / 2);
    bh.consume(store.getFuel());
    // Reset for next iteration
    store.setFuel(INITIAL_FUEL);
  }

  @Benchmark
  @OperationsPerInvocation(1)
  public void benchmarkMinimalFuelAllocation(Blackhole bh) throws WasmException {
    store.setFuel(1L);
    bh.consume(store.getFuel());
    // Reset for next iteration
    store.setFuel(INITIAL_FUEL);
  }

  /** Main method to run the benchmark. */
  public static void main(String[] args) throws Exception {
    org.openjdk.jmh.Main.main(args);
  }
}
