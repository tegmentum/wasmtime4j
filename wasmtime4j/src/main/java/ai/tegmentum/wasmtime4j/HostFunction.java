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

  /**
   * Creates a host function with caller context access that returns no values.
   *
   * <p>This variant provides access to the calling WebAssembly instance's context,
   * allowing the host function to inspect and manipulate the caller's state.
   *
   * @param impl the implementation that processes parameters and caller context
   * @return a new HostFunction with caller access
   * @since 1.0.0
   */
  static HostFunction voidFunctionWithCaller(final VoidHostFunctionWithCaller impl) {
    return new CallerAwareHostFunction((caller, params) -> {
      impl.execute(caller, params);
      return new WasmValue[0];
    });
  }

  /**
   * Creates a host function with caller context access that returns a single value.
   *
   * <p>This variant provides access to the calling WebAssembly instance's context,
   * allowing the host function to inspect and manipulate the caller's state.
   *
   * @param impl the implementation that returns a single value with caller access
   * @return a new HostFunction with caller access
   * @since 1.0.0
   */
  static HostFunction singleValueWithCaller(final SingleValueHostFunctionWithCaller impl) {
    return new CallerAwareHostFunction((caller, params) -> {
      final WasmValue result = impl.execute(caller, params);
      return result != null ? new WasmValue[] {result} : new WasmValue[0];
    });
  }

  /**
   * Creates a host function with caller context access that returns multiple values.
   *
   * <p>This variant provides access to the calling WebAssembly instance's context,
   * allowing the host function to inspect and manipulate the caller's state.
   *
   * @param impl the implementation that returns multiple values with caller access
   * @return a new HostFunction with caller access
   * @since 1.0.0
   */
  static HostFunction multiValueWithCaller(final MultiValueHostFunctionWithCaller impl) {
    return new CallerAwareHostFunction(impl);
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

  /** Functional interface for void host functions with caller context access. */
  @FunctionalInterface
  interface VoidHostFunctionWithCaller {
    void execute(Caller caller, WasmValue[] params) throws WasmException;
  }

  /** Functional interface for single-value host functions with caller context access. */
  @FunctionalInterface
  interface SingleValueHostFunctionWithCaller {
    WasmValue execute(Caller caller, WasmValue[] params) throws WasmException;
  }

  /** Functional interface for multi-value host functions with caller context access. */
  @FunctionalInterface
  interface MultiValueHostFunctionWithCaller {
    WasmValue[] execute(Caller caller, WasmValue[] params) throws WasmException;
  }

  /**
   * Implementation wrapper for host functions that require caller context access.
   * This is used internally to distinguish caller-aware functions from regular ones.
   */
  class CallerAwareHostFunction implements HostFunction {
    private final MultiValueHostFunctionWithCaller implementation;

    CallerAwareHostFunction(final MultiValueHostFunctionWithCaller implementation) {
      this.implementation = implementation;
    }

    @Override
    public WasmValue[] execute(final WasmValue[] params) throws WasmException {
      throw new UnsupportedOperationException(
          "CallerAwareHostFunction requires caller context - should not be called directly");
    }

    /**
     * Executes the function with caller context.
     *
     * @param caller the calling WebAssembly instance context
     * @param params the parameters passed from WebAssembly
     * @return the results to return to WebAssembly
     * @throws WasmException if function execution fails
     */
    public WasmValue[] executeWithCaller(final Caller caller, final WasmValue[] params) throws WasmException {
      return implementation.execute(caller, params);
    }

    /**
     * Checks if this is a caller-aware host function.
     *
     * @param hostFunction the host function to check
     * @return true if the function requires caller context
     */
    public static boolean isCallerAware(final HostFunction hostFunction) {
      return hostFunction instanceof CallerAwareHostFunction;
    }
  }
}
