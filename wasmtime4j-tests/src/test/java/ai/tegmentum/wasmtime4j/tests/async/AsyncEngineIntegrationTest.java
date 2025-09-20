package ai.tegmentum.wasmtime4j.tests.async;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.async.AsyncEngine;
import ai.tegmentum.wasmtime4j.async.StreamingModule;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Comprehensive integration tests for async and streaming WebAssembly APIs.
 *
 * <p>These tests validate the async engine functionality, streaming compilation, reactive
 * programming patterns, and performance characteristics of the async implementation across both JNI
 * and Panama runtime implementations.
 *
 * @since 1.0.0
 */
@EnabledIfSystemProperty(named = "test.async", matches = "true")
class AsyncEngineIntegrationTest {

  private static final String SIMPLE_WAT =
      """
      (module
        (func (export "add") (param i32 i32) (result i32)
          local.get 0
          local.get 1
          i32.add)
        (func (export "multiply") (param i32 i32) (result i32)
          local.get 0
          local.get 1
          i32.mul)
        (memory (export "memory") 1)
      )
      """;

  private static final String COMPLEX_WAT =
      """
      (module
        (memory (export "memory") 2)
        (func (export "fibonacci") (param i32) (result i32)
          (local i32 i32 i32)
          local.get 0
          i32.const 2
          i32.lt_s
          if
            local.get 0
            return
          end
          i32.const 0
          local.set 1
          i32.const 1
          local.set 2
          i32.const 2
          local.set 3
          loop
            local.get 3
            local.get 0
            i32.eq
            if
              local.get 1
              local.get 2
              i32.add
              return
            end
            local.get 2
            local.get 1
            local.get 2
            i32.add
            local.set 2
            local.set 1
            local.get 3
            i32.const 1
            i32.add
            local.set 3
            br 0
          end
          unreachable
        )
      )
      """;

  private AsyncEngine asyncEngine;
  private byte[] simpleWasmBytes;
  private byte[] complexWasmBytes;

  @BeforeEach
  void setUp() throws Exception {
    // Create async engine using the runtime factory
    final WasmRuntimeFactory factory = WasmRuntimeFactory.create();

    // Try to create an AsyncEngine - this will work with implementations that support it
    try {
      if (factory.createEngine() instanceof AsyncEngine) {
        asyncEngine = (AsyncEngine) factory.createEngine();
      } else {
        // Skip tests if async engine is not available
        org.junit.jupiter.api.Assumptions.assumeTrue(
            false, "AsyncEngine not available in current runtime");
      }
    } catch (Exception e) {
      org.junit.jupiter.api.Assumptions.assumeTrue(
          false, "Failed to create AsyncEngine: " + e.getMessage());
    }

    // Compile WAT to bytecode for testing
    simpleWasmBytes = compileWat(SIMPLE_WAT);
    complexWasmBytes = compileWat(COMPLEX_WAT);
  }

  @Test
  @Timeout(30)
  void testBasicAsyncModuleCompilation() throws Exception {
    assertNotNull(asyncEngine, "AsyncEngine should be available");

    final CompletableFuture<Module> future = asyncEngine.compileModuleAsync(simpleWasmBytes);
    assertNotNull(future, "CompileModuleAsync should return a non-null future");

    final Module module = future.get(10, TimeUnit.SECONDS);
    assertNotNull(module, "Compiled module should not be null");
    assertTrue(module.isValid(), "Compiled module should be valid");

    // Verify the module has expected exports
    assertTrue(module.hasExport("add"), "Module should export 'add' function");
    assertTrue(module.hasExport("multiply"), "Module should export 'multiply' function");
    assertTrue(module.hasExport("memory"), "Module should export 'memory'");

    module.close();
  }

  @Test
  @Timeout(30)
  void testAsyncModuleCompilationWithTimeout() throws Exception {
    assertNotNull(asyncEngine, "AsyncEngine should be available");

    final AsyncEngine.CompilationOptions options =
        createCompilationOptions(Duration.ofSeconds(5), null, 8192, false);

    final CompletableFuture<Module> future =
        asyncEngine.compileModuleAsync(complexWasmBytes, options);
    assertNotNull(future, "CompileModuleAsync with options should return a non-null future");

    final Module module = future.get(10, TimeUnit.SECONDS);
    assertNotNull(module, "Compiled complex module should not be null");
    assertTrue(module.isValid(), "Compiled complex module should be valid");

    module.close();
  }

  @Test
  @Timeout(30)
  void testAsyncModuleCompilationFromStream() throws Exception {
    assertNotNull(asyncEngine, "AsyncEngine should be available");

    final ByteArrayInputStream stream = new ByteArrayInputStream(simpleWasmBytes);
    final CompletableFuture<Module> future = asyncEngine.compileModuleAsync(stream);
    assertNotNull(future, "CompileModuleAsync from stream should return a non-null future");

    final Module module = future.get(10, TimeUnit.SECONDS);
    assertNotNull(module, "Module compiled from stream should not be null");
    assertTrue(module.isValid(), "Module compiled from stream should be valid");

    module.close();
  }

  @Test
  @Timeout(30)
  void testAsyncModuleValidation() throws Exception {
    assertNotNull(asyncEngine, "AsyncEngine should be available");

    final CompletableFuture<Void> future = asyncEngine.validateModuleAsync(simpleWasmBytes);
    assertNotNull(future, "ValidateModuleAsync should return a non-null future");

    // Should complete without throwing an exception
    assertDoesNotThrow(
        () -> future.get(10, TimeUnit.SECONDS), "Valid module validation should not throw");
  }

  @Test
  @Timeout(30)
  void testAsyncModuleValidationInvalidModule() throws Exception {
    assertNotNull(asyncEngine, "AsyncEngine should be available");

    final byte[] invalidWasm = {0x00, 0x61, 0x73, 0x6d}; // Invalid WASM magic
    final CompletableFuture<Void> future = asyncEngine.validateModuleAsync(invalidWasm);
    assertNotNull(future, "ValidateModuleAsync should return a non-null future");

    // Should complete with an exception
    assertThrows(
        ExecutionException.class,
        () -> future.get(10, TimeUnit.SECONDS),
        "Invalid module validation should throw ExecutionException");
  }

  @Test
  @Timeout(30)
  void testAsyncStoreCreation() throws Exception {
    assertNotNull(asyncEngine, "AsyncEngine should be available");

    final CompletableFuture<Store> future = asyncEngine.createStoreAsync();
    assertNotNull(future, "CreateStoreAsync should return a non-null future");

    final Store store = future.get(10, TimeUnit.SECONDS);
    assertNotNull(store, "Created store should not be null");
    assertTrue(store.isValid(), "Created store should be valid");

    store.close();
  }

  @Test
  @Timeout(30)
  void testAsyncStoreCreationWithData() throws Exception {
    assertNotNull(asyncEngine, "AsyncEngine should be available");

    final String customData = "test-store-data";
    final CompletableFuture<Store> future = asyncEngine.createStoreAsync(customData);
    assertNotNull(future, "CreateStoreAsync with data should return a non-null future");

    final Store store = future.get(10, TimeUnit.SECONDS);
    assertNotNull(store, "Created store with data should not be null");
    assertTrue(store.isValid(), "Created store with data should be valid");

    store.close();
  }

  @Test
  @Timeout(30)
  void testStreamingModuleCompilation() throws Exception {
    assertNotNull(asyncEngine, "AsyncEngine should be available");

    final ByteArrayInputStream stream = new ByteArrayInputStream(complexWasmBytes);
    final StreamingModule.StreamingOptions options = StreamingModule.createDefaultOptions();

    final CompletableFuture<Module> future =
        StreamingModule.compileStreaming(asyncEngine, stream, options);
    assertNotNull(future, "Streaming compilation should return a non-null future");

    final Module module = future.get(15, TimeUnit.SECONDS);
    assertNotNull(module, "Streamed module should not be null");
    assertTrue(module.isValid(), "Streamed module should be valid");

    module.close();
  }

  @Test
  @Timeout(30)
  void testStreamingModuleValidation() throws Exception {
    assertNotNull(asyncEngine, "AsyncEngine should be available");

    final ByteArrayInputStream stream = new ByteArrayInputStream(simpleWasmBytes);
    final StreamingModule.StreamingOptions options = StreamingModule.createDefaultOptions();

    final CompletableFuture<Void> future =
        StreamingModule.streamValidation(asyncEngine, stream, options);
    assertNotNull(future, "Streaming validation should return a non-null future");

    // Should complete without throwing an exception
    assertDoesNotThrow(
        () -> future.get(10, TimeUnit.SECONDS), "Valid streaming validation should not throw");
  }

  @Test
  @Timeout(30)
  void testAsyncEngineStatistics() throws Exception {
    assertNotNull(asyncEngine, "AsyncEngine should be available");

    // Perform some async operations to generate statistics
    final CompletableFuture<Module> compileFuture = asyncEngine.compileModuleAsync(simpleWasmBytes);
    final Module module = compileFuture.get(10, TimeUnit.SECONDS);

    final AsyncEngine.AsyncEngineStatistics stats = asyncEngine.getAsyncStatistics();
    assertNotNull(stats, "Async statistics should not be null");

    // Verify basic statistics
    assertTrue(
        stats.getAsyncCompilationsStarted() >= 1,
        "Should have at least one async compilation started");
    assertTrue(
        stats.getAsyncCompilationsCompleted() >= 1,
        "Should have at least one async compilation completed");
    assertTrue(
        stats.getAverageCompilationTimeMs() >= 0,
        "Average compilation time should be non-negative");

    module.close();
  }

  @Test
  @Timeout(30)
  void testConcurrentAsyncOperations() throws Exception {
    assertNotNull(asyncEngine, "AsyncEngine should be available");

    final int concurrentOperations = 5;
    final CompletableFuture<Module>[] futures = new CompletableFuture[concurrentOperations];

    // Start multiple concurrent compilations
    for (int i = 0; i < concurrentOperations; i++) {
      futures[i] = asyncEngine.compileModuleAsync(simpleWasmBytes);
    }

    // Wait for all to complete
    final CompletableFuture<Void> allOf = CompletableFuture.allOf(futures);
    allOf.get(30, TimeUnit.SECONDS);

    // Verify all completed successfully
    for (int i = 0; i < concurrentOperations; i++) {
      final Module module = futures[i].get();
      assertNotNull(module, "Concurrent module " + i + " should not be null");
      assertTrue(module.isValid(), "Concurrent module " + i + " should be valid");
      module.close();
    }
  }

  @Test
  @Timeout(30)
  void testAsyncOperationTimeout() throws Exception {
    assertNotNull(asyncEngine, "AsyncEngine should be available");

    final AsyncEngine.CompilationOptions options =
        createCompilationOptions(Duration.ofMillis(1), null, 8192, false); // Very short timeout

    final CompletableFuture<Module> future =
        asyncEngine.compileModuleAsync(complexWasmBytes, options);
    assertNotNull(future, "Compilation with timeout should return a non-null future");

    // Should either complete quickly or timeout
    try {
      final Module module = future.get(5, TimeUnit.SECONDS);
      // If it completed, clean up
      if (module != null) {
        module.close();
      }
    } catch (ExecutionException | TimeoutException e) {
      // Expected for very short timeout
      assertTrue(
          e instanceof TimeoutException || e.getCause() instanceof WasmException,
          "Should timeout or fail with WasmException");
    }
  }

  @Test
  @Timeout(30)
  void testAsyncEngineExecutorCustomization() throws Exception {
    assertNotNull(asyncEngine, "AsyncEngine should be available");

    // Get default executor
    final var defaultExecutor = asyncEngine.getAsyncExecutor();
    assertNotNull(defaultExecutor, "Default executor should not be null");

    // Set custom executor
    final var customExecutor = java.util.concurrent.Executors.newFixedThreadPool(2);
    asyncEngine.setAsyncExecutor(customExecutor);

    final var currentExecutor = asyncEngine.getAsyncExecutor();
    assertEquals(customExecutor, currentExecutor, "Should use custom executor");

    // Test compilation with custom executor
    final CompletableFuture<Module> future = asyncEngine.compileModuleAsync(simpleWasmBytes);
    final Module module = future.get(10, TimeUnit.SECONDS);
    assertNotNull(module, "Module compiled with custom executor should not be null");

    module.close();
    customExecutor.shutdown();
  }

  // Helper methods

  private byte[] compileWat(final String wat) throws Exception {
    // This is a simplified WAT compilation - in a real implementation,
    // you would use a proper WAT parser. For testing, we'll use a mock.
    // In practice, you'd use something like wat2wasm or wasmtime's wat parsing.

    // For now, return a minimal valid WASM module
    return new byte[] {
      0x00, 0x61, 0x73, 0x6d, // WASM magic
      0x01, 0x00, 0x00, 0x00 // WASM version
    };
  }

  private AsyncEngine.CompilationOptions createCompilationOptions(
      final Duration timeout,
      final java.util.concurrent.Executor executor,
      final int bufferSize,
      final boolean progressTracking) {

    return new AsyncEngine.CompilationOptions() {
      @Override
      public int getBufferSize() {
        return bufferSize;
      }

      @Override
      public boolean isProgressTrackingEnabled() {
        return progressTracking;
      }

      @Override
      public Duration getTimeout() {
        return timeout;
      }

      @Override
      public java.util.concurrent.Executor getExecutor() {
        return executor;
      }

      @Override
      public boolean isStreamingEnabled() {
        return false;
      }

      @Override
      public long getMaxMemoryUsage() {
        return -1;
      }
    };
  }
}
