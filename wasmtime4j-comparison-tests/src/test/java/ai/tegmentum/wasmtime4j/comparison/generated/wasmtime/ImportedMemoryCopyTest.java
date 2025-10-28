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
 * Generated test from WAST file: imported-memory-copy.wast
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream
 * Wasmtime implementation for this test case.
 */
public final class ImportedMemoryCopyTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = ImportedMemoryCopyTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("imported memory copy")
  public void testImportedMemoryCopy(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {

      // Compile and instantiate module 1
      // WAT file: ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/ImportedMemoryCopyTest_module1.wat
      final String moduleWat1 = loadResource("/ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/ImportedMemoryCopyTest_module1.wat");
      runner.compileAndInstantiate(moduleWat1);

      // ( register "foreign" $foreign)
      runner.registerModule("foreign");

      // Compile and instantiate module 2
      // WAT file: ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/ImportedMemoryCopyTest_module2.wat
      final String moduleWat2 = loadResource("/ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/ImportedMemoryCopyTest_module2.wat");
      runner.compileAndInstantiate(moduleWat2);

      // ( assert_return ( invoke "is hello?" ( i32.const 1000)) ( i32.const 1))
      runner.assertReturn("is hello?", new WasmValue[] { WasmValue.i32(1) }, WasmValue.i32(1000));

      // ( assert_return ( invoke "is olleh?" ( i32.const 2000)) ( i32.const 1))
      runner.assertReturn("is olleh?", new WasmValue[] { WasmValue.i32(1) }, WasmValue.i32(2000));

      // ( invoke "memory.copy" ( i32.const 500) ( i32.const 1000) ( i32.const 5))
      runner.invoke("memory.copy", WasmValue.i32(500), WasmValue.i32(1000), WasmValue.i32(5));

      // ( assert_return ( invoke "is hello?" ( i32.const 500)) ( i32.const 1))
      runner.assertReturn("is hello?", new WasmValue[] { WasmValue.i32(1) }, WasmValue.i32(500));

      // ( invoke "memory.copy" ( i32.const 1500) ( i32.const 1000) ( i32.const 5))
      runner.invoke("memory.copy", WasmValue.i32(1500), WasmValue.i32(1000), WasmValue.i32(5));

      // ( assert_return ( invoke "is hello?" ( i32.const 1500)) ( i32.const 1))
      runner.assertReturn("is hello?", new WasmValue[] { WasmValue.i32(1) }, WasmValue.i32(1500));

      // ( invoke "memory.copy" ( i32.const 1998) ( i32.const 2000) ( i32.const 5))
      runner.invoke("memory.copy", WasmValue.i32(1998), WasmValue.i32(2000), WasmValue.i32(5));

      // ( assert_return ( invoke "is olleh?" ( i32.const 1998)) ( i32.const 1))
      runner.assertReturn("is olleh?", new WasmValue[] { WasmValue.i32(1) }, WasmValue.i32(1998));

      // ( invoke "memory.copy" ( i32.const 2000) ( i32.const 1998) ( i32.const 5))
      runner.invoke("memory.copy", WasmValue.i32(2000), WasmValue.i32(1998), WasmValue.i32(5));

      // ( assert_return ( invoke "is olleh?" ( i32.const 2000)) ( i32.const 1))
      runner.assertReturn("is olleh?", new WasmValue[] { WasmValue.i32(1) }, WasmValue.i32(2000));

      // ( invoke "memory.copy" ( i32.const 2000) ( i32.const 2000) ( i32.const 5))
      runner.invoke("memory.copy", WasmValue.i32(2000), WasmValue.i32(2000), WasmValue.i32(5));

      // ( assert_return ( invoke "is olleh?" ( i32.const 2000)) ( i32.const 1))
      runner.assertReturn("is olleh?", new WasmValue[] { WasmValue.i32(1) }, WasmValue.i32(2000));

    }
  }
}
