package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.type.TagType;
import java.util.Objects;

/**
 * Default implementation of {@link TagType}.
 *
 * <p>This class wraps a {@link FunctionType} to provide tag type functionality for the WebAssembly
 * exception handling proposal.
 *
 * @since 1.0.0
 */
public final class DefaultTagType implements TagType {

  private final FunctionType funcType;

  /**
   * Creates a new DefaultTagType wrapping the given function type.
   *
   * @param funcType the function type to wrap
   * @throws IllegalArgumentException if funcType is null
   */
  public DefaultTagType(final FunctionType funcType) {
    if (funcType == null) {
      throw new IllegalArgumentException("funcType cannot be null");
    }
    this.funcType = funcType;
  }

  @Override
  public FunctionType getFunctionType() {
    return funcType;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof TagType)) {
      return false;
    }
    final TagType other = (TagType) obj;
    return funcType.equals(other.getFunctionType());
  }

  @Override
  public int hashCode() {
    return Objects.hash(funcType);
  }

  @Override
  public String toString() {
    return "TagType[" + funcType + "]";
  }
}
