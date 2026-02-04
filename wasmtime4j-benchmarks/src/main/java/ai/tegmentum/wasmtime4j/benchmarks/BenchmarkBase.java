package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Base class for all Wasmtime4j benchmarks providing common configuration and utilities.
 *
 * <p>This class establishes standard benchmark parameters and provides utility methods for
 * consistent benchmark execution across different implementation types.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(
    value = 2,
    jvmArgs = {"-Xms2g", "-Xmx2g"})
public abstract class BenchmarkBase {

  /** Logger for benchmark classes. */
  private static final Logger LOGGER = Logger.getLogger(BenchmarkBase.class.getName());

  /** Runtime instance for benchmark execution. */
  protected WasmRuntime runtime;

  /** Simple WebAssembly module that adds two i32 values. Exports: add(i32, i32) -> i32. */
  protected static final String SIMPLE_WAT_MODULE =
      "(module\n"
          + "  (func $add (export \"add\") (param i32 i32) (result i32)\n"
          + "    local.get 0\n"
          + "    local.get 1\n"
          + "    i32.add))\n";

  /**
   * Complex WebAssembly module with memory for performance testing. Exports: fibonacci(i32) -> i32,
   * memory.
   */
  protected static final String COMPLEX_WAT_MODULE =
      "(module\n"
          + "  (memory (export \"memory\") 1 16)\n"
          + "  (func $fibonacci (export \"fibonacci\") (param i32) (result i32)\n"
          + "    (if (result i32) (i32.lt_s (local.get 0) (i32.const 2))\n"
          + "      (then (local.get 0))\n"
          + "      (else\n"
          + "        (i32.add\n"
          + "          (call $fibonacci (i32.sub (local.get 0) (i32.const 1)))\n"
          + "          (call $fibonacci (i32.sub (local.get 0) (i32.const 2)))))))\n"
          + "  (func $sum (export \"sum\") (param i32 i32) (result i32)\n"
          + "    i32.const 0))\n";

  /**
   * Large WebAssembly module with multiple functions and memory for benchmarking module compilation
   * overhead. Exports: add(i32, i32) -> i32, sub(i32, i32) -> i32, mul(i32, i32) -> i32,
   * div_safe(i32, i32) -> i32, fibonacci(i32) -> i32, accumulate(i32) -> i32, memory.
   */
  protected static final String LARGE_WAT_MODULE =
      "(module\n"
          + "  (memory (export \"memory\") 4 64)\n"
          + "  (func $add (export \"add\") (param i32 i32) (result i32)\n"
          + "    local.get 0\n"
          + "    local.get 1\n"
          + "    i32.add)\n"
          + "  (func $sub (export \"sub\") (param i32 i32) (result i32)\n"
          + "    local.get 0\n"
          + "    local.get 1\n"
          + "    i32.sub)\n"
          + "  (func $mul (export \"mul\") (param i32 i32) (result i32)\n"
          + "    local.get 0\n"
          + "    local.get 1\n"
          + "    i32.mul)\n"
          + "  (func $div_safe (export \"div_safe\") (param i32 i32) (result i32)\n"
          + "    (if (result i32) (i32.eqz (local.get 1))\n"
          + "      (then (i32.const 0))\n"
          + "      (else (i32.div_s (local.get 0) (local.get 1)))))\n"
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
          + "        (br $loop)))    (local.get 1)))\n";

  /**
   * Gets the current Java version for runtime selection logic.
   *
   * @return the major Java version number
   */
  protected static int getJavaVersion() {
    String version = System.getProperty("java.version");
    if (version.startsWith("1.")) {
      return Integer.parseInt(version.substring(2, 3));
    } else {
      // Handle suffixes like "-ea", "-internal", etc.
      final int dash = version.indexOf("-");
      if (dash != -1) {
        version = version.substring(0, dash);
      }
      final int dot = version.indexOf(".");
      if (dot != -1) {
        return Integer.parseInt(version.substring(0, dot));
      } else {
        return Integer.parseInt(version);
      }
    }
  }

  /**
   * Determines the appropriate runtime type based on system capabilities.
   *
   * @return the recommended runtime type for the current environment
   */
  protected static RuntimeType getRecommendedRuntime() {
    final int javaVersion = getJavaVersion();
    if (javaVersion >= 23) {
      // Check if Panama is available
      try {
        Class.forName("java.lang.foreign.MemorySegment");
        return RuntimeType.PANAMA;
      } catch (final ClassNotFoundException e) {
        return RuntimeType.JNI;
      }
    } else {
      return RuntimeType.JNI;
    }
  }

  /**
   * Creates a WebAssembly runtime instance for the specified type.
   *
   * @param runtimeType the type of runtime to create, or null for auto-selection
   * @return the WebAssembly runtime instance
   * @throws WasmException if runtime creation fails
   */
  protected static WasmRuntime createRuntime(final RuntimeType runtimeType) throws WasmException {
    if (runtimeType == null) {
      return WasmRuntimeFactory.create();
    }
    return WasmRuntimeFactory.create(runtimeType);
  }

  /**
   * Creates a WebAssembly engine for the specified runtime.
   *
   * @param runtime the WebAssembly runtime
   * @return the WebAssembly engine
   * @throws WasmException if engine creation fails
   */
  protected static Engine createEngine(final WasmRuntime runtime) throws WasmException {
    return runtime.createEngine();
  }

  /**
   * Creates a WebAssembly store for the specified engine.
   *
   * @param engine the WebAssembly engine
   * @return the WebAssembly store
   * @throws WasmException if store creation fails
   */
  protected static Store createStore(final Engine engine) throws WasmException {
    return engine.createStore();
  }

  /**
   * Compiles a WebAssembly module from bytecode.
   *
   * @param engine the WebAssembly engine
   * @param wasmBytes the WebAssembly module bytecode
   * @return the compiled WebAssembly module
   * @throws WasmException if module compilation fails
   */
  protected static Module compileModule(final Engine engine, final byte[] wasmBytes)
      throws WasmException {
    validateWasmModule(wasmBytes);
    return engine.compileModule(wasmBytes);
  }

  /**
   * Compiles a WebAssembly module from WAT (WebAssembly Text) format.
   *
   * @param engine the WebAssembly engine
   * @param wat the WebAssembly module in text format
   * @return the compiled WebAssembly module
   * @throws WasmException if module compilation fails
   */
  protected static Module compileWatModule(final Engine engine, final String wat)
      throws WasmException {
    return engine.compileWat(wat);
  }

  /**
   * Instantiates a WebAssembly module.
   *
   * @param store the WebAssembly store
   * @param module the WebAssembly module
   * @return the WebAssembly instance
   * @throws WasmException if module instantiation fails
   */
  protected static Instance instantiateModule(final Store store, final Module module)
      throws WasmException {
    return module.instantiate(store);
  }

  /**
   * Validates that a WebAssembly module byte array is not null and has minimum expected size.
   *
   * @param wasmModule the WebAssembly module bytes to validate
   * @throws IllegalArgumentException if the module is invalid
   */
  protected static void validateWasmModule(final byte[] wasmModule) {
    if (wasmModule == null) {
      throw new IllegalArgumentException("WASM module cannot be null");
    }
    if (wasmModule.length < 8) {
      throw new IllegalArgumentException("WASM module too small");
    }
    // Check WASM magic number
    if (wasmModule[0] != 0x00
        || wasmModule[1] != 0x61
        || wasmModule[2] != 0x73
        || wasmModule[3] != 0x6d) {
      throw new IllegalArgumentException("Invalid WASM magic number");
    }
  }

  /**
   * Creates a formatted benchmark identifier for result tracking.
   *
   * @param operation the operation being benchmarked
   * @param runtime the runtime implementation being tested
   * @return formatted benchmark identifier
   */
  protected static String formatBenchmarkId(final String operation, final RuntimeType runtime) {
    return String.format(
        "%s_%s_%d", operation, runtime.name().toLowerCase(), System.currentTimeMillis() % 10000);
  }

  /**
   * Performs a simple blackhole operation to prevent dead code elimination. This ensures the JIT
   * compiler doesn't optimize away our benchmark code.
   *
   * @param value the value to consume
   * @return the same value (prevents optimization)
   */
  protected static int preventOptimization(final int value) {
    return value;
  }

  /**
   * Performs a simple blackhole operation for byte arrays.
   *
   * @param value the byte array to consume
   * @return the length of the array (prevents optimization)
   */
  protected static int preventOptimization(final byte[] value) {
    return value != null ? value.length : 0;
  }

  /**
   * Sets up the WebAssembly runtime based on the runtime type name.
   *
   * @param runtimeTypeName the name of the runtime type (JNI or PANAMA)
   * @throws WasmException if runtime creation fails
   */
  protected void setupRuntime(final String runtimeTypeName) throws WasmException {
    final RuntimeType type = RuntimeType.valueOf(runtimeTypeName);
    this.runtime = WasmRuntimeFactory.create(type);
    logInfo("Initialized runtime: " + runtimeTypeName);
  }

  /**
   * Tears down the WebAssembly runtime, releasing resources.
   *
   * @throws Exception if cleanup fails
   */
  protected void tearDownRuntime() throws Exception {
    if (runtime != null) {
      runtime.close();
      runtime = null;
      logInfo("Runtime teardown completed");
    }
  }

  /**
   * Logs an informational message.
   *
   * @param message the message to log
   */
  protected void logInfo(final String message) {
    LOGGER.info(message);
  }

  /**
   * Logs a warning message.
   *
   * @param message the message to log
   */
  protected void logWarn(final String message) {
    LOGGER.warning(message);
  }

  /**
   * Logs an error message.
   *
   * @param message the message to log
   */
  protected void logError(final String message) {
    LOGGER.severe(message);
  }

  /**
   * Logs an error message with an exception.
   *
   * @param message the message to log
   * @param throwable the exception that caused the error
   */
  protected void logError(final String message, final Throwable throwable) {
    LOGGER.log(Level.SEVERE, message, throwable);
  }

  /**
   * Safely closes an AutoCloseable resource without throwing exceptions.
   *
   * @param closeable the resource to close
   */
  protected static void closeQuietly(final AutoCloseable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (final Exception e) {
        LOGGER.log(Level.FINE, "Error closing resource", e);
      }
    }
  }
}
