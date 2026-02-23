package ai.tegmentum.wasmtime4j.type;

import ai.tegmentum.wasmtime4j.WasmValueType;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Default implementation of {@link TableType}.
 *
 * <p>This is a simple value-type implementation for use in the API module when no native handle is
 * needed.
 *
 * @since 1.1.0
 */
final class DefaultTableType implements TableType {

  private final WasmValueType elementType;
  private final long minimum;
  private final Long maximum;

  private DefaultTableType(final WasmValueType elementType, final long minimum, final Long maximum) {
    this.elementType = elementType;
    this.minimum = minimum;
    this.maximum = maximum;
  }

  /**
   * Creates a new DefaultTableType.
   *
   * @param elementType the element type
   * @param min the minimum size
   * @param max the maximum size
   * @return a new DefaultTableType
   * @throws IllegalArgumentException if elementType is null or min is negative
   */
  static DefaultTableType create(
      final WasmValueType elementType, final long min, final OptionalLong max) {
    if (elementType == null) {
      throw new IllegalArgumentException("elementType cannot be null");
    }
    if (min < 0) {
      throw new IllegalArgumentException("min cannot be negative");
    }
    if (max == null) {
      throw new IllegalArgumentException("max cannot be null; use OptionalLong.empty()");
    }
    final Long maxValue = max.isPresent() ? max.getAsLong() : null;
    return new DefaultTableType(elementType, min, maxValue);
  }

  @Override
  public WasmValueType getElementType() {
    return elementType;
  }

  @Override
  public long getMinimum() {
    return minimum;
  }

  @Override
  public Optional<Long> getMaximum() {
    return Optional.ofNullable(maximum);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof TableType)) {
      return false;
    }
    final TableType other = (TableType) obj;
    return this.elementType == other.getElementType()
        && this.minimum == other.getMinimum()
        && Objects.equals(getMaximum(), other.getMaximum());
  }

  @Override
  public int hashCode() {
    return Objects.hash(elementType, minimum, maximum);
  }

  @Override
  public String toString() {
    if (maximum != null) {
      return "TableType(" + elementType + ", " + minimum + ", " + maximum + ")";
    }
    return "TableType(" + elementType + ", " + minimum + ")";
  }
}
