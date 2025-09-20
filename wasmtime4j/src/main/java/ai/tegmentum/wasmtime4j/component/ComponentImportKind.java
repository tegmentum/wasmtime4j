package ai.tegmentum.wasmtime4j.component;

/**
 * Enumeration of WebAssembly component import kinds.
 *
 * <p>ComponentImportKind represents the different categories of imports that can be defined in
 * WebAssembly components. Each kind has specific type requirements and linking behavior.
 *
 * @since 1.0.0
 */
public enum ComponentImportKind {

  /**
   * Function import.
   *
   * <p>A function that must be provided by the host or another component. Function imports have
   * specific signatures that must be matched exactly.
   */
  FUNCTION("function"),

  /**
   * Interface import.
   *
   * <p>A complete interface definition that must be implemented by the host or another component.
   * Interface imports define a collection of related functions and types.
   */
  INTERFACE("interface"),

  /**
   * Resource import.
   *
   * <p>A resource type that must be provided by the host or another component. Resource imports
   * enable sharing of stateful objects across component boundaries.
   */
  RESOURCE("resource"),

  /**
   * Component import.
   *
   * <p>Another component that must be provided for composition. Component imports enable
   * hierarchical component composition and dependency injection.
   */
  COMPONENT("component"),

  /**
   * Instance import.
   *
   * <p>A component instance that must be provided. Instance imports allow sharing of
   * pre-instantiated components across multiple components.
   */
  INSTANCE("instance"),

  /**
   * Type import.
   *
   * <p>A type definition that must be provided by the host environment. Type imports enable
   * sharing of type definitions across component boundaries.
   */
  TYPE("type"),

  /**
   * Module import.
   *
   * <p>A core WebAssembly module that must be provided. Module imports allow components to
   * depend on traditional WebAssembly modules.
   */
  MODULE("module"),

  /**
   * Value import.
   *
   * <p>A constant value that must be provided by the host environment. Value imports enable
   * configuration and parameterization of components.
   */
  VALUE("value");

  private final String displayName;

  ComponentImportKind(final String displayName) {
    this.displayName = displayName;
  }

  /**
   * Gets the human-readable display name for this import kind.
   *
   * <p>Returns a string representation suitable for use in error messages, debugging output,
   * and user interfaces.
   *
   * @return the display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Checks if this import kind represents a callable entity.
   *
   * <p>Returns true if imports of this kind can be invoked or called directly.
   *
   * @return true if callable, false otherwise
   */
  public boolean isCallable() {
    return this == FUNCTION || this == INTERFACE;
  }

  /**
   * Checks if this import kind represents a stateful entity.
   *
   * <p>Returns true if imports of this kind maintain state across operations.
   *
   * @return true if stateful, false otherwise
   */
  public boolean isStateful() {
    return this == RESOURCE || this == INSTANCE || this == COMPONENT;
  }

  /**
   * Checks if this import kind represents a type definition.
   *
   * <p>Returns true if imports of this kind define or provide type information.
   *
   * @return true if type-related, false otherwise
   */
  public boolean isTypeDefinition() {
    return this == TYPE || this == INTERFACE;
  }

  /**
   * Gets the ComponentImportKind from its display name.
   *
   * <p>Parses the display name and returns the corresponding enum value.
   *
   * @param displayName the display name to parse
   * @return the ComponentImportKind
   * @throws IllegalArgumentException if displayName is null, empty, or not recognized
   */
  public static ComponentImportKind fromDisplayName(final String displayName) {
    if (displayName == null || displayName.trim().isEmpty()) {
      throw new IllegalArgumentException("Display name cannot be null or empty");
    }

    for (ComponentImportKind kind : values()) {
      if (kind.displayName.equalsIgnoreCase(displayName.trim())) {
        return kind;
      }
    }

    throw new IllegalArgumentException("Unknown import kind: " + displayName);
  }
}