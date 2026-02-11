package ai.tegmentum.wasmtime4j.func;

import ai.tegmentum.wasmtime4j.WasmFunction;

import ai.tegmentum.wasmtime4j.Export;

import ai.tegmentum.wasmtime4j.exception.WasmException;

import ai.tegmentum.wasmtime4j.Store;

import ai.tegmentum.wasmtime4j.WasmValueType;

import ai.tegmentum.wasmtime4j.WasmValue;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Represents a host function that can be called from WebAssembly.
 *
 * <p>Host functions allow Java code to provide implementations that can be imported and called by
 * WebAssembly modules. They enable bidirectional communication between Java and WebAssembly
 * runtimes. Supports the WebAssembly multi-value proposal for functions that return multiple
 * values.
 *
 * <p>Host functions must have a well-defined function type signature that matches the WebAssembly
 * import declaration. The runtime validates parameter and return types during module instantiation.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create a simple host function that adds two numbers
 * HostFunction addFunction = new HostFunction() {
 *     @Override
 *     public WasmValue[] execute(WasmValue[] params) throws WasmException {
 *         int a = params[0].asI32();
 *         int b = params[1].asI32();
 *         return new WasmValue[] { WasmValue.i32(a + b) };
 *     }
 * };
 *
 * // Create a multi-value host function that returns sum and product
 * HostFunction mathFunction = HostFunction.multiValue((params) -> {
 *     int a = params[0].asI32();
 *     int b = params[1].asI32();
 *     return WasmValue.multiValue(
 *         WasmValue.i32(a + b),    // sum
 *         WasmValue.i32(a * b)     // product
 *     );
 * });
 *
 * // Register with import map
 * ImportMap imports = ImportMap.empty();
 * Store store = engine.createStore();
 * WasmFunction func = store.createHostFunction("add", functionType, addFunction);
 * imports.addFunction("env", "add", func);
 * }</pre>
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface HostFunction {

  /**
   * Executes the host function with the given parameters.
   *
   * <p>This method is called when WebAssembly code invokes the host function. Parameters are
   * automatically converted from WebAssembly values to WasmValue objects. The implementation should
   * validate parameter types and counts as needed.
   *
   * <p>Exceptions thrown by this method are caught by the runtime and converted to WebAssembly
   * traps, causing the calling WebAssembly code to abort execution.
   *
   * @param params the parameters passed from WebAssembly, never null
   * @return the results to return to WebAssembly (may be multiple values), must match the function
   *     signature
   * @throws WasmException if function execution fails
   */
  WasmValue[] execute(final WasmValue[] params) throws WasmException;

  /**
   * Creates a host function that returns no values (void function).
   *
   * @param impl the implementation that processes parameters without returning values
   * @return a new HostFunction
   */
  static HostFunction voidFunction(final VoidHostFunction impl) {
    return (params) -> {
      impl.execute(params);
      return new WasmValue[0];
    };
  }

  /**
   * Creates a host function that returns a single value.
   *
   * @param impl the implementation that returns a single value
   * @return a new HostFunction
   */
  static HostFunction singleValue(final SingleValueHostFunction impl) {
    return (params) -> {
      final WasmValue result = impl.execute(params);
      return result != null ? new WasmValue[] {result} : new WasmValue[0];
    };
  }

  /**
   * Creates a host function that returns multiple values.
   *
   * @param impl the implementation that returns multiple values
   * @return a new HostFunction
   */
  static HostFunction multiValue(final MultiValueHostFunction impl) {
    return impl::execute;
  }

  /**
   * Creates a host function with explicit result validation.
   *
   * @param impl the implementation
   * @param expectedResultTypes the expected return types
   * @return a new HostFunction
   */
  static HostFunction withValidation(
      final HostFunction impl, final WasmValueType... expectedResultTypes) {
    return (params) -> {
      final WasmValue[] results = impl.execute(params);
      WasmValue.validateMultiValue(results, expectedResultTypes);
      return results;
    };
  }

  /**
   * Creates a host function with parameter and result validation.
   *
   * @param impl the implementation
   * @param expectedParamTypes the expected parameter types
   * @param expectedResultTypes the expected return types
   * @return a new HostFunction
   */
  static HostFunction withFullValidation(
      final HostFunction impl,
      final WasmValueType[] expectedParamTypes,
      final WasmValueType... expectedResultTypes) {
    return (params) -> {
      WasmValue.validateMultiValue(params, expectedParamTypes);
      final WasmValue[] results = impl.execute(params);
      WasmValue.validateMultiValue(results, expectedResultTypes);
      return results;
    };
  }

  /**
   * Creates a multi-value host function with caller context and validation.
   *
   * @param <T> the type of user data associated with the store
   * @param impl the implementation that receives caller context
   * @param expectedParamTypes the expected parameter types
   * @param expectedResultTypes the expected return types
   * @return a new HostFunction
   * @since 1.0.0
   */
  static <T> HostFunction multiValueWithCallerAndValidation(
      final MultiValueHostFunctionWithCaller<T> impl,
      final WasmValueType[] expectedParamTypes,
      final WasmValueType... expectedResultTypes) {
    return new CallerAwareHostFunction<T>(impl) {
      @Override
      public WasmValue[] execute(WasmValue[] params) throws WasmException {
        WasmValue.validateMultiValue(params, expectedParamTypes);
        WasmValue[] results = super.execute(params);
        WasmValue.validateMultiValue(results, expectedResultTypes);
        return results;
      }
    };
  }

  /**
   * Creates a streaming multi-value host function that can yield values incrementally.
   *
   * <p>This is useful for functions that need to return large amounts of data or perform streaming
   * operations.
   *
   * @param impl the streaming implementation
   * @return a new HostFunction
   * @since 1.0.0
   */
  static HostFunction streaming(final StreamingHostFunction impl) {
    return (params) -> {
      StreamingContext context = new StreamingContext();
      impl.execute(params, context);
      return context.getResults();
    };
  }

  /** Functional interface for void host functions. */
  @FunctionalInterface
  interface VoidHostFunction {
    void execute(WasmValue[] params) throws WasmException;
  }

  /** Functional interface for single-value host functions. */
  @FunctionalInterface
  interface SingleValueHostFunction {
    WasmValue execute(WasmValue[] params) throws WasmException;
  }

  /** Functional interface for multi-value host functions. */
  @FunctionalInterface
  interface MultiValueHostFunction {
    WasmValue[] execute(WasmValue[] params) throws WasmException;
  }

  /**
   * Creates a void host function that receives caller context.
   *
   * <p>Caller-aware host functions can access the calling WebAssembly instance's exports, memory,
   * and other resources through the provided Caller interface.
   *
   * @param <T> the type of user data associated with the store
   * @param impl the implementation that receives caller context
   * @return a new HostFunction
   * @since 1.0.0
   */
  static <T> HostFunction voidFunctionWithCaller(final VoidHostFunctionWithCaller<T> impl) {
    return new CallerAwareHostFunction<>(impl);
  }

  /**
   * Creates a single-value host function that receives caller context.
   *
   * @param <T> the type of user data associated with the store
   * @param impl the implementation that receives caller context
   * @return a new HostFunction
   * @since 1.0.0
   */
  static <T> HostFunction singleValueWithCaller(final SingleValueHostFunctionWithCaller<T> impl) {
    return new CallerAwareHostFunction<>(
        (caller, params) -> {
          @SuppressWarnings("unchecked")
          final WasmValue result = impl.execute((Caller<T>) caller, params);
          return result != null ? new WasmValue[] {result} : new WasmValue[0];
        });
  }

  /**
   * Creates a multi-value host function that receives caller context.
   *
   * @param <T> the type of user data associated with the store
   * @param impl the implementation that receives caller context
   * @return a new HostFunction
   * @since 1.0.0
   */
  static <T> HostFunction multiValueWithCaller(final MultiValueHostFunctionWithCaller<T> impl) {
    return new CallerAwareHostFunction<>(impl);
  }

  /** Functional interface for void host functions with caller context. */
  @FunctionalInterface
  interface VoidHostFunctionWithCaller<T> {
    /**
     * Executes the host function with access to the calling instance context.
     *
     * @param caller the calling instance context
     * @param params the function parameters
     * @throws WasmException if execution fails
     */
    void execute(Caller<T> caller, WasmValue[] params) throws WasmException;
  }

  /** Functional interface for single-value host functions with caller context. */
  @FunctionalInterface
  interface SingleValueHostFunctionWithCaller<T> {
    /**
     * Executes the host function with access to the calling instance context.
     *
     * @param caller the calling instance context
     * @param params the function parameters
     * @return the result value
     * @throws WasmException if execution fails
     */
    WasmValue execute(Caller<T> caller, WasmValue[] params) throws WasmException;
  }

  /** Functional interface for multi-value host functions with caller context. */
  @FunctionalInterface
  interface MultiValueHostFunctionWithCaller<T> {
    /**
     * Executes the host function with access to the calling instance context.
     *
     * @param caller the calling instance context
     * @param params the function parameters
     * @return the result values
     * @throws WasmException if execution fails
     */
    WasmValue[] execute(Caller<T> caller, WasmValue[] params) throws WasmException;
  }

  /** Functional interface for streaming host functions. */
  @FunctionalInterface
  interface StreamingHostFunction {
    /**
     * Executes the streaming host function.
     *
     * @param params the function parameters
     * @param context the streaming context for yielding results
     * @throws WasmException if execution fails
     */
    void execute(WasmValue[] params, StreamingContext context) throws WasmException;
  }

  /**
   * Context for streaming host functions to yield results incrementally.
   *
   * @since 1.0.0
   */
  class StreamingContext {
    private final java.util.List<WasmValue> results = new java.util.ArrayList<>();

    /**
     * Yields a single value.
     *
     * @param value the value to yield
     */
    public void yield(final WasmValue value) {
      if (value != null) {
        results.add(value);
      }
    }

    /**
     * Yields multiple values.
     *
     * @param values the values to yield
     */
    public void yield(final WasmValue... values) {
      if (values != null) {
        for (WasmValue value : values) {
          if (value != null) {
            results.add(value);
          }
        }
      }
    }

    /**
     * Gets the accumulated results.
     *
     * @return the results as an array
     */
    public WasmValue[] getResults() {
      return results.toArray(new WasmValue[0]);
    }

    /**
     * Gets the number of results yielded so far.
     *
     * @return the result count
     */
    public int getResultCount() {
      return results.size();
    }

    /** Clears all accumulated results. */
    public void clear() {
      results.clear();
    }
  }

  /**
   * Wrapper class for host functions that need caller context.
   *
   * <p>This class adapts caller-aware host functions to the standard HostFunction interface by
   * managing the caller context internally.
   *
   * @param <T> the type of user data associated with the store
   * @since 1.0.0
   */
  class CallerAwareHostFunction<T> implements HostFunction {
    private final MultiValueHostFunctionWithCaller<T> implementation;

    /**
     * Creates a caller-aware host function from a void implementation.
     *
     * @param voidImpl the void implementation
     */
    public CallerAwareHostFunction(VoidHostFunctionWithCaller<T> voidImpl) {
      this.implementation =
          (caller, params) -> {
            voidImpl.execute(caller, params);
            return new WasmValue[0];
          };
    }

    /**
     * Creates a caller-aware host function from a multi-value implementation.
     *
     * @param multiValueImpl the multi-value implementation
     */
    public CallerAwareHostFunction(MultiValueHostFunctionWithCaller<T> multiValueImpl) {
      this.implementation = multiValueImpl;
    }

    @Override
    @SuppressWarnings("unchecked")
    public WasmValue[] execute(WasmValue[] params) throws WasmException {
      // The actual caller context will be provided by the runtime implementation
      // This is a placeholder that will be replaced during execution
      Caller<T> caller = getCurrentCaller();
      return implementation.execute(caller, params);
    }

    /**
     * Gets the current caller context.
     *
     * <p>This method delegates to the runtime-provided CallerContextProvider to access the actual
     * calling instance context during execution.
     *
     * @return the current caller context
     * @throws UnsupportedOperationException if no provider is available or caller context is not
     *     set
     */
    @SuppressWarnings("unchecked")
    private Caller<T> getCurrentCaller() {
      // Use ServiceLoader to find the CallerContextProvider implementation
      java.util.ServiceLoader<ai.tegmentum.wasmtime4j.spi.CallerContextProvider> loader =
          java.util.ServiceLoader.load(ai.tegmentum.wasmtime4j.spi.CallerContextProvider.class);

      java.util.Iterator<ai.tegmentum.wasmtime4j.spi.CallerContextProvider> iterator =
          loader.iterator();
      if (iterator.hasNext()) {
        return iterator.next().getCurrentCaller();
      }

      throw new UnsupportedOperationException(
          "No CallerContextProvider found - ensure the runtime module (jni/panama) is on the"
              + " classpath");
    }

    /**
     * Gets the underlying implementation.
     *
     * @return the caller-aware implementation
     */
    public MultiValueHostFunctionWithCaller<T> getImplementation() {
      return implementation;
    }

    /**
     * Checks if this function requires caller context.
     *
     * @return true if caller context is required (always true for this class)
     */
    public boolean requiresCallerContext() {
      return true;
    }
  }

  /**
   * Enumeration of caller context usage patterns for optimization.
   *
   * @since 1.0.0
   */
  enum CallerContextUsage {
    /** No caller context features are used (maximum optimization). */
    NONE,
    /** Only export access is used. */
    EXPORTS_ONLY,
    /** Only fuel tracking is used. */
    FUEL_ONLY,
    /** Only epoch deadlines are used. */
    EPOCH_ONLY,
    /** Export access and fuel tracking are used. */
    EXPORTS_AND_FUEL,
    /** All caller context features are used. */
    FULL;

    /**
     * Checks if this usage pattern includes export access.
     *
     * @return true if export access is used
     */
    public boolean usesExports() {
      return this == EXPORTS_ONLY || this == EXPORTS_AND_FUEL || this == FULL;
    }

    /**
     * Checks if this usage pattern includes fuel tracking.
     *
     * @return true if fuel tracking is used
     */
    public boolean usesFuel() {
      return this == FUEL_ONLY || this == EXPORTS_AND_FUEL || this == FULL;
    }

    /**
     * Checks if this usage pattern includes epoch deadlines.
     *
     * @return true if epoch deadlines are used
     */
    public boolean usesEpoch() {
      return this == EPOCH_ONLY || this == FULL;
    }

    /**
     * Checks if no caller context features are used.
     *
     * @return true if no features are used
     */
    public boolean isNone() {
      return this == NONE;
    }
  }

  /**
   * Creates an optimized host function that avoids caller context overhead when not needed.
   *
   * @param impl the regular host function implementation
   * @return an optimized host function
   * @since 1.0.0
   */
  static HostFunction optimized(final HostFunction impl) {
    return new OptimizedHostFunction(impl);
  }

  /**
   * Creates an optimized caller-aware host function with usage hints.
   *
   * @param <T> the type of user data associated with the store
   * @param impl the caller-aware implementation
   * @param usage the expected usage pattern for optimization
   * @return an optimized caller-aware host function
   * @since 1.0.0
   */
  static <T> HostFunction optimizedWithCaller(
      final MultiValueHostFunctionWithCaller<T> impl, final CallerContextUsage usage) {
    return new OptimizedCallerAwareHostFunction<>(impl, usage);
  }

  /** Optimized host function wrapper that avoids unnecessary overhead. */
  class OptimizedHostFunction implements HostFunction {
    private final HostFunction delegate;
    private final boolean isCallerAware;

    public OptimizedHostFunction(HostFunction delegate) {
      this.delegate = delegate;
      this.isCallerAware = delegate instanceof CallerAwareHostFunction;
    }

    @Override
    public WasmValue[] execute(WasmValue[] params) throws WasmException {
      // For non-caller-aware functions, this provides zero-overhead execution
      return delegate.execute(params);
    }

    /**
     * Checks if this function is caller-aware.
     *
     * @return true if caller-aware
     */
    public boolean isCallerAware() {
      return isCallerAware;
    }

    /**
     * Gets the underlying delegate function.
     *
     * @return the delegate function
     */
    public HostFunction getDelegate() {
      return delegate;
    }
  }

  /**
   * Optimized caller-aware host function with usage pattern hints.
   *
   * @param <T> the type of user data associated with the store
   */
  class OptimizedCallerAwareHostFunction<T> extends CallerAwareHostFunction<T> {
    private final CallerContextUsage contextUsage;

    public OptimizedCallerAwareHostFunction(
        MultiValueHostFunctionWithCaller<T> impl, CallerContextUsage usage) {
      super(impl);
      this.contextUsage = usage;
    }

    /**
     * Gets the caller context usage pattern for optimization.
     *
     * @return the context usage pattern
     */
    public CallerContextUsage getContextUsage() {
      return contextUsage;
    }

    /**
     * Checks if this function can be optimized to avoid caller context overhead.
     *
     * @return true if optimization is possible
     */
    public boolean canOptimize() {
      return contextUsage == CallerContextUsage.NONE;
    }
  }
}
