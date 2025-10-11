package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for Panama instance export metadata functionality. */
public class PanamaInstanceExportMetadataTest {

  @Test
  @DisplayName("getExportNames should return export name")
  public void testGetExportNames() throws WasmException {
    // WASM bytecode for: (module (func (export "get42") (result i32) i32.const 42))
    final byte[] wasmBytes =
        new byte[] {
          0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x05, 0x01, 0x60, 0x00, 0x01,
          0x7f, 0x03, 0x02, 0x01, 0x00, 0x07, 0x09, 0x01, 0x05, 0x67, 0x65, 0x74, 0x34, 0x32,
          0x00, 0x00, 0x0a, 0x06, 0x01, 0x04, 0x00, 0x41, 0x2a, 0x0b
        };

    try (final PanamaEngine engine = new PanamaEngine();
        final PanamaStore store = new PanamaStore(engine);
        final PanamaModule module = new PanamaModule(engine, wasmBytes);
        final PanamaInstance instance = new PanamaInstance(module, store)) {

      final String[] exportNames = instance.getExportNames();
      assertNotNull(exportNames, "Export names should not be null");
      assertEquals(1, exportNames.length, "Should have 1 export");
      assertEquals("get42", exportNames[0], "Export name should be 'get42'");
    }
  }

  @Test
  @DisplayName("getMetadataExportCount should return correct count")
  public void testGetMetadataExportCount() throws WasmException {
    // WASM bytecode for: (module (func (export "get42") (result i32) i32.const 42))
    final byte[] wasmBytes =
        new byte[] {
          0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x05, 0x01, 0x60, 0x00, 0x01,
          0x7f, 0x03, 0x02, 0x01, 0x00, 0x07, 0x09, 0x01, 0x05, 0x67, 0x65, 0x74, 0x34, 0x32,
          0x00, 0x00, 0x0a, 0x06, 0x01, 0x04, 0x00, 0x41, 0x2a, 0x0b
        };

    try (final PanamaEngine engine = new PanamaEngine();
        final PanamaStore store = new PanamaStore(engine);
        final PanamaModule module = new PanamaModule(engine, wasmBytes);
        final PanamaInstance instance = new PanamaInstance(module, store)) {

      final int exportCount = instance.getMetadataExportCount();
      assertEquals(1, exportCount, "Should have 1 export");
    }
  }

  @Test
  @DisplayName("hasExport should return true for existing export")
  public void testHasExportExisting() throws WasmException {
    // WASM bytecode for: (module (func (export "get42") (result i32) i32.const 42))
    final byte[] wasmBytes =
        new byte[] {
          0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x05, 0x01, 0x60, 0x00, 0x01,
          0x7f, 0x03, 0x02, 0x01, 0x00, 0x07, 0x09, 0x01, 0x05, 0x67, 0x65, 0x74, 0x34, 0x32,
          0x00, 0x00, 0x0a, 0x06, 0x01, 0x04, 0x00, 0x41, 0x2a, 0x0b
        };

    try (final PanamaEngine engine = new PanamaEngine();
        final PanamaStore store = new PanamaStore(engine);
        final PanamaModule module = new PanamaModule(engine, wasmBytes);
        final PanamaInstance instance = new PanamaInstance(module, store)) {

      assertTrue(instance.hasExport("get42"), "Should have 'get42' export");
    }
  }

  @Test
  @DisplayName("hasExport should return false for non-existing export")
  public void testHasExportNonExisting() throws WasmException {
    // WASM bytecode for: (module (func (export "get42") (result i32) i32.const 42))
    final byte[] wasmBytes =
        new byte[] {
          0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x05, 0x01, 0x60, 0x00, 0x01,
          0x7f, 0x03, 0x02, 0x01, 0x00, 0x07, 0x09, 0x01, 0x05, 0x67, 0x65, 0x74, 0x34, 0x32,
          0x00, 0x00, 0x0a, 0x06, 0x01, 0x04, 0x00, 0x41, 0x2a, 0x0b
        };

    try (final PanamaEngine engine = new PanamaEngine();
        final PanamaStore store = new PanamaStore(engine);
        final PanamaModule module = new PanamaModule(engine, wasmBytes);
        final PanamaInstance instance = new PanamaInstance(module, store)) {

      assertFalse(instance.hasExport("nonexistent"), "Should not have 'nonexistent' export");
      assertFalse(instance.hasExport(""), "Should not have empty string export");
      assertFalse(instance.hasExport("foo"), "Should not have 'foo' export");
    }
  }

  @Test
  @DisplayName("getFunction should return empty Optional for non-existing function")
  public void testGetFunctionNonExisting() throws WasmException {
    // WASM bytecode for: (module (func (export "get42") (result i32) i32.const 42))
    final byte[] wasmBytes =
        new byte[] {
          0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x05, 0x01, 0x60, 0x00, 0x01,
          0x7f, 0x03, 0x02, 0x01, 0x00, 0x07, 0x09, 0x01, 0x05, 0x67, 0x65, 0x74, 0x34, 0x32,
          0x00, 0x00, 0x0a, 0x06, 0x01, 0x04, 0x00, 0x41, 0x2a, 0x0b
        };

    try (final PanamaEngine engine = new PanamaEngine();
        final PanamaStore store = new PanamaStore(engine);
        final PanamaModule module = new PanamaModule(engine, wasmBytes);
        final PanamaInstance instance = new PanamaInstance(module, store)) {

      final Optional<WasmFunction> function = instance.getFunction("nonexistent");
      assertNotNull(function, "Optional should not be null");
      assertFalse(function.isPresent(), "Function should not be present");
    }
  }

  @Test
  @DisplayName("getFunction should return function for existing export")
  public void testGetFunctionExisting() throws WasmException {
    // WASM bytecode for: (module (func (export "get42") (result i32) i32.const 42))
    final byte[] wasmBytes =
        new byte[] {
          0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x05, 0x01, 0x60, 0x00, 0x01,
          0x7f, 0x03, 0x02, 0x01, 0x00, 0x07, 0x09, 0x01, 0x05, 0x67, 0x65, 0x74, 0x34, 0x32,
          0x00, 0x00, 0x0a, 0x06, 0x01, 0x04, 0x00, 0x41, 0x2a, 0x0b
        };

    try (final PanamaEngine engine = new PanamaEngine();
        final PanamaStore store = new PanamaStore(engine);
        final PanamaModule module = new PanamaModule(engine, wasmBytes);
        final PanamaInstance instance = new PanamaInstance(module, store)) {

      final Optional<WasmFunction> function = instance.getFunction("get42");
      assertNotNull(function, "Optional should not be null");
      assertTrue(function.isPresent(), "Function should be present");
      assertEquals("get42", function.get().getName(), "Function name should match");
    }
  }

  @Test
  @DisplayName("getFunction by index should return function")
  public void testGetFunctionByIndex() throws WasmException {
    // WASM bytecode for: (module (func (export "get42") (result i32) i32.const 42))
    final byte[] wasmBytes =
        new byte[] {
          0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x05, 0x01, 0x60, 0x00, 0x01,
          0x7f, 0x03, 0x02, 0x01, 0x00, 0x07, 0x09, 0x01, 0x05, 0x67, 0x65, 0x74, 0x34, 0x32,
          0x00, 0x00, 0x0a, 0x06, 0x01, 0x04, 0x00, 0x41, 0x2a, 0x0b
        };

    try (final PanamaEngine engine = new PanamaEngine();
        final PanamaStore store = new PanamaStore(engine);
        final PanamaModule module = new PanamaModule(engine, wasmBytes);
        final PanamaInstance instance = new PanamaInstance(module, store)) {

      final Optional<WasmFunction> function = instance.getFunction(0);
      assertTrue(function.isPresent(), "Function at index 0 should be present");
    }
  }

  @Test
  @DisplayName("getFunction by index should return empty for out of bounds")
  public void testGetFunctionByIndexOutOfBounds() throws WasmException {
    // WASM bytecode for: (module (func (export "get42") (result i32) i32.const 42))
    final byte[] wasmBytes =
        new byte[] {
          0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x05, 0x01, 0x60, 0x00, 0x01,
          0x7f, 0x03, 0x02, 0x01, 0x00, 0x07, 0x09, 0x01, 0x05, 0x67, 0x65, 0x74, 0x34, 0x32,
          0x00, 0x00, 0x0a, 0x06, 0x01, 0x04, 0x00, 0x41, 0x2a, 0x0b
        };

    try (final PanamaEngine engine = new PanamaEngine();
        final PanamaStore store = new PanamaStore(engine);
        final PanamaModule module = new PanamaModule(engine, wasmBytes);
        final PanamaInstance instance = new PanamaInstance(module, store)) {

      final Optional<WasmFunction> function = instance.getFunction(99);
      assertNotNull(function, "Optional should not be null");
      assertFalse(function.isPresent(), "Function should not be present for out of bounds index");
    }
  }

  @Test
  @DisplayName("getFunction by negative index should throw exception")
  public void testGetFunctionByNegativeIndex() throws WasmException {
    // WASM bytecode for: (module (func (export "get42") (result i32) i32.const 42))
    final byte[] wasmBytes =
        new byte[] {
          0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x05, 0x01, 0x60, 0x00, 0x01,
          0x7f, 0x03, 0x02, 0x01, 0x00, 0x07, 0x09, 0x01, 0x05, 0x67, 0x65, 0x74, 0x34, 0x32,
          0x00, 0x00, 0x0a, 0x06, 0x01, 0x04, 0x00, 0x41, 0x2a, 0x0b
        };

    try (final PanamaEngine engine = new PanamaEngine();
        final PanamaStore store = new PanamaStore(engine);
        final PanamaModule module = new PanamaModule(engine, wasmBytes);
        final PanamaInstance instance = new PanamaInstance(module, store)) {

      assertThrows(
          IllegalArgumentException.class,
          () -> instance.getFunction(-1),
          "Should throw IllegalArgumentException for negative index");
    }
  }

  @Test
  @DisplayName("hasExport with null should throw exception")
  public void testHasExportNull() throws WasmException {
    // WASM bytecode for: (module (func (export "get42") (result i32) i32.const 42))
    final byte[] wasmBytes =
        new byte[] {
          0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x05, 0x01, 0x60, 0x00, 0x01,
          0x7f, 0x03, 0x02, 0x01, 0x00, 0x07, 0x09, 0x01, 0x05, 0x67, 0x65, 0x74, 0x34, 0x32,
          0x00, 0x00, 0x0a, 0x06, 0x01, 0x04, 0x00, 0x41, 0x2a, 0x0b
        };

    try (final PanamaEngine engine = new PanamaEngine();
        final PanamaStore store = new PanamaStore(engine);
        final PanamaModule module = new PanamaModule(engine, wasmBytes);
        final PanamaInstance instance = new PanamaInstance(module, store)) {

      assertThrows(
          IllegalArgumentException.class,
          () -> instance.hasExport(null),
          "Should throw IllegalArgumentException for null name");
    }
  }

  @Test
  @DisplayName("getFunction with null should throw exception")
  public void testGetFunctionNull() throws WasmException {
    // WASM bytecode for: (module (func (export "get42") (result i32) i32.const 42))
    final byte[] wasmBytes =
        new byte[] {
          0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, 0x01, 0x05, 0x01, 0x60, 0x00, 0x01,
          0x7f, 0x03, 0x02, 0x01, 0x00, 0x07, 0x09, 0x01, 0x05, 0x67, 0x65, 0x74, 0x34, 0x32,
          0x00, 0x00, 0x0a, 0x06, 0x01, 0x04, 0x00, 0x41, 0x2a, 0x0b
        };

    try (final PanamaEngine engine = new PanamaEngine();
        final PanamaStore store = new PanamaStore(engine);
        final PanamaModule module = new PanamaModule(engine, wasmBytes);
        final PanamaInstance instance = new PanamaInstance(module, store)) {

      assertThrows(
          IllegalArgumentException.class,
          () -> instance.getFunction(null),
          "Should throw IllegalArgumentException for null name");
    }
  }
}
