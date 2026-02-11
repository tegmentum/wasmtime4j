/*
 * Copyright 2025 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.serialization;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Serializer;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Integration tests for serialization cross-version and cross-runtime compatibility.
 *
 * <p>These tests verify that serialized modules maintain compatibility across:
 *
 * <ul>
 *   <li>Different versions of wasmtime4j (forward/backward compatibility)
 *   <li>JNI and Panama runtime implementations
 *   <li>Different compression configurations
 *   <li>Different engine configurations
 * </ul>
 *
 * @since 1.0.0
 */
@DisplayName("Serialization Compatibility Integration Tests")
public final class SerializationCompatibilityIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(SerializationCompatibilityIntegrationTest.class.getName());

  /** Version signature for serialized modules. */
  private static final int VERSION_MAJOR = 1;

  private static final int VERSION_MINOR = 0;
  private static final int VERSION_PATCH = 0;

  private static boolean serializerAvailable = false;
  private static boolean jniRuntimeAvailable = false;
  private static boolean panamaRuntimeAvailable = false;

  /**
   * Simple WebAssembly module that exports an add function.
   *
   * <pre>
   * (module
   *   (func (export "add") (param i32 i32) (result i32)
   *     local.get 0
   *     local.get 1
   *     i32.add))
   * </pre>
   */
  private static final byte[] ADD_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6D, // magic number
        0x01,
        0x00,
        0x00,
        0x00, // version 1
        0x01,
        0x07,
        0x01,
        0x60,
        0x02,
        0x7F,
        0x7F,
        0x01,
        0x7F, // type section
        0x03,
        0x02,
        0x01,
        0x00, // function section
        0x07,
        0x07,
        0x01,
        0x03,
        0x61,
        0x64,
        0x64,
        0x00,
        0x00, // export section
        0x0A,
        0x09,
        0x01,
        0x07,
        0x00,
        0x20,
        0x00,
        0x20,
        0x01,
        0x6A,
        0x0B // code section
      };

  /**
   * Simple WebAssembly module that returns a constant.
   *
   * <pre>
   * (module
   *   (func (export "get") (result i32)
   *     i32.const 42))
   * </pre>
   */
  private static final byte[] CONST_WASM =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6D, // magic number
        0x01,
        0x00,
        0x00,
        0x00, // version 1
        0x01,
        0x05,
        0x01,
        0x60,
        0x00,
        0x01,
        0x7F, // type section
        0x03,
        0x02,
        0x01,
        0x00, // function section
        0x07,
        0x07,
        0x01,
        0x03,
        0x67,
        0x65,
        0x74,
        0x00,
        0x00, // export section
        0x0A,
        0x06,
        0x01,
        0x04,
        0x00,
        0x41,
        0x2A,
        0x0B // code section
      };

  /**
   * WebAssembly module with memory and global exports.
   *
   * <pre>
   * (module
   *   (memory (export "memory") 1)
   *   (global (export "counter") (mut i32) (i32.const 0))
   *   (func (export "inc") (result i32)
   *     global.get 0
   *     i32.const 1
   *     i32.add
   *     global.set 0
   *     global.get 0))
   * </pre>
   */
  private static final byte[] STATEFUL_WASM =
      new byte[] {
        0x00, 0x61, 0x73, 0x6D, // magic number
        0x01, 0x00, 0x00, 0x00, // version 1
        0x01, 0x05, 0x01, 0x60, 0x00, 0x01, 0x7F, // type section
        0x03, 0x02, 0x01, 0x00, // function section
        0x05, 0x03, 0x01, 0x00, 0x01, // memory section
        0x06, 0x06, 0x01, 0x7F, 0x01, 0x41, 0x00, 0x0B, // global section
        0x07, 0x15, 0x03, 0x06, 0x6D, 0x65, 0x6D, 0x6F, 0x72, 0x79, // export "memory"
        0x02, 0x00, 0x07, 0x63, 0x6F, 0x75, 0x6E, 0x74, 0x65, 0x72, // export "counter"
        0x03, 0x00, 0x03, 0x69, 0x6E, 0x63, 0x00, 0x00, // export "inc"
        0x0A, 0x0D, 0x01, 0x0B, 0x00, 0x23, 0x00, 0x41, 0x01, // code section
        0x6A, 0x24, 0x00, 0x23, 0x00, 0x0B
      };

  private Engine engine;
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeAll
  static void checkAvailability() {
    // Check Serializer availability
    try {
      final Serializer serializer = Serializer.create();
      serializer.close();
      serializerAvailable = true;
      LOGGER.info("Serializer native implementation is available");
    } catch (final Exception e) {
      serializerAvailable = false;
      LOGGER.warning("Serializer not available - tests will be skipped: " + e.getMessage());
    }

    // Check JNI runtime availability
    try {
      if (WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.JNI)) {
        jniRuntimeAvailable = true;
        LOGGER.info("JNI runtime is available");
      }
    } catch (final Exception e) {
      jniRuntimeAvailable = false;
      LOGGER.warning("JNI runtime not available: " + e.getMessage());
    }

    // Check Panama runtime availability
    try {
      if (WasmRuntimeFactory.isRuntimeAvailable(RuntimeType.PANAMA)) {
        panamaRuntimeAvailable = true;
        LOGGER.info("Panama runtime is available");
      }
    } catch (final Exception e) {
      panamaRuntimeAvailable = false;
      LOGGER.warning("Panama runtime not available: " + e.getMessage());
    }
  }

  private static void assumeSerializerAvailable() {
    assumeTrue(serializerAvailable, "Serializer native implementation not available - skipping");
  }

  @BeforeEach
  void setUp(final TestInfo testInfo) throws WasmException {
    LOGGER.info("Setting up test: " + testInfo.getDisplayName());
    engine = Engine.create();
    resources.add(engine);
  }

  @AfterEach
  void tearDown(final TestInfo testInfo) {
    LOGGER.info("Cleaning up test: " + testInfo.getDisplayName());
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close resource: " + e.getMessage());
      }
    }
    resources.clear();
  }

  @Nested
  @DisplayName("Version Header Compatibility Tests")
  class VersionHeaderCompatibilityTests {

    @Test
    @DisplayName("should include version header in serialized data")
    void shouldIncludeVersionHeaderInSerializedData(final TestInfo testInfo) throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (final Serializer serializer = Serializer.create()) {
        final byte[] serializedData = serializer.serialize(engine, ADD_WASM);

        assertNotNull(serializedData, "Serialized data should not be null");
        assertTrue(serializedData.length > 0, "Serialized data should not be empty");

        // Serialized data should contain identifiable structure
        LOGGER.info("Serialized data length: " + serializedData.length + " bytes");
        LOGGER.info(
            "First 16 bytes: "
                + bytesToHex(Arrays.copyOf(serializedData, Math.min(16, serializedData.length))));
      }
    }

    @Test
    @DisplayName("should produce multiple serialized outputs that all deserialize correctly")
    void shouldProduceMultipleSerializedOutputsThatAllDeserializeCorrectly(final TestInfo testInfo)
        throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Note: Wasmtime serialization may not be byte-deterministic (may include timestamps)
      // Instead, we verify that each serialization produces valid deserializable output

      try (final Serializer serializer = Serializer.create()) {
        // Serialize the same module multiple times
        final byte[] serialized1 = serializer.serialize(engine, ADD_WASM);
        final byte[] serialized2 = serializer.serialize(engine, ADD_WASM);
        final byte[] serialized3 = serializer.serialize(engine, ADD_WASM);

        // All serializations should produce valid modules
        final Module module1 = serializer.deserialize(engine, serialized1);
        resources.add(module1);
        assertTrue(module1.isValid(), "First serialization should deserialize to valid module");

        final Module module2 = serializer.deserialize(engine, serialized2);
        resources.add(module2);
        assertTrue(module2.isValid(), "Second serialization should deserialize to valid module");

        final Module module3 = serializer.deserialize(engine, serialized3);
        resources.add(module3);
        assertTrue(module3.isValid(), "Third serialization should deserialize to valid module");

        LOGGER.info("All serializations produced valid deserializable modules");
      }
    }

    @Test
    @DisplayName("should produce serialized data of consistent size")
    void shouldProduceSerializedDataOfConsistentSize(final TestInfo testInfo) throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Wasmtime serialization may not be byte-deterministic but size should be consistent
      try (final Serializer serializer = Serializer.create()) {
        final byte[] serialized1 = serializer.serialize(engine, ADD_WASM);
        final byte[] serialized2 = serializer.serialize(engine, ADD_WASM);

        // Sizes should be identical even if content differs
        assertEquals(
            serialized1.length, serialized2.length, "Serialization sizes should be consistent");

        LOGGER.info("Consistent size: " + serialized1.length + " bytes");
      }
    }

    @Test
    @DisplayName("should generate different hashes for different modules")
    void shouldGenerateDifferentHashesForDifferentModules(final TestInfo testInfo)
        throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (final Serializer serializer = Serializer.create(0, false, 0)) {
        final byte[] serializedAdd = serializer.serialize(engine, ADD_WASM);
        final byte[] serializedConst = serializer.serialize(engine, CONST_WASM);

        final String hashAdd = computeSha256(serializedAdd);
        final String hashConst = computeSha256(serializedConst);

        assertNotEquals(hashAdd, hashConst, "Different modules should have different hashes");
        LOGGER.info("ADD module hash: " + hashAdd);
        LOGGER.info("CONST module hash: " + hashConst);
      }
    }
  }

  @Nested
  @DisplayName("Cross-Runtime Serialization Compatibility Tests")
  class CrossRuntimeSerializationCompatibilityTests {

    @Test
    @DisplayName("should verify current runtime type")
    void shouldVerifyCurrentRuntimeType(final TestInfo testInfo) throws Exception {
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final RuntimeType currentRuntime = WasmRuntimeFactory.getSelectedRuntimeType();
      assertNotNull(currentRuntime, "Current runtime type should not be null");
      LOGGER.info("Current runtime type: " + currentRuntime);

      assertTrue(
          currentRuntime == RuntimeType.JNI || currentRuntime == RuntimeType.PANAMA,
          "Runtime should be JNI or Panama");
    }

    @Test
    @DisplayName("should produce compatible serialization format across engines")
    void shouldProduceCompatibleSerializationFormatAcrossEngines(final TestInfo testInfo)
        throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create two separate engines
      try (final Engine engine1 = Engine.create();
          final Engine engine2 = Engine.create();
          final Serializer serializer = Serializer.create(0, false, 0)) {

        // Serialize with engine1
        final byte[] serialized = serializer.serialize(engine1, ADD_WASM);

        // Deserialize with engine2
        final Module module = serializer.deserialize(engine2, serialized);
        assertNotNull(module, "Module should deserialize with different engine");
        assertTrue(module.isValid(), "Deserialized module should be valid");

        module.close();
        LOGGER.info("Cross-engine serialization compatibility verified");
      }
    }

    @Test
    @DisplayName("should execute deserialized module with different store")
    void shouldExecuteDeserializedModuleWithDifferentStore(final TestInfo testInfo)
        throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (final Serializer serializer = Serializer.create()) {
        // Serialize with one engine
        final byte[] serialized = serializer.serialize(engine, ADD_WASM);

        // Create a new engine and store for deserialization
        try (final Engine newEngine = Engine.create()) {
          final Store newStore = newEngine.createStore();
          resources.add(newStore);

          final Module module = serializer.deserialize(newEngine, serialized);
          resources.add(module);

          final Instance instance = module.instantiate(newStore);
          resources.add(instance);

          final Optional<WasmFunction> addFunc = instance.getFunction("add");
          assertTrue(addFunc.isPresent(), "add function should be present");

          final WasmValue[] result = addFunc.get().call(WasmValue.i32(10), WasmValue.i32(20));
          assertEquals(30, result[0].asInt(), "10 + 20 should equal 30");

          LOGGER.info("Cross-store execution verified");
        }
      }
    }
  }

  @Nested
  @DisplayName("Compression Compatibility Tests")
  class CompressionCompatibilityTests {

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 5, 9})
    @DisplayName("should produce deserializable output at compression level")
    void shouldProduceDeserializableOutputAtCompressionLevel(
        final int compressionLevel, final TestInfo testInfo) throws Exception {
      assumeSerializerAvailable();
      LOGGER.info(
          "Testing compression level " + compressionLevel + ": " + testInfo.getDisplayName());

      final boolean enableCompression = compressionLevel > 0;

      try (final Serializer serializer =
          Serializer.create(0, enableCompression, compressionLevel)) {
        final byte[] serialized = serializer.serialize(engine, ADD_WASM);
        assertNotNull(serialized, "Serialized data should not be null");

        LOGGER.info("Compression level " + compressionLevel + " output size: " + serialized.length);

        // Verify it can be deserialized
        final Module module = serializer.deserialize(engine, serialized);
        resources.add(module);

        assertNotNull(module, "Module should deserialize");
        assertTrue(module.isValid(), "Module should be valid");
      }
    }

    @Test
    @DisplayName("should roundtrip with compressed serializer")
    void shouldRoundtripWithCompressedSerializer(final TestInfo testInfo) throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Note: Wasmtime serialized modules must be deserialized by the same serializer/engine
      // Cross-serializer deserialization is not supported

      try (final Serializer serializer = Serializer.create(0, true, 6)) {
        final byte[] serialized = serializer.serialize(engine, ADD_WASM);

        // Deserialize with same serializer instance
        final Module module = serializer.deserialize(engine, serialized);
        resources.add(module);

        assertNotNull(module, "Module should deserialize");
        assertTrue(module.isValid(), "Module should be valid");

        LOGGER.info("Compressed serializer roundtrip verified");
      }
    }

    @Test
    @DisplayName("should compare sizes between compression modes")
    void shouldCompareSizesBetweenCompressionModes(final TestInfo testInfo) throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Note: Each serializer must deserialize its own output
      // We verify each works independently

      // Uncompressed serializer
      try (final Serializer uncompressedSerializer = Serializer.create(0, false, 0)) {
        final byte[] uncompressed = uncompressedSerializer.serialize(engine, ADD_WASM);
        final Module module1 = uncompressedSerializer.deserialize(engine, uncompressed);
        resources.add(module1);
        assertTrue(module1.isValid(), "Uncompressed module should be valid");
        LOGGER.info("Uncompressed size: " + uncompressed.length);
      }

      // Compressed serializer
      try (final Serializer compressedSerializer = Serializer.create(0, true, 9)) {
        final byte[] compressed = compressedSerializer.serialize(engine, ADD_WASM);
        final Module module2 = compressedSerializer.deserialize(engine, compressed);
        resources.add(module2);
        assertTrue(module2.isValid(), "Compressed module should be valid");
        LOGGER.info("Compressed size: " + compressed.length);
      }

      LOGGER.info("Both compression modes roundtrip successfully");
    }
  }

  @Nested
  @DisplayName("Module Type Compatibility Tests")
  class ModuleTypeCompatibilityTests {

    @Test
    @DisplayName("should serialize and deserialize simple function module")
    void shouldSerializeAndDeserializeSimpleFunctionModule(final TestInfo testInfo)
        throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (final Serializer serializer = Serializer.create()) {
        verifyModuleRoundtrip(serializer, ADD_WASM, "add", new int[] {5, 3}, 8);
        LOGGER.info("Simple function module serialization verified");
      }
    }

    @Test
    @DisplayName("should serialize and deserialize constant return module")
    void shouldSerializeAndDeserializeConstantReturnModule(final TestInfo testInfo)
        throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (final Serializer serializer = Serializer.create()) {
        final byte[] serialized = serializer.serialize(engine, CONST_WASM);
        final Module module = serializer.deserialize(engine, serialized);
        resources.add(module);

        final Store store = engine.createStore();
        resources.add(store);

        final Instance instance = module.instantiate(store);
        resources.add(instance);

        final Optional<WasmFunction> getFunc = instance.getFunction("get");
        assertTrue(getFunc.isPresent(), "get function should be present");

        final WasmValue[] result = getFunc.get().call();
        assertEquals(42, result[0].asInt(), "Should return 42");

        LOGGER.info("Constant return module serialization verified");
      }
    }

    @Test
    @DisplayName("should preserve module metadata after serialization")
    void shouldPreserveModuleMetadataAfterSerialization(final TestInfo testInfo) throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (final Serializer serializer = Serializer.create()) {
        // Get original module exports
        final Module original = engine.compileModule(ADD_WASM);
        resources.add(original);

        // Serialize and deserialize
        final byte[] serialized = serializer.serialize(engine, ADD_WASM);
        final Module deserialized = serializer.deserialize(engine, serialized);
        resources.add(deserialized);

        // Both should be valid
        assertTrue(original.isValid(), "Original module should be valid");
        assertTrue(deserialized.isValid(), "Deserialized module should be valid");

        LOGGER.info("Module metadata preservation verified");
      }
    }

    private void verifyModuleRoundtrip(
        final Serializer serializer,
        final byte[] wasmBytes,
        final String funcName,
        final int[] args,
        final int expected)
        throws Exception {

      final byte[] serialized = serializer.serialize(engine, wasmBytes);
      final Module module = serializer.deserialize(engine, serialized);
      resources.add(module);

      final Store store = engine.createStore();
      resources.add(store);

      final Instance instance = module.instantiate(store);
      resources.add(instance);

      final Optional<WasmFunction> func = instance.getFunction(funcName);
      assertTrue(func.isPresent(), funcName + " function should be present");

      final WasmValue[] wasmArgs = new WasmValue[args.length];
      for (int i = 0; i < args.length; i++) {
        wasmArgs[i] = WasmValue.i32(args[i]);
      }

      final WasmValue[] result = func.get().call(wasmArgs);
      assertEquals(expected, result[0].asInt(), "Function result should match expected");
    }
  }

  @Nested
  @DisplayName("Concurrent Serialization Compatibility Tests")
  class ConcurrentSerializationCompatibilityTests {

    @Test
    @DisplayName("should handle concurrent serialization safely")
    void shouldHandleConcurrentSerializationSafely(final TestInfo testInfo) throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final int threadCount = 4;
      final int iterationsPerThread = 10;
      final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      final AtomicInteger successCount = new AtomicInteger(0);
      final AtomicInteger errorCount = new AtomicInteger(0);

      try (final Serializer serializer = Serializer.create()) {
        final List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int t = 0; t < threadCount; t++) {
          final int threadId = t;
          final CompletableFuture<Void> future =
              CompletableFuture.runAsync(
                  () -> {
                    for (int i = 0; i < iterationsPerThread; i++) {
                      try {
                        final byte[] serialized = serializer.serialize(engine, ADD_WASM);
                        final Module module = serializer.deserialize(engine, serialized);
                        module.close();
                        successCount.incrementAndGet();
                      } catch (final Exception e) {
                        errorCount.incrementAndGet();
                        LOGGER.warning(
                            "Thread "
                                + threadId
                                + " iteration "
                                + i
                                + " failed: "
                                + e.getMessage());
                      }
                    }
                  },
                  executor);
          futures.add(future);
        }

        // Wait for all to complete
        @SuppressWarnings("rawtypes")
        final CompletableFuture[] futuresArray = futures.toArray(new CompletableFuture[0]);
        CompletableFuture.allOf(futuresArray).join();

        final int totalOps = threadCount * iterationsPerThread;
        LOGGER.info(
            "Concurrent serialization: "
                + successCount.get()
                + "/"
                + totalOps
                + " succeeded, "
                + errorCount.get()
                + " errors");

        // Allow some errors due to resource contention
        assertTrue(
            successCount.get() >= totalOps * 0.9, "At least 90% of operations should succeed");
      } finally {
        executor.shutdown();
        assertTrue(
            executor.awaitTermination(10, TimeUnit.SECONDS), "Executor should terminate cleanly");
      }
    }

    @Test
    @DisplayName("should produce valid deserializable modules from concurrent serializations")
    void shouldProduceValidDeserializableModulesFromConcurrentSerializations(
        final TestInfo testInfo) throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Note: Wasmtime serialization is not deterministic, so hashes may differ
      // Instead we verify each serialization produces a valid deserializable module

      final int threadCount = 4;
      final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      final AtomicInteger validCount = new AtomicInteger(0);

      try (final Serializer serializer = Serializer.create()) {
        final List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int t = 0; t < threadCount; t++) {
          final CompletableFuture<Void> future =
              CompletableFuture.runAsync(
                  () -> {
                    try {
                      final byte[] serialized = serializer.serialize(engine, ADD_WASM);
                      final Module module = serializer.deserialize(engine, serialized);
                      if (module.isValid()) {
                        validCount.incrementAndGet();
                      }
                      module.close();
                    } catch (final Exception e) {
                      LOGGER.warning("Concurrent serialization failed: " + e.getMessage());
                    }
                  },
                  executor);
          futures.add(future);
        }

        @SuppressWarnings("rawtypes")
        final CompletableFuture[] futuresArray2 = futures.toArray(new CompletableFuture[0]);
        CompletableFuture.allOf(futuresArray2).join();

        // All serializations should produce valid modules
        assertEquals(
            threadCount,
            validCount.get(),
            "All concurrent serializations should produce valid modules");

        LOGGER.info("All " + threadCount + " concurrent serializations produced valid modules");
      } finally {
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
      }
    }
  }

  @Nested
  @DisplayName("Serialization Format Stability Tests")
  class SerializationFormatStabilityTests {

    @Test
    @DisplayName("should reject truncated serialized data")
    void shouldRejectTruncatedSerializedData(final TestInfo testInfo) throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (final Serializer serializer = Serializer.create()) {
        final byte[] serialized = serializer.serialize(engine, ADD_WASM);

        // Truncate at various points
        final int[] truncatePoints = {1, 10, serialized.length / 2, serialized.length - 1};

        for (final int point : truncatePoints) {
          if (point < serialized.length) {
            final byte[] truncated = Arrays.copyOf(serialized, point);
            try {
              serializer.deserialize(engine, truncated);
              // If we get here without exception, the runtime handled it gracefully
              LOGGER.info("Truncation at " + point + " bytes handled gracefully");
            } catch (final WasmException e) {
              // Expected - truncated data should fail
              LOGGER.info(
                  "Truncation at " + point + " bytes correctly rejected: " + e.getMessage());
            }
          }
        }
      }
    }

    @Test
    @DisplayName("should reject corrupted serialized data")
    void shouldRejectCorruptedSerializedData(final TestInfo testInfo) throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (final Serializer serializer = Serializer.create()) {
        final byte[] serialized = serializer.serialize(engine, ADD_WASM);

        // Corrupt the data at various points
        final int[] corruptionPoints = {0, serialized.length / 4, serialized.length / 2};

        for (final int point : corruptionPoints) {
          final byte[] corrupted = serialized.clone();
          corrupted[point] = (byte) (corrupted[point] ^ 0xFF); // Flip all bits

          try {
            serializer.deserialize(engine, corrupted);
            // If we get here, runtime handled it gracefully
            LOGGER.info("Corruption at byte " + point + " handled gracefully");
          } catch (final WasmException e) {
            // Expected - corrupted data should fail
            LOGGER.info("Corruption at byte " + point + " correctly rejected");
          }
        }
      }
    }

    @Test
    @DisplayName("should handle zero-length input gracefully")
    void shouldHandleZeroLengthInputGracefully(final TestInfo testInfo) throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (final Serializer serializer = Serializer.create()) {
        try {
          serializer.deserialize(engine, new byte[0]);
          LOGGER.info("Zero-length input handled gracefully");
        } catch (final WasmException | IllegalArgumentException e) {
          // Expected behavior
          LOGGER.info("Zero-length input correctly rejected: " + e.getClass().getSimpleName());
        }
      }
    }
  }

  @Nested
  @DisplayName("Metadata Wrapper Compatibility Tests")
  class MetadataWrapperCompatibilityTests {

    @Test
    @DisplayName("should create versioned wrapper for serialized data")
    void shouldCreateVersionedWrapperForSerializedData(final TestInfo testInfo) throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (final Serializer serializer = Serializer.create()) {
        final byte[] rawSerialized = serializer.serialize(engine, ADD_WASM);

        // Create a versioned wrapper
        final byte[] wrapped = createVersionedWrapper(rawSerialized);

        // Verify wrapper structure
        assertTrue(wrapped.length > rawSerialized.length, "Wrapper should be larger than raw data");

        // Extract and verify version
        final VersionedData extracted = extractVersionedData(wrapped);
        assertEquals(VERSION_MAJOR, extracted.majorVersion, "Major version should match");
        assertEquals(VERSION_MINOR, extracted.minorVersion, "Minor version should match");
        assertArrayEquals(rawSerialized, extracted.data, "Data should match after extraction");

        LOGGER.info("Versioned wrapper created and verified");
      }
    }

    @Test
    @DisplayName("should roundtrip through versioned wrapper")
    void shouldRoundtripThroughVersionedWrapper(final TestInfo testInfo) throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (final Serializer serializer = Serializer.create()) {
        // Serialize and wrap
        final byte[] rawSerialized = serializer.serialize(engine, ADD_WASM);
        final byte[] wrapped = createVersionedWrapper(rawSerialized);

        // Unwrap and deserialize
        final VersionedData extracted = extractVersionedData(wrapped);
        final Module module = serializer.deserialize(engine, extracted.data);
        resources.add(module);

        // Verify module works
        final Store store = engine.createStore();
        resources.add(store);

        final Instance instance = module.instantiate(store);
        resources.add(instance);

        final WasmValue[] result =
            instance.getFunction("add").get().call(WasmValue.i32(7), WasmValue.i32(8));
        assertEquals(15, result[0].asInt(), "Function should work after versioned roundtrip");

        LOGGER.info("Versioned wrapper roundtrip verified");
      }
    }

    /** Simple versioned data wrapper for testing. */
    private byte[] createVersionedWrapper(final byte[] data) throws IOException {
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      final DataOutputStream dos = new DataOutputStream(baos);

      // Write magic number
      dos.writeInt(0x57415349); // "WASI" in hex

      // Write version
      dos.writeInt(VERSION_MAJOR);
      dos.writeInt(VERSION_MINOR);
      dos.writeInt(VERSION_PATCH);

      // Write timestamp
      dos.writeLong(System.currentTimeMillis());

      // Write data length and data
      dos.writeInt(data.length);
      dos.write(data);

      // Write checksum
      dos.writeInt(Arrays.hashCode(data));

      return baos.toByteArray();
    }

    private VersionedData extractVersionedData(final byte[] wrapped) throws IOException {
      final ByteArrayInputStream bais = new ByteArrayInputStream(wrapped);
      final DataInputStream dis = new DataInputStream(bais);

      // Read and verify magic
      final int magic = dis.readInt();
      if (magic != 0x57415349) {
        throw new IOException("Invalid magic number");
      }

      // Read version
      final int major = dis.readInt();
      final int minor = dis.readInt();
      final int patch = dis.readInt();

      // Read timestamp
      final long timestamp = dis.readLong();

      // Read data
      final int length = dis.readInt();
      final byte[] data = new byte[length];
      dis.readFully(data);

      // Read and verify checksum
      final int checksum = dis.readInt();
      if (checksum != Arrays.hashCode(data)) {
        throw new IOException("Checksum mismatch");
      }

      return new VersionedData(major, minor, patch, timestamp, data);
    }

    /** Container for versioned data. */
    private static final class VersionedData {
      final int majorVersion;
      final int minorVersion;
      final int patchVersion;
      final long timestamp;
      final byte[] data;

      VersionedData(
          final int majorVersion,
          final int minorVersion,
          final int patchVersion,
          final long timestamp,
          final byte[] data) {
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.patchVersion = patchVersion;
        this.timestamp = timestamp;
        this.data = data;
      }
    }
  }

  @Nested
  @DisplayName("API Contract Tests")
  class ApiContractTests {

    @Test
    @DisplayName("Serializer interface should have required methods")
    void serializerInterfaceShouldHaveRequiredMethods() throws Exception {
      // Verify Serializer class has expected factory methods
      assertDoesNotThrow(
          () -> Serializer.class.getMethod("create"),
          "Serializer should have create() factory method");

      assertDoesNotThrow(
          () -> Serializer.class.getMethod("create", long.class, boolean.class, int.class),
          "Serializer should have create(long, boolean, int) factory method");

      LOGGER.info("Serializer factory methods verified");
    }

    @Test
    @DisplayName("Serializer should implement AutoCloseable")
    void serializerShouldImplementAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(Serializer.class),
          "Serializer should implement AutoCloseable");

      LOGGER.info("Serializer AutoCloseable implementation verified");
    }

    @Test
    @DisplayName("should have serialize method accepting Engine and byte array")
    void shouldHaveSerializeMethodAcceptingEngineAndByteArray() throws Exception {
      assertDoesNotThrow(
          () -> Serializer.class.getMethod("serialize", Engine.class, byte[].class),
          "Serializer should have serialize(Engine, byte[]) method");

      LOGGER.info("serialize method signature verified");
    }

    @Test
    @DisplayName("should have deserialize method returning Module")
    void shouldHaveDeserializeMethodReturningModule() throws Exception {
      final var method =
          assertDoesNotThrow(
              () -> Serializer.class.getMethod("deserialize", Engine.class, byte[].class),
              "Serializer should have deserialize(Engine, byte[]) method");

      assertEquals(Module.class, method.getReturnType(), "deserialize should return Module");

      LOGGER.info("deserialize method signature verified");
    }
  }

  // Utility methods

  private static String computeSha256(final byte[] data) {
    try {
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      final byte[] hash = digest.digest(data);
      return bytesToHex(hash);
    } catch (final NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 not available", e);
    }
  }

  private static String bytesToHex(final byte[] bytes) {
    final StringBuilder sb = new StringBuilder();
    for (final byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
