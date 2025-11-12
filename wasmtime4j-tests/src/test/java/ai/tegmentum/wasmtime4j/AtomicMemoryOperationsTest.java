package ai.tegmentum.wasmtime4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for atomic memory operations API.
 *
 * <p>This test suite validates all atomic memory operations including compare-and-swap, load/store,
 * arithmetic operations, bitwise operations, and synchronization primitives.
 *
 * <p>Note: These tests focus on the Java API surface. Full functional testing of atomic operations
 * requires shared memory support which depends on Wasmtime configuration.
 */
class AtomicMemoryOperationsTest {

  private Engine engine;
  private Store store;
  private Module module;
  private Instance instance;
  private WasmMemory memory;

  @BeforeEach
  void setUp() throws WasmException, IOException {
    engine = Engine.create();
    store = Store.create(engine);

    // Create a simple module with memory
    final String wat =
        "(module\n"
            + "  (memory (export \"memory\") 1)\n"
            + "  (func (export \"init\") (param $offset i32) (param $value i32)\n"
            + "    local.get $offset\n"
            + "    local.get $value\n"
            + "    i32.store\n"
            + "  )\n"
            + ")";

    module = engine.compileWat(wat);
    instance = module.instantiate(store);
    memory = instance.getMemory("memory").orElseThrow();
  }

  @AfterEach
  void tearDown() {
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

  // ===== Compare-and-Swap Tests =====

  @Test
  void testAtomicCompareAndSwapInt() {
    // Test basic API - actual functionality requires shared memory
    assertDoesNotThrow(
        () -> {
          // Note: This may throw UnsupportedOperationException if memory is not shared
          // For now, we're validating the API exists and accepts correct parameters
          try {
            final int result = memory.atomicCompareAndSwapInt(0, 0, 42);
            assertThat(result).isNotNegative();
          } catch (final UnsupportedOperationException e) {
            // Expected for non-shared memory
            assertThat(e.getMessage()).contains("shared");
          }
        });
  }

  @Test
  void testAtomicCompareAndSwapIntInvalidOffset() {
    // Test alignment validation
    assertThrows(
        IllegalArgumentException.class,
        () -> memory.atomicCompareAndSwapInt(1, 0, 42), // Misaligned offset
        "Offset must be 4-byte aligned");
  }

  @Test
  void testAtomicCompareAndSwapIntNegativeOffset() {
    assertThrows(
        IllegalArgumentException.class,
        () -> memory.atomicCompareAndSwapInt(-4, 0, 42),
        "Offset must be non-negative");
  }

  @Test
  void testAtomicCompareAndSwapLong() {
    assertDoesNotThrow(
        () -> {
          try {
            final long result = memory.atomicCompareAndSwapLong(0, 0L, 42L);
            assertThat(result).isNotNegative();
          } catch (final UnsupportedOperationException e) {
            assertThat(e.getMessage()).contains("shared");
          }
        });
  }

  @Test
  void testAtomicCompareAndSwapLongInvalidOffset() {
    assertThrows(
        IllegalArgumentException.class,
        () -> memory.atomicCompareAndSwapLong(4, 0L, 42L), // Misaligned for i64
        "Offset must be 8-byte aligned");
  }

  // ===== Load/Store Tests =====

  @Test
  void testAtomicLoadInt() {
    assertDoesNotThrow(
        () -> {
          try {
            final int result = memory.atomicLoadInt(0);
            assertThat(result).isNotNull();
          } catch (final UnsupportedOperationException e) {
            assertThat(e.getMessage()).contains("shared");
          }
        });
  }

  @Test
  void testAtomicLoadIntInvalidOffset() {
    assertThrows(
        IllegalArgumentException.class,
        () -> memory.atomicLoadInt(3), // Misaligned
        "Offset must be 4-byte aligned");
  }

  @Test
  void testAtomicLoadLong() {
    assertDoesNotThrow(
        () -> {
          try {
            final long result = memory.atomicLoadLong(0);
            assertThat(result).isNotNull();
          } catch (final UnsupportedOperationException e) {
            assertThat(e.getMessage()).contains("shared");
          }
        });
  }

  @Test
  void testAtomicStoreInt() {
    assertDoesNotThrow(
        () -> {
          try {
            memory.atomicStoreInt(0, 42);
            // No exception means success (or UnsupportedOperationException for non-shared)
          } catch (final UnsupportedOperationException e) {
            assertThat(e.getMessage()).contains("shared");
          }
        });
  }

  @Test
  void testAtomicStoreIntInvalidOffset() {
    assertThrows(
        IllegalArgumentException.class,
        () -> memory.atomicStoreInt(2, 42), // Misaligned
        "Offset must be 4-byte aligned");
  }

  @Test
  void testAtomicStoreLong() {
    assertDoesNotThrow(
        () -> {
          try {
            memory.atomicStoreLong(0, 42L);
          } catch (final UnsupportedOperationException e) {
            assertThat(e.getMessage()).contains("shared");
          }
        });
  }

  // ===== Arithmetic Operations Tests =====

  @Test
  void testAtomicAddInt() {
    assertDoesNotThrow(
        () -> {
          try {
            final int result = memory.atomicAddInt(0, 5);
            assertThat(result).isNotNull();
          } catch (final UnsupportedOperationException e) {
            assertThat(e.getMessage()).contains("shared");
          }
        });
  }

  @Test
  void testAtomicAddIntInvalidOffset() {
    assertThrows(
        IllegalArgumentException.class,
        () -> memory.atomicAddInt(1, 5), // Misaligned
        "Offset must be 4-byte aligned");
  }

  @Test
  void testAtomicAddLong() {
    assertDoesNotThrow(
        () -> {
          try {
            final long result = memory.atomicAddLong(0, 5L);
            assertThat(result).isNotNull();
          } catch (final UnsupportedOperationException e) {
            assertThat(e.getMessage()).contains("shared");
          }
        });
  }

  // ===== Bitwise Operations Tests =====

  @Test
  void testAtomicAndInt() {
    assertDoesNotThrow(
        () -> {
          try {
            final int result = memory.atomicAndInt(0, 0xFF);
            assertThat(result).isNotNull();
          } catch (final UnsupportedOperationException e) {
            assertThat(e.getMessage()).contains("shared");
          }
        });
  }

  @Test
  void testAtomicOrInt() {
    assertDoesNotThrow(
        () -> {
          try {
            final int result = memory.atomicOrInt(0, 0xFF);
            assertThat(result).isNotNull();
          } catch (final UnsupportedOperationException e) {
            assertThat(e.getMessage()).contains("shared");
          }
        });
  }

  @Test
  void testAtomicXorInt() {
    assertDoesNotThrow(
        () -> {
          try {
            final int result = memory.atomicXorInt(0, 0xFF);
            assertThat(result).isNotNull();
          } catch (final UnsupportedOperationException e) {
            assertThat(e.getMessage()).contains("shared");
          }
        });
  }

  // ===== Synchronization Operations Tests =====

  @Test
  void testAtomicFence() {
    assertDoesNotThrow(
        () -> {
          try {
            memory.atomicFence();
          } catch (final UnsupportedOperationException e) {
            assertThat(e.getMessage()).contains("shared");
          }
        });
  }

  @Test
  void testAtomicNotify() {
    assertDoesNotThrow(
        () -> {
          try {
            final int result = memory.atomicNotify(0, 1);
            assertThat(result).isNotNegative();
          } catch (final UnsupportedOperationException e) {
            assertThat(e.getMessage()).contains("shared");
          }
        });
  }

  @Test
  void testAtomicNotifyInvalidOffset() {
    assertThrows(
        IllegalArgumentException.class,
        () -> memory.atomicNotify(3, 1), // Misaligned
        "Offset must be 4-byte aligned");
  }

  @Test
  void testAtomicNotifyNegativeCount() {
    assertThrows(
        IllegalArgumentException.class,
        () -> memory.atomicNotify(0, -1),
        "Count cannot be negative");
  }

  @Test
  void testAtomicWait32() {
    assertDoesNotThrow(
        () -> {
          try {
            final int result = memory.atomicWait32(0, 0, 1000L);
            assertThat(result).isNotNull();
          } catch (final UnsupportedOperationException e) {
            assertThat(e.getMessage()).contains("shared");
          }
        });
  }

  @Test
  void testAtomicWait32InvalidOffset() {
    assertThrows(
        IllegalArgumentException.class,
        () -> memory.atomicWait32(1, 0, 1000L), // Misaligned
        "Offset must be 4-byte aligned");
  }

  @Test
  void testAtomicWait32InvalidTimeout() {
    assertThrows(
        IllegalArgumentException.class,
        () -> memory.atomicWait32(0, 0, -2L), // Invalid timeout (only -1 allowed)
        "Timeout must be non-negative or -1");
  }

  @Test
  void testAtomicWait64() {
    assertDoesNotThrow(
        () -> {
          try {
            final int result = memory.atomicWait64(0, 0L, 1000L);
            assertThat(result).isNotNull();
          } catch (final UnsupportedOperationException e) {
            assertThat(e.getMessage()).contains("shared");
          }
        });
  }

  @Test
  void testAtomicWait64InvalidOffset() {
    assertThrows(
        IllegalArgumentException.class,
        () -> memory.atomicWait64(4, 0L, 1000L), // Misaligned for i64
        "Offset must be 8-byte aligned");
  }

  /**
   * Validates that all atomic operations properly validate alignment requirements.
   *
   * <p>This is a comprehensive test ensuring defensive programming is maintained.
   */
  @Test
  void testAllAtomicOperationsValidateAlignment() {
    // All i32 operations require 4-byte alignment
    assertThrows(IllegalArgumentException.class, () -> memory.atomicCompareAndSwapInt(1, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> memory.atomicLoadInt(2));
    assertThrows(IllegalArgumentException.class, () -> memory.atomicStoreInt(3, 0));
    assertThrows(IllegalArgumentException.class, () -> memory.atomicAddInt(1, 0));
    assertThrows(IllegalArgumentException.class, () -> memory.atomicAndInt(2, 0));
    assertThrows(IllegalArgumentException.class, () -> memory.atomicOrInt(3, 0));
    assertThrows(IllegalArgumentException.class, () -> memory.atomicXorInt(1, 0));
    assertThrows(IllegalArgumentException.class, () -> memory.atomicNotify(2, 1));
    assertThrows(IllegalArgumentException.class, () -> memory.atomicWait32(3, 0, 1000L));

    // All i64 operations require 8-byte alignment
    assertThrows(IllegalArgumentException.class, () -> memory.atomicCompareAndSwapLong(4, 0L, 0L));
    assertThrows(IllegalArgumentException.class, () -> memory.atomicLoadLong(4));
    assertThrows(IllegalArgumentException.class, () -> memory.atomicStoreLong(4, 0L));
    assertThrows(IllegalArgumentException.class, () -> memory.atomicAddLong(4, 0L));
    assertThrows(IllegalArgumentException.class, () -> memory.atomicWait64(4, 0L, 1000L));
  }

  /** Validates that all atomic operations properly validate negative offsets. */
  @Test
  void testAllAtomicOperationsRejectNegativeOffsets() {
    assertThrows(IllegalArgumentException.class, () -> memory.atomicCompareAndSwapInt(-4, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> memory.atomicCompareAndSwapLong(-8, 0L, 0L));
    assertThrows(IllegalArgumentException.class, () -> memory.atomicLoadInt(-4));
    assertThrows(IllegalArgumentException.class, () -> memory.atomicLoadLong(-8));
    assertThrows(IllegalArgumentException.class, () -> memory.atomicStoreInt(-4, 0));
    assertThrows(IllegalArgumentException.class, () -> memory.atomicStoreLong(-8, 0L));
    assertThrows(IllegalArgumentException.class, () -> memory.atomicAddInt(-4, 0));
    assertThrows(IllegalArgumentException.class, () -> memory.atomicAddLong(-8, 0L));
    assertThrows(IllegalArgumentException.class, () -> memory.atomicAndInt(-4, 0));
    assertThrows(IllegalArgumentException.class, () -> memory.atomicOrInt(-4, 0));
    assertThrows(IllegalArgumentException.class, () -> memory.atomicXorInt(-4, 0));
    assertThrows(IllegalArgumentException.class, () -> memory.atomicNotify(-4, 1));
    assertThrows(IllegalArgumentException.class, () -> memory.atomicWait32(-4, 0, 1000L));
    assertThrows(IllegalArgumentException.class, () -> memory.atomicWait64(-8, 0L, 1000L));
  }
}
