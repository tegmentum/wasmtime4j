package ai.tegmentum.wasmtime4j.diagnostics;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Debug information for WebAssembly modules and execution.
 *
 * <p>This interface provides access to debugging information including source maps, symbol tables,
 * and execution state data.
 *
 * @since 1.0.0
 */
public interface DebugInfo {

  /** Debug information format types. */
  enum Format {
    /** DWARF debug information */
    DWARF,
    /** WebAssembly name section */
    NAME_SECTION,
    /** Source map format */
    SOURCE_MAP,
    /** Custom debug format */
    CUSTOM
  }

  /**
   * Gets the debug information format.
   *
   * @return the debug format
   */
  Format getFormat();

  /**
   * Gets the source map if available.
   *
   * @return the source map, or empty if not available
   */
  Optional<SourceMap> getSourceMap();

  /**
   * Gets the symbol table information.
   *
   * @return the symbol table, or empty if not available
   */
  Optional<SymbolTable> getSymbolTable();

  /**
   * Gets the function debug information.
   *
   * @return list of function debug info
   */
  List<FunctionDebugInfo> getFunctions();

  /**
   * Gets debug information for a specific function.
   *
   * @param functionIndex the function index
   * @return the function debug info, or empty if not available
   */
  Optional<FunctionDebugInfo> getFunctionInfo(int functionIndex);

  /**
   * Gets debug information for a specific function by name.
   *
   * @param functionName the function name
   * @return the function debug info, or empty if not available
   */
  Optional<FunctionDebugInfo> getFunctionInfo(String functionName);

  /**
   * Gets the variable information for a function.
   *
   * @param functionIndex the function index
   * @return list of variable information
   */
  List<VariableInfo> getVariables(int functionIndex);

  /**
   * Gets the breakpoint information.
   *
   * @return list of available breakpoints
   */
  List<BreakpointInfo> getBreakpoints();

  /**
   * Gets the line number mapping.
   *
   * @return the line number mapping, or empty if not available
   */
  Optional<LineNumberMapping> getLineMapping();

  /**
   * Gets additional debug properties.
   *
   * @return map of debug properties
   */
  Map<String, Object> getProperties();

  /**
   * Checks if debug information is complete.
   *
   * @return true if debug information is complete
   */
  boolean isComplete();

  /**
   * Checks if optimizations have affected debug information.
   *
   * @return true if optimizations may have affected debug info
   */
  boolean isOptimized();

  /**
   * Creates a builder for constructing DebugInfo instances.
   *
   * @return a new debug info builder
   */
  static DebugInfoBuilder builder() {
    return new DebugInfoBuilder();
  }
}
