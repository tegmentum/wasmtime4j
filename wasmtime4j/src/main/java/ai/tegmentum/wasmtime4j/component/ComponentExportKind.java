package ai.tegmentum.wasmtime4j.component;

/**
 * Enumeration of WebAssembly Component Model export kinds.
 *
 * <p>Defines the different types of entities that can be exported from a component according to
 * the WebAssembly Component Model specification.
 *
 * @since 1.0.0
 */
public enum ComponentExportKind {
  /**
   * A function export.
   *
   * <p>Represents a function that can be called with typed arguments and returns typed results.
   * Component functions support complex data types and structured arguments.
   */
  FUNCTION,

  /**
   * An interface export.
   *
   * <p>Represents a collection of related functions and types that form a cohesive interface.
   * Interfaces enable structured composition and interaction between components.
   */
  INTERFACE,

  /**
   * A resource export.
   *
   * <p>Represents a resource handle that can be used to manage stateful objects and capabilities.
   * Resources provide controlled access to component-managed state.
   */
  RESOURCE,

  /**
   * A type export.
   *
   * <p>Represents a type definition (record, variant, enum, etc.) that can be used by other
   * components or interfaces.
   */
  TYPE,

  /**
   * A core module export.
   *
   * <p>Represents a WebAssembly core module that is made available for direct instantiation or
   * composition with other modules.
   */
  MODULE,

  /**
   * A component export.
   *
   * <p>Represents another component that is nested within and exported by this component,
   * enabling hierarchical composition.
   */
  COMPONENT,

  /**
   * An instance export.
   *
   * <p>Represents an instantiated component or module that is exported with its current state
   * and bindings.
   */
  INSTANCE,

  /**
   * A value export.
   *
   * <p>Represents a constant value or data that is exported from the component.
   */
  VALUE
}