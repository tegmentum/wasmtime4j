package ai.tegmentum.wasmtime4j.wasmtime.generated.wasmtime;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.nativeloader.PlatformDetector;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.wasmtime.framework.WastTestRunner;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Generated test from WAST file: traps-skip-catch-all.wast
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 *
 * <p>Note: This test is skipped on aarch64 because trap handling causes JVM crash (SIGILL). The
 * crash occurs when executing WebAssembly code that uses try_table with catch_all to handle traps
 * (unreachable, div-by-zero, stack overflow). Wasmtime's trap instructions conflict with JVM signal
 * handlers on ARM64 Darwin.
 *
 * <p>Root cause analysis:
 *
 * <ul>
 *   <li>The module uses try_table (catch_all $h) to catch exceptions
 *   <li>Functions cause various traps: unreachable, div-by-zero, stack-overflow
 *   <li>WebAssembly specification says traps should NOT be catchable
 *   <li>The SIGILL suggests Wasmtime trap instructions conflict with JVM on aarch64
 * </ul>
 */
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
    assumeTrue(
        PlatformDetector.detectArchitecture() == PlatformDetector.Architecture.X86_64,
        "Skipped on aarch64: trap handling causes JVM crash (SIGILL)");
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
