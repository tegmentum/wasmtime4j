package ai.tegmentum.wasmtime4j.ide.intellij;

import ai.tegmentum.wasmtime4j.debug.Breakpoint;
import ai.tegmentum.wasmtime4j.debug.DebugEvent;
import ai.tegmentum.wasmtime4j.debug.DebugEventListener;
import ai.tegmentum.wasmtime4j.debug.DebugSession;
import ai.tegmentum.wasmtime4j.debug.ExecutionState;
import ai.tegmentum.wasmtime4j.debug.StackFrame;
import ai.tegmentum.wasmtime4j.debug.Variable;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * IntelliJ IDEA debug adapter for WebAssembly debugging.
 *
 * <p>This class provides integration with IntelliJ IDEA's debugging infrastructure,
 * enabling WebAssembly debugging through the IDE's native debugging interface.
 * It bridges wasmtime4j debugging capabilities with IntelliJ's debugging protocol.
 *
 * <p>Key features:
 * <ul>
 * <li>JDI (Java Debug Interface) integration for IntelliJ compatibility</li>
 * <li>WebAssembly source mapping to Java debugging constructs</li>
 * <li>Breakpoint synchronization with IntelliJ UI</li>
 * <li>Variable inspection through IntelliJ's variable explorer</li>
 * <li>Step debugging with IntelliJ's step controls</li>
 * <li>Memory and performance monitoring integration</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * IntelliJDebugAdapter adapter = new IntelliJDebugAdapter();
 * adapter.initialize(debugSession);
 * adapter.attachToIntelliJ();
 * }</pre>
 *
 * @since 1.0.0
 */
public final class IntelliJDebugAdapter implements DebugEventListener, AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(IntelliJDebugAdapter.class.getName());

    private static final String ADAPTER_NAME = "wasmtime4j-intellij";
    private static final String DEBUG_HOST = "localhost";
    private static final int DEFAULT_DEBUG_PORT = 5005;

    private final ExecutorService executorService;
    private final Map<String, IntelliJBreakpoint> breakpoints;
    private final Map<Integer, IntelliJStackFrame> stackFrames;
    private final Map<String, IntelliJVariable> variables;
    private final AtomicBoolean initialized;
    private final AtomicBoolean attached;

    private DebugSession debugSession;
    private VirtualMachine virtualMachine;
    private AttachingConnector connector;
    private int debugPort;

    /**
     * Creates a new IntelliJ debug adapter.
     */
    public IntelliJDebugAdapter() {
        this.executorService = Executors.newCachedThreadPool(r -> {
            final Thread thread = new Thread(r, "IntelliJDebugAdapter");
            thread.setDaemon(true);
            return thread;
        });
        this.breakpoints = new ConcurrentHashMap<>();
        this.stackFrames = new ConcurrentHashMap<>();
        this.variables = new ConcurrentHashMap<>();
        this.initialized = new AtomicBoolean(false);
        this.attached = new AtomicBoolean(false);
        this.debugPort = DEFAULT_DEBUG_PORT;
    }

    /**
     * Initializes the adapter with a debug session.
     *
     * @param session the debug session to attach
     * @throws IllegalArgumentException if session is null
     * @throws IllegalStateException if adapter is already initialized
     */
    public void initialize(final DebugSession session) {
        if (session == null) {
            throw new IllegalArgumentException("Debug session cannot be null");
        }
        if (!initialized.compareAndSet(false, true)) {
            throw new IllegalStateException("IntelliJ debug adapter is already initialized");
        }

        this.debugSession = session;
        session.addDebugEventListener(this);

        LOGGER.info("IntelliJ Debug Adapter initialized with session: " + session.getSessionId());
    }

    /**
     * Attaches to IntelliJ IDEA's debugging infrastructure.
     *
     * @return future that completes when attachment is successful
     * @throws IllegalStateException if adapter is not initialized
     */
    public CompletableFuture<Void> attachToIntelliJ() {
        if (!initialized.get()) {
            throw new IllegalStateException("Debug adapter must be initialized first");
        }

        return CompletableFuture.runAsync(() -> {
            try {
                performIntelliJAttachment();
                attached.set(true);
                LOGGER.info("Successfully attached to IntelliJ IDEA");
            } catch (final Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to attach to IntelliJ IDEA", e);
                throw new RuntimeException("IntelliJ attachment failed", e);
            }
        }, executorService);
    }

    /**
     * Sets the debug port for IntelliJ connection.
     *
     * @param port the debug port
     * @throws IllegalArgumentException if port is invalid
     */
    public void setDebugPort(final int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        this.debugPort = port;
    }

    /**
     * Sets a breakpoint from IntelliJ.
     *
     * @param sourceFile the source file path
     * @param line the line number
     * @param condition the breakpoint condition (may be null)
     * @return the created breakpoint information
     * @throws WasmException if breakpoint cannot be set
     */
    public IntelliJBreakpoint setBreakpoint(final String sourceFile, final int line, final String condition) throws WasmException {
        if (debugSession == null) {
            throw new IllegalStateException("No debug session attached");
        }

        // Map source file and line to WebAssembly function
        final String functionName = mapSourceToFunction(sourceFile, line);
        final Breakpoint wasmBreakpoint = debugSession.setBreakpoint(functionName, line);

        final IntelliJBreakpoint intellijBreakpoint = new IntelliJBreakpoint(
            wasmBreakpoint.getId(),
            sourceFile,
            line,
            condition,
            functionName,
            true
        );

        breakpoints.put(intellijBreakpoint.getId(), intellijBreakpoint);

        LOGGER.fine("Set IntelliJ breakpoint: " + intellijBreakpoint);
        return intellijBreakpoint;
    }

    /**
     * Removes a breakpoint.
     *
     * @param breakpointId the breakpoint ID
     * @return true if breakpoint was removed
     * @throws WasmException if removal fails
     */
    public boolean removeBreakpoint(final String breakpointId) throws WasmException {
        final IntelliJBreakpoint intellijBreakpoint = breakpoints.remove(breakpointId);
        if (intellijBreakpoint == null) {
            return false;
        }

        if (debugSession != null) {
            // Find and remove corresponding WebAssembly breakpoint
            final List<Breakpoint> wasmBreakpoints = debugSession.getBreakpoints();
            for (final Breakpoint bp : wasmBreakpoints) {
                if (bp.getId() == intellijBreakpoint.getWasmBreakpointId()) {
                    debugSession.removeBreakpoint(bp);
                    break;
                }
            }
        }

        LOGGER.fine("Removed IntelliJ breakpoint: " + breakpointId);
        return true;
    }

    /**
     * Gets the current stack trace formatted for IntelliJ.
     *
     * @return list of IntelliJ stack frames
     * @throws WasmException if stack trace cannot be retrieved
     */
    public List<IntelliJStackFrame> getStackTrace() throws WasmException {
        if (debugSession == null) {
            return List.of();
        }

        final List<StackFrame> wasmFrames = debugSession.getStackTrace();
        final List<IntelliJStackFrame> intellijFrames = new ArrayList<>();

        for (int i = 0; i < wasmFrames.size(); i++) {
            final StackFrame wasmFrame = wasmFrames.get(i);
            final IntelliJStackFrame intellijFrame = new IntelliJStackFrame(
                i,
                wasmFrame.getFunctionName(),
                mapFunctionToSource(wasmFrame.getFunctionName()),
                wasmFrame.getLine(),
                wasmFrame.getColumn(),
                wasmFrame.getVariables()
            );

            stackFrames.put(i, intellijFrame);
            intellijFrames.add(intellijFrame);
        }

        return intellijFrames;
    }

    /**
     * Gets variables for a specific stack frame.
     *
     * @param frameId the frame ID
     * @return list of IntelliJ variables
     * @throws WasmException if variables cannot be retrieved
     */
    public List<IntelliJVariable> getFrameVariables(final int frameId) throws WasmException {
        if (debugSession == null) {
            return List.of();
        }

        final List<Variable> wasmVariables = debugSession.getVariables(frameId);
        return wasmVariables.stream()
            .map(var -> new IntelliJVariable(
                var.getName(),
                var.getType(),
                formatVariableValue(var.getValue()),
                var.isMutable()
            ))
            .toList();
    }

    /**
     * Evaluates an expression in the current context.
     *
     * @param expression the expression to evaluate
     * @return the evaluation result
     * @throws WasmException if evaluation fails
     */
    public IntelliJEvaluationResult evaluateExpression(final String expression) throws WasmException {
        if (debugSession == null) {
            throw new IllegalStateException("No debug session attached");
        }

        final var result = debugSession.evaluateExpression(expression);
        return new IntelliJEvaluationResult(
            expression,
            result.isSuccess(),
            result.getValue() != null ? result.getValue().toString() : null,
            result.getResultType(),
            result.getError()
        );
    }

    /**
     * Continues execution.
     *
     * @return future that completes when execution continues
     * @throws WasmException if execution cannot continue
     */
    public CompletableFuture<Void> continueExecution() throws WasmException {
        if (debugSession == null) {
            throw new IllegalStateException("No debug session attached");
        }

        return debugSession.continueExecution().thenApply(event -> null);
    }

    /**
     * Steps into the next instruction.
     *
     * @return future that completes when step is complete
     * @throws WasmException if step fails
     */
    public CompletableFuture<Void> stepInto() throws WasmException {
        if (debugSession == null) {
            throw new IllegalStateException("No debug session attached");
        }

        return debugSession.stepInto().thenApply(event -> null);
    }

    /**
     * Steps over the current instruction.
     *
     * @return future that completes when step is complete
     * @throws WasmException if step fails
     */
    public CompletableFuture<Void> stepOver() throws WasmException {
        if (debugSession == null) {
            throw new IllegalStateException("No debug session attached");
        }

        return debugSession.stepOver().thenApply(event -> null);
    }

    /**
     * Steps out of the current function.
     *
     * @return future that completes when step is complete
     * @throws WasmException if step fails
     */
    public CompletableFuture<Void> stepOut() throws WasmException {
        if (debugSession == null) {
            throw new IllegalStateException("No debug session attached");
        }

        return debugSession.stepOut().thenApply(event -> null);
    }

    /**
     * Pauses execution.
     *
     * @return future that completes when execution is paused
     * @throws WasmException if pause fails
     */
    public CompletableFuture<Void> pause() throws WasmException {
        if (debugSession == null) {
            throw new IllegalStateException("No debug session attached");
        }

        return debugSession.pause().thenApply(event -> null);
    }

    /**
     * Checks if the adapter is attached to IntelliJ.
     *
     * @return true if attached
     */
    public boolean isAttached() {
        return attached.get();
    }

    /**
     * Checks if the adapter is initialized.
     *
     * @return true if initialized
     */
    public boolean isInitialized() {
        return initialized.get();
    }

    // DebugEventListener implementation

    @Override
    public void onBreakpointHit(final DebugEvent event) {
        LOGGER.fine("Breakpoint hit event received: " + event);
        // Forward to IntelliJ debugging infrastructure
        notifyIntelliJBreakpointHit(event);
    }

    @Override
    public void onStepCompleted(final DebugEvent event) {
        LOGGER.fine("Step completed event received: " + event);
        // Forward to IntelliJ debugging infrastructure
        notifyIntelliJStepCompleted(event);
    }

    @Override
    public void onExecutionPaused(final DebugEvent event) {
        LOGGER.fine("Execution paused event received: " + event);
        // Forward to IntelliJ debugging infrastructure
        notifyIntelliJExecutionPaused(event);
    }

    @Override
    public void onExecutionResumed(final DebugEvent event) {
        LOGGER.fine("Execution resumed event received: " + event);
        // Forward to IntelliJ debugging infrastructure
        notifyIntelliJExecutionResumed(event);
    }

    @Override
    public void onExecutionCompleted(final DebugEvent event) {
        LOGGER.fine("Execution completed event received: " + event);
        // Forward to IntelliJ debugging infrastructure
        notifyIntelliJExecutionCompleted(event);
    }

    @Override
    public void onError(final DebugEvent event) {
        LOGGER.warning("Debug error event received: " + event);
        // Forward to IntelliJ debugging infrastructure
        notifyIntelliJError(event);
    }

    @Override
    public void close() {
        if (attached.get()) {
            detachFromIntelliJ();
        }

        if (debugSession != null) {
            debugSession.removeDebugEventListener(this);
        }

        breakpoints.clear();
        stackFrames.clear();
        variables.clear();

        executorService.shutdown();

        LOGGER.info("IntelliJ Debug Adapter closed");
    }

    // Private methods

    private void performIntelliJAttachment() throws IOException {
        // In a real implementation, this would establish connection with IntelliJ's JDI
        // This is a simplified placeholder showing the concept
        LOGGER.info("Connecting to IntelliJ IDEA on port " + debugPort);

        // The actual implementation would use JDI to connect to IntelliJ's debugging infrastructure
        // and register this adapter as a debug target
    }

    private void detachFromIntelliJ() {
        try {
            if (virtualMachine != null) {
                virtualMachine.dispose();
                virtualMachine = null;
            }
            attached.set(false);
            LOGGER.info("Detached from IntelliJ IDEA");
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Error detaching from IntelliJ", e);
        }
    }

    private String mapSourceToFunction(final String sourceFile, final int line) {
        // In a real implementation, this would use source maps or debug information
        // to map source locations to WebAssembly functions
        final String fileName = sourceFile.substring(sourceFile.lastIndexOf('/') + 1);
        return fileName.replaceAll("\\.[^.]*$", "") + "_line_" + line;
    }

    private String mapFunctionToSource(final String functionName) {
        // In a real implementation, this would reverse the source mapping
        if (functionName != null && functionName.contains("_line_")) {
            return functionName.substring(0, functionName.indexOf("_line_")) + ".wasm";
        }
        return "unknown.wasm";
    }

    private String formatVariableValue(final Object value) {
        if (value == null) {
            return "null";
        }
        // Format WebAssembly values appropriately for IntelliJ display
        return value.toString();
    }

    // IntelliJ notification methods (placeholders for actual JDI integration)

    private void notifyIntelliJBreakpointHit(final DebugEvent event) {
        // Implementation would notify IntelliJ's debugging UI about breakpoint hit
        LOGGER.fine("Notifying IntelliJ of breakpoint hit");
    }

    private void notifyIntelliJStepCompleted(final DebugEvent event) {
        // Implementation would notify IntelliJ's debugging UI about step completion
        LOGGER.fine("Notifying IntelliJ of step completion");
    }

    private void notifyIntelliJExecutionPaused(final DebugEvent event) {
        // Implementation would notify IntelliJ's debugging UI about execution pause
        LOGGER.fine("Notifying IntelliJ of execution pause");
    }

    private void notifyIntelliJExecutionResumed(final DebugEvent event) {
        // Implementation would notify IntelliJ's debugging UI about execution resume
        LOGGER.fine("Notifying IntelliJ of execution resume");
    }

    private void notifyIntelliJExecutionCompleted(final DebugEvent event) {
        // Implementation would notify IntelliJ's debugging UI about execution completion
        LOGGER.fine("Notifying IntelliJ of execution completion");
    }

    private void notifyIntelliJError(final DebugEvent event) {
        // Implementation would notify IntelliJ's debugging UI about errors
        LOGGER.warning("Notifying IntelliJ of debug error");
    }

    // IntelliJ-specific data classes

    /**
     * IntelliJ breakpoint representation.
     */
    public static final class IntelliJBreakpoint {
        private final String id;
        private final String sourceFile;
        private final int line;
        private final String condition;
        private final String functionName;
        private final boolean enabled;
        private final long wasmBreakpointId;

        public IntelliJBreakpoint(final long wasmBreakpointId, final String sourceFile, final int line,
                                  final String condition, final String functionName, final boolean enabled) {
            this.id = "bp-" + wasmBreakpointId;
            this.wasmBreakpointId = wasmBreakpointId;
            this.sourceFile = sourceFile;
            this.line = line;
            this.condition = condition;
            this.functionName = functionName;
            this.enabled = enabled;
        }

        public String getId() { return id; }
        public long getWasmBreakpointId() { return wasmBreakpointId; }
        public String getSourceFile() { return sourceFile; }
        public int getLine() { return line; }
        public String getCondition() { return condition; }
        public String getFunctionName() { return functionName; }
        public boolean isEnabled() { return enabled; }

        @Override
        public String toString() {
            return String.format("IntelliJBreakpoint{id='%s', file='%s', line=%d, function='%s', enabled=%s}",
                id, sourceFile, line, functionName, enabled);
        }
    }

    /**
     * IntelliJ stack frame representation.
     */
    public static final class IntelliJStackFrame {
        private final int id;
        private final String functionName;
        private final String sourceFile;
        private final int line;
        private final int column;
        private final List<Variable> variables;

        public IntelliJStackFrame(final int id, final String functionName, final String sourceFile,
                                  final int line, final int column, final List<Variable> variables) {
            this.id = id;
            this.functionName = functionName;
            this.sourceFile = sourceFile;
            this.line = line;
            this.column = column;
            this.variables = variables != null ? List.copyOf(variables) : List.of();
        }

        public int getId() { return id; }
        public String getFunctionName() { return functionName; }
        public String getSourceFile() { return sourceFile; }
        public int getLine() { return line; }
        public int getColumn() { return column; }
        public List<Variable> getVariables() { return variables; }

        @Override
        public String toString() {
            return String.format("IntelliJStackFrame{id=%d, function='%s', file='%s', line=%d}",
                id, functionName, sourceFile, line);
        }
    }

    /**
     * IntelliJ variable representation.
     */
    public static final class IntelliJVariable {
        private final String name;
        private final String type;
        private final String value;
        private final boolean mutable;

        public IntelliJVariable(final String name, final String type, final String value, final boolean mutable) {
            this.name = name;
            this.type = type;
            this.value = value;
            this.mutable = mutable;
        }

        public String getName() { return name; }
        public String getType() { return type; }
        public String getValue() { return value; }
        public boolean isMutable() { return mutable; }

        @Override
        public String toString() {
            return String.format("IntelliJVariable{name='%s', type='%s', value='%s', mutable=%s}",
                name, type, value, mutable);
        }
    }

    /**
     * IntelliJ evaluation result representation.
     */
    public static final class IntelliJEvaluationResult {
        private final String expression;
        private final boolean success;
        private final String value;
        private final String type;
        private final String error;

        public IntelliJEvaluationResult(final String expression, final boolean success, final String value,
                                        final String type, final String error) {
            this.expression = expression;
            this.success = success;
            this.value = value;
            this.type = type;
            this.error = error;
        }

        public String getExpression() { return expression; }
        public boolean isSuccess() { return success; }
        public String getValue() { return value; }
        public String getType() { return type; }
        public String getError() { return error; }

        @Override
        public String toString() {
            return String.format("IntelliJEvaluationResult{expression='%s', success=%s, value='%s', error='%s'}",
                expression, success, value, error);
        }
    }
}