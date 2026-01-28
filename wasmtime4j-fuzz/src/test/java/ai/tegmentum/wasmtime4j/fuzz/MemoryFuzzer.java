package ai.tegmentum.wasmtime4j.fuzz;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * Fuzz tests for WebAssembly memory operations.
 *
 * <p>This fuzzer tests the robustness of memory read/write operations by:
 *
 * <ul>
 *   <li>Reading/writing at various offsets
 *   <li>Testing boundary conditions
 *   <li>Testing memory growth
 *   <li>Testing buffer access patterns
 * </ul>
 *
 * @since 1.0.0
 */
public class MemoryFuzzer {

  /** A test module that exports memory. */
  private static final String MEMORY_MODULE_WAT =
      """
        (module
            (memory (export "memory") 1 10)
            (func (export "load_i32") (param i32) (result i32)
                local.get 0
                i32.load)
            (func (export "store_i32") (param i32 i32)
                local.get 0
                local.get 1
                i32.store)
        )
        """;

  /** Page size in bytes (64KB). */
  private static final int PAGE_SIZE = 65536;

  /**
   * Fuzz test for memory byte read/write operations.
   *
   * <p>Tests reading and writing individual bytes at fuzzed offsets.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzMemoryByteAccess(final FuzzedDataProvider data) {
    final int offset = data.consumeInt();
    final byte value = data.consumeByte();

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(MEMORY_MODULE_WAT);
        Instance instance = module.instantiate(store)) {

      final Optional<WasmMemory> memoryOpt = instance.getMemory("memory");
      if (memoryOpt.isEmpty()) {
        return;
      }

      final WasmMemory memory = memoryOpt.get();

      // Try to write a byte at the fuzzed offset
      // This should throw IndexOutOfBoundsException for invalid offsets
      try {
        memory.writeByte(offset, value);

        // If write succeeded, verify read
        final byte readValue = memory.readByte(offset);
        if (readValue != value) {
          throw new AssertionError("Read value does not match written value");
        }
      } catch (IndexOutOfBoundsException e) {
        // Expected for out-of-bounds offsets
      }

    } catch (WasmException e) {
      // Expected
    } catch (Exception e) {
      throw e;
    }
  }

  /**
   * Fuzz test for memory bulk read/write operations.
   *
   * <p>Tests reading and writing byte arrays at fuzzed offsets and lengths.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzMemoryBulkAccess(final FuzzedDataProvider data) {
    final int offset = Math.abs(data.consumeInt()) % (PAGE_SIZE * 2);
    final int length = data.consumeInt(0, 4096);
    final byte[] writeData = data.consumeBytes(length);

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(MEMORY_MODULE_WAT);
        Instance instance = module.instantiate(store)) {

      final Optional<WasmMemory> memoryOpt = instance.getMemory("memory");
      if (memoryOpt.isEmpty()) {
        return;
      }

      final WasmMemory memory = memoryOpt.get();

      // Try bulk write
      try {
        memory.writeBytes(offset, writeData, 0, writeData.length);

        // If write succeeded, verify with read
        final byte[] readData = new byte[writeData.length];
        memory.readBytes(offset, readData, 0, readData.length);

        // Verify data matches
        for (int i = 0; i < writeData.length; i++) {
          if (readData[i] != writeData[i]) {
            throw new AssertionError("Data mismatch at offset " + i);
          }
        }
      } catch (IndexOutOfBoundsException e) {
        // Expected for out-of-bounds access
      }

    } catch (WasmException e) {
      // Expected
    } catch (Exception e) {
      throw e;
    }
  }

  /**
   * Fuzz test for memory growth operations.
   *
   * <p>Tests growing memory by fuzzed page counts.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzMemoryGrowth(final FuzzedDataProvider data) {
    final int growPages = data.consumeInt(0, 100);

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(MEMORY_MODULE_WAT);
        Instance instance = module.instantiate(store)) {

      final Optional<WasmMemory> memoryOpt = instance.getMemory("memory");
      if (memoryOpt.isEmpty()) {
        return;
      }

      final WasmMemory memory = memoryOpt.get();
      final int initialSize = memory.getSize();

      // Try to grow memory
      final int prevSize = memory.grow(growPages);

      if (prevSize >= 0) {
        // Growth succeeded - verify new size
        final int newSize = memory.getSize();
        if (newSize != initialSize + growPages) {
          throw new AssertionError("Memory size mismatch after growth");
        }

        // Try to access newly allocated memory (only if pages were actually added)
        if (growPages > 0) {
          final int newOffset = initialSize * PAGE_SIZE;
          try {
            memory.writeByte(newOffset, (byte) 0x42);
            final byte readBack = memory.readByte(newOffset);
            if (readBack != 0x42) {
              throw new AssertionError("Failed to read from grown memory");
            }
          } catch (IndexOutOfBoundsException e) {
            // Unexpected - growth succeeded but access failed
            throw new AssertionError("Cannot access grown memory", e);
          }
        }
      }
      // If prevSize is -1, growth failed which is expected for large requests

    } catch (WasmException e) {
      // Expected
    } catch (Exception e) {
      throw e;
    }
  }

  /**
   * Fuzz test for ByteBuffer access to memory.
   *
   * <p>Tests accessing memory through the ByteBuffer interface.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzMemoryBufferAccess(final FuzzedDataProvider data) {
    final int position = Math.abs(data.consumeInt()) % PAGE_SIZE;
    final byte[] writeData = data.consumeBytes(100);

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(MEMORY_MODULE_WAT);
        Instance instance = module.instantiate(store)) {

      final Optional<WasmMemory> memoryOpt = instance.getMemory("memory");
      if (memoryOpt.isEmpty()) {
        return;
      }

      final WasmMemory memory = memoryOpt.get();
      final ByteBuffer buffer = memory.getBuffer();

      if (buffer == null) {
        return;
      }

      try {
        // Try to write through ByteBuffer
        buffer.position(position);
        if (position + writeData.length <= buffer.capacity()) {
          buffer.put(writeData);

          // Read back and verify
          buffer.position(position);
          final byte[] readData = new byte[writeData.length];
          buffer.get(readData);

          for (int i = 0; i < writeData.length; i++) {
            if (readData[i] != writeData[i]) {
              throw new AssertionError("ByteBuffer data mismatch at " + i);
            }
          }
        }
      } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
        // Expected for out-of-bounds positions
      }

    } catch (WasmException e) {
      // Expected
    } catch (Exception e) {
      throw e;
    }
  }

  /**
   * Fuzz test for memory access via WASM load/store instructions.
   *
   * <p>Tests memory access through exported functions that use i32.load and i32.store instructions.
   *
   * @param data fuzzed data provider
   */
  @FuzzTest
  public void fuzzMemoryViaWasm(final FuzzedDataProvider data) {
    final int offset = data.consumeInt();
    final int value = data.consumeInt();

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(MEMORY_MODULE_WAT);
        Instance instance = module.instantiate(store)) {

      // Use the store_i32 and load_i32 functions
      try {
        // Store a value
        instance.callFunction(
            "store_i32",
            ai.tegmentum.wasmtime4j.WasmValue.i32(offset),
            ai.tegmentum.wasmtime4j.WasmValue.i32(value));

        // Load it back
        final ai.tegmentum.wasmtime4j.WasmValue[] results =
            instance.callFunction("load_i32", ai.tegmentum.wasmtime4j.WasmValue.i32(offset));

        // Verify
        if (results.length > 0) {
          final int loadedValue = results[0].asInt();
          if (loadedValue != value) {
            throw new AssertionError("WASM load/store mismatch");
          }
        }
      } catch (WasmException e) {
        // Expected for out-of-bounds access (trap)
      }

    } catch (WasmException e) {
      // Expected
    } catch (Exception e) {
      throw e;
    }
  }
}
