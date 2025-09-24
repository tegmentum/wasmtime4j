package ai.tegmentum.wasmtime4j.debug;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.debug.ExecutionTracer.TraceConfiguration;
import ai.tegmentum.wasmtime4j.debug.ExecutionTracer.TraceEventType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for WebAssembly execution tracing functionality.
 */
@DisplayName("Execution Tracer Tests")
class ExecutionTracerTest {

    @Mock
    private WasmDebugger mockDebugger;

    private ExecutionTracer tracer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        final TraceConfiguration config = TraceConfiguration.defaultConfig();
        tracer = new ExecutionTracer(mockDebugger, config);
    }

    @AfterEach
    void tearDown() {
        if (tracer != null) {
            tracer.close();
        }
    }

    @Nested
    @DisplayName("Trace Configuration Tests")
    class TraceConfigurationTests {

        @Test
        @DisplayName("Default configuration")
        void defaultConfiguration() {
            final TraceConfiguration config = TraceConfiguration.defaultConfig();

            assertNotNull(config);
            assertTrue(config.getTraceTypes().contains(TraceEventType.FUNCTION_CALL));
            assertTrue(config.getTraceTypes().contains(TraceEventType.INSTRUCTION_EXECUTED));
            assertTrue(config.includeSourceMapping());
            assertTrue(config.profileFunctions());
            assertEquals(100_000, config.getMaxEvents());
        }

        @Test
        @DisplayName("Minimal configuration")
        void minimalConfiguration() {
            final TraceConfiguration config = TraceConfiguration.minimalConfig();

            assertNotNull(config);
            assertTrue(config.getTraceTypes().contains(TraceEventType.FUNCTION_CALL));
            assertEquals(1, config.getTraceTypes().size());
            assertFalse(config.includeSourceMapping());
            assertTrue(config.profileFunctions());
            assertEquals(10_000, config.getMaxEvents());
        }

        @Test
        @DisplayName("Custom configuration")
        void customConfiguration() {
            final EnumSet<TraceEventType> types = EnumSet.of(TraceEventType.FUNCTION_CALL);
            final TraceConfiguration config = new TraceConfiguration(types, 5000, false, false);

            assertNotNull(config);
            assertEquals(types, config.getTraceTypes());
            assertEquals(5000, config.getMaxEvents());
            assertFalse(config.includeSourceMapping());
            assertFalse(config.profileFunctions());
        }
    }

    @Nested
    @DisplayName("Trace Lifecycle Tests")
    class TraceLifecycleTests {

        @Test
        @DisplayName("Start and stop tracing")
        void startAndStopTracing() {
            doNothing().when(mockDebugger).addEventListener(any());

            assertDoesNotThrow(() -> tracer.startTracing());
            assertDoesNotThrow(() -> tracer.stopTracing());

            verify(mockDebugger).addEventListener(any());
        }

        @Test
        @DisplayName("Multiple start calls are safe")
        void multipleStartCallsAreSafe() {
            doNothing().when(mockDebugger).addEventListener(any());

            tracer.startTracing();
            tracer.startTracing(); // Should be safe to call multiple times

            // Should only register listener once
            verify(mockDebugger, times(1)).addEventListener(any());
        }

        @Test
        @DisplayName("Stop without start is safe")
        void stopWithoutStartIsSafe() {
            assertDoesNotThrow(() -> tracer.stopTracing());
        }
    }

    @Nested
    @DisplayName("Event Recording Tests")
    class EventRecordingTests {

        @Test
        @DisplayName("Record trace events")
        void recordTraceEvents() {
            doNothing().when(mockDebugger).addEventListener(any());

            tracer.startTracing();

            // In real implementation, events would be recorded automatically
            // Here we verify the tracer is in the correct state
            final List<TraceEvent> events = tracer.getTraceEvents();
            assertNotNull(events);
            // Should have at least a trace started event
        }

        @Test
        @DisplayName("Get empty events when not tracing")
        void getEmptyEventsWhenNotTracing() {
            final List<TraceEvent> events = tracer.getTraceEvents();

            assertNotNull(events);
            assertTrue(events.isEmpty());
        }
    }

    @Nested
    @DisplayName("Function Profiling Tests")
    class FunctionProfilingTests {

        @Test
        @DisplayName("Function profiles created during tracing")
        void functionProfilesCreatedDuringTracing() {
            doNothing().when(mockDebugger).addEventListener(any());

            tracer.startTracing();

            final Map<String, FunctionProfile> profiles = tracer.getFunctionProfiles();
            assertNotNull(profiles);
            // Initially empty until functions are called
        }

        @Test
        @DisplayName("Function profile statistics")
        void functionProfileStatistics() {
            final FunctionProfile profile = new FunctionProfile(
                "testFunction",
                10, // call count
                Duration.ofMillis(100), // total time
                Duration.ofMillis(5),   // min time
                Duration.ofMillis(15),  // max time
                1000 // instruction count
            );

            assertEquals("testFunction", profile.getFunctionName());
            assertEquals(10, profile.getCallCount());
            assertEquals(Duration.ofMillis(100), profile.getTotalTime());
            assertEquals(Duration.ofMillis(10), profile.getAverageTime());
            assertEquals(100.0, profile.getAverageInstructions(), 0.1);
            assertTrue(profile.getInstructionRate() > 0);
        }
    }

    @Nested
    @DisplayName("Performance Analysis Tests")
    class PerformanceAnalysisTests {

        @Test
        @DisplayName("Analyze performance without bottlenecks")
        void analyzePerformanceWithoutBottlenecks() {
            doNothing().when(mockDebugger).addEventListener(any());

            tracer.startTracing();

            final PerformanceAnalysis analysis = tracer.analyzePerformance();

            assertNotNull(analysis);
            assertEquals(Duration.ZERO, analysis.getTotalExecutionTime());
            assertEquals(0, analysis.getTotalInstructions());
            assertTrue(analysis.getBottlenecks().isEmpty());
            assertEquals(0, analysis.getAnalyzedFunctions());
        }

        @Test
        @DisplayName("Performance analysis with mock data")
        void performanceAnalysisWithMockData() {
            final PerformanceAnalysis analysis = new PerformanceAnalysis(
                Duration.ofSeconds(1),
                10000,
                List.of(),
                5
            );

            assertEquals(Duration.ofSeconds(1), analysis.getTotalExecutionTime());
            assertEquals(10000, analysis.getTotalInstructions());
            assertEquals(10000.0, analysis.getInstructionsPerSecond(), 0.1);
            assertEquals(5, analysis.getAnalyzedFunctions());

            final List<String> recommendations = analysis.getOptimizationRecommendations();
            assertNotNull(recommendations);
            assertFalse(recommendations.isEmpty());
            assertEquals("No significant performance bottlenecks identified",
                       recommendations.get(0));
        }
    }

    @Nested
    @DisplayName("Trace Statistics Tests")
    class TraceStatisticsTests {

        @Test
        @DisplayName("Get trace statistics")
        void getTraceStatistics() {
            doNothing().when(mockDebugger).addEventListener(any());

            tracer.startTracing();

            final TraceStatistics stats = tracer.getStatistics();

            assertNotNull(stats);
            assertTrue(stats.getTotalEvents() >= 0);
            assertEquals(0, stats.getTotalInstructions());
            assertEquals(Duration.ZERO, stats.getTotalExecutionTime());
            assertEquals(0, stats.getProfiledFunctions());
        }

        @Test
        @DisplayName("Trace statistics calculations")
        void traceStatisticsCalculations() {
            final Map<TraceEventType, Long> eventCounts = Map.of(
                TraceEventType.FUNCTION_CALL, 100L,
                TraceEventType.INSTRUCTION_EXECUTED, 10000L
            );

            final TraceStatistics stats = new TraceStatistics(
                10100, // total events
                10000, // total instructions
                Duration.ofSeconds(1), // execution time
                eventCounts,
                5 // profiled functions
            );

            assertEquals(10100, stats.getTotalEvents());
            assertEquals(10000, stats.getTotalInstructions());
            assertEquals(10000.0, stats.getInstructionsPerSecond(), 0.1);
            assertEquals(10100.0, stats.getEventsPerSecond(), 0.1);
            assertEquals(100L, stats.getEventCount(TraceEventType.FUNCTION_CALL));
        }
    }

    @Nested
    @DisplayName("Trace Export Tests")
    class TraceExportTests {

        @Test
        @DisplayName("Export trace data")
        void exportTraceData() {
            doNothing().when(mockDebugger).addEventListener(any());

            tracer.startTracing();

            final TraceExport export = tracer.exportTrace();

            assertNotNull(export);
            assertNotNull(export.getEvents());
            assertNotNull(export.getStatistics());
            assertNotNull(export.getFunctionProfiles());
            assertNotNull(export.getExportTimestamp());
        }

        @Test
        @DisplayName("Chrome trace format export")
        void chromeTraceFormatExport() {
            final TraceExport.ExportEvent event = new TraceExport.ExportEvent(
                java.time.Instant.now(),
                TraceEventType.FUNCTION_CALL,
                "Function call: main",
                Map.of("functionName", "main", "duration", "100ms")
            );

            final TraceExport export = new TraceExport(
                List.of(event),
                new TraceStatistics(1, 0, Duration.ZERO, Map.of(), 0),
                Map.of()
            );

            final String chromeFormat = export.toChromeTraceFormat();

            assertNotNull(chromeFormat);
            assertTrue(chromeFormat.contains("\"traceEvents\""));
            assertTrue(chromeFormat.contains("\"name\""));
            assertTrue(chromeFormat.contains("\"main\""));
        }
    }

    @Nested
    @DisplayName("Memory Analysis Tests")
    class MemoryAnalysisTests {

        @Test
        @DisplayName("Memory inspection result")
        void memoryInspectionResult() {
            final byte[] testData = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
            final MemoryInspection inspection = new MemoryInspection(0x1000, testData);

            assertEquals(0x1000, inspection.getAddress());
            assertEquals(8, inspection.getLength());
            assertArrayEquals(testData, inspection.getData());

            final String hexString = inspection.toHexString();
            assertTrue(hexString.contains("01"));
            assertTrue(hexString.contains("02"));

            // Test typed reads (little-endian)
            final byte[] intData = {0x2A, 0x00, 0x00, 0x00}; // 42 in little-endian
            final MemoryInspection intInspection = new MemoryInspection(0x2000, intData);
            assertEquals(42, intInspection.readInt32(0));
        }

        @Test
        @DisplayName("Memory inspection bounds checking")
        void memoryInspectionBoundsChecking() {
            final byte[] smallData = {0x01, 0x02};
            final MemoryInspection inspection = new MemoryInspection(0x1000, smallData);

            assertThrows(IndexOutOfBoundsException.class, () -> inspection.readInt32(0));
            assertThrows(IndexOutOfBoundsException.class, () -> inspection.readInt64(0));
        }
    }

    @Test
    @DisplayName("Resource cleanup on close")
    void resourceCleanupOnClose() {
        doNothing().when(mockDebugger).addEventListener(any());

        tracer.startTracing();

        assertDoesNotThrow(() -> tracer.close());

        // After close, operations should be safe but not functional
        final List<TraceEvent> events = tracer.getTraceEvents();
        assertTrue(events.isEmpty());
    }

    @Test
    @DisplayName("Concurrent access safety")
    void concurrentAccessSafety() throws InterruptedException {
        doNothing().when(mockDebugger).addEventListener(any());

        tracer.startTracing();

        final int numThreads = 10;
        final java.util.concurrent.CountDownLatch latch =
            new java.util.concurrent.CountDownLatch(numThreads);
        final java.util.List<Exception> exceptions =
            java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        for (int i = 0; i < numThreads; i++) {
            new Thread(() -> {
                try {
                    tracer.getTraceEvents();
                    tracer.getFunctionProfiles();
                    tracer.getStatistics();
                } catch (final Exception e) {
                    exceptions.add(e);
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        assertTrue(latch.await(5, java.util.concurrent.TimeUnit.SECONDS));
        assertTrue(exceptions.isEmpty(), "No exceptions should occur during concurrent access");
    }
}