package ai.tegmentum.wasmtime4j.tests.async;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.async.reactive.CompilationEvent;
import ai.tegmentum.wasmtime4j.async.reactive.CompilationPhase;
import ai.tegmentum.wasmtime4j.async.reactive.EngineEvent;
import ai.tegmentum.wasmtime4j.async.reactive.ReactiveEngine;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Integration tests for reactive WebAssembly engine functionality.
 *
 * <p>These tests validate the reactive programming patterns, event streams, and Project Reactor
 * integration of the ReactiveEngine implementation.
 *
 * @since 1.0.0
 */
@EnabledIfSystemProperty(named = "test.reactive", matches = "true")
class ReactiveEngineIntegrationTest {

  private ReactiveEngine reactiveEngine;

  @BeforeEach
  void setUp() throws Exception {
    // Create reactive engine using the runtime factory
    final WasmRuntimeFactory factory = WasmRuntimeFactory.create();

    // Try to create a ReactiveEngine - this will work with implementations that support it
    try {
      if (factory.createEngine() instanceof ReactiveEngine) {
        reactiveEngine = (ReactiveEngine) factory.createEngine();
      } else {
        // Skip tests if reactive engine is not available
        org.junit.jupiter.api.Assumptions.assumeTrue(
            false, "ReactiveEngine not available in current runtime");
      }
    } catch (Exception e) {
      org.junit.jupiter.api.Assumptions.assumeTrue(
          false, "Failed to create ReactiveEngine: " + e.getMessage());
    }
  }

  @Test
  @Timeout(30)
  void testCompilationEventStream() throws Exception {
    assertNotNull(reactiveEngine, "ReactiveEngine should be available");

    // Create module sources for reactive compilation
    final Flux<ReactiveEngine.ModuleSource> moduleStream =
        Flux.just(
            createModuleSource("module1", createSimpleWasmBytes()),
            createModuleSource("module2", createSimpleWasmBytes()));

    final Flux<CompilationEvent> compilationEvents = reactiveEngine.compileReactive(moduleStream);
    assertNotNull(compilationEvents, "Compilation event stream should not be null");

    // Use StepVerifier to test the reactive stream
    StepVerifier.create(compilationEvents)
        .expectNextMatches(
            event -> {
              assertNotNull(event.getModuleId(), "Module ID should not be null");
              assertNotNull(event.getPhase(), "Compilation phase should not be null");
              assertTrue(
                  event.getProgress() >= 0.0 && event.getProgress() <= 100.0,
                  "Progress should be between 0 and 100");
              return true;
            })
        .expectNextCount(1) // Expect at least one more event
        .verifyTimeout(Duration.ofSeconds(10));
  }

  @Test
  @Timeout(30)
  void testEngineEventStream() throws Exception {
    assertNotNull(reactiveEngine, "ReactiveEngine should be available");

    final Flux<EngineEvent> eventStream = reactiveEngine.getEventStream();
    assertNotNull(eventStream, "Engine event stream should not be null");

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<EngineEvent> capturedEvent = new AtomicReference<>();

    // Subscribe to the event stream
    final var subscription =
        eventStream
            .take(1) // Take only the first event
            .subscribe(
                event -> {
                  capturedEvent.set(event);
                  latch.countDown();
                });

    // Perform some operation to generate events
    final var future = reactiveEngine.compileModuleAsync(createSimpleWasmBytes());

    // Wait for an event
    assertTrue(latch.await(10, TimeUnit.SECONDS), "Should receive an engine event");

    final EngineEvent event = capturedEvent.get();
    assertNotNull(event, "Captured event should not be null");
    assertNotNull(event.getEventId(), "Event ID should not be null");
    assertNotNull(event.getEngineId(), "Engine ID should not be null");
    assertNotNull(event.getEventType(), "Event type should not be null");
    assertNotNull(event.getTimestamp(), "Event timestamp should not be null");

    subscription.dispose();
    if (future.isDone() && !future.isCompletedExceptionally()) {
      future.get().close();
    }
  }

  @Test
  @Timeout(30)
  void testFilteredEngineEventStream() throws Exception {
    assertNotNull(reactiveEngine, "ReactiveEngine should be available");

    // Filter for error events only
    final Flux<EngineEvent> errorEventStream =
        reactiveEngine.getEventStream(event -> event.isError());
    assertNotNull(errorEventStream, "Filtered event stream should not be null");

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicInteger eventCount = new AtomicInteger(0);

    // Subscribe to error events
    final var subscription =
        errorEventStream
            .take(Duration.ofSeconds(5))
            .subscribe(
                event -> {
                  assertTrue(event.isError(), "Filtered event should be an error event");
                  eventCount.incrementAndGet();
                },
                error -> latch.countDown(),
                () -> latch.countDown());

    // Try to cause an error by compiling invalid WASM
    final byte[] invalidWasm = {0x00, 0x61, 0x73, 0x6d}; // Invalid WASM
    try {
      reactiveEngine.compileModuleAsync(invalidWasm).get(5, TimeUnit.SECONDS);
    } catch (Exception e) {
      // Expected for invalid WASM
    }

    assertTrue(latch.await(10, TimeUnit.SECONDS), "Should complete subscription");
    subscription.dispose();
  }

  @Test
  @Timeout(30)
  void testPerformanceMetricsStream() throws Exception {
    assertNotNull(reactiveEngine, "ReactiveEngine should be available");

    final Flux<ReactiveEngine.PerformanceMetrics> metricsStream =
        reactiveEngine.getMetricsStream(Duration.ofMillis(100));
    assertNotNull(metricsStream, "Performance metrics stream should not be null");

    final CountDownLatch latch = new CountDownLatch(3); // Wait for 3 metrics updates
    final AtomicInteger metricsCount = new AtomicInteger(0);

    // Subscribe to metrics updates
    final var subscription =
        metricsStream
            .take(3)
            .subscribe(
                metrics -> {
                  assertNotNull(metrics, "Performance metrics should not be null");
                  assertTrue(
                      metrics.getAverageCompilationTime() >= 0,
                      "Average compilation time should be non-negative");
                  assertTrue(
                      metrics.getCpuUsage() >= 0 && metrics.getCpuUsage() <= 100,
                      "CPU usage should be between 0 and 100");
                  metricsCount.incrementAndGet();
                  latch.countDown();
                });

    // Perform some operations to generate metrics
    for (int i = 0; i < 3; i++) {
      reactiveEngine.compileModuleAsync(createSimpleWasmBytes());
      Thread.sleep(50);
    }

    assertTrue(latch.await(10, TimeUnit.SECONDS), "Should receive performance metrics");
    assertEquals(3, metricsCount.get(), "Should have received 3 metrics updates");

    subscription.dispose();
  }

  @Test
  @Timeout(30)
  void testReactiveEngineStatistics() throws Exception {
    assertNotNull(reactiveEngine, "ReactiveEngine should be available");

    final Mono<ai.tegmentum.wasmtime4j.EngineStatistics> statsMono =
        reactiveEngine.getStatisticsReactive();
    assertNotNull(statsMono, "Statistics Mono should not be null");

    // Use StepVerifier to test the Mono
    StepVerifier.create(statsMono)
        .expectNextMatches(
            stats -> {
              assertNotNull(stats, "Engine statistics should not be null");
              return true;
            })
        .verifyComplete();
  }

  @Test
  @Timeout(30)
  void testHealthStatusMonitoring() throws Exception {
    assertNotNull(reactiveEngine, "ReactiveEngine should be available");

    final Mono<ReactiveEngine.EngineHealth> healthMono = reactiveEngine.getHealthStatus();
    assertNotNull(healthMono, "Health status Mono should not be null");

    // Test current health status
    StepVerifier.create(healthMono)
        .expectNextMatches(
            health -> {
              assertNotNull(health, "Health status should not be null");
              assertNotNull(health.getStatus(), "Health status should have a status");
              assertTrue(
                  health.getHealthScore() >= 0.0 && health.getHealthScore() <= 100.0,
                  "Health score should be between 0 and 100");
              assertNotNull(health.getCheckTime(), "Health check time should not be null");
              return true;
            })
        .verifyComplete();
  }

  @Test
  @Timeout(30)
  void testHealthStatusStream() throws Exception {
    assertNotNull(reactiveEngine, "ReactiveEngine should be available");

    final Flux<ReactiveEngine.EngineHealth> healthStream =
        reactiveEngine.getHealthStream(Duration.ofMillis(200));
    assertNotNull(healthStream, "Health status stream should not be null");

    final CountDownLatch latch = new CountDownLatch(2);
    final AtomicInteger healthUpdates = new AtomicInteger(0);

    // Subscribe to health updates
    final var subscription =
        healthStream
            .take(2)
            .subscribe(
                health -> {
                  assertNotNull(health, "Health update should not be null");
                  assertNotNull(health.getStatus(), "Health status should not be null");
                  healthUpdates.incrementAndGet();
                  latch.countDown();
                });

    assertTrue(latch.await(5, TimeUnit.SECONDS), "Should receive health updates");
    assertEquals(2, healthUpdates.get(), "Should have received 2 health updates");

    subscription.dispose();
  }

  @Test
  @Timeout(30)
  void testGracefulShutdown() throws Exception {
    assertNotNull(reactiveEngine, "ReactiveEngine should be available");

    final Mono<Void> shutdownMono = reactiveEngine.shutdownGracefully(Duration.ofSeconds(5));
    assertNotNull(shutdownMono, "Shutdown Mono should not be null");

    // Test graceful shutdown
    StepVerifier.create(shutdownMono).verifyComplete();

    // After shutdown, the engine should not accept new operations
    assertThrows(
        Exception.class,
        () -> {
          reactiveEngine.compileModuleAsync(createSimpleWasmBytes()).get(1, TimeUnit.SECONDS);
        },
        "Should reject operations after shutdown");
  }

  @Test
  @Timeout(30)
  void testCompilationEventTypes() throws Exception {
    // Test individual compilation event creation and properties
    final CompilationEvent phaseEvent =
        CompilationEvent.phaseTransition(
            "test-module", CompilationPhase.COMPILATION, Duration.ofMillis(100));

    assertNotNull(phaseEvent, "Phase transition event should not be null");
    assertEquals("test-module", phaseEvent.getModuleId(), "Module ID should match");
    assertEquals(CompilationPhase.COMPILATION, phaseEvent.getPhase(), "Phase should match");
    assertTrue(phaseEvent.isPhaseTransition(), "Should be a phase transition");
    assertFalse(phaseEvent.isProgressUpdate(), "Should not be a progress update");

    final CompilationEvent progressEvent =
        CompilationEvent.progressUpdate(
            "test-module", CompilationPhase.OPTIMIZATION, 75.0, Duration.ofMillis(500), 1000, 2000);

    assertNotNull(progressEvent, "Progress update event should not be null");
    assertEquals(75.0, progressEvent.getProgress(), 0.1, "Progress should match");
    assertEquals(1000, progressEvent.getBytesProcessed(), "Bytes processed should match");
    assertEquals(2000, progressEvent.getTotalBytes(), "Total bytes should match");
    assertTrue(progressEvent.isProgressUpdate(), "Should be a progress update");
    assertFalse(progressEvent.isPhaseTransition(), "Should not be a phase transition");

    final CompilationEvent completedEvent =
        CompilationEvent.completed("test-module", Duration.ofSeconds(2));

    assertNotNull(completedEvent, "Completed event should not be null");
    assertTrue(completedEvent.isCompleted(), "Should be completed");
    assertFalse(completedEvent.isFailed(), "Should not be failed");
    assertEquals(100.0, completedEvent.getProgress(), 0.1, "Progress should be 100%");

    final Exception testError = new RuntimeException("Test compilation error");
    final CompilationEvent failedEvent =
        CompilationEvent.failed("test-module", testError, Duration.ofMillis(750));

    assertNotNull(failedEvent, "Failed event should not be null");
    assertTrue(failedEvent.isFailed(), "Should be failed");
    assertFalse(failedEvent.isCompleted(), "Should not be completed");
    assertTrue(failedEvent.getError().isPresent(), "Should have error");
    assertEquals(testError, failedEvent.getError().get(), "Error should match");
  }

  // Helper methods

  private ReactiveEngine.ModuleSource createModuleSource(final String id, final byte[] wasmBytes) {
    return new ReactiveEngine.ModuleSource() {
      @Override
      public String getModuleId() {
        return id;
      }

      @Override
      public byte[] getWasmBytes() {
        return wasmBytes;
      }

      @Override
      public ReactiveEngine.CompilationMetadata getMetadata() {
        return new ReactiveEngine.CompilationMetadata() {
          @Override
          public String getOptimizationLevel() {
            return "default";
          }

          @Override
          public boolean isDebugEnabled() {
            return false;
          }

          @Override
          public String getTargetPlatform() {
            return null;
          }
        };
      }
    };
  }

  private byte[] createSimpleWasmBytes() {
    // Return a minimal valid WASM module for testing
    return new byte[] {
      0x00, 0x61, 0x73, 0x6d, // WASM magic
      0x01, 0x00, 0x00, 0x00 // WASM version
    };
  }
}
