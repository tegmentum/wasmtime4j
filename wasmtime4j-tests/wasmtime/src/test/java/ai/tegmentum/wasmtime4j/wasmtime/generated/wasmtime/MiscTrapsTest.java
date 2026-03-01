/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.tegmentum.wasmtime4j.wasmtime.generated.wasmtime;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.wasmtime.framework.WastTestRunner;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Generated test from WAST file: misc_traps.wast
 *
 * <p>This test validates that wasmtime4j produces the same results as the upstream Wasmtime
 * implementation for this test case.
 */
public final class MiscTrapsTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = MiscTrapsTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("misc traps")
  public void testMiscTraps(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);

    try (final WastTestRunner runner = new WastTestRunner()) {

      // Compile and instantiate module 1
      // WAT file: ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/MiscTrapsTest_module1.wat
      final String moduleWat1 =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/wasmtime/MiscTrapsTest_module1.wat");
      runner.compileAndInstantiate(moduleWat1);

      // ( assert_trap ( invoke "load_oob") "out of bounds memory access")
      runner.assertTrap("load_oob", "out of bounds memory access");

      // ( assert_trap ( invoke "load_oob") "out of bounds memory access")
      runner.assertTrap("load_oob", "out of bounds memory access");

      // Compile and instantiate module 2
      // WAT file: ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/MiscTrapsTest_module2.wat
      final String moduleWat2 =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/wasmtime/MiscTrapsTest_module2.wat");
      runner.compileAndInstantiate(moduleWat2);

      // ( assert_trap ( invoke "store_oob") "out of bounds memory access")
      runner.assertTrap("store_oob", "out of bounds memory access");

      // ( assert_trap ( invoke "store_oob") "out of bounds memory access")
      runner.assertTrap("store_oob", "out of bounds memory access");

      // Compile and instantiate module 3
      // WAT file: ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/MiscTrapsTest_module3.wat
      final String moduleWat3 =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/wasmtime/MiscTrapsTest_module3.wat");
      runner.compileAndInstantiate(moduleWat3);

      // ( assert_trap ( invoke "load_oob_0") "out of bounds memory access")
      runner.assertTrap("load_oob_0", "out of bounds memory access");

      // ( assert_trap ( invoke "load_oob_0") "out of bounds memory access")
      runner.assertTrap("load_oob_0", "out of bounds memory access");

      // Compile and instantiate module 4
      // WAT file: ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/MiscTrapsTest_module4.wat
      final String moduleWat4 =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/wasmtime/MiscTrapsTest_module4.wat");
      runner.compileAndInstantiate(moduleWat4);

      // ( assert_trap ( invoke "store_oob_0") "out of bounds memory access")
      runner.assertTrap("store_oob_0", "out of bounds memory access");

      // ( assert_trap ( invoke "store_oob_0") "out of bounds memory access")
      runner.assertTrap("store_oob_0", "out of bounds memory access");

      // Compile and instantiate module 5
      // WAT file: ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/MiscTrapsTest_module5.wat
      final String moduleWat5 =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/wasmtime/MiscTrapsTest_module5.wat");
      runner.compileAndInstantiate(moduleWat5);

      // ( assert_trap ( invoke "divbyzero") "integer divide by zero")
      runner.assertTrap("divbyzero", "integer divide by zero");

      // ( assert_trap ( invoke "divbyzero") "integer divide by zero")
      runner.assertTrap("divbyzero", "integer divide by zero");

      // Compile and instantiate module 6
      // WAT file: ai/tegmentum/wasmtime4j/comparison/generated/wasmtime/MiscTrapsTest_module6.wat
      final String moduleWat6 =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/wasmtime/MiscTrapsTest_module6.wat");
      runner.compileAndInstantiate(moduleWat6);

      // ( assert_trap ( invoke "unreachable") "unreachable")
      runner.assertTrap("unreachable", "unreachable");

      // ( assert_trap ( invoke "unreachable") "unreachable")
      runner.assertTrap("unreachable", "unreachable");
    }
  }
}
