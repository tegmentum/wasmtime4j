package ai.tegmentum.wasmtime4j.type;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Represents the type signature of a WebAssembly function.
 *
 * <p>This interface provides access to function type metadata including parameter types and return
 * types. Function types define the calling convention for WebAssembly functions.
 *
 * @since 1.0.0
 */
public interface FuncType extends WasmType {

  /**
   * Gets the parameter types for this function.
   *
   * @return an immutable list of parameter types
   */
  List<WasmValueType> getParams();

  /**
   * Gets the return types for this function.
   *
   * @return an immutable list of return types
   */
  List<WasmValueType> getResults();

  /**
   * Gets the number of parameters.
   *
   * @return the parameter count
   */
  default int getParamCount() {
    return getParams().size();
  }

  /**
   * Gets the number of return values.
   *
   * @return the return value count
   */
  default int getResultCount() {
    return getResults().size();
  }

  /**
   * Gets the parameter types as a list of {@link ValType}.
   *
   * <p>This provides richer type information than {@link #getParams()}, as {@link ValType} carries
   * reference type details including heap type and nullability.
   *
   * @return an immutable list of parameter value types
   * @since 1.1.0
   */
  default List<ValType> params() {
    return getParams().stream().map(ValType::from).collect(Collectors.toUnmodifiableList());
  }

  /**
   * Gets the result types as a list of {@link ValType}.
   *
   * <p>This provides richer type information than {@link #getResults()}, as {@link ValType} carries
   * reference type details including heap type and nullability.
   *
   * @return an immutable list of result value types
   * @since 1.1.0
   */
  default List<ValType> results() {
    return getResults().stream().map(ValType::from).collect(Collectors.toUnmodifiableList());
  }

  /**
   * Gets a specific parameter type by index.
   *
   * @param index the parameter index
   * @return the parameter type, or empty if index is out of bounds
   * @since 1.1.0
   */
  default Optional<ValType> param(final int index) {
    final List<WasmValueType> p = getParams();
    if (index < 0 || index >= p.size()) {
      return Optional.empty();
    }
    return Optional.of(ValType.from(p.get(index)));
  }

  /**
   * Gets a specific result type by index.
   *
   * @param index the result index
   * @return the result type, or empty if index is out of bounds
   * @since 1.1.0
   */
  default Optional<ValType> result(final int index) {
    final List<WasmValueType> r = getResults();
    if (index < 0 || index >= r.size()) {
      return Optional.empty();
    }
    return Optional.of(ValType.from(r.get(index)));
  }

  /**
   * Checks if this function type matches another by comparing params and results.
   *
   * @param other the function type to compare with
   * @return true if the function types have the same params and results
   */
  default boolean matches(final FuncType other) {
    if (other == null) {
      return false;
    }
    return getParams().equals(other.getParams()) && getResults().equals(other.getResults());
  }

  /**
   * Creates a FuncType with the specified parameter and result types.
   *
   * @param params the parameter types
   * @param results the result types
   * @return a new FuncType
   * @throws IllegalArgumentException if params or results is null
   */
  static FuncType of(final WasmValueType[] params, final WasmValueType[] results) {
    return FunctionType.of(params, results);
  }

  /**
   * Creates a FuncType from {@link ValType} arrays, extracting the underlying {@link WasmValueType}
   * from each.
   *
   * <p>This overload is useful when working with the richer {@link ValType} representations. An
   * {@link ai.tegmentum.wasmtime4j.Engine} may be used by implementations to create a canonicalized
   * type via the native type system.
   *
   * @param params the parameter types
   * @param results the result types
   * @return a new FuncType
   * @throws IllegalArgumentException if params or results is null
   * @since 1.1.0
   */
  static FuncType of(final ValType[] params, final ValType[] results) {
    if (params == null) {
      throw new IllegalArgumentException("params cannot be null");
    }
    if (results == null) {
      throw new IllegalArgumentException("results cannot be null");
    }
    final WasmValueType[] paramTypes = new WasmValueType[params.length];
    for (int i = 0; i < params.length; i++) {
      paramTypes[i] = params[i].getValueType();
    }
    final WasmValueType[] resultTypes = new WasmValueType[results.length];
    for (int i = 0; i < results.length; i++) {
      resultTypes[i] = results[i].getValueType();
    }
    return FunctionType.of(paramTypes, resultTypes);
  }

  /**
   * Creates a FuncType with finality and supertype metadata for the GC proposal.
   *
   * <p>This factory method creates a function type with GC subtyping metadata. In the GC proposal,
   * function types can be declared as final (cannot be subtyped) or non-final (can be subtyped),
   * and can optionally declare a supertype.
   *
   * @param finality the finality of this type
   * @param supertype the supertype, or null if no supertype
   * @param params the parameter types
   * @param results the result types
   * @return a new FuncType with finality and supertype
   * @throws IllegalArgumentException if finality, params, or results is null
   * @since 1.1.0
   */
  static FuncType withFinalityAndSupertype(
      final Finality finality,
      final FuncType supertype,
      final WasmValueType[] params,
      final WasmValueType[] results) {
    return new FunctionType(params, results, finality, supertype);
  }

  /**
   * Creates a FuncType with finality and supertype metadata for the GC proposal, using {@link
   * ValType} arrays.
   *
   * <p>This overload extracts the underlying {@link WasmValueType} from each {@link ValType}.
   *
   * @param finality the finality of this type
   * @param supertype the supertype, or null if no supertype
   * @param params the parameter types
   * @param results the result types
   * @return a new FuncType with finality and supertype
   * @throws IllegalArgumentException if finality, params, or results is null
   * @since 1.1.0
   */
  static FuncType withFinalityAndSupertype(
      final Finality finality,
      final FuncType supertype,
      final ValType[] params,
      final ValType[] results) {
    if (params == null) {
      throw new IllegalArgumentException("params cannot be null");
    }
    if (results == null) {
      throw new IllegalArgumentException("results cannot be null");
    }
    final WasmValueType[] paramTypes = new WasmValueType[params.length];
    for (int i = 0; i < params.length; i++) {
      paramTypes[i] = params[i].getValueType();
    }
    final WasmValueType[] resultTypes = new WasmValueType[results.length];
    for (int i = 0; i < results.length; i++) {
      resultTypes[i] = results[i].getValueType();
    }
    return new FunctionType(paramTypes, resultTypes, finality, supertype);
  }

  /**
   * Gets the finality of this function type, if available.
   *
   * <p>In the GC proposal, types can be final (default) or non-final. The finality determines
   * whether other types can declare this type as their supertype.
   *
   * @return the finality, or empty if not available (e.g., for types not registered with an engine)
   * @since 1.1.0
   */
  default Optional<Finality> getFinality() {
    return Optional.empty();
  }

  /**
   * Gets the supertype of this function type, if available.
   *
   * <p>In the GC proposal, function types can declare a supertype. This returns the declared
   * supertype if one exists and the type system information is available.
   *
   * @return the supertype, or empty if this type has no supertype or the info is not available
   * @since 1.1.0
   */
  default Optional<FuncType> getSupertype() {
    return Optional.empty();
  }

  /**
   * Returns the default value for this function type.
   *
   * <p>The default value for a function type is a null funcref, since function references default
   * to null. This corresponds to Wasmtime's {@code FuncType::default_value()}.
   *
   * @return an Optional containing the default WasmValue (null funcref)
   * @since 1.1.0
   */
  default Optional<WasmValue> defaultValue() {
    return Optional.of(WasmValue.nullFuncref());
  }

  @Override
  default WasmTypeKind getKind() {
    return WasmTypeKind.FUNCTION;
  }
}
