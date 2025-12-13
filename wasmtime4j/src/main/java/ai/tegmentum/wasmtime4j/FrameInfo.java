package ai.tegmentum.wasmtime4j;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Information about a WebAssembly stack frame.
 *
 * <p>FrameInfo provides details about a single frame in a WebAssembly call stack, including
 * function information, module context, and debug symbols extracted from DWARF data.
 *
 * @since 1.0.0
 */
public final class FrameInfo {

  private final int funcIndex;
  private final Module module;
  private final String funcName;
  private final Integer moduleOffset;
  private final Integer funcOffset;
  private final List<FrameSymbol> symbols;

  /**
   * Creates a new FrameInfo with the specified frame details.
   *
   * @param funcIndex the function index within the module
   * @param module the module containing this function
   * @param funcName the function name (if available)
   * @param moduleOffset the offset within the module in bytes
   * @param funcOffset the offset within the function in bytes
   * @param symbols the debug symbols for this frame
   */
  public FrameInfo(
      final int funcIndex,
      final Module module,
      final String funcName,
      final Integer moduleOffset,
      final Integer funcOffset,
      final List<FrameSymbol> symbols) {
    this.funcIndex = funcIndex;
    this.module = module;
    this.funcName = funcName;
    this.moduleOffset = moduleOffset;
    this.funcOffset = funcOffset;
    this.symbols =
        symbols != null ? Collections.unmodifiableList(symbols) : Collections.emptyList();
  }

  /**
   * Gets the function index within the module.
   *
   * <p>This is the index into the module's function space (imports + defined functions).
   *
   * @return the function index
   */
  public int getFuncIndex() {
    return funcIndex;
  }

  /**
   * Gets the module containing this function.
   *
   * @return the module
   */
  public Module getModule() {
    return module;
  }

  /**
   * Gets the function name.
   *
   * <p>This may come from the WebAssembly name section or from DWARF debug info.
   *
   * @return the function name, or empty if not available
   */
  public Optional<String> getFuncName() {
    return Optional.ofNullable(funcName);
  }

  /**
   * Gets the offset within the module where this frame's instruction is located.
   *
   * @return the module offset in bytes, or empty if not available
   */
  public Optional<Integer> getModuleOffset() {
    return Optional.ofNullable(moduleOffset);
  }

  /**
   * Gets the offset within the function where this frame's instruction is located.
   *
   * @return the function offset in bytes, or empty if not available
   */
  public Optional<Integer> getFuncOffset() {
    return Optional.ofNullable(funcOffset);
  }

  /**
   * Gets the debug symbols associated with this frame.
   *
   * <p>Symbols provide source-level information like file names, line numbers, and column numbers
   * extracted from DWARF debug data.
   *
   * @return an immutable list of frame symbols (may be empty)
   */
  public List<FrameSymbol> getSymbols() {
    return new java.util.ArrayList<>(symbols);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final FrameInfo that = (FrameInfo) obj;
    return funcIndex == that.funcIndex
        && Objects.equals(module, that.module)
        && Objects.equals(funcName, that.funcName)
        && Objects.equals(moduleOffset, that.moduleOffset)
        && Objects.equals(funcOffset, that.funcOffset)
        && Objects.equals(symbols, that.symbols);
  }

  @Override
  public int hashCode() {
    return Objects.hash(funcIndex, module, funcName, moduleOffset, funcOffset, symbols);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("FrameInfo{");
    sb.append("funcIndex=").append(funcIndex);
    if (funcName != null) {
      sb.append(", funcName='").append(funcName).append('\'');
    }
    if (moduleOffset != null) {
      sb.append(", moduleOffset=").append(moduleOffset);
    }
    if (funcOffset != null) {
      sb.append(", funcOffset=").append(funcOffset);
    }
    if (!symbols.isEmpty()) {
      sb.append(", symbols=").append(symbols);
    }
    sb.append('}');
    return sb.toString();
  }
}
