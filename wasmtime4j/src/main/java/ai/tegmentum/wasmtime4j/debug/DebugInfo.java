package ai.tegmentum.wasmtime4j.debug;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Debug information for a WebAssembly instance.
 *
 * <p>Contains metadata about debuggable instances, their modules,
 * and available debugging features.
 *
 * @since 1.0.0
 */
public final class DebugInfo {

    private final String instanceId;
    private final String moduleName;
    private final boolean hasSourceMap;
    private final boolean hasDwarfInfo;
    private final List<String> availableFunctions;
    private final List<String> sourceFiles;
    private final int breakpointCount;
    private final boolean isDebuggable;
    private final String version;

    /**
     * Creates debug information.
     *
     * @param instanceId instance identifier
     * @param moduleName module name
     * @param hasSourceMap whether source map is available
     * @param hasDwarfInfo whether DWARF info is available
     * @param availableFunctions list of debuggable functions
     * @param sourceFiles list of source files
     * @param breakpointCount number of active breakpoints
     * @param isDebuggable whether instance is debuggable
     * @param version debug info version
     */
    public DebugInfo(final String instanceId, final String moduleName,
                    final boolean hasSourceMap, final boolean hasDwarfInfo,
                    final List<String> availableFunctions, final List<String> sourceFiles,
                    final int breakpointCount, final boolean isDebuggable,
                    final String version) {
        this.instanceId = Objects.requireNonNull(instanceId, "instanceId cannot be null");
        this.moduleName = moduleName;
        this.hasSourceMap = hasSourceMap;
        this.hasDwarfInfo = hasDwarfInfo;
        this.availableFunctions = availableFunctions != null ?
                Collections.unmodifiableList(availableFunctions) : Collections.emptyList();
        this.sourceFiles = sourceFiles != null ?
                Collections.unmodifiableList(sourceFiles) : Collections.emptyList();
        this.breakpointCount = breakpointCount;
        this.isDebuggable = isDebuggable;
        this.version = Objects.requireNonNull(version, "version cannot be null");
    }

    /**
     * Creates basic debug information.
     *
     * @param instanceId instance ID
     * @param moduleName module name
     * @return basic debug info
     */
    public static DebugInfo basic(final String instanceId, final String moduleName) {
        return new DebugInfo(instanceId, moduleName, false, false,
                Collections.emptyList(), Collections.emptyList(), 0, true, "1.0.0");
    }

    /**
     * Gets the instance ID.
     *
     * @return instance ID
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Gets the module name.
     *
     * @return module name or null
     */
    public String getModuleName() {
        return moduleName;
    }

    /**
     * Checks if source map is available.
     *
     * @return true if source map is available
     */
    public boolean hasSourceMap() {
        return hasSourceMap;
    }

    /**
     * Checks if DWARF debug info is available.
     *
     * @return true if DWARF info is available
     */
    public boolean hasDwarfInfo() {
        return hasDwarfInfo;
    }

    /**
     * Gets available debuggable functions.
     *
     * @return list of function names
     */
    public List<String> getAvailableFunctions() {
        return availableFunctions;
    }

    /**
     * Gets available source files.
     *
     * @return list of source files
     */
    public List<String> getSourceFiles() {
        return sourceFiles;
    }

    /**
     * Gets the number of active breakpoints.
     *
     * @return breakpoint count
     */
    public int getBreakpointCount() {
        return breakpointCount;
    }

    /**
     * Checks if the instance is debuggable.
     *
     * @return true if debuggable
     */
    public boolean isDebuggable() {
        return isDebuggable;
    }

    /**
     * Gets the debug info version.
     *
     * @return version string
     */
    public String getVersion() {
        return version;
    }

    /**
     * Checks if source-level debugging is supported.
     *
     * @return true if source debugging is supported
     */
    public boolean supportsSourceDebugging() {
        return hasSourceMap || hasDwarfInfo;
    }

    /**
     * Gets the number of available functions.
     *
     * @return function count
     */
    public int getFunctionCount() {
        return availableFunctions.size();
    }

    /**
     * Gets the number of source files.
     *
     * @return source file count
     */
    public int getSourceFileCount() {
        return sourceFiles.size();
    }

    /**
     * Checks if a function is available for debugging.
     *
     * @param functionName function name
     * @return true if function is available
     */
    public boolean hasFunctionDebugInfo(final String functionName) {
        return availableFunctions.contains(functionName);
    }

    /**
     * Creates a copy with updated breakpoint count.
     *
     * @param newCount new breakpoint count
     * @return updated debug info
     */
    public DebugInfo withBreakpointCount(final int newCount) {
        return new DebugInfo(instanceId, moduleName, hasSourceMap, hasDwarfInfo,
                availableFunctions, sourceFiles, newCount, isDebuggable, version);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final DebugInfo other = (DebugInfo) obj;
        return hasSourceMap == other.hasSourceMap &&
                hasDwarfInfo == other.hasDwarfInfo &&
                breakpointCount == other.breakpointCount &&
                isDebuggable == other.isDebuggable &&
                Objects.equals(instanceId, other.instanceId) &&
                Objects.equals(moduleName, other.moduleName) &&
                Objects.equals(availableFunctions, other.availableFunctions) &&
                Objects.equals(sourceFiles, other.sourceFiles) &&
                Objects.equals(version, other.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instanceId, moduleName, hasSourceMap, hasDwarfInfo,
                availableFunctions, sourceFiles, breakpointCount, isDebuggable, version);
    }

    @Override
    public String toString() {
        return "DebugInfo{" +
                "instanceId='" + instanceId + '\'' +
                ", moduleName='" + moduleName + '\'' +
                ", sourceMap=" + hasSourceMap +
                ", dwarfInfo=" + hasDwarfInfo +
                ", functions=" + availableFunctions.size() +
                ", sourceFiles=" + sourceFiles.size() +
                ", breakpoints=" + breakpointCount +
                ", debuggable=" + isDebuggable +
                ", version='" + version + '\'' +
                '}';
    }
}