/*
 * Copyright 2025 Tegmentum AI
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

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
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
  private String runtimeTypeName;

  /** Module type to test with different complexity levels. */
  @Param({"SIMPLE", "COMPLEX", "LARGE"})
  private String moduleType;

  /** WebAssembly runtime components. */
  private WasmRuntime runtime;

  private Engine engine;
  private Store store;

  /** WAT source for the selected module type. */
  private String watSource;

  /** Compiled WebAssembly module. */
  private Module compiledModule;

  /** WebAssembly instance. */
  private Instance wasmInstance;

  /** Setup performed before each benchmark iteration. */
  @Setup(Level.Iteration)
  public void setupIteration() throws WasmException {
    // Create runtime components
    final RuntimeType runtimeType = RuntimeType.valueOf(runtimeTypeName);
    runtime = createRuntime(runtimeType);
    engine = createEngine(runtime);
    store = createStore(engine);

    // Select appropriate WAT source based on type
    switch (moduleType) {
      case "SIMPLE":
        watSource = SIMPLE_WAT_MODULE;
        break;
      case "COMPLEX":
        watSource = COMPLEX_WAT_MODULE;
        break;
      case "LARGE":
        watSource = LARGE_WAT_MODULE;
        break;
      default:
        watSource = SIMPLE_WAT_MODULE;
        break;
    }

    // Clean up any leftover compiled resources from prior iteration
    if (wasmInstance != null) {
      wasmInstance.close();
      wasmInstance = null;
    }
    if (compiledModule != null) {
      compiledModule.close();
      compiledModule = null;
    }
  }

  /** Cleanup performed after each benchmark iteration. */
  @TearDown(Level.Iteration)
  public void teardownIteration() {
    cleanup();
    watSource = null;
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
      final Module module = compileWatModule(engine, watSource);
      blackhole.consume(module.getName());
      blackhole.consume(watSource.length());
      return module;
    } catch (final WasmException e) {
      throw new RuntimeException("Module compilation failed", e);
    }
  }

  /**
   * Benchmarks module validation by compiling and checking the result.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkModuleValidation(final Blackhole blackhole) {
    try {
      final Module module = compileWatModule(engine, watSource);
      blackhole.consume(module.getName());
      module.close();
      blackhole.consume(true);
    } catch (final WasmException e) {
      blackhole.consume(false);
    }
    blackhole.consume(moduleType);
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
      final Module module = compileWatModule(engine, watSource);
      final Instance instance = instantiateModule(store, module);

      blackhole.consume(module.getName());

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
      final Module module = compileWatModule(engine, watSource);
      blackhole.consume(module.getName());

      // Then instantiate
      final Instance instance = instantiateModule(store, module);

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
        modules[i] = compileWatModule(engine, watSource);
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
      final Module module = compileWatModule(engine, watSource);
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
      final Module module = compileWatModule(engine, watSource);

      // Serialize compiled module
      final byte[] serialized = module.serialize();
      blackhole.consume(serialized.length);

      // Clean up
      module.close();
    } catch (final WasmException e) {
      throw new RuntimeException("Module serialization failed", e);
    }
  }
}
