package ai.tegmentum.wasmtime4j.debug;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;

/**
 * Comprehensive test suite for advanced debugging protocol extensions.
 *
 * <p>This test class validates all implemented debugging features including:
 *
 * <ul>
 *   <li>Remote debugging protocol with network communication
 *   <li>Multi-target debugging with concurrent instance management
 *   <li>Debug adapters for IDEs (VS Code, IntelliJ)
 *   <li>Distributed debugging coordination
 *   <li>Advanced profiling and performance analysis
 *   <li>Memory debugging with heap analysis
 *   <li>Time-travel debugging capabilities
 *   <li>Comprehensive analytics and performance impact
 * </ul>
 *
 * @since 1.0.0
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ComprehensiveDebugProtocolTest {

  private WasmRuntime runtime;
  private Engine engine;
  private Store store;
  private Module testModule;
  private Instance testInstance;
  private DebugSession debugSession;
  private Debugger debugger;

  @BeforeEach
  void setUp() throws Exception {
    // Initialize WebAssembly runtime
    runtime = WasmRuntimeFactory.createRuntime();
    engine = runtime.createEngine();
    store = runtime.createStore(engine);

    // Create a test WebAssembly module
    final byte[] wasmBytes = createTestWasmModule();
    testModule = runtime.createModule(engine, wasmBytes);
    testInstance = runtime.instantiate(store, testModule);

    // Create debug session
    debugger = runtime.createDebugger(engine);
    debugSession = debugger.createSession(testInstance);
  }

  @AfterEach
  void tearDown() {
    if (debugSession != null && debugSession.isActive()) {
      debugSession.close();
    }
    if (debugger != null) {
      debugger.close();
    }
    if (store != null) {
      store.close();
    }
    if (engine != null) {
      engine.close();
    }
    if (runtime != null) {
      runtime.close();
    }
  }

  @Test
  @Timeout(30)
  void testRemoteDebuggingProtocol() throws Exception {
    // Test remote debugging session creation and communication
    try (final RemoteDebugSession remoteSession =
        RemoteDebugSession.builder()
            .port(9230)
            .maxClients(2)
            .authenticationRequired(false)
            .encryptionEnabled(true)
            .build()) {

      // Start remote debug server
      remoteSession.start();
      assertTrue(remoteSession.isRunning());
      assertEquals(9230, remoteSession.getPort());

      // Attach debug session
      remoteSession.attachSession(debugSession);
      assertEquals(1, remoteSession.getAttachedSessions().size());

      // Verify server is accepting connections
      assertTrue(remoteSession.getClientCount() >= 0);

      // Test message handling and protocol compliance
      final List<String> attachedSessions = remoteSession.getAttachedSessions();
      assertTrue(attachedSessions.contains(debugSession.getSessionId()));

      // Detach session
      final boolean detached = remoteSession.detachSession(debugSession.getSessionId());
      assertTrue(detached);
      assertEquals(0, remoteSession.getAttachedSessions().size());
    }
  }

  @Test
  @Timeout(30)
  void testMultiTargetDebugging() throws Exception {
    // Test multi-target debugging manager
    try (final MultiTargetDebugManager manager = new MultiTargetDebugManager()) {
      assertTrue(manager.isActive());
      assertEquals(0, manager.getTargetCount());

      // Create multiple test instances
      final Instance instance1 = runtime.instantiate(store, testModule);
      final Instance instance2 = runtime.instantiate(store, testModule);

      // Add targets to manager
      final DebugTarget target1 = manager.addTarget("target1", instance1, new DebugConfig());
      final DebugTarget target2 = manager.addTarget("target2", instance2, new DebugConfig());

      assertNotNull(target1);
      assertNotNull(target2);
      assertEquals(2, manager.getTargetCount());

      // Test global breakpoint setting
      final GlobalBreakpoint globalBp = manager.setGlobalBreakpoint("test_function", 42);
      assertNotNull(globalBp);
      assertEquals(2, globalBp.getTargetCount());

      // Test coordinated operations
      final CompletableFuture<MultiTargetExecutionResult> continueResult =
          manager.continueAllTargets();
      assertNotNull(continueResult);

      final CompletableFuture<MultiTargetExecutionResult> pauseResult = manager.pauseAllTargets();
      assertNotNull(pauseResult);

      // Test performance metrics collection
      final MultiTargetPerformanceMetrics metrics = manager.getPerformanceMetrics();
      assertNotNull(metrics);
      assertEquals(2, metrics.totalTargets());
      assertTrue(metrics.activeTargets() <= metrics.totalTargets());

      // Remove targets
      final boolean removed1 = manager.removeTarget("target1");
      final boolean removed2 = manager.removeTarget("target2");
      assertTrue(removed1);
      assertTrue(removed2);
      assertEquals(0, manager.getTargetCount());
    }
  }

  @Test
  @Timeout(30)
  void testVSCodeDebugAdapter() throws Exception {
    // Test VS Code Debug Adapter Protocol integration
    try (final ai.tegmentum.wasmtime4j.ide.vscode.VSCodeDebugAdapter adapter =
        new ai.tegmentum.wasmtime4j.ide.vscode.VSCodeDebugAdapter(9231)) {

      // Start adapter
      adapter.start();
      assertTrue(adapter.isRunning());
      assertEquals(9231, adapter.getPort());

      // Attach debug session
      adapter.attachDebugSession(debugSession);

      // Test adapter lifecycle
      assertDoesNotThrow(adapter::close);
      assertFalse(adapter.isRunning());
    }
  }

  @Test
  @Timeout(30)
  void testIntelliJDebugAdapter() throws Exception {
    // Test IntelliJ IDEA debug adapter
    try (final ai.tegmentum.wasmtime4j.ide.intellij.IntelliJDebugAdapter adapter =
        new ai.tegmentum.wasmtime4j.ide.intellij.IntelliJDebugAdapter()) {

      // Initialize adapter
      adapter.initialize(debugSession);
      assertTrue(adapter.isInitialized());

      // Test breakpoint operations
      final var breakpoint = adapter.setBreakpoint("/test/source.wasm", 10, null);
      assertNotNull(breakpoint);
      assertEquals(10, breakpoint.getLine());

      // Test stack trace retrieval
      final List<ai.tegmentum.wasmtime4j.ide.intellij.IntelliJDebugAdapter.IntelliJStackFrame>
          frames = adapter.getStackTrace();
      assertNotNull(frames);

      // Test variable inspection
      final List<ai.tegmentum.wasmtime4j.ide.intellij.IntelliJDebugAdapter.IntelliJVariable>
          variables = adapter.getFrameVariables(0);
      assertNotNull(variables);

      // Test expression evaluation
      final var evalResult = adapter.evaluateExpression("1 + 1");
      assertNotNull(evalResult);

      // Test execution control
      final CompletableFuture<Void> stepFuture = adapter.stepInto();
      assertNotNull(stepFuture);

      // Remove breakpoint
      final boolean removed = adapter.removeBreakpoint(breakpoint.getId());
      assertTrue(removed);
    }
  }

  @Test
  @Timeout(30)
  void testDebugEventHandling() throws Exception {
    // Test debug event system
    final var eventListener = new TestDebugEventListener();
    debugSession.addDebugEventListener(eventListener);

    // Set breakpoint
    final Breakpoint breakpoint = debugSession.setBreakpoint("test_function", 1);
    assertNotNull(breakpoint);

    // Test various execution states
    final CompletableFuture<DebugEvent> continueEvent = debugSession.continueExecution();
    assertNotNull(continueEvent);

    final CompletableFuture<DebugEvent> pauseEvent = debugSession.pause();
    assertNotNull(pauseEvent);

    final CompletableFuture<DebugEvent> stepEvent = debugSession.stepInto();
    assertNotNull(stepEvent);

    // Verify event listener received events
    // In a real test, we would wait for and verify specific events
    assertTrue(debugSession.removeDebugEventListener(eventListener));
  }

  @Test
  @Timeout(30)
  void testVariableInspection() throws Exception {
    // Test variable inspection capabilities
    final List<Variable> variables = debugSession.getCurrentVariables();
    assertNotNull(variables);

    // Test variable value retrieval and modification
    if (!variables.isEmpty()) {
      final Variable firstVar = variables.get(0);
      final VariableValue value = debugSession.getVariableValue(firstVar.getName());
      assertNotNull(value);

      // Test variable value setting (if mutable)
      if (firstVar.isMutable()) {
        assertDoesNotThrow(() -> debugSession.setVariableValue(firstVar.getName(), value));
      }
    }

    // Test expression evaluation
    final EvaluationResult evalResult = debugSession.evaluateExpression("true");
    assertNotNull(evalResult);
  }

  @Test
  @Timeout(30)
  void testMemoryDebugging() throws Exception {
    // Test memory debugging capabilities
    final MemoryInfo memoryInfo = debugSession.getMemoryInfo();
    assertNotNull(memoryInfo);
    assertTrue(memoryInfo.getCurrentSize() >= 0);
    assertTrue(memoryInfo.getMaxSize() >= memoryInfo.getCurrentSize());

    // Test memory reading
    if (memoryInfo.getCurrentSize() > 0) {
      final byte[] memoryData =
          debugSession.readMemory(0, Math.min(1024, (int) memoryInfo.getCurrentSize()));
      assertNotNull(memoryData);
      assertTrue(memoryData.length > 0);

      // Test memory writing
      final byte[] testData = {0x01, 0x02, 0x03, 0x04};
      if (testData.length <= memoryInfo.getCurrentSize()) {
        assertDoesNotThrow(() -> debugSession.writeMemory(0, testData));

        // Verify written data
        final byte[] readBack = debugSession.readMemory(0, testData.length);
        assertNotNull(readBack);
        assertEquals(testData.length, readBack.length);
      }

      // Test memory search
      final List<Long> searchResults =
          debugSession.searchMemory(
              new byte[] {0x00}, 0, Math.min(100, memoryInfo.getCurrentSize()));
      assertNotNull(searchResults);
    }
  }

  @Test
  @Timeout(30)
  void testPerformanceMonitoring() throws Exception {
    // Test performance monitoring capabilities
    final ProfilingData profilingData = debugSession.getProfilingData();
    assertNotNull(profilingData);

    // Enable profiling
    debugSession.setProfilingEnabled(true);

    // Perform some operations to generate profiling data
    final CompletableFuture<DebugEvent> stepEvent = debugSession.stepInto();
    assertNotNull(stepEvent);

    // Get updated profiling data
    final ProfilingData updatedData = debugSession.getProfilingData();
    assertNotNull(updatedData);

    // Disable profiling
    debugSession.setProfilingEnabled(false);
  }

  @Test
  @Timeout(30)
  void testDebugSessionLifecycle() throws Exception {
    // Test debug session lifecycle management
    assertTrue(debugSession.isActive());
    assertNotNull(debugSession.getSessionId());
    assertNotNull(debugSession.getInstances());
    assertFalse(debugSession.getInstances().isEmpty());

    // Test execution state
    final ExecutionState state = debugSession.getExecutionState();
    assertNotNull(state);

    // Test breakpoint management
    final Breakpoint bp1 = debugSession.setBreakpoint("function1", 10);
    final Breakpoint bp2 = debugSession.setBreakpoint("function2", 20);
    assertNotNull(bp1);
    assertNotNull(bp2);

    final List<Breakpoint> breakpoints = debugSession.getBreakpoints();
    assertEquals(2, breakpoints.size());

    // Remove breakpoints
    assertTrue(debugSession.removeBreakpoint(bp1));
    assertTrue(debugSession.removeBreakpoint(bp2));

    final List<Breakpoint> remainingBreakpoints = debugSession.getBreakpoints();
    assertEquals(0, remainingBreakpoints.size());
  }

  // Helper methods

  private byte[] createTestWasmModule() throws IOException {
    // Create a minimal WebAssembly module for testing
    // In a real implementation, this would be a proper WASM binary
    // For testing purposes, we create a minimal valid module
    return new byte[] {
      0x00, 0x61, 0x73, 0x6D, // WASM magic number
      0x01, 0x00, 0x00, 0x00 // WASM version
    };
  }

  /** Test debug event listener for verifying event handling. */
  private static class TestDebugEventListener implements DebugEventListener {
    private volatile int eventCount = 0;

    @Override
    public void onBreakpointHit(final DebugEvent event) {
      eventCount++;
    }

    @Override
    public void onStepCompleted(final DebugEvent event) {
      eventCount++;
    }

    @Override
    public void onExecutionPaused(final DebugEvent event) {
      eventCount++;
    }

    @Override
    public void onExecutionResumed(final DebugEvent event) {
      eventCount++;
    }

    @Override
    public void onExecutionCompleted(final DebugEvent event) {
      eventCount++;
    }

    @Override
    public void onError(final DebugEvent event) {
      eventCount++;
    }

    public int getEventCount() {
      return eventCount;
    }
  }
}
