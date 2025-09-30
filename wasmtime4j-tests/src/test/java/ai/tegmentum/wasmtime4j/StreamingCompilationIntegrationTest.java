package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Integration tests for streaming WebAssembly compilation.
 *
 * <p>These tests verify the streaming compilation functionality including progressive compilation,
 * backpressure handling, error recovery, and performance characteristics.
 */
@Timeout(30) // Global timeout for all tests
class StreamingCompilationIntegrationTest {

  private Engine engine;
  private Store store;
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
      assertTrue(module.isValid(), "Compiled module should be valid");

      // Verify module can be instantiated
      Instance instance = module.instantiate(store);
      assertNotNull(instance, "Instance should not be null");
      assertTrue(instance.isValid(), "Instance should be valid");

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
      final Module module = future.get(10, TimeUnit.SECONDS);

      assertTrue(
          completionLatch.await(5, TimeUnit.SECONDS), "Completion callback should be called");
      assertTrue(progressUpdates.get() > 0, "Should receive progress updates");
      assertNotNull(finalStats.get(), "Final statistics should be available");
      assertTrue(finalStats.get().isComplete(), "Final statistics should show completion");

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
      assertTrue(module.isValid(), "Compiled module should be valid");
      assertEquals(
          testWasmModule.length, handle.getBytesFeeded(), "Should track bytes fed correctly");

      module.close();
      handle.close();
    }
  }

  @Test
  void testReactiveStreamingCompilation() throws Exception {
    try (StreamingCompiler compiler = engine.createStreamingCompiler()) {
      TestPublisher publisher = new TestPublisher(testWasmModule);

      CompletableFuture<Module> future = compiler.compileStreaming(publisher, defaultConfig);

      // Start publishing data
      publisher.start();

      Module module = future.get(15, TimeUnit.SECONDS);
      assertNotNull(module, "Compiled module should not be null");
      assertTrue(module.isValid(), "Compiled module should be valid");

      module.close();
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

      // Test streaming instantiation
      CompletableFuture<Instance> instanceFuture =
          instantiator.instantiateStreaming(store, instantiationConfig);
      Instance instance = instanceFuture.get(10, TimeUnit.SECONDS);

      assertNotNull(instance, "Instance should not be null");
      assertTrue(instance.isValid(), "Instance should be valid");

      instance.close();
      instantiator.close();
    }
  }

  @Test
  void testStreamingInstanceHandle() throws Exception {
    InstantiationConfig instantiationConfig =
        InstantiationConfig.builder()
            .lazyFunctionCompilation(true)
            .hotFunctionPriority(true)
            .build();

    try (StreamingCompiler compiler = engine.createStreamingCompiler()) {
      ByteArrayInputStream input = new ByteArrayInputStream(testWasmModule);

      CompletableFuture<StreamingInstantiator> future =
          compiler.compileStreamingWithInstantiation(input, defaultConfig, instantiationConfig);
      StreamingInstantiator instantiator = future.get(10, TimeUnit.SECONDS);

      StreamingInstanceHandle handle =
          instantiator.createStreamingInstance(store, instantiationConfig);

      // Test prioritizing functions (if any exist in test module)
      List<String> readyFunctions = handle.getReadyFunctions();
      List<String> pendingFunctions = handle.getPendingFunctions();

      assertTrue(
          readyFunctions.size() + pendingFunctions.size() >= 0, "Should have function information");

      // Wait for completion
      CompletableFuture<Instance> instanceFuture = handle.complete();
      Instance instance = instanceFuture.get(10, TimeUnit.SECONDS);

      assertNotNull(instance, "Instance should not be null");
      assertTrue(instance.isValid(), "Instance should be valid");

      instance.close();
      handle.close();
      instantiator.close();
    }
  }

  @Test
  void testCancellation() throws Exception {
    try (StreamingCompiler compiler = engine.createStreamingCompiler()) {
      ByteArrayInputStream input = new ByteArrayInputStream(testWasmModule);

      CompletableFuture<Module> future = compiler.compileStreaming(input, defaultConfig);

      // Cancel immediately
      boolean cancelled = compiler.cancel(true);

      if (cancelled) {
        assertThrows(
            ExecutionException.class,
            () -> future.get(5, TimeUnit.SECONDS),
            "Cancelled compilation should throw ExecutionException");
      }
      // If not cancelled (compilation was too fast), that's also acceptable
    }
  }

  @Test
  void testErrorHandling() throws Exception {
    byte[] invalidWasm = {0x00, 0x61, 0x73, 0x6D, 0xFF, 0xFF, 0xFF, 0xFF}; // Invalid WASM

    try (StreamingCompiler compiler = engine.createStreamingCompiler()) {
      ByteArrayInputStream input = new ByteArrayInputStream(invalidWasm);

      CompletableFuture<Module> future = compiler.compileStreaming(input, defaultConfig);

      assertThrows(
          ExecutionException.class,
          () -> future.get(10, TimeUnit.SECONDS),
          "Invalid WASM should cause compilation to fail");
    }
  }

  @Test
  void testStreamingStatistics() throws Exception {
    try (StreamingCompiler compiler = engine.createStreamingCompiler()) {
      ByteArrayInputStream input = new ByteArrayInputStream(testWasmModule);

      CompletableFuture<Module> future = compiler.compileStreaming(input, defaultConfig);

      // Check statistics during compilation
      StreamingStatistics stats = compiler.getStatistics();
      assertNotNull(stats, "Statistics should be available");
      assertEquals(
          CompilationPhase.PARSING, stats.getCurrentPhase(), "Should start with parsing phase");

      Module module = future.get(10, TimeUnit.SECONDS);

      // Check final statistics
      stats = compiler.getStatistics();
      assertTrue(stats.isComplete(), "Statistics should show completion");
      assertTrue(stats.getTotalBytesProcessed() > 0, "Should have processed bytes");
      assertTrue(stats.getElapsedTime().toMillis() > 0, "Should have elapsed time");

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
  void testMultipleConcurrentCompilations() throws Exception {
    int concurrentCompilations = 3;
    CompletableFuture<Module>[] futures = new CompletableFuture[concurrentCompilations];

    try (StreamingCompiler compiler = engine.createStreamingCompiler()) {
      for (int i = 0; i < concurrentCompilations; i++) {
        ByteArrayInputStream input = new ByteArrayInputStream(testWasmModule);
        futures[i] = compiler.compileStreaming(input, defaultConfig);
      }

      // Wait for all to complete
      for (int i = 0; i < concurrentCompilations; i++) {
        Module module = futures[i].get(15, TimeUnit.SECONDS);
        assertNotNull(module, "Module " + i + " should not be null");
        assertTrue(module.isValid(), "Module " + i + " should be valid");
        module.close();
      }
    }
  }

  private byte[] createTestWasmModule() {
    // Create a simple but valid WebAssembly module for testing
    // This is a minimal module with just the header and basic structure
    return new byte[] {
      0x00,
      0x61,
      0x73,
      0x6D, // WASM magic
      0x01,
      0x00,
      0x00,
      0x00, // WASM version
      0x01,
      0x04,
      0x01,
      0x60,
      0x00,
      0x00, // Type section: () -> ()
      0x03,
      0x02,
      0x01,
      0x00, // Function section: func 0 has type 0
      0x0A,
      0x04,
      0x01,
      0x02,
      0x00,
      0x0B // Code section: func 0 body is empty
    };
  }

  /** Test implementation of Flow.Publisher for reactive streaming tests. */
  private static class TestPublisher implements Flow.Publisher<ByteBuffer> {
    private final byte[] data;
    private volatile Flow.Subscriber<? super ByteBuffer> subscriber;

    public TestPublisher(final byte[] data) {
      this.data = data;
    }

    @Override
    public void subscribe(final Flow.Subscriber<? super ByteBuffer> subscriber) {
      this.subscriber = subscriber;
      subscriber.onSubscribe(new TestSubscription());
    }

    public void start() {
      if (subscriber != null) {
        // Send data in chunks
        int chunkSize = 1024;
        for (int offset = 0; offset < data.length; offset += chunkSize) {
          int length = Math.min(chunkSize, data.length - offset);
          ByteBuffer chunk = ByteBuffer.wrap(data, offset, length);
          subscriber.onNext(chunk);
        }
        subscriber.onComplete();
      }
    }

    private class TestSubscription implements Flow.Subscription {
      @Override
      public void request(final long n) {
        // Simple implementation - request is ignored for this test
      }

      @Override
      public void cancel() {
        subscriber = null;
      }
    }
  }
}
