package ai.tegmentum.wasmtime4j.tests.memory;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/** Comprehensive tests for WebAssembly memory operations. */
public class MemoryOperationsTest extends DualRuntimeTest {

  /** Clears the runtime selection after each test. */
  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Basic memory allocation")
  public void testMemoryAllocation(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final String wat =
        """
        (module
          (memory (export "mem") 1)
          (func (export "get_memory_size") (result i32)
            memory.size
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    final WasmValue[] results = instance.callFunction("get_memory_size");

    assertEquals(1, results.length);
    assertEquals(1, results[0].asInt()); // 1 page = 64KB

    instance.close();
    store.close();
    engine.close();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Memory store and load i32")
  public void testMemoryStoreLoadI32(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final String wat =
        """
        (module
          (memory 1)
          (func (export "store_and_load") (param i32 i32) (result i32)
            ;; Store value at address
            local.get 0  ;; address
            local.get 1  ;; value
            i32.store

            ;; Load value from address
            local.get 0
            i32.load
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    final WasmValue[] results =
        instance.callFunction("store_and_load", WasmValue.i32(0), WasmValue.i32(42));

    assertEquals(1, results.length);
    assertEquals(42, results[0].asInt());

    instance.close();
    store.close();
    engine.close();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Memory store and load i64")
  public void testMemoryStoreLoadI64(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final String wat =
        """
        (module
          (memory 1)
          (func (export "store_and_load_i64") (param i32 i64) (result i64)
            local.get 0
            local.get 1
            i64.store

            local.get 0
            i64.load
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    final WasmValue[] results =
        instance.callFunction("store_and_load_i64", WasmValue.i32(8), WasmValue.i64(123456789L));

    assertEquals(1, results.length);
    assertEquals(123456789L, results[0].asLong());

    instance.close();
    store.close();
    engine.close();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Memory store and load f32")
  public void testMemoryStoreLoadF32(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final String wat =
        """
        (module
          (memory 1)
          (func (export "store_and_load_f32") (param i32 f32) (result f32)
            local.get 0
            local.get 1
            f32.store

            local.get 0
            f32.load
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    final WasmValue[] results =
        instance.callFunction("store_and_load_f32", WasmValue.i32(16), WasmValue.f32(3.14159f));

    assertEquals(1, results.length);
    assertEquals(3.14159f, results[0].asFloat(), 0.00001f);

    instance.close();
    store.close();
    engine.close();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Memory store and load f64")
  public void testMemoryStoreLoadF64(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final String wat =
        """
        (module
          (memory 1)
          (func (export "store_and_load_f64") (param i32 f64) (result f64)
            local.get 0
            local.get 1
            f64.store

            local.get 0
            f64.load
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    final WasmValue[] results =
        instance.callFunction("store_and_load_f64", WasmValue.i32(24), WasmValue.f64(2.718281828));

    assertEquals(1, results.length);
    assertEquals(2.718281828, results[0].asDouble(), 0.000000001);

    instance.close();
    store.close();
    engine.close();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Memory store at different offsets")
  public void testMemoryMultipleOffsets(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final String wat =
        """
        (module
          (memory 1)
          (func (export "write_pattern") (result i32)
            ;; Store values at different offsets
            i32.const 0
            i32.const 10
            i32.store

            i32.const 4
            i32.const 20
            i32.store

            i32.const 8
            i32.const 30
            i32.store

            ;; Load and sum
            i32.const 0
            i32.load
            i32.const 4
            i32.load
            i32.add
            i32.const 8
            i32.load
            i32.add
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    final WasmValue[] results = instance.callFunction("write_pattern");

    assertEquals(1, results.length);
    assertEquals(60, results[0].asInt()); // 10 + 20 + 30

    instance.close();
    store.close();
    engine.close();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Memory growth")
  public void testMemoryGrow(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final String wat =
        """
        (module
          (memory (export "mem") 1 10)
          (func (export "grow_memory") (param i32) (result i32)
            local.get 0
            memory.grow
          )
          (func (export "get_size") (result i32)
            memory.size
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    // Get initial size
    WasmValue[] results = instance.callFunction("get_size");
    assertEquals(1, results[0].asInt());

    // Grow by 2 pages
    results = instance.callFunction("grow_memory", WasmValue.i32(2));
    assertEquals(1, results[0].asInt()); // Returns previous size

    // Check new size
    results = instance.callFunction("get_size");
    assertEquals(3, results[0].asInt());

    instance.close();
    store.close();
    engine.close();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Memory byte operations")
  public void testMemoryByteOperations(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final String wat =
        """
        (module
          (memory 1)
          (func (export "store_byte") (param i32 i32)
            local.get 0
            local.get 1
            i32.store8
          )
          (func (export "load_byte") (param i32) (result i32)
            local.get 0
            i32.load8_u
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    // Store byte value
    instance.callFunction("store_byte", WasmValue.i32(0), WasmValue.i32(255));

    // Load byte value
    final WasmValue[] results = instance.callFunction("load_byte", WasmValue.i32(0));

    assertEquals(1, results.length);
    assertEquals(255, results[0].asInt());

    instance.close();
    store.close();
    engine.close();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Memory boundary access")
  public void testMemoryBoundaryAccess(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final String wat =
        """
        (module
          (memory 1)
          (func (export "write_at_boundary") (result i32)
            ;; Write at near end of first page (64KB - 4 bytes)
            i32.const 65532
            i32.const 42
            i32.store

            ;; Read it back
            i32.const 65532
            i32.load
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    final WasmValue[] results = instance.callFunction("write_at_boundary");

    assertEquals(1, results.length);
    assertEquals(42, results[0].asInt());

    instance.close();
    store.close();
    engine.close();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Memory copy pattern")
  public void testMemoryCopyPattern(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final String wat =
        """
        (module
          (memory 1)
          (func (export "copy_value") (param i32 i32) (result i32)
            ;; Store value at source
            local.get 0
            i32.const 42
            i32.store

            ;; Copy from source to destination
            local.get 1
            local.get 0
            i32.load
            i32.store

            ;; Load from destination and return
            local.get 1
            i32.load
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    final WasmValue[] results =
        instance.callFunction("copy_value", WasmValue.i32(0), WasmValue.i32(100));

    assertEquals(1, results.length);
    assertEquals(42, results[0].asInt());

    instance.close();
    store.close();
    engine.close();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Memory fill pattern")
  public void testMemoryFillPattern(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final String wat =
        """
        (module
          (memory 1)
          (func (export "fill_and_read") (result i32)
            ;; Fill 5 consecutive i32 values
            i32.const 0
            i32.const 10
            i32.store

            i32.const 4
            i32.const 20
            i32.store

            i32.const 8
            i32.const 30
            i32.store

            i32.const 12
            i32.const 40
            i32.store

            i32.const 16
            i32.const 50
            i32.store

            ;; Sum them
            i32.const 0
            i32.load
            i32.const 4
            i32.load
            i32.add
            i32.const 8
            i32.load
            i32.add
            i32.const 12
            i32.load
            i32.add
            i32.const 16
            i32.load
            i32.add
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    final WasmValue[] results = instance.callFunction("fill_and_read");

    assertEquals(1, results.length);
    assertEquals(150, results[0].asInt()); // 10+20+30+40+50

    instance.close();
    store.close();
    engine.close();
  }
}
