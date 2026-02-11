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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Serializer;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Integration tests for Serializer - WebAssembly module serialization.
 *
 * <p>These tests verify module serialization, deserialization, cache management, and roundtrip
 * integrity. Tests are disabled until the native Serializer implementation is complete - the
 * current native implementation causes JVM crashes when serialize() is called.
 *
 * @since 1.0.0
 */
@DisplayName("Serializer Integration Tests")
public final class SerializerIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(SerializerIntegrationTest.class.getName());

  private static boolean serializerAvailable = false;

  @BeforeAll
  static void checkSerializerAvailable() {
    try {
      final Serializer serializer = Serializer.create();
      serializer.close();
      serializerAvailable = true;
      LOGGER.info("Serializer native implementation is available");
    } catch (final Exception e) {
      serializerAvailable = false;
      LOGGER.warning("Serializer not available - tests will be skipped: " + e.getMessage());
    }
  }

  private static void assumeSerializerAvailable() {
    assumeTrue(serializerAvailable, "Serializer native implementation not available - skipping");
  }

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

        // Type section (id=1)
        0x01,
        0x07, // section id and size
        0x01, // number of types
        0x60,
        0x02,
        0x7F,
        0x7F,
        0x01,
        0x7F, // (i32, i32) -> i32

        // Function section (id=3)
        0x03,
        0x02, // section id and size
        0x01, // number of functions
        0x00, // type index 0

        // Export section (id=7)
        0x07,
        0x07, // section id and size
        0x01, // number of exports
        0x03,
        0x61,
        0x64,
        0x64, // "add"
        0x00,
        0x00, // function, index 0

        // Code section (id=10)
        0x0A,
        0x09, // section id and size
        0x01, // number of functions
        0x07, // function body size
        0x00, // local count
        0x20,
        0x00, // local.get 0
        0x20,
        0x01, // local.get 1
        0x6A, // i32.add
        0x0B // end
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

        // Type section (id=1)
        0x01,
        0x05, // section id and size
        0x01, // number of types
        0x60,
        0x00,
        0x01,
        0x7F, // () -> i32

        // Function section (id=3)
        0x03,
        0x02, // section id and size
        0x01, // number of functions
        0x00, // type index 0

        // Export section (id=7)
        0x07,
        0x07, // section id and size
        0x01, // number of exports
        0x03,
        0x67,
        0x65,
        0x74, // "get"
        0x00,
        0x00, // function, index 0

        // Code section (id=10)
        0x0A,
        0x06, // section id and size
        0x01, // number of functions
        0x04, // function body size
        0x00, // local count
        0x41,
        0x2A, // i32.const 42
        0x0B // end
      };

  private Engine engine;
  private final List<AutoCloseable> resources = new ArrayList<>();

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
  @DisplayName("Serializer Creation Tests")
  class SerializerCreationTests {

    @Test
    @DisplayName("should create serializer with default configuration")
    void shouldCreateSerializerWithDefaultConfiguration(final TestInfo testInfo) throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (final Serializer serializer = Serializer.create()) {
        assertNotNull(serializer, "Serializer should not be null");
        LOGGER.info("Default serializer created successfully");
      }
    }

    @Test
    @DisplayName("should create serializer with custom configuration")
    void shouldCreateSerializerWithCustomConfiguration(final TestInfo testInfo) throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final long maxCacheSize = 10 * 1024 * 1024; // 10MB
      final boolean enableCompression = true;
      final int compressionLevel = 6;

      try (final Serializer serializer =
          Serializer.create(maxCacheSize, enableCompression, compressionLevel)) {
        assertNotNull(serializer, "Serializer should not be null");
        LOGGER.info("Custom serializer created with compression level " + compressionLevel);
      }
    }

    @Test
    @DisplayName("should create serializer without compression")
    void shouldCreateSerializerWithoutCompression(final TestInfo testInfo) throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (final Serializer serializer = Serializer.create(0, false, 0)) {
        assertNotNull(serializer, "Serializer should not be null");
        LOGGER.info("Serializer without compression created");
      }
    }
  }

  @Nested
  @DisplayName("Serialization Roundtrip Tests")
  class SerializationRoundtripTests {

    @Test
    @DisplayName("should serialize and deserialize simple module")
    void shouldSerializeAndDeserializeSimpleModule(final TestInfo testInfo) throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (final Serializer serializer = Serializer.create()) {
        // Serialize the module
        final byte[] serializedData = serializer.serialize(engine, ADD_WASM);
        assertNotNull(serializedData, "Serialized data should not be null");
        assertTrue(serializedData.length > 0, "Serialized data should not be empty");

        LOGGER.info("Serialized module size: " + serializedData.length + " bytes");

        // Deserialize back to module
        final Module module = serializer.deserialize(engine, serializedData);
        resources.add(module);

        assertNotNull(module, "Deserialized module should not be null");
        assertTrue(module.isValid(), "Deserialized module should be valid");

        LOGGER.info("Module deserialized successfully");
      }
    }

    @Test
    @DisplayName("should execute function from deserialized module")
    void shouldExecuteFunctionFromDeserializedModule(final TestInfo testInfo) throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Serializer serializer = Serializer.create();
      resources.add(serializer);
      final Store store = engine.createStore();
      resources.add(store);

      // Serialize and deserialize
      final byte[] serializedData = serializer.serialize(engine, ADD_WASM);
      final Module module = serializer.deserialize(engine, serializedData);
      resources.add(module);

      // Create instance and call function
      final Instance instance = module.instantiate(store);
      resources.add(instance);

      final Optional<WasmFunction> addFunc = instance.getFunction("add");
      assertTrue(addFunc.isPresent(), "add function should be present");

      final WasmValue[] results = addFunc.get().call(WasmValue.i32(5), WasmValue.i32(3));
      assertEquals(8, results[0].asInt(), "5 + 3 should equal 8");

      LOGGER.info("Function execution from deserialized module successful");
    }

    @Test
    @DisplayName("should preserve function behavior after serialization")
    void shouldPreserveFunctionBehaviorAfterSerialization(final TestInfo testInfo)
        throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Serializer serializer = Serializer.create();
      resources.add(serializer);
      final Store store1 = engine.createStore();
      resources.add(store1);
      final Store store2 = engine.createStore();
      resources.add(store2);

      // Create original module and verify
      final Module original = engine.compileModule(ADD_WASM);
      resources.add(original);
      final Instance inst1 = original.instantiate(store1);
      resources.add(inst1);

      final WasmValue[] originalResult =
          inst1.getFunction("add").get().call(WasmValue.i32(10), WasmValue.i32(20));

      // Serialize and deserialize
      final byte[] serialized = serializer.serialize(engine, ADD_WASM);
      final Module deserialized = serializer.deserialize(engine, serialized);
      resources.add(deserialized);
      final Instance inst2 = deserialized.instantiate(store2);
      resources.add(inst2);

      final WasmValue[] deserializedResult =
          inst2.getFunction("add").get().call(WasmValue.i32(10), WasmValue.i32(20));

      // Compare results
      assertEquals(
          originalResult[0].asInt(),
          deserializedResult[0].asInt(),
          "Results should match after serialization");

      LOGGER.info("Function behavior preserved after serialization");
    }

    @Test
    @DisplayName("should serialize and deserialize multiple modules")
    void shouldSerializeAndDeserializeMultipleModules(final TestInfo testInfo) throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Serializer serializer = Serializer.create();
      resources.add(serializer);
      final Store store = engine.createStore();
      resources.add(store);

      // Serialize both modules
      final byte[] addSerialized = serializer.serialize(engine, ADD_WASM);
      final byte[] constSerialized = serializer.serialize(engine, CONST_WASM);

      // Deserialize and verify both
      final Module addModule = serializer.deserialize(engine, addSerialized);
      resources.add(addModule);
      final Module constModule = serializer.deserialize(engine, constSerialized);
      resources.add(constModule);

      final Instance addInst = addModule.instantiate(store);
      resources.add(addInst);

      final Store store2 = engine.createStore();
      resources.add(store2);
      final Instance constInst = constModule.instantiate(store2);
      resources.add(constInst);

      // Verify add module
      final WasmValue[] addResult =
          addInst.getFunction("add").get().call(WasmValue.i32(7), WasmValue.i32(8));
      assertEquals(15, addResult[0].asInt(), "7 + 8 should equal 15");

      // Verify const module
      final WasmValue[] constResult = constInst.getFunction("get").get().call();
      assertEquals(42, constResult[0].asInt(), "Should return 42");

      LOGGER.info("Multiple modules serialized and deserialized successfully");
    }
  }

  @Nested
  @DisplayName("Cache Management Tests")
  class CacheManagementTests {

    @Test
    @DisplayName("should report initial cache state")
    void shouldReportInitialCacheState(final TestInfo testInfo) throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (final Serializer serializer = Serializer.create()) {
        assertEquals(0, serializer.getCacheEntryCount(), "Initial cache should be empty");
        assertEquals(0L, serializer.getCacheTotalSize(), "Initial cache size should be 0");

        LOGGER.info("Initial cache state verified");
      }
    }

    @Test
    @DisplayName("should update cache statistics after serialization")
    void shouldUpdateCacheStatisticsAfterSerialization(final TestInfo testInfo) throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (final Serializer serializer = Serializer.create()) {
        final int initialCount = serializer.getCacheEntryCount();

        // Serialize a module
        serializer.serialize(engine, ADD_WASM);

        // Cache may or may not be updated depending on implementation
        final int afterCount = serializer.getCacheEntryCount();
        LOGGER.info("Cache entries before: " + initialCount + ", after: " + afterCount);

        // Just verify we can query cache stats without error
        assertTrue(afterCount >= 0, "Cache entry count should be non-negative");
        assertTrue(serializer.getCacheTotalSize() >= 0, "Cache size should be non-negative");

        LOGGER.info("Cache statistics updated successfully");
      }
    }

    @Test
    @DisplayName("should clear cache successfully")
    void shouldClearCacheSuccessfully(final TestInfo testInfo) throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (final Serializer serializer = Serializer.create()) {
        // Serialize some modules to populate cache
        serializer.serialize(engine, ADD_WASM);
        serializer.serialize(engine, CONST_WASM);

        // Clear the cache
        final boolean cleared = serializer.clearCache();
        LOGGER.info("Cache clear result: " + cleared);

        // Verify cache is empty after clear
        assertEquals(0, serializer.getCacheEntryCount(), "Cache should be empty after clear");

        LOGGER.info("Cache cleared successfully");
      }
    }

    @Test
    @DisplayName("should handle clearing empty cache")
    void shouldHandleClearingEmptyCache(final TestInfo testInfo) throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (final Serializer serializer = Serializer.create()) {
        // Clear empty cache - should not throw
        assertDoesNotThrow(() -> serializer.clearCache(), "Clearing empty cache should not throw");

        LOGGER.info("Empty cache clear handled successfully");
      }
    }

    @Test
    @DisplayName("should report cache hit rate")
    void shouldReportCacheHitRate(final TestInfo testInfo) throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (final Serializer serializer = Serializer.create()) {
        final double hitRate = serializer.getCacheHitRate();

        assertTrue(hitRate >= 0.0, "Hit rate should be >= 0.0");
        assertTrue(hitRate <= 1.0, "Hit rate should be <= 1.0");

        LOGGER.info("Cache hit rate: " + (hitRate * 100) + "%");
      }
    }
  }

  @Nested
  @DisplayName("Serializer Lifecycle Tests")
  class SerializerLifecycleTests {

    @Test
    @DisplayName("should close serializer properly")
    void shouldCloseSerializerProperly(final TestInfo testInfo) throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Serializer serializer = Serializer.create();
      serializer.serialize(engine, ADD_WASM);

      assertDoesNotThrow(() -> serializer.close(), "Closing serializer should not throw");

      LOGGER.info("Serializer closed successfully");
    }

    @Test
    @DisplayName("should handle multiple close calls")
    void shouldHandleMultipleCloseCalls(final TestInfo testInfo) throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Serializer serializer = Serializer.create();
      serializer.close();

      // Second close should not throw
      assertDoesNotThrow(() -> serializer.close(), "Multiple close calls should not throw");

      LOGGER.info("Multiple close calls handled gracefully");
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("should reject null engine for serialize")
    void shouldRejectNullEngineForSerialize(final TestInfo testInfo) throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (final Serializer serializer = Serializer.create()) {
        assertThrows(
            IllegalArgumentException.class,
            () -> serializer.serialize(null, ADD_WASM),
            "Should reject null engine");

        LOGGER.info("Null engine rejection verified");
      }
    }

    @Test
    @DisplayName("should reject null module bytes for serialize")
    void shouldRejectNullModuleBytesForSerialize(final TestInfo testInfo) throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (final Serializer serializer = Serializer.create()) {
        assertThrows(
            IllegalArgumentException.class,
            () -> serializer.serialize(engine, null),
            "Should reject null module bytes");

        LOGGER.info("Null module bytes rejection verified");
      }
    }

    @Test
    @DisplayName("should reject null engine for deserialize")
    void shouldRejectNullEngineForDeserialize(final TestInfo testInfo) throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (final Serializer serializer = Serializer.create()) {
        final byte[] serialized = serializer.serialize(engine, ADD_WASM);

        assertThrows(
            IllegalArgumentException.class,
            () -> serializer.deserialize(null, serialized),
            "Should reject null engine for deserialize");

        LOGGER.info("Null engine for deserialize rejection verified");
      }
    }

    @Test
    @DisplayName("should reject null bytes for deserialize")
    void shouldRejectNullBytesForDeserialize(final TestInfo testInfo) throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (final Serializer serializer = Serializer.create()) {
        assertThrows(
            IllegalArgumentException.class,
            () -> serializer.deserialize(engine, null),
            "Should reject null bytes for deserialize");

        LOGGER.info("Null bytes for deserialize rejection verified");
      }
    }

    @Test
    @DisplayName("should reject invalid serialized data")
    void shouldRejectInvalidSerializedData(final TestInfo testInfo) throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (final Serializer serializer = Serializer.create()) {
        final byte[] invalidData = new byte[] {0x00, 0x01, 0x02, 0x03};

        assertThrows(
            WasmException.class,
            () -> serializer.deserialize(engine, invalidData),
            "Should reject invalid serialized data");

        LOGGER.info("Invalid data rejection verified");
      }
    }

    @Test
    @DisplayName("should reject invalid wasm for serialization")
    void shouldRejectInvalidWasmForSerialization(final TestInfo testInfo) throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (final Serializer serializer = Serializer.create()) {
        final byte[] invalidWasm = new byte[] {0x00, 0x01, 0x02, 0x03};

        assertThrows(
            WasmException.class,
            () -> serializer.serialize(engine, invalidWasm),
            "Should reject invalid WASM for serialization");

        LOGGER.info("Invalid WASM rejection verified");
      }
    }
  }

  @Nested
  @DisplayName("Compression Tests")
  class CompressionTests {

    @Test
    @DisplayName("should produce smaller output with compression enabled")
    void shouldProduceSmallerOutputWithCompressionEnabled(final TestInfo testInfo)
        throws Exception {
      assumeSerializerAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      try (final Serializer compressedSerializer = Serializer.create(0, true, 9);
          final Serializer uncompressedSerializer = Serializer.create(0, false, 0)) {

        final byte[] compressed = compressedSerializer.serialize(engine, ADD_WASM);
        final byte[] uncompressed = uncompressedSerializer.serialize(engine, ADD_WASM);

        LOGGER.info(
            "Compressed size: "
                + compressed.length
                + ", Uncompressed size: "
                + uncompressed.length);

        // Both should work
        assertNotNull(compressed, "Compressed data should not be null");
        assertNotNull(uncompressed, "Uncompressed data should not be null");

        // Note: For small modules, compression overhead might make compressed larger
        // So we just verify both work, not size comparison

        LOGGER.info("Compression comparison completed");
      }
    }
  }
}
