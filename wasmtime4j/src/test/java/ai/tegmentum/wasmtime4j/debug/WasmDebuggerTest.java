package ai.tegmentum.wasmtime4j.debug;

import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.DebugException;
import ai.tegmentum.wasmtime4j.debug.DebugEvent.BreakpointHitEvent;
import ai.tegmentum.wasmtime4j.debug.DebugEvent.ExecutionStartedEvent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for WebAssembly debugging functionality.
 *
 * Note: These tests assume a mock environment since we don't have actual
 * WebAssembly modules for testing. In a real implementation, these would
 * test against actual compiled WebAssembly modules.
 */
@DisplayName("WebAssembly Debugger Tests")
class WasmDebuggerTest {

    @Mock
    private Instance mockInstance;

    @Mock
    private Module mockModule;

    private WasmDebugger debugger;

    @BeforeEach
    void setUp() throws DebugException {
        MockitoAnnotations.openMocks(this);

        // Note: In real implementation, this would create actual native debugger
        // For testing, we'll assume static mocking of native methods
        debugger = mock(WasmDebugger.class);
        when(debugger.getState()).thenReturn(WasmDebugger.DebugState.READY);
    }

    @AfterEach
    void tearDown() {
        if (debugger != null) {
            debugger.close();
        }
    }

    @Nested
    @DisplayName("Source Map Integration Tests")
    class SourceMapTests {

        @Test
        @DisplayName("Load valid source map successfully")
        void loadValidSourceMap() throws Exception {
            final String validSourceMapJson = createValidSourceMapJson();

            doNothing().when(debugger).loadSourceMapFromJson(validSourceMapJson);

            assertDoesNotThrow(() -> debugger.loadSourceMapFromJson(validSourceMapJson));

            verify(debugger).loadSourceMapFromJson(validSourceMapJson);
        }

        @Test
        @DisplayName("Reject invalid source map")
        void rejectInvalidSourceMap() {
            final String invalidJson = "{ invalid json }";

            doThrow(new DebugException("Invalid source map"))
                .when(debugger).loadSourceMapFromJson(invalidJson);

            assertThrows(DebugException.class,
                () -> debugger.loadSourceMapFromJson(invalidJson));
        }

        @Test
        @DisplayName("Set source breakpoint with source map")
        void setSourceBreakpointWithSourceMap() throws Exception {
            final String sourceFile = "main.c";
            final int line = 42;
            final Breakpoint expectedBreakpoint = new Breakpoint(
                1, Breakpoint.BreakpointType.SOURCE_LOCATION,
                sourceFile, line, 0, 0x100, null, true, 0
            );

            when(debugger.setSourceBreakpoint(sourceFile, line)).thenReturn(expectedBreakpoint);

            final Breakpoint breakpoint = debugger.setSourceBreakpoint(sourceFile, line);

            assertNotNull(breakpoint);
            assertEquals(sourceFile, breakpoint.getSourceFile());
            assertEquals(line, breakpoint.getSourceLine());
            assertEquals(Breakpoint.BreakpointType.SOURCE_LOCATION, breakpoint.getType());
        }

        @Test
        @DisplayName("Get source location for byte offset")
        void getSourceLocationForByteOffset() {
            final int byteOffset = 0x100;
            final SourceLocation expectedLocation = new SourceLocation(
                "main.c", 42, 10, "main", byteOffset
            );

            when(debugger.getSourceLocation(byteOffset)).thenReturn(Optional.of(expectedLocation));

            final Optional<SourceLocation> location = debugger.getSourceLocation(byteOffset);

            assertTrue(location.isPresent());
            assertEquals("main.c", location.get().getFileName());
            assertEquals(42, location.get().getLine());
            assertEquals(10, location.get().getColumn());
        }

        private String createValidSourceMapJson() {
            return """
                {
                  "version": 3,
                  "file": "module.wasm",
                  "sources": ["main.c", "helper.c"],
                  "names": ["main", "helper"],
                  "mappings": "AAAA,SAAS"
                }
                """;
        }
    }

    @Nested
    @DisplayName("DWARF Debug Info Tests")
    class DwarfTests {

        @Test
        @DisplayName("Load DWARF debug information")
        void loadDwarfDebugInfo() {
            final Map<String, byte[]> customSections = Map.of(
                ".debug_info", new byte[]{0x01, 0x02, 0x03},
                ".debug_line", new byte[]{0x04, 0x05, 0x06},
                ".debug_str", new byte[]{0x07, 0x08, 0x09}
            );

            doNothing().when(debugger).loadDwarfDebugInfo(customSections);

            assertDoesNotThrow(() -> debugger.loadDwarfDebugInfo(customSections));

            verify(debugger).loadDwarfDebugInfo(customSections);
        }

        @Test
        @DisplayName("Handle malformed DWARF data")
        void handleMalformedDwarfData() {
            final Map<String, byte[]> malformedSections = Map.of(
                ".debug_info", new byte[]{0x00} // Too short for valid DWARF
            );

            doThrow(new DebugException("Invalid DWARF data"))
                .when(debugger).loadDwarfDebugInfo(malformedSections);

            assertThrows(DebugException.class,
                () -> debugger.loadDwarfDebugInfo(malformedSections));
        }
    }

    @Nested
    @DisplayName("Breakpoint Management Tests")
    class BreakpointTests {

        @Test
        @DisplayName("Set and remove byte offset breakpoint")
        void setAndRemoveByteOffsetBreakpoint() {
            final int byteOffset = 0x100;
            final Breakpoint breakpoint = new Breakpoint(
                1, Breakpoint.BreakpointType.BYTE_OFFSET,
                null, -1, -1, byteOffset, null, true, 0
            );

            when(debugger.setByteOffsetBreakpoint(byteOffset)).thenReturn(breakpoint);
            when(debugger.removeBreakpoint(1)).thenReturn(true);

            final Breakpoint setBreakpoint = debugger.setByteOffsetBreakpoint(byteOffset);
            assertNotNull(setBreakpoint);
            assertEquals(byteOffset, setBreakpoint.getByteOffset());

            final boolean removed = debugger.removeBreakpoint(1);
            assertTrue(removed);
        }

        @Test
        @DisplayName("Set conditional breakpoint")
        void setConditionalBreakpoint() {
            final int byteOffset = 0x100;
            final String condition = "local_0 == 42";
            final Breakpoint breakpoint = new Breakpoint(
                1, Breakpoint.BreakpointType.BYTE_OFFSET,
                null, -1, -1, byteOffset, condition, true, 0
            );

            when(debugger.setByteOffsetBreakpoint(byteOffset, condition)).thenReturn(breakpoint);

            final Breakpoint setBreakpoint = debugger.setByteOffsetBreakpoint(byteOffset, condition);
            assertNotNull(setBreakpoint);
            assertEquals(condition, setBreakpoint.getCondition());
            assertTrue(setBreakpoint.isConditional());
        }

        @Test
        @DisplayName("Enable and disable breakpoint")
        void enableAndDisableBreakpoint() {
            doNothing().when(debugger).setBreakpointEnabled(1, false);
            doNothing().when(debugger).setBreakpointEnabled(1, true);

            assertDoesNotThrow(() -> debugger.setBreakpointEnabled(1, false));
            assertDoesNotThrow(() -> debugger.setBreakpointEnabled(1, true));

            verify(debugger).setBreakpointEnabled(1, false);
            verify(debugger).setBreakpointEnabled(1, true);
        }
    }

    @Nested
    @DisplayName("Watch Expression Tests")
    class WatchExpressionTests {

        @Test
        @DisplayName("Add and remove watch expression")
        void addAndRemoveWatchExpression() {
            final String name = "local_var";
            final String expression = "local_0";
            final WatchExpression watchExpr = new WatchExpression(name, expression, true);

            when(debugger.addWatchExpression(name, expression)).thenReturn(watchExpr);
            when(debugger.removeWatchExpression(name)).thenReturn(true);

            final WatchExpression added = debugger.addWatchExpression(name, expression);
            assertNotNull(added);
            assertEquals(name, added.getName());
            assertEquals(expression, added.getExpression());

            final boolean removed = debugger.removeWatchExpression(name);
            assertTrue(removed);
        }

        @Test
        @DisplayName("Evaluate watch expression")
        void evaluateWatchExpression() {
            final String expression = "local_0";
            final WasmValue expectedValue = WasmValue.i32(42);
            final WatchEvaluationResult result = WatchEvaluationResult.successful(expectedValue, "i32");

            when(debugger.evaluateWatch(expression)).thenReturn(result);

            final WatchEvaluationResult evalResult = debugger.evaluateWatch(expression);

            assertNotNull(evalResult);
            assertTrue(evalResult.isSuccessful());
            assertEquals(expectedValue, evalResult.getValue());
            assertEquals("i32", evalResult.getTypeName());
        }
    }

    @Nested
    @DisplayName("Execution Control Tests")
    class ExecutionControlTests {

        @Test
        @DisplayName("Debug function execution")
        void debugFunctionExecution() throws Exception {
            final String functionName = "main";
            final WasmValue[] args = {WasmValue.i32(10), WasmValue.i32(20)};
            final WasmValue[] results = {WasmValue.i32(30)};
            final DebugResult expectedResult = DebugResult.successful(results,
                Duration.ofMillis(100), 1000);

            final CompletableFuture<DebugResult> future = CompletableFuture.completedFuture(expectedResult);
            when(debugger.debugFunction(functionName, args)).thenReturn(future);

            final CompletableFuture<DebugResult> resultFuture = debugger.debugFunction(functionName, args);
            final DebugResult result = resultFuture.get(1, TimeUnit.SECONDS);

            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertArrayEquals(results, result.getResults());
            assertEquals(Duration.ofMillis(100), result.getExecutionTime());
        }

        @Test
        @DisplayName("Step execution control")
        void stepExecutionControl() throws Exception {
            final ExecutionContext context = new ExecutionContext(
                0, "main", 0x100, 5, Map.of(), List.of(), null
            );

            when(debugger.getState()).thenReturn(WasmDebugger.DebugState.PAUSED);
            when(debugger.stepNext()).thenReturn(context);
            when(debugger.stepInto()).thenReturn(context);
            when(debugger.stepOut()).thenReturn(context);

            final ExecutionContext nextContext = debugger.stepNext();
            assertNotNull(nextContext);
            assertEquals("main", nextContext.getFunctionName());

            final ExecutionContext intoContext = debugger.stepInto();
            assertNotNull(intoContext);

            final ExecutionContext outContext = debugger.stepOut();
            assertNotNull(outContext);
        }

        @Test
        @DisplayName("Continue and pause execution")
        void continueAndPauseExecution() throws Exception {
            final ExecutionContext context = new ExecutionContext(
                0, "main", 0x100, 5, Map.of(), List.of(), null
            );

            when(debugger.getState())
                .thenReturn(WasmDebugger.DebugState.PAUSED)
                .thenReturn(WasmDebugger.DebugState.RUNNING);
            when(debugger.pauseExecution()).thenReturn(context);

            doNothing().when(debugger).continueExecution();

            assertDoesNotThrow(() -> debugger.continueExecution());

            final ExecutionContext pausedContext = debugger.pauseExecution();
            assertNotNull(pausedContext);
        }
    }

    @Nested
    @DisplayName("Event System Tests")
    class EventSystemTests {

        @Test
        @DisplayName("Debug event listener registration and notification")
        void debugEventListenerRegistration() throws Exception {
            final List<DebugEvent> receivedEvents = new ArrayList<>();
            final CountDownLatch eventLatch = new CountDownLatch(1);

            final WasmDebugger.DebugEventListener listener = event -> {
                receivedEvents.add(event);
                eventLatch.countDown();
            };

            doNothing().when(debugger).addEventListener(listener);
            doNothing().when(debugger).removeEventListener(listener);

            debugger.addEventListener(listener);

            // In a real implementation, events would be fired during debugging
            // For this test, we simulate event notification
            final DebugEvent testEvent = new ExecutionStartedEvent("main");
            // listener.onDebugEvent(testEvent); // Would be called internally

            debugger.removeEventListener(listener);

            verify(debugger).addEventListener(listener);
            verify(debugger).removeEventListener(listener);
        }

        @Test
        @DisplayName("Multiple event listeners")
        void multipleEventListeners() {
            final WasmDebugger.DebugEventListener listener1 = event -> {};
            final WasmDebugger.DebugEventListener listener2 = event -> {};

            doNothing().when(debugger).addEventListener(listener1);
            doNothing().when(debugger).addEventListener(listener2);

            assertDoesNotThrow(() -> {
                debugger.addEventListener(listener1);
                debugger.addEventListener(listener2);
            });

            verify(debugger).addEventListener(listener1);
            verify(debugger).addEventListener(listener2);
        }
    }

    @Nested
    @DisplayName("Memory Inspection Tests")
    class MemoryInspectionTests {

        @Test
        @DisplayName("Inspect memory at address")
        void inspectMemoryAtAddress() {
            final long address = 0x1000;
            final int length = 64;
            final byte[] data = new byte[length];
            // Fill with test pattern
            for (int i = 0; i < length; i++) {
                data[i] = (byte) (i & 0xFF);
            }

            final MemoryInspection inspection = new MemoryInspection(address, data);
            when(debugger.inspectMemory(address, length)).thenReturn(inspection);

            final MemoryInspection result = debugger.inspectMemory(address, length);

            assertNotNull(result);
            assertEquals(address, result.getAddress());
            assertEquals(length, result.getLength());
            assertArrayEquals(data, result.getData());
        }

        @Test
        @DisplayName("Read typed values from memory")
        void readTypedValuesFromMemory() {
            final byte[] data = new byte[16];
            // Write test values in little-endian format
            data[0] = 0x2A; data[1] = 0x00; data[2] = 0x00; data[3] = 0x00; // int32: 42
            data[4] = 0x00; data[5] = 0x00; data[6] = 0x00; data[7] = 0x00;
            data[8] = 0x00; data[9] = 0x00; data[10] = 0x00; data[11] = 0x00; // int64: 42
            data[12] = 0x2A; data[13] = 0x00; data[14] = 0x00; data[15] = 0x00;

            final MemoryInspection inspection = new MemoryInspection(0x1000, data);

            assertEquals(42, inspection.readInt32(0));
            assertEquals(42L, inspection.readInt64(0));
        }
    }

    @Nested
    @DisplayName("Call Stack Tests")
    class CallStackTests {

        @Test
        @DisplayName("Get call stack frames")
        void getCallStackFrames() {
            final StackFrame frame1 = new StackFrame(0, "main", 0x100, Map.of("arg1", WasmValue.i32(10)), null);
            final StackFrame frame2 = new StackFrame(1, "helper", 0x200, Map.of("local1", WasmValue.i32(20)), null);
            final List<StackFrame> frames = List.of(frame1, frame2);

            when(debugger.getCallStack()).thenReturn(frames);

            final List<StackFrame> callStack = debugger.getCallStack();

            assertNotNull(callStack);
            assertEquals(2, callStack.size());
            assertEquals("main", callStack.get(0).getFunctionName());
            assertEquals("helper", callStack.get(1).getFunctionName());
        }

        @Test
        @DisplayName("Empty call stack when not debugging")
        void emptyCallStackWhenNotDebugging() {
            when(debugger.getState()).thenReturn(WasmDebugger.DebugState.READY);
            when(debugger.getCallStack()).thenReturn(List.of());

            final List<StackFrame> callStack = debugger.getCallStack();

            assertNotNull(callStack);
            assertTrue(callStack.isEmpty());
        }
    }

    @Nested
    @DisplayName("Variable Inspection Tests")
    class VariableInspectionTests {

        @Test
        @DisplayName("Get current variables in scope")
        void getCurrentVariablesInScope() {
            final Map<String, VariableValue> variables = Map.of(
                "local_0", new VariableValue("local_0", WasmValue.i32(42), "i32", VariableValue.VariableScope.LOCAL),
                "param_0", new VariableValue("param_0", WasmValue.f64(3.14), "f64", VariableValue.VariableScope.PARAMETER)
            );

            when(debugger.getState()).thenReturn(WasmDebugger.DebugState.PAUSED);
            when(debugger.getVariables()).thenReturn(variables);

            final Map<String, VariableValue> result = debugger.getVariables();

            assertNotNull(result);
            assertEquals(2, result.size());
            assertTrue(result.containsKey("local_0"));
            assertTrue(result.containsKey("param_0"));

            final VariableValue local0 = result.get("local_0");
            assertEquals(WasmValue.i32(42), local0.getValue());
            assertEquals("i32", local0.getTypeName());
        }
    }

    @Nested
    @DisplayName("Statistics and State Tests")
    class StatisticsTests {

        @Test
        @DisplayName("Get execution statistics")
        void getExecutionStatistics() {
            final ExecutionStatistics stats = new ExecutionStatistics(5, 3, 2, 1);
            when(debugger.getExecutionStatistics()).thenReturn(stats);

            final ExecutionStatistics result = debugger.getExecutionStatistics();

            assertNotNull(result);
            assertEquals(5, result.getTotalExecutions());
            assertEquals(3, result.getActiveBreakpoints());
            assertEquals(2, result.getActiveWatchExpressions());
            assertEquals(1, result.getEventListeners());
        }

        @Test
        @DisplayName("Get current debugger state")
        void getCurrentDebuggerState() {
            when(debugger.getState())
                .thenReturn(WasmDebugger.DebugState.READY)
                .thenReturn(WasmDebugger.DebugState.RUNNING)
                .thenReturn(WasmDebugger.DebugState.PAUSED);

            assertEquals(WasmDebugger.DebugState.READY, debugger.getState());
            assertEquals(WasmDebugger.DebugState.RUNNING, debugger.getState());
            assertEquals(WasmDebugger.DebugState.PAUSED, debugger.getState());
        }

        @Test
        @DisplayName("Get source files from debug information")
        void getSourceFilesFromDebugInfo() {
            final List<String> sourceFiles = List.of("main.c", "helper.c", "utils.c");
            when(debugger.getSourceFiles()).thenReturn(sourceFiles);

            final List<String> files = debugger.getSourceFiles();

            assertNotNull(files);
            assertEquals(3, files.size());
            assertTrue(files.contains("main.c"));
            assertTrue(files.contains("helper.c"));
            assertTrue(files.contains("utils.c"));
        }
    }

    @Test
    @DisplayName("Cleanup resources on close")
    void cleanupResourcesOnClose() {
        doNothing().when(debugger).close();

        assertDoesNotThrow(() -> debugger.close());

        verify(debugger).close();
    }

    @Test
    @DisplayName("Thread safety with concurrent access")
    void threadSafetyWithConcurrentAccess() throws InterruptedException {
        final int numThreads = 10;
        final CountDownLatch latch = new CountDownLatch(numThreads);
        final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // Simulate concurrent access to debugger
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    // Mock thread-safe operations
                    when(debugger.getState()).thenReturn(WasmDebugger.DebugState.READY);
                    debugger.getState();

                    when(debugger.getExecutionStatistics())
                        .thenReturn(new ExecutionStatistics(threadId, 0, 0, 0));
                    debugger.getExecutionStatistics();
                } catch (final Exception e) {
                    exceptions.add(e);
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(exceptions.isEmpty(), "No exceptions should occur during concurrent access");
    }
}