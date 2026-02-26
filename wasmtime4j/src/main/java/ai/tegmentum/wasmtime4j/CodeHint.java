package ai.tegmentum.wasmtime4j;

/**
 * Hint to the {@link CodeBuilder} about what kind of WebAssembly code is being compiled.
 *
 * <p>This hint allows the compilation pipeline to optimize for the expected code type. If the hint
 * does not match the actual code, compilation will still succeed but may not be optimally tuned.
 *
 * @since 1.1.0
 */
public enum CodeHint {

  /** The code is expected to be a WebAssembly module. */
  MODULE,

  /** The code is expected to be a WebAssembly component. */
  COMPONENT;

  /**
   * Converts a {@link WasmBinaryKind} to a CodeHint.
   *
   * @param kind the binary kind to convert
   * @return the corresponding CodeHint
   * @throws IllegalArgumentException if kind is null or UNKNOWN
   */
  public static CodeHint fromWasmBinaryKind(final WasmBinaryKind kind) {
    if (kind == null) {
      throw new IllegalArgumentException("WasmBinaryKind cannot be null");
    }
    switch (kind) {
      case MODULE:
        return MODULE;
      case COMPONENT:
        return COMPONENT;
      default:
        throw new IllegalArgumentException("Cannot convert UNKNOWN WasmBinaryKind to CodeHint");
    }
  }
}
