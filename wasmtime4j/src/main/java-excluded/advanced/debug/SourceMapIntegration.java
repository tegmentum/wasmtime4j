package ai.tegmentum.wasmtime4j.debug;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.Closeable;
import java.util.List;
import java.util.Optional;

/**
 * Advanced WebAssembly source map integration for debugging and symbol resolution.
 *
 * <p>This interface provides comprehensive source map parsing, symbol resolution, and debugging
 * information extraction for WebAssembly modules. It supports standard WebAssembly source maps
 * as well as DWARF debugging information embedded in custom sections.
 *
 * <p>Key features include:
 * <ul>
 * <li>Source map parsing and interpretation</li>
 * <li>Symbol resolution with function name and variable mapping</li>
 * <li>Stack trace mapping from WebAssembly to source code</li>
 * <li>Line number and column mapping with accurate positioning</li>
 * <li>Source file resolution and content loading</li>
 * <li>DWARF debugging information extraction</li>
 * <li>Source map caching and performance optimization</li>
 * <li>Comprehensive validation and error recovery</li>
 * </ul>
 *
 * <p>Thread-safe implementation with defensive programming practices.
 *
 * @since 1.0.0
 */
public interface SourceMapIntegration extends Closeable {

    /**
     * Load and parse a source map from JSON data.
     *
     * <p>Parses a standard WebAssembly source map in JSON format and validates its structure.
     * The source map is cached for performance optimization.
     *
     * @param jsonData the source map JSON data
     * @return the parsed source map
     * @throws WasmException if parsing fails or the source map is invalid
     * @throws IllegalArgumentException if jsonData is null or empty
     */
    SourceMap loadSourceMap(final String jsonData) throws WasmException;

    /**
     * Load and parse a source map from binary data.
     *
     * @param binaryData the source map binary data (typically JSON as UTF-8)
     * @return the parsed source map
     * @throws WasmException if parsing fails or the source map is invalid
     * @throws IllegalArgumentException if binaryData is null or empty
     */
    SourceMap loadSourceMapBinary(final byte[] binaryData) throws WasmException;

    /**
     * Load and parse DWARF debugging information.
     *
     * <p>Extracts DWARF debugging information from WebAssembly custom sections,
     * providing detailed symbol and type information.
     *
     * @param dwarfData the DWARF debugging data from custom sections
     * @return the parsed DWARF information
     * @throws WasmException if parsing fails
     * @throws IllegalArgumentException if dwarfData is null
     */
    DwarfInfo loadDwarfInfo(final byte[] dwarfData) throws WasmException;

    /**
     * Map a WebAssembly stack trace to source code locations.
     *
     * <p>Takes a list of WebAssembly addresses (function index + instruction offset)
     * and maps them to source code locations using available source maps and DWARF information.
     *
     * @param frames the WebAssembly stack frames to map
     * @param sourceMap the source map to use (optional)
     * @param dwarfInfo the DWARF information to use (optional)
     * @param moduleId the module identifier for caching
     * @return list of mapped stack frames with source information
     * @throws WasmException if mapping fails
     * @throws IllegalArgumentException if frames or moduleId is null
     */
    List<SourceMappedFrame> mapStackTrace(
            final List<WasmAddress> frames,
            final SourceMap sourceMap,
            final DwarfInfo dwarfInfo,
            final String moduleId) throws WasmException;

    /**
     * Get the source position for a specific WebAssembly address.
     *
     * @param sourceMap the source map to use
     * @param wasmAddress the WebAssembly address
     * @return the source position if available
     * @throws WasmException if mapping fails
     * @throws IllegalArgumentException if sourceMap or wasmAddress is null
     */
    Optional<SourcePosition> getSourcePosition(
            final SourceMap sourceMap,
            final WasmAddress wasmAddress) throws WasmException;

    /**
     * Resolve function symbol information.
     *
     * <p>Attempts to resolve function name, signature, and variable information
     * for a given function index using available debugging information.
     *
     * @param moduleId the module identifier
     * @param functionIndex the WebAssembly function index
     * @param dwarfInfo optional DWARF information
     * @return the function symbol information if available
     * @throws IllegalArgumentException if moduleId is null
     */
    Optional<FunctionSymbol> resolveFunctionSymbol(
            final String moduleId,
            final int functionIndex,
            final DwarfInfo dwarfInfo);

    /**
     * Load source file content.
     *
     * <p>Loads and caches source file content for display in debugging interfaces.
     * Supports both local file paths and embedded source content from source maps.
     *
     * @param path the source file path
     * @return the source file content
     * @throws WasmException if the file cannot be loaded
     * @throws IllegalArgumentException if path is null
     */
    String loadSourceFile(final String path) throws WasmException;

    /**
     * Format a mapped stack trace as a human-readable string.
     *
     * @param frames the mapped stack frames
     * @return formatted stack trace string
     * @throws IllegalArgumentException if frames is null
     */
    String formatStackTrace(final List<SourceMappedFrame> frames);

    /**
     * Validate source map structure and content.
     *
     * @param sourceMap the source map to validate
     * @return validation result with errors and warnings
     * @throws IllegalArgumentException if sourceMap is null
     */
    ValidationResult validateSourceMap(final SourceMap sourceMap);

    /**
     * Validate DWARF debugging information.
     *
     * @param dwarfInfo the DWARF information to validate
     * @return validation result with errors and warnings
     * @throws IllegalArgumentException if dwarfInfo is null
     */
    ValidationResult validateDwarfInfo(final DwarfInfo dwarfInfo);

    /**
     * Clear all caches to free memory.
     *
     * <p>Clears source map cache, symbol cache, and file content cache.
     * Use this method to manage memory usage in long-running applications.
     */
    void clearCaches();

    /**
     * Get cache statistics for monitoring and optimization.
     *
     * @return cache statistics
     */
    CacheStatistics getCacheStatistics();

    /**
     * Configure cache behavior.
     *
     * @param config the cache configuration
     * @throws IllegalArgumentException if config is null
     */
    void configureCaching(final CacheConfiguration config);

    /**
     * Check if source map support is available.
     *
     * @return true if source map functionality is available
     */
    boolean isSourceMapSupported();

    /**
     * Check if DWARF support is available.
     *
     * @return true if DWARF debugging functionality is available
     */
    boolean isDwarfSupported();

    /**
     * Closes the source map integration and releases resources.
     *
     * <p>After closing, the integration becomes invalid and should not be used.
     */
    @Override
    void close();
}