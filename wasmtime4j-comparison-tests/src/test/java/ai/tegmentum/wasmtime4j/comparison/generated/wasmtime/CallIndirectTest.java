package ai.tegmentum.wasmtime4j.comparison.generated.wasmtime;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.comparison.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.comparison.framework.WastTestRunner;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Exception;
import java.lang.String;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Generated test from WAST file: call_indirect.wast
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream
 * Wasmtime implementation for this test case.
 */
public final class CallIndirectTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = CallIndirectTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("call indirect")
  public void testCallIndirect(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {

      // Compile and instantiate module 1
      // WAT file: ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/CallIndirectTest_module1.wat
      final String moduleWat1 = loadResource("/ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/CallIndirectTest_module1.wat");
      runner.compileAndInstantiate(moduleWat1);

      // ( register "test")
      runner.registerModule("test");

      // Compile and instantiate module 2
      // WAT file: ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/CallIndirectTest_module2.wat
      final String moduleWat2 = loadResource("/ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/CallIndirectTest_module2.wat");
      runner.compileAndInstantiate(moduleWat2);

      // ( assert_return ( invoke "type-i32") ( i32.const 0x132))
      runner.assertReturn("type-i32", new WasmValue[] { WasmValue.i32(0x132) });

      // ( assert_return ( invoke "type-i64") ( i64.const 0x164))
      runner.assertReturn("type-i64", new WasmValue[] { WasmValue.i64(0x164L) });

      // ( assert_return ( invoke "type-f32") ( f32.const 0xf32))
      runner.assertReturn("type-f32", new WasmValue[] { WasmValue.f32(3890.0f) });

      // ( assert_return ( invoke "type-f64") ( f64.const 0xf64))
      runner.assertReturn("type-f64", new WasmValue[] { WasmValue.f64(3940.0) });

      // ( assert_return ( invoke "type-index") ( i64.const 100))
      runner.assertReturn("type-index", new WasmValue[] { WasmValue.i64(100L) });

      // ( assert_return ( invoke "type-first-i32") ( i32.const 32))
      runner.assertReturn("type-first-i32", new WasmValue[] { WasmValue.i32(32) });

      // ( assert_return ( invoke "type-first-i64") ( i64.const 64))
      runner.assertReturn("type-first-i64", new WasmValue[] { WasmValue.i64(64L) });

      // ( assert_return ( invoke "type-first-f32") ( f32.const 1.32))
      runner.assertReturn("type-first-f32", new WasmValue[] { WasmValue.f32(1.32f) });

      // ( assert_return ( invoke "type-first-f64") ( f64.const 1.64))
      runner.assertReturn("type-first-f64", new WasmValue[] { WasmValue.f64(1.64) });

      // ( assert_return ( invoke "type-second-i32") ( i32.const 32))
      runner.assertReturn("type-second-i32", new WasmValue[] { WasmValue.i32(32) });

      // ( assert_return ( invoke "type-second-i64") ( i64.const 64))
      runner.assertReturn("type-second-i64", new WasmValue[] { WasmValue.i64(64L) });

      // ( assert_return ( invoke "type-second-f32") ( f32.const 32))
      runner.assertReturn("type-second-f32", new WasmValue[] { WasmValue.f32(32f) });

      // ( assert_return ( invoke "type-second-f64") ( f64.const 64.1))
      runner.assertReturn("type-second-f64", new WasmValue[] { WasmValue.f64(64.1) });

    }
  }
}
