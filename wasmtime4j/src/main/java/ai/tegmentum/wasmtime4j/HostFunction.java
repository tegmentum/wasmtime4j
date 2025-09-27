package ai.tegmentum.wasmtime4j;

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
   * <p>Caller-aware host functions can access the calling WebAssembly instance's
   * exports, memory, and other resources through the provided Caller interface.
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
    return new CallerAwareHostFunction<>((caller, params) -> {
      final WasmValue result = impl.execute(caller, params);
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

  /**
   * Wrapper class for host functions that need caller context.
   *
   * <p>This class adapts caller-aware host functions to the standard HostFunction
   * interface by managing the caller context internally.
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
      this.implementation = (caller, params) -> {
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
     * <p>This method is implemented by the runtime to provide access to the
     * actual calling instance context during execution.
     *
     * @return the current caller context
     */
    @SuppressWarnings("unchecked")
    private Caller<T> getCurrentCaller() {
      // This will be implemented by the runtime to provide the actual caller
      throw new UnsupportedOperationException("Caller context not available - this should be provided by the runtime");
    }

    /**
     * Gets the underlying implementation.
     *
     * @return the caller-aware implementation
     */
    public MultiValueHostFunctionWithCaller<T> getImplementation() {
      return implementation;
    }
  }
}
