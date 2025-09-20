package ai.tegmentum.wasmtime4j.concurrency;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents a function call specification for batch execution.
 *
 * <p>This class encapsulates the details of a function call, including the function name and
 * parameters, for use in concurrent batch execution scenarios.
 *
 * @since 1.0.0
 */
public final class FunctionCall {

  private final String functionName;
  private final Object[] parameters;
  private final String callId;

  /**
   * Creates a new function call specification.
   *
   * @param functionName the name of the function to call
   * @param parameters the parameters to pass to the function
   * @throws IllegalArgumentException if functionName is null
   */
  public FunctionCall(final String functionName, final Object... parameters) {
    this(generateCallId(functionName), functionName, parameters);
  }

  /**
   * Creates a new function call specification with a custom call ID.
   *
   * @param callId the unique identifier for this call
   * @param functionName the name of the function to call
   * @param parameters the parameters to pass to the function
   * @throws IllegalArgumentException if callId or functionName is null
   */
  public FunctionCall(final String callId, final String functionName, final Object... parameters) {
    this.callId = Objects.requireNonNull(callId, "Call ID cannot be null");
    this.functionName = Objects.requireNonNull(functionName, "Function name cannot be null");
    this.parameters = parameters != null ? parameters.clone() : new Object[0];
  }

  /**
   * Gets the unique identifier for this function call.
   *
   * @return the call ID
   */
  public String getCallId() {
    return callId;
  }

  /**
   * Gets the name of the function to call.
   *
   * @return the function name
   */
  public String getFunctionName() {
    return functionName;
  }

  /**
   * Gets the parameters to pass to the function.
   *
   * @return a copy of the parameters array
   */
  public Object[] getParameters() {
    return parameters.clone();
  }

  /**
   * Gets the number of parameters.
   *
   * @return the parameter count
   */
  public int getParameterCount() {
    return parameters.length;
  }

  /**
   * Gets a specific parameter by index.
   *
   * @param index the parameter index
   * @return the parameter value
   * @throws IndexOutOfBoundsException if index is out of bounds
   */
  public Object getParameter(final int index) {
    return parameters[index];
  }

  /**
   * Creates a copy of this function call with different parameters.
   *
   * @param newParameters the new parameters
   * @return a new FunctionCall with the same name and ID but different parameters
   */
  public FunctionCall withParameters(final Object... newParameters) {
    return new FunctionCall(callId, functionName, newParameters);
  }

  /**
   * Creates a copy of this function call with a different call ID.
   *
   * @param newCallId the new call ID
   * @return a new FunctionCall with the same name and parameters but different ID
   */
  public FunctionCall withCallId(final String newCallId) {
    return new FunctionCall(newCallId, functionName, parameters);
  }

  private static String generateCallId(final String functionName) {
    return functionName + "-" + System.nanoTime() + "-" + Thread.currentThread().getId();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final FunctionCall that = (FunctionCall) obj;
    return Objects.equals(callId, that.callId)
        && Objects.equals(functionName, that.functionName)
        && Arrays.equals(parameters, that.parameters);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(callId, functionName);
    result = 31 * result + Arrays.hashCode(parameters);
    return result;
  }

  @Override
  public String toString() {
    return String.format(
        "FunctionCall{callId='%s', functionName='%s', parameters=%s}",
        callId, functionName, Arrays.toString(parameters));
  }
}
