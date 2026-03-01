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
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wasi.WasiConfig;
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
 * Benchmarks for WASI (WebAssembly System Interface) operations performance.
 *
 * <p>This benchmark class measures the performance characteristics of real Wasmtime WASI
 * operations, comparing JNI and Panama implementations across WASI linker initialization, module
 * compilation, instantiation, function calls, configuration creation, and resource cleanup.
 *
 * <p>Key metrics measured:
 *
 * <ul>
 *   <li>WASI linker creation and initialization overhead
 *   <li>WASI module compilation with WASI imports
 *   <li>WASI module instantiation via linker
 *   <li>Function call overhead in WASI-linked instances
 *   <li>WasiConfig builder API overhead
 *   <li>Batch WASI operations (end-to-end)
 *   <li>WASI resource cleanup performance
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
public class WasiBenchmark extends BenchmarkBase {

  /**
   * Simple WASI WAT module that imports proc_exit from wasi_snapshot_preview1, exports a compute
   * function (i32 add), a _start function, and memory.
   */
  private static final String SIMPLE_WASI_WAT =
      "(module\n"
          + "  (import \"wasi_snapshot_preview1\" \"proc_exit\" (func $proc_exit (param i32)))\n"
          + "  (memory (export \"memory\") 1)\n"
          + "  (func $compute (export \"compute\") (param i32 i32) (result i32)\n"
          + "    local.get 0\n"
          + "    local.get 1\n"
          + "    i32.add)\n"
          + "  (func $_start (export \"_start\")\n"
          + "    nop))\n";

  /**
   * Complex WASI WAT module with additional functions and larger memory, importing proc_exit.
   * Exports: compute, fibonacci, accumulate, _start, memory.
   */
  private static final String COMPLEX_WASI_WAT =
      "(module\n"
          + "  (import \"wasi_snapshot_preview1\" \"proc_exit\" (func $proc_exit (param i32)))\n"
          + "  (memory (export \"memory\") 4 64)\n"
          + "  (func $compute (export \"compute\") (param i32 i32) (result i32)\n"
          + "    local.get 0\n"
          + "    local.get 1\n"
          + "    i32.add)\n"
          + "  (func $fibonacci (export \"fibonacci\") (param i32) (result i32)\n"
          + "    (if (result i32) (i32.lt_s (local.get 0) (i32.const 2))\n"
          + "      (then (local.get 0))\n"
          + "      (else\n"
          + "        (i32.add\n"
          + "          (call $fibonacci (i32.sub (local.get 0) (i32.const 1)))\n"
          + "          (call $fibonacci (i32.sub (local.get 0) (i32.const 2)))))))\n"
          + "  (func $accumulate (export \"accumulate\") (param i32) (result i32)\n"
          + "    (local i32)\n"
          + "    (local.set 1 (i32.const 0))\n"
          + "    (block $break\n"
          + "      (loop $loop\n"
          + "        (br_if $break (i32.le_s (local.get 0) (i32.const 0)))\n"
          + "        (local.set 1 (i32.add (local.get 1) (local.get 0)))\n"
          + "        (local.set 0 (i32.sub (local.get 0) (i32.const 1)))\n"
          + "        (br $loop)))\n"
          + "    (local.get 1))\n"
          + "  (func $_start (export \"_start\")\n"
          + "    nop))\n";

  /** Number of function calls per batch benchmark iteration. */
  private static final int BATCH_CALL_COUNT = 10;

  /** Runtime implementation to benchmark. */
  @Param({"JNI", "PANAMA"})
  private String runtimeTypeName;

  /** Module complexity level for benchmarks. */
  @Param({"SIMPLE", "COMPLEX"})
  private String moduleComplexity;

  /** Pre-created runtime for trial-level reuse. */
  private WasmRuntime trialRuntime;

  /** Pre-created engine for trial-level reuse. */
  private Engine trialEngine;

  /** Pre-created store for trial-level reuse. */
  private Store trialStore;

  /** Pre-compiled WASI module for benchmarks that need it. */
  private Module trialModule;

  /** Pre-created linker with WASI enabled for benchmarks that need it. */
  private Linker<?> trialLinker;

  /** Pre-instantiated instance for function call benchmarks. */
  private Instance trialInstance;

  /** Pre-resolved compute function for function call benchmarks. */
  private WasmFunction computeFunction;

  /**
   * Returns the appropriate WASI WAT source based on the current module complexity parameter.
   *
   * @return the WAT module source string
   */
  private String getWasiWatSource() {
    if ("COMPLEX".equals(moduleComplexity)) {
      return COMPLEX_WASI_WAT;
    }
    return SIMPLE_WASI_WAT;
  }

  /**
   * Trial-level setup that creates reusable runtime components, compiles the WASI module,
   * initializes the WASI linker, instantiates the module, and resolves the compute function.
   *
   * @throws WasmException if any Wasmtime operation fails during setup
   */
  @Setup(Level.Trial)
  public void setupTrial() throws WasmException {
    final RuntimeType runtimeType = RuntimeType.valueOf(runtimeTypeName);
    trialRuntime = createRuntime(runtimeType);
    trialEngine = createEngine(trialRuntime);
    trialStore = createStore(trialEngine);

    trialModule = compileWatModule(trialEngine, getWasiWatSource());

    trialLinker = Linker.create(trialEngine);
    trialLinker.enableWasi();

    trialInstance = trialLinker.instantiate(trialStore, trialModule);

    final Optional<WasmFunction> funcOpt = trialInstance.getFunction("compute");
    if (!funcOpt.isPresent()) {
      throw new WasmException("compute function not found in WASI module");
    }
    computeFunction = funcOpt.get();
  }

  /**
   * Trial-level teardown that closes all resources in reverse creation order.
   *
   * @throws Exception if resource cleanup fails
   */
  @TearDown(Level.Trial)
  public void teardownTrial() throws Exception {
    computeFunction = null;
    closeQuietly(trialInstance);
    trialInstance = null;
    closeQuietly(trialLinker);
    trialLinker = null;
    closeQuietly(trialModule);
    trialModule = null;
    closeQuietly(trialStore);
    trialStore = null;
    closeQuietly(trialEngine);
    trialEngine = null;
    closeQuietly(trialRuntime);
    trialRuntime = null;
  }

  /**
   * Benchmarks WASI linker creation and initialization.
   *
   * <p>Creates a new engine, creates a linker, enables WASI, then closes both. Measures the full
   * cost of WASI initialization.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkWasiLinkerCreation(final Blackhole blackhole) throws WasmException {
    Engine engine = null;
    Linker<?> linker = null;
    try {
      engine = createEngine(trialRuntime);
      linker = Linker.create(engine);
      linker.enableWasi();
      blackhole.consume(linker.isValid());
    } finally {
      closeQuietly(linker);
      closeQuietly(engine);
    }
  }

  /**
   * Benchmarks WASI module compilation.
   *
   * <p>Uses the pre-created trial engine to compile the WASI WAT module, then closes the compiled
   * module. Measures compilation overhead for modules with WASI imports.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkWasiModuleCompilation(final Blackhole blackhole) throws WasmException {
    Module module = null;
    try {
      module = compileWatModule(trialEngine, getWasiWatSource());
      blackhole.consume(module);
    } finally {
      closeQuietly(module);
    }
  }

  /**
   * Benchmarks WASI module instantiation.
   *
   * <p>Uses the pre-created WASI linker and pre-compiled module to instantiate, then closes the
   * instance. Measures the cost of instantiating a module with WASI imports resolved.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkWasiInstantiation(final Blackhole blackhole) throws WasmException {
    // Create a fresh engine/linker/store/module pipeline per call to avoid both the 10,000
    // instance count limit (when reusing a store) and mmap exhaustion (when creating a second
    // store on the same engine). Module must be compiled with the same engine used for the
    // linker/store, since Wasmtime modules are engine-bound.
    Engine engine = null;
    Linker<?> linker = null;
    Store store = null;
    Module module = null;
    Instance instance = null;
    try {
      engine = createEngine(trialRuntime);
      linker = Linker.create(engine);
      linker.enableWasi();
      store = createStore(engine);
      module = compileWatModule(engine, getWasiWatSource());
      instance = linker.instantiate(store, module);
      blackhole.consume(instance);
    } finally {
      closeQuietly(instance);
      closeQuietly(module);
      closeQuietly(store);
      closeQuietly(linker);
      closeQuietly(engine);
    }
  }

  /**
   * Benchmarks function call overhead in a WASI-linked instance.
   *
   * <p>Calls the pre-resolved compute function with i32 parameters and consumes the result.
   * Measures the per-call cost of invoking functions in a WASI-enabled module.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkWasiFunctionCall(final Blackhole blackhole) throws WasmException {
    final WasmValue[] result = computeFunction.call(WasmValue.i32(17), WasmValue.i32(25));
    blackhole.consume(result[0].asInt());
  }

  /**
   * Benchmarks WasiConfig creation overhead.
   *
   * <p>Builds WasiConfig objects using the builder API with various configuration options. Measures
   * the cost of constructing WASI configuration.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkWasiConfigCreation(final Blackhole blackhole) throws WasmException {
    // Default config
    final WasiConfig defaultConfig = WasiConfig.defaultConfig();
    blackhole.consume(defaultConfig);

    // Config with environment variables
    final WasiConfig envConfig =
        WasiConfig.builder()
            .withEnvironment("HOME", "/app")
            .withEnvironment("PATH", "/usr/bin:/bin")
            .withEnvironment("USER", "benchmark")
            .build();
    blackhole.consume(envConfig.getEnvironment().size());

    // Config with arguments
    final WasiConfig argConfig =
        WasiConfig.builder()
            .withArgument("--verbose")
            .withArgument("--config")
            .withArgument("/etc/app.conf")
            .build();
    blackhole.consume(argConfig.getArguments().size());
  }

  /**
   * Benchmarks end-to-end batch WASI operations.
   *
   * <p>Creates a linker with WASI, compiles a module, instantiates it, calls the compute function
   * multiple times, and closes all resources. Measures the full pipeline cost of WASI usage.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkBatchWasiOperations(final Blackhole blackhole) throws WasmException {
    Engine engine = null;
    Linker<?> linker = null;
    Module module = null;
    Store store = null;
    Instance instance = null;
    try {
      engine = createEngine(trialRuntime);
      linker = Linker.create(engine);
      linker.enableWasi();

      store = createStore(engine);
      module = compileWatModule(engine, getWasiWatSource());
      instance = linker.instantiate(store, module);

      final Optional<WasmFunction> funcOpt = instance.getFunction("compute");
      if (!funcOpt.isPresent()) {
        throw new WasmException("compute function not found in batch WASI benchmark");
      }
      final WasmFunction func = funcOpt.get();

      int accumulator = 0;
      for (int i = 0; i < BATCH_CALL_COUNT; i++) {
        final WasmValue[] result = func.call(WasmValue.i32(i), WasmValue.i32(i + 1));
        accumulator += result[0].asInt();
      }
      blackhole.consume(accumulator);
    } finally {
      closeQuietly(instance);
      closeQuietly(module);
      closeQuietly(store);
      closeQuietly(linker);
      closeQuietly(engine);
    }
  }

  /**
   * Benchmarks WASI resource cleanup performance.
   *
   * <p>Creates a full set of WASI resources (engine, linker, module, instance) and immediately
   * closes them. Measures the overhead of resource allocation and deallocation for WASI-linked
   * components.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkWasiResourceCleanup(final Blackhole blackhole) throws WasmException {
    Engine engine = null;
    Linker<?> linker = null;
    Module module = null;
    Store store = null;
    Instance instance = null;
    try {
      engine = createEngine(trialRuntime);
      linker = Linker.create(engine);
      linker.enableWasi();

      store = createStore(engine);
      module = compileWatModule(engine, getWasiWatSource());
      instance = linker.instantiate(store, module);

      blackhole.consume(instance);
    } finally {
      closeQuietly(instance);
      closeQuietly(module);
      closeQuietly(store);
      closeQuietly(linker);
      closeQuietly(engine);
    }
  }
}
