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
 * Generated test from WAST file: bit-and-conditions.wast
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream
 * Wasmtime implementation for this test case.
 */
public final class BitAndConditionsTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = BitAndConditionsTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("bit and conditions")
  public void testBitAndConditions(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {

      // Compile and instantiate module 1
      // WAT file: ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/BitAndConditionsTest_module1.wat
      final String moduleWat1 = loadResource("/ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/BitAndConditionsTest_module1.wat");
      runner.compileAndInstantiate(moduleWat1);

      // ( assert_return ( invoke "if_b20" ( i32.const 0)) ( i32.const 200))
      runner.assertReturn("if_b20", new WasmValue[] { WasmValue.i32(200) }, WasmValue.i32(0));

      // ( assert_return ( invoke "if_b20" ( i32.const 0x100000)) ( i32.const 100))
      runner.assertReturn("if_b20", new WasmValue[] { WasmValue.i32(100) }, WasmValue.i32(0x100000));

      // ( assert_return ( invoke "select_b20" ( i32.const 0) ( i32.const 100) ( i32.const 200)) ( i32.const 200))
      runner.assertReturn("select_b20", new WasmValue[] { WasmValue.i32(200) }, WasmValue.i32(0), WasmValue.i32(100), WasmValue.i32(200));

      // ( assert_return ( invoke "select_b20" ( i32.const 0x100000) ( i32.const 100) ( i32.const 200)) ( i32.const 100))
      runner.assertReturn("select_b20", new WasmValue[] { WasmValue.i32(100) }, WasmValue.i32(0x100000), WasmValue.i32(100), WasmValue.i32(200));

      // ( assert_return ( invoke "eqz_b20" ( i32.const 0)) ( i32.const 1))
      runner.assertReturn("eqz_b20", new WasmValue[] { WasmValue.i32(1) }, WasmValue.i32(0));

      // ( assert_return ( invoke "eqz_b20" ( i32.const 0x100000)) ( i32.const 0))
      runner.assertReturn("eqz_b20", new WasmValue[] { WasmValue.i32(0) }, WasmValue.i32(0x100000));

      // ( assert_return ( invoke "if_b40" ( i64.const 0)) ( i64.const 200))
      runner.assertReturn("if_b40", new WasmValue[] { WasmValue.i64(200L) }, WasmValue.i64(0L));

      // ( assert_return ( invoke "if_b40" ( i64.const 0x10000000000)) ( i64.const 100))
      runner.assertReturn("if_b40", new WasmValue[] { WasmValue.i64(100L) }, WasmValue.i64(0x10000000000L));

      // ( assert_return ( invoke "select_b40" ( i64.const 0) ( i64.const 100) ( i64.const 200)) ( i64.const 200))
      runner.assertReturn("select_b40", new WasmValue[] { WasmValue.i64(200L) }, WasmValue.i64(0L), WasmValue.i64(100L), WasmValue.i64(200L));

      // ( assert_return ( invoke "select_b40" ( i64.const 0x10000000000) ( i64.const 100) ( i64.const 200)) ( i64.const 100))
      runner.assertReturn("select_b40", new WasmValue[] { WasmValue.i64(100L) }, WasmValue.i64(0x10000000000L), WasmValue.i64(100L), WasmValue.i64(200L));

      // ( assert_return ( invoke "eqz_b40" ( i64.const 0)) ( i32.const 1))
      runner.assertReturn("eqz_b40", new WasmValue[] { WasmValue.i32(1) }, WasmValue.i64(0L));

      // ( assert_return ( invoke "eqz_b40" ( i64.const 0x10000000000)) ( i32.const 0))
      runner.assertReturn("eqz_b40", new WasmValue[] { WasmValue.i32(0) }, WasmValue.i64(0x10000000000L));

      // ( assert_return ( invoke "if_bit32" ( i32.const 0) ( i32.const 1)) ( i32.const 200))
      runner.assertReturn("if_bit32", new WasmValue[] { WasmValue.i32(200) }, WasmValue.i32(0), WasmValue.i32(1));

      // ( assert_return ( invoke "if_bit32" ( i32.const 0) ( i32.const 0)) ( i32.const 200))
      runner.assertReturn("if_bit32", new WasmValue[] { WasmValue.i32(200) }, WasmValue.i32(0), WasmValue.i32(0));

      // ( assert_return ( invoke "if_bit32" ( i32.const 1) ( i32.const 1)) ( i32.const 200))
      runner.assertReturn("if_bit32", new WasmValue[] { WasmValue.i32(200) }, WasmValue.i32(1), WasmValue.i32(1));

      // ( assert_return ( invoke "if_bit32" ( i32.const 1) ( i32.const 33)) ( i32.const 200))
      runner.assertReturn("if_bit32", new WasmValue[] { WasmValue.i32(200) }, WasmValue.i32(1), WasmValue.i32(33));

      // ( assert_return ( invoke "if_bit32" ( i32.const 1) ( i32.const 0)) ( i32.const 100))
      runner.assertReturn("if_bit32", new WasmValue[] { WasmValue.i32(100) }, WasmValue.i32(1), WasmValue.i32(0));

      // ( assert_return ( invoke "if_bit32" ( i32.const 1) ( i32.const 32)) ( i32.const 100))
      runner.assertReturn("if_bit32", new WasmValue[] { WasmValue.i32(100) }, WasmValue.i32(1), WasmValue.i32(32));

      // ( assert_return ( invoke "if_bit32" ( i32.const 0x100000) ( i32.const 20)) ( i32.const 100))
      runner.assertReturn("if_bit32", new WasmValue[] { WasmValue.i32(100) }, WasmValue.i32(0x100000), WasmValue.i32(20));

      // ( assert_return ( invoke "if_bit32" ( i32.const 0x100000) ( i32.const 52)) ( i32.const 100))
      runner.assertReturn("if_bit32", new WasmValue[] { WasmValue.i32(100) }, WasmValue.i32(0x100000), WasmValue.i32(52));

      // ( assert_return ( invoke "if_bit64" ( i64.const 0) ( i64.const 1)) ( i64.const 200))
      runner.assertReturn("if_bit64", new WasmValue[] { WasmValue.i64(200L) }, WasmValue.i64(0L), WasmValue.i64(1L));

      // ( assert_return ( invoke "if_bit64" ( i64.const 0) ( i64.const 0)) ( i64.const 200))
      runner.assertReturn("if_bit64", new WasmValue[] { WasmValue.i64(200L) }, WasmValue.i64(0L), WasmValue.i64(0L));

      // ( assert_return ( invoke "if_bit64" ( i64.const 1) ( i64.const 1)) ( i64.const 200))
      runner.assertReturn("if_bit64", new WasmValue[] { WasmValue.i64(200L) }, WasmValue.i64(1L), WasmValue.i64(1L));

      // ( assert_return ( invoke "if_bit64" ( i64.const 1) ( i64.const 33)) ( i64.const 200))
      runner.assertReturn("if_bit64", new WasmValue[] { WasmValue.i64(200L) }, WasmValue.i64(1L), WasmValue.i64(33L));

      // ( assert_return ( invoke "if_bit64" ( i64.const 1) ( i64.const 0)) ( i64.const 100))
      runner.assertReturn("if_bit64", new WasmValue[] { WasmValue.i64(100L) }, WasmValue.i64(1L), WasmValue.i64(0L));

      // ( assert_return ( invoke "if_bit64" ( i64.const 1) ( i64.const 64)) ( i64.const 100))
      runner.assertReturn("if_bit64", new WasmValue[] { WasmValue.i64(100L) }, WasmValue.i64(1L), WasmValue.i64(64L));

      // ( assert_return ( invoke "if_bit64" ( i64.const 0x100000) ( i64.const 20)) ( i64.const 100))
      runner.assertReturn("if_bit64", new WasmValue[] { WasmValue.i64(100L) }, WasmValue.i64(0x100000L), WasmValue.i64(20L));

      // ( assert_return ( invoke "if_bit64" ( i64.const 0x100000) ( i64.const 52)) ( i64.const 200))
      runner.assertReturn("if_bit64", new WasmValue[] { WasmValue.i64(200L) }, WasmValue.i64(0x100000L), WasmValue.i64(52L));

      // ( assert_return ( invoke "if_bit64" ( i64.const 0x100000) ( i64.const 84)) ( i64.const 100))
      runner.assertReturn("if_bit64", new WasmValue[] { WasmValue.i64(100L) }, WasmValue.i64(0x100000L), WasmValue.i64(84L));

    }
  }
}
