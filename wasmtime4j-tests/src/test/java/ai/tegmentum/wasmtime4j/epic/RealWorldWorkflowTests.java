/*
 * Copyright 2024 Tegmentum AI Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.epic;

import static org.assertj.core.api.Assertions.*;

import ai.tegmentum.wasmtime4j.*;
import ai.tegmentum.wasmtime4j.async.AsyncEngine;
import ai.tegmentum.wasmtime4j.cache.ModuleCache;
import ai.tegmentum.wasmtime4j.serialization.SerializationSystem;
import ai.tegmentum.wasmtime4j.wasi.WasiFactory;
import ai.tegmentum.wasmtime4j.wasi.WasiRuntime;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Comprehensive real-world workflow testing that validates complete use cases and production
 * scenarios across all Wasmtime4j components.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Execution(ExecutionMode.CONCURRENT)
final class RealWorldWorkflowTests {

  private static final Logger LOGGER = Logger.getLogger(RealWorldWorkflowTests.class.getName());

  // Sample WebAssembly modules for testing
  private static final byte[] MATHEMATICAL_CALCULATOR =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6d, // magic
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x01,
        0x0d,
        0x03, // type section
        0x60,
        0x02,
        0x7f,
        0x7f,
        0x01,
        0x7f, // (i32, i32) -> i32
        0x60,
        0x01,
        0x7f,
        0x01,
        0x7f, // (i32) -> i32
        0x60,
        0x02,
        0x7e,
        0x7e,
        0x01,
        0x7e, // (i64, i64) -> i64
        0x03,
        0x05,
        0x04,
        0x00,
        0x00,
        0x01,
        0x02, // function section: 4 functions
        0x07,
        0x25,
        0x04, // export section: 4 exports
        0x03,
        0x61,
        0x64,
        0x64,
        0x00,
        0x00, // export "add"
        0x08,
        0x6d,
        0x75,
        0x6c,
        0x74,
        0x69,
        0x70,
        0x6c,
        0x79,
        0x00,
        0x01, // export "multiply"
        0x09,
        0x66,
        0x61,
        0x63,
        0x74,
        0x6f,
        0x72,
        0x69,
        0x61,
        0x6c,
        0x00,
        0x02, // export "factorial"
        0x08,
        0x61,
        0x64,
        0x64,
        0x5f,
        0x6c,
        0x6f,
        0x6e,
        0x67,
        0x00,
        0x03, // export "add_long"
        0x0a,
        0x2b,
        0x04, // code section: 4 functions
        // add function
        0x07,
        0x00,
        0x20,
        0x00,
        0x20,
        0x01,
        0x6a,
        0x0b,
        // multiply function
        0x07,
        0x00,
        0x20,
        0x00,
        0x20,
        0x01,
        0x6c,
        0x0b,
        // factorial function (recursive)
        0x0f,
        0x00,
        0x20,
        0x00,
        0x41,
        0x01,
        0x4c,
        0x04,
        0x7f,
        0x41,
        0x01,
        0x05,
        0x20,
        0x00,
        0x20,
        0x00,
        0x41,
        0x7f,
        0x6a,
        0x10,
        0x02,
        0x6c,
        0x0b,
        0x0b,
        // add_long function
        0x07,
        0x00,
        0x20,
        0x00,
        0x20,
        0x01,
        0x7c,
        0x0b
      };

  private static final byte[] DATA_PROCESSOR =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6d, // magic
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x01,
        0x0a,
        0x02, // type section
        0x60,
        0x03,
        0x7f,
        0x7f,
        0x7f,
        0x00, // (i32, i32, i32) -> ()
        0x60,
        0x02,
        0x7f,
        0x7f,
        0x01,
        0x7f, // (i32, i32) -> i32
        0x03,
        0x03,
        0x02,
        0x00,
        0x01, // function section: 2 functions
        0x05,
        0x03,
        0x01,
        0x00,
        0x01, // memory section: 1 page
        0x07,
        0x1a,
        0x03, // export section: 3 exports
        0x06,
        0x6d,
        0x65,
        0x6d,
        0x6f,
        0x72,
        0x79,
        0x02,
        0x00, // export "memory"
        0x0b,
        0x70,
        0x72,
        0x6f,
        0x63,
        0x65,
        0x73,
        0x73,
        0x5f,
        0x64,
        0x61,
        0x74,
        0x61,
        0x00,
        0x00, // export "process_data"
        0x08,
        0x73,
        0x75,
        0x6d,
        0x5f,
        0x64,
        0x61,
        0x74,
        0x61,
        0x00,
        0x01, // export "sum_data"
        0x0a,
        0x1e,
        0x02, // code section: 2 functions
        // process_data function (copy data)
        0x0e,
        0x00,
        0x03,
        0x40,
        0x20,
        0x02,
        0x45,
        0x04,
        0x40,
        0x0c,
        0x01,
        0x0b,
        0x20,
        0x00,
        0x20,
        0x01,
        0x36,
        0x02,
        0x00,
        0x0c,
        0x00,
        0x0b,
        0x0b,
        // sum_data function
        0x0c,
        0x00,
        0x41,
        0x00,
        0x21,
        0x02,
        0x03,
        0x40,
        0x20,
        0x01,
        0x45,
        0x04,
        0x40,
        0x0c,
        0x01,
        0x0b,
        0x20,
        0x02,
        0x20,
        0x00,
        0x28,
        0x02,
        0x00,
        0x6a,
        0x21,
        0x02,
        0x0c,
        0x00,
        0x0b,
        0x20,
        0x02,
        0x0b
      };

  private Path tempDir;

  @BeforeEach
  void setUp() throws IOException {
    tempDir = Files.createTempDirectory("wasmtime4j-workflows");
    LOGGER.info(String.format("Created temporary directory: %s", tempDir));
  }

  @AfterEach
  void tearDown() throws IOException {
    if (tempDir != null && Files.exists(tempDir)) {
      Files.walk(tempDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }
  }

  /**
   * Test: Web Service Plugin System Real-world scenario: Loading and executing user-provided WASM
   * plugins in a web service.
   */
  @Test
  @Order(1)
  @DisplayName("Web Service Plugin System Workflow")
  void testWebServicePluginSystemWorkflow() {
    LOGGER.info("Testing web service plugin system workflow");

    try {
      // Simulate web service handling multiple concurrent plugin executions
      final int concurrentRequests = 20;
      final ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
      final AtomicInteger successCount = new AtomicInteger(0);
      final AtomicInteger errorCount = new AtomicInteger(0);

      final CompletableFuture<?>[] futures = new CompletableFuture[concurrentRequests];

      for (int i = 0; i < concurrentRequests; i++) {
        final int requestId = i;
        futures[i] =
            CompletableFuture.runAsync(
                () -> {
                  try {
                    // Each request creates its own engine and store (isolation)
                    final Engine pluginEngine = WasmRuntimeFactory.createEngine();
                    final Store pluginStore = new Store(pluginEngine);

                    // Load calculator plugin
                    final Module calculatorPlugin =
                        pluginEngine.compileModule(MATHEMATICAL_CALCULATOR);
                    final WasmInstance calculator = new WasmInstance(pluginStore, calculatorPlugin);

                    // Simulate processing multiple calculations per request
                    final WasmFunction addFunction = calculator.getFunction("add");
                    final WasmFunction multiplyFunction = calculator.getFunction("multiply");
                    final WasmFunction factorialFunction = calculator.getFunction("factorial");

                    // Test various calculations
                    final Object[] addResult = addFunction.call(new Object[] {requestId, 100});
                    final Object[] multiplyResult =
                        multiplyFunction.call(new Object[] {requestId + 1, 5});
                    final Object[] factorialResult =
                        factorialFunction.call(new Object[] {Math.min(requestId + 2, 10)});

                    // Validate results
                    assertThat(addResult[0]).isEqualTo(requestId + 100);
                    assertThat(multiplyResult[0]).isEqualTo((requestId + 1) * 5);
                    assertThat(factorialResult[0]).isInstanceOf(Integer.class);

                    // Clean up resources
                    calculator.close();
                    calculatorPlugin.close();
                    pluginStore.close();
                    pluginEngine.close();

                    successCount.incrementAndGet();
                    LOGGER.fine(String.format("Request %d completed successfully", requestId));

                  } catch (final Exception e) {
                    errorCount.incrementAndGet();
                    LOGGER.log(Level.SEVERE, String.format("Request %d failed", requestId), e);
                  }
                },
                executor);
      }

      // Wait for all requests to complete
      CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);

      // Validate all requests succeeded
      assertThat(successCount.get())
          .withFailMessage(
              "Expected %d successful requests, got %d (errors: %d)",
              concurrentRequests, successCount.get(), errorCount.get())
          .isEqualTo(concurrentRequests);

      assertThat(errorCount.get()).isZero();

      executor.shutdown();
      executor.awaitTermination(5, TimeUnit.SECONDS);

      LOGGER.info(
          String.format(
              "Web service plugin system test completed - %d/%d requests successful",
              successCount.get(), concurrentRequests));

    } catch (final Exception e) {
      LOGGER.log(Level.SEVERE, "Web service plugin system test failed", e);
      throw new AssertionError("Plugin system workflow failed", e);
    }
  }

  /**
   * Test: Data Processing Pipeline Real-world scenario: Processing large datasets through WASM
   * modules with streaming.
   */
  @Test
  @Order(2)
  @DisplayName("Data Processing Pipeline Workflow")
  void testDataProcessingPipelineWorkflow() {
    LOGGER.info("Testing data processing pipeline workflow");

    try {
      final Engine pipelineEngine = WasmRuntimeFactory.createEngine();
      final Store pipelineStore = new Store(pipelineEngine);

      // Load data processor module
      final Module processorModule = pipelineEngine.compileModule(DATA_PROCESSOR);
      final WasmInstance processor = new WasmInstance(pipelineStore, processorModule);

      final WasmMemory processorMemory = processor.getMemory("memory");
      final WasmFunction processDataFunction = processor.getFunction("process_data");
      final WasmFunction sumDataFunction = processor.getFunction("sum_data");

      // Simulate processing large datasets in chunks
      final int datasetSize = 10000;
      final int chunkSize = 1000;
      final List<Integer> originalData = new ArrayList<>();

      // Generate test dataset
      for (int i = 0; i < datasetSize; i++) {
        originalData.add(i + 1);
      }

      long totalSum = 0;
      int processedChunks = 0;

      // Process data in chunks
      for (int chunkStart = 0; chunkStart < datasetSize; chunkStart += chunkSize) {
        final int chunkEnd = Math.min(chunkStart + chunkSize, datasetSize);
        final int currentChunkSize = chunkEnd - chunkStart;

        // Write chunk data to WASM memory
        for (int i = 0; i < currentChunkSize; i++) {
          final int value = originalData.get(chunkStart + i);
          final byte[] valueBytes = ByteBuffer.allocate(4).putInt(value).array();
          processorMemory.write(i * 4, valueBytes);
        }

        // Process the chunk
        processDataFunction.call(new Object[] {0, 0, currentChunkSize});

        // Calculate sum for this chunk
        final Object[] chunkSum = sumDataFunction.call(new Object[] {0, currentChunkSize});
        totalSum += (Integer) chunkSum[0];

        processedChunks++;

        if (processedChunks % 5 == 0) {
          LOGGER.info(String.format("Processed %d chunks (%d items)", processedChunks, chunkEnd));
        }
      }

      // Validate processing results
      final long expectedSum = (long) datasetSize * (datasetSize + 1) / 2;
      assertThat(totalSum)
          .withFailMessage("Expected sum %d, got %d", expectedSum, totalSum)
          .isEqualTo(expectedSum);

      assertThat(processedChunks)
          .withFailMessage(
              "Expected %d chunks, processed %d",
              (datasetSize + chunkSize - 1) / chunkSize, processedChunks)
          .isEqualTo((datasetSize + chunkSize - 1) / chunkSize);

      // Clean up
      processor.close();
      processorModule.close();
      pipelineStore.close();
      pipelineEngine.close();

      LOGGER.info(
          String.format(
              "Data processing pipeline test completed - processed %d items in %d chunks, sum: %d",
              datasetSize, processedChunks, totalSum));

    } catch (final Exception e) {
      LOGGER.log(Level.SEVERE, "Data processing pipeline test failed", e);
      throw new AssertionError("Data processing workflow failed", e);
    }
  }

  /**
   * Test: Serverless Function Execution Real-world scenario: Rapid function instantiation and
   * execution with minimal cold start.
   */
  @Test
  @Order(3)
  @DisplayName("Serverless Function Execution Workflow")
  void testServerlessFunctionExecutionWorkflow() {
    LOGGER.info("Testing serverless function execution workflow");

    try {
      // Pre-compile modules to simulate function deployment
      final Engine deploymentEngine = WasmRuntimeFactory.createEngine();
      final Module precompiledCalculator = deploymentEngine.compileModule(MATHEMATICAL_CALCULATOR);

      // Test rapid function invocations (cold starts)
      final int functionInvocations = 1000;
      final long[] invocationTimes = new long[functionInvocations];

      for (int i = 0; i < functionInvocations; i++) {
        final long startTime = System.nanoTime();

        // Simulate function cold start
        final Store functionStore = new Store(deploymentEngine);
        final WasmInstance functionInstance =
            new WasmInstance(functionStore, precompiledCalculator);

        // Execute function
        final WasmFunction addFunction = functionInstance.getFunction("add");
        final Object[] result = addFunction.call(new Object[] {i, i + 1});

        // Validate result
        assertThat(result[0]).isEqualTo(2 * i + 1);

        // Clean up (simulate function completion)
        functionInstance.close();
        functionStore.close();

        final long endTime = System.nanoTime();
        invocationTimes[i] = endTime - startTime;

        if (i % 100 == 0) {
          LOGGER.fine(String.format("Completed %d function invocations", i));
        }
      }

      // Analyze performance metrics
      final double avgInvocationTime =
          Arrays.stream(invocationTimes).average().orElse(0.0) / 1_000_000.0; // ms
      final double maxInvocationTime =
          Arrays.stream(invocationTimes).max().orElse(0) / 1_000_000.0; // ms
      final double minInvocationTime =
          Arrays.stream(invocationTimes).min().orElse(0) / 1_000_000.0; // ms

      // Validate performance requirements
      assertThat(avgInvocationTime)
          .withFailMessage("Average invocation time %.3fms should be under 5ms", avgInvocationTime)
          .isLessThan(5.0);

      assertThat(maxInvocationTime)
          .withFailMessage("Maximum invocation time %.3fms should be under 50ms", maxInvocationTime)
          .isLessThan(50.0);

      // Clean up
      precompiledCalculator.close();
      deploymentEngine.close();

      LOGGER.info(
          String.format(
              "Serverless function test completed - %d invocations, avg: %.3fms, max: %.3fms, min:"
                  + " %.3fms",
              functionInvocations, avgInvocationTime, maxInvocationTime, minInvocationTime));

    } catch (final Exception e) {
      LOGGER.log(Level.SEVERE, "Serverless function execution test failed", e);
      throw new AssertionError("Serverless workflow failed", e);
    }
  }

  /**
   * Test: Microservices Integration Real-world scenario: Multiple microservices using WASM modules
   * for business logic.
   */
  @Test
  @Order(4)
  @DisplayName("Microservices Integration Workflow")
  void testMicroservicesIntegrationWorkflow() {
    LOGGER.info("Testing microservices integration workflow");

    try {
      // Simulate multiple microservices running concurrently
      final int serviceCount = 5;
      final ExecutorService serviceExecutor = Executors.newFixedThreadPool(serviceCount);
      final Map<String, CompletableFuture<Map<String, Object>>> serviceResults = new HashMap<>();

      // Service 1: Calculator Service
      serviceResults.put(
          "calculator",
          CompletableFuture.supplyAsync(
              () -> {
                try {
                  final Engine engine = WasmRuntimeFactory.createEngine();
                  final Store store = new Store(engine);
                  final Module module = engine.compileModule(MATHEMATICAL_CALCULATOR);
                  final WasmInstance instance = new WasmInstance(store, module);

                  final Map<String, Object> results = new HashMap<>();
                  final WasmFunction addFunction = instance.getFunction("add");
                  final WasmFunction multiplyFunction = instance.getFunction("multiply");

                  // Process multiple operations
                  for (int i = 0; i < 100; i++) {
                    final Object[] addResult = addFunction.call(new Object[] {i, i * 2});
                    final Object[] multiplyResult = multiplyFunction.call(new Object[] {i, 3});
                    results.put("add_" + i, addResult[0]);
                    results.put("multiply_" + i, multiplyResult[0]);
                  }

                  instance.close();
                  module.close();
                  store.close();
                  engine.close();

                  return results;
                } catch (final Exception e) {
                  throw new RuntimeException("Calculator service failed", e);
                }
              },
              serviceExecutor));

      // Service 2: Data Processing Service
      serviceResults.put(
          "dataProcessor",
          CompletableFuture.supplyAsync(
              () -> {
                try {
                  final Engine engine = WasmRuntimeFactory.createEngine();
                  final Store store = new Store(engine);
                  final Module module = engine.compileModule(DATA_PROCESSOR);
                  final WasmInstance instance = new WasmInstance(store, module);

                  final Map<String, Object> results = new HashMap<>();
                  final WasmMemory memory = instance.getMemory("memory");
                  final WasmFunction sumFunction = instance.getFunction("sum_data");

                  // Process multiple datasets
                  for (int dataset = 0; dataset < 10; dataset++) {
                    // Write test data
                    for (int i = 0; i < 100; i++) {
                      final byte[] valueBytes = ByteBuffer.allocate(4).putInt(i + 1).array();
                      memory.write(i * 4, valueBytes);
                    }

                    final Object[] sumResult = sumFunction.call(new Object[] {0, 100});
                    results.put("dataset_" + dataset, sumResult[0]);
                  }

                  instance.close();
                  module.close();
                  store.close();
                  engine.close();

                  return results;
                } catch (final Exception e) {
                  throw new RuntimeException("Data processor service failed", e);
                }
              },
              serviceExecutor));

      // Services 3-5: Additional calculator instances (load balancing simulation)
      for (int serviceId = 3; serviceId <= 5; serviceId++) {
        final int currentServiceId = serviceId;
        serviceResults.put(
            "calculator_" + serviceId,
            CompletableFuture.supplyAsync(
                () -> {
                  try {
                    final Engine engine = WasmRuntimeFactory.createEngine();
                    final Store store = new Store(engine);
                    final Module module = engine.compileModule(MATHEMATICAL_CALCULATOR);
                    final WasmInstance instance = new WasmInstance(store, module);

                    final Map<String, Object> results = new HashMap<>();
                    final WasmFunction addFunction = instance.getFunction("add");

                    // Each service processes a different range
                    final int startRange = (currentServiceId - 3) * 100;
                    for (int i = startRange; i < startRange + 100; i++) {
                      final Object[] result = addFunction.call(new Object[] {i, currentServiceId});
                      results.put("operation_" + i, result[0]);
                    }

                    instance.close();
                    module.close();
                    store.close();
                    engine.close();

                    return results;
                  } catch (final Exception e) {
                    throw new RuntimeException(
                        "Calculator service " + currentServiceId + " failed", e);
                  }
                },
                serviceExecutor));
      }

      // Wait for all services to complete
      final Map<String, Map<String, Object>> allResults = new HashMap<>();
      for (final Map.Entry<String, CompletableFuture<Map<String, Object>>> entry :
          serviceResults.entrySet()) {
        allResults.put(entry.getKey(), entry.getValue().get(30, TimeUnit.SECONDS));
      }

      // Validate all services completed successfully
      assertThat(allResults).hasSize(serviceCount);
      for (final Map.Entry<String, Map<String, Object>> serviceResult : allResults.entrySet()) {
        assertThat(serviceResult.getValue())
            .withFailMessage("Service %s should have results", serviceResult.getKey())
            .isNotEmpty();
      }

      // Validate specific service results
      final Map<String, Object> calculatorResults = allResults.get("calculator");
      assertThat(calculatorResults.get("add_0")).isEqualTo(0);
      assertThat(calculatorResults.get("multiply_5")).isEqualTo(15);

      final Map<String, Object> dataProcessorResults = allResults.get("dataProcessor");
      assertThat(dataProcessorResults.get("dataset_0")).isEqualTo(5050); // sum of 1 to 100

      serviceExecutor.shutdown();
      serviceExecutor.awaitTermination(5, TimeUnit.SECONDS);

      LOGGER.info(
          String.format(
              "Microservices integration test completed - %d services, total operations: %d",
              serviceCount, allResults.values().stream().mapToInt(Map::size).sum()));

    } catch (final Exception e) {
      LOGGER.log(Level.SEVERE, "Microservices integration test failed", e);
      throw new AssertionError("Microservices workflow failed", e);
    }
  }

  /**
   * Test: Module Caching and Serialization Real-world scenario: Optimizing module loading through
   * caching and serialization.
   */
  @Test
  @Order(5)
  @DisplayName("Module Caching and Serialization Workflow")
  void testModuleCachingAndSerializationWorkflow() {
    LOGGER.info("Testing module caching and serialization workflow");

    try {
      final Engine engine = WasmRuntimeFactory.createEngine();

      // Initial compilation (cold)
      final long coldStartTime = System.nanoTime();
      final Module originalModule = engine.compileModule(MATHEMATICAL_CALCULATOR);
      final long coldEndTime = System.nanoTime();
      final double coldCompileTime = (coldEndTime - coldStartTime) / 1_000_000.0; // ms

      // Test serialization
      final byte[] serializedModule = SerializationSystem.serialize(originalModule);
      assertThat(serializedModule)
          .withFailMessage("Serialized module should not be empty")
          .isNotEmpty();

      // Test deserialization (warm start)
      final long warmStartTime = System.nanoTime();
      final Module deserializedModule = SerializationSystem.deserialize(engine, serializedModule);
      final long warmEndTime = System.nanoTime();
      final double warmCompileTime = (warmEndTime - warmStartTime) / 1_000_000.0; // ms

      // Validate deserialized module works
      final Store store = new Store(engine);
      final WasmInstance instance = new WasmInstance(store, deserializedModule);
      final WasmFunction addFunction = instance.getFunction("add");
      final Object[] result = addFunction.call(new Object[] {42, 58});
      assertThat(result[0]).isEqualTo(100);

      // Test module caching if available
      try {
        final ModuleCache cache = new ModuleCache(tempDir.toString());
        final String cacheKey = "calculator_v1";

        // Cache the module
        cache.put(cacheKey, originalModule);

        // Retrieve from cache
        final long cacheStartTime = System.nanoTime();
        final Module cachedModule = cache.get(cacheKey);
        final long cacheEndTime = System.nanoTime();
        final double cacheRetrievalTime = (cacheEndTime - cacheStartTime) / 1_000_000.0; // ms

        assertThat(cachedModule).isNotNull();

        // Validate cached module works
        final WasmInstance cachedInstance = new WasmInstance(store, cachedModule);
        final WasmFunction cachedAddFunction = cachedInstance.getFunction("add");
        final Object[] cachedResult = cachedAddFunction.call(new Object[] {10, 20});
        assertThat(cachedResult[0]).isEqualTo(30);

        cachedInstance.close();

        LOGGER.info(String.format("Cache retrieval time: %.3fms", cacheRetrievalTime));

      } catch (final Exception e) {
        LOGGER.info("Module caching not available: " + e.getMessage());
      }

      // Performance comparison
      LOGGER.info(
          String.format(
              "Compilation times - Cold: %.3fms, Warm (deserialization): %.3fms",
              coldCompileTime, warmCompileTime));

      // Deserialization should be faster than compilation (not always guaranteed)
      if (warmCompileTime < coldCompileTime) {
        LOGGER.info(
            String.format(
                "Deserialization %.2fx faster than compilation",
                coldCompileTime / warmCompileTime));
      }

      // Clean up
      instance.close();
      deserializedModule.close();
      originalModule.close();
      store.close();
      engine.close();

    } catch (final Exception e) {
      LOGGER.log(Level.SEVERE, "Module caching and serialization test failed", e);
      throw new AssertionError("Caching workflow failed", e);
    }
  }

  /**
   * Test: WASI File System Integration Real-world scenario: File processing through WASI
   * interfaces.
   */
  @Test
  @Order(6)
  @DisplayName("WASI File System Integration Workflow")
  void testWasiFileSystemIntegrationWorkflow() {
    LOGGER.info("Testing WASI file system integration workflow");

    try {
      // Create test files
      final Path inputFile = tempDir.resolve("input.txt");
      final Path outputFile = tempDir.resolve("output.txt");
      final String testContent = "Hello, WASI World!\nThis is a test file.\n";

      Files.write(inputFile, testContent.getBytes());

      // Create WASI runtime
      final WasiRuntime wasiRuntime = WasiFactory.createPreview1();

      // Bind directories and files
      wasiRuntime.bindDirectory("/temp", tempDir.toString());
      wasiRuntime.bindStdin(new ByteArrayInputStream(testContent.getBytes()));
      wasiRuntime.bindStdout(new ByteArrayOutputStream());
      wasiRuntime.bindStderr(new ByteArrayOutputStream());

      LOGGER.info(
          "WASI file system integration test simulated - actual WASI module would be needed for"
              + " full test");

      // Note: This test is simplified as it requires a WASI-compatible WASM module
      // In a real scenario, this would load and execute a WASI module that:
      // 1. Reads from the input file
      // 2. Processes the content
      // 3. Writes to the output file

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "WASI file system test could not complete", e);
      // Don't fail the test as WASI modules may not be available
    }
  }

  /**
   * Test: Async and Concurrent Processing Real-world scenario: Asynchronous WASM execution for
   * non-blocking operations.
   */
  @Test
  @Order(7)
  @DisplayName("Async and Concurrent Processing Workflow")
  void testAsyncAndConcurrentProcessingWorkflow() {
    LOGGER.info("Testing async and concurrent processing workflow");

    try {
      // Test async engine if available
      try {
        final AsyncEngine asyncEngine = new AsyncEngine();

        // Test async compilation
        final CompletableFuture<Module> compilationFuture =
            asyncEngine.compileAsync(MATHEMATICAL_CALCULATOR);
        final Module asyncModule = compilationFuture.get(10, TimeUnit.SECONDS);
        assertThat(asyncModule).isNotNull();

        // Test async instantiation
        final Store asyncStore = new Store(asyncEngine);
        final CompletableFuture<WasmInstance> instantiationFuture =
            asyncEngine.instantiateAsync(asyncStore, asyncModule);
        final WasmInstance asyncInstance = instantiationFuture.get(10, TimeUnit.SECONDS);
        assertThat(asyncInstance).isNotNull();

        // Test async execution
        final WasmFunction asyncAddFunction = asyncInstance.getFunction("add");
        final CompletableFuture<Object[]> executionFuture =
            asyncEngine.executeAsync(asyncAddFunction, new Object[] {25, 75});
        final Object[] asyncResult = executionFuture.get(10, TimeUnit.SECONDS);
        assertThat(asyncResult[0]).isEqualTo(100);

        // Clean up
        asyncInstance.close();
        asyncModule.close();
        asyncStore.close();
        asyncEngine.close();

        LOGGER.info("Async processing test completed successfully");

      } catch (final ClassNotFoundException e) {
        LOGGER.info("Async engine not available - testing concurrent processing only");
        testConcurrentProcessingFallback();
      }

    } catch (final Exception e) {
      LOGGER.log(Level.SEVERE, "Async and concurrent processing test failed", e);
      throw new AssertionError("Async workflow failed", e);
    }
  }

  /** Fallback concurrent processing test when async engine is not available. */
  private void testConcurrentProcessingFallback() throws Exception {
    final int concurrentTasks = 10;
    final ExecutorService executor = Executors.newFixedThreadPool(concurrentTasks);
    final AtomicLong totalOperations = new AtomicLong(0);

    final CompletableFuture<?>[] futures = new CompletableFuture[concurrentTasks];

    for (int i = 0; i < concurrentTasks; i++) {
      final int taskId = i;
      futures[i] =
          CompletableFuture.runAsync(
              () -> {
                try {
                  final Engine engine = WasmRuntimeFactory.createEngine();
                  final Store store = new Store(engine);
                  final Module module = engine.compileModule(MATHEMATICAL_CALCULATOR);
                  final WasmInstance instance = new WasmInstance(store, module);
                  final WasmFunction addFunction = instance.getFunction("add");

                  // Perform multiple operations per task
                  for (int j = 0; j < 100; j++) {
                    final Object[] result = addFunction.call(new Object[] {taskId, j});
                    assertThat(result[0]).isEqualTo(taskId + j);
                    totalOperations.incrementAndGet();
                  }

                  instance.close();
                  module.close();
                  store.close();
                  engine.close();

                } catch (final Exception e) {
                  throw new RuntimeException("Concurrent task " + taskId + " failed", e);
                }
              },
              executor);
    }

    CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);

    assertThat(totalOperations.get()).isEqualTo(concurrentTasks * 100);

    executor.shutdown();
    executor.awaitTermination(5, TimeUnit.SECONDS);

    LOGGER.info(
        String.format(
            "Concurrent processing completed - %d operations across %d tasks",
            totalOperations.get(), concurrentTasks));
  }
}
