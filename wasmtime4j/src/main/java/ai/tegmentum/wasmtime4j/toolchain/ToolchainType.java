package ai.tegmentum.wasmtime4j.toolchain;

/**
 * Types of WebAssembly toolchains supported by wasmtime4j.
 *
 * <p>Defines the different toolchain environments that can be used
 * for WebAssembly development and compilation.
 *
 * @since 1.0.0
 */
public enum ToolchainType {
  /**
   * Rust toolchain using cargo and rustc.
   * Primary toolchain for Wasmtime development.
   */
  RUST("rust", "Rust toolchain with cargo"),

  /**
   * C/C++ toolchain using clang or gcc.
   * For compiling C/C++ code to WebAssembly.
   */
  C_CPP("c-cpp", "C/C++ toolchain with clang/gcc"),

  /**
   * AssemblyScript toolchain.
   * TypeScript-like language that compiles to WebAssembly.
   */
  ASSEMBLYSCRIPT("assemblyscript", "AssemblyScript toolchain"),

  /**
   * Emscripten toolchain.
   * Complete toolchain for porting C/C++ to WebAssembly.
   */
  EMSCRIPTEN("emscripten", "Emscripten C/C++ to WebAssembly toolchain"),

  /**
   * TinyGo toolchain.
   * Go compiler for small devices and WebAssembly.
   */
  TINYGO("tinygo", "TinyGo WebAssembly toolchain"),

  /**
   * WebAssembly Binary Toolkit.
   * Tools for working with WebAssembly binaries.
   */
  WABT("wabt", "WebAssembly Binary Toolkit"),

  /**
   * Custom or unknown toolchain.
   * For user-defined or unsupported toolchains.
   */
  CUSTOM("custom", "Custom toolchain");

  private final String identifier;
  private final String description;

  ToolchainType(final String identifier, final String description) {
    this.identifier = identifier;
    this.description = description;
  }

  /**
   * Gets the toolchain identifier.
   *
   * @return unique identifier for this toolchain type
   */
  public String getIdentifier() {
    return identifier;
  }

  /**
   * Gets the toolchain description.
   *
   * @return human-readable description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Finds a toolchain type by identifier.
   *
   * @param identifier the toolchain identifier
   * @return matching toolchain type, or CUSTOM if not found
   */
  public static ToolchainType fromIdentifier(final String identifier) {
    if (identifier == null) {
      return CUSTOM;
    }

    for (final ToolchainType type : values()) {
      if (type.identifier.equalsIgnoreCase(identifier)) {
        return type;
      }
    }

    return CUSTOM;
  }

  /**
   * Checks if this toolchain supports WebAssembly compilation.
   *
   * @return true if the toolchain can compile to WebAssembly
   */
  public boolean supportsWasmCompilation() {
    return switch (this) {
      case RUST, C_CPP, ASSEMBLYSCRIPT, EMSCRIPTEN, TINYGO -> true;
      case WABT, CUSTOM -> false;
    };
  }

  /**
   * Checks if this toolchain supports debugging information.
   *
   * @return true if the toolchain can generate debug info
   */
  public boolean supportsDebugging() {
    return switch (this) {
      case RUST, C_CPP, EMSCRIPTEN -> true;
      case ASSEMBLYSCRIPT, TINYGO, WABT, CUSTOM -> false;
    };
  }

  /**
   * Gets the typical file extension for source files.
   *
   * @return common file extension for this toolchain
   */
  public String getSourceExtension() {
    return switch (this) {
      case RUST -> ".rs";
      case C_CPP -> ".c";
      case ASSEMBLYSCRIPT -> ".ts";
      case EMSCRIPTEN -> ".c";
      case TINYGO -> ".go";
      case WABT -> ".wat";
      case CUSTOM -> ".txt";
    };
  }

  @Override
  public String toString() {
    return String.format("%s (%s)", identifier, description);
  }
}