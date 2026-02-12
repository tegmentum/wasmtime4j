/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.wit;

import ai.tegmentum.wasmtime4j.exception.ValidationException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * WIT function binding and marshaling system.
 *
 * <p>This class provides comprehensive support for binding WIT interface functions to Java methods,
 * including parameter marshaling, return value conversion, and type safety validation.
 *
 * @since 1.0.0
 */
public final class WitFunctionBinder {

  private static final Logger LOGGER = Logger.getLogger(WitFunctionBinder.class.getName());

  private final WitTypeValidator validator;
  private final WitValueMarshaler marshaler;
  private final Map<String, BoundFunction> boundFunctions;
  private final Map<Class<?>, TypeAdapter<?>> typeAdapters;

  /** Creates a new WIT function binder. */
  public WitFunctionBinder() {
    this.validator = new WitTypeValidator();
    this.marshaler = new WitValueMarshaler();
    this.boundFunctions = new ConcurrentHashMap<>();
    this.typeAdapters = new ConcurrentHashMap<>();
    initializeBuiltInAdapters();
  }

  /**
   * Binds a WIT function to a Java method implementation.
   *
   * @param functionName the WIT function name
   * @param function the WIT function definition
   * @param implementation the Java object containing the implementation
   * @param methodName the Java method name
   * @throws WasmException if binding fails
   */
  @SuppressFBWarnings(
      value = "REC_CATCH_EXCEPTION",
      justification = "Intentionally catching Exception to wrap all failures in WasmException")
  public void bindFunction(
      final String functionName,
      final WitFunction function,
      final Object implementation,
      final String methodName)
      throws WasmException {

    Objects.requireNonNull(functionName, "functionName");
    Objects.requireNonNull(function, "function");
    Objects.requireNonNull(implementation, "implementation");
    Objects.requireNonNull(methodName, "methodName");

    try {
      // Find the matching Java method
      final Method javaMethod = findMatchingMethod(implementation, methodName, function);

      // Validate method signature compatibility
      validateMethodCompatibility(javaMethod, function);

      // Create bound function
      final BoundFunction boundFunction =
          new BoundFunction(
              function,
              implementation,
              javaMethod,
              createParameterMarshalers(function.getParameters()),
              createReturnMarshalers(function.getReturnTypes()));

      boundFunctions.put(functionName, boundFunction);
      LOGGER.fine("Bound WIT function '" + functionName + "' to Java method '" + methodName + "'");

    } catch (final Exception e) {
      throw new WasmException("Failed to bind WIT function: " + functionName, e);
    }
  }

  /**
   * Invokes a bound WIT function with the given arguments.
   *
   * @param functionName the function name
   * @param args the function arguments
   * @return the function result
   * @throws WasmException if invocation fails
   */
  public Object invokeFunction(final String functionName, final Object... args)
      throws WasmException {
    Objects.requireNonNull(functionName, "functionName");

    final BoundFunction boundFunction = boundFunctions.get(functionName);
    if (boundFunction == null) {
      throw new WasmException("Function not bound: " + functionName);
    }

    try {
      // Marshal input arguments
      final Object[] marshaledArgs = marshalInputArguments(boundFunction, args);

      // Invoke Java method
      final Object result =
          boundFunction.getMethod().invoke(boundFunction.getImplementation(), marshaledArgs);

      // Marshal return value
      return marshalReturnValue(boundFunction, result);

    } catch (final Exception e) {
      throw new WasmException("Failed to invoke function: " + functionName, e);
    }
  }

  /**
   * Registers a custom type adapter for marshaling.
   *
   * @param javaType the Java type
   * @param adapter the type adapter
   * @param <T> the type parameter
   */
  public <T> void registerTypeAdapter(final Class<T> javaType, final TypeAdapter<T> adapter) {
    Objects.requireNonNull(javaType, "javaType");
    Objects.requireNonNull(adapter, "adapter");

    typeAdapters.put(javaType, adapter);
    LOGGER.fine("Registered type adapter for: " + javaType.getName());
  }

  /**
   * Checks if a function is bound.
   *
   * @param functionName the function name
   * @return true if bound, false otherwise
   */
  public boolean isFunctionBound(final String functionName) {
    return boundFunctions.containsKey(functionName);
  }

  /**
   * Gets all bound function names.
   *
   * @return list of bound function names
   */
  public List<String> getBoundFunctionNames() {
    return new ArrayList<>(boundFunctions.keySet());
  }

  /**
   * Unbinds a function.
   *
   * @param functionName the function name
   * @return true if function was bound and removed, false otherwise
   */
  public boolean unbindFunction(final String functionName) {
    final BoundFunction removed = boundFunctions.remove(functionName);
    if (removed != null) {
      LOGGER.fine("Unbound WIT function: " + functionName);
      return true;
    }
    return false;
  }

  /** Clears all function bindings. */
  public void clearBindings() {
    boundFunctions.clear();
    LOGGER.fine("Cleared all WIT function bindings");
  }

  /**
   * Finds a matching Java method for a WIT function.
   *
   * @param implementation the implementation object
   * @param methodName the method name
   * @param function the WIT function
   * @return the matching Java method
   * @throws NoSuchMethodException if no matching method is found
   */
  private Method findMatchingMethod(
      final Object implementation, final String methodName, final WitFunction function)
      throws NoSuchMethodException {

    final Class<?> implementationClass = implementation.getClass();
    final Method[] methods = implementationClass.getMethods();

    // First try exact name match
    for (final Method method : methods) {
      if (method.getName().equals(methodName)) {
        if (isMethodCompatible(method, function)) {
          return method;
        }
      }
    }

    // If no exact match, try parameter count match
    final int expectedParamCount = function.getParameters().size();
    for (final Method method : methods) {
      if (method.getName().equals(methodName) && method.getParameterCount() == expectedParamCount) {
        return method;
      }
    }

    throw new NoSuchMethodException(
        "No compatible method found: " + methodName + " in " + implementationClass.getName());
  }

  /**
   * Checks if a Java method is compatible with a WIT function.
   *
   * @param method the Java method
   * @param function the WIT function
   * @return true if compatible, false otherwise
   */
  private boolean isMethodCompatible(final Method method, final WitFunction function) {

    // Check parameter count
    if (method.getParameterCount() != function.getParameters().size()) {
      return false;
    }

    // Check parameter types (basic compatibility)
    final Class<?>[] paramTypes = method.getParameterTypes();
    final List<WitParameter> witParams = function.getParameters();

    for (int i = 0; i < paramTypes.length; i++) {
      if (!isTypeCompatible(paramTypes[i], witParams.get(i).getType())) {
        return false;
      }
    }

    // Check return type
    final Class<?> returnType = method.getReturnType();
    final List<WitType> witReturnTypes = function.getReturnTypes();

    if (witReturnTypes.isEmpty()) {
      return returnType == void.class || returnType == Void.class;
    } else if (witReturnTypes.size() == 1) {
      return isTypeCompatible(returnType, witReturnTypes.get(0));
    } else {
      // Multiple return values - Java method should return array or collection
      return returnType.isArray() || java.util.Collection.class.isAssignableFrom(returnType);
    }
  }

  /**
   * Validates method compatibility with WIT function.
   *
   * @param method the Java method
   * @param function the WIT function
   * @throws ValidationException if not compatible
   */
  private void validateMethodCompatibility(final Method method, final WitFunction function)
      throws ValidationException {

    final List<String> errors = new ArrayList<>();

    // Validate parameter compatibility
    final Class<?>[] paramTypes = method.getParameterTypes();
    final List<WitParameter> witParams = function.getParameters();

    if (paramTypes.length != witParams.size()) {
      errors.add(
          "Parameter count mismatch: Java method has "
              + paramTypes.length
              + " parameters, WIT function has "
              + witParams.size());
    }

    for (int i = 0; i < Math.min(paramTypes.length, witParams.size()); i++) {
      if (!isTypeCompatible(paramTypes[i], witParams.get(i).getType())) {
        errors.add(
            "Parameter "
                + i
                + " type incompatible: Java "
                + paramTypes[i].getName()
                + " vs WIT "
                + witParams.get(i).getType().getName());
      }
    }

    // Validate return type compatibility
    final Class<?> returnType = method.getReturnType();
    final List<WitType> witReturnTypes = function.getReturnTypes();

    if (witReturnTypes.isEmpty() && returnType != void.class && returnType != Void.class) {
      errors.add("WIT function returns no value but Java method returns " + returnType.getName());
    } else if (witReturnTypes.size() == 1) {
      if (!isTypeCompatible(returnType, witReturnTypes.get(0))) {
        errors.add(
            "Return type incompatible: Java "
                + returnType.getName()
                + " vs WIT "
                + witReturnTypes.get(0).getName());
      }
    }

    if (!errors.isEmpty()) {
      throw new ValidationException(
          "Method compatibility validation failed: " + String.join("; ", errors));
    }
  }

  /**
   * Checks if a Java type is compatible with a WIT type.
   *
   * @param javaType the Java type
   * @param witType the WIT type
   * @return true if compatible, false otherwise
   */
  private boolean isTypeCompatible(final Class<?> javaType, final WitType witType) {
    // Check for registered type adapters
    if (typeAdapters.containsKey(javaType)) {
      return true;
    }

    // Basic primitive type compatibility
    if (witType.isPrimitive()) {
      // Use name-based matching for primitive compatibility
      return isJavaTypePrimitiveCompatible(javaType, witType.getName());
    }

    // String compatibility
    if (javaType == String.class && witType.getName().equals("string")) {
      return true;
    }

    // Object types can be marshaled
    if (!javaType.isPrimitive()) {
      return true;
    }

    return false;
  }

  /**
   * Checks Java primitive type compatibility.
   *
   * @param javaType the Java type
   * @param witTypeName the WIT type name
   * @return true if compatible, false otherwise
   */
  private boolean isJavaTypePrimitiveCompatible(final Class<?> javaType, final String witTypeName) {
    switch (witTypeName) {
      case "bool":
        return javaType == boolean.class || javaType == Boolean.class;
      case "s8":
      case "u8":
        return javaType == byte.class || javaType == Byte.class;
      case "s16":
      case "u16":
        return javaType == short.class || javaType == Short.class;
      case "s32":
      case "u32":
        return javaType == int.class || javaType == Integer.class;
      case "s64":
      case "u64":
        return javaType == long.class || javaType == Long.class;
      case "float32":
        return javaType == float.class || javaType == Float.class;
      case "float64":
        return javaType == double.class || javaType == Double.class;
      case "char":
        return javaType == char.class || javaType == Character.class;
      case "string":
        return javaType == String.class;
      default:
        return false;
    }
  }

  /**
   * Creates parameter marshalers for WIT function parameters.
   *
   * @param parameters the WIT function parameters
   * @return list of parameter marshalers
   */
  private List<ParameterMarshaler> createParameterMarshalers(final List<WitParameter> parameters) {

    final List<ParameterMarshaler> marshalers = new ArrayList<>();
    for (final WitParameter parameter : parameters) {
      marshalers.add(new ParameterMarshaler(parameter, marshaler));
    }
    return marshalers;
  }

  /**
   * Creates return value marshalers for WIT function return types.
   *
   * @param returnTypes the WIT function return types
   * @return list of return marshalers
   */
  private List<ReturnMarshaler> createReturnMarshalers(final List<WitType> returnTypes) {
    final List<ReturnMarshaler> marshalers = new ArrayList<>();
    for (final WitType returnType : returnTypes) {
      marshalers.add(new ReturnMarshaler(returnType, marshaler));
    }
    return marshalers;
  }

  /**
   * Marshals input arguments for function invocation.
   *
   * @param boundFunction the bound function
   * @param args the input arguments
   * @return marshaled arguments
   * @throws Exception if marshaling fails
   */
  private Object[] marshalInputArguments(final BoundFunction boundFunction, final Object[] args)
      throws Exception {

    final List<ParameterMarshaler> paramMarshalers = boundFunction.getParameterMarshalers();
    if (args.length != paramMarshalers.size()) {
      throw new IllegalArgumentException(
          "Argument count mismatch: expected " + paramMarshalers.size() + ", got " + args.length);
    }

    final Object[] marshaledArgs = new Object[args.length];
    for (int i = 0; i < args.length; i++) {
      marshaledArgs[i] = paramMarshalers.get(i).marshal(args[i]);
    }

    return marshaledArgs;
  }

  /**
   * Marshals return value from function invocation.
   *
   * @param boundFunction the bound function
   * @param result the function result
   * @return marshaled result
   * @throws Exception if marshaling fails
   */
  private Object marshalReturnValue(final BoundFunction boundFunction, final Object result)
      throws Exception {

    final List<ReturnMarshaler> returnMarshalers = boundFunction.getReturnMarshalers();
    if (returnMarshalers.isEmpty()) {
      return null;
    } else if (returnMarshalers.size() == 1) {
      return returnMarshalers.get(0).marshal(result);
    } else {
      // Multiple return values - marshal each component
      if (result instanceof Object[]) {
        final Object[] resultArray = (Object[]) result;
        final Object[] marshaledResults = new Object[resultArray.length];
        for (int i = 0; i < resultArray.length; i++) {
          marshaledResults[i] = returnMarshalers.get(i).marshal(resultArray[i]);
        }
        return marshaledResults;
      } else {
        throw new IllegalArgumentException("Expected array return for multiple return types");
      }
    }
  }

  /** Initializes built-in type adapters. */
  private void initializeBuiltInAdapters() {
    // String adapter
    registerTypeAdapter(
        String.class,
        new TypeAdapter<String>() {
          @Override
          public Object toWit(final String value) {
            return value;
          }

          @Override
          public String fromWit(final Object value) {
            return value.toString();
          }
        });

    // Primitive wrapper adapters
    registerTypeAdapter(
        Boolean.class,
        new TypeAdapter<Boolean>() {
          @Override
          public Object toWit(final Boolean value) {
            return value;
          }

          @Override
          public Boolean fromWit(final Object value) {
            return (Boolean) value;
          }
        });

    registerTypeAdapter(
        Integer.class,
        new TypeAdapter<Integer>() {
          @Override
          public Object toWit(final Integer value) {
            return value;
          }

          @Override
          public Integer fromWit(final Object value) {
            return (Integer) value;
          }
        });

    // Add more built-in adapters as needed
  }

  /** Represents a bound WIT function. */
  private static final class BoundFunction {
    private final WitFunction function;
    private final Object implementation;
    private final Method method;
    private final List<ParameterMarshaler> parameterMarshalers;
    private final List<ReturnMarshaler> returnMarshalers;

    public BoundFunction(
        final WitFunction function,
        final Object implementation,
        final Method method,
        final List<ParameterMarshaler> parameterMarshalers,
        final List<ReturnMarshaler> returnMarshalers) {
      this.function = function;
      this.implementation = implementation;
      this.method = method;
      this.parameterMarshalers = parameterMarshalers;
      this.returnMarshalers = returnMarshalers;
    }

    public WitFunction getFunction() {
      return function;
    }

    public Object getImplementation() {
      return implementation;
    }

    public Method getMethod() {
      return method;
    }

    public List<ParameterMarshaler> getParameterMarshalers() {
      return parameterMarshalers;
    }

    public List<ReturnMarshaler> getReturnMarshalers() {
      return returnMarshalers;
    }
  }

  /** Parameter marshaler. */
  private static final class ParameterMarshaler {
    private final WitParameter parameter;
    private final WitValueMarshaler marshaler;

    public ParameterMarshaler(final WitParameter parameter, final WitValueMarshaler marshaler) {
      this.parameter = parameter;
      this.marshaler = marshaler;
    }

    public Object marshal(final Object value) throws Exception {
      return marshaler.marshalToJava(value, parameter.getType());
    }
  }

  /** Return value marshaler. */
  private static final class ReturnMarshaler {
    private final WitType returnType;
    private final WitValueMarshaler marshaler;

    public ReturnMarshaler(final WitType returnType, final WitValueMarshaler marshaler) {
      this.returnType = returnType;
      this.marshaler = marshaler;
    }

    public Object marshal(final Object value) throws Exception {
      return marshaler.marshalToWit(value, returnType);
    }
  }

  /** Type adapter interface for custom type conversions. */
  public interface TypeAdapter<T> {
    /**
     * Converts Java value to WIT representation.
     *
     * @param value the Java value
     * @return the WIT representation
     */
    Object toWit(T value);

    /**
     * Converts WIT representation to Java value.
     *
     * @param value the WIT representation
     * @return the Java value
     */
    T fromWit(Object value);
  }
}
