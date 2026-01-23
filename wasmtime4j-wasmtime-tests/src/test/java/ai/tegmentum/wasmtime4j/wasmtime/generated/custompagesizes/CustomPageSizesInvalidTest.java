/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.wasmtime.generated.custompagesizes;

import static org.junit.jupiter.api.Assertions.assertThrows;

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
 * Generated test from WAST file: custom-page-sizes/custom-page-sizes-invalid.wast
 *
 * <p>Tests that invalid custom page sizes are properly rejected. Only page sizes of 1 and 65536 are
 * valid.
 *
 * <p>Requires: custom_page_sizes = true
 *
 * <p>Source:
 * https://github.com/bytecodealliance/wasmtime/blob/main/tests/misc_testsuite/custom-page-sizes/custom-page-sizes-invalid.wast
 */
public final class CustomPageSizesInvalidTest extends DualRuntimeTest {
  private static String loadResource(final String path) throws IOException {
    try (final InputStream is = CustomPageSizesInvalidTest.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Invalid page size 2 - not 1 or 65536")
  public void testInvalidPageSize2(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/custompagesizes/CustomPageSizesInvalidTest_invalid2.wat");
      assertThrows(Exception.class, () -> runner.compileAndInstantiate(moduleWat));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Invalid page size 4 - not 1 or 65536")
  public void testInvalidPageSize4(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/custompagesizes/CustomPageSizesInvalidTest_invalid4.wat");
      assertThrows(Exception.class, () -> runner.compileAndInstantiate(moduleWat));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Invalid page size 8 - not 1 or 65536")
  public void testInvalidPageSize8(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/custompagesizes/CustomPageSizesInvalidTest_invalid8.wat");
      assertThrows(Exception.class, () -> runner.compileAndInstantiate(moduleWat));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Invalid page size 1024 - not 1 or 65536")
  public void testInvalidPageSize1024(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/custompagesizes/CustomPageSizesInvalidTest_invalid1024.wat");
      assertThrows(Exception.class, () -> runner.compileAndInstantiate(moduleWat));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Invalid page size 4096 - not 1 or 65536")
  public void testInvalidPageSize4096(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      final String moduleWat =
          loadResource(
              "/ai/tegmentum/wasmtime4j/wasmtime/generated/custompagesizes/CustomPageSizesInvalidTest_invalid4096.wat");
      assertThrows(Exception.class, () -> runner.compileAndInstantiate(moduleWat));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Invalid page size 0x20000 - larger than 64KiB")
  public void testInvalidPageSizeTooLarge(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    try (final WastTestRunner runner = new WastTestRunner()) {
      final String path =
          "/ai/tegmentum/wasmtime4j/wasmtime/generated/custompagesizes/"
              + "CustomPageSizesInvalidTest_invalid131072.wat";
      final String moduleWat = loadResource(path);
      assertThrows(Exception.class, () -> runner.compileAndInstantiate(moduleWat));
    }
  }
}
