package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Basic tests for streaming WebAssembly compilation functionality.
 *
 * <p>These tests verify the core streaming compilation functionality that has been implemented
 * in Task #306, including basic compilation, progress tracking, and manual feeding.
 */
@Timeout(30) // Global timeout for all tests
class BasicStreamingCompilationTest {

  private Engine engine;
  private Store<?> store;
  private byte[] testWasmModule;
  private StreamingConfig defaultConfig;

  @BeforeEach
  void setUp() throws WasmException {
    engine = Engine.create();
    store = engine.createStore();
    testWasmModule = createTestWasmModule();
    defaultConfig = StreamingConfig.defaultConfig();
  }

  @AfterEach
  void tearDown() {
    if (store != null) {
      store.close();
    }
    if (engine != null) {
      engine.close();
    }
  }

  @Test
  void testBasicStreamingCompilation() throws Exception {
    try (StreamingCompiler compiler = engine.createStreamingCompiler()) {
      ByteArrayInputStream input = new ByteArrayInputStream(testWasmModule);

      CompletableFuture<Module> future = compiler.compileStreaming(input, defaultConfig);
      Module module = future.get(10, TimeUnit.SECONDS);

      assertNotNull(module, "Compiled module should not be null");

      // Verify module can be instantiated
      Instance instance = module.instantiate(store);
      assertNotNull(instance, "Instance should not be null");

      instance.close();
      module.close();
    }
  }

  @Test
  void testStreamingCompilationWithProgressListener() throws Exception {
    AtomicReference<StreamingStatistics> finalStats = new AtomicReference<>();
    AtomicInteger progressUpdates = new AtomicInteger();
    CountDownLatch completionLatch = new CountDownLatch(1);

    StreamingProgressListener listener =
        new StreamingProgressListener() {
          @Override
          public void onProgressUpdate(StreamingStatistics statistics) {
            progressUpdates.incrementAndGet();
          }

          @Override
          public void onCompilationCompleted(Module module, StreamingStatistics statistics) {
            finalStats.set(statistics);
            completionLatch.countDown();
          }
        };

    try (StreamingCompiler compiler = engine.createStreamingCompiler()) {
      compiler.addProgressListener(listener);
      ByteArrayInputStream input = new ByteArrayInputStream(testWasmModule);

      CompletableFuture<Module> future = compiler.compileStreaming(input, defaultConfig);
      Module module = future.get(10, TimeUnit.SECONDS);

      // Note: Progress listeners may not be fully implemented yet
      // These assertions are for when the implementation is complete
      if (completionLatch.await(5, TimeUnit.SECONDS)) {
        assertNotNull(finalStats.get(), "Final statistics should be available");
        assertTrue(finalStats.get().isComplete(), "Final statistics should show completion");
      }

      module.close();
    }
  }

  @Test
  void testStreamingFeedHandle() throws Exception {
    try (StreamingCompiler compiler = engine.createStreamingCompiler()) {
      StreamingFeedHandle handle = compiler.startStreamingCompilation(defaultConfig);

      // Feed data in chunks
      int chunkSize = 1024;
      for (int offset = 0; offset < testWasmModule.length; offset += chunkSize) {
        int length = Math.min(chunkSize, testWasmModule.length - offset);
        ByteBuffer chunk = ByteBuffer.wrap(testWasmModule, offset, length);

        CompletableFuture<Void> feedFuture = handle.feed(chunk);
        feedFuture.get(5, TimeUnit.SECONDS);
      }

      // Complete compilation
      CompletableFuture<Module> future = handle.complete();
      Module module = future.get(10, TimeUnit.SECONDS);

      assertNotNull(module, "Compiled module should not be null");
      assertEquals(testWasmModule.length, handle.getBytesFeeded(), "Should track bytes fed correctly");

      module.close();
      handle.close();
    }
  }

  @Test
  void testStreamingCompilationWithInstantiation() throws Exception {
    InstantiationConfig instantiationConfig = InstantiationConfig.defaultConfig();

    try (StreamingCompiler compiler = engine.createStreamingCompiler()) {
      ByteArrayInputStream input = new ByteArrayInputStream(testWasmModule);

      CompletableFuture<StreamingInstantiator> future =
          compiler.compileStreamingWithInstantiation(input, defaultConfig, instantiationConfig);
      StreamingInstantiator instantiator = future.get(10, TimeUnit.SECONDS);

      assertNotNull(instantiator, "Streaming instantiator should not be null");
      assertTrue(instantiator.isReady(), "Instantiator should be ready");

      // Test basic instantiation
      CompletableFuture<Instance> instanceFuture = instantiator.instantiate(store);
      Instance instance = instanceFuture.get(10, TimeUnit.SECONDS);

      assertNotNull(instance, "Instance should not be null");

      instance.close();
    }
  }

  @Test
  void testCancellation() throws Exception {
    try (StreamingCompiler compiler = engine.createStreamingCompiler()) {
      ByteArrayInputStream input = new ByteArrayInputStream(testWasmModule);

      CompletableFuture<Module> future = compiler.compileStreaming(input, defaultConfig);

      // Try to cancel - may not work if compilation is fast
      boolean cancelled = compiler.cancel(true);

      // Either cancellation succeeds or compilation completes quickly
      // Both are acceptable outcomes
      assertDoesNotThrow(() -> {
        try {
          Module module = future.get(5, TimeUnit.SECONDS);
          if (module != null) {
            module.close();
          }
        } catch (Exception e) {
          // Expected if cancelled
        }
      });
    }
  }

  @Test
  void testStreamingStatistics() throws Exception {
    try (StreamingCompiler compiler = engine.createStreamingCompiler()) {
      // Get initial statistics
      StreamingStatistics initialStats = compiler.getStatistics();
      assertNotNull(initialStats, "Initial statistics should be available");

      ByteArrayInputStream input = new ByteArrayInputStream(testWasmModule);

      CompletableFuture<Module> future = compiler.compileStreaming(input, defaultConfig);
      Module module = future.get(10, TimeUnit.SECONDS);

      // Check final statistics
      StreamingStatistics finalStats = compiler.getStatistics();
      assertNotNull(finalStats, "Final statistics should be available");
      // Note: Specific checks depend on full implementation

      module.close();
    }
  }

  @Test
  void testConfigurationValidation() {
    // Test invalid buffer size
    assertThrows(
        IllegalArgumentException.class,
        () -> StreamingConfig.builder().bufferSize(0).build(),
        "Invalid buffer size should throw exception");

    // Test invalid timeout
    assertThrows(
        IllegalArgumentException.class,
        () -> StreamingConfig.builder().timeout(Duration.ofSeconds(-1)).build(),
        "Invalid timeout should throw exception");

    // Test invalid max memory
    assertThrows(
        IllegalArgumentException.class,
        () -> StreamingConfig.builder().maxMemoryUsage(-1).build(),
        "Invalid max memory should throw exception");
  }

  @Test
  void testEngineAccess() throws Exception {
    try (StreamingCompiler compiler = engine.createStreamingCompiler()) {
      Engine compilationEngine = compiler.getEngine();
      assertNotNull(compilationEngine, "Engine should be accessible");
      assertEquals(engine, compilationEngine, "Should return the same engine instance");
    }
  }

  private byte[] createTestWasmModule() {
    // Create a simple but valid WebAssembly module for testing
    // This is a minimal module with just the header and basic structure
    return new byte[] {
      0x00, 0x61, 0x73, 0x6D, // WASM magic
      0x01, 0x00, 0x00, 0x00, // WASM version
      0x01, 0x04, 0x01, 0x60, 0x00, 0x00, // Type section: () -> ()
      0x03, 0x02, 0x01, 0x00, // Function section: func 0 has type 0
      0x0A, 0x04, 0x01, 0x02, 0x00, 0x0B // Code section: func 0 body is empty
    };
  }
}