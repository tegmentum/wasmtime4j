package ai.tegmentum.wasmtime4j.benchmarks;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValue;
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
 * Benchmarks for WebAssembly function execution performance.
 *
 * <p>This benchmark class measures the performance characteristics of calling WebAssembly functions
 * from Java, comparing JNI and Panama implementations across different function types and call
 * patterns.
 *
 * <p>Key metrics measured:
 *
 * <ul>
 *   <li>Function call overhead
 *   <li>Parameter marshalling performance
 *   <li>Return value handling
 *   <li>Recursive function call performance
 *   <li>Batch function execution
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
public class FunctionExecutionBenchmark extends BenchmarkBase {

  /** Runtime implementation to benchmark. */
  @Param({"JNI", "PANAMA"})
  private String runtimeTypeName;

  /** Function type to test different call patterns. */
  @Param({"SIMPLE", "COMPLEX", "RECURSIVE"})
  private String functionType;

  /** Number of parameters to pass to the function. */
  @Param({"1", "2", "4"})
  private int parameterCount;
  
  /** WebAssembly runtime components. */
  private WasmRuntime runtime;
  private Engine engine;
  private Store store;
  private Module module;
  private Instance instance;
  
  /** Function to benchmark. */
  private WasmFunction targetFunction;

  /** Test parameters for function calls with current parameter count. */
  private WasmValue[] testParams;
  
  /** Current module bytecode. */
  private byte[] moduleBytes;


  /** Setup performed before each benchmark iteration. */
  @Setup(Level.Iteration)
  public void setupIteration() throws WasmException {
    // Create runtime components
    final RuntimeType runtimeType = RuntimeType.valueOf(runtimeTypeName);
    runtime = createRuntime(runtimeType);
    engine = createEngine(runtime);
    store = createStore(engine);
    
    // Select appropriate module based on function type
    switch (functionType) {
      case "SIMPLE":
        moduleBytes = SIMPLE_WASM_MODULE.clone();
        break;
      case "COMPLEX":
        moduleBytes = COMPLEX_WASM_MODULE.clone();
        break;
      case "RECURSIVE":
        moduleBytes = COMPLEX_WASM_MODULE.clone(); // Uses fibonacci
        break;
      default:
        moduleBytes = SIMPLE_WASM_MODULE.clone();
        break;
    }
    
    // Compile and instantiate module
    module = compileModule(engine, moduleBytes);
    instance = instantiateModule(store, module);
    
    // Get the target function
    final String functionName = getFunctionNameForType(functionType);
    final java.util.Optional<WasmFunction> functionOpt = instance.getFunction(functionName);
    if (!functionOpt.isPresent()) {
      throw new WasmException("Target function not found: " + functionName);
    }
    targetFunction = functionOpt.get();
    
    // Setup test parameters based on parameter count
    testParams = new WasmValue[Math.min(parameterCount, 2)]; // Limit to available params
    for (int i = 0; i < testParams.length; i++) {
      testParams[i] = WasmValue.i32(i + 1); // Use simple sequential values
    }
  }

  /** Cleanup performed after each benchmark iteration. */
  @TearDown(Level.Iteration)
  public void teardownIteration() {
    cleanup();
    testParams = null;
    moduleBytes = null;
  }
  
  /** Helper method to clean up WebAssembly resources. */
  private void cleanup() {
    try {
      // WasmFunction does not implement AutoCloseable, no need to close
      targetFunction = null;
      if (instance != null) {
        instance.close();
        instance = null;
      }
      if (module != null) {
        module.close();
        module = null;
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
   * Benchmarks single function call performance.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   * @return the function result
   */
  @Benchmark
  public WasmValue[] benchmarkSingleFunctionCall(final Blackhole blackhole) {
    try {
      final WasmValue[] result = targetFunction.call(testParams);
      blackhole.consume(result.length);
      return result;
    } catch (final WasmException e) {
      throw new RuntimeException("Function call failed", e);
    }
  }

  /**
   * Benchmarks repeated function calls.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   * @return the last function result
   */
  @Benchmark
  public WasmValue[] benchmarkRepeatedFunctionCalls(final Blackhole blackhole) {
    final int iterations = functionType.equals("RECURSIVE") ? 5 : 10;
    WasmValue[] lastResult = null;

    try {
      for (int i = 0; i < iterations; i++) {
        lastResult = targetFunction.call(testParams);
        blackhole.consume(lastResult.length);
      }
    } catch (final WasmException e) {
      throw new RuntimeException("Repeated function calls failed", e);
    }

    return lastResult;
  }

  /**
   * Benchmarks function calls with different parameter patterns.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkParameterVariations(final Blackhole blackhole) {
    final WasmValue[][] paramVariations = generateParameterVariations();

    try {
      for (final WasmValue[] params : paramVariations) {
        if (params.length <= testParams.length) {
          final WasmValue[] result = targetFunction.call(params);
          blackhole.consume(result.length);
        }
      }
    } catch (final WasmException e) {
      throw new RuntimeException("Parameter variations benchmark failed", e);
    }
  }

  /**
   * Benchmarks function call with result validation.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkFunctionCallWithValidation(final Blackhole blackhole) {
    try {
      final WasmValue[] result = targetFunction.call(testParams);

      // Validate result based on function type
      final boolean isValidResult = validateResult(result);
      blackhole.consume(isValidResult);
      blackhole.consume(result.length);
    } catch (final WasmException e) {
      throw new RuntimeException("Function call with validation failed", e);
    }
  }

  /**
   * Benchmarks batch function execution.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkBatchFunctionExecution(final Blackhole blackhole) {
    final int batchSize = functionType.equals("RECURSIVE") ? 3 : 8;

    try {
      for (int i = 0; i < batchSize; i++) {
        // Modify parameters slightly for each call
        final WasmValue[] batchParams = testParams.clone();
        for (int j = 0; j < batchParams.length; j++) {
          final int currentValue = batchParams[j].asInt();
          batchParams[j] = WasmValue.i32(currentValue + i);
        }

        final WasmValue[] result = targetFunction.call(batchParams);
        blackhole.consume(result.length);
      }
      blackhole.consume(batchSize);
    } catch (final WasmException e) {
      throw new RuntimeException("Batch function execution failed", e);
    }
  }

  /**
   * Benchmarks function call performance under memory pressure.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkCallWithMemoryPressure(final Blackhole blackhole) {
    // Create memory pressure
    final Object[] memoryPressure = new Object[100];
    for (int i = 0; i < memoryPressure.length; i++) {
      memoryPressure[i] = new int[256]; // 1KB per allocation
    }

    try {
      final WasmValue[] result = targetFunction.call(testParams);
      blackhole.consume(result.length);
      blackhole.consume(memoryPressure.length);
    } catch (final WasmException e) {
      throw new RuntimeException("Function call with memory pressure failed", e);
    } finally {
      // Clear memory pressure
      for (int i = 0; i < memoryPressure.length; i++) {
        memoryPressure[i] = null;
      }
    }
  }

  /**
   * Benchmarks error handling during function calls.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkErrorHandling(final Blackhole blackhole) {
    int successfulCalls = 0;
    int errorCalls = 0;

    // Mix of valid and invalid calls
    for (int i = 0; i < 5; i++) {
      try {
        if (i % 3 == 0) {
          // Invalid call with wrong parameter count - create extra parameters
          final WasmValue[] wrongParams = new WasmValue[testParams.length + 1];
          System.arraycopy(testParams, 0, wrongParams, 0, testParams.length);
          wrongParams[testParams.length] = WasmValue.i32(999);
          targetFunction.call(wrongParams);
        } else {
          // Valid call
          targetFunction.call(testParams);
          successfulCalls++;
        }
      } catch (final WasmException e) {
        errorCalls++;
        blackhole.consume(e.getMessage());
      }
    }

    blackhole.consume(successfulCalls);
    blackhole.consume(errorCalls);
  }

  /**
   * Benchmarks function call statistics tracking.
   *
   * @param blackhole JMH blackhole to prevent dead code elimination
   */
  @Benchmark
  public void benchmarkCallStatistics(final Blackhole blackhole) {
    try {
      final long startTime = System.nanoTime();
      final WasmValue[] result = targetFunction.call(testParams);
      final long endTime = System.nanoTime();
      final long executionTime = endTime - startTime;

      blackhole.consume(result.length);
      blackhole.consume(executionTime);
      blackhole.consume(functionType);
    } catch (final WasmException e) {
      throw new RuntimeException("Function call statistics failed", e);
    }
  }

  /**
   * Generates different parameter variations for testing.
   *
   * @return array of parameter combinations
   */
  private WasmValue[][] generateParameterVariations() {
    final int actualParamCount = Math.min(parameterCount, testParams.length);
    final WasmValue[][] variations = new WasmValue[6][];

    // Variation 1: All zeros
    variations[0] = new WasmValue[actualParamCount];
    for (int i = 0; i < actualParamCount; i++) {
      variations[0][i] = WasmValue.i32(0);
    }

    // Variation 2: Sequential numbers
    variations[1] = new WasmValue[actualParamCount];
    for (int i = 0; i < actualParamCount; i++) {
      variations[1][i] = WasmValue.i32(i + 1);
    }

    // Variation 3: All same value
    variations[2] = new WasmValue[actualParamCount];
    for (int i = 0; i < actualParamCount; i++) {
      variations[2][i] = WasmValue.i32(42);
    }

    // Variation 4: Large values
    variations[3] = new WasmValue[actualParamCount];
    for (int i = 0; i < actualParamCount; i++) {
      variations[3][i] = WasmValue.i32(1000 + i);
    }

    // Variation 5: Small positive values
    variations[4] = new WasmValue[actualParamCount];
    for (int i = 0; i < actualParamCount; i++) {
      variations[4][i] = WasmValue.i32(i + 1);
    }

    // Variation 6: Mixed values
    variations[5] = new WasmValue[actualParamCount];
    for (int i = 0; i < actualParamCount; i++) {
      variations[5][i] = WasmValue.i32((i % 2 == 0) ? i : i + 10);
    }

    return variations;
  }

  /**
   * Validates function result based on function type.
   *
   * @param result the result to validate
   * @return true if the result is valid
   */
  private boolean validateResult(final WasmValue[] result) {
    if (result == null || result.length == 0) {
      return false;
    }
    
    final int value = result[0].asInt();
    
    switch (functionType) {
      case "SIMPLE":
        // For simple addition, result should be sum of parameters
        int expectedSum = 0;
        for (final WasmValue param : testParams) {
          expectedSum += param.asInt();
        }
        return value >= expectedSum; // Account for any overhead

      case "COMPLEX":
        // Complex function should return non-zero for positive inputs
        return value != 0;

      case "RECURSIVE":
        // Fibonacci results should be non-negative
        return value >= 0;

      default:
        return true;
    }
  }
  
  /**
   * Gets the function name for the specified function type.
   *
   * @param type the function type
   * @return the WebAssembly function name
   */
  private String getFunctionNameForType(final String type) {
    switch (type) {
      case "SIMPLE":
        return "add";
      case "COMPLEX":
      case "RECURSIVE":
        return "fibonacci";
      default:
        return "add";
    }
  }
}
