package ai.tegmentum.wasmtime4j.debug;

import java.io.Closeable;
import java.util.List;
import java.util.Optional;

/**
 * Represents a WebAssembly source map for debugging support.
 *
 * <p>This interface provides access to source map information that enables mapping between
 * WebAssembly bytecode and original source code locations. It supports standard source map
 * format version 3 with extensions for WebAssembly-specific features.
 *
 * <p>Source maps contain:
 * <ul>
 * <li>Source file references</li>
 * <li>VLQ-encoded position mappings</li>
 * <li>Symbol name tables</li>
 * <li>Optional embedded source content</li>
 * </ul>
 *
 * @since 1.0.0
 */
public interface SourceMap extends Closeable {

    /**
     * Gets the source map format version.
     *
     * @return the source map version (typically 3)
     */
    int getVersion();

    /**
     * Gets the list of source files referenced by this source map.
     *
     * @return an immutable list of source file paths
     */
    List<String> getSources();

    /**
     * Gets the embedded source content if available.
     *
     * <p>This returns the actual content of source files embedded in the source map.
     * Each entry corresponds to the source file at the same index in the sources list.
     * Entries may be null if content is not embedded for that source.
     *
     * @return optional list of source file contents
     */
    Optional<List<Optional<String>>> getSourcesContent();

    /**
     * Gets the list of symbol names used in mappings.
     *
     * @return an immutable list of symbol names
     */
    List<String> getNames();

    /**
     * Gets the VLQ-encoded mappings string.
     *
     * <p>The mappings string contains the actual position mappings in Variable Length
     * Quantity (VLQ) Base64 encoding format as defined by the source map specification.
     *
     * @return the VLQ-encoded mappings string
     */
    String getMappings();

    /**
     * Gets the source root path if specified.
     *
     * @return optional source root path
     */
    Optional<String> getSourceRoot();

    /**
     * Gets the original file path if specified.
     *
     * @return optional original file path
     */
    Optional<String> getFile();

    /**
     * Looks up the source position for a given generated position.
     *
     * @param generatedLine the generated line number (0-based)
     * @param generatedColumn the generated column number (0-based)
     * @return the original source position if available
     */
    Optional<SourcePosition> getOriginalPosition(final int generatedLine, final int generatedColumn);

    /**
     * Looks up all source positions for a given generated line.
     *
     * @param generatedLine the generated line number (0-based)
     * @return list of source positions for the line
     */
    List<SourcePosition> getOriginalPositionsForLine(final int generatedLine);

    /**
     * Gets the source content for a specific source file.
     *
     * @param sourceIndex the index of the source file
     * @return the source content if embedded
     * @throws IndexOutOfBoundsException if sourceIndex is invalid
     */
    Optional<String> getSourceContent(final int sourceIndex);

    /**
     * Gets the source content for a specific source file by path.
     *
     * @param sourcePath the path of the source file
     * @return the source content if embedded
     */
    Optional<String> getSourceContent(final String sourcePath);

    /**
     * Checks if the source map contains mappings for a specific generated position.
     *
     * @param generatedLine the generated line number (0-based)
     * @param generatedColumn the generated column number (0-based)
     * @return true if mappings exist for the position
     */
    boolean hasMapping(final int generatedLine, final int generatedColumn);

    /**
     * Validates the source map structure and content.
     *
     * @return validation result with any errors or warnings
     */
    ValidationResult validate();

    /**
     * Gets metadata about this source map.
     *
     * @return source map metadata
     */
    SourceMapMetadata getMetadata();

    /**
     * Checks if the source map is still valid and usable.
     *
     * @return true if the source map is valid
     */
    boolean isValid();

    /**
     * Closes the source map and releases any associated resources.
     */
    @Override
    void close();
}