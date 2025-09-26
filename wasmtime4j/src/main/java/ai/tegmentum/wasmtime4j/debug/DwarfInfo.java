package ai.tegmentum.wasmtime4j.debug;

import java.io.Closeable;
import java.util.List;
import java.util.Optional;

/**
 * DWARF debugging information extracted from WebAssembly modules.
 *
 * <p>This interface provides access to DWARF debugging information embedded in
 * WebAssembly custom sections. DWARF information includes compilation units,
 * line number programs, function symbols, and type information.
 *
 * <p>DWARF support enables:
 * <ul>
 * <li>Function name resolution</li>
 * <li>Variable and parameter information</li>
 * <li>Type information</li>
 * <li>Line number mapping</li>
 * <li>Compilation unit metadata</li>
 * </ul>
 *
 * @since 1.0.0
 */
public interface DwarfInfo extends Closeable {

    /**
     * Gets the compilation units found in the DWARF information.
     *
     * @return list of compilation units
     */
    List<CompilationUnit> getCompilationUnits();

    /**
     * Gets line number information for mapping addresses to source lines.
     *
     * @return list of line number programs
     */
    List<LineProgram> getLinePrograms();

    /**
     * Gets function information extracted from DWARF.
     *
     * @return list of function symbols
     */
    List<DwarfFunction> getFunctions();

    /**
     * Gets type information extracted from DWARF.
     *
     * @return list of type definitions
     */
    List<DwarfType> getTypes();

    /**
     * Looks up function information by address.
     *
     * @param address the function address
     * @return function information if found
     */
    Optional<DwarfFunction> getFunctionByAddress(final long address);

    /**
     * Looks up line information by address.
     *
     * @param address the instruction address
     * @return line information if found
     */
    Optional<LineInfo> getLineByAddress(final long address);

    /**
     * Gets the producer information (compiler, version, etc.).
     *
     * @return producer information if available
     */
    Optional<String> getProducer();

    /**
     * Gets the source language information.
     *
     * @return language code if available (DW_LANG_* constants)
     */
    Optional<Integer> getLanguage();

    /**
     * Validates the DWARF information structure.
     *
     * @return validation result
     */
    ValidationResult validate();

    /**
     * Gets metadata about the DWARF information.
     *
     * @return DWARF metadata
     */
    DwarfMetadata getMetadata();

    /**
     * Checks if the DWARF information is still valid.
     *
     * @return true if valid and usable
     */
    boolean isValid();

    /**
     * Closes and releases resources associated with this DWARF information.
     */
    @Override
    void close();

    /**
     * Represents a DWARF compilation unit.
     */
    interface CompilationUnit {
        /**
         * Gets the compilation unit offset.
         *
         * @return the unit offset
         */
        long getOffset();

        /**
         * Gets the producer information.
         *
         * @return producer string if available
         */
        Optional<String> getProducer();

        /**
         * Gets the source language.
         *
         * @return language code if available
         */
        Optional<Integer> getLanguage();

        /**
         * Gets the compilation directory.
         *
         * @return compilation directory if available
         */
        Optional<String> getCompilationDirectory();

        /**
         * Gets the main source file name.
         *
         * @return source file name if available
         */
        Optional<String> getName();

        /**
         * Gets the low PC (start address).
         *
         * @return start address if available
         */
        Optional<Long> getLowPc();

        /**
         * Gets the high PC (end address or size).
         *
         * @return end address or size if available
         */
        Optional<Long> getHighPc();
    }

    /**
     * Represents a DWARF line number program.
     */
    interface LineProgram {
        /**
         * Gets the file entries in this line program.
         *
         * @return list of file entries
         */
        List<FileEntry> getFiles();

        /**
         * Gets the directory entries.
         *
         * @return list of directories
         */
        List<String> getDirectories();

        /**
         * Gets the line number entries.
         *
         * @return list of line entries
         */
        List<LineEntry> getLines();

        /**
         * Looks up line information by address.
         *
         * @param address the address to look up
         * @return line information if found
         */
        Optional<LineInfo> getLineByAddress(final long address);
    }

    /**
     * Represents a file entry in a line program.
     */
    interface FileEntry {
        /**
         * Gets the file name.
         *
         * @return the file name
         */
        String getName();

        /**
         * Gets the directory index.
         *
         * @return the directory index
         */
        int getDirectoryIndex();

        /**
         * Gets the modification time.
         *
         * @return modification time if available
         */
        Optional<Long> getModificationTime();

        /**
         * Gets the file size.
         *
         * @return file size if available
         */
        Optional<Long> getSize();

        /**
         * Gets the full path by combining directory and file name.
         *
         * @param directories the directory list
         * @return the full path
         */
        String getFullPath(final List<String> directories);
    }

    /**
     * Represents a line entry in a line program.
     */
    interface LineEntry {
        /**
         * Gets the address.
         *
         * @return the address
         */
        long getAddress();

        /**
         * Gets the file index.
         *
         * @return the file index
         */
        int getFileIndex();

        /**
         * Gets the line number.
         *
         * @return the line number
         */
        int getLine();

        /**
         * Gets the column number.
         *
         * @return the column number
         */
        int getColumn();

        /**
         * Checks if this is a statement.
         *
         * @return true if this is a statement
         */
        boolean isStatement();

        /**
         * Checks if this is a basic block boundary.
         *
         * @return true if this is a basic block boundary
         */
        boolean isBasicBlock();

        /**
         * Checks if this is an end sequence marker.
         *
         * @return true if this is an end sequence marker
         */
        boolean isEndSequence();
    }

    /**
     * Combined line information from DWARF.
     */
    interface LineInfo {
        /**
         * Gets the source file path.
         *
         * @return the source file path
         */
        String getSourceFile();

        /**
         * Gets the line number.
         *
         * @return the line number
         */
        int getLine();

        /**
         * Gets the column number.
         *
         * @return the column number
         */
        int getColumn();

        /**
         * Checks if this is a statement.
         *
         * @return true if this is a statement
         */
        boolean isStatement();
    }

    /**
     * Metadata about DWARF information.
     */
    interface DwarfMetadata {
        /**
         * Gets the number of compilation units.
         *
         * @return compilation unit count
         */
        int getCompilationUnitCount();

        /**
         * Gets the number of functions.
         *
         * @return function count
         */
        int getFunctionCount();

        /**
         * Gets the number of line entries.
         *
         * @return line entry count
         */
        int getLineEntryCount();

        /**
         * Gets the number of type entries.
         *
         * @return type entry count
         */
        int getTypeCount();

        /**
         * Gets the creation time.
         *
         * @return creation time in milliseconds since epoch
         */
        long getCreationTime();

        /**
         * Estimates memory usage.
         *
         * @return estimated memory usage in bytes
         */
        long estimateMemoryUsage();
    }
}