package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Benchmarks for {@link Linker} performance testing.
 *
 * <p>These benchmarks measure the performance of various linker operations including:
 *
 * <ul>
 *   <li>Host function definition
 *   <li>Module instantiation with import resolution
 *   <li>Host function invocation overhead
 *   <li>Linker creation and cleanup
 * </ul>
 *
 * <p>The benchmarks compare JNI and Panama implementations to ensure performance parity.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(
    value = 1,
    jvmArgs = {"-XX:+UnlockExperimentalVMOptions", "--enable-preview"})
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
public class LinkerBenchmark {

  private WasmRuntime runtime;
  private Engine engine;
  private Store store;
  private Linker linker;
  private Module moduleWithImports;
  private Module moduleNoImports;
  private Instance instanceWithHostFunction;
  private WasmFunction hostBoundFunction;

  // Simple WebAssembly module that imports a function called "add" from module "env"
  // and exports a function called "call_add" that calls the imported function
  private static final String WAT_MODULE_WITH_IMPORTS =
      "(module\n"
          + "  (import \"env\" \"add\" (func $add (param i32 i32) (result i32)))\n"
          + "  (func (export \"call_add\") (param i32 i32) (result i32)\n"
          + "    local.get 0\n"
          + "    local.get 1\n"
          + "    call $add\n"
          + "  )\n"
          + ")";

  // Simple WebAssembly module with no imports
  private static final String WAT_MODULE_NO_IMPORTS =
      "(module\n"
          + "  (func (export \"get_answer\") (result i32)\n"
          + "    i32.const 42\n"
          + "  )\n"
          + ")";

  @Setup(Level.Trial)
  public void setup() throws WasmException {
    runtime = WasmRuntimeFactory.create();
    engine = runtime.createEngine();
    store = runtime.createStore(engine);
    linker = runtime.createLinker(engine);

    // Compile modules
    moduleWithImports = runtime.compileModuleWat(engine, WAT_MODULE_WITH_IMPORTS);
    moduleNoImports = runtime.compileModuleWat(engine, WAT_MODULE_NO_IMPORTS);

    // Define a host function for the import-dependent module
    final FunctionType addType =
        new FunctionType(
            new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
            new WasmValueType[] {WasmValueType.I32});

    final HostFunction addFunction =
        (params) -> {
          final int a = params[0].asInt();
          final int b = params[1].asInt();
          return new WasmValue[] {WasmValue.i32(a + b)};
        };

    linker.defineHostFunction("env", "add", addType, addFunction);

    // Create instance for host function calls
    instanceWithHostFunction = linker.instantiate(store, moduleWithImports);
    hostBoundFunction = instanceWithHostFunction.getFunction("call_add").orElseThrow();
  }

  @TearDown(Level.Trial)
  public void tearDown() {
    if (instanceWithHostFunction != null) {
      instanceWithHostFunction.close();
    }
    if (linker != null) {
      linker.close();
    }
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

  @Benchmark
  public Linker benchmarkLinkerCreation() throws WasmException {
    final Linker newLinker = runtime.createLinker(engine);
    newLinker.close();
    return newLinker;
  }

  @Benchmark
  public void benchmarkDefineHostFunction() throws WasmException {
    final Linker tempLinker = runtime.createLinker(engine);
    try {
      final FunctionType mathType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I32});

      final HostFunction multiplyFunction =
          (params) -> {
            final int a = params[0].asInt();
            final int b = params[1].asInt();
            return new WasmValue[] {WasmValue.i32(a * b)};
          };

      tempLinker.defineHostFunction("env", "multiply", mathType, multiplyFunction);
    } finally {
      tempLinker.close();
    }
  }

  @Benchmark
  public Instance benchmarkInstantiateWithoutImports() throws WasmException {
    final Linker tempLinker = runtime.createLinker(engine);
    try {
      return tempLinker.instantiate(store, moduleNoImports);
    } finally {
      tempLinker.close();
    }
  }

  @Benchmark
  public Instance benchmarkInstantiateWithImports() throws WasmException {
    final Linker tempLinker = runtime.createLinker(engine);
    try {
      // Define required host function
      final FunctionType addType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I32});

      final HostFunction addFunction =
          (params) -> {
            final int a = params[0].asInt();
            final int b = params[1].asInt();
            return new WasmValue[] {WasmValue.i32(a + b)};
          };

      tempLinker.defineHostFunction("env", "add", addType, addFunction);

      return tempLinker.instantiate(store, moduleWithImports);
    } finally {
      tempLinker.close();
    }
  }

  @Benchmark
  public WasmValue[] benchmarkHostFunctionCall() throws WasmException {
    return hostBoundFunction.call(WasmValue.i32(10), WasmValue.i32(32));
  }

  @Benchmark
  public void benchmarkDefineMultipleHostFunctions() throws WasmException {
    final Linker tempLinker = runtime.createLinker(engine);
    try {
      final FunctionType mathType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I32});

      // Define multiple functions
      tempLinker.defineHostFunction(
          "env",
          "add",
          mathType,
          (params) -> {
            final int a = params[0].asInt();
            final int b = params[1].asInt();
            return new WasmValue[] {WasmValue.i32(a + b)};
          });

      tempLinker.defineHostFunction(
          "env",
          "sub",
          mathType,
          (params) -> {
            final int a = params[0].asInt();
            final int b = params[1].asInt();
            return new WasmValue[] {WasmValue.i32(a - b)};
          });

      tempLinker.defineHostFunction(
          "env",
          "mul",
          mathType,
          (params) -> {
            final int a = params[0].asInt();
            final int b = params[1].asInt();
            return new WasmValue[] {WasmValue.i32(a * b)};
          });

      tempLinker.defineHostFunction(
          "env",
          "div",
          mathType,
          (params) -> {
            final int a = params[0].asInt();
            final int b = params[1].asInt();
            return new WasmValue[] {WasmValue.i32(a / b)};
          });

      tempLinker.defineHostFunction(
          "env",
          "mod",
          mathType,
          (params) -> {
            final int a = params[0].asInt();
            final int b = params[1].asInt();
            return new WasmValue[] {WasmValue.i32(a % b)};
          });
    } finally {
      tempLinker.close();
    }
  }

  @Benchmark
  public void benchmarkCreateAlias() throws WasmException {
    final Linker tempLinker = runtime.createLinker(engine);
    try {
      // First define a function
      final FunctionType mathType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
              new WasmValueType[] {WasmValueType.I32});

      tempLinker.defineHostFunction(
          "env",
          "add",
          mathType,
          (params) -> {
            final int a = params[0].asInt();
            final int b = params[1].asInt();
            return new WasmValue[] {WasmValue.i32(a + b)};
          });

      // Create alias
      tempLinker.alias("env", "add", "math", "plus");
    } finally {
      tempLinker.close();
    }
  }

  @Benchmark
  public void benchmarkLinkerCleanup() throws WasmException {
    final Linker tempLinker = runtime.createLinker(engine);

    // Define some functions to make cleanup more representative
    final FunctionType mathType =
        new FunctionType(
            new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
            new WasmValueType[] {WasmValueType.I32});

    tempLinker.defineHostFunction(
        "env", "test1", mathType, (params) -> new WasmValue[] {WasmValue.i32(1)});
    tempLinker.defineHostFunction(
        "env", "test2", mathType, (params) -> new WasmValue[] {WasmValue.i32(2)});
    tempLinker.defineHostFunction(
        "env", "test3", mathType, (params) -> new WasmValue[] {WasmValue.i32(3)});

    // Measure cleanup time
    tempLinker.close();
  }

  @Benchmark
  public Instance benchmarkInstantiateWithModuleName() throws WasmException {
    final Linker tempLinker = runtime.createLinker(engine);
    try {
      return tempLinker.instantiate(store, "test_module", moduleNoImports);
    } finally {
      tempLinker.close();
    }
  }
}
