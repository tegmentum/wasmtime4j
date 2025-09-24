package ai.tegmentum.wasmtime4j;

/**
 * Represents the compatibility status between two components.
 *
 * @since 1.0.0
 */
public final class ComponentCompatibility {

  private final boolean compatible;
  private final String message;

  public ComponentCompatibility(final boolean compatible, final String message) {
    this.compatible = compatible;
    this.message = message;
  }

  public boolean isCompatible() {
    return compatible;
  }

  public String getMessage() {
    return message;
  }
}
