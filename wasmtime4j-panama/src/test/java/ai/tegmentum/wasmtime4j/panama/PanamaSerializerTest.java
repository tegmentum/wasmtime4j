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

package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.WasmException;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Test for Panama serializer functionality. */
public class PanamaSerializerTest {

  // WASM bytecode for: (module (func (export "get42") (result i32) i32.const 42))
  private static final byte[] SIMPLE_MODULE_BYTES =
      new byte[] {
        0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x05, 0x01, 0x60, 0x00, 0x01,
        0x7f, 0x03, 0x02, 0x01, 0x00, 0x07, 0x09, 0x01, 0x05, 0x67, 0x65, 0x74, 0x34, 0x32,
        0x00, 0x00, 0x0a, 0x06, 0x01, 0x04, 0x00, 0x41, 0x2a, 0x0b
      };

  @Test
  @DisplayName("Serialize and deserialize module successfully")
  public void testSerializeDeserialize() throws WasmException {
    final PanamaWasmRuntime runtime = new PanamaWasmRuntime();
    final PanamaEngine engine = new PanamaEngine();
    final PanamaSerializer serializer = new PanamaSerializer(runtime);

    try {
      // Serialize the module
      final byte[] serializedBytes = serializer.serialize(engine, SIMPLE_MODULE_BYTES);
      assertNotNull(serializedBytes, "Serialized bytes should not be null");
      assertTrue(serializedBytes.length > 0, "Serialized bytes should not be empty");

      // Deserialize the module
      final Module deserializedModule = serializer.deserialize(engine, serializedBytes);
      assertNotNull(deserializedModule, "Deserialized module should not be null");
      assertTrue(deserializedModule.isValid(), "Deserialized module should be valid");

      // Verify the deserialized module works correctly
      final PanamaStore store = new PanamaStore(engine);
      final PanamaInstance instance = new PanamaInstance((PanamaModule) deserializedModule, store);

      final WasmFunction func = instance.getFunction("get42");
      assertNotNull(func, "Function should not be null");

      final WasmValue[] results = func.call();
      assertNotNull(results, "Results should not be null");
      assertEquals(1, results.length, "Should have one result");
      assertEquals(42, results[0].asI32(), "Result should be 42");

      // Cleanup
      instance.close();
      store.close();
      deserializedModule.close();
    } finally {
      serializer.close();
      engine.close();
      runtime.close();
    }
  }

  @Test
  @DisplayName("Cache hit rate tracking")
  public void testCacheHitRate() throws WasmException {
    final PanamaWasmRuntime runtime = new PanamaWasmRuntime();
    final PanamaEngine engine = new PanamaEngine();
    final PanamaSerializer serializer = new PanamaSerializer(runtime);

    try {
      // Initial hit rate should be 0.0
      assertEquals(0.0, serializer.getCacheHitRate(), 0.001, "Initial hit rate should be 0.0");
      assertEquals(0, serializer.getCacheEntryCount(), "Initial cache should be empty");

      // First serialization (cache miss)
      final byte[] serialized1 = serializer.serialize(engine, SIMPLE_MODULE_BYTES);
      assertNotNull(serialized1, "First serialization should succeed");
      assertEquals(0.0, serializer.getCacheHitRate(), 0.001, "Hit rate should still be 0.0");
      assertEquals(1, serializer.getCacheEntryCount(), "Cache should have 1 entry");

      // Second serialization of same module (cache hit)
      final byte[] serialized2 = serializer.serialize(engine, SIMPLE_MODULE_BYTES);
      assertNotNull(serialized2, "Second serialization should succeed");
      assertEquals(0.5, serializer.getCacheHitRate(), 0.001, "Hit rate should be 0.5 (1/2)");
      assertEquals(1, serializer.getCacheEntryCount(), "Cache should still have 1 entry");

      // Third serialization of same module (another cache hit)
      final byte[] serialized3 = serializer.serialize(engine, SIMPLE_MODULE_BYTES);
      assertNotNull(serialized3, "Third serialization should succeed");
      assertEquals(
          0.666, serializer.getCacheHitRate(), 0.01, "Hit rate should be ~0.666 (2/3)");
      assertEquals(1, serializer.getCacheEntryCount(), "Cache should still have 1 entry");

    } finally {
      serializer.close();
      engine.close();
      runtime.close();
    }
  }

  @Test
  @DisplayName("Clear cache functionality")
  public void testClearCache() throws WasmException {
    final PanamaWasmRuntime runtime = new PanamaWasmRuntime();
    final PanamaEngine engine = new PanamaEngine();
    final PanamaSerializer serializer = new PanamaSerializer(runtime);

    try {
      // Serialize to populate cache
      serializer.serialize(engine, SIMPLE_MODULE_BYTES);
      assertEquals(1, serializer.getCacheEntryCount(), "Cache should have 1 entry");
      assertTrue(serializer.getCacheTotalSize() > 0, "Cache size should be > 0");

      // Clear cache
      final boolean cleared = serializer.clearCache();
      assertTrue(cleared, "Cache clear should return true");
      assertEquals(0, serializer.getCacheEntryCount(), "Cache should be empty");
      assertEquals(0, serializer.getCacheTotalSize(), "Cache size should be 0");

      // Clear empty cache
      final boolean clearedAgain = serializer.clearCache();
      assertFalse(clearedAgain, "Clearing empty cache should return false");

    } finally {
      serializer.close();
      engine.close();
      runtime.close();
    }
  }

  @Test
  @DisplayName("Max cache size limit")
  public void testMaxCacheSize() throws WasmException {
    final PanamaWasmRuntime runtime = new PanamaWasmRuntime();
    final PanamaEngine engine = new PanamaEngine();

    // Create serializer with tiny cache size (1 byte - too small for any module)
    final PanamaSerializer serializer = new PanamaSerializer(runtime, 1L, false, 6);

    try {
      // Serialize (should not cache due to size limit)
      serializer.serialize(engine, SIMPLE_MODULE_BYTES);
      assertEquals(0, serializer.getCacheEntryCount(), "Cache should remain empty");
      assertEquals(0, serializer.getCacheTotalSize(), "Cache size should be 0");

    } finally {
      serializer.close();
      engine.close();
      runtime.close();
    }
  }

  @Test
  @DisplayName("Reject null engine in serialize")
  public void testSerializeNullEngine() throws WasmException {
    final PanamaWasmRuntime runtime = new PanamaWasmRuntime();
    final PanamaSerializer serializer = new PanamaSerializer(runtime);

    try {
      assertThrows(
          IllegalArgumentException.class,
          () -> serializer.serialize(null, SIMPLE_MODULE_BYTES),
          "Should reject null engine");
    } finally {
      serializer.close();
      runtime.close();
    }
  }

  @Test
  @DisplayName("Reject null module bytes in serialize")
  public void testSerializeNullBytes() throws WasmException {
    final PanamaWasmRuntime runtime = new PanamaWasmRuntime();
    final PanamaEngine engine = new PanamaEngine();
    final PanamaSerializer serializer = new PanamaSerializer(runtime);

    try {
      assertThrows(
          IllegalArgumentException.class,
          () -> serializer.serialize(engine, null),
          "Should reject null module bytes");
    } finally {
      serializer.close();
      engine.close();
      runtime.close();
    }
  }

  @Test
  @DisplayName("Reject non-Panama engine in serialize")
  public void testSerializeWrongEngineType() throws WasmException {
    final PanamaWasmRuntime runtime = new PanamaWasmRuntime();
    final PanamaSerializer serializer = new PanamaSerializer(runtime);

    try {
      // Create a mock engine that's not a PanamaEngine
      final ai.tegmentum.wasmtime4j.Engine mockEngine =
          new ai.tegmentum.wasmtime4j.Engine() {
            @Override
            public Module compileModule(final byte[] wasmBytes) {
              return null;
            }

            @Override
            public void close() {}

            @Override
            public boolean isValid() {
              return true;
            }
          };

      assertThrows(
          IllegalArgumentException.class,
          () -> serializer.serialize(mockEngine, SIMPLE_MODULE_BYTES),
          "Should reject non-Panama engine");
    } finally {
      serializer.close();
      runtime.close();
    }
  }

  @Test
  @DisplayName("Reject operations on closed serializer")
  public void testClosedSerializer() throws WasmException {
    final PanamaWasmRuntime runtime = new PanamaWasmRuntime();
    final PanamaEngine engine = new PanamaEngine();
    final PanamaSerializer serializer = new PanamaSerializer(runtime);

    serializer.close();

    // All operations should fail on closed serializer
    assertThrows(
        IllegalStateException.class,
        () -> serializer.serialize(engine, SIMPLE_MODULE_BYTES),
        "Should reject serialize on closed serializer");

    assertThrows(
        IllegalStateException.class,
        () -> serializer.deserialize(engine, new byte[10]),
        "Should reject deserialize on closed serializer");

    assertThrows(
        IllegalStateException.class,
        () -> serializer.clearCache(),
        "Should reject clearCache on closed serializer");

    assertThrows(
        IllegalStateException.class,
        () -> serializer.getCacheEntryCount(),
        "Should reject getCacheEntryCount on closed serializer");

    engine.close();
    runtime.close();
  }
}
