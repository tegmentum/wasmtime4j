package ai.tegmentum.wasmtime4j.type;

import ai.tegmentum.wasmtime4j.WasmValueType;
import java.util.Objects;

/**
 * Default implementation of {@link GlobalType}.
 *
 * <p>This is a simple value-type implementation for use in the API module when no native handle is
 * needed.
 *
 * @since 1.1.0
 */
final class DefaultGlobalType implements GlobalType {

  private final WasmValueType valueType;
  private final boolean mutable;

  /**
   * Creates a new DefaultGlobalType.
   *
   * @param valueType the value type
   * @param mutability the mutability
   */
  DefaultGlobalType(final WasmValueType valueType, final Mutability mutability) {
    this.valueType = valueType;
    this.mutable = mutability.isVar();
  }

  @Override
  public WasmValueType getValueType() {
    return valueType;
  }

  @Override
  public boolean isMutable() {
    return mutable;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof GlobalType)) {
      return false;
    }
    final GlobalType other = (GlobalType) obj;
    return this.valueType == other.getValueType() && this.mutable == other.isMutable();
  }

  @Override
  public int hashCode() {
    return Objects.hash(valueType, mutable);
  }

  @Override
  public String toString() {
    return "GlobalType(" + valueType + ", " + (mutable ? "var" : "const") + ")";
  }
}
