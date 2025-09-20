package ai.tegmentum.wasmtime4j.component;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.List;
import java.util.Optional;

/**
 * Interface for WebAssembly Component Model component functions.
 *
 * <p>ComponentFunction represents a callable function exported from a component instance.
 * Component functions support typed arguments and return values according to the Component Model
 * specification, enabling type-safe interaction between components and host environments.
 *
 * <p>Component functions handle complex data types, resource management, and error propagation
 * through the component interface system.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ComponentFunction func = export.asFunction();
 * List<ComponentValue> args = List.of(
 *     ComponentValue.string("hello"),
 *     ComponentValue.u32(42)
 * );
 * ComponentValue result = func.call(args);
 * }</pre>
 *
 * @since 1.0.0
 */
public interface ComponentFunction {

  /**
   * Gets the name of this function.
   *
   * @return the function name
   */
  String getName();

  /**
   * Gets the function signature including parameter and return types.
   *
   * @return the function signature
   */
  ComponentFunctionSignature getSignature();

  /**
   * Gets the parameter types for this function.
   *
   * @return list of parameter types
   */
  List<ComponentValueType> getParameterTypes();

  /**
   * Gets the return type for this function.
   *
   * @return the return type, or empty if function returns void
   */
  Optional<ComponentValueType> getReturnType();

  /**
   * Gets the error type if this function can fail.
   *
   * @return the error type, or empty if function cannot fail
   */
  Optional<ComponentValueType> getErrorType();

  /**
   * Calls the function with the specified arguments.
   *
   * <p>Invokes the component function with typed arguments and returns a typed result. Arguments
   * must match the function's parameter types and count.
   *
   * @param arguments the function arguments
   * @return the function result
   * @throws WasmException if the function call fails or arguments are invalid
   * @throws IllegalArgumentException if arguments don't match the function signature
   */
  ComponentValue call(final List<ComponentValue> arguments) throws WasmException;

  /**
   * Calls the function with no arguments.
   *
   * <p>Convenience method for calling functions that take no parameters.
   *
   * @return the function result
   * @throws WasmException if the function call fails
   * @throws IllegalArgumentException if the function requires arguments
   */
  ComponentValue call() throws WasmException;

  /**
   * Calls the function with a single argument.
   *
   * <p>Convenience method for calling functions that take exactly one parameter.
   *
   * @param argument the function argument
   * @return the function result
   * @throws WasmException if the function call fails
   * @throws IllegalArgumentException if the function signature doesn't match
   */
  ComponentValue call(final ComponentValue argument) throws WasmException;

  /**
   * Calls the function asynchronously if supported.
   *
   * <p>For functions marked as async/suspendable, this method enables non-blocking execution.
   *
   * @param arguments the function arguments
   * @return a future representing the pending result
   * @throws WasmException if the function call fails or async is not supported
   * @throws IllegalArgumentException if arguments don't match the function signature
   */
  java.util.concurrent.CompletableFuture<ComponentValue> callAsync(
      final List<ComponentValue> arguments) throws WasmException;

  /**
   * Validates arguments against this function's signature.
   *
   * <p>Checks that the provided arguments match the function's parameter types and count
   * without actually calling the function.
   *
   * @param arguments the arguments to validate
   * @return true if arguments are valid, false otherwise
   * @throws IllegalArgumentException if arguments is null
   */
  boolean validateArguments(final List<ComponentValue> arguments);

  /**
   * Checks if this function supports asynchronous execution.
   *
   * @return true if the function can be called asynchronously, false otherwise
   */
  boolean isAsync();

  /**
   * Checks if this function is a resource method.
   *
   * @return true if this is a method on a resource type, false otherwise
   */
  boolean isResourceMethod();

  /**
   * Gets the resource type if this is a resource method.
   *
   * @return the resource type, or empty if not a resource method
   */
  Optional<ComponentResourceType> getResourceType();

  /**
   * Checks if this function is a constructor.
   *
   * @return true if this function creates resource instances, false otherwise
   */
  boolean isConstructor();

  /**
   * Checks if this function is static.
   *
   * @return true if this function doesn't require a resource instance, false otherwise
   */
  boolean isStatic();

  /**
   * Gets documentation for this function.
   *
   * @return function documentation, or empty if not available
   */
  Optional<String> getDocumentation();

  /**
   * Gets performance metrics for this function.
   *
   * <p>Returns information about call counts, execution time, and other performance data.
   *
   * @return function performance metrics
   */
  ComponentFunctionMetrics getMetrics();

  /**
   * Checks if this function is still valid and callable.
   *
   * <p>Functions become invalid when their parent component instance is closed or becomes
   * invalid.
   *
   * @return true if the function is valid and callable, false otherwise
   */
  boolean isValid();
}