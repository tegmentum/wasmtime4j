package ai.tegmentum.wasmtime4j;

/**
 * WebAssembly features that can be enabled or disabled during compilation.
 *
 * <p>WebAssembly features represent optional capabilities that extend the core WebAssembly
 * specification. Different runtimes may support different sets of features, and enabling features
 * affects both compilation and runtime behavior.
 *
 * @since 1.0.0
 */
public enum WasmFeature {

  /**
   * Multi-value proposal allowing functions to return multiple values.
   *
   * <p>This feature enables functions to return multiple values directly without using linear
   * memory or global variables.
   */
  MULTI_VALUE("multi-value"),

  /**
   * Reference types proposal introducing new reference types.
   *
   * <p>This feature introduces new reference types including funcref and externref, enabling more
   * efficient host-guest interaction.
   */
  REFERENCE_TYPES("reference-types"),

  /**
   * SIMD (Single Instruction, Multiple Data) proposal for vector operations.
   *
   * <p>This feature adds 128-bit SIMD instructions for efficient parallel computation on vector
   * data.
   */
  SIMD("simd"),

  /**
   * Bulk memory operations proposal for efficient memory operations.
   *
   * <p>This feature adds instructions for bulk memory copying, filling, and table operations.
   */
  BULK_MEMORY("bulk-memory"),

  /**
   * Sign extension operators proposal for efficient integer conversions.
   *
   * <p>This feature adds instructions for sign-extending smaller integer types to larger ones.
   */
  SIGN_EXTENSION("sign-extension"),

  /**
   * Mutable globals proposal allowing mutable global variables.
   *
   * <p>This feature enables global variables to be mutable, allowing shared state between
   * instances.
   */
  MUTABLE_GLOBALS("mutable-globals"),

  /**
   * Saturating float-to-int conversions proposal.
   *
   * <p>This feature adds instructions that perform saturating conversions from floating-point to
   * integer types.
   */
  SATURATING_FLOAT_TO_INT("saturating-float-to-int"),

  /**
   * Multi-memory proposal allowing multiple linear memories.
   *
   * <p>This feature enables modules to have multiple linear memory instances for better memory
   * organization.
   */
  MULTI_MEMORY("multi-memory"),

  /**
   * Exception handling proposal for structured exception handling.
   *
   * <p>This feature adds try-catch exception handling capabilities to WebAssembly.
   */
  EXCEPTION_HANDLING("exception-handling"),

  /**
   * Tail call proposal for efficient tail recursion.
   *
   * <p>This feature adds tail call instructions that enable efficient tail recursion without
   * growing the call stack.
   */
  TAIL_CALL("tail-call"),

  /**
   * Function references proposal for first-class function references.
   *
   * <p>This feature extends reference types with typed function references and call_ref
   * instructions.
   */
  FUNCTION_REFERENCES("function-references"),

  /**
   * GC (Garbage Collection) proposal for managed memory allocation.
   *
   * <p>This feature adds garbage-collected reference types and structured data types.
   */
  GC("gc"),

  /**
   * Relaxed SIMD proposal for additional SIMD operations.
   *
   * <p>This feature adds relaxed SIMD instructions that may have implementation-defined behavior
   * for better performance.
   */
  RELAXED_SIMD("relaxed-simd"),

  /**
   * Extended constant expressions proposal.
   *
   * <p>This feature allows more complex constant expressions in global initializers and other
   * contexts.
   */
  EXTENDED_CONST("extended-const"),

  /**
   * Component model proposal for composable WebAssembly components.
   *
   * <p>This feature enables high-level component composition and interface definitions.
   */
  COMPONENT_MODEL("component-model"),

  /**
   * Threads proposal for shared memory and atomic operations.
   *
   * <p>This feature adds shared linear memory and atomic operations for multi-threaded
   * WebAssembly.
   */
  THREADS("threads");

  private final String name;

  WasmFeature(final String name) {
    this.name = name;
  }

  /**
   * Gets the string name of this WebAssembly feature.
   *
   * @return the feature name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets a WasmFeature by its string name.
   *
   * @param name the feature name
   * @return the corresponding WasmFeature
   * @throws IllegalArgumentException if the name is not recognized
   */
  public static WasmFeature fromName(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Feature name cannot be null");
    }

    for (final WasmFeature feature : values()) {
      if (feature.name.equals(name)) {
        return feature;
      }
    }

    throw new IllegalArgumentException("Unknown WebAssembly feature: " + name);
  }

  /**
   * Checks if this feature is considered stable.
   *
   * <p>Stable features are those that have been finalized in the WebAssembly specification and
   * are widely supported.
   *
   * @return true if the feature is stable, false if experimental
   */
  public boolean isStable() {
    return switch (this) {
      case MULTI_VALUE, REFERENCE_TYPES, BULK_MEMORY, SIGN_EXTENSION, MUTABLE_GLOBALS,
          SATURATING_FLOAT_TO_INT -> true;
      case SIMD, MULTI_MEMORY, EXCEPTION_HANDLING, TAIL_CALL, FUNCTION_REFERENCES, GC,
          RELAXED_SIMD, EXTENDED_CONST, COMPONENT_MODEL, THREADS -> false;
    };
  }

  /**
   * Checks if this feature requires specific runtime support.
   *
   * <p>Some features require special runtime support beyond basic WebAssembly execution.
   *
   * @return true if special runtime support is required, false otherwise
   */
  public boolean requiresSpecialSupport() {
    return switch (this) {
      case THREADS, GC, EXCEPTION_HANDLING, COMPONENT_MODEL -> true;
      default -> false;
    };
  }

  @Override
  public String toString() {
    return name;
  }
}