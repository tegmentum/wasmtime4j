package ai.tegmentum.wasmtime4j.component;

/**
 * Enumeration of component item kinds matching Wasmtime's {@code ComponentItem} variants.
 *
 * <p>Each variant represents a different type of item that can appear in a component's imports or
 * exports.
 *
 * @since 1.1.0
 */
public enum ComponentItemKind {
  /** A component model function (high-level, with component model types). */
  COMPONENT_FUNC,

  /** A core WebAssembly function (low-level, with core value types). */
  CORE_FUNC,

  /** A WebAssembly core module. */
  MODULE,

  /** A nested component. */
  COMPONENT,

  /** A component instance (a collection of named exports). */
  COMPONENT_INSTANCE,

  /** A type definition. */
  TYPE,

  /** A resource type. */
  RESOURCE
}
