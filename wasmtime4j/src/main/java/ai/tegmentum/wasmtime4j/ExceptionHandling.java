package ai.tegmentum.wasmtime4j;

import java.util.List;
import java.util.Map;

/**
 * Exception handling support for WebAssembly exception proposal.
 *
 * <p>This interface provides access to WebAssembly exception handling features that enable
 * structured exception handling within WebAssembly modules. Exception handling allows for
 * more sophisticated error management and control flow patterns in WebAssembly applications.
 *
 * <p>All operations include comprehensive validation and defensive programming to ensure
 * exception safety and prevent runtime errors during exception handling operations.
 *
 * @since 1.0.0
 */
public interface ExceptionHandling {

  /**
   * Checks if exception handling is supported by the current runtime.
   *
   * @return true if exceptions are supported, false otherwise
   */
  boolean areExceptionsSupported();

  /**
   * Creates an exception type with the specified parameter types.
   *
   * @param parameters the list of parameter types for the exception
   * @return a new exception type
   * @throws IllegalArgumentException if parameters is null or contains null values
   * @throws RuntimeException if exception type creation fails
   */
  ExceptionType createExceptionType(final List<WasmValueType> parameters);

  /**
   * Declares an exception type with a name for later reference.
   *
   * @param name the name to associate with the exception type
   * @param type the exception type to declare
   * @throws IllegalArgumentException if name or type is null, or name is empty
   * @throws RuntimeException if exception type declaration fails
   */
  void declareExceptionType(final String name, final ExceptionType type);

  /**
   * Gets a previously declared exception type by name.
   *
   * @param name the name of the exception type to retrieve
   * @return the exception type, or null if not found
   * @throws IllegalArgumentException if name is null or empty
   */
  ExceptionType getExceptionType(final String name);

  /**
   * Creates an exception instance with the specified type and values.
   *
   * @param type the exception type
   * @param values the parameter values for the exception
   * @return a new exception instance
   * @throws IllegalArgumentException if type is null, values is null, or parameter count mismatch
   * @throws RuntimeException if exception creation fails
   */
  WasmException createException(final ExceptionType type, final Object... values);

  /**
   * Throws the specified exception within the WebAssembly execution context.
   *
   * @param exception the exception to throw
   * @throws IllegalArgumentException if exception is null
   * @throws WasmException the thrown exception (always)
   * @throws RuntimeException if the throw operation fails
   */
  void throwException(final WasmException exception) throws WasmException;

  /**
   * Installs an exception handler for the specified exception type.
   *
   * @param type the exception type to handle
   * @param handler the handler function for the exception
   * @throws IllegalArgumentException if type or handler is null
   * @throws RuntimeException if handler installation fails
   */
  void installExceptionHandler(final ExceptionType type, final ExceptionHandler handler);

  /**
   * Removes the exception handler for the specified exception type.
   *
   * @param type the exception type to remove the handler for
   * @throws IllegalArgumentException if type is null
   * @throws RuntimeException if handler removal fails
   */
  void removeExceptionHandler(final ExceptionType type);

  /**
   * Gets all currently installed exception handlers.
   *
   * @return an immutable map of exception types to their handlers
   */
  Map<ExceptionType, ExceptionHandler> getInstalledHandlers();

  /**
   * Gets the exception type of the specified exception instance.
   *
   * @param exception the exception instance to examine
   * @return the exception type
   * @throws IllegalArgumentException if exception is null
   * @throws RuntimeException if unable to determine the exception type
   */
  ExceptionType getExceptionInstanceType(final WasmException exception);

  /**
   * Gets the parameter values from the specified exception instance.
   *
   * @param exception the exception instance to examine
   * @return an array of parameter values
   * @throws IllegalArgumentException if exception is null
   * @throws RuntimeException if unable to extract parameter values
   */
  Object[] getExceptionValues(final WasmException exception);

  /**
   * Checks if the specified exception instance matches the given exception type.
   *
   * @param exception the exception instance to check
   * @param type the exception type to match against
   * @return true if the exception matches the type, false otherwise
   * @throws IllegalArgumentException if exception or type is null
   */
  boolean isExceptionOfType(final WasmException exception, final ExceptionType type);

  /**
   * Enables exception handling for the current execution context.
   *
   * @throws RuntimeException if exception handling cannot be enabled
   */
  void enableExceptionHandling();

  /**
   * Disables exception handling for the current execution context.
   *
   * @throws RuntimeException if exception handling cannot be disabled
   */
  void disableExceptionHandling();

  /**
   * Checks if exception handling is currently enabled.
   *
   * @return true if exception handling is enabled, false otherwise
   */
  boolean isExceptionHandlingEnabled();

  /**
   * Represents a WebAssembly exception type.
   */
  final class ExceptionType {
    private final List<WasmValueType> parameters;
    private final String tag;

    /**
     * Creates a new exception type.
     *
     * @param parameters the parameter types for this exception
     * @param tag a unique identifier for this exception type
     */
    public ExceptionType(final List<WasmValueType> parameters, final String tag) {
      this.parameters = List.copyOf(parameters);
      this.tag = tag;
    }

    /**
     * Gets the parameter types for this exception.
     *
     * @return an immutable list of parameter types
     */
    public List<WasmValueType> getParameters() {
      return parameters;
    }

    /**
     * Gets the unique tag for this exception type.
     *
     * @return the exception type tag
     */
    public String getTag() {
      return tag;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      final ExceptionType that = (ExceptionType) obj;
      return parameters.equals(that.parameters) && tag.equals(that.tag);
    }

    @Override
    public int hashCode() {
      int result = parameters.hashCode();
      result = 31 * result + tag.hashCode();
      return result;
    }

    @Override
    public String toString() {
      return "ExceptionType{" + "parameters=" + parameters + ", tag='" + tag + '\'' + '}';
    }
  }

  /**
   * Functional interface for exception handlers.
   */
  @FunctionalInterface
  interface ExceptionHandler {
    /**
     * Handles the specified exception.
     *
     * @param exception the exception to handle
     * @throws Exception if handling the exception fails
     */
    void handle(final WasmException exception) throws Exception;
  }
}