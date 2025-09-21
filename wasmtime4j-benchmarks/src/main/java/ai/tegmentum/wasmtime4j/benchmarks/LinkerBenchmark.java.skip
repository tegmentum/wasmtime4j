package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.HostFunction;
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
  private static final byte[] WASM_MODULE_WITH_IMPORTS = {
    0x00,
    0x61,
    0x73,
    0x6d, // WASM magic number
    0x01,
    0x00,
    0x00,
    0x00, // Version 1

    // Type section
    0x01,
    0x07, // Section id=1, length=7
    0x01, // 1 type
    0x60,
    0x02,
    0x7f,
    0x7f,
    0x01,
    0x7f, // (i32, i32) -> i32

    // Import section
    0x02,
    0x0b, // Section id=2, length=11
    0x01, // 1 import
    0x03,
    0x65,
    0x6e,
    0x76, // Module name "env"
    0x03,
    0x61,
    0x64,
    0x64, // Field name "add"
    0x00,
    0x00, // Import type: function index 0

    // Function section
    0x03,
    0x02, // Section id=3, length=2
    0x01,
    0x00, // 1 function, type index 0

    // Export section
    0x07,
    0x0c, // Section id=7, length=12
    0x01, // 1 export
    0x08,
    0x63,
    0x61,
    0x6c,
    0x6c,
    0x5f,
    0x61,
    0x64,
    0x64, // Name "call_add"
    0x00,
    0x01, // Export type: function index 1

    // Code section
    0x0a,
    0x09, // Section id=10, length=9
    0x01, // 1 function body
    0x07, // Body length=7
    0x00, // Local declarations count=0
    0x20,
    0x00, // local.get 0
    0x20,
    0x01, // local.get 1
    0x10,
    0x00, // call 0
    0x0b // end
  };

  // Simple WebAssembly module with no imports
  private static final byte[] WASM_MODULE_NO_IMPORTS = {
    0x00,
    0x61,
    0x73,
    0x6d, // WASM magic number
    0x01,
    0x00,
    0x00,
    0x00, // Version 1

    // Type section
    0x01,
    0x05, // Section id=1, length=5
    0x01, // 1 type
    0x60,
    0x00,
    0x01,
    0x7f, // () -> i32

    // Function section
    0x03,
    0x02, // Section id=3, length=2
    0x01,
    0x00, // 1 function, type index 0

    // Export section
    0x07,
    0x0e, // Section id=7, length=14
    0x01, // 1 export
    0x0a,
    0x67,
    0x65,
    0x74,
    0x5f,
    0x61,
    0x6e,
    0x73,
    0x77,
    0x65,
    0x72, // Name "get_answer"
    0x00,
    0x00, // Export type: function index 0

    // Code section
    0x0a,
    0x06, // Section id=10, length=6
    0x01, // 1 function body
    0x04, // Body length=4
    0x00, // Local declarations count=0
    0x41,
    0x2a, // i32.const 42
    0x0b // end
  };

  @Setup(Level.Trial)
  public void setup() throws WasmException {
    runtime = WasmRuntimeFactory.create();
    engine = runtime.createEngine();
    store = runtime.createStore(engine);
    linker = runtime.createLinker(engine);

    // Compile modules
    moduleWithImports = runtime.compileModule(engine, WASM_MODULE_WITH_IMPORTS);
    moduleNoImports = runtime.compileModule(engine, WASM_MODULE_NO_IMPORTS);

    // Define a host function for the import-dependent module
    final FunctionType addType =
        new FunctionType(
            new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
            new WasmValueType[] {WasmValueType.I32});

    final HostFunction addFunction =
        (params) -> {
          final int a = params[0].asI32();
          final int b = params[1].asI32();
          return new WasmValue[] {WasmValue.ofI32(a + b)};
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
            final int a = params[0].asI32();
            final int b = params[1].asI32();
            return new WasmValue[] {WasmValue.ofI32(a * b)};
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
            final int a = params[0].asI32();
            final int b = params[1].asI32();
            return new WasmValue[] {WasmValue.ofI32(a + b)};
          };

      tempLinker.defineHostFunction("env", "add", addType, addFunction);

      return tempLinker.instantiate(store, moduleWithImports);
    } finally {
      tempLinker.close();
    }
  }

  @Benchmark
  public WasmValue[] benchmarkHostFunctionCall() throws WasmException {
    return hostBoundFunction.call(WasmValue.ofI32(10), WasmValue.ofI32(32));
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
            final int a = params[0].asI32();
            final int b = params[1].asI32();
            return new WasmValue[] {WasmValue.ofI32(a + b)};
          });

      tempLinker.defineHostFunction(
          "env",
          "sub",
          mathType,
          (params) -> {
            final int a = params[0].asI32();
            final int b = params[1].asI32();
            return new WasmValue[] {WasmValue.ofI32(a - b)};
          });

      tempLinker.defineHostFunction(
          "env",
          "mul",
          mathType,
          (params) -> {
            final int a = params[0].asI32();
            final int b = params[1].asI32();
            return new WasmValue[] {WasmValue.ofI32(a * b)};
          });

      tempLinker.defineHostFunction(
          "env",
          "div",
          mathType,
          (params) -> {
            final int a = params[0].asI32();
            final int b = params[1].asI32();
            return new WasmValue[] {WasmValue.ofI32(a / b)};
          });

      tempLinker.defineHostFunction(
          "env",
          "mod",
          mathType,
          (params) -> {
            final int a = params[0].asI32();
            final int b = params[1].asI32();
            return new WasmValue[] {WasmValue.ofI32(a % b)};
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
            final int a = params[0].asI32();
            final int b = params[1].asI32();
            return new WasmValue[] {WasmValue.ofI32(a + b)};
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
        "env", "test1", mathType, (params) -> new WasmValue[] {WasmValue.ofI32(1)});
    tempLinker.defineHostFunction(
        "env", "test2", mathType, (params) -> new WasmValue[] {WasmValue.ofI32(2)});
    tempLinker.defineHostFunction(
        "env", "test3", mathType, (params) -> new WasmValue[] {WasmValue.ofI32(3)});

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
