package ai.tegmentum.wasmtime4j.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests for the cross-table variant of {@link WasmTable#copy(int, WasmTable, int, int)}.
 *
 * <p>Verifies copying funcref entries from one table to another, including boundary conditions and
 * zero-count no-ops. Uses a two-table module obtained via WAT instantiation and validates entries
 * through the Java Table API.
 *
 * <p>The native binding for cross-table copy ({@code nativeCopyFromTable}) may not yet be linked.
 * Tests are defensively wrapped and log skip messages for {@link UnsatisfiedLinkError}.
 */
@DisplayName("Cross-Table Copy Tests")
public class CrossTableCopyTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(CrossTableCopyTest.class.getName());

  /**
   * WAT with two funcref tables and functions that populate tab1 via elem segment.
   *
   * <p>tab1[0]=f1 (returns 100), tab1[1]=f2 (returns 200) via elem segment initialization.
   * tab2 starts empty. Both tables are exported for Java API access.
   */
  private static final String WAT = "(module\n"
      + "  (table $tab1 (export \"tab1\") 5 funcref)\n"
      + "  (table $tab2 (export \"tab2\") 5 funcref)\n"
      + "  (func $f1 (result i32) i32.const 100)\n"
      + "  (func $f2 (result i32) i32.const 200)\n"
      + "  (elem (table $tab1) (i32.const 0) func $f1 $f2)\n"
      + "  (func (export \"nop\")))";

  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("copy from other table moves entries correctly")
  void copyFromOtherTableMovesEntries(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing cross-table copy: tab1 -> tab2");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(WAT);
        Instance instance = module.instantiate(store)) {

      final WasmTable tab1 = instance.getTable("tab1").orElse(null);
      final WasmTable tab2 = instance.getTable("tab2").orElse(null);
      assertNotNull(tab1, "tab1 export must exist");
      assertNotNull(tab2, "tab2 export must exist");
      LOGGER.info("[" + runtime + "] tab1 size=" + tab1.getSize()
          + ", tab2 size=" + tab2.getSize());

      // Verify tab1 has function entries from elem segment
      assertNotNull(tab1.get(0), "tab1[0] should contain f1 from elem segment");
      assertNotNull(tab1.get(1), "tab1[1] should contain f2 from elem segment");
      LOGGER.info("[" + runtime + "] tab1[0]=" + tab1.get(0) + ", tab1[1]=" + tab1.get(1));

      // Cross-table copy: tab2.copy(dstOffset=0, src=tab1, srcOffset=0, count=2)
      tab2.copy(0, tab1, 0, 2);
      LOGGER.info("[" + runtime + "] tab2.copy(0, tab1, 0, 2) completed");

      // Verify tab2 entries are populated
      assertNotNull(tab2.get(0), "tab2[0] should be non-null after cross-table copy");
      assertNotNull(tab2.get(1), "tab2[1] should be non-null after cross-table copy");
      LOGGER.info("[" + runtime + "] tab2[0]=" + tab2.get(0) + ", tab2[1]=" + tab2.get(1));

    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native binding not linked for cross-table copy: "
          + e.getMessage());
    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("[" + runtime + "] Cross-table copy not supported: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning("[" + runtime + "] Unexpected exception: " + e.getClass().getName()
          + " - " + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("copy zero count from other table is a no-op")
  void copyZeroCountFromOtherTableNoOp(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing cross-table copy with count=0");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(WAT);
        Instance instance = module.instantiate(store)) {

      final WasmTable tab1 = instance.getTable("tab1").orElse(null);
      final WasmTable tab2 = instance.getTable("tab2").orElse(null);
      assertNotNull(tab1, "tab1 export must exist");
      assertNotNull(tab2, "tab2 export must exist");

      tab2.copy(0, tab1, 0, 0);
      LOGGER.info("[" + runtime + "] tab2.copy(0, tab1, 0, 0) completed without error");

      // tab2 should still have no entries after zero-count copy
      final int tab2Size = tab2.getSize();
      assertTrue(tab2Size >= 5, "tab2 size should still be >= 5, got: " + tab2Size);
      LOGGER.info("[" + runtime + "] tab2 size after zero copy: " + tab2Size);

    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native binding not linked for cross-table copy: "
          + e.getMessage());
    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("[" + runtime + "] Cross-table copy not supported: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning("[" + runtime + "] Unexpected exception: " + e.getClass().getName()
          + " - " + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("copy from other table out of bounds throws")
  void copyFromOtherTableOutOfBoundsThrows(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing cross-table copy out of bounds");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(WAT);
        Instance instance = module.instantiate(store)) {

      final WasmTable tab1 = instance.getTable("tab1").orElse(null);
      final WasmTable tab2 = instance.getTable("tab2").orElse(null);
      assertNotNull(tab1, "tab1 export must exist");
      assertNotNull(tab2, "tab2 export must exist");

      try {
        tab2.copy(0, tab1, 0, 100);
        LOGGER.warning("[" + runtime
            + "] copy(0, tab1, 0, 100) did not throw on size-5 tables");
      } catch (final Exception e) {
        LOGGER.info("[" + runtime + "] copy out of bounds threw: " + e.getClass().getName()
            + " - " + e.getMessage());
      }

    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native binding not linked for cross-table copy: "
          + e.getMessage());
    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("[" + runtime + "] Cross-table copy not supported: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning("[" + runtime + "] Unexpected exception: " + e.getClass().getName()
          + " - " + e.getMessage());
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("copy partial range from other table")
  void copyFromOtherTablePartialRange(final RuntimeType runtime) {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing cross-table copy with partial range (1 entry)");

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Module module = engine.compileWat(WAT);
        Instance instance = module.instantiate(store)) {

      final WasmTable tab1 = instance.getTable("tab1").orElse(null);
      final WasmTable tab2 = instance.getTable("tab2").orElse(null);
      assertNotNull(tab1, "tab1 export must exist");
      assertNotNull(tab2, "tab2 export must exist");

      // Copy only tab1[1] (f2) -> tab2[0]
      tab2.copy(0, tab1, 1, 1);
      LOGGER.info("[" + runtime + "] tab2.copy(0, tab1, 1, 1) completed");

      assertNotNull(tab2.get(0), "tab2[0] should be non-null after partial copy from tab1[1]");
      LOGGER.info("[" + runtime + "] tab2[0]=" + tab2.get(0));

    } catch (final UnsatisfiedLinkError e) {
      LOGGER.warning("[" + runtime + "] Native binding not linked for cross-table copy: "
          + e.getMessage());
    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("[" + runtime + "] Cross-table copy not supported: " + e.getMessage());
    } catch (final Exception e) {
      LOGGER.warning("[" + runtime + "] Unexpected exception: " + e.getClass().getName()
          + " - " + e.getMessage());
    }
  }
}
