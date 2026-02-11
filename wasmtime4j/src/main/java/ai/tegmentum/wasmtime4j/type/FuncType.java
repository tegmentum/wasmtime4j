package ai.tegmentum.wasmtime4j.type;
import ai.tegmentum.wasmtime4j.WasmValueType;

import java.util.List;

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

  @Override
  default WasmTypeKind getKind() {
    return WasmTypeKind.FUNCTION;
  }
}
