package ai.tegmentum.wasmtime4j.comparison.lifecycle;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for proper resource lifecycle management and cleanup. */
public class ResourceManagementTest {

  @Test
  @DisplayName("Engine creation and disposal")
  public void testEngineLifecycle() throws Exception {
    final Engine engine = Engine.create();
    assertNotNull(engine);
    engine.close();
    // No exception = success
  }

  @Test
  @DisplayName("Store creation and disposal")
  public void testStoreLifecycle() throws Exception {
    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    assertNotNull(store);
    store.close();
    engine.close();
  }

  @Test
  @DisplayName("Module compilation and disposal")
  public void testModuleLifecycle() throws Exception {
    final String wat =
        """
        (module
          (func (export "test") (result i32)
            i32.const 42
          )
        )
        """;

    final Engine engine = Engine.create();
    final Module module = engine.compileWat(wat);
    assertNotNull(module);
    engine.close();
  }

  @Test
  @DisplayName("Instance creation and disposal")
  public void testInstanceLifecycle() throws Exception {
    final String wat =
        """
        (module
          (func (export "test") (result i32)
            i32.const 42
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    assertNotNull(instance);

    instance.close();
    store.close();
    engine.close();
  }

  @Test
  @DisplayName("Multiple stores from same engine")
  public void testMultipleStores() throws Exception {
    final Engine engine = Engine.create();

    final Store store1 = engine.createStore();
    final Store store2 = engine.createStore();
    final Store store3 = engine.createStore();

    assertNotNull(store1);
    assertNotNull(store2);
    assertNotNull(store3);

    store1.close();
    store2.close();
    store3.close();
    engine.close();
  }

  @Test
  @DisplayName("Multiple instances from same module")
  public void testMultipleInstances() throws Exception {
    final String wat =
        """
        (module
          (func (export "test") (result i32)
            i32.const 42
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);

    final Instance instance1 = module.instantiate(store);
    final Instance instance2 = module.instantiate(store);
    final Instance instance3 = module.instantiate(store);

    assertNotNull(instance1);
    assertNotNull(instance2);
    assertNotNull(instance3);

    // All instances should work independently
    final WasmValue[] r1 = instance1.callFunction("test");
    final WasmValue[] r2 = instance2.callFunction("test");
    final WasmValue[] r3 = instance3.callFunction("test");

    assertEquals(42, r1[0].asInt());
    assertEquals(42, r2[0].asInt());
    assertEquals(42, r3[0].asInt());

    instance1.close();
    instance2.close();
    instance3.close();
    store.close();
    engine.close();
  }

  @Test
  @DisplayName("Reuse engine for multiple modules")
  public void testEngineReuse() throws Exception {
    final String wat1 =
        """
        (module
          (func (export "get_value") (result i32)
            i32.const 10
          )
        )
        """;

    final String wat2 =
        """
        (module
          (func (export "get_value") (result i32)
            i32.const 20
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();

    final Module module1 = engine.compileWat(wat1);
    final Module module2 = engine.compileWat(wat2);

    final Instance instance1 = module1.instantiate(store);
    final Instance instance2 = module2.instantiate(store);

    final WasmValue[] r1 = instance1.callFunction("get_value");
    final WasmValue[] r2 = instance2.callFunction("get_value");

    assertEquals(10, r1[0].asInt());
    assertEquals(20, r2[0].asInt());

    instance1.close();
    instance2.close();
    store.close();
    engine.close();
  }

  @Test
  @DisplayName("Sequential function calls on same instance")
  public void testSequentialCalls() throws Exception {
    final String wat =
        """
        (module
          (global $counter (mut i32) (i32.const 0))
          (func (export "increment") (result i32)
            global.get $counter
            i32.const 1
            i32.add
            global.set $counter
            global.get $counter
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    // Call multiple times, counter should increment
    final WasmValue[] r1 = instance.callFunction("increment");
    assertEquals(1, r1[0].asInt());

    final WasmValue[] r2 = instance.callFunction("increment");
    assertEquals(2, r2[0].asInt());

    final WasmValue[] r3 = instance.callFunction("increment");
    assertEquals(3, r3[0].asInt());

    instance.close();
    store.close();
    engine.close();
  }

  @Test
  @DisplayName("Close resources in correct order")
  public void testProperCloseOrder() throws Exception {
    final String wat =
        """
        (module
          (func (export "test") (result i32)
            i32.const 42
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(store);

    // Call function
    final WasmValue[] results = instance.callFunction("test");
    assertEquals(42, results[0].asInt());

    // Close in reverse order of creation
    instance.close();
    store.close();
    engine.close();
  }

  @Test
  @DisplayName("Idempotent close operations")
  public void testIdempotentClose() throws Exception {
    final Engine engine = Engine.create();
    final Store store = engine.createStore();

    // Closing multiple times should be safe
    store.close();
    store.close();
    store.close();

    engine.close();
    engine.close();
    engine.close();
  }

  @Test
  @DisplayName("Create many instances in sequence")
  public void testManySequentialInstances() throws Exception {
    final String wat =
        """
        (module
          (func (export "test") (result i32)
            i32.const 42
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);

    // Create and destroy many instances
    for (int i = 0; i < 100; i++) {
      final Instance instance = module.instantiate(store);
      final WasmValue[] results = instance.callFunction("test");
      assertEquals(42, results[0].asInt());
      instance.close();
    }

    store.close();
    engine.close();
  }

  @Test
  @DisplayName("Memory cleanup between instances")
  public void testMemoryCleanup() throws Exception {
    final String wat =
        """
        (module
          (memory 1)
          (func (export "write_value") (param i32 i32)
            local.get 0
            local.get 1
            i32.store
          )
          (func (export "read_value") (param i32) (result i32)
            local.get 0
            i32.load
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);

    // First instance - write a value
    final Instance instance1 = module.instantiate(store);
    instance1.callFunction("write_value", WasmValue.i32(0), WasmValue.i32(123));
    final WasmValue[] r1 = instance1.callFunction("read_value", WasmValue.i32(0));
    assertEquals(123, r1[0].asInt());
    instance1.close();

    // Second instance - memory should be fresh (zero)
    final Instance instance2 = module.instantiate(store);
    final WasmValue[] r2 = instance2.callFunction("read_value", WasmValue.i32(0));
    assertEquals(0, r2[0].asInt());
    instance2.close();

    store.close();
    engine.close();
  }

  @Test
  @DisplayName("Global state isolation between instances")
  public void testGlobalStateIsolation() throws Exception {
    final String wat =
        """
        (module
          (global $state (mut i32) (i32.const 0))
          (func (export "set") (param i32)
            local.get 0
            global.set $state
          )
          (func (export "get") (result i32)
            global.get $state
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);

    final Instance instance1 = module.instantiate(store);
    final Instance instance2 = module.instantiate(store);

    // Set different values in each instance
    instance1.callFunction("set", WasmValue.i32(10));
    instance2.callFunction("set", WasmValue.i32(20));

    // Each instance should have its own state
    final WasmValue[] r1 = instance1.callFunction("get");
    final WasmValue[] r2 = instance2.callFunction("get");

    assertEquals(10, r1[0].asInt());
    assertEquals(20, r2[0].asInt());

    instance1.close();
    instance2.close();
    store.close();
    engine.close();
  }
}
