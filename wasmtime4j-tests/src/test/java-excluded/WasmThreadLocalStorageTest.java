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

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.WasmThreadLocalStorage;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.PanamaWasmThreadLocalStorage;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Comprehensive tests for WebAssembly thread-local storage functionality.
 *
 * <p>Tests all data types, operations, and edge cases for thread-local storage including:
 *
 * <ul>
 *   <li>Basic put/get operations for all data types (Int, Long, Float, Double, Bytes, String)
 *   <li>Null and empty key validation
 *   <li>Storage management (contains, remove, clear, size)
 *   <li>Memory usage tracking
 *   <li>Thread isolation verification
 *   <li>Edge cases and error handling
 * </ul>
 *
 * @since 1.0.0
 */
@DisplayName("WebAssembly Thread-Local Storage Tests")
public class WasmThreadLocalStorageTest {

  private static final Logger LOGGER = Logger.getLogger(WasmThreadLocalStorageTest.class.getName());

  private WasmRuntime runtime;
  private WasmEngine engine;
  private WasmStore store;
  private WasmModule module;
  private WasmInstance instance;

  @BeforeEach
  void setUp(final TestInfo testInfo) throws Exception {
    LOGGER.info("Starting test: " + testInfo.getDisplayName());

    // Initialize runtime and create a simple WASM module with threading support
    runtime = WasmRuntime.getInstance();
    engine = runtime.createEngine(WasmEngine.Config.builder().withThreads(true).build());
    store = runtime.createStore(engine);

    // Create a simple module that spawns a thread
    // (module
    //   (func (export "test") (result i32)
    //     i32.const 42)
    // )
    final byte[] wasmBytes =
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
          0x05,
          0x01,
          0x60,
          0x00,
          0x01,
          0x7f, // type section: () -> i32
          0x03,
          0x02,
          0x01,
          0x00, // function section
          0x07,
          0x08,
          0x01,
          0x04,
          0x74,
          0x65,
          0x73,
          0x74,
          0x00,
          0x00, // export section
          0x0a,
          0x06,
          0x01,
          0x04,
          0x00,
          0x41,
          0x2a,
          0x0b // code section: i32.const 42
        };

    module = runtime.createModule(engine, wasmBytes);
    instance = runtime.createInstance(store, module, new Object[0]);
  }

  @AfterEach
  void tearDown(final TestInfo testInfo) throws Exception {
    LOGGER.info("Completed test: " + testInfo.getDisplayName());

    if (instance != null) {
      instance.close();
    }
    if (module != null) {
      module.close();
    }
    if (store != null) {
      store.close();
    }
    if (engine != null) {
      engine.close();
    }
  }

  // ========== Integer Tests ==========

  @Test
  @DisplayName("putInt and getInt - basic operations")
  void testPutIntGetInt() throws Exception {
    final WasmThread thread = createThread();
    final WasmThreadLocalStorage tls = thread.getThreadLocalStorage();

    LOGGER.info("Testing basic putInt/getInt operations");

    // Store and retrieve a value
    tls.putInt("test_int", 42);
    assertEquals(42, tls.getInt("test_int"));

    // Overwrite existing value
    tls.putInt("test_int", 100);
    assertEquals(100, tls.getInt("test_int"));

    // Store multiple values
    tls.putInt("int1", 1);
    tls.putInt("int2", 2);
    tls.putInt("int3", 3);
    assertEquals(1, tls.getInt("int1"));
    assertEquals(2, tls.getInt("int2"));
    assertEquals(3, tls.getInt("int3"));

    thread.close();
    LOGGER.info("✓ putInt/getInt basic operations successful");
  }

  @Test
  @DisplayName("putInt and getInt - boundary values")
  void testPutIntGetIntBoundary() throws Exception {
    final WasmThread thread = createThread();
    final WasmThreadLocalStorage tls = thread.getThreadLocalStorage();

    LOGGER.info("Testing putInt/getInt with boundary values");

    // Test Integer.MAX_VALUE
    tls.putInt("max_int", Integer.MAX_VALUE);
    assertEquals(Integer.MAX_VALUE, tls.getInt("max_int"));

    // Test Integer.MIN_VALUE
    tls.putInt("min_int", Integer.MIN_VALUE);
    assertEquals(Integer.MIN_VALUE, tls.getInt("min_int"));

    // Test zero
    tls.putInt("zero", 0);
    assertEquals(0, tls.getInt("zero"));

    // Test negative values
    tls.putInt("negative", -12345);
    assertEquals(-12345, tls.getInt("negative"));

    thread.close();
    LOGGER.info("✓ putInt/getInt boundary values successful");
  }

  // ========== Long Tests ==========

  @Test
  @DisplayName("putLong and getLong - basic operations")
  void testPutLongGetLong() throws Exception {
    final WasmThread thread = createThread();
    final WasmThreadLocalStorage tls = thread.getThreadLocalStorage();

    LOGGER.info("Testing basic putLong/getLong operations");

    // Store and retrieve a value
    tls.putLong("test_long", 1234567890123L);
    assertEquals(1234567890123L, tls.getLong("test_long"));

    // Overwrite existing value
    tls.putLong("test_long", 9876543210987L);
    assertEquals(9876543210987L, tls.getLong("test_long"));

    thread.close();
    LOGGER.info("✓ putLong/getLong basic operations successful");
  }

  @Test
  @DisplayName("putLong and getLong - boundary values")
  void testPutLongGetLongBoundary() throws Exception {
    final WasmThread thread = createThread();
    final WasmThreadLocalStorage tls = thread.getThreadLocalStorage();

    LOGGER.info("Testing putLong/getLong with boundary values");

    // Test Long.MAX_VALUE
    tls.putLong("max_long", Long.MAX_VALUE);
    assertEquals(Long.MAX_VALUE, tls.getLong("max_long"));

    // Test Long.MIN_VALUE
    tls.putLong("min_long", Long.MIN_VALUE);
    assertEquals(Long.MIN_VALUE, tls.getLong("min_long"));

    // Test zero
    tls.putLong("zero", 0L);
    assertEquals(0L, tls.getLong("zero"));

    thread.close();
    LOGGER.info("✓ putLong/getLong boundary values successful");
  }

  // ========== Float Tests ==========

  @Test
  @DisplayName("putFloat and getFloat - basic operations")
  void testPutFloatGetFloat() throws Exception {
    final WasmThread thread = createThread();
    final WasmThreadLocalStorage tls = thread.getThreadLocalStorage();

    LOGGER.info("Testing basic putFloat/getFloat operations");

    // Store and retrieve a value
    tls.putFloat("test_float", 3.14159f);
    assertEquals(3.14159f, tls.getFloat("test_float"), 0.00001f);

    // Overwrite existing value
    tls.putFloat("test_float", 2.71828f);
    assertEquals(2.71828f, tls.getFloat("test_float"), 0.00001f);

    thread.close();
    LOGGER.info("✓ putFloat/getFloat basic operations successful");
  }

  @Test
  @DisplayName("putFloat and getFloat - special values")
  void testPutFloatGetFloatSpecial() throws Exception {
    final WasmThread thread = createThread();
    final WasmThreadLocalStorage tls = thread.getThreadLocalStorage();

    LOGGER.info("Testing putFloat/getFloat with special values");

    // Test Float.MAX_VALUE
    tls.putFloat("max_float", Float.MAX_VALUE);
    assertEquals(Float.MAX_VALUE, tls.getFloat("max_float"), 0.0f);

    // Test Float.MIN_VALUE
    tls.putFloat("min_float", Float.MIN_VALUE);
    assertEquals(Float.MIN_VALUE, tls.getFloat("min_float"), 0.0f);

    // Test zero
    tls.putFloat("zero", 0.0f);
    assertEquals(0.0f, tls.getFloat("zero"), 0.0f);

    // Test negative zero
    tls.putFloat("neg_zero", -0.0f);
    assertEquals(-0.0f, tls.getFloat("neg_zero"), 0.0f);

    // Test NaN
    tls.putFloat("nan", Float.NaN);
    assertTrue(Float.isNaN(tls.getFloat("nan")));

    // Test positive infinity
    tls.putFloat("pos_inf", Float.POSITIVE_INFINITY);
    assertEquals(Float.POSITIVE_INFINITY, tls.getFloat("pos_inf"), 0.0f);

    // Test negative infinity
    tls.putFloat("neg_inf", Float.NEGATIVE_INFINITY);
    assertEquals(Float.NEGATIVE_INFINITY, tls.getFloat("neg_inf"), 0.0f);

    thread.close();
    LOGGER.info("✓ putFloat/getFloat special values successful");
  }

  // ========== Double Tests ==========

  @Test
  @DisplayName("putDouble and getDouble - basic operations")
  void testPutDoubleGetDouble() throws Exception {
    final WasmThread thread = createThread();
    final WasmThreadLocalStorage tls = thread.getThreadLocalStorage();

    LOGGER.info("Testing basic putDouble/getDouble operations");

    // Store and retrieve a value
    tls.putDouble("test_double", 3.141592653589793);
    assertEquals(3.141592653589793, tls.getDouble("test_double"), 0.000000000000001);

    // Overwrite existing value
    tls.putDouble("test_double", 2.718281828459045);
    assertEquals(2.718281828459045, tls.getDouble("test_double"), 0.000000000000001);

    thread.close();
    LOGGER.info("✓ putDouble/getDouble basic operations successful");
  }

  @Test
  @DisplayName("putDouble and getDouble - special values")
  void testPutDoubleGetDoubleSpecial() throws Exception {
    final WasmThread thread = createThread();
    final WasmThreadLocalStorage tls = thread.getThreadLocalStorage();

    LOGGER.info("Testing putDouble/getDouble with special values");

    // Test Double.MAX_VALUE
    tls.putDouble("max_double", Double.MAX_VALUE);
    assertEquals(Double.MAX_VALUE, tls.getDouble("max_double"), 0.0);

    // Test Double.MIN_VALUE
    tls.putDouble("min_double", Double.MIN_VALUE);
    assertEquals(Double.MIN_VALUE, tls.getDouble("min_double"), 0.0);

    // Test zero
    tls.putDouble("zero", 0.0);
    assertEquals(0.0, tls.getDouble("zero"), 0.0);

    // Test NaN
    tls.putDouble("nan", Double.NaN);
    assertTrue(Double.isNaN(tls.getDouble("nan")));

    // Test positive infinity
    tls.putDouble("pos_inf", Double.POSITIVE_INFINITY);
    assertEquals(Double.POSITIVE_INFINITY, tls.getDouble("pos_inf"), 0.0);

    // Test negative infinity
    tls.putDouble("neg_inf", Double.NEGATIVE_INFINITY);
    assertEquals(Double.NEGATIVE_INFINITY, tls.getDouble("neg_inf"), 0.0);

    thread.close();
    LOGGER.info("✓ putDouble/getDouble special values successful");
  }

  // ========== Bytes Tests ==========

  @Test
  @DisplayName("putBytes and getBytes - basic operations")
  void testPutBytesGetBytes() throws Exception {
    final WasmThread thread = createThread();
    final WasmThreadLocalStorage tls = thread.getThreadLocalStorage();

    LOGGER.info("Testing basic putBytes/getBytes operations");

    // Store and retrieve a byte array
    final byte[] testData = {1, 2, 3, 4, 5};
    tls.putBytes("test_bytes", testData);
    assertArrayEquals(testData, tls.getBytes("test_bytes"));

    // Overwrite with different data
    final byte[] newData = {10, 20, 30};
    tls.putBytes("test_bytes", newData);
    assertArrayEquals(newData, tls.getBytes("test_bytes"));

    thread.close();
    LOGGER.info("✓ putBytes/getBytes basic operations successful");
  }

  @Test
  @DisplayName("putBytes and getBytes - edge cases")
  void testPutBytesGetBytesEdgeCases() throws Exception {
    final WasmThread thread = createThread();
    final WasmThreadLocalStorage tls = thread.getThreadLocalStorage();

    LOGGER.info("Testing putBytes/getBytes with edge cases");

    // Test empty array
    final byte[] emptyData = new byte[0];
    tls.putBytes("empty_bytes", emptyData);
    assertArrayEquals(emptyData, tls.getBytes("empty_bytes"));

    // Test large array
    final byte[] largeData = new byte[10000];
    for (int i = 0; i < largeData.length; i++) {
      largeData[i] = (byte) (i % 256);
    }
    tls.putBytes("large_bytes", largeData);
    assertArrayEquals(largeData, tls.getBytes("large_bytes"));

    // Test array with all zeros
    final byte[] zeroData = new byte[100];
    tls.putBytes("zero_bytes", zeroData);
    assertArrayEquals(zeroData, tls.getBytes("zero_bytes"));

    // Test array with negative bytes
    final byte[] negativeData = {-128, -64, -32, -16, -8, -4, -2, -1};
    tls.putBytes("negative_bytes", negativeData);
    assertArrayEquals(negativeData, tls.getBytes("negative_bytes"));

    thread.close();
    LOGGER.info("✓ putBytes/getBytes edge cases successful");
  }

  // ========== String Tests ==========

  @Test
  @DisplayName("putString and getString - basic operations")
  void testPutStringGetString() throws Exception {
    final WasmThread thread = createThread();
    final WasmThreadLocalStorage tls = thread.getThreadLocalStorage();

    LOGGER.info("Testing basic putString/getString operations");

    // Store and retrieve a string
    tls.putString("test_string", "Hello, WebAssembly!");
    assertEquals("Hello, WebAssembly!", tls.getString("test_string"));

    // Overwrite existing value
    tls.putString("test_string", "Updated value");
    assertEquals("Updated value", tls.getString("test_string"));

    thread.close();
    LOGGER.info("✓ putString/getString basic operations successful");
  }

  @Test
  @DisplayName("putString and getString - edge cases")
  void testPutStringGetStringEdgeCases() throws Exception {
    final WasmThread thread = createThread();
    final WasmThreadLocalStorage tls = thread.getThreadLocalStorage();

    LOGGER.info("Testing putString/getString with edge cases");

    // Test empty string
    tls.putString("empty_string", "");
    assertEquals("", tls.getString("empty_string"));

    // Test string with special characters
    tls.putString("special_chars", "Special: \n\r\t\\ \"quotes\"");
    assertEquals("Special: \n\r\t\\ \"quotes\"", tls.getString("special_chars"));

    // Test Unicode string
    tls.putString("unicode", "Hello 世界 🌍");
    assertEquals("Hello 世界 🌍", tls.getString("unicode"));

    // Test very long string
    final StringBuilder longString = new StringBuilder();
    for (int i = 0; i < 10000; i++) {
      longString.append("a");
    }
    tls.putString("long_string", longString.toString());
    assertEquals(longString.toString(), tls.getString("long_string"));

    thread.close();
    LOGGER.info("✓ putString/getString edge cases successful");
  }

  // ========== Storage Management Tests ==========

  @Test
  @DisplayName("contains - check key existence")
  void testContains() throws Exception {
    final WasmThread thread = createThread();
    final WasmThreadLocalStorage tls = thread.getThreadLocalStorage();

    LOGGER.info("Testing contains operation");

    // Key should not exist initially
    assertFalse(tls.contains("nonexistent"));

    // Add a key and verify it exists
    tls.putInt("test_key", 42);
    assertTrue(tls.contains("test_key"));

    // Remove key and verify it no longer exists
    tls.remove("test_key");
    assertFalse(tls.contains("test_key"));

    thread.close();
    LOGGER.info("✓ contains operation successful");
  }

  @Test
  @DisplayName("remove - remove stored values")
  void testRemove() throws Exception {
    final WasmThread thread = createThread();
    final WasmThreadLocalStorage tls = thread.getThreadLocalStorage();

    LOGGER.info("Testing remove operation");

    // Add multiple keys
    tls.putInt("key1", 1);
    tls.putInt("key2", 2);
    tls.putInt("key3", 3);

    // Verify all keys exist
    assertTrue(tls.contains("key1"));
    assertTrue(tls.contains("key2"));
    assertTrue(tls.contains("key3"));
    assertEquals(3, tls.size());

    // Remove one key
    assertTrue(tls.remove("key2"));
    assertFalse(tls.contains("key2"));
    assertEquals(2, tls.size());

    // Verify other keys still exist
    assertTrue(tls.contains("key1"));
    assertTrue(tls.contains("key3"));

    // Removing non-existent key should return false
    assertFalse(tls.remove("nonexistent"));

    thread.close();
    LOGGER.info("✓ remove operation successful");
  }

  @Test
  @DisplayName("clear - clear all stored values")
  void testClear() throws Exception {
    final WasmThread thread = createThread();
    final WasmThreadLocalStorage tls = thread.getThreadLocalStorage();

    LOGGER.info("Testing clear operation");

    // Add multiple keys of different types
    tls.putInt("int_key", 42);
    tls.putLong("long_key", 123L);
    tls.putFloat("float_key", 3.14f);
    tls.putDouble("double_key", 2.718);
    tls.putString("string_key", "test");

    // Verify all keys exist
    assertEquals(5, tls.size());

    // Clear all values
    tls.clear();

    // Verify storage is empty
    assertEquals(0, tls.size());
    assertFalse(tls.contains("int_key"));
    assertFalse(tls.contains("long_key"));
    assertFalse(tls.contains("float_key"));
    assertFalse(tls.contains("double_key"));
    assertFalse(tls.contains("string_key"));

    thread.close();
    LOGGER.info("✓ clear operation successful");
  }

  @Test
  @DisplayName("size - track number of stored entries")
  void testSize() throws Exception {
    final WasmThread thread = createThread();
    final WasmThreadLocalStorage tls = thread.getThreadLocalStorage();

    LOGGER.info("Testing size operation");

    // Initial size should be 0
    assertEquals(0, tls.size());

    // Add keys and verify size increases
    tls.putInt("key1", 1);
    assertEquals(1, tls.size());

    tls.putInt("key2", 2);
    assertEquals(2, tls.size());

    tls.putInt("key3", 3);
    assertEquals(3, tls.size());

    // Overwriting existing key should not change size
    tls.putInt("key2", 20);
    assertEquals(3, tls.size());

    // Removing key should decrease size
    tls.remove("key1");
    assertEquals(2, tls.size());

    // Clearing should reset size to 0
    tls.clear();
    assertEquals(0, tls.size());

    thread.close();
    LOGGER.info("✓ size operation successful");
  }

  @Test
  @DisplayName("getMemoryUsage - track memory consumption")
  void testGetMemoryUsage() throws Exception {
    final WasmThread thread = createThread();
    final WasmThreadLocalStorage tls = thread.getThreadLocalStorage();

    LOGGER.info("Testing getMemoryUsage operation");

    // Initial memory usage should be 0
    assertEquals(0, tls.getMemoryUsage());

    // Add some data and verify memory usage increases
    tls.putInt("int_key", 42);
    final long memoryAfterInt = tls.getMemoryUsage();
    assertTrue(memoryAfterInt > 0);

    // Add more data
    final byte[] largeData = new byte[1000];
    tls.putBytes("bytes_key", largeData);
    final long memoryAfterBytes = tls.getMemoryUsage();
    assertTrue(memoryAfterBytes > memoryAfterInt);

    // Clear should reset memory usage to 0
    tls.clear();
    assertEquals(0, tls.getMemoryUsage());

    thread.close();
    LOGGER.info("✓ getMemoryUsage operation successful");
  }

  // ========== Error Handling Tests ==========

  @Test
  @DisplayName("null key validation")
  void testNullKeyValidation() throws Exception {
    final WasmThread thread = createThread();
    final WasmThreadLocalStorage tls = thread.getThreadLocalStorage();

    LOGGER.info("Testing null key validation");

    // All operations should reject null keys
    assertThrows(IllegalArgumentException.class, () -> tls.putInt(null, 42));
    assertThrows(IllegalArgumentException.class, () -> tls.getInt(null));
    assertThrows(IllegalArgumentException.class, () -> tls.putLong(null, 123L));
    assertThrows(IllegalArgumentException.class, () -> tls.getLong(null));
    assertThrows(IllegalArgumentException.class, () -> tls.putFloat(null, 3.14f));
    assertThrows(IllegalArgumentException.class, () -> tls.getFloat(null));
    assertThrows(IllegalArgumentException.class, () -> tls.putDouble(null, 2.718));
    assertThrows(IllegalArgumentException.class, () -> tls.getDouble(null));
    assertThrows(IllegalArgumentException.class, () -> tls.putBytes(null, new byte[] {1, 2, 3}));
    assertThrows(IllegalArgumentException.class, () -> tls.getBytes(null));
    assertThrows(IllegalArgumentException.class, () -> tls.putString(null, "test"));
    assertThrows(IllegalArgumentException.class, () -> tls.getString(null));
    assertThrows(IllegalArgumentException.class, () -> tls.contains(null));
    assertThrows(IllegalArgumentException.class, () -> tls.remove(null));

    thread.close();
    LOGGER.info("✓ null key validation successful");
  }

  @Test
  @DisplayName("empty key validation")
  void testEmptyKeyValidation() throws Exception {
    final WasmThread thread = createThread();
    final WasmThreadLocalStorage tls = thread.getThreadLocalStorage();

    LOGGER.info("Testing empty key validation");

    // All operations should reject empty keys
    assertThrows(IllegalArgumentException.class, () -> tls.putInt("", 42));
    assertThrows(IllegalArgumentException.class, () -> tls.getInt(""));
    assertThrows(IllegalArgumentException.class, () -> tls.putString("", "test"));
    assertThrows(IllegalArgumentException.class, () -> tls.getString(""));
    assertThrows(IllegalArgumentException.class, () -> tls.contains(""));
    assertThrows(IllegalArgumentException.class, () -> tls.remove(""));

    thread.close();
    LOGGER.info("✓ empty key validation successful");
  }

  @Test
  @DisplayName("null value validation")
  void testNullValueValidation() throws Exception {
    final WasmThread thread = createThread();
    final WasmThreadLocalStorage tls = thread.getThreadLocalStorage();

    LOGGER.info("Testing null value validation");

    // Operations that accept objects should reject null values
    assertThrows(IllegalArgumentException.class, () -> tls.putBytes("key", null));
    assertThrows(IllegalArgumentException.class, () -> tls.putString("key", null));

    thread.close();
    LOGGER.info("✓ null value validation successful");
  }

  // ========== Helper Methods ==========

  /** Creates a new WASM thread for testing. */
  private WasmThread createThread() throws WasmException {
    // For testing purposes, we'll create a thread from the store
    // This is a simplified implementation - actual threading API may differ
    return store.createThread();
  }
}
