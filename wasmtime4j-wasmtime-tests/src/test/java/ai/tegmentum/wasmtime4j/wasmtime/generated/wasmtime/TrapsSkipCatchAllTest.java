package ai.tegmentum.wasmtime4j.wasmtime.generated.wasmtime;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.wasmtime.framework.WastTestRunner;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Generated test from WAST file: traps-skip-catch-all.wast
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 *
 * <p>DISABLED: This test causes a JVM crash (exit code 132 = SIGILL - illegal instruction) when
 * executing WebAssembly code that uses try_table with catch_all to handle traps (unreachable,
 * div-by-zero, stack overflow). The crash appears to occur in Wasmtime's JIT-compiled code when
 * handling trap unwinding in combination with the exception handling proposal's try_table
 * construct.
 *
 * <p>Root cause analysis:
 *
 * <ul>
 *   <li>The module uses try_table (catch_all $h) to catch exceptions
 *   <li>Functions cause various traps: unreachable, div-by-zero, stack-overflow
 *   <li>WebAssembly specification says traps should NOT be catchable
 *   <li>The SIGILL suggests JIT code generation or execution issue in Wasmtime
 * </ul>
 *
 * <p>This is likely a Wasmtime internal issue with trap handling in combination with the exception
 * handling proposal. Requires further investigation at the Wasmtime level.
 */
@Disabled("Causes JVM crash - SIGILL (exit code 132) when handling traps with try_table/catch_all")
public final class TrapsSkipCatchAllTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = TrapsSkipCatchAllTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("traps skip catch all")
  public void testTrapsSkipCatchAll(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {

      // Compile and instantiate module 1
      // WAT file:
      // ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/TrapsSkipCatchAllTest_module1.wat
      final String moduleWat1 =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/wasmtime/TrapsSkipCatchAllTest_module1.wat");
      runner.compileAndInstantiate(moduleWat1);

      // ( assert_trap ( invoke "run" ( i32.const 0)) "unreachable")
      runner.assertTrap("run", "unreachable", WasmValue.i32(0));

      // ( assert_return ( invoke "g") ( i32.const 0))
      runner.assertReturn("g", new WasmValue[] {WasmValue.i32(0)});

      // ( assert_trap ( invoke "run" ( i32.const 1)) "divide by zero")
      runner.assertTrap("run", "divide by zero", WasmValue.i32(1));

      // ( assert_return ( invoke "g") ( i32.const 0))
      runner.assertReturn("g", new WasmValue[] {WasmValue.i32(0)});

      // ( assert_trap ( invoke "run" ( i32.const 2)) "call stack exhausted")
      runner.assertTrap("run", "call stack exhausted", WasmValue.i32(2));

      // ( assert_return ( invoke "g") ( i32.const 0))
      runner.assertReturn("g", new WasmValue[] {WasmValue.i32(0)});

      // ( assert_return ( invoke "run" ( i32.const 3)))
      runner.invoke("run", WasmValue.i32(3));

      // ( assert_return ( invoke "g") ( i32.const 1))
      runner.assertReturn("g", new WasmValue[] {WasmValue.i32(1)});

      // ( assert_trap ( invoke "run" ( i32.const 0)) "unreachable")
      runner.assertTrap("run", "unreachable", WasmValue.i32(0));

      // ( assert_return ( invoke "g") ( i32.const 0))
      runner.assertReturn("g", new WasmValue[] {WasmValue.i32(0)});
    }
  }
}
