package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.WasmException;
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
 * Benchmarks for WebAssembly module loading, compilation, and instantiation performance.
 *
 * <p>This benchmark class measures the performance characteristics of WebAssembly module
 * operations, comparing JNI and Panama implementations across different module types and sizes.
 *
 * <p>Key metrics measured:
 *
 * <ul>
 *   <li>Module compilation time
 *   <li>Module validation performance
 *   <li>Instance creation overhead
 *   <li>Module caching efficiency
 *   <li>Large module handling performance
 * </ul>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(
    value = 2,
    jvmArgs = {"-Xms1g", "-Xmx2g"})
public class ModuleOperationBenchmark extends BenchmarkBase {

  /** Runtime implementation to benchmark. */
  @Param({"JNI", "PANAMA"})
  private RuntimeType runtimeType;

  /** Module type to test with different complexity levels. */
  @Param({"SIMPLE", "COMPLEX", "LARGE"})
  private String moduleType;

  /** WebAssembly runtime components. */
  private WasmRuntime runtime;
  private Engine engine;
  private Store store;

  /** Module bytecode based on the selected type. */
  private byte[] moduleBytes;
  
  /** Compiled WebAssembly module. */
  private Module compiledModule;
  
  /** WebAssembly instance. */
  private Instance wasmInstance;

  /** Setup performed before each benchmark iteration. */
  @Setup(Level.Iteration)
  public void setupIteration() throws WasmException {
    // Create runtime components
    runtime = createRuntime(runtimeType);
    engine = createEngine(runtime);
    store = createStore(engine);
    
    // Select appropriate module bytes based on type
    switch (moduleType) {
      case "SIMPLE":
        moduleBytes = SIMPLE_WASM_MODULE.clone();
        break;
      case "COMPLEX":
        moduleBytes = COMPLEX_WASM_MODULE.clone();
        break;
      case "LARGE":
        moduleBytes = generateLargeModule();
        break;
      default:
        moduleBytes = SIMPLE_WASM_MODULE.clone();
        break;
    }

    // Clean up any existing compiled resources
    cleanup();

    // Force GC to ensure clean state
    System.gc();
  }

  /** Cleanup performed after each benchmark iteration. */
  @TearDown(Level.Iteration)
  public void teardownIteration() {
    cleanup();
    moduleBytes = null;
  }
  
  /** Helper method to clean up WebAssembly resources. */
  private void cleanup() {
    try {
      if (wasmInstance != null) {
        wasmInstance.close();
        wasmInstance = null;
      }
      if (compiledModule != null) {
        compiledModule.close();
        compiledModule = null;
      }
      if (store != null) {
        store.close();
        store = null;
      }
      if (engine != null) {
        engine.close();
        engine = null;
      }
      if (runtime != null) {
        runtime.close();
        runtime = null;
      }
    } catch (final Exception e) {
      // Ignore cleanup errors in benchmarks
    }
  }

  /**
   * Benchmarks WebAssembly module compilation performance.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   * @return the compiled module
   */
  @Benchmark
  public Module benchmarkModuleCompilation(final Blackhole blackhole) {
    try {
      final Module module = compileModule(engine, moduleBytes);
      blackhole.consume(module.getName());
      blackhole.consume(moduleBytes.length);
      return module;
    } catch (final WasmException e) {
      throw new RuntimeException("Module compilation failed", e);
    }
  }

  /**
   * Benchmarks module validation without compilation.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkModuleValidation(final Blackhole blackhole) {
    try {
      // Validate the WebAssembly module
      final boolean isValid = engine.validateModule(moduleBytes);
      blackhole.consume(isValid);
      blackhole.consume(moduleBytes.length);
      blackhole.consume(moduleType);
    } catch (final WasmException e) {
      throw new RuntimeException("Module validation failed", e);
    }
  }

  /**
   * Benchmarks full module instantiation including compilation.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   * @return the instantiated module
   */
  @Benchmark
  public Instance benchmarkModuleInstantiation(final Blackhole blackhole) {
    try {
      final Module module = compileModule(engine, moduleBytes);
      final Instance instance = instantiateModule(store, module);
      
      blackhole.consume(module.getName());
      blackhole.consume(instance.getExports().size());
      
      // Clean up for next iteration
      instance.close();
      module.close();
      
      return instance;
    } catch (final WasmException e) {
      throw new RuntimeException("Module instantiation failed", e);
    }
  }

  /**
   * Benchmarks compile-then-instantiate workflow.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   * @return the processed module
   */
  @Benchmark
  public Instance benchmarkCompileThenInstantiate(final Blackhole blackhole) {
    try {
      // First compile
      final Module module = compileModule(engine, moduleBytes);
      blackhole.consume(module.getName());

      // Then instantiate
      final Instance instance = instantiateModule(store, module);
      blackhole.consume(instance.getExports().size());

      // Clean up
      instance.close();
      module.close();
      
      return instance;
    } catch (final WasmException e) {
      throw new RuntimeException("Compile-then-instantiate failed", e);
    }
  }

  /**
   * Benchmarks multiple module compilation for batch scenarios.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkBatchModuleCompilation(final Blackhole blackhole) {
    final int batchSize = moduleType.equals("LARGE") ? 3 : 5;
    final Module[] modules = new Module[batchSize];

    try {
      for (int i = 0; i < batchSize; i++) {
        modules[i] = compileModule(engine, moduleBytes);
        blackhole.consume(modules[i].getName());
      }
    } catch (final WasmException e) {
      throw new RuntimeException("Batch module compilation failed", e);
    } finally {
      // Cleanup
      for (final Module module : modules) {
        if (module != null) {
          try {
            module.close();
          } catch (final Exception e) {
            // Ignore cleanup errors
          }
        }
      }
    }
  }

  /**
   * Benchmarks module compilation with memory pressure.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkCompilationWithMemoryPressure(final Blackhole blackhole) {
    // Create memory pressure
    final byte[][] memoryPressure = new byte[200][];
    for (int i = 0; i < memoryPressure.length; i++) {
      memoryPressure[i] = new byte[2048]; // 2KB per allocation
    }

    try {
      final Module module = compileModule(engine, moduleBytes);
      blackhole.consume(module.getName());
      blackhole.consume(memoryPressure.length);
      module.close();
    } catch (final WasmException e) {
      throw new RuntimeException("Compilation with memory pressure failed", e);
    } finally {
      // Clear memory pressure
      for (int i = 0; i < memoryPressure.length; i++) {
        memoryPressure[i] = null;
      }
    }
  }

  /**
   * Benchmarks module serialization and deserialization performance.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkModuleSerialization(final Blackhole blackhole) {
    try {
      final Module module = compileModule(engine, moduleBytes);
      
      // Serialize compiled module
      final byte[] serialized = module.serialize();
      blackhole.consume(serialized.length);

      // Deserialize from bytes
      final Module deserializedModule = engine.deserializeModule(serialized);
      blackhole.consume(deserializedModule.getName());

      // Clean up
      module.close();
      deserializedModule.close();
    } catch (final WasmException e) {
      throw new RuntimeException("Module serialization failed", e);
    }
  }

  /**
   * Generates a larger WebAssembly module for testing scalability.
   *
   * @return byte array representing a large WebAssembly module
   */
  private byte[] generateLargeModule() {
    // Create a larger module by duplicating and modifying the complex module
    final byte[] base = COMPLEX_WASM_MODULE.clone();
    final byte[] large = new byte[base.length * 3];

    // Copy base module multiple times with slight modifications
    System.arraycopy(base, 0, large, 0, base.length);
    System.arraycopy(base, 0, large, base.length, base.length);
    System.arraycopy(base, 0, large, base.length * 2, base.length);

    // Add some variation to make it realistic
    for (int i = base.length; i < large.length; i++) {
      if (large[i] != 0x00 && large[i] != 0x61 && large[i] != 0x73 && large[i] != 0x6d) {
        large[i] = (byte) ((large[i] + i) % 256);
      }
    }

    return large;
  }
}
