package ai.tegmentum.wasmtime4j.validation;

import java.util.Objects;

/** Result of store validation containing validity status and data. */
public final class StoreValidationResult {
  private final boolean isValid;
  private final Object data;

  private StoreValidationResult(final boolean isValid, final Object data) {
    this.isValid = isValid;
    this.data = data;
  }

  /**
   * Creates a store validation result.
   *
   * @param isValid whether the store is valid
   * @param data the store data
   * @return the validation result
   */
  public static StoreValidationResult of(final boolean isValid, final Object data) {
    return new StoreValidationResult(isValid, data);
  }

  /**
   * Gets whether the store is valid.
   *
   * @return true if valid
   */
  public boolean isValid() {
    return isValid;
  }

  /**
   * Gets the store data.
   *
   * @return the data
   */
  public Object getData() {
    return data;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final StoreValidationResult that = (StoreValidationResult) o;
    return isValid == that.isValid && Objects.equals(data, that.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(isValid, data);
  }

  @Override
  public String toString() {
    return "StoreValidationResult{isValid=" + isValid + ", data=" + data + '}';
  }
}
