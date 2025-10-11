package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Basic test for Panama instance creation. */
public class PanamaInstanceBasicTest {

  @Test
  @DisplayName("Create and destroy Panama instance successfully")
  public void testPanamaInstanceCreation() throws WasmException {
    // WASM bytecode for: (module (func (export "get42") (result i32) i32.const 42))
    final byte[] wasmBytes =
        new byte[] {
          0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x05, 0x01, 0x60, 0x00, 0x01,
          0x7f, 0x03, 0x02, 0x01, 0x00, 0x07, 0x09, 0x01, 0x05, 0x67, 0x65, 0x74, 0x34, 0x32,
          0x00, 0x00, 0x0a, 0x06, 0x01, 0x04, 0x00, 0x41, 0x2a, 0x0b
        };

    final PanamaEngine engine = new PanamaEngine();
    assertNotNull(engine, "Engine should not be null");
    assertTrue(engine.isValid(), "Engine should be valid");

    final PanamaStore store = new PanamaStore(engine);
    assertNotNull(store, "Store should not be null");
    assertTrue(store.isValid(), "Store should be valid");

    final PanamaModule module = new PanamaModule(engine, wasmBytes);
    assertNotNull(module, "Module should not be null");
    assertTrue(module.isValid(), "Module should be valid");

    final PanamaInstance instance = new PanamaInstance(module, store);
    assertNotNull(instance, "Instance should not be null");
    assertTrue(instance.isValid(), "Instance should be valid");
    assertNotNull(instance.getNativeInstance(), "Native instance should not be null");

    // Verify resource lifecycle
    instance.close();
    assertFalse(instance.isValid(), "Instance should be invalid after close");

    store.close();
    assertFalse(store.isValid(), "Store should be invalid after close");

    module.close();
    assertFalse(module.isValid(), "Module should be invalid after close");

    engine.close();
    assertFalse(engine.isValid(), "Engine should be invalid after close");
  }

  @Test
  @DisplayName("Reject null module")
  public void testNullModule() throws WasmException {
    final PanamaEngine engine = new PanamaEngine();
    final PanamaStore store = new PanamaStore(engine);

    assertThrows(
        IllegalArgumentException.class,
        () -> new PanamaInstance(null, store),
        "Should reject null module");

    store.close();
    engine.close();
  }

  @Test
  @DisplayName("Reject null store")
  public void testNullStore() throws WasmException {
    final byte[] wasmBytes =
        new byte[] {
          0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x05, 0x01, 0x60, 0x00, 0x01,
          0x7f, 0x03, 0x02, 0x01, 0x00, 0x07, 0x09, 0x01, 0x05, 0x67, 0x65, 0x74, 0x34, 0x32,
          0x00, 0x00, 0x0a, 0x06, 0x01, 0x04, 0x00, 0x41, 0x2a, 0x0b
        };

    final PanamaEngine engine = new PanamaEngine();
    final PanamaModule module = new PanamaModule(engine, wasmBytes);

    assertThrows(
        IllegalArgumentException.class,
        () -> new PanamaInstance(module, null),
        "Should reject null store");

    module.close();
    engine.close();
  }

  @Test
  @DisplayName("Reject invalid module")
  public void testInvalidModule() throws WasmException {
    final byte[] wasmBytes =
        new byte[] {
          0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x05, 0x01, 0x60, 0x00, 0x01,
          0x7f, 0x03, 0x02, 0x01, 0x00, 0x07, 0x09, 0x01, 0x05, 0x67, 0x65, 0x74, 0x34, 0x32,
          0x00, 0x00, 0x0a, 0x06, 0x01, 0x04, 0x00, 0x41, 0x2a, 0x0b
        };

    final PanamaEngine engine = new PanamaEngine();
    final PanamaStore store = new PanamaStore(engine);
    final PanamaModule module = new PanamaModule(engine, wasmBytes);

    // Close module to make it invalid
    module.close();

    assertThrows(
        IllegalStateException.class,
        () -> new PanamaInstance(module, store),
        "Should reject invalid module");

    store.close();
    engine.close();
  }

  @Test
  @DisplayName("Reject invalid store")
  public void testInvalidStore() throws WasmException {
    final byte[] wasmBytes =
        new byte[] {
          0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x05, 0x01, 0x60, 0x00, 0x01,
          0x7f, 0x03, 0x02, 0x01, 0x00, 0x07, 0x09, 0x01, 0x05, 0x67, 0x65, 0x74, 0x34, 0x32,
          0x00, 0x00, 0x0a, 0x06, 0x01, 0x04, 0x00, 0x41, 0x2a, 0x0b
        };

    final PanamaEngine engine = new PanamaEngine();
    final PanamaStore store = new PanamaStore(engine);
    final PanamaModule module = new PanamaModule(engine, wasmBytes);

    // Close store to make it invalid
    store.close();

    assertThrows(
        IllegalStateException.class,
        () -> new PanamaInstance(module, store),
        "Should reject invalid store");

    module.close();
    engine.close();
  }
}
