package ai.tegmentum.wasmtime4j.debug;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * WebAssembly source map implementation supporting the ECMA-426 standard.
 * Provides bidirectional mapping between WebAssembly binary locations and original source code.
 *
 * <p>This implementation supports:
 * <ul>
 *   <li>Source map version 3 format</li>
 *   <li>VLQ (Variable Length Quantity) encoded mappings</li>
 *   <li>Inline source content</li>
 *   <li>Multi-file source mapping</li>
 *   <li>WebAssembly-specific byte offset mappings</li>
 * </ul>
 */
public final class SourceMap {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String SOURCE_MAP_URL_PREFIX = "sourceMappingURL=";

    private final int version;
    private final String file;
    private final String sourceRoot;
    private final List<String> sources;
    private final List<String> sourcesContent;
    private final List<String> names;
    private final String mappings;
    private final List<Mapping> parsedMappings;
    private final Map<Integer, List<Mapping>> lineToMappings;

    /**
     * Creates a source map from parsed JSON data.
     *
     * @param version Source map version (should be 3)
     * @param file The generated file name
     * @param sourceRoot Optional source root URL
     * @param sources List of source file names
     * @param sourcesContent Optional inline source content
     * @param names List of symbol names
     * @param mappings VLQ-encoded mapping data
     * @throws IllegalArgumentException if version is not supported
     */
    public SourceMap(final int version, final String file, final String sourceRoot,
                     final List<String> sources, final List<String> sourcesContent,
                     final List<String> names, final String mappings) {
        if (version != 3) {
            throw new IllegalArgumentException("Unsupported source map version: " + version);
        }

        this.version = version;
        this.file = Objects.requireNonNull(file, "File cannot be null");
        this.sourceRoot = sourceRoot;
        this.sources = Collections.unmodifiableList(new ArrayList<>(
            Objects.requireNonNull(sources, "Sources cannot be null")));
        this.sourcesContent = sourcesContent != null ?
            Collections.unmodifiableList(new ArrayList<>(sourcesContent)) : null;
        this.names = Collections.unmodifiableList(new ArrayList<>(
            Objects.requireNonNull(names, "Names cannot be null")));
        this.mappings = Objects.requireNonNull(mappings, "Mappings cannot be null");

        // Parse mappings during construction for performance
        this.parsedMappings = parseMappings(mappings);
        this.lineToMappings = buildLineIndex(parsedMappings);
    }

    /**
     * Loads a source map from JSON content.
     *
     * @param json The JSON source map content
     * @return Parsed source map
     * @throws SourceMapParseException if parsing fails
     */
    public static SourceMap fromJson(final String json) throws SourceMapParseException {
        try {
            final JsonNode root = OBJECT_MAPPER.readTree(json);

            final int version = root.get("version").asInt();
            final String file = root.has("file") ? root.get("file").asText() : "";
            final String sourceRoot = root.has("sourceRoot") ? root.get("sourceRoot").asText() : null;

            final List<String> sources = new ArrayList<>();
            final JsonNode sourcesNode = root.get("sources");
            if (sourcesNode != null && sourcesNode.isArray()) {
                for (final JsonNode source : sourcesNode) {
                    sources.add(source.asText());
                }
            }

            final List<String> sourcesContent = new ArrayList<>();
            final JsonNode sourcesContentNode = root.get("sourcesContent");
            if (sourcesContentNode != null && sourcesContentNode.isArray()) {
                for (final JsonNode content : sourcesContentNode) {
                    sourcesContent.add(content.isNull() ? null : content.asText());
                }
            }

            final List<String> names = new ArrayList<>();
            final JsonNode namesNode = root.get("names");
            if (namesNode != null && namesNode.isArray()) {
                for (final JsonNode name : namesNode) {
                    names.add(name.asText());
                }
            }

            final String mappings = root.has("mappings") ? root.get("mappings").asText() : "";

            return new SourceMap(version, file, sourceRoot, sources,
                               sourcesContent.isEmpty() ? null : sourcesContent,
                               names, mappings);

        } catch (final IOException | RuntimeException e) {
            throw new SourceMapParseException("Failed to parse source map JSON", e);
        }
    }

    /**
     * Loads a source map from a file.
     *
     * @param path Path to the source map file
     * @return Parsed source map
     * @throws SourceMapParseException if loading or parsing fails
     */
    public static SourceMap fromFile(final Path path) throws SourceMapParseException {
        try {
            final String content = Files.readString(path);
            return fromJson(content);
        } catch (final IOException e) {
            throw new SourceMapParseException("Failed to read source map file: " + path, e);
        }
    }

    /**
     * Extracts source map URL from WebAssembly custom section data.
     *
     * @param customSectionData The custom section binary data
     * @return Source map URL if found
     */
    public static Optional<String> extractSourceMapUrl(final byte[] customSectionData) {
        if (customSectionData == null || customSectionData.length == 0) {
            return Optional.empty();
        }

        final String content = new String(customSectionData);
        final int urlIndex = content.indexOf(SOURCE_MAP_URL_PREFIX);
        if (urlIndex == -1) {
            return Optional.empty();
        }

        final int urlStart = urlIndex + SOURCE_MAP_URL_PREFIX.length();
        final int urlEnd = content.indexOf('\n', urlStart);
        final String url = urlEnd == -1 ?
            content.substring(urlStart).trim() :
            content.substring(urlStart, urlEnd).trim();

        return url.isEmpty() ? Optional.empty() : Optional.of(url);
    }

    /**
     * Maps a WebAssembly byte offset to original source location.
     *
     * @param byteOffset The byte offset in the WebAssembly binary
     * @return Source location if mapping exists
     */
    public Optional<SourceLocation> mapToSource(final int byteOffset) {
        // For WebAssembly, line is always 1 and column is byte offset
        final List<Mapping> lineMappings = lineToMappings.get(1);
        if (lineMappings == null || lineMappings.isEmpty()) {
            return Optional.empty();
        }

        // Find the mapping with the highest generated column <= byteOffset
        Mapping bestMapping = null;
        for (final Mapping mapping : lineMappings) {
            if (mapping.generatedColumn <= byteOffset) {
                if (bestMapping == null || mapping.generatedColumn > bestMapping.generatedColumn) {
                    bestMapping = mapping;
                }
            }
        }

        if (bestMapping == null || bestMapping.sourceIndex == -1) {
            return Optional.empty();
        }

        final String sourceName = bestMapping.sourceIndex < sources.size() ?
            sources.get(bestMapping.sourceIndex) : null;
        final String symbolName = bestMapping.nameIndex != -1 && bestMapping.nameIndex < names.size() ?
            names.get(bestMapping.nameIndex) : null;

        return Optional.of(new SourceLocation(
            sourceName,
            bestMapping.sourceIndex,
            bestMapping.originalLine,
            bestMapping.originalColumn,
            symbolName,
            byteOffset
        ));
    }

    /**
     * Maps original source location to WebAssembly byte offset.
     *
     * @param sourceIndex Index of the source file
     * @param line Original source line (1-based)
     * @param column Original source column (0-based)
     * @return WebAssembly byte offset if mapping exists
     */
    public Optional<Integer> mapToGenerated(final int sourceIndex, final int line, final int column) {
        for (final Mapping mapping : parsedMappings) {
            if (mapping.sourceIndex == sourceIndex &&
                mapping.originalLine == line &&
                mapping.originalColumn == column) {
                return Optional.of(mapping.generatedColumn);
            }
        }
        return Optional.empty();
    }

    /**
     * Gets all source locations for a given WebAssembly function.
     *
     * @param functionStartOffset Start byte offset of the function
     * @param functionEndOffset End byte offset of the function
     * @return List of source locations within the function range
     */
    public List<SourceLocation> getSourceLocationsForFunction(final int functionStartOffset,
                                                               final int functionEndOffset) {
        final List<SourceLocation> locations = new ArrayList<>();
        final List<Mapping> lineMappings = lineToMappings.get(1);
        if (lineMappings == null) {
            return locations;
        }

        for (final Mapping mapping : lineMappings) {
            if (mapping.generatedColumn >= functionStartOffset &&
                mapping.generatedColumn < functionEndOffset &&
                mapping.sourceIndex != -1) {

                final String sourceName = mapping.sourceIndex < sources.size() ?
                    sources.get(mapping.sourceIndex) : null;
                final String symbolName = mapping.nameIndex != -1 && mapping.nameIndex < names.size() ?
                    names.get(mapping.nameIndex) : null;

                locations.add(new SourceLocation(
                    sourceName,
                    mapping.sourceIndex,
                    mapping.originalLine,
                    mapping.originalColumn,
                    symbolName,
                    mapping.generatedColumn
                ));
            }
        }

        return locations;
    }

    /**
     * Gets inline source content for a source file.
     *
     * @param sourceIndex Index of the source file
     * @return Source content if available
     */
    public Optional<String> getSourceContent(final int sourceIndex) {
        if (sourcesContent == null || sourceIndex < 0 || sourceIndex >= sourcesContent.size()) {
            return Optional.empty();
        }
        return Optional.ofNullable(sourcesContent.get(sourceIndex));
    }

    /**
     * Gets the source file name at the given index.
     *
     * @param sourceIndex Index of the source file
     * @return Source file name if valid index
     */
    public Optional<String> getSourceName(final int sourceIndex) {
        if (sourceIndex < 0 || sourceIndex >= sources.size()) {
            return Optional.empty();
        }
        return Optional.of(sources.get(sourceIndex));
    }

    /**
     * Validates the source map integrity.
     *
     * @return Validation result with any issues found
     */
    public ValidationResult validate() {
        final List<String> warnings = new ArrayList<>();
        final List<String> errors = new ArrayList<>();

        if (version != 3) {
            errors.add("Unsupported version: " + version);
        }

        if (sources.isEmpty()) {
            warnings.add("No source files specified");
        }

        if (sourcesContent != null && sourcesContent.size() != sources.size()) {
            warnings.add("Source content count does not match source count");
        }

        // Validate mappings
        for (int i = 0; i < parsedMappings.size(); i++) {
            final Mapping mapping = parsedMappings.get(i);

            if (mapping.sourceIndex != -1 && mapping.sourceIndex >= sources.size()) {
                errors.add("Invalid source index at mapping " + i + ": " + mapping.sourceIndex);
            }

            if (mapping.nameIndex != -1 && mapping.nameIndex >= names.size()) {
                errors.add("Invalid name index at mapping " + i + ": " + mapping.nameIndex);
            }

            if (mapping.originalLine < 1) {
                errors.add("Invalid original line at mapping " + i + ": " + mapping.originalLine);
            }

            if (mapping.originalColumn < 0) {
                errors.add("Invalid original column at mapping " + i + ": " + mapping.originalColumn);
            }
        }

        return new ValidationResult(errors, warnings);
    }

    // Getters
    public int getVersion() { return version; }
    public String getFile() { return file; }
    public String getSourceRoot() { return sourceRoot; }
    public List<String> getSources() { return sources; }
    public List<String> getSourcesContent() { return sourcesContent; }
    public List<String> getNames() { return names; }
    public String getMappings() { return mappings; }
    public List<Mapping> getParsedMappings() { return parsedMappings; }

    private static List<Mapping> parseMappings(final String mappings) {
        final List<Mapping> result = new ArrayList<>();
        final String[] lines = mappings.split(";");

        int generatedLine = 1;
        int generatedColumn = 0;
        int sourceIndex = 0;
        int originalLine = 1;
        int originalColumn = 0;
        int nameIndex = 0;

        for (final String line : lines) {
            if (line.isEmpty()) {
                generatedLine++;
                generatedColumn = 0;
                continue;
            }

            final String[] segments = line.split(",");

            for (final String segment : segments) {
                if (segment.isEmpty()) {
                    continue;
                }

                final int[] values = decodeVLQ(segment);
                if (values.length == 0) {
                    continue;
                }

                generatedColumn += values[0];

                if (values.length == 1) {
                    // Only generated column
                    result.add(new Mapping(generatedLine, generatedColumn, -1, -1, -1, -1));
                } else if (values.length >= 4) {
                    sourceIndex += values[1];
                    originalLine += values[2];
                    originalColumn += values[3];

                    final int currentNameIndex = values.length >= 5 ? nameIndex + values[4] : -1;
                    if (values.length >= 5) {
                        nameIndex += values[4];
                    }

                    result.add(new Mapping(generatedLine, generatedColumn, sourceIndex,
                                         originalLine, originalColumn, currentNameIndex));
                }
            }

            generatedLine++;
            generatedColumn = 0;
        }

        return result;
    }

    private static int[] decodeVLQ(final String encoded) {
        final List<Integer> values = new ArrayList<>();
        int index = 0;

        while (index < encoded.length()) {
            int value = 0;
            int shift = 0;
            int digit;

            do {
                if (index >= encoded.length()) {
                    break;
                }

                digit = decodeBase64Char(encoded.charAt(index++));
                if (digit == -1) {
                    return new int[0]; // Invalid character
                }

                value |= (digit & 31) << shift;
                shift += 5;
            } while ((digit & 32) != 0);

            // Convert from VLQ to signed integer
            final int decodedValue = (value & 1) == 1 ? -(value >> 1) : (value >> 1);
            values.add(decodedValue);
        }

        return values.stream().mapToInt(Integer::intValue).toArray();
    }

    private static int decodeBase64Char(final char c) {
        if (c >= 'A' && c <= 'Z') return c - 'A';
        if (c >= 'a' && c <= 'z') return c - 'a' + 26;
        if (c >= '0' && c <= '9') return c - '0' + 52;
        if (c == '+') return 62;
        if (c == '/') return 63;
        return -1;
    }

    private static Map<Integer, List<Mapping>> buildLineIndex(final List<Mapping> mappings) {
        final Map<Integer, List<Mapping>> index = new HashMap<>();
        for (final Mapping mapping : mappings) {
            index.computeIfAbsent(mapping.generatedLine, k -> new ArrayList<>()).add(mapping);
        }

        // Sort each line's mappings by generated column
        for (final List<Mapping> lineMappings : index.values()) {
            lineMappings.sort((a, b) -> Integer.compare(a.generatedColumn, b.generatedColumn));
        }

        return index;
    }

    /**
     * Source map mapping entry.
     */
    public static final class Mapping {
        public final int generatedLine;
        public final int generatedColumn;
        public final int sourceIndex;
        public final int originalLine;
        public final int originalColumn;
        public final int nameIndex;

        public Mapping(final int generatedLine, final int generatedColumn,
                       final int sourceIndex, final int originalLine,
                       final int originalColumn, final int nameIndex) {
            this.generatedLine = generatedLine;
            this.generatedColumn = generatedColumn;
            this.sourceIndex = sourceIndex;
            this.originalLine = originalLine;
            this.originalColumn = originalColumn;
            this.nameIndex = nameIndex;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (!(o instanceof Mapping)) return false;
            final Mapping mapping = (Mapping) o;
            return generatedLine == mapping.generatedLine &&
                   generatedColumn == mapping.generatedColumn &&
                   sourceIndex == mapping.sourceIndex &&
                   originalLine == mapping.originalLine &&
                   originalColumn == mapping.originalColumn &&
                   nameIndex == mapping.nameIndex;
        }

        @Override
        public int hashCode() {
            return Objects.hash(generatedLine, generatedColumn, sourceIndex,
                              originalLine, originalColumn, nameIndex);
        }

        @Override
        public String toString() {
            return String.format("Mapping{gen=%d:%d, src=%d:%d:%d, name=%d}",
                generatedLine, generatedColumn, sourceIndex, originalLine, originalColumn, nameIndex);
        }
    }

    /**
     * Source location information.
     */
    public static final class SourceLocation {
        private final String sourceName;
        private final int sourceIndex;
        private final int line;
        private final int column;
        private final String symbolName;
        private final int generatedByteOffset;

        public SourceLocation(final String sourceName, final int sourceIndex,
                              final int line, final int column, final String symbolName,
                              final int generatedByteOffset) {
            this.sourceName = sourceName;
            this.sourceIndex = sourceIndex;
            this.line = line;
            this.column = column;
            this.symbolName = symbolName;
            this.generatedByteOffset = generatedByteOffset;
        }

        public String getSourceName() { return sourceName; }
        public int getSourceIndex() { return sourceIndex; }
        public int getLine() { return line; }
        public int getColumn() { return column; }
        public String getSymbolName() { return symbolName; }
        public int getGeneratedByteOffset() { return generatedByteOffset; }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (!(o instanceof SourceLocation)) return false;
            final SourceLocation that = (SourceLocation) o;
            return sourceIndex == that.sourceIndex &&
                   line == that.line &&
                   column == that.column &&
                   generatedByteOffset == that.generatedByteOffset &&
                   Objects.equals(sourceName, that.sourceName) &&
                   Objects.equals(symbolName, that.symbolName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sourceName, sourceIndex, line, column, symbolName, generatedByteOffset);
        }

        @Override
        public String toString() {
            return String.format("SourceLocation{file='%s', line=%d, column=%d, symbol='%s', offset=%d}",
                sourceName, line, column, symbolName, generatedByteOffset);
        }
    }

    /**
     * Source map validation result.
     */
    public static final class ValidationResult {
        private final List<String> errors;
        private final List<String> warnings;

        public ValidationResult(final List<String> errors, final List<String> warnings) {
            this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
            this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
        }

        public boolean isValid() { return errors.isEmpty(); }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("ValidationResult{valid=").append(isValid());
            if (!errors.isEmpty()) {
                sb.append(", errors=").append(errors);
            }
            if (!warnings.isEmpty()) {
                sb.append(", warnings=").append(warnings);
            }
            sb.append('}');
            return sb.toString();
        }
    }

    /**
     * Exception thrown when source map parsing fails.
     */
    public static final class SourceMapParseException extends Exception {
        public SourceMapParseException(final String message) {
            super(message);
        }

        public SourceMapParseException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}