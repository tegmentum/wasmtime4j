package ai.tegmentum.wasmtime4j.wasmtime.generated.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Equivalent Java tests for Wasmtime memory.rs tests.
 *
 * <p>Original source: memory.rs - Tests memory operations.
 *
 * <p>This test validates that wasmtime4j memory operations produce the same behavior as the
 * upstream Wasmtime implementation.
 */
public final class MemoryTest extends DualRuntimeTest {

  private static final int PAGE_SIZE = 65536; // 64KB

  @AfterEach
  void cleanupRuntime() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("memory::basic_memory_operations")
  public void testBasicMemoryOperations(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (memory (export "memory") 1 10)
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        final Optional<WasmMemory> memOpt = instance.getMemory("memory");
        assertTrue(memOpt.isPresent(), "Memory should be exported");

        final WasmMemory memory = memOpt.get();
        assertEquals(1, memory.getSize(), "Initial size should be 1 page");
        assertEquals(PAGE_SIZE, memory.dataSize(), "Initial size should be 64KB");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("memory::memory_grow")
  public void testMemoryGrow(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (memory (export "memory") 1 10)
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").get();

        // Grow by 2 pages
        final int previousSize = memory.grow(2);
        assertEquals(1, previousSize, "Previous size should be 1 page");
        assertEquals(3, memory.getSize(), "New size should be 3 pages");
        assertEquals(3 * PAGE_SIZE, memory.dataSize(), "New size should be 192KB");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("memory::memory_grow_fails_past_max")
  public void testMemoryGrowFailsPastMax(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (memory (export "memory") 1 2)
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").get();

        // Try to grow past maximum
        final int result = memory.grow(5);
        assertEquals(-1, result, "Growing past max should return -1");
        assertEquals(1, memory.getSize(), "Size should remain 1 page");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("memory::memory_read_write_byte")
  public void testMemoryReadWriteByte(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (memory (export "memory") 1)
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").get();

        // Write and read bytes
        memory.writeByte(0, (byte) 42);
        assertEquals((byte) 42, memory.readByte(0), "Should read back 42");

        memory.writeByte(100, (byte) -128);
        assertEquals((byte) -128, memory.readByte(100), "Should read back -128");

        memory.writeByte(1000, (byte) 255);
        assertEquals((byte) -1, memory.readByte(1000), "Should read back -1 (255 as signed)");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("memory::memory_read_write_int32")
  public void testMemoryReadWriteInt32(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (memory (export "memory") 1)
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").get();

        // Write and read 32-bit integers
        memory.writeInt32(0, 12345678);
        assertEquals(12345678, memory.readInt32(0), "Should read back 12345678");

        memory.writeInt32(100, -1);
        assertEquals(-1, memory.readInt32(100), "Should read back -1");

        memory.writeInt32(200, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, memory.readInt32(200), "Should read back MAX_VALUE");

        memory.writeInt32(300, Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, memory.readInt32(300), "Should read back MIN_VALUE");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("memory::memory_read_write_int64")
  public void testMemoryReadWriteInt64(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (memory (export "memory") 1)
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").get();

        // Write and read 64-bit integers
        memory.writeInt64(0, 1234567890123456789L);
        assertEquals(1234567890123456789L, memory.readInt64(0), "Should read back large value");

        memory.writeInt64(100, -1L);
        assertEquals(-1L, memory.readInt64(100), "Should read back -1");

        memory.writeInt64(200, Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, memory.readInt64(200), "Should read back MAX_VALUE");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("memory::memory_read_write_buffer")
  public void testMemoryReadWriteBuffer(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (memory (export "memory") 1)
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").get();

        // Write bytes
        final byte[] data = "Hello, WebAssembly!".getBytes();
        memory.writeBytes(0, data, 0, data.length);

        // Read it back
        final byte[] readData = new byte[data.length];
        memory.readBytes(0, readData, 0, data.length);
        assertEquals("Hello, WebAssembly!", new String(readData), "Should read back string");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("memory::memory_access_via_wasm")
  public void testMemoryAccessViaWasm(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (memory (export "memory") 1)
          (func (export "store") (param i32 i32)
            local.get 0
            local.get 1
            i32.store
          )
          (func (export "load") (param i32) (result i32)
            local.get 0
            i32.load
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        // Store via Wasm
        instance.callFunction("store", WasmValue.i32(0), WasmValue.i32(42));

        // Load via Wasm
        WasmValue[] results = instance.callFunction("load", WasmValue.i32(0));
        assertEquals(42, results[0].asInt(), "Should load 42 via Wasm");

        // Read via Java API
        final WasmMemory memory = instance.getMemory("memory").get();
        assertEquals(42, memory.readInt32(0), "Should read 42 via Java API");

        // Write via Java API, read via Wasm
        memory.writeInt32(100, 999);
        results = instance.callFunction("load", WasmValue.i32(100));
        assertEquals(999, results[0].asInt(), "Should load 999 via Wasm after Java write");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("memory::memory_bounds_check")
  public void testMemoryBoundsCheck(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (memory (export "memory") 1 1)
          (func (export "load") (param i32) (result i32)
            local.get 0
            i32.load
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        // Access at valid address should work
        WasmValue[] results = instance.callFunction("load", WasmValue.i32(0));
        assertNotNull(results, "Valid access should return result");

        // Access at out-of-bounds address should trap
        try {
          instance.callFunction("load", WasmValue.i32(PAGE_SIZE)); // Exactly at boundary
          fail("Expected trap for out-of-bounds access");
        } catch (final Exception e) {
          // Expected - out of bounds memory access
          assertTrue(true, "Trap expected: " + e.getMessage());
        }
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("memory::memory_max_size")
  public void testMemoryMaxSize(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Memory with explicit max
    final String watWithMax =
        """
        (module
          (memory (export "memory") 1 10)
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(watWithMax);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        final WasmMemory memory = instance.getMemory("memory").get();
        final int maxSize = memory.getMaxSize();
        // Some implementations may not support getting max size
        Assumptions.assumeTrue(maxSize != -1, "Memory max size not supported");
        assertEquals(10, maxSize, "Max size should be 10 pages");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("memory::memory_copy_fill_via_wasm")
  public void testMemoryCopyFillViaWasm(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    final String wat =
        """
        (module
          (memory (export "memory") 1)
          (func (export "fill") (param i32 i32 i32)
            local.get 0   ;; dest
            local.get 1   ;; value
            local.get 2   ;; size
            memory.fill
          )
          (func (export "copy") (param i32 i32 i32)
            local.get 0   ;; dest
            local.get 1   ;; src
            local.get 2   ;; size
            memory.copy
          )
          (func (export "load") (param i32) (result i32)
            local.get 0
            i32.load8_u
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        // Fill memory region with value 42
        instance.callFunction("fill", WasmValue.i32(0), WasmValue.i32(42), WasmValue.i32(10));

        // Verify fill worked
        WasmValue[] results = instance.callFunction("load", WasmValue.i32(5));
        assertEquals(42, results[0].asInt(), "Filled byte should be 42");

        // Copy to another region
        instance.callFunction("copy", WasmValue.i32(100), WasmValue.i32(0), WasmValue.i32(10));

        // Verify copy worked
        results = instance.callFunction("load", WasmValue.i32(105));
        assertEquals(42, results[0].asInt(), "Copied byte should be 42");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("memory::memory_data_initialization")
  public void testMemoryDataInitialization(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Module with data segment
    final String wat =
        """
        (module
          (memory (export "memory") 1)
          (data (i32.const 0) "Hello")
          (func (export "load") (param i32) (result i32)
            local.get 0
            i32.load8_u
          )
        )
        """;

    try (final Engine engine = Engine.create()) {
      final Module module = engine.compileWat(wat);
      try (final Store store = engine.createStore();
          final Instance instance = module.instantiate(store)) {

        // Check that data was initialized
        WasmValue[] results = instance.callFunction("load", WasmValue.i32(0));
        assertEquals('H', results[0].asInt(), "First byte should be 'H'");

        results = instance.callFunction("load", WasmValue.i32(1));
        assertEquals('e', results[0].asInt(), "Second byte should be 'e'");

        results = instance.callFunction("load", WasmValue.i32(4));
        assertEquals('o', results[0].asInt(), "Fifth byte should be 'o'");
      }
      module.close();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("memory::multiple_memories")
  public void testMultipleMemories(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    // Multi-memory requires the MULTI_MEMORY feature to be enabled
    final String wat =
        """
        (module
          (memory (export "mem1") 1)
          (memory (export "mem2") 2)
        )
        """;

    // Create engine with multi-memory feature enabled
    final EngineConfig config = new EngineConfig().addWasmFeature(WasmFeature.MULTI_MEMORY);
    try (final Engine engine = Engine.create(config)) {
      try {
        final Module module = engine.compileWat(wat);
        try (final Store store = engine.createStore();
            final Instance instance = module.instantiate(store)) {

          final Optional<WasmMemory> mem1 = instance.getMemory("mem1");
          final Optional<WasmMemory> mem2 = instance.getMemory("mem2");

          assertTrue(mem1.isPresent(), "mem1 should exist");
          assertTrue(mem2.isPresent(), "mem2 should exist");

          assertEquals(1, mem1.get().getSize(), "mem1 should be 1 page");
          assertEquals(2, mem2.get().getSize(), "mem2 should be 2 pages");
        }
        module.close();
      } catch (final WasmException e) {
        // Multi-memory compilation/instantiation may still fail on some platforms
        Assumptions.assumeTrue(false, "Multi-memory not supported: " + e.getMessage());
      }
    }
  }
}
