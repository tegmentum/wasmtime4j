package ai.tegmentum.wasmtime4j.debug;

/**
 * Source map integration interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface SourceMapIntegration {

  /**
   * Loads a source map from file.
   *
   * @param filePath source map file path
   * @return true if loaded successfully
   */
  boolean loadSourceMap(String filePath);

  /**
   * Maps WebAssembly address to source location.
   *
   * @param address WebAssembly address
   * @return source location or null
   */
  SourceLocation mapToSource(long address);

  /**
   * Maps source location to WebAssembly address.
   *
   * @param location source location
   * @return WebAssembly address or -1
   */
  long mapToAddress(SourceLocation location);

  /**
   * Gets available source files.
   *
   * @return list of source files
   */
  java.util.List<String> getSourceFiles();

  /**
   * Checks if source map is loaded.
   *
   * @return true if loaded
   */
  boolean isLoaded();

  /**
   * Gets source content for a file.
   *
   * @param fileName source file name
   * @return source content or null
   */
  String getSourceContent(String fileName);

  /** Source location interface. */
  interface SourceLocation {
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

    /**
     * Gets the function name.
     *
     * @return function name or null
     */
    String getFunctionName();
  }
}
