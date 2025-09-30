package ai.tegmentum.wasmtime4j.debug;

import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.DebugException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Comprehensive WebAssembly debugger with source mapping, breakpoints, execution control,
 * and variable inspection capabilities.
 *
 * <p>Features:
 * <ul>
 *   <li>Source map and DWARF debug information integration</li>
 *   <li>Breakpoint management with conditional support</li>
 *   <li>Single-step execution control (step over, into, out)</li>
 *   <li>Call stack inspection and unwinding</li>
 *   <li>Variable value inspection and modification</li>
 *   <li>Memory inspection at arbitrary addresses</li>
 *   <li>Execution tracing and profiling</li>
 *   <li>Asynchronous debugging events</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * WasmDebugger debugger = WasmDebugger.create(instance, module);
 * debugger.loadSourceMap(Path.of("module.wasm.map"));
 *
 * // Set breakpoint at source location
 * Breakpoint bp = debugger.setSourceBreakpoint("main.c", 42);
 *
 * // Add event listener
 * debugger.addEventListener(event -> {
 *     if (event instanceof BreakpointHitEvent) {
 *         System.out.println("Breakpoint hit at: " + event.getLocation());
 *     }
 * });
 *
 * // Start debugging
 * debugger.debugFunction("main");
 * }</pre>
 */
public final class WasmDebugger implements AutoCloseable {

    private final Instance instance;
    private final Module module;
    private final long nativeDebuggerHandle;
    private final Map<Integer, Breakpoint> breakpoints;
    private final Map<String, WatchExpression> watchExpressions;
    private final List<DebugEventListener> eventListeners;
    private final AtomicInteger nextBreakpointId;
    private final AtomicLong executionCounter;

    private SourceMap sourceMap;
    private DwarfDebugInfo dwarfInfo;
    private volatile DebugState state;
    private volatile ExecutionContext currentContext;

    private WasmDebugger(final Instance instance, final Module module, final long nativeHandle) {
        this.instance = Objects.requireNonNull(instance, "Instance cannot be null");
        this.module = Objects.requireNonNull(module, "Module cannot be null");
        this.nativeDebuggerHandle = nativeHandle;
        this.breakpoints = new ConcurrentHashMap<>();
        this.watchExpressions = new ConcurrentHashMap<>();
        this.eventListeners = new ArrayList<>();
        this.nextBreakpointId = new AtomicInteger(1);
        this.executionCounter = new AtomicLong(0);
        this.state = DebugState.READY;
    }

    /**
     * Creates a new WebAssembly debugger for the given instance.
     *
     * @param instance The WebAssembly instance to debug
     * @param module The WebAssembly module
     * @return New debugger instance
     * @throws DebugException if debugger creation fails
     */
    public static WasmDebugger create(final Instance instance, final Module module) throws DebugException {
        final long handle = createNativeDebugger(instance, module);
        if (handle == 0) {
            throw new DebugException("Failed to create native debugger");
        }

        return new WasmDebugger(instance, module, handle);
    }

    /**
     * Loads source map information for source-level debugging.
     *
     * @param sourceMapPath Path to the source map file
     * @throws DebugException if loading fails
     */
    public void loadSourceMap(final Path sourceMapPath) throws DebugException {
        try {
            this.sourceMap = SourceMap.fromFile(sourceMapPath);
            final SourceMap.ValidationResult validation = sourceMap.validate();
            if (!validation.isValid()) {
                throw new DebugException("Invalid source map: " + validation.getErrors());
            }
        } catch (final SourceMap.SourceMapParseException e) {
            throw new DebugException("Failed to load source map", e);
        }
    }

    /**
     * Loads source map from JSON content.
     *
     * @param sourceMapJson Source map JSON content
     * @throws DebugException if parsing fails
     */
    public void loadSourceMapFromJson(final String sourceMapJson) throws DebugException {
        try {
            this.sourceMap = SourceMap.fromJson(sourceMapJson);
            final SourceMap.ValidationResult validation = sourceMap.validate();
            if (!validation.isValid()) {
                throw new DebugException("Invalid source map: " + validation.getErrors());
            }
        } catch (final SourceMap.SourceMapParseException e) {
            throw new DebugException("Failed to parse source map", e);
        }
    }

    /**
     * Loads DWARF debug information from WebAssembly custom sections.
     *
     * @param customSections Map of custom section names to their binary data
     * @throws DebugException if loading fails
     */
    public void loadDwarfDebugInfo(final Map<String, byte[]> customSections) throws DebugException {
        try {
            final Optional<DwarfDebugInfo> dwarf = DwarfDebugInfo.fromCustomSections(customSections);
            if (dwarf.isPresent()) {
                this.dwarfInfo = dwarf.get();
                final DwarfDebugInfo.ValidationResult validation = dwarfInfo.validate();
                if (!validation.isValid()) {
                    throw new DebugException("Invalid DWARF debug info: " + validation.getErrors());
                }
            }
        } catch (final DwarfDebugInfo.DwarfParseException e) {
            throw new DebugException("Failed to load DWARF debug information", e);
        }
    }

    /**
     * Sets a breakpoint at a WebAssembly byte offset.
     *
     * @param byteOffset Byte offset in the WebAssembly code section
     * @return Created breakpoint
     */
    public Breakpoint setByteOffsetBreakpoint(final int byteOffset) {
        return setByteOffsetBreakpoint(byteOffset, null);
    }

    /**
     * Sets a conditional breakpoint at a WebAssembly byte offset.
     *
     * @param byteOffset Byte offset in the WebAssembly code section
     * @param condition Breakpoint condition expression (null for unconditional)
     * @return Created breakpoint
     */
    public Breakpoint setByteOffsetBreakpoint(final int byteOffset, final String condition) {
        final int id = nextBreakpointId.getAndIncrement();
        final Breakpoint breakpoint = new Breakpoint(id, BreakpointType.BYTE_OFFSET,
                                                    null, -1, -1, byteOffset, condition, true, 0);

        breakpoints.put(id, breakpoint);
        setNativeBreakpoint(nativeDebuggerHandle, id, byteOffset, condition);

        notifyEventListeners(new BreakpointSetEvent(breakpoint));
        return breakpoint;
    }

    /**
     * Sets a breakpoint at a source file location (requires source map).
     *
     * @param sourceFile Source file name
     * @param line Line number (1-based)
     * @return Created breakpoint
     * @throws DebugException if source map is not loaded or mapping fails
     */
    public Breakpoint setSourceBreakpoint(final String sourceFile, final int line) throws DebugException {
        return setSourceBreakpoint(sourceFile, line, 0, null);
    }

    /**
     * Sets a conditional breakpoint at a source file location.
     *
     * @param sourceFile Source file name
     * @param line Line number (1-based)
     * @param column Column number (0-based)
     * @param condition Breakpoint condition expression (null for unconditional)
     * @return Created breakpoint
     * @throws DebugException if source map is not loaded or mapping fails
     */
    public Breakpoint setSourceBreakpoint(final String sourceFile, final int line,
                                         final int column, final String condition) throws DebugException {
        if (sourceMap == null) {
            throw new DebugException("Source map not loaded - cannot set source breakpoints");
        }

        // Find source index
        final List<String> sources = sourceMap.getSources();
        int sourceIndex = -1;
        for (int i = 0; i < sources.size(); i++) {
            if (sources.get(i).equals(sourceFile) || sources.get(i).endsWith("/" + sourceFile)) {
                sourceIndex = i;
                break;
            }
        }

        if (sourceIndex == -1) {
            throw new DebugException("Source file not found in source map: " + sourceFile);
        }

        // Map to generated byte offset
        final Optional<Integer> byteOffset = sourceMap.mapToGenerated(sourceIndex, line, column);
        if (!byteOffset.isPresent()) {
            throw new DebugException("No mapping found for source location: " +
                                   sourceFile + ":" + line + ":" + column);
        }

        final int id = nextBreakpointId.getAndIncrement();
        final Breakpoint breakpoint = new Breakpoint(id, BreakpointType.SOURCE_LOCATION,
                                                    sourceFile, line, column, byteOffset.get(),
                                                    condition, true, 0);

        breakpoints.put(id, breakpoint);
        setNativeBreakpoint(nativeDebuggerHandle, id, byteOffset.get(), condition);

        notifyEventListeners(new BreakpointSetEvent(breakpoint));
        return breakpoint;
    }

    /**
     * Removes a breakpoint.
     *
     * @param breakpointId Breakpoint ID to remove
     * @return true if breakpoint was removed, false if not found
     */
    public boolean removeBreakpoint(final int breakpointId) {
        final Breakpoint breakpoint = breakpoints.remove(breakpointId);
        if (breakpoint != null) {
            removeNativeBreakpoint(nativeDebuggerHandle, breakpointId);
            notifyEventListeners(new BreakpointRemovedEvent(breakpoint));
            return true;
        }
        return false;
    }

    /**
     * Enables or disables a breakpoint.
     *
     * @param breakpointId Breakpoint ID
     * @param enabled true to enable, false to disable
     */
    public void setBreakpointEnabled(final int breakpointId, final boolean enabled) {
        final Breakpoint breakpoint = breakpoints.get(breakpointId);
        if (breakpoint != null) {
            final Breakpoint updated = breakpoint.withEnabled(enabled);
            breakpoints.put(breakpointId, updated);
            setNativeBreakpointEnabled(nativeDebuggerHandle, breakpointId, enabled);
            notifyEventListeners(new BreakpointChangedEvent(updated));
        }
    }

    /**
     * Adds a watch expression for variable monitoring.
     *
     * @param name Watch expression name
     * @param expression Expression to evaluate
     * @return Created watch expression
     */
    public WatchExpression addWatchExpression(final String name, final String expression) {
        final WatchExpression watch = new WatchExpression(name, expression, true);
        watchExpressions.put(name, watch);
        addNativeWatchExpression(nativeDebuggerHandle, name, expression);
        notifyEventListeners(new WatchExpressionAddedEvent(watch));
        return watch;
    }

    /**
     * Removes a watch expression.
     *
     * @param name Watch expression name
     * @return true if removed, false if not found
     */
    public boolean removeWatchExpression(final String name) {
        final WatchExpression watch = watchExpressions.remove(name);
        if (watch != null) {
            removeNativeWatchExpression(nativeDebuggerHandle, name);
            notifyEventListeners(new WatchExpressionRemovedEvent(watch));
            return true;
        }
        return false;
    }

    /**
     * Starts debugging execution of a function.
     *
     * @param functionName Function name to debug
     * @param arguments Function arguments
     * @return Future for the debug execution result
     */
    public CompletableFuture<DebugResult> debugFunction(final String functionName,
                                                        final WasmValue... arguments) {
        if (state != DebugState.READY) {
            return CompletableFuture.completedFuture(
                DebugResult.failed("Debugger not ready: " + state));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                state = DebugState.RUNNING;
                executionCounter.incrementAndGet();
                notifyEventListeners(new ExecutionStartedEvent(functionName));

                final DebugResult result = executeWithDebugging(functionName, arguments);

                state = DebugState.READY;
                notifyEventListeners(new ExecutionFinishedEvent(result));

                return result;
            } catch (final Exception e) {
                state = DebugState.ERROR;
                final DebugResult result = DebugResult.failed("Execution failed: " + e.getMessage());
                notifyEventListeners(new ExecutionFinishedEvent(result));
                return result;
            }
        });
    }

    /**
     * Steps to the next instruction.
     *
     * @return Execution context after stepping
     * @throws DebugException if not in paused state
     */
    public ExecutionContext stepNext() throws DebugException {
        if (state != DebugState.PAUSED) {
            throw new DebugException("Cannot step when not paused (current state: " + state + ")");
        }

        currentContext = stepNextNative(nativeDebuggerHandle);
        notifyEventListeners(new StepCompletedEvent(currentContext));
        return currentContext;
    }

    /**
     * Steps into a function call.
     *
     * @return Execution context after stepping
     * @throws DebugException if not in paused state
     */
    public ExecutionContext stepInto() throws DebugException {
        if (state != DebugState.PAUSED) {
            throw new DebugException("Cannot step when not paused (current state: " + state + ")");
        }

        currentContext = stepIntoNative(nativeDebuggerHandle);
        notifyEventListeners(new StepCompletedEvent(currentContext));
        return currentContext;
    }

    /**
     * Steps out of the current function.
     *
     * @return Execution context after stepping
     * @throws DebugException if not in paused state
     */
    public ExecutionContext stepOut() throws DebugException {
        if (state != DebugState.PAUSED) {
            throw new DebugException("Cannot step when not paused (current state: " + state + ")");
        }

        currentContext = stepOutNative(nativeDebuggerHandle);
        notifyEventListeners(new StepCompletedEvent(currentContext));
        return currentContext;
    }

    /**
     * Continues execution from a paused state.
     */
    public void continueExecution() throws DebugException {
        if (state != DebugState.PAUSED) {
            throw new DebugException("Cannot continue when not paused (current state: " + state + ")");
        }

        state = DebugState.RUNNING;
        continueNative(nativeDebuggerHandle);
        notifyEventListeners(new ExecutionContinuedEvent());
    }

    /**
     * Pauses execution if currently running.
     *
     * @return Current execution context
     */
    public ExecutionContext pauseExecution() throws DebugException {
        if (state != DebugState.RUNNING) {
            throw new DebugException("Cannot pause when not running (current state: " + state + ")");
        }

        state = DebugState.PAUSED;
        currentContext = pauseNative(nativeDebuggerHandle);
        notifyEventListeners(new ExecutionPausedEvent(currentContext));
        return currentContext;
    }

    /**
     * Gets the current call stack.
     *
     * @return List of stack frames
     */
    public List<StackFrame> getCallStack() {
        if (state != DebugState.PAUSED) {
            return Collections.emptyList();
        }

        final List<StackFrame> nativeStack = getCallStackNative(nativeDebuggerHandle);
        return enhanceStackFramesWithDebugInfo(nativeStack);
    }

    /**
     * Inspects memory at the given address.
     *
     * @param address Memory address
     * @param length Number of bytes to read
     * @return Memory inspection result
     */
    public MemoryInspection inspectMemory(final long address, final int length) {
        final byte[] data = readMemoryNative(nativeDebuggerHandle, address, length);
        return new MemoryInspection(address, data);
    }

    /**
     * Gets current variable values in scope.
     *
     * @return Map of variable names to values
     */
    public Map<String, VariableValue> getVariables() {
        if (state != DebugState.PAUSED) {
            return Collections.emptyMap();
        }

        final Map<String, VariableValue> nativeVars = getVariablesNative(nativeDebuggerHandle);
        return enhanceVariablesWithDebugInfo(nativeVars);
    }

    /**
     * Evaluates a watch expression in the current context.
     *
     * @param expression Expression to evaluate
     * @return Evaluation result
     */
    public WatchEvaluationResult evaluateWatch(final String expression) {
        if (state != DebugState.PAUSED) {
            return WatchEvaluationResult.failed("Not paused");
        }

        return evaluateWatchNative(nativeDebuggerHandle, expression);
    }

    /**
     * Gets source location for the current execution position.
     *
     * @return Source location if available
     */
    public Optional<SourceLocation> getCurrentSourceLocation() {
        if (currentContext == null) {
            return Optional.empty();
        }

        return getSourceLocation(currentContext.getByteOffset());
    }

    /**
     * Gets source location for a given byte offset.
     *
     * @param byteOffset Byte offset in WebAssembly code
     * @return Source location if available
     */
    public Optional<SourceLocation> getSourceLocation(final int byteOffset) {
        if (sourceMap != null) {
            final Optional<SourceMap.SourceLocation> sourceMapLocation = sourceMap.mapToSource(byteOffset);
            if (sourceMapLocation.isPresent()) {
                final SourceMap.SourceLocation loc = sourceMapLocation.get();
                return Optional.of(new SourceLocation(loc.getSourceName(), loc.getLine(),
                                                    loc.getColumn(), loc.getSymbolName(), byteOffset));
            }
        }

        if (dwarfInfo != null) {
            final Optional<DwarfDebugInfo.DebugLineInfo> dwarfLine = dwarfInfo.getLineInfo(byteOffset);
            if (dwarfLine.isPresent()) {
                final DwarfDebugInfo.DebugLineInfo info = dwarfLine.get();
                return Optional.of(new SourceLocation(info.fileName, info.line,
                                                    info.column, null, byteOffset));
            }
        }

        return Optional.empty();
    }

    /**
     * Gets all available source files.
     *
     * @return List of source file names
     */
    public List<String> getSourceFiles() {
        final List<String> files = new ArrayList<>();

        if (sourceMap != null) {
            files.addAll(sourceMap.getSources());
        }

        if (dwarfInfo != null) {
            files.addAll(dwarfInfo.getSourceFiles());
        }

        return files;
    }

    /**
     * Adds a debug event listener.
     *
     * @param listener Event listener to add
     */
    public void addEventListener(final DebugEventListener listener) {
        if (listener != null) {
            synchronized (eventListeners) {
                eventListeners.add(listener);
            }
        }
    }

    /**
     * Removes a debug event listener.
     *
     * @param listener Event listener to remove
     */
    public void removeEventListener(final DebugEventListener listener) {
        synchronized (eventListeners) {
            eventListeners.remove(listener);
        }
    }

    /**
     * Gets all active breakpoints.
     *
     * @return List of breakpoints
     */
    public List<Breakpoint> getBreakpoints() {
        return new ArrayList<>(breakpoints.values());
    }

    /**
     * Gets all watch expressions.
     *
     * @return List of watch expressions
     */
    public List<WatchExpression> getWatchExpressions() {
        return new ArrayList<>(watchExpressions.values());
    }

    /**
     * Gets current debug state.
     *
     * @return Current debug state
     */
    public DebugState getState() {
        return state;
    }

    /**
     * Gets current execution context.
     *
     * @return Current execution context, null if not debugging
     */
    public ExecutionContext getCurrentContext() {
        return currentContext;
    }

    /**
     * Gets execution statistics.
     *
     * @return Execution statistics
     */
    public ExecutionStatistics getExecutionStatistics() {
        return new ExecutionStatistics(executionCounter.get(), breakpoints.size(),
                                      watchExpressions.size(), eventListeners.size());
    }

    @Override
    public void close() {
        if (state == DebugState.RUNNING || state == DebugState.PAUSED) {
            try {
                stopDebugging();
            } catch (final Exception e) {
                // Log but continue cleanup
            }
        }

        breakpoints.clear();
        watchExpressions.clear();
        eventListeners.clear();

        if (nativeDebuggerHandle != 0) {
            destroyNativeDebugger(nativeDebuggerHandle);
        }
    }

    /**
     * Stops the current debugging session.
     */
    public void stopDebugging() {
        if (state == DebugState.RUNNING || state == DebugState.PAUSED) {
            stopNative(nativeDebuggerHandle);
            state = DebugState.READY;
            currentContext = null;
            notifyEventListeners(new ExecutionStoppedEvent());
        }
    }

    private List<StackFrame> enhanceStackFramesWithDebugInfo(final List<StackFrame> nativeFrames) {
        final List<StackFrame> enhanced = new ArrayList<>();

        for (final StackFrame frame : nativeFrames) {
            final Optional<SourceLocation> sourceLocation = getSourceLocation(frame.getByteOffset());
            final StackFrame enhancedFrame = new StackFrame(
                frame.getFunctionIndex(),
                frame.getFunctionName(),
                frame.getByteOffset(),
                frame.getVariables(),
                sourceLocation.orElse(null)
            );
            enhanced.add(enhancedFrame);
        }

        return enhanced;
    }

    private Map<String, VariableValue> enhanceVariablesWithDebugInfo(final Map<String, VariableValue> nativeVars) {
        // TODO: Enhance with DWARF type information
        return new HashMap<>(nativeVars);
    }

    private void notifyEventListeners(final DebugEvent event) {
        final List<DebugEventListener> currentListeners;
        synchronized (eventListeners) {
            currentListeners = new ArrayList<>(eventListeners);
        }

        for (final DebugEventListener listener : currentListeners) {
            try {
                listener.onDebugEvent(event);
            } catch (final Exception e) {
                // Log but continue with other listeners
                System.err.println("Debug event listener failed: " + e.getMessage());
            }
        }
    }

    private DebugResult executeWithDebugging(final String functionName, final WasmValue[] arguments) {
        return executeWithDebuggingNative(nativeDebuggerHandle, functionName, arguments);
    }

    // Native method declarations
    private static native long createNativeDebugger(Instance instance, Module module);
    private static native void destroyNativeDebugger(long handle);
    private static native void setNativeBreakpoint(long handle, int id, int byteOffset, String condition);
    private static native void removeNativeBreakpoint(long handle, int id);
    private static native void setNativeBreakpointEnabled(long handle, int id, boolean enabled);
    private static native void addNativeWatchExpression(long handle, String name, String expression);
    private static native void removeNativeWatchExpression(long handle, String name);
    private static native ExecutionContext stepNextNative(long handle);
    private static native ExecutionContext stepIntoNative(long handle);
    private static native ExecutionContext stepOutNative(long handle);
    private static native void continueNative(long handle);
    private static native ExecutionContext pauseNative(long handle);
    private static native void stopNative(long handle);
    private static native List<StackFrame> getCallStackNative(long handle);
    private static native byte[] readMemoryNative(long handle, long address, int length);
    private static native Map<String, VariableValue> getVariablesNative(long handle);
    private static native WatchEvaluationResult evaluateWatchNative(long handle, String expression);
    private static native DebugResult executeWithDebuggingNative(long handle, String functionName,
                                                                WasmValue[] arguments);

    // Enums and data classes follow...

    /**
     * Debugger state enumeration.
     */
    public enum DebugState {
        READY,
        RUNNING,
        PAUSED,
        ERROR
    }

    /**
     * Breakpoint type enumeration.
     */
    public enum BreakpointType {
        BYTE_OFFSET,
        SOURCE_LOCATION,
        FUNCTION_ENTRY
    }

    /**
     * Debug event listener interface.
     */
    public interface DebugEventListener {
        void onDebugEvent(DebugEvent event);
    }

    // Additional classes will be defined in separate files for better organization
}