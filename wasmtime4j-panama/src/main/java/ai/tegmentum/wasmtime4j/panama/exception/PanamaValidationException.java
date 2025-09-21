/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.panama.exception;

/**
 * Exception thrown when parameter validation fails before making Panama FFI calls.
 *
 * <p>This exception is thrown when:
 *
 * <ul>
 *   <li>Required parameters are null or invalid
 *   <li>Parameter values are out of valid range
 *   <li>Parameter combinations are invalid
 *   <li>Buffer sizes or array lengths are invalid
 *   <li>MemorySegment handles are invalid or expired
 *   <li>Arena resources are closed or invalid
 * </ul>
 *
 * <p>Panama-specific validation scenarios include:
 * <ul>
 *   <li>MemorySegment NULL pointer validation
 *   <li>Arena scope validation before operations
 *   <li>MethodHandle validation before invocation
 *   <li>Memory layout compatibility validation
 *   <li>Foreign function parameter type validation
 *   <li>ValueLayout alignment validation
 * </ul>
 *
 * <p>This exception is part of the defensive programming strategy to prevent JVM crashes by
 * validating all parameters before making Panama FFI calls.
 *
 * @since 1.0.0
 */
public final class PanamaValidationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** The parameter name that failed validation. */
  private final String parameterName;

  /** The invalid parameter value. */
  private final transient Object parameterValue;

  /**
   * Creates a new Panama validation exception with the specified message.
   *
   * @param message the error message
   */
  public PanamaValidationException(final String message) {
    super(message);
    this.parameterName = null;
    this.parameterValue = null;
  }

  /**
   * Creates a new Panama validation exception with the specified message and parameter details.
   *
   * @param message the error message
   * @param parameterName the name of the parameter that failed validation
   * @param parameterValue the invalid parameter value
   */
  public PanamaValidationException(
      final String message, final String parameterName, final Object parameterValue) {
    super(message);
    this.parameterName = parameterName;
    this.parameterValue = parameterValue;
  }

  /**
   * Gets the name of the parameter that failed validation.
   *
   * @return the parameter name, or null if not specified
   */
  public String getParameterName() {
    return parameterName;
  }

  /**
   * Gets the invalid parameter value.
   *
   * @return the parameter value, or null if not specified
   */
  public Object getParameterValue() {
    return parameterValue;
  }

  /**
   * Checks if this exception has parameter details.
   *
   * @return true if parameter name and value are available, false otherwise
   */
  public boolean hasParameterDetails() {
    return parameterName != null;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(super.toString());
    if (hasParameterDetails()) {
      sb.append(" (parameter: ")
          .append(parameterName)
          .append(" = ")
          .append(parameterValue)
          .append(")");
    }
    return sb.toString();
  }
}