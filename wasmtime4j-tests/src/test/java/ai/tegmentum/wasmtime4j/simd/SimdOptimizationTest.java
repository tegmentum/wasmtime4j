package ai.tegmentum.wasmtime4j.simd;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.*;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive SIMD optimization tests for wasmtime4j.
 *
 * <p>Tests platform-specific optimizations, fallback mechanisms, and cross-runtime compatibility
 * for SIMD operations.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SimdOptimizationTest {

  private static final Logger logger = Logger.getLogger(SimdOptimizationTest.class.getName());

  private static final int TEST_VECTOR_SIZE = 16;
  private static final float FLOAT_EPSILON = 1e-6f;
  private static final Random RANDOM = new Random(42);

  private WasmRuntime runtime;
  private Module simdModule;
  private Instance simdInstance;

  @BeforeEach
  void setUp() throws Exception {
    logger.info("Setting up SIMD optimization test");

    // Create engine configuration with SIMD enabled
    EngineConfig engineConfig =
        EngineConfig.builder()
            .withFeature(WasmFeature.SIMD, true)
            .withFeature(WasmFeature.MULTI_VALUE, true)
            .withFeature(WasmFeature.BULK_MEMORY, true)
            .withOptimizationLevel(3)
            .build();

    runtime = WasmRuntimeFactory.createRuntime(engineConfig);
    assertNotNull(runtime, "Runtime should be created successfully");

    // Load SIMD test module (would be a real WASM module with SIMD operations)
    byte[] wasmBytes = createSimdTestModule();
    simdModule = Module.fromBinary(runtime, wasmBytes);
    assertNotNull(simdModule, "SIMD module should be loaded successfully");

    simdInstance = Instance.create(runtime, simdModule);
    assertNotNull(simdInstance, "SIMD instance should be created successfully");
  }

  @AfterEach
  void tearDown() throws Exception {
    if (simdInstance != null) {
      simdInstance.close();
    }
    if (runtime != null) {
      runtime.close();
    }
    logger.info("SIMD optimization test cleanup completed");
  }

  /** Test platform-specific SIMD capability detection. */
  @Test
  @Order(1)
  void testSimdCapabilityDetection() throws Exception {
    logger.info("Testing SIMD capability detection");

    // Test that runtime reports SIMD capabilities
    assertTrue(
        runtime.getEngine().getConfig().isFeatureEnabled(WasmFeature.SIMD),
        "SIMD feature should be enabled");

    // Test platform-specific capabilities through WebAssembly
    WasmValue[] result = simdInstance.call("get_simd_capabilities");
    assertNotNull(result, "SIMD capabilities query should succeed");
    assertTrue(result.length > 0, "Should return capability information");

    logger.info("SIMD capabilities: " + Arrays.toString(result));
  }

  /** Test V128 vector operations on different platforms. */
  @ParameterizedTest
  @ValueSource(strings = {"add", "sub", "mul", "div", "and", "or", "xor"})
  @Order(2)
  void testV128Operations(String operation) throws Exception {
    logger.info("Testing V128 " + operation + " operation");

    // Create test vectors with known values
    int[] vector1 = {1, 2, 3, 4};
    int[] vector2 = {5, 6, 7, 8};

    WasmValue[] args = new WasmValue[8];
    for (int i = 0; i < 4; i++) {
      args[i] = WasmValue.i32(vector1[i]);
      args[i + 4] = WasmValue.i32(vector2[i]);
    }

    // Call SIMD operation
    WasmValue[] result = simdInstance.call("v128_" + operation, args);
    assertNotNull(result, operation + " operation should succeed");
    assertEquals(4, result.length, "Should return 4 i32 values");

    // Verify expected results based on operation
    int[] expected = calculateExpected(vector1, vector2, operation);
    for (int i = 0; i < 4; i++) {
      assertEquals(
          expected[i],
          result[i].i32(),
          String.format("%s operation failed at index %d", operation, i));
    }

    logger.info("V128 " + operation + " test passed");
  }

  /** Test V256 operations if platform supports them. */
  @Test
  @Order(3)
  void testV256Operations() throws Exception {
    logger.info("Testing V256 operations");

    // Check if V256 is supported
    WasmValue[] capResult = simdInstance.call("supports_v256");
    boolean supportsV256 = capResult[0].i32() != 0;

    if (!supportsV256) {
      logger.info("V256 not supported on this platform, skipping test");
      return;
    }

    // Create V256 test vectors (8 i32 values)
    int[] vector1 = {1, 2, 3, 4, 5, 6, 7, 8};
    int[] vector2 = {8, 7, 6, 5, 4, 3, 2, 1};

    WasmValue[] args = new WasmValue[16];
    for (int i = 0; i < 8; i++) {
      args[i] = WasmValue.i32(vector1[i]);
      args[i + 8] = WasmValue.i32(vector2[i]);
    }

    WasmValue[] result = simdInstance.call("v256_add", args);
    assertNotNull(result, "V256 add operation should succeed");
    assertEquals(8, result.length, "Should return 8 i32 values");

    // All results should be 9 (vector1[i] + vector2[i])
    for (int i = 0; i < 8; i++) {
      assertEquals(9, result[i].i32(), String.format("V256 add failed at index %d", i));
    }

    logger.info("V256 operations test passed");
  }

  /** Test V512 operations if platform supports them. */
  @Test
  @Order(4)
  void testV512Operations() throws Exception {
    logger.info("Testing V512 operations");

    // Check if V512 is supported (AVX-512)
    WasmValue[] capResult = simdInstance.call("supports_v512");
    boolean supportsV512 = capResult[0].i32() != 0;

    if (!supportsV512) {
      logger.info("V512 not supported on this platform, skipping test");
      return;
    }

    // Create V512 test vectors (16 i32 values)
    WasmValue[] args = new WasmValue[32];
    for (int i = 0; i < 16; i++) {
      args[i] = WasmValue.i32(1);
      args[i + 16] = WasmValue.i32(2);
    }

    WasmValue[] result = simdInstance.call("v512_add", args);
    assertNotNull(result, "V512 add operation should succeed");
    assertEquals(16, result.length, "Should return 16 i32 values");

    // All results should be 3 (1 + 2)
    for (int i = 0; i < 16; i++) {
      assertEquals(3, result[i].i32(), String.format("V512 add failed at index %d", i));
    }

    logger.info("V512 operations test passed");
  }

  /** Test ARM64 NEON-specific optimizations. */
  @Test
  @Order(5)
  void testNeonOptimizations() throws Exception {
    logger.info("Testing ARM64 NEON optimizations");

    // Check if running on ARM64
    WasmValue[] archResult = simdInstance.call("get_architecture");
    String arch = archResult[0].toString();

    if (!arch.contains("aarch64")) {
      logger.info("Not running on ARM64, skipping NEON test");
      return;
    }

    // Test NEON-specific operations
    float[] vector1 = {1.0f, 2.0f, 3.0f, 4.0f};
    float[] vector2 = {2.0f, 3.0f, 4.0f, 5.0f};

    WasmValue[] args = new WasmValue[8];
    for (int i = 0; i < 4; i++) {
      args[i] = WasmValue.f32(vector1[i]);
      args[i + 4] = WasmValue.f32(vector2[i]);
    }

    // Test NEON FMA operation
    WasmValue[] fmaResult = simdInstance.call("neon_fma", args);
    assertNotNull(fmaResult, "NEON FMA should succeed");
    assertEquals(4, fmaResult.length, "Should return 4 f32 values");

    // Verify FMA results: a * b + c (using vector1 as c)
    for (int i = 0; i < 4; i++) {
      float expected = vector1[i] * vector2[i] + vector1[i];
      float actual = fmaResult[i].f32();
      assertEquals(
          expected, actual, FLOAT_EPSILON, String.format("NEON FMA failed at index %d", i));
    }

    logger.info("NEON optimizations test passed");
  }

  /** Test fallback mechanisms when platform-specific optimizations fail. */
  @Test
  @Order(6)
  void testFallbackMechanisms() throws Exception {
    logger.info("Testing SIMD fallback mechanisms");

    // Force fallback mode
    WasmValue[] args = {WasmValue.i32(1)}; // Force fallback flag
    WasmValue[] result = simdInstance.call("test_fallback", args);
    assertNotNull(result, "Fallback test should succeed");

    // Test that operations still work in fallback mode
    int[] vector1 = {10, 20, 30, 40};
    int[] vector2 = {1, 2, 3, 4};

    WasmValue[] addArgs = new WasmValue[9]; // 8 values + fallback flag
    addArgs[0] = WasmValue.i32(1); // Force fallback
    for (int i = 0; i < 4; i++) {
      addArgs[i + 1] = WasmValue.i32(vector1[i]);
      addArgs[i + 5] = WasmValue.i32(vector2[i]);
    }

    WasmValue[] addResult = simdInstance.call("v128_add_with_fallback", addArgs);
    assertNotNull(addResult, "Fallback add should succeed");
    assertEquals(4, addResult.length, "Should return 4 i32 values");

    // Verify results are correct even with fallback
    int[] expected = {11, 22, 33, 44};
    for (int i = 0; i < 4; i++) {
      assertEquals(
          expected[i], addResult[i].i32(), String.format("Fallback add failed at index %d", i));
    }

    logger.info("Fallback mechanisms test passed");
  }

  /** Test complex mathematical operations using SIMD. */
  @Test
  @Order(7)
  void testComplexMathOperations() throws Exception {
    logger.info("Testing complex mathematical SIMD operations");

    // Test dot product
    float[] vector1 = {1.0f, 2.0f, 3.0f, 4.0f};
    float[] vector2 = {2.0f, 3.0f, 4.0f, 5.0f};

    WasmValue[] dotArgs = new WasmValue[8];
    for (int i = 0; i < 4; i++) {
      dotArgs[i] = WasmValue.f32(vector1[i]);
      dotArgs[i + 4] = WasmValue.f32(vector2[i]);
    }

    WasmValue[] dotResult = simdInstance.call("simd_dot_product", dotArgs);
    assertNotNull(dotResult, "Dot product should succeed");
    assertEquals(1, dotResult.length, "Dot product should return single value");

    float expectedDot = 1.0f * 2.0f + 2.0f * 3.0f + 3.0f * 4.0f + 4.0f * 5.0f; // = 40.0
    assertEquals(
        expectedDot, dotResult[0].f32(), FLOAT_EPSILON, "Dot product calculation incorrect");

    // Test vector reduction (sum)
    int[] intVector = {5, 10, 15, 20};
    WasmValue[] sumArgs = new WasmValue[4];
    for (int i = 0; i < 4; i++) {
      sumArgs[i] = WasmValue.i32(intVector[i]);
    }

    WasmValue[] sumResult = simdInstance.call("simd_reduce_sum", sumArgs);
    assertNotNull(sumResult, "Vector sum should succeed");
    assertEquals(1, sumResult.length, "Vector sum should return single value");

    int expectedSum = 50; // 5 + 10 + 15 + 20
    assertEquals(expectedSum, sumResult[0].i32(), "Vector sum calculation incorrect");

    logger.info("Complex mathematical operations test passed");
  }

  /** Test gather/scatter operations for non-contiguous memory access. */
  @Test
  @Order(8)
  void testGatherScatterOperations() throws Exception {
    logger.info("Testing gather/scatter operations");

    // Check if gather/scatter is supported
    WasmValue[] capResult = simdInstance.call("supports_gather_scatter");
    boolean supportsGatherScatter = capResult[0].i32() != 0;

    if (!supportsGatherScatter) {
      logger.info("Gather/scatter not supported on this platform, skipping test");
      return;
    }

    // Test gather operation with indices
    int[] indices = {0, 4, 8, 12}; // Gather every 4th element
    WasmValue[] gatherArgs = new WasmValue[4];
    for (int i = 0; i < 4; i++) {
      gatherArgs[i] = WasmValue.i32(indices[i]);
    }

    WasmValue[] gatherResult = simdInstance.call("simd_gather", gatherArgs);
    assertNotNull(gatherResult, "Gather operation should succeed");
    assertEquals(4, gatherResult.length, "Should return 4 gathered values");

    // Test scatter operation
    int[] values = {100, 200, 300, 400};
    WasmValue[] scatterArgs = new WasmValue[8];
    for (int i = 0; i < 4; i++) {
      scatterArgs[i] = WasmValue.i32(indices[i]);
      scatterArgs[i + 4] = WasmValue.i32(values[i]);
    }

    WasmValue[] scatterResult = simdInstance.call("simd_scatter", scatterArgs);
    assertNotNull(scatterResult, "Scatter operation should succeed");

    logger.info("Gather/scatter operations test passed");
  }

  /** Test memory alignment requirements and optimizations. */
  @Test
  @Order(9)
  void testMemoryAlignment() throws Exception {
    logger.info("Testing memory alignment requirements");

    // Test aligned loads
    WasmValue[] alignedResult = simdInstance.call("test_aligned_load", WasmValue.i32(16));
    assertNotNull(alignedResult, "Aligned load should succeed");

    // Test unaligned loads (should use fallback or special handling)
    WasmValue[] unalignedResult = simdInstance.call("test_unaligned_load", WasmValue.i32(5));
    assertNotNull(unalignedResult, "Unaligned load should succeed with fallback");

    // Performance test: aligned vs unaligned
    long alignedTime =
        measureOperation(
            () -> {
              try {
                return simdInstance.call("test_aligned_load", WasmValue.i32(16));
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            });

    long unalignedTime =
        measureOperation(
            () -> {
              try {
                return simdInstance.call("test_unaligned_load", WasmValue.i32(5));
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            });

    logger.info(
        String.format("Aligned load: %d ns, Unaligned load: %d ns", alignedTime, unalignedTime));

    // Aligned should generally be faster, but allow for measurement variance
    assertTrue(
        alignedTime <= unalignedTime * 2, "Aligned access should not be significantly slower");

    logger.info("Memory alignment test passed");
  }

  /** Test concurrent SIMD operations. */
  @Test
  @Order(10)
  void testConcurrentSimdOperations() throws Exception {
    logger.info("Testing concurrent SIMD operations");

    final int THREAD_COUNT = 4;
    final int OPERATIONS_PER_THREAD = 100;

    ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

    // Create separate instances for each thread (if supported)
    Instance[] instances = new Instance[THREAD_COUNT];
    for (int i = 0; i < THREAD_COUNT; i++) {
      instances[i] = Instance.create(runtime, simdModule);
    }

    try {
      for (int threadId = 0; threadId < THREAD_COUNT; threadId++) {
        final int tid = threadId;
        executor.submit(
            () -> {
              try {
                for (int op = 0; op < OPERATIONS_PER_THREAD; op++) {
                  // Generate unique test data for each thread and operation
                  int base = tid * 1000 + op;
                  WasmValue[] args = {
                    WasmValue.i32(base + 1),
                    WasmValue.i32(base + 2),
                    WasmValue.i32(base + 3),
                    WasmValue.i32(base + 4),
                    WasmValue.i32(base + 5),
                    WasmValue.i32(base + 6),
                    WasmValue.i32(base + 7),
                    WasmValue.i32(base + 8)
                  };

                  WasmValue[] result = instances[tid].call("v128_add", args);
                  assertNotNull(result, "Concurrent operation should succeed");
                  assertEquals(4, result.length, "Should return 4 values");

                  // Verify results
                  for (int i = 0; i < 4; i++) {
                    int expected = (base + i + 1) + (base + i + 5);
                    assertEquals(
                        expected,
                        result[i].i32(),
                        String.format(
                            "Concurrent operation failed: thread %d, op %d, index %d", tid, op, i));
                  }
                }
              } catch (Exception e) {
                logger.severe("Concurrent test failed: " + e.getMessage());
                fail("Concurrent SIMD operation failed: " + e.getMessage());
              } finally {
                latch.countDown();
              }
            });
      }

      assertTrue(
          latch.await(30, TimeUnit.SECONDS),
          "Concurrent operations should complete within 30 seconds");

    } finally {
      executor.shutdown();
      for (Instance instance : instances) {
        if (instance != null) {
          instance.close();
        }
      }
    }

    logger.info("Concurrent SIMD operations test passed");
  }

  /** Cross-runtime compatibility test. */
  @Test
  @Order(11)
  void testCrossRuntimeCompatibility() throws Exception {
    logger.info("Testing cross-runtime SIMD compatibility");

    // Test the same SIMD operations with different runtime configurations
    String[] runtimeTypes = {"JNI", "PANAMA"};

    for (String runtimeType : runtimeTypes) {
      logger.info("Testing with runtime type: " + runtimeType);

      try {
        // Create runtime with specific type
        EngineConfig config =
            EngineConfig.builder()
                .withFeature(WasmFeature.SIMD, true)
                .withRuntimeProperty("wasmtime4j.runtime", runtimeType.toLowerCase())
                .build();

        try (WasmRuntime testRuntime = WasmRuntimeFactory.createRuntime(config)) {
          Module testModule = Module.fromBinary(testRuntime, createSimdTestModule());
          try (Instance testInstance = Instance.create(testRuntime, testModule)) {

            // Test basic operation
            WasmValue[] args = {
              WasmValue.i32(10), WasmValue.i32(20), WasmValue.i32(30), WasmValue.i32(40),
              WasmValue.i32(5), WasmValue.i32(10), WasmValue.i32(15), WasmValue.i32(20)
            };

            WasmValue[] result = testInstance.call("v128_add", args);
            assertNotNull(result, "Cross-runtime operation should succeed for " + runtimeType);
            assertEquals(4, result.length, "Should return 4 values for " + runtimeType);

            // Verify results are consistent across runtimes
            int[] expected = {15, 30, 45, 60};
            for (int i = 0; i < 4; i++) {
              assertEquals(
                  expected[i],
                  result[i].i32(),
                  String.format(
                      "Cross-runtime consistency failed for %s at index %d", runtimeType, i));
            }
          }
        }

        logger.info(runtimeType + " runtime compatibility verified");

      } catch (UnsupportedOperationException e) {
        logger.info(runtimeType + " runtime not available on this platform: " + e.getMessage());
      }
    }

    logger.info("Cross-runtime compatibility test passed");
  }

  /** Helper method to calculate expected results for vector operations. */
  private int[] calculateExpected(int[] a, int[] b, String operation) {
    int[] result = new int[4];
    for (int i = 0; i < 4; i++) {
      result[i] =
          switch (operation) {
            case "add" -> a[i] + b[i];
            case "sub" -> a[i] - b[i];
            case "mul" -> a[i] * b[i];
            case "div" -> a[i] / b[i];
            case "and" -> a[i] & b[i];
            case "or" -> a[i] | b[i];
            case "xor" -> a[i] ^ b[i];
            default -> throw new IllegalArgumentException("Unknown operation: " + operation);
          };
    }
    return result;
  }

  /** Helper method to measure operation performance. */
  private long measureOperation(Runnable operation) {
    // Warm up
    for (int i = 0; i < 10; i++) {
      operation.run();
    }

    // Measure
    long startTime = System.nanoTime();
    for (int i = 0; i < 100; i++) {
      operation.run();
    }
    long endTime = System.nanoTime();

    return (endTime - startTime) / 100; // Average per operation
  }

  /**
   * Creates a minimal SIMD test WebAssembly module. In a real implementation, this would load a
   * pre-compiled WASM module.
   */
  private byte[] createSimdTestModule() {
    // This is a placeholder - in reality, you'd load a WASM file
    // that contains all the SIMD test functions
    return new byte[] {
      0x00, 0x61, 0x73, 0x6d, // magic
      0x01, 0x00, 0x00, 0x00, // version
      // ... rest of WASM module with SIMD functions
    };
  }
}
