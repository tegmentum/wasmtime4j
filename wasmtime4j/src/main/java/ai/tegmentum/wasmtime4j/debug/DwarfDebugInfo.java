package ai.tegmentum.wasmtime4j.debug;

/**
 * DWARF debug information interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface DwarfDebugInfo {

  /**
   * Gets the debug info version.
   *
   * @return version number
   */
  int getVersion();

  /**
   * Gets the compilation unit count.
   *
   * @return compilation unit count
   */
  int getCompilationUnitCount();

  /**
   * Gets compilation units.
   *
   * @return list of compilation units
   */
  java.util.List<CompilationUnit> getCompilationUnits();

  /**
   * Finds function by address.
   *
   * @param address function address
   * @return function info or null
   */
  FunctionInfo findFunction(long address);

  /**
   * Gets line number information.
   *
   * @param address instruction address
   * @return line number info or null
   */
  LineNumberInfo getLineNumberInfo(long address);

  /** Compilation unit interface. */
  interface CompilationUnit {
    /**
     * Gets the unit name.
     *
     * @return unit name
     */
    String getName();

    /**
     * Gets the producer information.
     *
     * @return producer info
     */
    String getProducer();

    /**
     * Gets the base address.
     *
     * @return base address
     */
    long getBaseAddress();
  }

  /** Function information interface. */
  interface FunctionInfo {
    /**
     * Gets the function name.
     *
     * @return function name
     */
    String getName();

    /**
     * Gets the start address.
     *
     * @return start address
     */
    long getStartAddress();

    /**
     * Gets the end address.
     *
     * @return end address
     */
    long getEndAddress();
  }

  /** Line number information interface. */
  interface LineNumberInfo {
    /**
     * Gets the file name.
     *
     * @return file name
     */
    String getFileName();

    /**
     * Gets the line number.
     *
     * @return line number
     */
    int getLineNumber();

    /**
     * Gets the column number.
     *
     * @return column number
     */
    int getColumnNumber();
  }
}
