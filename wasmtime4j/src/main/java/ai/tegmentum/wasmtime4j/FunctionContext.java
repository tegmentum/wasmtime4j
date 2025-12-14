package ai.tegmentum.wasmtime4j;

/**
 * Represents the execution context of a WebAssembly function.
 *
 * <p>The function context provides information about a function's execution environment, including
 * stack frame details, parameter and return types, and optimization capabilities.
 *
 * @since 1.1.0
 */
public interface FunctionContext {

  /**
   * Gets the function name, if available.
   *
   * @return the function name, or null if not available
   */
  String getFunctionName();

  /**
   * Gets the function index within the module.
   *
   * @return the function index
   */
  int getFunctionIndex();

  /**
   * Gets the function type signature.
   *
   * @return the function type
   */
  FunctionType getFunctionType();

  /**
   * Gets the parameter types for this function.
   *
   * @return array of parameter types
   */
  default WasmValueType[] getParameterTypes() {
    return getFunctionType().getParamTypes();
  }

  /**
   * Gets the return types for this function.
   *
   * @return array of return types
   */
  default WasmValueType[] getReturnTypes() {
    return getFunctionType().getReturnTypes();
  }

  /**
   * Checks if this function supports tail calls.
   *
   * @return true if tail calls are supported
   */
  boolean supportsTailCalls();

  /**
   * Gets the estimated stack frame size for this function.
   *
   * @return stack frame size in bytes
   */
  int getStackFrameSize();

  /**
   * Gets the local variable count for this function.
   *
   * @return number of local variables
   */
  int getLocalCount();

  /**
   * Gets the types of local variables.
   *
   * @return array of local variable types
   */
  WasmValueType[] getLocalTypes();

  /**
   * Checks if this function is recursive (calls itself).
   *
   * @return true if the function is recursive
   */
  boolean isRecursive();

  /**
   * Checks if this function is tail-recursive.
   *
   * @return true if the function only makes tail calls to itself
   */
  boolean isTailRecursive();

  /**
   * Gets the recursion depth limit for this function.
   *
   * @return maximum recursion depth, or -1 if unlimited
   */
  int getRecursionDepthLimit();

  /**
   * Gets the current call depth for this function.
   *
   * @return current call depth
   */
  int getCurrentCallDepth();

  /**
   * Gets optimization information for this function.
   *
   * @return optimization information
   */
  FunctionOptimization getOptimization();

  /**
   * Gets performance metrics for this function.
   *
   * @return performance metrics
   */
  FunctionMetrics getMetrics();

  /** Function optimization information. */
  interface FunctionOptimization {
    /** Whether this function has been optimized by the JIT compiler. */
    boolean isJitCompiled();

    /** Whether this function is inlined at call sites. */
    boolean isInlined();

    /** Whether tail call optimization is enabled for this function. */
    boolean hasTailCallOptimization();

    /** Whether this function uses frame pointer elimination. */
    boolean hasFramePointerElimination();

    /** The optimization level applied to this function. */
    WasmExecutionContext.OptimizationLevel getOptimizationLevel();

    /** Estimated compilation cost. */
    int getCompilationCost();

    /** Estimated execution improvement from optimization. */
    double getPerformanceGain();
  }

  /** Function performance metrics. */
  interface FunctionMetrics {
    /** Total number of times this function has been called. */
    long getCallCount();

    /** Total execution time across all calls (nanoseconds). */
    long getTotalExecutionTime();

    /** Average execution time per call (nanoseconds). */
    double getAverageExecutionTime();

    /** Peak memory usage during execution (bytes). */
    long getPeakMemoryUsage();

    /** Number of tail calls made from this function. */
    long getTailCallCount();

    /** Number of successful tail call optimizations. */
    long getOptimizedTailCallCount();

    /** Stack space saved through tail call optimization (bytes). */
    long getStackSpaceSaved();
  }

  /**
   * Creates a function context.
   *
   * @param functionName the function name
   * @param functionIndex the function index
   * @param functionType the function type
   * @return a new function context
   */
  static FunctionContext create(
      final String functionName, final int functionIndex, final FunctionType functionType) {
    return create(functionName, functionIndex, functionType, true, 64);
  }

  /**
   * Creates a function context with specific settings.
   *
   * @param functionName the function name
   * @param functionIndex the function index
   * @param functionType the function type
   * @param supportsTailCalls whether tail calls are supported
   * @param stackFrameSize the estimated stack frame size
   * @return a new function context
   */
  static FunctionContext create(
      final String functionName,
      final int functionIndex,
      final FunctionType functionType,
      final boolean supportsTailCalls,
      final int stackFrameSize) {

    return new FunctionContext() {
      private final FunctionMetricsImpl metrics = new FunctionMetricsImpl();
      private final FunctionOptimizationImpl optimization = new FunctionOptimizationImpl();
      private int currentCallDepth = 0;

      @Override
      public String getFunctionName() {
        return functionName;
      }

      @Override
      public int getFunctionIndex() {
        return functionIndex;
      }

      @Override
      public FunctionType getFunctionType() {
        return functionType;
      }

      @Override
      public boolean supportsTailCalls() {
        return supportsTailCalls;
      }

      @Override
      public int getStackFrameSize() {
        return stackFrameSize;
      }

      @Override
      public int getLocalCount() {
        // Simplified: assume parameters + 2 locals
        return functionType.getParamTypes().length + 2;
      }

      @Override
      public WasmValueType[] getLocalTypes() {
        // Simplified: return parameter types + two i32 locals
        final WasmValueType[] paramTypes = functionType.getParamTypes();
        final WasmValueType[] localTypes = new WasmValueType[paramTypes.length + 2];
        System.arraycopy(paramTypes, 0, localTypes, 0, paramTypes.length);
        localTypes[paramTypes.length] = WasmValueType.I32;
        localTypes[paramTypes.length + 1] = WasmValueType.I32;
        return localTypes;
      }

      @Override
      public boolean isRecursive() {
        // Simplified: determine by name pattern or analysis
        return functionName != null && functionName.contains("recursive");
      }

      @Override
      public boolean isTailRecursive() {
        // Simplified: assume tail recursive if recursive and optimized
        return isRecursive() && optimization.hasTailCallOptimization();
      }

      @Override
      public int getRecursionDepthLimit() {
        return 1000; // Default limit
      }

      @Override
      public int getCurrentCallDepth() {
        return currentCallDepth;
      }

      @Override
      public FunctionOptimization getOptimization() {
        return optimization;
      }

      @Override
      public FunctionMetrics getMetrics() {
        return metrics;
      }
    };
  }

  /** Default implementation of FunctionOptimization. */
  class FunctionOptimizationImpl implements FunctionOptimization {
    @Override
    public boolean isJitCompiled() {
      return false;
    }

    @Override
    public boolean isInlined() {
      return false;
    }

    @Override
    public boolean hasTailCallOptimization() {
      return true;
    }

    @Override
    public boolean hasFramePointerElimination() {
      return false;
    }

    @Override
    public WasmExecutionContext.OptimizationLevel getOptimizationLevel() {
      return WasmExecutionContext.OptimizationLevel.BASIC;
    }

    @Override
    public int getCompilationCost() {
      return 100;
    }

    @Override
    public double getPerformanceGain() {
      return 1.2;
    }
  }

  /** Default implementation of FunctionMetrics. */
  class FunctionMetricsImpl implements FunctionMetrics {
    private long callCount = 0;
    private long totalExecutionTime = 0;
    private long peakMemoryUsage = 0;
    private long tailCallCount = 0;
    private long optimizedTailCallCount = 0;
    private long stackSpaceSaved = 0;

    @Override
    public long getCallCount() {
      return callCount;
    }

    @Override
    public long getTotalExecutionTime() {
      return totalExecutionTime;
    }

    @Override
    public double getAverageExecutionTime() {
      return callCount > 0 ? (double) totalExecutionTime / callCount : 0.0;
    }

    @Override
    public long getPeakMemoryUsage() {
      return peakMemoryUsage;
    }

    @Override
    public long getTailCallCount() {
      return tailCallCount;
    }

    @Override
    public long getOptimizedTailCallCount() {
      return optimizedTailCallCount;
    }

    @Override
    public long getStackSpaceSaved() {
      return stackSpaceSaved;
    }

    void recordCall(final long executionTime) {
      callCount++;
      totalExecutionTime += executionTime;
    }

    void recordTailCall(final boolean optimized, final long stackSaved) {
      tailCallCount++;
      if (optimized) {
        optimizedTailCallCount++;
        stackSpaceSaved += stackSaved;
      }
    }
  }
}
