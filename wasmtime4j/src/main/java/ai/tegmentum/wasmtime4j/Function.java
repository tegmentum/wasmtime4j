package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.List;

/**
 * Represents a WebAssembly function instance.
 *
 * <p>WebAssembly functions can be called from Java code, allowing for interoperation
 * between Java and WebAssembly. Functions have a signature consisting of parameter
 * types and return types.
 *
 * @param <T> the type of user-defined data associated with the store
 * @since 1.0.0
 */
public interface Function<T> {

  /**
   * Calls this function with the provided arguments.
   *
   * @param args the arguments to pass to the function
   * @return the return value(s) from the function call, or null if void
   * @throws WasmException if the function call fails or arguments are invalid
   */
  Object[] call(final Object... args) throws WasmException;

  /**
   * Calls this function with the provided arguments and returns a single result.
   *
   * @param args the arguments to pass to the function
   * @return the single return value from the function call, or null if void
   * @throws WasmException if the function call fails, arguments are invalid, or function returns multiple values
   */
  Object callSingle(final Object... args) throws WasmException;

  /**
   * Gets the function signature.
   *
   * @return the function signature
   */
  FunctionSignature getSignature();

  /**
   * Gets the parameter types for this function.
   *
   * @return list of parameter types
   */
  List<ValueType> getParameterTypes();

  /**
   * Gets the return types for this function.
   *
   * @return list of return types
   */
  List<ValueType> getReturnTypes();

  /**
   * Gets the name of this function, if available.
   *
   * @return the function name, or null if not available
   */
  String getName();

  /**
   * Checks if this function instance is still valid.
   *
   * @return true if the function is valid and can be called, false otherwise
   */
  boolean isValid();

  /**
   * Gets the number of parameters this function expects.
   *
   * @return the parameter count
   */
  int getParameterCount();

  /**
   * Gets the number of return values this function produces.
   *
   * @return the return value count
   */
  int getReturnCount();

  /**
   * Represents the signature of a WebAssembly function.
   */
  interface FunctionSignature {
    /**
     * Gets the parameter types.
     *
     * @return list of parameter types
     */
    List<ValueType> getParameterTypes();

    /**
     * Gets the return types.
     *
     * @return list of return types
     */
    List<ValueType> getReturnTypes();

    /**
     * Checks if this signature matches another signature.
     *
     * @param other the other signature to compare
     * @return true if signatures match, false otherwise
     */
    boolean matches(final FunctionSignature other);
  }

  /**
   * Enumeration of WebAssembly value types.
   */
  enum ValueType {
    /** 32-bit integer */
    I32,
    /** 64-bit integer */
    I64,
    /** 32-bit float */
    F32,
    /** 64-bit float */
    F64,
    /** 128-bit vector */
    V128,
    /** Function reference */
    FUNCREF,
    /** External reference */
    EXTERNREF
  }
}