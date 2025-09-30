package ai.tegmentum.wasmtime4j.debug;

import java.util.Objects;

/**
 * Metadata information about a WebAssembly source map.
 *
 * <p>This class provides statistical and structural information about a source map
 * without exposing the full content. Useful for debugging, monitoring, and validation.
 *
 * @since 1.0.0
 */
public final class SourceMapMetadata {

    private final int version;
    private final int sourceCount;
    private final int nameCount;
    private final int mappingsLength;
    private final long creationTime;

    /**
     * Creates new source map metadata.
     *
     * @param version the source map format version
     * @param sourceCount the number of source files
     * @param nameCount the number of symbol names
     * @param mappingsLength the length of the mappings string
     */
    public SourceMapMetadata(final int version, final int sourceCount, final int nameCount, final int mappingsLength) {
        this.version = version;
        this.sourceCount = sourceCount;
        this.nameCount = nameCount;
        this.mappingsLength = mappingsLength;
        this.creationTime = System.currentTimeMillis();
    }

    /**
     * Gets the source map format version.
     *
     * @return the format version
     */
    public int getVersion() {
        return version;
    }

    /**
     * Gets the number of source files in the source map.
     *
     * @return the source file count
     */
    public int getSourceCount() {
        return sourceCount;
    }

    /**
     * Gets the number of symbol names in the source map.
     *
     * @return the symbol name count
     */
    public int getNameCount() {
        return nameCount;
    }

    /**
     * Gets the length of the VLQ-encoded mappings string.
     *
     * @return the mappings string length
     */
    public int getMappingsLength() {
        return mappingsLength;
    }

    /**
     * Gets the creation time of this metadata.
     *
     * @return the creation time in milliseconds since epoch
     */
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * Estimates the memory usage of the source map.
     *
     * @return estimated memory usage in bytes
     */
    public long estimateMemoryUsage() {
        // Rough estimation based on string lengths and object overhead
        return (long) sourceCount * 50 + (long) nameCount * 20 + mappingsLength + 1000;
    }

    /**
     * Checks if this represents a valid source map structure.
     *
     * @return true if the metadata indicates a valid structure
     */
    public boolean isValid() {
        return version > 0 && sourceCount >= 0 && nameCount >= 0 && mappingsLength >= 0;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final SourceMapMetadata that = (SourceMapMetadata) obj;
        return version == that.version
                && sourceCount == that.sourceCount
                && nameCount == that.nameCount
                && mappingsLength == that.mappingsLength;
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, sourceCount, nameCount, mappingsLength);
    }

    @Override
    public String toString() {
        return String.format(
                "SourceMapMetadata{version=%d, sources=%d, names=%d, mappings=%d bytes}",
                version, sourceCount, nameCount, mappingsLength
        );
    }
}